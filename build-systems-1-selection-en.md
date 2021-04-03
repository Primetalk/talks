Tags: continuous deployment, разработка ПО, CI/CD, DevOps, системы сборки, gradle
PAA: ??? 
Labels:  gradle, kotlin

# Building projects (CI/CD)

In some projects, the build script is playing the role of Cinderella. The team
focuses its main effort on code development. And the build process itself could be handled by people
who are far from development (for example, those responsible for operation or deployment).
If the build script works somehow, then everyone prefers not to touch it, and
noone ever is thinking about optimization.
However, in large heterogeneous projects, the build process could be quite complex,
and it is possible to approach it as an independent project.
If you treat the build script as a secondary unimportant project,
then the result will be an indigestible imperative script, the support of which
will be rather difficult.

In this note we will take look at the criteria by which we chose the toolkit, and
in [the next one](build-systems-2-gradle-howto-en.md) - how we use this toolkit.

[![CI/CD (opensource.com)](https://opensource.com/sites/default/files/uploads/devops_pipeline_pipe-2.png)](https://opensource.com/article/19/7/cicd-pipeline-rule-them-all)

## General model of project assembly

Project build model in all considered tools
is a ([DAG](https://en.wikipedia.org/wiki/Directed_acyclic_graph)), and not a structural approach
(when a procedure calls other procedures and then uses the results).
This is due to the fact that during the development of the project, frequently minor changes are made 
and most no assembly operations required. That is, the organization of the project in the form of a digraph is
the basis for performing only those actions
which are necessary for the immediate task, thereby frequently used operations
will execute promptly.

The nodes in the graph are either goals or tasks. Goals are the results to be achieved,
and tasks are operations that need to be performed in order to achieve the current goal.
A task can be executed only when all dependencies are satisfied.

Higher-level models are implemented on top of this basic model in some build tools.

## Tool selection

In the beginning of a project, it is sometimes possible to predict how complex the integration 
of the project will be. In our case, it turned out that it was required to build several node.js modules, 
several go-lang modules, and deploy multiple interconnected terraform modules. 
None of the subprojects were JVM-based.

Other similar projects that have been evolving "organically" build is performed using `make`.
In `Makefile`s one could find `bash`, `perl`, `curl`, `php` and other dedicated utilities.
Such build mechanism is error prone, difficult to maintain, and it has rather poor performance.

For a new integration project we decided to try to find an alternative build tool.
The following options were considered:

- make,
- maven,
- sbt,
- gradle/groovy,
- gradle/kotlin.

<cut/>

### Make

This is a mature widespread program and allows implementing build scripts of large and complex projects.

Pros
- competence;
- similarity to shell script;

Cons
- only basic model of goals and tasks is supported; no notion of a project;
- cumbersome syntax;
- global states;
- no plugins support, it's difficult to reuse code;
- no way to describe the build in a declarative way, only imperative.

When there are no constraints, one may find in build scripts:
- code generation from templates using `perl`, 
- automatic installation of executable files by downloading and executing `.sh` scripts from
  Internet on each build run,
- recursive call of make-files for subprojects with surprising target names,
- absence of consistent error handling.

These factors make maintenance more difficult and also make build process an unsafe operation.

### Maven

Maven revolutionized build systems when it was first introduced. Ideas of declarative
descriptions of projects, wide use of conventions, code reuse via plugins,
storing artifacts in repositories, 
the use of an identification system, including versions - all this provided
recognition and widespread use in many JVM projects to this day.

Pros
- competence (part of the team has extensive experience working with Maven projects);
- developed model of projects and subprojects;
- plugin support;
- declarative model;
- maven wrapper.

Cons
- weak support for other technologies;
- the presence of difficult-to-overcome restrictions;
- the complexity of implementing plugins;
- lack of convenient implementation of imperative scripts;
- rigid lifecycle structure;
- not a very convenient XML format.

The lack of imperative scripts is both a plus and a minus. On the one hand,
the declarative approach provides a strict separation of code and model, on the other hand,
build tasks often require some small snippets of imperative logic, and solving such tasks
in maven is painful.

### Sbt

Sbt is a tool for building Scala projects. Appeared around the same time as gradle.

Pros
- developed language;
- support for plugins and imperative snippets;
- incremental build support;
- support for continuous rebuilds as you change any file;
- parallel execution of independent tasks.

Cons
- unexpected model (instead of tasks — "settings");
- implicit dependencies via `. value`;
- weak support for other technologies (go, node.js);
- unexpected syntax.

Sbt looks more like a niche tool for building Scala project rather than a universal tool for
any project.

### Gradle

Gradle appeared in 2007 as a response to the major limitations of Maven — the lack of imperative
code, the difficulty of implementing plugins, and the inconvenience of performing non-standard operations.
Gradle is based on the ideas proposed by Maven, develops them and changes the emphasis.
The main parts of the gradle model are the following:
- task — operation performed, node of the dependency graph, name + description;
- project — logical unit of code organization, set and scope of tasks,
  plug-in connection point;
- plugin — a feature or feature that is added to the project. Among other things — a set of tasks;
- dependencies, up-to-date check.

An important improvement was the use of DSL (domain specific language). The DSL is based on an imperative language, but actually provides the declarative model. Imperative tasks are of course easily solved due to the underlying language.

Pros
- support for project model;
- support for a declarative (model-based) and imperative approach at the same time;
- incremental build support;
- unparalleled flexibility;
- excellent documentation (for two dialects at once);
- very fast operation (even task definitions are performed only when necessary);
- cross-platform — it works everywhere;
- gradle wrapper — a small script to download and run the correct version of gradle;
  developers do not need to manually configure the utilities and update when the version changes in the repository;
- user-friendly and intuitive DSL.

Cons
- lack of competence (gradle has not been widely used by team members before);
- as far as I know, there is no
  mechanism to protect against excessive use of imperative code. You need to be disciplined and
  follow the recommended practices when developing build scripts, and avoid writing spagetti code;
- there is no dependency mechanism other than JVM-based (maven-repository, ivy2);
- the need to put certain effort to ensure that each task supports
  an up-to-date check (useful for incremental builds). In particular, for each task, you need to
  describe the input and output data. Basically, the usual DAG capabilities are available effortlessly,
  but gradle allows you to achieve even higher speed of operations provided
  configuration for inputs and outputs.


#### Dialect selection — gradle/groovy or gradle/kotlin

Gradle was originally used with groovy-DSL. Later Kotlin-based DSL was developed.

Kotlin pros
- compiled strongly-typed language:
  - protection against errors at the compilation stage
  - support for intelli-sense,
  - safe refactoring,
- good DSL support;
- simple syntax, less boilerplate, compared to Java;
- quite a lot of sugar.

Cons
- most of the examples out there are for groovy. Initially it might be difficult to figure out,
  how to rewrite the example in kotlin;
- gradle/groovy DSL was implemented in the first place, so some elements are
  represented in kotlin imperfectly ('extra`, string task names,...);
- the entry barrier is slightly increased due to the need to learn a new language.

## Conclusion

Based on the results of comparison of the available project build tools, we decided
to try to implement the build and CI/CD in our project using gradle/kotlin.
This option has a number of advantages in comparison with the implementation of the project build based on make/shell.

### Gradle/kotlin vs make

Below are the comparative advantages of gradle/kotlin with respect to make:

Advantages:
- the highest speed of operation. The Gradle team puts continuous effort
  to improve the speed and implement features that facilitate
  the implementation of high-speed scripts. It is possible to ensure that all tasks,
  those that are not required for the execution will be skipped. And the tasks that need to be completed are executed only for the changed files.
- unification of the language. All tasks are solved within the framework of one
  strongly-typed compiled language with a consistent and well-thought-out syntax - Kotlin.
  There is no need to study the features of make modes, differences in shell interpreter versions,
  and options for processing command-line parameters in different utilities;
  separate programming languages for templates (php?, perl?).
  Due to the use of a modern language with static
  typing, many classes of errors pertinent to scripting languages are eliminated altogether.
- declarative model of projects/subprojects and plugins on top
  of a declarative digraph of tasks. In make there are only imperative tasks.
- the ability to combine a declarative and imperative approach.
  Despite the fact that the declarative approach provides clarity and purity of code,
  ease of support, ability to combine components, imperative approach
  can be indispensable because of its flexibility. One can create new tasks initially in an imperative way (ad-hoc), and then generalize and abstract away in the form of
  declarative configurable plugins.
- ability to create reusable plugins. Writing such plugins does is not very difficult, 
  there is a convenient API with extensive features for writing plugins.
  In the case of make, there is no standard mechanism, which causes code duplication
  and the re-invention of bicycles.
- a JVM platform on which there exists plenty of platform-independent libraries for all needs.
  In make, some tasks require the installation of platform-specific applications.

Disadvantages:
- it is inconvenient to call shell commands. For each command, you have to create and configure a task. Though it might not be needed much.
- higher requirements for engineering culture - language with static type system, declarative
  project model, using advanced concepts (properties, dependency inference, up-to-date checks).

In the next part, we'll look at some of the features of using gradle/kotlin to build
non-JVM projects.

### Acknowledgements

I would like to thank @nolequen, @Starcounter, @tovarischzhukov
for constructive criticism of the draft article.
