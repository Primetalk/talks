Разработка программ высокого качества подразумевает, что программа и её части подвергаются тестированию. Классическое модульное (unit) тестирование подразумевает разбиение большой программы на маленькие блоки, удобные для тестов. Либо, если разработка тестов происходит параллельно с разработкой кода или тесты разрабатываются до программы (TDD - test driven development), то программа изначально разрабатыватся небольшими блоками, подходящими под требования тестов.

Одной из разновидностей модульного тестирования можно считать propery-based testing (такой подход реализован, например, в библиотеках [QuickCheck](https://hackage.haskell.org/package/QuickCheck), [ScalaCheck](https://www.scalacheck.org/)). Этот подход основан на нахождении универсальных свойств, которые должны быть справедливы для любых входных данных. Например, _сериализация с последующей десериализацией должна давать такой же объект_. Или, _повторная сортировка не должна менять порядок элементов в списке_. Для проверки таких универсальных свойств в вышеупомянутых библиотеках поддерживается механизм генерации случайных входных данных. Особенно хорошо такой подход работает для программ, основанных на математических законах, которые служат универсальными свойствами, справедливыми для широкого класса программ. Есть даже библиотека готовых математических свойств - [discipline](https://github.com/typelevel/discipline) - позволяющая проверить выполнение этих свойств в новых программах (хороший пример повторного использования тестов).

Иногда оказывается, что необходимо протестировать сложную программу, не имея возможности разобрать её на независимо проверяемые части. В таком случае тестируемая программа представляет собой ~~черный~~ белый ящик (белый - потому что мы имеем возможность изучать внутреннее устройство программы).

Под катом описаны несколько подходов к тестированию сложных программ с одним входом с разной степенью сложности (вовлеченности) и разной степенью покрытия.

<cut />

**В этой статье мы предполагаем, что тестируемую программу можно представить в виде чистой функции без внутреннего состояния. (Некоторые соображения, приведённые далее, можно применять и в том случае, если внутреннее состояние присутствует, но есть возможность сброса этого состояния к фиксированному значению.)*

### Тестовый стенд (test bench)

Прежде всего, так как тестируется всего одна функция, код вызова которой всегда одинаков, то у нас нет необходимости создавать отдельные unit test'ы. Все такие тесты были бы одинаковыми с точностью до входных данных и проверок. Вполне достаточно в цикле передавать исходные данные (`input`) и проверять результаты (`expectedOutput`). Чтобы в случае обнаружения ошибки можно было идентифицировать проблемный набор тестовых данных, все тестовые данные надо снабдить меткой (`label`). Таким образом, один набор тестовых данных можно представить в виде тройки:

```scala
case class TestCase[A, B](label: String, input: A, expectedOutput: B)
```

Результат одного прогона можно представить в виде `TestCaseResult`:
```scala
case class TestCaseResult[A, B](testCase: TestCase[A, B], actualOutput: Try[B])
```
(Результат запуска мы представляем с помощью `Try`, чтобы захватить возможные исключения.)

Чтобы упростить прогон всех тестовых данных через тестируемую программу, можно использовать вспомогательную функцию, которая будет вызывать программу для каждого входного значения:

```scala
def runTestCases[A, B](cases: Seq[TestCase[A, B])(f: A => B): Seq[TestCaseResult[A, B]] = 
  cases
    .map{ testCase => 
      TestCaseResult(testCase, 
        Try{ f(testCase.input) }
      ) 
    }
    .filter(r => r.actualOutput != Success(r.testCase.expectedOutput))
```

Эта вспомогательная функция вернёт проблемные данные и результаты, которые отличаются от ожидаемых.

Для удобства можно отформатировать результаты тестирования
```scala
def report(results: Seq[TestCaseResult[_, _]]): String = 
  s"Failed ${results.length}:\n" + 
    results
      .map(r => r.testCase.label + ": expected " + r.testCase.expectedOutput + ", but got " + r.actualOutput)
      .mkString("\n")
```

и выводить отчёт только в случае ошибок:
```scala
val testCases = Seq(
  TestCase("1", 0, 0)
)

test("all test cases"){
  val testBench = runTestCases(testCases) _
  val results = testBench(f)
  assert(results.isEmpty, report(results))
}
```

### Подготовка входных данных

В простейшем случае можно вручную создать тестовые данные для проверки программы, записать их напрямую в тестовом коде, и использовать, как продемонстрировано выше. Часто оказывается, что интересные случаи тестовых данных имеют много общего и могут быть представлены как некоторый базовый экземпляр, с небольшими изменениями.

```scala
val baseline = MyObject(...) // входной объект можно создать вручную или сгенерировать
val testCases = Seq(
  TestCase("baseline", baseline, ???),
  TestCase("baseline + (field1 = 123)", baseline.copy(field1 = "123"), ???)
)
```

При работе со вложенными неизменяемыми структурами данных большим подспорьем являются линзы, например, из библиотеки [Monocle](http://julien-truffaut.github.io/Monocle/):

```scala
val baseline = ??? 
val testObject1 = (field1 composeLens field2).set("123")(baseline)
// что эквивалентно следующей строке:
val testObject1 = baseline.copy(field1 = baseline.field1.copy(field2 = "123"))
```

Линзы позволяют элегантно "модифицировать" глубоко вложенные части структур данных: Каждая линза представляет собой getter и setter для одного свойства. Линзы можно соединять и получать линзы, "фокусирующиеся" на следующем уровне.

### Использование DSL для представления изменений

Далее будем рассматривать формирование тестовых данных путём внесения изменений в некоторый исходный входной объект. Обычно для получения нужного нам тестового объекта требуется внести несколько изменений. При этом весьма полезно в текстовое описание TestCase'а включить перечень изменений:

```scala
val testCases = Seq(
  TestCase("baseline", baseline, ???),
  TestCase("baseline + " + 
              "(field1 = 123) + " +  // описание 1-го изменения
              "(field2 = 456) + " +  // 2-го
              "(field3 = 789)",      // 3-го
           baseline
             .copy(field1 = "123")   // 1-е изменение
             .copy(field2 = "456")   // 2-е изменение
             .copy(field3 = "789"),  // 3-е изменение
           ???)
)
```
Тогда мы всегда будем знать, для каких тестовых данных выполняется тестирование.

Чтобы текстовый перечень изменений не расходился с фактическими изменениями, необходимо следовать принципу "единой версии правды". (Если одна и та же информация требуется/используется в нескольких точках, то следует иметь единственный первичный источник уникальной информации, а во все остальные точки использования информация должны распространяться автоматически, с необходимыми преобразованиями. Если этот принцип нарушать, и копировать информацию вручную, то неизбежно расхождение версий информации в разных точках. То есть в описании тестовых данных мы увидем одно, а в тестовые данных - другое. Например, копируя изменение `field2 = "456"` и корректируя его в `field3 = "789"` мы можем случайно забыть исправить описание. В итоге описание будет отражать только два изменения из трёх.)

В нашем случае первичным источником информации являются сами изменения, вернее, исходный код программы, которая вносит изменения. Нам хотелось бы вывести из них текст, описывающий изменения. Навскидку, в качестве первого варианта, можно предложить использовать макрос, который будет захватывать исходный код изменений, и использовать исходный код в качестве документации. Это, по-видимому, хороший и относительно несложный способ задокументировать фактические изменения и он вполне может применяться в некоторых случаях. К сожалению, если мы представляем изменения в виде простого текста, мы теряем возможность выполнять осмысленные трансформации перечня изменений. Например, обнаруживать и устранять дублирующиеся или перекрывающиеся изменения, оформлять перечень изменений удобным для конечного пользователя способом.

Чтобы иметь возможность оперировать изменениями, необходимо иметь их структурированную модель. Модель должна быть достаточно выразительной, чтобы описывать все интересующие нас изменения. Частью этой модели, например, будет адресация полей объектов, константы, операции присваивания.

Модель изменений должна позволять решать следующие задачи:
1. Порождение экземпляров модели изменений. (То есть фактически создание конкретного списка изменений.)
2. Формирование текстового описания изменений.
3. Применение изменений к объектам предметной области.
4. Выполнение оптимизационных преобразований над моделью.

Если для внесения изменений будет использоваться универсальный язык программирования, то могут возникнуть затруднения с тем, чтобы представить эти изменения в модели. В исходном тексте программы могут использоваться сложные конструкции, которые не поддерживаются моделью. Такая программа для изменения полей объекта может использовать вторичные паттерны, наподобие линз или метода `copy`, которые являются более низкоуровневыми абстракциями по отношению к уровню модели изменений. В результате, для вывода экземпляров изменений может потребоваться дополнительный анализ таких паттернов. Тем самым, изначально неплохой вариант с использованием макроса, оказывается не очень удобным.

Другим способом формирования экземпляров модели изменений может служить специализированный язык (DSL), создающий объекты моделей изменения с помощью набора extension-методов и вспомогательных операторов. Ну а в простейших случаях экземпляры модели изменений можно создавать непосредственно, через конструкторы.

<spoiler title="Подробности языка изменений">
Язык изменений представляет собой довольно сложную конструкцию, включающую несколько компонентов, которые также, в свою очередь, нетривиальны.

1. Модель структуры данных. 
2. Модель изменений.
3. Собственно Embedded(?) DSL - вспомогательные конструкции, extension-методы, для удобного конструирования изменений.
4. Интерпретатор изменений, позволяющий фактически "модифицировать" объект (на самом деле, естественно, создать изменённую копию).

Приведём пример программы, записанной с использованием DSL:

```scala
val target: Entity[Target] // объект, в который вносятся изменения
val updateField1 = target \ field1 := "123"
val updateField2 = target \ subobject \ field2 := "456"
// или, без использования DSL:
val updateField1 = SetProperty(PropertyAccess(target, Property(field1, typeTag[String])), LiftedString("123"))
val updateField2 = SetProperty(PropertyAccess(PropertyAccess(target, 
                    Property(subobject, typeTag[SubObject])), 
                      Property(field2, typeTag[String])), LiftedString("456"))
```
То есть с помощью extension-методов `\` и `:=` формируются объекты `PropertyAccess`, `SetProperty` из ранее созданных объектов `target`, `field1`, `subobject`, `field2`. Также за счёт (опасных) implicit-конвертаций строка "123" упаковывается в `LiftedString` (можно обойтись без implicit-конвертаций и вызвать соответствующий метод явно: `lift("123")`).

В качестве модели данных может быть использована типизированная онтология (см. https://habr.com/post/229035/ и https://habr.com/post/222553/). (Вкратце: объявляются объекты-имена, представляющие свойства какого-либо типа предметной области: `val field1: Property[Target, String]`.) При этом собственно данные могут храниться, например, в виде JSON. Удобство типизированной онтологии в нашем случае заключается в том, что модель изменений обычно оперирует отдельными свойствами объектов, а онтология как раз даёт подходящий инструмент для адресации свойств.

Для представления изменений необходим набор классов того же плана, что и вышеприведённый класс `SetProperty`:
- `Modify` - применение функции,
- `Changes` - применение нескольких изменений последовательно,
- `ForEach` - применение изменений к каждому элементу коллекции,
- и т.д.

Интерпретатор языка изменений представляет собой обычный рекурсивный вычислитель выражений, основанный на PatternMatching'е. Что-то наподобие:

```scala
def eval(expression: DslExpression, gamma: Map[String, Any]): Any = expression match {
  case LiftedString(str) =>
    str
  case PropertyAccess(obj, prop) =>
    Getter(prop)(gamma).get(obj)
}
def change[T] (expression: DslChangeExpression, gamma: Map[String, Any], target: T): T = expression match {
  case SetProperty(path, valueExpr) =>
    val value = eval(valueExpr, gamma)
    Setter(path)(gamma).set(value)(target)
}
```

Для непосредственного оперирования свойствами объектов необходимо для каждого свойства, используемого в модели изменений, задать getter и setter. Этого можно достичь, заполнив отображение (`Map`) между онтологическими свойствами и соответствующими им линзами.
</spoiler>

Такой подход в целом работает, и действительно позволяет описывать изменения один раз, однако постепенно появляется потребность представлять всё более сложные изменения и модель изменений несколько разрастается. Например, если необходимо изменить какое-то свойство с использованием значения другого свойства того же объекта (например, `field1 = field2 + 1`), то возникает необходимость в поддержки переменных на уровне DSL. А если изменение свойства нетривиально, то на уровне DSL потребуется поддержка арифметических выражений и функций.

### Тестирование ветвей

Тестируемый код может быть линейным, и тогда нам по большому счёту достаточно одного набора тестовых данных, чтобы понять, работает ли он. В случае наличия ветвления (`if-then-else`), необходимо запускать белый ящик как минимум дважды с разными входными данными, чтобы были исполнены обе ветки. Количество наборов входных данных, достаточных для покрытия всех ветвей, по-видимому, численно равно цикломатической сложности кода с ветвлениями.

Как сформировать все наборы входных данных? Так как мы имеем дело с белым ящиком, то мы можем вычленить условия ветвления и дважды модифицировать входной объект так, чтобы в одном случае выполнялась одна ветвь, в другом случае - другая. Рассмотрим пример:
```scala
if (object.field1 == "123") A else B
```
Имея такое условие, мы можем сформировать два тестовых случая:
```scala
val testCase1 = TestCase("A", field1.set("123")(baseline), /* result of A */)
val testCase2 = TestCase("B", field1.set(/* не "123", а, например, */"123" + "1">)(baseline), /*result of B*/)
```
*(В случае, если один из тестовых сценариев невозможно создать, то можно считать, что обнаружен мертвый код, и условие вместе с соответствующей веткой можно спокойно удалить.)*

Если в нескольких ветвлениях проверяются независимые свойства объекта, то можно довольно просто сформировать исчерпывающий набор измененных тестовых объектов, который полностью покрывает все возможные комбинации.

<spoiler title="DSL для формирования всех комбинаций изменений">
Рассмотрим подробнее механизм, позволяющий сформировать все возможные перечни изменений, обеспечивающие полное покрытие всех ветвлений. Для того, чтобы использовать перечень изменений при тестировании, нам надо все изменения объединить в один объект, который мы подадим на вход тестируемого кода, то есть требуется поддержка композиции. Для этого можно либо воспользоваться вышеприведённым DSL для моделирования изменений, и тогда достаточно простого списка изменений, либо представить одно изменение в виде функции модификации `T => T`:

```scala
val change1: T => T = field1.set("123")(_)
// val change1: T => T = _.copy(field1 = "123")
val change2: T => T = field2.set("456")
```
тогда цепочка изменений будет представлять собой просто композицию функций:
```scala
val changes = change1 compose change2
```
или, для списка изменений:
```scala
val rawChangesList: Seq[T => T] = Seq(change1, change2)
val allChanges: T => T = rawChangesList.foldLeft(identity)(_ compose _)
```

Чтобы компактно записать все изменения, соответствующие всем возможным ветвлениям, можно использовать DSL следующего уровня абстракции, моделирующий структуру тестируемого белого ящика:

```scala
val tests: Seq[(String, T => T)] =
           IF("field1 == '123'")  // название условия, которое мы моделируем
           THEN( field1.set("123"))( // или target \ field1 := "123"
             IF("field2 == '456')
             THEN(field2.set("456"))(TERMINATE)
             ELSE(field2.set("456" + "1"))(TERMINATE)
           )
           ELSE( field1.set("123" + "1") )(TERMINATE)
```
Здесь коллекция `tests` содержит агрегированные изменения, соответствующие всем возможным комбинациям ветвей. Параметр типа `String` будет содержать все названия условий и все описания изменений, из которых сформирована агрегированная функция изменений. А второй элемент пары типа `T => T` - как раз агрегированная функция изменений, полученная в результате композиции отдельных изменений.

Чтобы получить изменённые объекты, надо все агрегированные функции изменений применить к baseline-объекту:
```scala
val tests2: Seq[(String, T)] = tests.map(_.map_2(_(baseline)))
```
В результате мы получим коллекцию пар, причем строка будет описывать применённые изменения, а второй элемент пары будет объектом, в котором все эти изменения объединены.

Исходя из структуры модели тестируемого кода в форме дерева, перечни изменений будут представлять собой пути от корня к листам этого дерева. Тем самым значительная часть изменений будет дублироваться. Можно избавиться от этого дублирования, используя вариант DSL, при котором изменения непосредственно применяются к baseline-объекту по мере продвижения по ветвям. В этом случае будет производиться несколько меньше лишних вычислений.
</spoiler>

### Автоматическое формирование тестовых данных

Так как мы имеем дело с белым ящиком, то можем видеть все ветвления. Это даёт возможность построения модели логики, содержащейся в белом ящике, и использования модели для генерации тестовых данных. В случае, если тестируемый код написан на Scala, можно, например, использовать [scalameta](https://scalameta.org/) для чтения кода, с последующем преобразованием в модель логики. Опять же, как и в рассмотренном ранее вопросе моделирования логики изменений, для нас затруднительно моделирование всех возможностей универсального языка. Далее будем предполагать, что тестируемый код реализован с использованием ограниченного подмножества языка, либо на другом языке или DSL, который изначально ограничен. Это позволяет сосредоточиться на тех аспектах языка, которые представляют для нас интерес.

Рассмотрим пример кода, содержащего единственное ветвление:
```scala
if(object.field1 == "123") A else B
```
Условие разбивает множество значений поля `field1` на два класса эквивалентности: `== "123"` и `!= "123"`. Тем самым, всё множество входных данных также разбивается на два класса эквивалентности по отношению к этому условию - `ClassCondition1IsTrue` и `ClassCondition1IsFalse`. С точки зрения полноты покрытия нам достаточно взять хотя бы по одному примеру из этих двух классов, чтобы покрыть обе ветви `A` и `B`. Для первого класса мы можем построить пример, в каком-то смысле, единственным образом: взять случайный объект, но изменить поле `field1` на `"123"`. При этом объект обязательно окажется в классе эквивалентности `ClassCondition1IsTrue` и вычисления пойдут по ветви `A`. Для второго класса примеров больше. Один из способов генерации какого-то примера второго класса: генерировать  произвольные входные объекты и отбрасывать такие, у которых `field1 == "123"`. Ещё один способ: взять случайный объект, но изменить поле `field1` на `"123" + "*"` (для модификации можно использовать любое изменение контрольной строки, гарантирующее, что новая строка не равна контрольной). 

В качестве генераторов случайных данных вполне подходит [механизм генераторов `Arbitrary` и `Gen` из библиотеки ScalaCheck](https://github.com/rickynils/scalacheck/blob/master/doc/UserGuide.md#generators).

По-сути, мы выполняем **обращение** булевой функции, используемой в операторе `if`. То есть находим все значения входного объекта, для которых эта булева функция принимает значение `true` - `ClassCondition1IsTrue`, и все значения входного объекта, для которых она принимает значение `false` - `ClassCondition1IsFalse`.

Подобным образом можно генерировать данные, подходящие под ограничения, порождаемые простыми условными операторами с константами (больше/меньше константы, входит во множество, начинается с константы). Такие условия нетрудно обратить. Даже если в тестируемом коде вызываются несложные функции, то мы можем заменить их вызов на их определение (inline) и всё-таки осуществить обращение условных выражений.

#### Трудно обратимые функции

Иначе обстоит дело в том случае, когда в условии используется функция, которую затруднительно обратить. Например, если используется хэш-функция, то автоматически генерировать пример, дающий требуемое значение хэш-кода, по-видимому, не получится. 

В таком случае можно добавить во входной объект дополнительный параметр, представляющий результат вычисления функции, заменить вызов функции на обращение к этому параметру, и обновлять этот параметр, невзирая на нарушение функциональной связи:
```scala
if(sha(object.field1)=="a9403...") ...
// после введения дополнительного параметра ==> 
if(object.sha_field1 == "a9403...") ...
```
Дополнительный параметр позволяет обеспечить выполнение кода внутри ветки, но, очевидно, может привести к фактически некорректным результатам. То есть тестируемая программа будет выдавать результаты, которые никогда не могут наблюдаться в реальности. Тем не менее, проверка части кода, которая иначе нам недоступна, всё равно полезна и может рассматриваться как разновидность модульного тестирования. Ведь и при модульном тестировании подфункция вызывается с такими аргументами, которые, возможно, никогда не будут использоваться в программе.

При таких манипуляциях мы заменяем (подменяем) объект тестирования. Тем не менее, в каком-то смысле новая построенная программа включает логику прежней программы. Действительно, если в качестве значений новых искуственных параметров взять результаты вычисления функций, которые мы заменили на параметры, то программа выдаст те же самые результаты. По-видимому, тестирование изменённой программы по-прежнему может представлять интерес. Надо лишь помнить, при каких условиях изменённая программа будет вести себя также, как исходная.

#### Зависимые условия

Если ветвления в коде опираются на независимые поля объекта, то задача полного покрытия решается прямолинейно. Достаточно записать все подмножества для каждого тестируемого поля, а затем, используя подходящие генераторы, сгенерировать новые значения для всех полей. Если же последующие условия уточняют множество значений поля, то необходимо брать пересечение подмножеств. (Например, первое условие, `x > 0`, а следующее  - `x <= 1`. Каждое из условий могло бы дать по два подмножества, но за счёт пересечений в итоге получится только три подмножества - `(-∞, 0]`, `(0, 1]`, `(1, +∞)`, - примеры из которых надо будет сгенерировать.)

Если при формировании уточняющих множеств мы обнаружим, что одно из подмножеств пусто, то это означает, что условие всегда будет принимать фиксированное значение `true` или `false` вне зависимости от входных значений. Поэтому соответствующая ветка, которая никогда не вызывается, является "мертвым кодом" и может быть удалена из кода вместе с условием.

#### Связанные параметры

Рассмотрим случай, когда условие ветвления основано на двух полях объекта, также связанных условиями:
```scala
if(x > 0)
  if(y > 0)
   if (y > x)
```
(Здесь каждое из полей ограничено `> 0`, и оба поля совместно тоже ограничены - `y > x`.)
Если условие является "свободным", как, в этом примере, то достаточно сгенерировать примеры значений полей, проверить выполнение всех условий, и отбросить неподходящие значения свойств. Так как область допустимых значений достаточно велика по сравнению с областью неподходящих значений, то в большом числе случаев мы будем генерировать подходящие комбинации свойств и "коэффициент полезного действия" такого метода будет достаточно большим.
В случае, если условие "жёсткое", функциональное (`y == x + 1`), то можно построить функцию, вычисляющую значение второго поля на основе сгенерированного первого.
Если условие "полужесткое" (`y > x + 1 && y < x + 2`), то можно построить зависимый генератор, диапазон значений которого определяется сгенерированным значением первого поля.

#### Символьное выполнение

Для того, чтобы собрать все условия, порождающие результат по какой-либо из ветвей, можно воспользоваться "cимвольным выполнением" ([Symbolic Execution](https://en.wikipedia.org/wiki/Symbolic_execution), [Символьное выполнение программ](http://erseal.blogspot.com/2012/02/blog-post_16.html)), суть которого заключается в следующем. Входные данные принимаются равными некоторым символьным значениям (`field1 = field1_initial_value`). Затем над символьными значениями производятся все манипуляции, описанные в тестируемом коде. Все манипуляции выполняются в символьном же виде:
```scala
val a = field1 + 10
// добавляем в контекст a = field_initial_value + 10
val b = a * 3
// добавляем в контекст b = 3 * field_initial_value + 30
```
Для обработки ветвления принимаются возможными оба варианта условия - `true` и `false`. И расчёт производится сразу для двух веток. При этом на символьные значения в каждой ветке накладываются противоположные ограничения. Например,
```scala
if(a > 0) A else B
// в ветке A принимаем, что field_initial_value + 10  > 0
// в ветке B принимаем, что field_initial_value + 10 <= 0
```
Накопленные в символьном виде ограничения можно использовать либо для формирования генератора, порождающего значения, удовлетворяющие этим ограничениям, либо для проверки случайных значений, формируемых менее точным генератором. В любом случае появляется возможность генерировать случайные данные, приводящие к исполнению заранее известного пути (и, возможно, к известному результату).

#### Тестирование циклов и рекурсивных функций

До сих пор мы обходили вопрос циклов стороной. Связано это с тем, что в цикле меняется состояние, то есть цикл обязательно использует изменяемую переменную. Мы же обозначили границы нашего рассмотрения чистыми функциями, что подразумевает использование только неизменяемых структур данных. Также при наличии циклов существует риск формирования таких условий, при которых результат не будет получен за разумное время. 

Известно, что любой цикл можно заменить рекурсией. Это может быть непросто для сложных циклов. Но, допустим, что в нашем случае такая операция была произведена. Тем самым, в тестируемом коде будут встречаться рекурсивные вызовы, а мы можем продолжать наши рассуждения, сохраняя исходное предположение о рассмотрении только чистых функций. Как мы могли бы протестировать такой белый ящик, учитывая тот факт, что рекурсивные вызовы, так же, как и циклы, могут не завершиться за разумное время?

Воспользуемся такой конструкцией как Y-комбинатор (["комбинатор неподвижной точки"](https://ru.wikipedia.org/wiki/Комбинатор_неподвижной_точки), [stackoverflow:What is a Y-combinator? (2-ой ответ)](https://stackoverflow.com/questions/93526/what-is-a-y-combinator), [habr:Получение Y-комбинатора в 7 простых шагов](https://habr.com/post/118927/)). Комбинатор позволяет реализовать рекурсию в языках, которые рекурсию в чистом виде не поддерживают. (Сам комбинатор является рекурсивным, поэтому должен быть реализован на языке, поддерживающем рекурсию.) Работает он следующим образом. Из рекурсивной функции удаляются все рекурсивные вызовы и заменяются на вызовы функции, которая передаётся в качестве дополнительного аргумента. Такая переработанная функция уже не будет являться рекурсивной, а служит только "заготовкой" для получения целевой функции. Y-комбинатор превращает такую "заготовку рекурсивной функции" в полноценную рекурсивную функцию (передавая в качестве аргумента собственное продолжение).

Рассмотрим вначале важный частный случай хвостовой рекурсии ([Хвостовая рекурсия](https://ru.wikipedia.org/wiki/Хвостовая_рекурсия)). Перепишем тестируемый код, заменив рекурсивные вызовы на вызовы вспомогательной функции. Для целей тестирования мы передадим собственную реализацию вспомогательной функции, которая не будет формировать рекурсию. Такая вспомогательная функция может формировать возвращаемый результат, соответствующий типу возвращаемых значений белого ящика. Например, если возвращаются строки, то вспомогательная функция также будет формировать строку, которую мы сможем проверить в рамках `TestCase`'а. В случае, если тип возвращаемых значений нам окажется неудобен, мы можем выбрасывать исключение (`throw` имеет тип `Nothing` или `bottom`, являющийся подтипом всех остальных). Тем самым мы получим возможность тестирования кода без циклов и риска зависания.

В случае общей рекурсии рекурсивный вызов возвращает результат, который затем используется. В этом случае вышеприведённый подход напрямую не работает. Можно попробовать применить подход, аналогичный тому, что мы использовали для вызовов трудно обратимых функций. А именно, заменим каждый рекурсивный вызов на новый параметр. Значение этих параметров можно будет генерировать как обычно, исходя из условий ветвлений, в которых эти параметры используются. Как и в случае с заменой вызовов функций на параметры, результаты, которые мы будем получать, могут отличаться от результатов, которые мы можем получить в действительности. Совпадение будет достигаться в том случае, если значение параметра совпадает со значением рекурсивной функции. Такой подход позволяет нам протестировать шаги, выполняемые после рекурсивного вызова.

### Смысл тестирования белого ящика

При определённом усердии можно добиться того, что тесты, написанные вручную или сгенерированные автоматически, будут покрывать все ветви тестируемого кода, то есть обеспечат 100% покрытие. Тем самым мы сможем с уверенностью сказать, что белый ящик делает то, что он делает. Хм. Секундочку. А в чём, собственно, смысл такого тестирования, спросит внимательный читатель? Ведь для любого содержимого белого ящика будут построены тесты, которые только лишь подтверждают, что белый ящик работает каким-то определённым образом.

В некоторых случаях такой набор тестов всё же может иметь смысл:
1. Если белый ящик является прототипом и существует ненулевая вероятность ошибки для действительной реализации в другой среде.
2. Если существует несколько реализаций одинаковой логики (на разных языках или в разных окружениях).
3. Код белого ящика эволюционирует или подвергается рефакторингу, при этом мы хотели бы обнаруживать отличия от эталонного поведения.
4. Мы хотим обнаружить подмножества входных данных, обеспечивающие требуемые нам результаты.

Следует иметь в виду некоторые особенности тестирования, основанного на реализации, в отличие от тестирования на основе спецификации. Во-первых, если изначальная реализация не поддерживала некоторую функциональность, которую можно было бы ожидать, основываясь на спецификации, то наши тесты не заметят её отсутствия. Во-вторых, если такая функциональность присутствовала, но работала иначе, чем указано в спецификации (то есть, с ошибками), то наши тесты не просто этих ошибок не обнаружат, а напротив, ошибки будут "кодифицированы" в тестах. И если последующие/альтернативные реализации попробуют исправить ошибки, то такие тесты не позволят этого просто так сделать.

### Заключение

Тестирование белого ящика смещает акцент с вопроса "что должен делать код" на "что фактически делает код". Иными словами, вместо использования более высокого уровня абстракции, формирования тестов на основе спецификации, используется точно тот же уровень абстракции, что и при реализации кода. Мы можем получить хорошие результаты в плане покрытия кода, но при этом такое тестирование имеет смысл в ограниченном наборе случаев.

Если вы столкнулись с таким случаем, в котором тестирование белого ящика оправдано, то соображения, приведённые выше, могут пригодиться. Во-первых, основные усилия имеет смысл сосредоточить на формировании тестовых наборов данных, так как вход у белого ящика один (вызов функции), а протестировать хотелось бы все ветви. Во-вторых, по-видимому, имеет смысл построить модель тестируемого кода. Для этого может использоваться специализированный DSL, достаточно выразительный, чтобы представлять тестируемую логику. В-третьих, пользуясь моделью тестируемой логики можно попробовать автоматически сформировать тестовые данные, покрывающие все ветви. В-четвертых, тестируемый код может быть подвергнут автоматическим преобразованиям, которые делают его более удобным для тестирования (исключение вызовов труднообратимых функций, переход от циклов к рекурсии, исключение рекурсивных вызовов). При использовании этих подходов можно получить хорошие результаты в плане покрытия кода.

Таким образом, в благоприятных условиях и при реализации некоторых из вышеприведённых подходов, появляется возможность автоматической генерации содержательных тестов. Возможно, заинтересованные читатели предложат и другие области, где могло бы применяться тестирование белого ящика или какие-либо из рассмотренных подходов.

### Благодарности

Хотелось бы поблагодарить [@mneychev](https://github.com/mneychev/) за терпение и неоценимую помощь при подготовке статьи.

