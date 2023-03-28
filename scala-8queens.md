Tags: 
PAA: ??? 
КДПВ
Labels: 
Scala
Ненормальное программирование
Функциональное программирование
Совершенный код
Программирование

# Решение задачи о 8 ферзях на трёх уровнях Scala - программа, типы, метапрограмма 

В заметке Ричарда Тауэрса (Richard Towers) [Typescripting the technical interview](https://www.richard-towers.com/2023/03/11/typescripting-the-technical-interview.html) (есть перевод на Хабре: [Руны и лёд: техническое собеседование по TypeScript](https://habr.com/ru/post/723752/)) по ходу повествования была решена классическая задача расстановки 8 ферзей на шахматной доске. Для решения использовалась система типов TypeScript. Мне захотелось посмотреть, как эта задача будет выглядеть на Scala. Т.к. Scala 3 помимо развитой системы типов предлагает превосходную поддержку метапрограммирования, то здесь мы рассмотрим не только решение на типах, но и мета-программное решение.

<cut/>

## Решение на типах

Без необходимости запуска программы компилятор сформирует тип, представляющий решение.
Для удобства этот результирующий тип можно конвертировать в экземпляр, значение которого будет выведено при запуске программы.

### Предварительные объявления

Вслед за [Richard Towers](https://www.richard-towers.com/2023/03/11/typescripting-the-technical-interview.html) введём собственные типы для Boolean:

```scala
object ᛊ
object ᛚ

type True = ᛊ.type
type False = ᛚ.type
```

целых чисел:
```scala
object ᛞ

type Zero = ᛞ.type

type S[n] = (n, Unit)

type One = S[Zero]
type Two = S[One]
type Three = S[Two]
type Four = S[Three]
type Five = S[Four]
type Six = S[Five]
type Seven = S[Six]
type Eight = S[Seven]
```

и списков:

```scala
object ᚾ

type Nil = ᚾ.type
type Cons[x, xs] = (x, xs)
```

Определим операции для Boolean:

```scala
type Not[b1] = b1 match
  case True  => False
  case False => True
  case _     => Nothing

type Or[b1, b2] = b1 match
  case True  => True
  case False => b2
  case _     => Nothing
```

Также потребуется операция ИЛИ для списка Boolean:
```scala
type AnyTrue[list] = list match
  case Cons[x, xs] =>
    x match
      case True => True
      case _    => AnyTrue[xs]
  case _ => False
```

Вычитание (по модулю):
```scala
type AbsDiff[a, b] = (a, b) match
  case (S[a1], S[b1]) =>
    AbsDiff[a1, b1]
  case (S[a1], Zero) =>
    S[a1]
  case (Zero, S[b1]) =>
    S[b1]
  case _ => Nothing
```
и сравнение чисел:
```scala
type Equals[a, b] = (a, b) match
  case (S[a1], S[b1]) =>
    Equals[a1, b1]
  case (Zero, Zero) => True
  case _            => False
```
генерация диапазона чисел:

```scala
type RangeFromZeroTo0[n, xs] = n match 
  case S[n1] => RangeFromZeroTo0[n1, Cons[n, xs]]
  case _ => Cons[Zero, xs]
type RangeFromZeroTo[n] = RangeFromZeroTo0[n, Nil]
```

### Конструирование решения

Объявим "структуру данных", представляющую одного ферзя:
```scala
type Queen[x, y] = (x, y)
```
Начальная расстановка ферзей - все ферзи, находящиеся в колонках `cols`, получают одинаковое значение `row`:
```scala
type RowOfQueens[cols, row] = 
  cols match
    case Cons[col, cols1] => Cons[Queen[row, col], RowOfQueens[cols1, row]]
    case _ => 
      cols
```

Одна королева угрожает другой в одном их трёх случаев - совпадают x, совпадают y, или абсолютная разность по осям одинаковая:
```scala
type Threatens[a, b] = 
  (a, b) match
    case (Queen[ax, ay], Queen[bx, by]) =>
      Or[
        Or[
          Equals[ax, bx], 
          Equals[ay, by],
        ],
        Equals[AbsDiff[ax, bx], AbsDiff[ay, by]]
      ]
    case _ => Nothing
```

Отфильтруем уже размещённых ферзей исходя из того, угрожают ли они заданному ферзю:

```scala
type ThreateningQueens[placedQueens, queen] =
  placedQueens match
    case Cons[placedQueen, placedQueens1] =>
      Cons[
        Threatens[placedQueen, queen],
        ThreateningQueens[placedQueens1, queen]
      ]
    case _ =>
      Nil
```
Ферзь находится "в безопасности", если этот список пуст:
```scala
type Safe[placedQueens, queen] =
  Not[AnyTrue[ThreateningQueens[placedQueens, queen]]]
```

Теперь мы можем отфильтровать уже всех потенциальных кандидатов, чтобы оставить только варианты, безопасные относительно уже размещённых ферзей:
```scala
type FilterSafeQueens[candidates, placedQueens] =
  candidates match
    case Cons[q, qs] =>
      Safe[placedQueens, q] match
        case True =>
          Cons[q, FilterSafeQueens[qs, placedQueens]]
        case _ =>
          FilterSafeQueens[qs, placedQueens]
    case _ => Nil
```
Можно сформировать следующий ряд потенциальных расстановок ферзей:
```scala
type Next[row, placedQueens] =
  FilterSafeQueens[RowOfQueens[RangeFromZeroTo[N], row], placedQueens]
```
(Здесь упомянут размер доски `N`, который будет определён ниже.)
Функция `Next` принимает на вход список уже размещённых ферзей и номер следующего ряда. Возвращает набор ферзей-кандидатов в этом ряду, которые не конфликтуют с уже размещёнными ферзями.

Следующие функции рекурсивно решают задачу поиска решения.
```scala
type SolveNextRow[row, placedQueens] =
    Solve[Next[S[row], placedQueens], S[row], placedQueens]

type Solve[candidates, row, placedQueens] = Equals[row, N] match
  case True =>
    candidates match
      case Cons[x, _] =>
        Cons[x, placedQueens]
      case _ =>
        Nil
  case _ =>
    candidates match
      case Cons[x, xs] =>
        SolveNextRow[row, Cons[x, placedQueens]] match
          case Nil =>
            Solve[xs, row, placedQueens]
          case _ =>
            SolveNextRow[row, Cons[x, placedQueens]]
      case _ =>
        Nil
```
Разберём работу этих функций немного подробнее.

`Solve` принимает на вход список кандидатов `candidates`, которых надо рассмотреть, находящихся в ряду `row`, при том, что уже имеются расставленые ферзи `placedQueens`. В случае, если мы оказались на последнем ряду `Equals[row, N]`, и у нас имеется хоть сколько-то кандидатов, то мы берём из них первого кандидата, приклеиваем к уже размещённым ферзям и возвращаем готовое решение.
Если это ещё не последний вариант, то мы пробегаем каждого из предложенных вариантов, приклеиваем его к размещённым ферзям и продвигаем решение вперёд. Если для какого-то кандидата решения не найдено, то убираем этого кандидата и продолжаем с оставшимися кандидатами. Если же решение найдено, то приклеиваем этого кандидата уже окончательно и снова решаем задачу уже зная, что решение существует.

Теперь осталось подготовить пустое поле и запустить поиск решения:
```scala
type N = Six // Seven 
type Solution = Solve[Next[Zero, Nil], Zero, Nil]
```

### Красивый вывод решения на печать

Решение представляет собой тип списка ферзей. Чтобы отобразить тип в виде обычной строки, воспользуемся набором implicit объектов.в

Во-первых, нам необходимо тип целых чисел уметь конвертировать в число:
```scala
sealed trait ToInt[T]:
  def toInt: Int

given ToInt[Zero] with
  def toInt: Int = 0

given sToInt[T](using t: ToInt[T]): ToInt[S[T]] with
  def toInt: Int = t.toInt + 1
```
Далее нам надо уметь преобразовывать разные типы в строки:

```scala
sealed trait Render[T]:
  def render: String
```
В частности, целые числа:
```scala
given renderInt[T](using t: ToInt[T]): Render[T] with
  def render: String = t.toInt.toString()
```
и ферзей
```scala
given renderQueen[x,y](using x: ToInt[x], y: ToInt[y]):Render[Queen[x,y]] with
  def render: String = s"♕(${x.toInt}, ${y.toInt})"
```
Осталось собрать список:

```scala
sealed trait ToStringList[T]:
  def toStringList: List[String]

given ToStringList[Nil] with
  def toStringList: List[String] = scala.Nil

given consToList[x, xs](using x: Render[x], xs: ToStringList[xs]): ToStringList[Cons[x, xs]] with
  def toStringList: List[String] = x.render :: xs.toStringList
```

И отрисовать его:
```scala
given renderList[xs](using xs: ToStringList[xs]): Render[xs] with
  def render: String = xs.toStringList.mkString("(", ",", ")")
```

### Замечания

В переводе и в оригинале шахматная доска, похоже, имеет размер 7x7. В этом случае решение имеет вид:
```
(♕(6, 5),♕(5, 3),♕(4, 1),♕(3, 6),♕(2, 4),♕(1, 2),♕(0, 0))
```
В случае шахматной доски 8х8:
```
(♕(7, 3),♕(6, 1),♕(5, 6),♕(4, 2),♕(3, 5),♕(2, 7),♕(1, 4),♕(0, 0))
```

## Решение на типах 2

В этом варианте решения мы воспользуемся тем, что Scala 3 предлагает неплохой набор базовых типов, пригодных для вычислений, и ряд синтаксических улучшений, упрощающих работу с типами.

### Предварительные объявления

Нет необходимости создавать свой тип Boolean. Благодаря синглетонным типам символы `true` и `false` можно непосредственно использовать на уровне типов. Пакет `import scala.compiletime.ops.boolean.*` содержит все необходимые операции. Можно только для удобства добавить `If`:
```scala
type If[c <: Boolean, t, f] = c match
  case true  => t
  case false => f
```

В Scala имеется поддержка целых чисел на уровне типов в пакете `import scala.compiletime.ops.int.{S, Abs, -, +}`.

Равенство типов можно проверить с использованием типа `==` - `import scala.compiletime.ops.any.{==}`. 

Список сохраним такой же, как был объявлен ранее, чтобы не добавлять сторонних библиотек. (В библиотеках Scala можно найти, например, `HList`, который можно было бы использовать здесь.):
```scala

object ᚾ

type Nil = ᚾ.type
type ::[x, xs] = (x, xs)
```

Вместо `Cons` мы используем операторное имя `::`, благодаря которому можно красиво и удобно конструировать тип: `x :: xs == ::[x,xs]`.

Добавим несколько вспомогательных операций, которые обыкновенно используются при работе со списками:
```scala
/** Свёртка значений списка list с помощью операции op, и начального значения z.*/
type FoldLeft[z, op[_, _], list] = list match
  case x :: xs => FoldLeft[op[z, x], op, xs]
  case Nil     => z

type FoldLeftBoolean[z <: Boolean, op[
    _ <: Boolean,
    _ <: Boolean
] <: Boolean, list] <: Boolean = list match
  case x :: xs => FoldLeftBoolean[op[z, x], op, xs]
  case Nil     => z

/** Применение функции f к каждому элементу списка list. */
type Map[f[arg], list] = list match
  case x :: xs => f[x] :: Map[f, xs]
  case Nil     => Nil
type MapInt[f[arg<:Int], list] = list match
  case x :: xs => f[x] :: MapInt[f, xs]
  case Nil     => Nil
/** Фильтрация элементов списка. */
type Filter[p[_] <: Boolean, list] = list match
  case x :: xs => If[p[x], x :: Filter[p, xs], Filter[p, xs]]
  case Nil     => Nil

type Exists[p[_] <: Boolean, list] =
  FoldLeftBoolean[false, [previous <: Boolean,
  next] =>> previous || p[next], list]
```
Как видно, объявления функций на типах мало чем отличаются от объявлений обычных функций, работающих со значениями.

С использованием этих возможностей можно немного упростить решение на типах.


### Конструирование решения

Объявим "структуру данных", представляющую одного ферзя:
```scala
type Queen[x <: Int, y <: Int] = (x, y)
```
Здесь мы требуем, чтобы координаты были целочисленными.

Начальная расстановка ферзей - все ферзи, находящиеся в колонках `cols`, получают одинаковое значение `row`:
```scala
type RowOfQueens[cols, row <: Int] =
  MapInt[[col <: Int] =>> Queen[row, col], cols]
```

Одна королева угрожает другой в одном их трёх случаев - совпадают x, совпадают y, или абсолютная разность по осям одинаковая:
```scala
type Threatens[q1, q2] <: Boolean =
  (q1, q2) match
    case (Queen[ax, ay], Queen[bx, by]) =>
      ax == bx || ay == by || Abs[ax - bx] == Abs[ay - by]
```
Любопытно, что выражение на типах выглядит совершенно также, как выражение на значениях.

Ферзь находится "в безопасности", если не существует уже размещённого ферзя, который бы ему угрожал:
```scala
type Safe[placedQueens, queen] =
  ![Exists[[placedQueen] =>> Threatens[placedQueen, queen], placedQueens]]
```

Теперь мы можем отфильтровать уже всех потенциальных кандидатов, чтобы оставить только варианты, безопасные относительно уже размещённых ферзей:
```scala
type FilterSafeQueens[candidates, placedQueens] =
  Filter[[candidate] =>> Safe[placedQueens, candidate], candidates]
```

Можно сформировать следующий ряд потенциальных расстановок ферзей:
```scala
type Next[row <: Int, placedQueens] =
  FilterSafeQueens[RowOfQueens[RangeFromZeroTo[N], row], placedQueens]
```
(Здесь упомянут размер доски `N`, который будет определён ниже.)
Функция `Next` принимает на вход список уже размещённых ферзей и номер следующего ряда. Возвращает набор ферзей-кандидатов в этом ряду, которые не конфликтуют с уже размещёнными ферзями.

Следующие функции рекурсивно решают задачу поиска решения.
```scala
type SolveNextRow[row <: Int, placedQueens] =
  Solve[Next[row + 1, placedQueens], row + 1, placedQueens]

type Solve[candidates, row <: Int, placedQueens] =
  If[
    row == N,
    candidates match
      case x :: _ => x :: placedQueens // return the solution
      case _      => Nil // no solution found
    ,
    candidates match
      case x :: xs =>
        SolveNextRow[row, x :: placedQueens] match
          case Nil => // no solution found with this assumption
            Solve[xs, row, placedQueens]
          case y :: ys => // there is a solution
            y :: ys
      case _ =>
        Nil
  ]

```


Теперь осталось подготовить пустое поле и запустить поиск решения:
```scala
type N = 6 // 7 
type Solution = Solve[Next[0, Nil], 0, Nil]
```
Как и раньше, решение может быть преобразовано в значения с использованием implicit'ов.
```
(♕(6, 5),♕(5, 3),♕(4, 1),♕(3, 6),♕(2, 4),♕(1, 2),♕(0, 0))
(♕(7, 3),♕(6, 1),♕(5, 6),♕(4, 2),♕(3, 5),♕(2, 7),♕(1, 4),♕(0, 0))
```

## Решение времени исполнения

При запуске программы будет выполнена некоторая последовательность шагов, в результате которой будет получено решение.

Программу можно получить путём ряда замен:
- `type` -> `def`
- `[]` -> `()`
- `=>>` -> `=>`
- заменить функции типов на методы коллекций
- для структур данных использовать case class'ы
- добавить аннотации типов и исправить ошибки компиляции.

Вот получившаяся программа:
```scala
def RangeFromZeroTo(n: Int, xs: List[Int] = Nil): List[Int] =
  if n == 0 then 0 :: xs
  else RangeFromZeroTo(n - 1, n :: xs)

case class Queen(x: Int, y: Int)

def RowOfQueens(cols: List[Int], row: Int) =
  cols.map(Queen(row, _))

def Threatens(q1: Queen, q2: Queen): Boolean =
  (q1, q2) match
    case (Queen(ax, ay), Queen(bx, by)) =>
      ax == bx || ay == by || math.abs(ax - bx) == math.abs(ay - by)

def Safe(placedQueens: List[Queen], queen: Queen): Boolean =
  !placedQueens.exists((placedQueen) => Threatens(placedQueen, queen))

def FilterSafeQueens(
    candidates: List[Queen],
    placedQueens: List[Queen]
): List[Queen] =
  candidates.filter((candidate) => Safe(placedQueens, candidate))

def Next(row: Int, placedQueens: List[Queen]): List[Queen] =
  FilterSafeQueens(RowOfQueens(RangeFromZeroTo(N), row), placedQueens)

def SolveNextRow(row: Int, placedQueens: List[Queen]): List[Queen] =
  Solve(Next(row + 1, placedQueens), row + 1, placedQueens)

def Solve(
    candidates: List[Queen],
    row: Int,
    placedQueens: List[Queen]
): List[Queen] =
  if row == N then
    candidates match
      case x :: _ => x :: placedQueens // return the solution
      case _      => Nil // no solution found
  else
    candidates match
      case x :: xs =>
        SolveNextRow(row, x :: placedQueens) match
          case Nil => // no solution found with this assumption
            Solve(xs, row, placedQueens)
          case y :: ys => // there is a solution
            y :: ys // SolveNextRow(row, x :: placedQueens)
      case _ =>
        Nil

def N = 7
def Solution = Solve(Next(0, Nil), 0, Nil)
```
Результаты (для 6 и 7):
```scala
List(Queen(6,5), Queen(5,3), Queen(4,1), Queen(3,6), Queen(2,4), Queen(1,2), Queen(0,0))
List(Queen(7,3), Queen(6,1), Queen(5,6), Queen(4,2), Queen(3,5), Queen(2,7), Queen(1,4), Queen(0,0))
```

## Решение с использованием метапрограммирования

Без необходимости запуска программы компилятор выполнит вычисления, и получит экземпляр, представляющий решение, значение которого будет выведено при запуске программы.

Программа, находящая решение, уже реализована выше. Нам остаётся лишь вызвать её в процессе компиляции и результат представить в виде выражения.

Вызов макроса производится с использованием сплайсинга:
```scala
inline def Solution(inline N: Int): List[plain.Queen] = ${ MyMacros.SolutionM('N) }
```
Сам макрос при вызове находит решение и формирует выражение, представляющее результат:
```scala
  def SolutionM(N: Expr[Int])(using Quotes): Expr[List[Queen]] =
    val solution = plain.Solution(N.valueOrAbort)
    Expr(solution)
```
Т.к. мы используем наш собственный тип `Queen`, а простое `Expr(queen)` для пользовательских типов не работает, то его необходимо представлять и в процессе работы макроса (`Expr[Queen]`) и в самом выражении (`Queen`). Для этого нам потребуется конвертер `ToExpr`:
```scala
  given ToExpr[Queen] with
    def apply(queen: Queen)(using Quotes): Expr[Queen] = 
      val Queen(x,y) = queen
      '{Queen(${Expr(x)},${Expr(y)})}
```

# Заключение

В настоящей заметке проблема расстановки ферзей решена несколькими способами:
- буквальной трансляцией решения TypeScript в систему типов Scala 3;
- с использованием системы типов Scala 3 и её развитых возможностей;
- обыкновенной программой (в функциональном стиле), работающей в runtime на основе значений;
- мета-программой, выполняющей все вычисления на этапе компиляции, а в runtime лишь печатающей готовый результат.

С прикладной точки зрения, выполнение вычислений на этапе компиляции позволяет алгоритмически конструировать корректные программы, которые будут работать в реальном времени. Простой пример - вычисление коэффициентов цифрового фильтра в процессе компиляции позволяет получить маленькую компактную программу, не включающую сложную библиотеку для расчётов.

Выполнение вычислений на типах сродни доказательству теорем о программе. Например, можно совместить реализацию алгоритма с механизмом расчёта типов и гарантировать некоторые свойства решения, выраженные в типах. Такие гарантии гораздо надёжнее, чем юнит-тесты, проверяющие лишь несколько отдельных случаев.
