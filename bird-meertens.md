Tags: 
PAA: ??? 
КДПВ
Тип: перевод?
Labels: функциональное программирование, вывод алгоритмов
Функциональное программирование
Совершенный код
Программирование

# Мета

На Вики есть описание вывода https://en.wikipedia.org/wiki/Bird%E2%80%93Meertens_formalism. 

# Вывод оптимального алгоритма с помощью формализма Бёрда-Меертенса

Некоторые оптимальные алгоритмы, оказывается, можно вывести из неоптимальных, пользуясь эквивалентными преобразованиями алгоритма. Бёрд и Меертенс разработали [формализм](https://academic.oup.com/comjnl/article/32/2/122/543545?login=false), который устанавливает свойства функций высшего порядка `map`, `fold`, `scan`, позволяющие преобразовывать алгоритмы в эквивалентные. ([См. также на Вики](https://en.wikipedia.org/wiki/Bird%E2%80%93Meertens_formalism)). Ниже представлен вольный перевод статьи Бёрда.

Рассмотрим [задачу поиска максимальной суммы сегмента массива](https://en.wikipedia.org/wiki/Maximum_subarray_problem). Эту задачу можно переформулировать в виде математически точного ответа:

> Для всех сегментов, которые можно получить из массива, необходимо посчитать сумму чисел, а затем среди всех таких сумм найти максимальную.

<cut/>

Такая словесная формулировка неудобна для последующих манипуляций. Поэтому введём дополнительные определения.

Функция `segs` возвращает все возможные сегменты (подмассивы) массива:

```scala
def segs(input: Array[Int]): List[Array[Int]] = ???
```

```haskell
segs :: [Int] -> [[Int]]
```

Функция `sum` вычисляет сумму элементов:
```scala
def sum(input: Array[Int]): Int = input.sum
```

```haskell
sum :: [Int] -> Int
```

Чтобы получить все возможные суммы, нам достаточно применить к каждому подмассиву функцию `sum`. Такая операция делается функцией `map`:

```scala
def map[A, B](f: A => B)(list: List[A]): List[B] = list.map(f)
```

```haskell
map :: (a -> b) -> [a] -> [b]
```

Теперь, чтобы найти максимальную сумму, достаточно применить функцию `max`:

```scala
def max(lst: List[Int]): Int = lst.max
```

```haskell
max :: [Int] -> Int
```

Обозначим функцию, находящую решение задачи, `mss` ("max segment sum"). Эта функция будет иметь примерно такой вид:

```scala
def mss(arr: Array[Int]): Int = max(map(sum)(segs(arr)))
```

```haskell
mss :: [Int] -> Int
mss arr = max(map(sum)(segs(arr)))
```

В Haskell'е можно ту же самую функцию записать элегантнее, если использовать оператор композиции `.`. Такой оператор возвращает функцию одного аргумента, которая вначале применит правую функцию к своему аргументу, а затем левую функцию к результату, возвращённому правой функцией. Иными словами, `f . g` эквивалентно `x => f(g(x))`.

```haskell
mss = max . map(sum) . segs
```
Также можно опустить скобочки, т.к. аргументы передаются и так:
```haskell
mss = max . map sum . segs
```
В Scala вместо `.` используется `compose`:
```scala
val mss: Array[Int] => Int = 
    max `compose` map(sum) `compose` segs
```

Полученное выражение является одной из возможных формулировок исходной задачи и в то же время даёт алгоритм её решения. Этот алгоритм имеет сложность `O(n^3)` и требует `O(n^2)` памяти.

Вслед за Бёрдом попробуем произвести эквивалентные преобразования этого алгоритма.

## Определение `segs`

Получить все сегменты можно в два приёма. Вначале получить все возможные начала массива (`inits`). А затем для каждого начала найти все возможные окончания (`tails`).

```scala
def inits(arr: Array[Int]): List[Array[Int]] = ???
def tails(arr: Array[Int]): List[Array[Int]] = ???
```

```haskell
inits :: [Int] -> [[Int]]
tails :: [Int] -> [[Int]]
```

Чтобы применить `tails` к каждому началу, воспользуемся функцией `map`:

```haskell
segs1 :: [Int] -> [[[Int]]]
segs1 arr = map(tails)(inits(arr))
segs1 = map tails . inits
```

К сожалению, результатом будет вложенный список списков сегментов. Чтобы получить простой список, склеим все вложенные списки с помощью функции `concat`:

```haskell
concat :: [[a]] -> [a]
```

```scala
def concat[A](lst2: List[List[A]]): List[A] = lst2.flatten
```

Таким образом, функция `segs` может быть реализована так:

```haskell
segs = concat . map tails . inits
```

```scala
val segs = concat `compose` map(tails) `compose` inits
```

Подставив это определение в исходный алгоритм, получим:

```haskell
mss = max . map sum . concat . map tails . inits
```

```scala
val mss: Array[Int] => Int = 
    max `compose` map(sum) `compose` concat `compose` map(tails) `compose` inits
```

## Распределение `map` внутри вложенных списков (map promotion)

Если мы склеиваем вложенные списки, а затем каждый элемент преобразуем с помощью какой-то функции, то мы с тем же успехом можем вначале преобразовать каждый элемент, а уже затем склеивать вложенные списки. Результат будет тем же.

Чтобы преобразовывать каждый элемент внутри вложенных списков, нам потребуется дважды использовать `map`. Один раз для внешнего списка, а потом, для каждого вложенного.

```scala
def mapconcat[A, B](f: A => B)(lst2: List[List[A]]): List[B] = 
  lst2.map(innerList => innerList.map(f)).flatten
def mapconcat2[A, B](f: A => B)(lst2: List[List[A]]): List[B] = 
  lst2.flatten.map(f)
```

```haskell
mapconcat f lst2 = concat(map(map(f))(lst2))
mapconcat f = concat . map(map(f))
mapconcat f = concat . map (map f)

mapconcat2 f lst2 = map(f) (concat(lst2))
mapconcat2 f = map(f) . concat
```
Эти две функции дают одинаковый результат. То есть они эквивалентны. Будем обозначать такого рода эквивалентность символом `≡`:

```haskell
map(f) . concat ≡ concat . map(map f) 
```

Воспользуемся этим свойством и преобразуем `mss`:
```haskell
mss = max . map sum . concat . map tails . inits
mss = max . concat . map (map sum) . map tails . inits
```

```scala
val mss: Array[Int] => Int = 
    max `compose` map(sum) `compose` concat `compose` map(tails) `compose` inits
val mss: Array[Int] => Int = 
    max `compose` concat `compose` map(map(sum)) `compose` map(tails) `compose` inits
```

## Распределение `max` по вложенным спискам с использованием распределительного свойства `concat`/`fold` (fold promotion)

Функция `max`, используемая выше, не является элементарной. Она принимает на вход список элементов, которые потом обрабатывает последовательно, на каждом шагу применяя бинарную операцию максимизации. Такую операцию можно обозначить стрелочкой `⋏` (в дальнейшем мы будем использовать термин "операция `max`", чтобы было понятнее). Функция `max`, принимающая на вход список элементов, может быть реализована с помощью бинарной операции `max` и функции свёртки `foldl`:

```haskell
max = foldl(⋏)(-∞)
```

```scala
def max(lst: List[Int]): Int =
  lst.foldLeft(Int.MinValue)(math.max)
```

Операция `max` обладает полезным свойством ассоциативности. Можно вначале посчитать `max` для вложенных списков, а потом уже посчитать `max` для результатов. Иными словами:

```haskell
max . concat ≡ max . map(max)
```
(Здесь мы сразу используем символ эквивалентности `≡`.)

```scala
def maxConcat(lst2: List[List[Int]]): Int = 
  max(concat(lst2))
def maxConcat2(lst2: List[List[Int]]): Int = 
  max(lst2.map(innerList => max(innerList)))
```

Такое же свойство работает для любых ассоциативных операций и позволяет избавиться от склейки списков.


Воспользуемся этим свойством и преобразуем `mss`:
```haskell
mss = max . concat  . map (map sum) . map tails . inits
mss = max . map max . map (map sum) . map tails . inits
```

```scala
val mss: Array[Int] => Int = 
    max `compose` concat `compose` map(map(sum)) `compose` map(tails) `compose` inits
val mss: Array[Int] => Int = 
    max `compose` map(max) `compose` map(map(sum)) `compose` map(tails) `compose` inits
```

## Дистрибутивный закон для `map` и `compose` (слияние циклов)

Если мы последовательно применяем функции к элементам списка, то это можно сделать либо двумя проходами `map` которые склеиваются `compose`, либо одним проходом, применяя склеенную функцию:

```
map f . map g ≡ map (f . g)
```

В последнем выражении для `mss` у нас имеется три подряд `map`'а. Можно склеить все три:
```
map max . map (map sum) . map tails ≡ map (max . (map sum) . tails)
```

Таким образом, получаем новую версию `mss`:
```haskell
mss = max . map max . map (map sum) . map tails . inits
mss = max . map (max . (map sum) . tails)       . inits
```

```scala
val mss: Array[Int] => Int = 
    max `compose` map(max) `compose` map(map(sum)) `compose` map(tails) `compose` inits
val mss: Array[Int] => Int = 
    max `compose` map(    max `compose` map(sum) `compose` tails      ) `compose` inits
```

## Схема Горнера

До сих пор правила преобразований были не очень сложными и затрагивали отдельные операции. Схема Горнера позволяет упростить свёртку с двумя операциями сразу. 

В схеме Горнера задействованы две операции `⊕`, `⊗`. Для операции `⊗` необходим нейтральный элемент, обозначаемый `1`, кроме того, должен выполняться дистрибутивный закон относительно `⊕`. Запишем вначале эту схему для этих двух операций:

$$display$$
x_1 ⊗ x_2 ⊗ x_3 ⊗ ... ⊗ x_n + x_1 ⊗ x_2 ⊗ x_3 ⊗ ... ⊗ x_{n-1} + ... + x_1 ⊗ x_2 + x_1 + 1 = \\
= x_1 ⊗ (x_2 ⊗ x_3 ⊗ ... ⊗ x_n + x_2 ⊗ x_3 ⊗ ... ⊗ x_{n-1} + ... + x_2 + 1) + 1 = \\
= x_1 ⊗ (x_2 ⊗ (x_3 ⊗ ... ⊗ x_n + x_3 ⊗ ... ⊗ x_{n-1} + ... + 1) + 1) + 1 = \\
= x_1 ⊗ (x_2 ⊗ (x_3 ⊗ (... (x_{n-1}⊗(x_n + 1)) ...) + 1) + 1) + 1
$$display$$

Мы видим, что в исходном выражении имеется `n*(n-1)/2` операций `⊗`, а в последнем — только `n`. Т.е. такая схема выгодна с вычислительной точки зрения.

Применим эту схему к нашему алгоритму. В качестве операции `⊗` у нас будет обычное сложение (`+`), нейтральным элементом `1` для которого является `0`. А в качестве операции `⊕` мы будем использовать `⋏` (операцию `max`). Для сложения  выполняется дистрибутивный закон относительно операции `max`:

$$display$$
a + (b ⋏ c) ≡ (a + b) ⋏ (a + c)
$$display$$

(То есть, можно найти максимум, а затем прибавить константу, либо вначале прибавить контстанту, а потом найти максимум.) Для этих операций схема Горнера принимает вид:


$$display$$
(x_1 + x_2 + x_3 + ... + x_n) ⋏ (x_1 + x_2 + x_3 + ... + x_{n-1}) ⋏ ... ⋏ (x_1 + x_2) ⋏ x_1 ⋏ 0 = \\
= x_1 + (x_2 + (x_3 + (... (x_{n-1}+(x_n ⋏ 0)) ...) ⋏ 0) ⋏ 0) ⋏ 0
$$display$$

Нам удобнее перенумеровать переменные в обратном порядке:

$$display$$
(x_n + x_{n-1} + x_{n-2} + ... + x_1) ⋏ (x_n + x_{n-1} + x_{n-2} + ... + x_2) ⋏ ... ⋏ (x_n + x_{n-1}) ⋏ x_n ⋏ 0 = \\
=
x_n + (x_{n-1} + (x_{n-2} + (... (x_2+(x_1 ⋏ 0)) ...) ⋏ 0) ⋏ 0) ⋏ 0
$$display$$

Запишем левую часть с использованием функций, рассмотренных выше. В каждой скобке находятся элементы массива, начиная с некоторого индекса и до конца. Все элементы для всех скобок предоставляются функцией `tails`.

В скобках производится суммирование элементов. Чтобы произвести суммирование в каждом наборе элементов, воспользуемся функцией `map` с параметром `sum`:

```haskell
map sum . tails
```
И все полученные скобки затем сворачиваются с использованием операции `max`. Т.е. необходимо применить функцию `max`:
```haskell
max . map sum . tails
```

Рассмотрим теперь правую часть схемы Горнера. Здесь перебираются все элементы слева направо и постепенно вмешиваются в аккумулятор с помощью пары операций — сложения и `max`. Таким же образом работает `foldl` (`foldLeft`). Нам надо лишь выделить операцию, которая будет выполняться каждый раз:

```scala
(element, accumulator) => element + accumulator ⋏ 0
```
Обозначим такую операцию `⊙` (или `summax0`). Тогда 
```scala
val summax0 =
  (element: Int, accumulator: Int) => element + math.max(accumulator, 0)
```
```haskell
⊙ element accumulator = element + (max 0 accumulator)
⊙ = (+) . (max 0)
```
Начало вычислений следует начинать с нейтрального элемента для операции `max`. В качестве такого нейтрального элемента возьмём `-∞` (или `Int.MinValue`). Правая часть схемы Горнера принимает вид:

```scala
foldLeft(Int.MinValue)(summax0)
```
```haskell
foldl(⊙)(-∞)
```
И в целом схема Горнера:

```haskell
max . map sum . tails ≡ foldl(⊙)(-∞)
```

Воспользуемся схемой, чтобы упростить выражение для `mss`:
```haskell
mss = max . map (max . (map sum) . tails) . inits
mss = max . map (foldl(⊙)(-∞)           ) . inits
```

```scala
val mss: Array[Int] => Int = 
    max `compose` map(    max `compose` map(sum) `compose` tails      ) `compose` inits
val mss: Array[Int] => Int = 
    max `compose` map(    foldLeft(Int.MinValue)(summax0)             ) `compose` inits
```

## Сканирование с накоплением

Функции `foldl` и `foldr` осуществляют поэлементную свёртку с аккумулятором и возвращают самое последнее значение. Иногда требуется вернуть все промежуточные результаты, получаемые в процессе свёртки, в виде списка. Такие функции также имеются в стандартной библиотеке и носят названия `scanl` и `scanr` (`scanLeft`, `scanRight`):
```haskell
scanl :: (b -> a -> b) -> b -> [a] -> [b]
```

Обычная функция `foldl` легко выражается через `scanl`. Достаточно взять последний элемент: 
```
foldl f zero ≡ last . scanl f zero
```
Также можно и `scanl` выразить через `foldl`. Достаточно просто вызвать `foldl` для всех начал списка:

```
scanl f zero ≡ map (foldl f zero) . inits
```
Эти два эквивалентных алгоритма отличаются производительностью. В правой части сложность составляет `O(n^2)`, а в левой — `O(n)`. Поэтому это соотношение имеет смысл применять в обратную сторону, для улучшения эффективности. В частности, используя в качестве функции `f` операцию `summax0` (`⊙`), а в качестве начального значения `Int.MinValue` (`-∞`), получим такое соотношение:

```
map (foldl ⊙ -∞) . inits
≡
scanl ⊙ -∞
```

```scala
map(foldLeft(Int.MinValue)(summax0)) `compose` inits
≡
scanLeft(Int.MinValue)(summax0)
```

Подставляя в выражение для `mss`, получаем:
```haskell
mss = max . map (foldl(⊙)(-∞)) . inits
mss = max . scanl ⊙ -∞
```

```scala
val mss: Array[Int] => Int = 
    max `compose` map(foldLeft(Int.MinValue)(summax0)) `compose` inits
val mss: Array[Int] => Int = 
    max `compose` scanLeft(Int.MinValue)(summax0)
```

## Свёртка сканирования

В нашем последнем выражении мы дважды проходим по списку длиной `n` и по дороге вынуждены сохранять промежуточные результаты в памяти `O(n)`. Можем совместить эти два прохода и сохранять промежуточные результаты для каждого из них в двух отдельных ячейках. Для этого достаточно в качестве аккумулятора использовать пару.

Запишем функцию `max` через `foldl`:
```
max ≡ foldl ⋏ -∞
```
Аккумулятор для этой части алгоритма будем хранить в первом элементе пары.

`scanl` возвращает значения своего аккумулятора на каждом шаге в виде списка. Т.к. мы формируем один проход, то нам больше не потребуется список, а только лишь значение аккумулятора на каждом шаге. Этот аккумулятор будем хранить во втором элементе пары.
Сконструируем функцию `maxsummax0`, которая будет обрабатывать следующий элемент и пару аккумуляторов:
```scala
def maxsummax0(element: Int, pairOfAccumulators: (Int, Int)): (Int, Int) =
  val (w1, v1) = pairOfAccumulators
  val v2 = summax0(element, v1)
  val w2 = math.max(w1, v2)
  (w2, v2)
```

```haskell
maxsummax0 element (w1, v1) = (w2, v2)
  where
    v2 = summax0 element v1
    w2 = max w1 v2
```

Для `foldl` также требуется начальное значение аккумуляторов. Т.к. начальное значение `scanl` равно `0` и мы обязательно применяем к нему операцию `max` (`⋏`), то начальным значением для `foldl` будет пара нулей `(0, 0)`.

В конце алгоритма нам нужен только результат максимизации. Т.е. первый элемент пары `fst` (`_._1`).

Таким образом, получаем следующее соотношение
```
max . scanl ⊙ -∞ ≡ fst . foldl maxsummax0 (0,0)
```
```scala
def fst[A, B](p: (A, B)): A = p._1

fst . foldLeft((0,0))(maxsummax0)
```
Используем это соотношение в `mss`:
```haskell
mss = max . scanl ⊙ -∞
mss = fst . foldl maxsummax0 (0,0)
```

```scala
val mss: Array[Int] => Int = 
    max `compose` scanLeft(Int.MinValue)(summax0)
val mss: Array[Int] => Int = 
    fst . foldLeft((0,0))(maxsummax0)
```

# Заключение

Начав с буквальной формулировки постановки задачи, которая по совместительству оказалась алгоритмом, имеющим сложность `O(n^3)`, Бёрд в результате ряда эквивалентных преобразований получил линейный алгоритм (`O(n)`):

```haskell
mss = max . map sum . segs
≡
mss = fst . foldl maxsummax0 (0,0)
```
Получившийся алгоритм является оптимальным для этой задачи [алгоритмом Кадане](https://habr.com/ru/articles/539166/).