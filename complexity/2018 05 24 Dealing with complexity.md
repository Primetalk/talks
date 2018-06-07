Title: Dealing with software complexity

Goal: In more or less simple words, without formulas, using manager's language, explain that DSL is a good thing for complex problems.

Publish in Linked In.

Image: https://www.maxpixel.net/Complexity-Fractal-Geometric-3d-Dimensional-1722098

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

Complex problems arise in various business areas. In fact, new business opportunities arise in those spheres that previously were not considered as options due to their extraordinary complexity. To name a few: genome sequencing, formal blockchain-based smart contracts, understanding and interpreting laws and legal acts. The programming side of the business becomes rather diverse and unsurprisingly complex. Ignoring the complexity growth threatens the business with unmanageable and hard-to-maintain codebase, ultimately endangering business existence. Given the need to  keep the codebase up with the everchanging landscape we can say that the destiny of business buried under unmanaged complexity is unenviable.

Why are program systems so complex? The fact that business conquers complex domains seems to play the key role in the complexity increase. The bigger problem we are addressing, the higher precision of our solutions, the more people working on the system, the higher variability of input data - the more complex program is required. And if there is an increasing demand on all directions, then a super complex program is needed.

So, what's the problem? Why dealing with super complex programs could present a problem? Unmanaged complexity quickly makes manager's life a nightmare: unexpected bugs, features that require too much effort to implement, data corruption revealed too late, extra costs for cloud resources, security breaches. And given all that fixing and repairing is slower than software erosion. Only heroic effort can temporary delay the disaster.

How can we turn it into a managed complexity? With predictable feature readiness deadlines, prompt discovery of data problems, reasonable resources cost? This means that we want both run a super complex software and have it under our full managerial control. Is this at all possible?

We know for more than 30 years that there is no silver bullet that could resolve all our problems. What is known so far is that there is a single mechanism invented by humankind to reduce complexity. Abstraction. It may take various forms like reusable libraries, reusable cloud infrastructure, higher-kinded types, domain specific languages. All these are just different forms of abstraction that help humans to deal with super complex software.

Let's take a closer look at how it reduces complexity. First of all, when we have a complex system we can find a stable reusable part and extract it as a component. Then the component is concealed behind some API and thoroughly tested in order to get a reliable building block. After replacing all instances with the new brick we significantly reduce the complexity of the system, because now we can deal with a single well-defined component instead of it's lower level implementation. If we repeat this process continuously, we'll build a robust system that solves the same problem but is less complex at the top level. We should be careful when extracting components and make sure that the API is well thought and the component behaves as prescribed. Otherwise we'll have abstraction leak and accidental complexity increase (and no profit in terms of addressing the overall complexity).

Among various ways to perform abstractions we want to emphasize one that is very powerful and allows to not only significantly decrease complexity, but also to slow down further complexity increase. It's selecting a better basis for the program - the language and the set of libraries we are using for implementing the system. A language provides a handpicked collection of abstractions that match each other and work together very well. A coherent set of libraries unleashes the full potential of the language and provides the actual components that do the job. So instead of abstracting one component after another, we obtain a high level toolset of well defined, tested and composable components. The diversity of components might be called the "basis vocabulary" because in order to use it one has to learn all the "words" in the vocabulary. (Another similar abstraction mechanism is to use a domain specific framework within the existing programming language. This has some limitations and there is often abstraction leak that does not allow for maximum simplification.)

When we select a different (better) programming language, we have to admit that it contains new abstractions, new notions that require learning and consequently knowledge management. One prominent example is switching from Java to Scala. Scala is not only a language equipped with higher level abstractions, it's also a language that has a few flavors or layers of depth. (For example, Martin Odersky [discriminates](https://www.scala-lang.org/old/node/8610)  a few language levels - A1, A2, A3, L1, L2, L3. Each level has it's own knowledge and skill set.) So when switching to Scala, one might have to precisely specify the required depth of knowledge or the language level.

If a team has already moved to a better language and the complexity is still high, then the next possible big step is to have a dedicated DSL that operates domain specific abstractions rather than general purpose abstractions in the base language. Due to the uniqueness nature of hi-end business, this kind of DSL often does not exist in advance so it needs to be designed and implemented. Does it worth it? Due to the promise of significant complexity decrease it seems to be inevitable at certain level of business maturity and problem complexity. Other abstraction ways like extracting components one by one might be too slow and might not allow the company to break through.

A good example of an emerged language and a set of components is HashiCorp Terraform. The Terraform script provides the base language with simple syntax that allows to declare and connect components, while Terraform module registry defines the vocabulary with all ready-made components. As a result there is no abstraction leak from lower level. Due to significant simplification of infrastructure management, it becomes the basis for creating even more complex systems atop of it.

In conclusion, modern business challenges include dealing with super complex programs. Today there exists only one instrument to take complexity under control. It's abstraction (in various forms). And one of the best ways to address complexity is to select a better language (or build a new one as the next step). A better language brings a coherent handpicked collection of abstractions that have synergetic effect. New abstractions of course require learning and knowledge management. In return business gets more simple system built on higher level abstractions which is more manageable.

