Tags: Scala, continuous deployment, software development, configuration management, DevOps, abnormal programming
PAA: http://www.drdobbs.com/architecture-and-design/xebialabs-why-dev-needs-devops/240166541, 
Labels: compilable configuration, configuration management, Scala, 

# Compilable configuration of a distributed system

In this post we'd like to share an interesting way of dealing with configuration of a distributed system. 
The configuration is represented directly in Scala language in a type safe manner. 
An example implementation is described in details. 
Various aspects of the proposal are discussed, 
including influence on the overall development process.

![Overall configuration management process](https://habrastorage.org/webt/71/bl/ax/71blaxtldz-ia4yftyebaxbam7c.png)

### Introduction

Building robust distributed systems requires the use of correct and 
coherent configuration on all nodes.
A typical solution is to use a textual deployment description (terraform, ansible or something 
alike) and automatically generated configuration files (often - dedicated for each node/role). 
We would also want to use the same protocols of the same versions on each communicating nodes 
(otherwise we would experience incompatibility issues). In JVM world this means that at least
the messaging library should be of the same version on all communicating nodes.

What about testing the system? Of course, we should have unit tests for all components before 
coming to integration tests. To be able to extrapolate test results on runtime, 
we should make sure that the versions of all libraries are kept identical in both runtime 
and testing environments.

When running integration tests, it's often much easier to have 
the same classpath on all nodes. We just need to make sure that the same classpath is used 
on deployment. (It is possible to use different classpaths on different nodes, but it's 
more difficult to represent this configuration and correctly deploy it.) So in order to keep 
things simple we will only consider identical classpaths on all nodes. 

Configuration tends to evolve together with the software. We usually use versions to identify various 
stages of software evolution. It seems reasonable to cover configuration under version management and
identify different configurations with some labels. If there is only one configuration in production,
we may use single version as an identifier. Sometimes we may have multiple production environments. 
And for each environment we might need a separate branch of configuration. So configurations
might be labelled with branch and version to uniquely identify different configurations. Each 
branch label and version corresponds to a single combination of distributed nodes, ports, 
external resources, classpath library versions on each node. Here we'll only cover the single branch 
and identify configurations by a three component decimal version (1.2.3),
in the same way as other artifacts.

In modern environments configuration files are not modified manually anymore. Typically we generate
config files at deployment time and [never touch them](https://devrant.com/rants/1537005/dont-touch-it) afterwards. So one could ask why  
do we still use text format for configuration files? A viable option is to place the configuration
inside a compilation unit and benefit from compile-time configuration validation.

In this post we will examine the idea of keeping the configuration in the compiled artifact.

<cut/>

### Compilable configuration

In this section we will discuss an example of static configuration. Two simple services - echo service 
and the client of the echo service are being configured and implemented. Then two different 
distributed systems with both services are instantiated. One is for a single node configuration 
and another one for two nodes configuration.

A typical distributed system consists of a few nodes. The nodes could be identified using some type:
```scala
sealed trait NodeId
case object Backend extends NodeId
case object Frontend extends NodeId
```
or just
```scala
case class NodeId(hostName: String)
```
or even
```scala
object Singleton
type NodeId = Singleton.type
```

These nodes perform various roles, run some services and should 
be able to communicate with the other nodes by means of TCP/HTTP connections.

For TCP connection at least a port number is required. We also want to make sure that client and server are talking the 
same protocol. In order to model a connection between nodes let's declare the following class:

```scala
case class TcpEndPoint[Protocol](node: NodeId, port: Port[Protocol])
```
where `Port` is just an `Int` within the allowed range:
```scala
type PortNumber = Refined[Int, Closed[_0, W.`65535`.T]]
```
<spoiler title="Refined types">
See [refined](https://github.com/fthomas/refined/) library. In short, it allows to add compile time
constraints to other types. In this case `Int` is only allowed to have 16-bit values that can 
represent port number. There is no requirement to use this library for this configuration approach.
It just seems to fit very well.
</spoiler>

For HTTP (REST) we might also need a path of the service:
```scala
type UrlPathPrefix = Refined[String, MatchesRegex[W.`"[a-zA-Z_0-9/]*"`.T]]
case class PortWithPrefix[Protocol](portNumber: PortNumber, pathPrefix: UrlPathPrefix)
```

<spoiler title="Phantom type">
In order to identify protocol during compilation we are using the Scala 
feature of declaring type argument `Protocol` that is not used in the class. It's a so called _phantom type_. 
At runtime we rarely need an instance of protocol identifier, that's why we don't store it.
During compilation this phantom type gives additional type safety. We cannot pass port with 
incorrect protocol.
</spoiler>

One of the most widely used protocols is REST API with Json serialization:
```scala
sealed trait JsonHttpRestProtocol[RequestMessage, ResponseMessage]
```
where `RequestMessage` is the base type of messages that client can send to server and `ResponseMessage` is
the response message from server. Of course, we may create other protocol descriptions that specify 
the communication protocol with the desired precision.

For the purposes of this post we'll use a simpler version of the protocol:
```scala
sealed trait SimpleHttpGetRest[RequestMessage, ResponseMessage]
```
In this protocol request message is appended to url and response message is returned as plain string.

A service configuration could be described by the service name, a collection of ports and 
some dependencies. There are a few possible ways of how to represent all these elements in Scala 
(for instance, `HList`, algebraic data types).
For the purposes of this post we'll use Cake Pattern and represent combinable pieces (modules) as traits. 
(Cake Pattern is not a requirement for this compilable configuration approach. It just one possible implementation of the idea.)
  
Dependencies could be represented using the Cake Pattern as endpoints of other nodes:
```scala
  type EchoProtocol[A] = SimpleHttpGetRest[A, A]

  trait EchoConfig[A] extends ServiceConfig {
    def portNumber: PortNumber = 8081
    def echoPort: PortWithPrefix[EchoProtocol[A]] = PortWithPrefix[EchoProtocol[A]](portNumber, "echo")
    def echoService: HttpSimpleGetEndPoint[NodeId, EchoProtocol[A]] = providedSimpleService(echoPort)
  }
```
Echo service only needs a port configured. And we declare that this port supports echo protocol.
Note that we do not need to specify a particular port at this moment, because trait's allows abstract methods declarations.
If we use abstract methods, compiler will require an implementation in a configuration instance. Here we have
provided the implementation (`8081`) and it will be used as the default value if we skip it in a concrete configuration.

We can declare a dependency in the configuration of the echo service client:
```scala
  trait EchoClientConfig[A] {
    def testMessage: String = "test"
    def pollInterval: FiniteDuration
    def echoServiceDependency: HttpSimpleGetEndPoint[_, EchoProtocol[A]]
  }
```
Dependency has the same type as the `echoService`. In particular, it demands the same protocol. 
Hence, we can be sure that if we connect these two dependencies they will work correctly.

<spoiler title="Services implementation">
A service needs a function to start and gracefully shutdown. (Ability to shutdown a service is
critical for testing.)
Again there are a few options of specifying
such a function for a given config (for instance, we could use type classes). For this post we'll 
again use Cake Pattern.
We can represent a service using `cats.Resource` which already provides bracketing and 
resource release.
In order to acquire a resource we should provide a configuration and some runtime context. 
So the service starting function might look like:

```scala
  type ResourceReader[F[_], Config, A] = Reader[Config, Resource[F, A]]
  
  trait ServiceImpl[F[_]] {
    type Config
    def resource(
      implicit
      resolver: AddressResolver[F],
      timer: Timer[F],
      contextShift: ContextShift[F],
      ec: ExecutionContext,
      applicative: Applicative[F]
    ): ResourceReader[F, Config, Unit]
  }
```
where 

* `Config` - type of configuration that is required by this service starter
* `AddressResolver` - a runtime object that has the ability to obtain real addresses of other nodes (keep reading for details).

the other types comes from `cats`:
* `F[_]` - effect type (In the simplest case `F[A]` could be just `() => A`. In this post we'll use `cats.IO`.)
* `Reader[A,B]` - is more or less a synonym for a function `A => B`
* `cats.Resource` - has ways to acquire and release
* `Timer` - allows to sleep/measure time
* `ContextShift` - analog of `ExecutionContext`
* `Applicative` - wrapper of functions in effect (almost a monad) (we might eventually replace it with something else)

Using this interface we can implement a few services. For instance, a service that does nothing:
```scala
  trait ZeroServiceImpl[F[_]] extends ServiceImpl[F] {
    type Config <: Any
    def resource(...): ResourceReader[F, Config, Unit] =
      Reader(_ => Resource.pure[F, Unit](()))
  }
```
</spoiler>

(See [Source code](https://github.com/Primetalk/distributed-compilable-configuration) 
for other services implementations - 
[echo service](https://github.com/Primetalk/distributed-compilable-configuration/blob/master/src/main/scala/ru/primetalk/config/example/echo/EchoServiceService.scala), 
[echo client](https://github.com/Primetalk/distributed-compilable-configuration/blob/master/src/main/scala/ru/primetalk/config/example/echo/EchoClientService.scala)
and [lifetime controllers](https://github.com/Primetalk/distributed-compilable-configuration/blob/master/src/main/scala/ru/primetalk/config/example/meta/Meta.scala#L143).)

A node is a single object that runs a few services (starting a chain of resources is enabled by Cake Pattern):
```scala
object SingleNodeImpl extends ZeroServiceImpl[IO]
  with EchoServiceService
  with EchoClientService
  with FiniteDurationLifecycleServiceImpl
{
  type Config = EchoConfig[String] with EchoClientConfig[String] with FiniteDurationLifecycleConfig
}
```
Note that in the node we specify the exact type of configuration that is needed by this node. 
Compiler won't let us to build the object (Cake) with insufficient type, because each service trait declares 
a constraint on the `Config` type. Also we won't be able to start node without providing 
complete configuration.

<spoiler title="Node address resolution">

In order to establish a connection we need a real host address for each node.
It might be known later than other parts of the configuration. Hence, we need a way to supply a mapping
between node id and it's actual address. This mapping is a function:
```scala
case class NodeAddress[NodeId](host: Uri.Host)
trait AddressResolver[F[_]] {
  def resolve[NodeId](nodeId: NodeId): F[NodeAddress[NodeId]]
}
```
There are a few possible ways to implement such a function.
1. If we know actual addresses before deployment, during node hosts instantiation, then we can generate 
Scala code with the actual addresses and run the build afterwards (which performs compile time 
checks and then runs integration test suite). In this case our mapping function is known statically 
and can be simplified to something like a `Map[NodeId, NodeAddress]`. 
2. Sometimes we obtain actual addresses only at a later point when the node is actually started, 
or we don't have addresses of nodes that haven't been started yet. In this case we might have a 
discovery service that is started before all other nodes and each node might advertise it's address 
in that service and subscribe to dependencies. 
3. If we can modify `/etc/hosts`, we can use predefined host names (like `my-project-main-node` and `echo-backend`)
and just associate this name with ip address at deployment time.

In this post we don't cover these cases in more details.
In fact in our toy example all nodes will have the same IP address - `127.0.0.1`.
</spoiler>

In this post we'll consider two distributed system layouts:
1. Single node layout, where all services are placed on the single node.
2. Two node layout, where service and client are on different nodes.

The configuration for a [single 
node](https://github.com/Primetalk/distributed-compilable-configuration/blob/master/src/main/scala/ru/primetalk/config/example/echo/SingleNodeConfig.scala) 
layout is as follows:
<spoiler title="Single node configuration">
```scala
object SingleNodeConfig extends EchoConfig[String] 
  with EchoClientConfig[String] with FiniteDurationLifecycleConfig
{
  case object Singleton // identifier of the single node 
  // configuration of server
  type NodeId = Singleton.type
  def nodeId = Singleton

  /** Type safe service port specification. */
  override def portNumber: PortNumber = 8088

  // configuration of client

  /** We'll use the service provided by the same host. */
  def echoServiceDependency = echoService

  override def testMessage: UrlPathElement = "hello"

  def pollInterval: FiniteDuration = 1.second

  // lifecycle controller configuration
  def lifetime: FiniteDuration = 10500.milliseconds // additional 0.5 seconds so that there are 10 requests, not 9.
}
```
</spoiler>

Here we create a single configuration that extends both server and client configuration. 
Also we configure a lifecycle controller that will normally terminate client and server 
after `lifetime` interval passes.

The same set of service implementations and configurations can be used to create a system's layout with 
two separate nodes. We just need to create [two separate node 
configs](https://github.com/Primetalk/distributed-compilable-configuration/blob/master/src/main/scala/ru/primetalk/config/example/echo/TwoJvmConfig.scala)
with the appropriate services:
<spoiler title="Two nodes configuration">
```scala
  object NodeServerConfig extends EchoConfig[String] with SigTermLifecycleConfig
  {
    type NodeId = NodeIdImpl

    def nodeId = NodeServer

    override def portNumber: PortNumber = 8080
  }

  object NodeClientConfig extends EchoClientConfig[String] with FiniteDurationLifecycleConfig
  {
    // NB! dependency specification
    def echoServiceDependency = NodeServerConfig.echoService

    def pollInterval: FiniteDuration = 1.second

    def lifetime: FiniteDuration = 10500.milliseconds // additional 0.5 seconds so that there are 10 request, not 9.

    def testMessage: String = "dolly"
  }
``` 
</spoiler>

See how we specify the dependency. We mention the other node's provided service as a dependency of 
the current node. The type of dependency is checked because it contains phantom type that describes 
protocol. And at runtime we'll have the correct node id. This is one of the important aspects of
the proposed configuration approach. It provides us with the ability to set port only once and 
make sure that we are referencing the correct port. 

<spoiler title="Two nodes implementation">

For this configuration we use exactly the same services implementations. No changes at all. 
However, we create two different node implementations that contain different set of services:
```scala
  object TwoJvmNodeServerImpl extends ZeroServiceImpl[IO] with EchoServiceService with SigIntLifecycleServiceImpl {
    type Config = EchoConfig[String] with SigTermLifecycleConfig
  }

  object TwoJvmNodeClientImpl extends ZeroServiceImpl[IO] with EchoClientService with FiniteDurationLifecycleServiceImpl {
    type Config = EchoClientConfig[String] with FiniteDurationLifecycleConfig
  }
``` 

The first node implements server and it only needs server side config. The second node implements client 
and needs another part of config. Both nodes require some lifetime specification.
For the purposes of this post service node will have infinite lifetime that could be terminated 
using `SIGTERM`, while echo client will terminate after the configured finite duration. See the 
[starter application](https://github.com/Primetalk/distributed-compilable-configuration/blob/master/src/main/scala/ru/primetalk/config/example/echo/TwoJvmStarterApp.scala) 
for details.
</spoiler>

#### Overall development process 

Let's see how this approach changes the way we work with configuration. 

The configuration as code will be compiled and produces an artifact. It seems reasonable to separate
configuration artifact from other code artifacts. Often we can have multitude of configurations on
the same code base. And of course, we can have multiple versions of various configuration branches.
In a configuration we can select particular versions of libraries and this will remain constant 
whenever we deploy this configuration. 

A configuration change becomes code change. So it should be covered by the same 
quality assurance process:

Ticket -> PR -> review -> merge -> continuous integration -> continuous deployment

There are the following consequences of the approach:

1. The configuration is coherent for a particular system's instance. It seems that there 
is no way to have incorrect connection between nodes. 

2. It's not easy to change configuration just in one node. It seems unreasonable to log 
in and change some text files. So configuration drift becomes less possible.
   
3. Small configuration changes are not easy to make.

4. Most of the configuration changes will follow the same development process, and 
it will pass some review.

Do we need a separate repository for production configuration? The production configuration might 
contain sensitive information that we would like to keep out of reach of many people. So it might
worth keeping a separate repository with restricted access that will contain the production 
configuration. We may split the configuration into two parts - one that contains most open 
parameters of production and one that contains the secret part of configuration. This would 
enable access to most of the developers to the vast majority of parameters while restricting
access to really sensitive things. It's easy to accomplish this using intermediate traits with 
default parameter values.  

### Variations

Let's see pros and cons of the proposed approach compared to the other configuration management 
techniques.

First of all, we'll list a few alternatives to the different aspects of 
the proposed way of dealing with configuration:
1. Text file on the target machine. 
2. Centralized key-value storage (like `etcd`/`zookeeper`).
3. Subprocess components that could be reconfigured/restarted without restarting process.
4. Configuration outside artifact and version control.

Text file gives some flexibility in terms of ad-hoc fixes. A system's administrator can login to the
target node, make a change and simply restart the service. This might not be as good for bigger systems. 
No traces are left behind the change. The change is not reviewed by another pair of eyes. 
It might be difficult to find out what have caused the change. It has not been tested.
From distributed system's perspective an administrator can simply forget to update the configuration 
in one of the other nodes.

(Btw, if eventually there will be a need to start using text config files, we'll only have to add 
parser + validator that could produce the same `Config` type and that would be enough to start using 
text configs. This also shows that the complexity of compile-time configuration is a little smaller
that the complexity of text-based configs, because in text-based version we need some additional 
code.)

Centralized key-value storage is a good mechanism for distributing application meta parameters. Here
we need to think about what we consider to be configuration values and what is just data. 
Given a function `C => A => B` we usually call rarely changing values `C` "configuration", while 
frequently changed data `A` - just input data. 
Configuration should be provided to the function earlier than the data `A`. 
Given this idea we can say that 
it's expected frequency of changes what could be used to distinguish 
configuration data from just data. Also data typically
comes from one source (user) and configuration comes from a different source (admin). Dealing with 
parameters that can be changed after process initialization leads to an increase of 
application complexity. For such parameters we'll have to handle their delivery mechanism, 
parsing and validation, handling incorrect values. Hence, in order to reduce program complexity, 
we'd better reduce the number of parameters that can change at runtime 
(or even eliminate them altogether).

From the perspective of this post we should make a distinction between static and dynamic parameters.
If service logic requires rare change of some parameters at runtime, then we may call them 
dynamic parameters. Otherwise they are static and could be configured using the proposed approach. 
For dynamic reconfiguration other approaches might be needed. For example, parts of the system might be
restarted with the new configuration parameters in a similar way to restarting separate processes of 
a distributed system.
(My humble opinion is to avoid runtime reconfiguration because it increases complexity of the system.
It' might be more straightforward to just rely on OS support of restarting processes. 
Though, it might not always be possible.)

One important aspect of using static configuration that sometimes makes people consider dynamic configuration 
(without other reasons) is service downtime during configuration update. Indeed, if we have 
to make changes to static configuration, we have to restart the system so that new values become effective.
The requirements for downtime vary for different systems, so it might not be that critical. If it is critical,
then we have to plan ahead for any system restarts. For instance, we could implement 
[AWS ELB connection draining](https://aws.amazon.com/blogs/aws/elb-connection-draining-remove-instances-from-service-with-care/).
In this scenario whenever we need to restart the system, we start a new instance of the system in parallel, then 
switch ELB to it, while letting the old system to complete servicing existing connections.

What about keeping configuration inside versioned artifact or outside? Keeping configuration inside
an artifact means in most of the cases that this configuration has passed the same quality assurance 
process as other artifacts. So one might be sure that the configuration is of good quality and 
trustworthy. On the contrary configuration in a separate file means that there are no traces of who
and why made changes to that file. Is this important? We believe that for most production systems
it's better to have stable and high quality configuration.

Version of the artifact allows to find out
when it was created, what values it contains, what features are enabled/disabled, who
was responsible for making each change in the configuration. It might require some effort to keep 
configuration inside an artifact and it's a design choice to make.

### Pros & cons

Here we would like to highlight some advantages and to discuss some disadvantages of the proposed approach.

#### Advantages

Features of the compilable configuration of a complete distributed system:

1. Static check of configuration. This gives a high level of confidence, 
that the configuration is correct given type constraints.
2. Rich language of configuration. Typically other configuration approaches are limited to at most variable substitution.
Using Scala one can use wide range of language features to make configuration better. For instance, we can use traits to
provide default values, objects to set different scope, we can refer to `val`s defined only once in the outer scope (DRY). 
It's possible to use literal sequences, or instances of certain classes (`Seq`, `Map`, etc.).
3. DSL. Scala has decent support for DSL writers. One can use these features to establish a configuration language that is
more convenient and end-user friendly, so that the final configuration is at least readable by domain users.
4. Integrity and coherence across nodes. One of the benefits of having configuration for the whole distributed system
in one place is that all values are defined strictly once and then reused in all places where we need them. Also type safe
port declarations ensures that in all possible correct configurations the system's nodes will speak the same language.
There are explicit dependencies between nodes which makes it hard to forget to provide some services.
5. High quality of changes. The overall approach of passing configuration changes through normal PR process establishes 
high standards of quality also in configuration.
6. Simultaneous configuration changes. Whenever we make any changes in the configuration automatic deployment ensures that
all nodes are being updated.
7. Application simplification. The application doesn't need to parse and validate configuration and handle incorrect 
configuration values. This simplifies the overall application. (Some complexity increase is in the configuration itself,
but it's a conscious trade-off towards safety.) It's pretty straightforward to return to ordinary configuration - just 
add the missing pieces. It's easier to get started with compiled configuration and postpone implementation of additional
pieces to some later times.
8. Versioned configuration. Due to the fact that configuration changes follow the same development process, as a result
we get an artifact with unique version. It allows us to switch configuration back if needed. We can even deploy a
configuration that was used a year ago and it will work exactly the same way. Stable configuration improves
predictability and reliability of the distributed system. 
The configuration is fixed at compile time and cannot be easily tampered on a production system.
9. Modularity. The proposed framework is modular and modules could be combined in various ways to 
support different configurations (setups/layouts). In particular, it's possible to have a small scale single node 
layout and a large scale multi node setting. It's reasonable to have multiple production layouts.
10. Testing. 
For testing purposes one might implement a mock service and use it as a dependency in a type safe way.
A few different testing layouts with various parts replaced by mocks could be maintained simultaneously.
11. Integration testing. Sometimes in distributed systems it's difficult to run integration tests. 
Using the described approach to type safe configuration of the complete distributed system, 
we can run all distributed parts on a single server in a controllable way. It's easy to emulate the situation 
when one of the services becomes unavailable.

#### Disadvantages

The compiled configuration approach is different from "normal" configuration and it might not suit all needs. Here 
are some of the disadvantages of the compiled config:
1. Static configuration. It might not be suitable for all applications. In some cases there is a need of quickly fixing
the configuration in production bypassing all safety measures. This approach makes it more difficult. 
The compilation and redeployment are required after making any change in configuration. This is both the feature and the burden.
2. Configuration generation. When config is generated by some automation tool this approach requires subsequent compilation
(which might in turn fail). It might require additional effort to integrate this additional step into the build system.
3. Instruments. There are plenty of tools in use today that rely on text-based configs. Some of them 
won't be applicable when configuration is compiled.
4. A shift in mindset is needed. Developers and DevOps are familiar with text configuration files. The idea of compiling
configuration might appear strange to them.
5. Before introducing compilable configuration a high quality software development process is required.

There are some limitations of the implemented example:

1. If we provide extra config that is not demanded by the node implementation, compiler won't help us
to detect the absent implementation. This could be addressed by using `HList` or ADTs (case classes) for node
configuration instead of traits and Cake Pattern.
2. We have to provide some boilerplate in config file: (`package`, `import`, `object` declarations;
`override def`'s for parameters that have default values). This might be partially addressed using a DSL.
3. In this post we do not cover dynamic reconfiguration of clusters of similar nodes.

### Conclusion

In this post we have discussed the idea of representing configuration directly in the source code in a type safe way.
The approach could be utilized in many applications as a replacement to 
xml- and other text- based configs. Despite that our example has been implemented in Scala, 
it could also be translated to other compilable languages (like Kotlin, C#, Swift, etc.).
One could try this approach in a new project and, in case it doesn't fit 
well, switch to the old fashioned way.

Of course, compilable configuration requires high quality development process. In return it promises to provide 
equally high quality robust configuration.

This approach could be extended in various ways:
1. One could use macros to perform configuration validation and fail at compile time in case of any business-logic 
constraints failures.
2. A DSL could be implemented to represent configuration in a domain-user-friendly way.
3. Dynamic resource management with automatic configuration adjustments. For instance, when we adjust the number of cluster 
nodes we might want (1) the nodes to obtain slightly modified configuration; (2) cluster manager to receive new nodes info. 

### Thanks

I would like to say thank you to Andrey Saksonov, Pavel Popov, Anton Nehaev for giving inspirational feedback
on the draft of this post that helped me make it more clear.
