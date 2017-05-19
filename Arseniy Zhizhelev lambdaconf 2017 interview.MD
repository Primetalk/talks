# Arseniy Zhizhelev LambdaConf 2017 Interview

Arseniy Zhizhelev will be giving a talk at
[LambdaConf 2017][CONF]. He will be speaking about Flexible Typed Ontology Applications.

[CONF]: http://lambdaconf.us/events/2017/lcusc.html

Follow him on his [Homepage], [GitHub], and [Twitter].

[Homepage]: http://www.primetalk.ru/
[GitHub]: https://github.com/Primetalk
[Twitter]: https://twitter.com/azhizhelev

***PurelyFunctional.tv***: How did you get into Functional Programming?
 
***Arseniy Zhizhelev***: I was running a startup with not much resources and we suffered from various typical troubles - multithreading issues under load, unconstrained complexity growth, dependency injection via reflection, and so on. We spent some time fighting with these problems and we conquered most of them, however, it wasn't easy and our pace of development was not as fast as we wanted it to be.

One lucky day we decided to try Scala in one of smaller projects. And it was a huge (as we perceived) boost in our productivity. Scala removed quite a lot of Java's verbosity and we immediately gained some benefits. So we never wrote new code using Java since then. As we were mastering Scala at this level ("Java without semicolons") we come to certain boundaries that were not easily comprehensible. And our progress slowed down almost to "stagnation". Then our team passed the brilliant course of Martin Odersky. It was amazing. The influence of that course was so big! We created open source library SynapseGrid that runs immutable Data Flow based on a diagram composed from the simplest building blocks - Scala functions. We completely rewrote our project. And it wasn't that difficult because immutability and Functional Programming helped to protect us from most of our previous troubles.
 
***PF.tv***: What is your talk about?
 
***AZ***: My LambdaConf's talk is about an interesting technique for modelling data within multilayered applications. In today's world typical data structure is Json. It is very flexible, convenient, versatile. Unfortunately, it is not typesafe. The proposed technique is a unique fusion of ontology ideas, features of strong Scala type system and Json flexibility. Variants of this technique have been implemented in a few projects and proved to provide indispensable foundation for rather generic data handling applications. 
 
***PF.tv***: Who is your talk for?

***AZ***: I'm sure that this talk might be interested to full stack developers. In particular, this concept can be used throughout the whole application from data storage layer to front end.

***PF.tv***: What do you hope people will take away from the talk?

***AZ***: I hope that people will see new opportunities for data representation in their applications. And will be able to abstract their data to a higher level using ontology. And have a single point of truth in their applications - domain ontology model, that can be directly used to handle data in any part of the application.

***PF.tv***: What concepts do you recommend people be familiar with to maximize their experience with the talk?

***AZ***: I would suggest to familiarize type classes and ontology concepts. Also there are a few levels of abstraction in the implementation:

* data level (different forms), 
* ontology level to describe data structure ("single point of truth"), 
* meta level - to give instruments to define ontology,
* and "meta-meta" level to facilitate meta level definitions.

These abstractions might require some mental effort to get used to.

***PF.tv***: What resources are available for people who want to study up before the talk?

***AZ***: For type classes and their features there is a typelevel.org initiative with good resources. For ontology there are also plenty of resources about RDF/OWL ontology, and good tutorials in protégé ontology editor. As for abstraction levels, any fundamental book on software development will do very well.

***PF.tv***: Where can people follow you online?

***AZ***: I publish some pet projects on github from time to time. And sometimes I publish posts on [habrahabr](https://habrahabr.ru/) (for those who read Russian or feel comfortable with translation tools).

***PF.tv***: Are there any projects you'd like people to be aware of? How can people help out?

***AZ***: There is an open source library SynapseGrid that gives an alternative perspective on data streaming applications. It is a framework for building dynamic event processing (streaming) systems with managed state and with super low latencies (measured by milliseconds). This library has unique introspection abilities, allows to construct systems with abstracted subsystems that have multiple inputs/outputs (not as constrained as simple functions with only one output), allows to describe/define the complete distributed system in a single scheme.

The key contribution that is really-really needed and is very welcome is documentation and popularization. The library is powerful, but it lacks documentation for many important parts.

***PF.tv***: Where do you see the state of  functional programming in 10 years?

***AZ***: I observe significant progress in type systems and I feel that during upcoming 10 year we will see a couple of revolutions here.

Another interesting trend is in "build systems" or delivery systems (aka "continuous delivery"). Our "build" process is changing towards programmatic definition of all aspects of build including infrastructure definition. And single software source works on all the levels from source code to the ultimate running system. For instance, there is type level programming that allows to run computation during compilation, there are macros that work at this stage too, there is build script that influence one of the intermediate step towards production, there are deployment scripts, and there are runtime monitoring and management. Everything in this chain is created as a piece of software in the form of some source file. And we'll see rapid abstraction level growth in all these areas during the next 10 years.

From data perspective the key elements are snapshots and event sourcing (CQRS). These concepts will penetrate everywhere, and we'll soon be familiar with tools to address these concepts in various places. (Btw, this talk also gives a valuable instrument for implementing event sourcing.)

***PF.tv***: If functional programming were a superhero, what superpower would it have?

***AZ***: I think that immutability might be more primal concept than FP itself. When your data is immutable there is literally no other way to perform calculations except applying a function. So functional programming actually mathematically follows from the immutability idea.  