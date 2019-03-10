# Compilable configuration of a distributed system

Tags: Scala, continuous deployment, software development, configuration management, DevOps

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
config files at deployment time and never touch them afterwards. So one could ask why  
do we still use text format for configuration files? A viable option is to place the configuration
inside a compilation unit and benefit from compile-time configuration validation.

In this post we will examine the idea of keeping the configuration in the compiled artifact.

<cut/>

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

These nodes perform various roles and should 
be able to communicate with the other nodes by means of some (TCP/HTTP) connections. For TCP connection 
at least a port number is required. We also want to make sure that client and server are talking the 
same protocol. 
In order to model a connection between nodes let's declare the following class:

```scala
case class EndPoint[Protocol](node: NodeId, port: Port[Protocol])
```
where `Port` is just an `Int` within the allowed range:
```scala
type PortNumber = Refined[Int, Closed[_0, W.`65535`.T]]
type UrlPathPrefix = Refined[String, MatchesRegex[W.`"[a-zA-Z_0-9/]*"`.T]]
case class PortWithPrefix[Protocol](portNumber: PortNumber, pathPrefix: UrlPathPrefix)
```
(See [refined](https://github.com/fthomas/refined/) library.)

`Protocol` is a phantom type that identifies the used protocol. At runtime we rarely need
an instance of protocol identifier. Hence - only compile time phantom type.

One of the most widely used protocols is REST API with Json serialization:
```scala
sealed trait JsonHttpRestProtocol[RequestMessage, ResponseMessage]
```
where `RequestMessage` is the base type of messages that client can send to server and `ResponseMessage` is
the response message from server. Of course, we may create other protocol descriptions that specify 
the communication protocol with the desired precision.

For the purposes of this post we'll use a simpler version of protocol:
```scala
sealed trait SimpleHttpGetRest[RequestMessage, ResponseMessage]
```
In this protocol request message is added to url and response message is returned as plain string.

A service configuration could be described by the service name, a collection of ports and 
some dependencies. There are a few possible ways of how to represent all these elements in Scala (`HList`, algebraic types).
For the purposes of this post we'll use Cake Pattern and represent combinable pieces as traits.
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
Note that we do not need to specify a particular port at this moment. We can postpone it until we connect 
a few nodes.

We can declare a dependency in the configuration of the echo service client:
```scala
  trait EchoClientConfig[A] {
    def testMessage: String = "test"
    def pollInterval: FiniteDuration
    def echoServiceDependency: HttpSimpleGetEndPoint[_, EchoProtocol[A]]
  }
```
Dependency has the same type as the `echoService`. In particular, it demands the same protocol. 
Hence we can be sure that if we connect these two dependencies they will work correctly.

A service needs a function to start and gracefully shutdown. Again there are a few options of specifying
such a function for a given config (for instance, we could use type classes). For this post we'll use
Cake Pattern.
We can represent a service using `cats.Resource` which already provides bracketing and resource release.
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
* `AddressResolver` - a runtime object that knows real addresses of other nodes.

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
(See [Source code](https://github.com/Primetalk/distributed-compilable-configuration) 
for other services implementations - echo service, echo client and lifetime controllers.)

A node is a single object that can run a few services:
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
a constraint on the `Config` type.  

In order to establish a connection we need a real host address for each node.
It might be known later than other parts of the configuration. Hence we need a way to supply a mapping
between node id and it's actual address. This mapping is a function:
```scala
case class NodeAddress[NodeId](host: Uri.Host)
trait AddressResolver[F[_]] {
  def resolve[NodeId](nodeId: NodeId): F[NodeAddress[NodeId]]
}
```     
If we know actual addresses before deployment, during node hosts instantiation, then we can generate 
Scala code with the actual addresses and run builder afterwards (which performs compile time 
checks and then runs integration test suite). In this case our mapping function is known statically 
and can be simplified to something like a `Map[NodeId, NodeAddress]`. 

Sometimes though we obtain actual addresses only at a later point when the node is actually started, 
or we don't have addresses of nodes that haven't been started yet. In this case we might have a 
discovery service that is started before all other nodes and each node might advertise it's address 
in that service and subscribe to dependencies. In this post we don't cover this case in more details.
In fact in our toy example all nodes have the same IP address - `127.0.0.1`.

The configuration for a [single 
node](https://github.com/Primetalk/distributed-compilable-configuration/blob/master/src/main/scala/ru/primetalk/config/example/echo/SingleNodeConfig.scala) 
setting is as follows:
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

Here we create a single configuration that extends both server and client configuration. 
Also we configure a lifecycle controller that will normally terminate client and server 
after `lifetime` interval passes.

The same set of role implementations and configurations can be used to create a setting with 
two separate nodes. We just need to create [two separate node 
configs](https://github.com/Primetalk/distributed-compilable-configuration/blob/master/src/main/scala/ru/primetalk/config/example/echo/TwoJvmConfig.scala)
with the appropriate services:
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

See how we specify the dependency. We mention the other node's provided service as a dependency of 
the current node. The type of dependency is checked because it contains phantom type that describes 
protocol. And at runtime we'll have the correct node id. 

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
using `SIGTERM`. See 
[starter application](https://github.com/Primetalk/distributed-compilable-configuration/blob/master/src/main/scala/ru/primetalk/config/example/echo/TwoJvmStarterApp.scala) 
for details.

In conclusion I'd like to highlight some features of the configuration of the complete 
distributed system:

1. Type safe configuration. No need to do runtime parsing and validation.
2. Single file could contain configuration for all nodes.
3. There are explicit dependencies between nodes.
4. Each configuration parameter could be specified only once (DRY). For instance, 
we can use names of outer scope to specify common reusable parameters.
5. The configuration is fixed at compile time and cannot be easily tampered on a production system.
6. The configuration framework is modular and allows various settings. In particular, it's possible 
to have a small scale single node configuration and a large scale multi node setting. For testing purposes 
one might implement a mock service and use it as a dependency in a type safe way.
7. Sometimes in distributed systems it's difficult to run integration tests. Using the described approach
to type safe configuration of the complete distributed system, we can run all distributed parts on 
a single server in a controllable way. 
Also we can create a few different testing configuration with various parts replaced by mocks. 
It's easy to emulate the situation when one of the services becomes unavailable.     

There are some limitations:
1. If we provide extra config that is not demanded by the node implementation, compiler won't help us
to detect the absent implementation. This could be addressed by using `HList` or a case class for node
configuration instead of traits and Cake Pattern.
2. We have to provide some boilerplate in config file: (`package`, `import`, `object` declarations, 
`override def`'s for parameters that have default values).
3. The compilation and redeployment are required after making any change in configuration. 
This is both the feature and the burden.
4. In this post we do not cover dynamic reconfiguration of clusters of similar nodes.

The approach, described in this post, could be utilized in many applications as a replacement to 
xml- and text- based configs. It could also be translated to other compilable languages
(like Kotlin, C#, Swift, etc.). 
