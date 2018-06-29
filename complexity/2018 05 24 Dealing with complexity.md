Title: Dealing with software complexity

Goal: In more or less simple words, without formulas, using manager's language, explain that DSL is a good thing for complex problems.

Publish in Linked In.

Image: https://www.maxpixel.net/Complexity-Fractal-Geometric-3d-Dimensional-1722098

Tags: Dealing with #software #complexity with #abstraction and #dsl

Ideas:
1. Name a few complex problems from business domain. No complex examples!
2. Why is it complex, what makes it complex.
3. Why is it bad to have unmanaged complexity?
- number of bugs ~ LOC - each bug is costly to 
- 
3. Tools for dealing with complexity:
- abstraction (giving names).
- code reuse (which is a form of abstraction),
- modules (which is a form of abstraction),
...
Nothing more so far.
4. Code reuse - plenty of ways invented - functions, objects, modules, types, type-classes, ...
5. Abstraction - key to deal with complexity
"Abstraction layer", "Abstraction leak".
6. One thing that is good for complex programs - DSL.

Great way to decrease complexity - is to select a better language. (Sometimes - implement a DSL)

Ready-made libraries, concise syntax,


Per Brooks the problem is twofold. There is essential complexity that is determined by the problem being solved and an accidental complexity caused by the imperfect instruments.

Modern business rapidly moves to IT sphere.

requirements in the data analytics brings 

Plan:

1. Intro. Complexity is all around us. 
2. Complexity sides. Why, What, Why is it bad,
3. What to do. - Single answer - abstraction.
4. Select a better language. Accidental/essential complexity (ref.Fauler)
5. Conclusion. 
-- [dup]Hence we have to admit that in the modern business we have to deal with super complex programs.




# Dealing with software complexity

Complex problems arise in various business areas. In fact, new business opportunities emerge in the fields that were previously not considered as potentially feasible because of their extraordinary complexity. To name a few of such businesses: genome sequencing, formal blockchain-based smart contracts, automatic understanding and interpretation of laws and legal acts. The programming side of the business becomes rather diverse and unsurprisingly complex. Ignoring the complexity growth results in unmanabeable and hard-to-maintain codebase. And this ultimately endangers business existence. Given the need to  keep the codebase up to date with the ever-changing landscape, we can say that the business is set for failure, buried under the burden of the unmanaged complexity.

Why program systems are so intricate? The fact that business conquers complex domains seems to play the key role in the increase of complexity. The bigger problem we are addressing, the higher precision of our solutions, the more people working on the system, or the higher variability of input data - the more complex program is required. And the complexity just explodes when there is a demand to increase all of the factors, bringing a super complex program to life.

So, what is the problem? Why dealing with super complex programs could present a problem? Unmanaged complexity quickly makes manager's life a nightmare: unexpected bugs, features that require too much effort to implement, data corruptions that are revealed too late, extra costs for cloud resources, security breaches. And given all that, fixing and repairing is slower than the software erosion. Only a heroic effort can temporary delay the ineluctable disaster.

How can we make the complexity manageable? Provide predictable feature readiness deadlines, prompt discovery of data problems, and reasonable resource costs? This means that we want both, running a super complex software and having it under our full managerial control. Is this at all possible?

We have known for more than 30 years that there is no silver bullet that could solve all our problems. What is known so far is that there is a single mechanism invented by humankind to reduce complexity. Abstraction. It may take various forms like reusable libraries, reusable cloud infrastructure, higher kinded types, domain specific languages. All these are just different forms of abstraction that help humans to deal with super complex software.

Let's take a closer look at how abstraction reduces complexity. First of all, when we have a complex system we can find a stable reusable part and extract it as a component. Then the extracted component can be concealed behind some API and thoroughly tested in order to get a reliable building block. Replacing all instances with the new brick significantly reduces the system's complexity, because now we can deal with a single well-defined component instead of it's lower level implementation. And if we repeat this process continuously, we will build a robust system that solves the same problem, but will be less complex at the top level. We should be careful when extracting components and we should make sure that the API is well thought and the component is well tested and behaves as expected. Otherwise we will have an abstraction leak and an accidental increase of complexity (with no gain in reducing the overall complexity).

Among various ways to abstract system parts there is one that we want to emphasize. One that is very powerful and allows not only to significantly decrease complexity, but also to slow down further complexity increase. It is selecting a better basis for the program, and more specifically, the language and the set of libraries to use for implementing the system.

A language provides a handpicked collection of abstractions, with elements that harmonize and work together very well. A coherent set of libraries unleashes the full potential of the language and provides the actual components that do the job. So, instead of abstracting one component after another, we obtain a high level toolset of well defined, tested and composable components. The diversity of components might be called the "basis vocabulary" because in order to use it one has to learn all the "words" in the vocabulary. (Another similar abstraction mechanism is to use a domain specific framework within the existing programming language. This has some limitations and there is often abstraction leak that introduces limitations on further simplifications.)

When we select a more appropriate programming language, we have to admit that the new language contains new concepts and new notions. This means that learning of these notions is required and, consequently, some kind of knowledge management is needed. One prominent example is switching from Java to Scala. Scala is not only a language equipped with higher level abstractions, it is also a language that has a few flavors or layers of depth. (For example, Martin Odersky [discriminates](https://www.scala-lang.org/old/node/8610)  a few language levels - A1, A2, A3, L1, L2, L3. Each level has it's own knowledge and skill set.) So when switching to Scala, one might have to precisely specify the required depth of knowledge or the language level.

If a team has already moved to a better language and the complexity is still high, then the next possible big step is to use a dedicated DSL (Domain Specific Language). DSL operates at the level of domain specific abstractions in contrast to the base language, which operates at the level of general purpose abstractions. Due to the uniqueness nature of business pioneers, this kind of DSL often does not yet exist and it has to be designed and implemented. Does it worth it? Thanks to the promise of significant complexity decrease it seems to be inevitable at certain level of business maturity and problem complexity. Other abstraction ways like extracting components one by one might be too slow and might not allow the company to break through.

A good example of emerged languages with sets of components are scripts used in the Infrastructure as Code approach (like Ansible, Chef, Terraform, etc.). A script provides the base language with simple syntax that allows to declare and connect components, while some module repository defines the vocabulary with supporting components. As a result there is no abstraction leak from lower level. Due to significant simplification of infrastructure management, it becomes the basis for creating even more complex systems atop of it.

In conclusion, I would like to say that modern business challenges include dealing with super complex programs. There is only one instrument today that allows to handle rapidly growing complexity and keep it under control. It is abstraction (which may take various forms). There is a multitude of abstraction mechanisms that are used to address complexity. One that is remarkably powerful is to select a better language or build a new one as the next step. The better language brings in a coherent handpicked collection of abstractions that have synergistic effect. Of course, new abstractions come with a price of a learning curve. Nonetheless in return business gets more benefits in return in the long term by obtaining a simpler system built on higher level abstractions, which is more manageable.
