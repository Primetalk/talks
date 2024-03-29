# Бестолковые тесты versus качественное ПО. Часть 2. Что делать? 4 Эквивалентность функций

В [первой части](https://habr.com/ru/articles/756776/) мы рассмотрели примеры тестов, из которых не все одинаково полезны. [Затем](https://habr.com/ru/articles/756780/) попытались определиться, что же такое качество ПО, и предложили ["распрямлять" код](https://habr.com/ru/articles/756784/). Рассмотрели [классификацию ошибок](https://habr.com/ru/articles/756788/).

Теперь рассмотрим, как использовать типы, чтобы реализуемая программа сразу гарантированно соответствовала предъявляемым требованиям.

<cut />
---
- [Бестолковые тесты versus качественное ПО. Часть 2. Что делать? 4 Эквивалентность функций](#бестолковые-тесты-versus-качественное-по-часть-2-что-делать-4-эквивалентность-функций)
    - [Задание функции таблицей и алгоритмом](#задание-функции-таблицей-и-алгоритмом)
    - [Эквивалентность функций](#эквивалентность-функций)
      - [Доказательство эквивалентности](#доказательство-эквивалентности)
      - [Гарантии корректности "по построению"](#гарантии-корректности-по-построению)
      - [Доказательство сделанной работы с помощью типов. 1](#доказательство-сделанной-работы-с-помощью-типов-1)
      - [Доказательство сделанной работы с помощью типов. 2](#доказательство-сделанной-работы-с-помощью-типов-2)
      - [Увеличение сложности программ](#увеличение-сложности-программ)
    - [Пример цепочки действий](#пример-цепочки-действий)
    - [Взаимодействие с подсистемами, обладающими состоянием (statful)](#взаимодействие-с-подсистемами-обладающими-состоянием-statful)
      - [Пример разделения сценария](#пример-разделения-сценария)
- [Заключение](#заключение)


### Задание функции таблицей и алгоритмом

С математической точки зрения функция представляет собой отображение `A ⇒ B`. Одним из способов представления отображения является табличный — в строках таблицы указывается конечный набор пар входных и выходных значений `{(a,b)} ⊆ A⨯B`. При этом множество входных данных конечно и его мощность равна количеству примеров.

Другим способом является алгоритмический — описывается алгоритм/программа, позволяющая получить выходное значение из входного `f: A ⇒ B`. Мощность множества входных данных равна мощности типа `A`.

Табличное представление функции используется в юнит-тестах. Алгоритмическое — в коде. Требования к программному обеспечению могут быть выражены в форме алгоритма, сформулированного на высоком уровне абстракции. Например, "при нажатии на кнопку А данные формы Б сохраняются в базу данных". В коде тот же алгоритм может быть выражен на более низком уровне абстракции и более формально.

### Эквивалентность функций

Можно ли убедиться, что две отдельные функции эквивалентны (`f ≡ g`), то есть для всех входных значений возвращают одинаковые результаты (или производят одинаковые действия)?

Если обе функции заданы в табличной форме, то достаточно проверить конечное множество пар и убедиться, что они совпадают.

Если одна функция задана в табличной форме (тест), а другая в алгоритмической (код), то без дополнительных предположений нельзя сказать, что функции эквивалентны. Можно сделать более слабое заключение о том, что алгоритмическая функция не противоречит табличным значениям (`тест ⊆ код`), для чего подставить тестовые данные в алгоритм.

В типичном случае тестовые данные готовятся на основе функциональных требований (функции, заданной на высоком уровне абстракции) и вручную проверяется (либо предполагается), что табличные данные соответствуют требуемой функции (`тест ⊆ требования`). Затем эти табличные данные используются в юнит-тестах для проверки того, что тестовые данные также не противоречат реализованной функции (`тест ⊆ код`). Можно ли на основе этих двух свойств сделать вывод о том, что код соответствует требованиям (`код ≡ требования`)? В общем случае — нет.

Какие дополнительные предположения необходимо сделать, чтобы можно было с определённой степенью уверенности говорить о том, что *код соответствует требованиям*?

#### Доказательство эквивалентности

Доказать эквивалентность двух функций, рассматриваемых как чёрные ящики, в общем случае невозможно. Зачастую пространство аргументов имеет больше элементов, чем можно перебрать за разумное время. (В обратную сторону ситуация несколько лучше. Чтобы опровергнуть эквивалентность, достаточно привести единственный пример, на котором функции дадут разный результат. Отсюда, по-видимому, следует рекомендация TDD писать "красный" тест.)

Если же мы рассматриваем две функции вместе с их реализацией, то можно попытаться предъявить цепочку эквивалентных преобразований, переводящую из одной формы в другую.

Рассмотрим пример.

```scala
def f(a: Int): Int =
  CD(B(a))

def g(a: Int): Int =
  D(BC(a))
```
(здесь имена функций `BC` и `CD` соответствуют их реализациям)

Подстановка тела чистой функции (`CD` или `BC`) является эквивалентным преобразованием. Аналогично и абстрагирование чистой функции (извлечение выражения в отдельную функцию с добавлением нового имени) тоже является эквивалентным преобразованием.

Выполняя одно из этих эквивалентных преобразований, мы получим **промежуточную форму**:
```scala
def h(a: Int): Int =
  D(C(B(a)))
```

Тем самым, получается цепочка эквивалентных преобразований `f ≡ h ≡ g`. ∎

Если же такую цепочку эквивалентных преобразований построить не удалось, то мы, к сожалению, не сможем обоснованно утверждать, что функции эквивалентны. Все остальные наши попытки, надежды и заблуждения можно отложить в сторону, и принять важный факт: 

> *Обоснованно утверждать, что какие-либо функции эквивалентны, мы можем только предъявив математическое доказательство их эквивалентности*.

Для нас важно рассмотреть, каким образом мы могли бы доказать эквивалентность алгоритма, заданного в требованиях к ПО, и алгоритма, реализованного разработчиками.

Разработчики уже имеют замечательный инструмент, обеспечивающий точное доказательство эквивалентности — компилятор. Если программа собирается, значит имя функции эквивалентно её реализации. Это даёт возможность полностью автоматического доказательства эквивалентности между высокоуровневым описанием алгоритма и конкретной реализацией. Если требования к программе записаны формальным языком и существует цепочка эквивалентных преобразований в работающую программу, то мы автоматически получаем **программу, достигающую требуемого результата**.

В процессе доказательства эквивалентности мы можем использовать некоторые приёмы, основанные на типах данных.

#### Гарантии корректности "по построению"

В некоторых доказательствах в математике используется подход "по построению". Т.е. строится мысленная конструкция определённым образом с соблюдением заданных ограничений. Затем оказывается, что эта конструкция обладает требуемыми свойствами в силу того, что на каждом шаге соблюдались ограничения.

#### Доказательство сделанной работы с помощью типов. 1

Типы данных могут выступать/рассматриваться в качестве логических утверждений, а работающая программа, преобразующая один тип в другой, может служить конструктивным доказательством результирующего утверждения ([изоморфизм Карри — Ховарда](https://ru.wikipedia.org/wiki/Соответствие_Карри_—_Ховарда)).
```scala
def solve(t: Task): Solution =
  ???
```
Если функция `solve` является чистой тотальной функцией без побочных эффектов, то для каждой задачи `Task` она сможет алгоритмически предоставить решение `Solution`.

Вот как можно было бы отразить алгоритм решения квадратного уравнения: 
```scala
def `решение квадратного уравнения`(a: Double, b: Double, c: Double): () | Double | (Double, Double) =
  ???
```
Глядя на сигнатуру этой функции, мы можем заключить, что у произвольного квадратного уравнения может быть 0, 1 или 2 вещественных корня.

#### Доказательство сделанной работы с помощью типов. 2

Попробуем смоделировать ситуацию, при которой мы сможем гарантировать, что экземпляр сущности был вставлен в базу данных. (То есть реализовать обработчик HTTP-запроса `POST /form`.) Сформулируем требование к программе:
```
TODO: Сервис должен сохранить полученную форму или вернуть ошибку.
```
В такой формулировке действие "сохранить" не отражено в результате (является побочным эффектом). Для проверки корректности работы потребуются моки и другая магия.

Какой результат обеспечит уверенность в том, что форма сохранена? Например, идентификатор, сгенерированный базой данных. В таком случае, требование к программе будет выглядеть так:
```
TODO: Простой JSON RPC сервис должен, получив форму в виде JSON, вернуть идентификатор формы в базе данных или ошибку.
```
Уже само требование, сформулированное в такой форме, фокусирует наше внимание не на процессе ("сохранить"), а на предъявляемом результате этого процесса ("идентификатор формы в базе данных").

Воспользуемся opaque типами на уровне библиотеки доступа к БД. (Вопрос написания такой библиотеки мы оставим за скобками.)

Пусть библиотека предоставляет следующий интерфейс:
```scala
opaque type Идентификатор[E] = Int

/** Возвращает идентификатор вставленной строки, сгенерированный базой. */
def вставить[A](a: A): ОперацияБД[Идентификатор[A]] = ???

/** Возвращает сущность A по идентификатору.
 * Тип идентификатора - обычный, т.к. поступает извне.
 */
def найти[A](id: Int): ОперацияБД[Option[A]] = ???

/** Выполнить операцию и вернуть IO (результат или ошибку). */
def выполнитьВБазеIO[A](op: ОперацияБД[A])(using db: DB): IO[A] = ???

def значениеИлиОшибка[A](ao: A | ошибка): ОперацияБД[A] =
  ao match
    case о: ошибка => 
      ОперацияБД.ошибка(о)
    case a =>
      ОперацияБД.значение(a)
```
(Кстати сказать, функция `значениеИлиОшибка` является конструктивным доказательством простой теоремы — "любое значение или ошибка представимы в виде `ОперацияБД`".)
Далее мы исходим из того, что библиотека протестирована и работает корректно.

Также нам потребуется библиотека http4s для реализации HTTP-сервиса и такая вспомогательная функция:

```scala
def `простой JSON RPC сервис`[A, B](f: A => IO[B]): Request[IO] => IO[Response[IO]] = запрос =>
  for 
    форма         <- запрос.as[Форма]
    идентификатор <- `сохранить в базе`(форма)
    ответ         <- Ok(идентификатор.asJson)
  yield
    ответ
```
Естественно, для записи в базу, необходимо получить соединение:
```scala
val пулСоединений: IO[DB] = ???
```

Пусть наша форма уже представлена в виде case-class'а с поддержкой сериализации в JSON:
```scala
case class Форма(data: Int)

given formDecoder: Decoder[Форма] = deriveDecoder
given formEncoder: Encoder[Форма] = deriveEncoder
```

Запишем требование и попробуем его конкретизировать с использованием имеющихся инструментов.

```
TODO: Простой JSON RPC сервис должен, получив форму в виде JSON, вернуть идентификатор формы в базе данных или ошибку.
```

Часть `"Простой JSON RPC сервис должен, получив форму в виде JSON, вернуть"` соответствует и заменяется на вышеприведённую функцию `простой JSON RPC сервис`:
```scala
`простой JSON RPC сервис`(TODO: вернуть идентификатор формы в базе данных или ошибку)
```
Часть требования `"... или ошибку"` уже предусмотрена в типе `IO`.

Остаётся реализовать вставку формы в базу. Т.е. сконструировать задание для БД `вставить`, и выполнить это задание в базе:
```scala
выполнитьВБазеIO(вставить(форма))
```
Чтобы что-то можно было выполнить в базе, необходимо получить соединение из пула. Объединение двух последовательных действий `IO` в одно действие осуществляется либо через `flatMap`, либо через `for`:
```scala
def `сохранить в базе`[A](a: A): IO[Идентификатор[A]] =
  for 
    соединениеСБД <- пулСоединений
    идентификатор <- выполнитьВБазеIO(вставить(a))(using соединениеСБД)
  yield
    идентификатор
```

Для удобства добавим конкретную версию функции для нашего типа формы:
```scala
def `сохранить форму в базе и вернуть идентификатор формы в базе данных`(форма: Форма): IO[Идентификатор[Форма]] =
  `сохранить в базе`(форма)
```

Продолжая эквивалентные преобразования требований, получаем:
```scala
`простой JSON RPC сервис`(`сохранить форму в базе и вернуть идентификатор формы в базе данных`)
```

Остаётся только завернуть в "церемонию", предлагаемую библиотекой http4s:
```scala
val routes =
  HttpRoutes.of[IO] {
    case запрос @ POST -> Root / "form" =>
      `простой JSON RPC сервис`(`сохранить форму в базе и вернуть идентификатор формы в базе данных`)(запрос)
  }
```

Что можно заметить? Во-первых, требования к программе, сформулированные с акцентом на результат, приводят к программе, корректность которой отражена в типах. Во-вторых, программа распадается на несколько независимых функций, вопрос корректности которых может быть рассмотрен индивидуально. В-третьих, собственно корректность сохранения в базе данных "доказывается" путём предоставления конструктивного доказательства в виде программы, сводящей задачу сохранения к вызову библиотечных функций (доказательство корректности которых мы оставляем за скобками, и считаем априори корректными).

И самое главное, требования к программе эквивалентными и уточняющими преобразованиями преобразованы в действующую программу. Сам процесс преобразования/конкретизации может служить доказательством эквивалентности (при условии, что термины понимаются сторонами корректно и в процессе конкретизации выбрана правильная реализация терминов).

Нужны ли для этой программы тесты?

#### Увеличение сложности программ

Отдельные требования, выражаемые в разных частях документации, либо разными заинтересованными сторонами, могут взаимодействовать между собой и приводить в конечном итоге к усложнению программы.

Рассмотрим, например, взаимодействие таких требований:
- простой JSON RPC сервис должен, получив форму в виде JSON, вернуть идентификатор формы в базе данных или ошибку
- все изменения в БД должны производиться транзакционно;
- все изменения в БД должны логироваться с указанием имени пользователя и момента выполнения изменений.

Реализация первого требования выглядит относительно несложно и рассмотрена выше.

Чтобы поддержать второе требование на уровне типов, мы можем исключить доступ к БД без явного указания транзакции. Например, можно объявить функцию верхнего уровня `простой транзакционный JSON RPC сервис`, которая будет управлять транзакциями. Все сервисы окажутся транзакционными.

Реализация третьего требования зависит от выбранного способа аутентификации на уровне базы. Если для каждого пользователя приложения создаётся пользователь базы, то возможна реализация на уровне самой базы с помощью триггера. Если же используется только аутентификация на уровне сервиса, то потребуется передавать идентификатор пользователя из запроса на уровень базы данных. В этом случае, логирование можно произвести с помощью следующей функции:

```scala
case class СтрокаЖурнала(типФормы: ИдентификаторТипаФормы, userId: UserId, time: LocalDateTime)

def logUserId[A: ТипФормы](dbid: Идентификатор[A], userId: UserId, момент: IO[LocalDateTime] = now()): ОперацияБД[Идентификатор[СтрокаЖурнала]] = 
  for
    time <- ОперацияБД.lift(момент)
    строкаЖурнала = СтрокаЖурнала(типФормы, userId, time)
    идентификатор <- вставить(строкаЖурнала)
  yield
    идентификатор
```

Для использования этой функции потребуется изменить операцию сохранения:
```scala
def `сохранить форму в базе, залогировать операцию и вернуть идентификаторы`(форма: Форма)(using UserId): IO[(Идентификатор[Форма], Идентификатор[СтрокаЖурнала])] =
  for 
    бд <- пулСоединений
    given DB = бд
    идентификатор <- выполнитьВБазеIO(вставить(форма))
    идентификаторСтрокиЖурнала <- выполнитьВБазеIO(logUserId(идентификатор, summon[UserId]))
  yield
    (идентификатор, идентификаторСтрокиЖурнала)
```

### Пример цепочки действий

Пусть "простая продажа" подразумевает выполнение следующих действий:
- чистое вычисление (например, полной стоимости заказа с учётом скидок, уведомления пользователя, содержащего сгенерированный номер заказа);
- транзакция в БД;
- отправка SMS.

Традиционно для тестирования такой составной операции, задействующей внешние зависимости (БД и сервис СМС), используются мок-объекты, позволяющие подменить зависимости и протестировать именно этот сценарий.

На этом примере хочется понять, каким образом можно написать качественную программу и при этом обойтись без моков.

Во-первых, представим результат побочных эффектов с помощью типов данных. Сервис SMS может предоставлять подтверждение отправки с помощью экземпляра trait'а:
```scala
sealed trait ПодтверждениеОтправкиSMS:
  val кому: String
  val текст: String
```
Наличие ключевого слова `sealed` гарантирует, что экземпляр может быть создан только в модуле, отвечающем за отправку SMS.

```scala
case class КвитанцияОбработкиЗаказа(
  идентификатор: Идентификатор[Заказ], 
  подтверждениеОтправкиSMS: ПодтверждениеОтправкиSMS,
)
```

Несмотря на то, что экземпляр этого класса нам не обязательно нужен в дальнейшем, само его существование говорит о том, что существует идентификатор, сгенерированный базой данных и существует `ПодтверждениеОтправкиSMS`, которое может быть создано только сервисом SMS.

Собственно реализация сервиса простой продажи будет иметь следующий вид:
```scala
def `простая продажа`(заказ: Заказ)(using SmsService, DB): IO[КвитанцияОбработкиЗаказа] = 
  for
    id <- выполнитьВБазеIO(вставить(заказ))
    уведомление = `текст уведомления`(заказ, id)
    sms <- уведомить("пользователь", уведомление)
  yield
    КвитанцияОбработкиЗаказа(id, sms)
```
Мы исходим из того, что корректность самих сервисов и используемых функций лежит за рамками настоящего рассмотрения. Остаётся вопрос корректности реализации сервиса.

Сервис имеет две внешние зависимости — `SmsService` и `DB`. Обычной практикой является реализация мок-объектов и запуск сервиса в рамках теста. В данном случае, на мой взгляд, корректность реализации демонстрируется сигнатурой функции. Само наличие функции, возвращающей `КвитанцияОбработкиЗаказа`, является конструктивным доказательством того, что такая квитанция будет сформирована в результате исполнения этого алгоритма. А содержимое квитанции демонстрирует результат — объект записан в БД и уведомление отправлено.

### Взаимодействие с подсистемами, обладающими состоянием (statful)

Для распределённых систем широкое признание получила идея отказа от "сессии" при взаимодействии между удалёнными системами и использовании stateless-протоколов (без состояния) наподобие REST. Аналогичная идея зачастую может быть использована при организации взаимодействия между подсистемами. Вместо цепочки действий, приводящих подсистему в требуемое состояние, формируется сложный объект, передаваемый подсистеме. Подсистема, обрабатывая этот сложный объект, самостоятельно приходит в конечное состояние.

К примеру, при работе с базой данных может потребоваться произвести согласованные изменения в нескольких таблицах, например: сохранить изменённые свойства объекта в новую строку таблицы версий, записать вложенные элементы в связанную таблицу, сохранить сведения о пользователе, выполняющем операцию, залогировать момент времени. Вместо того, чтобы эти изменения выполнять на уровне приложения, можно сформировать высокоуровневое событие, содержащее все требуемые параметры ("пользователь А изменил объект Б в момент времени Т"), и передать это событие в хранимую процедуру на уровне базы. Хранимая процедура, в свою очередь, выполнит все необходимые изменения в рамках транзакции.

Тестирование приложения, использующего такую подсистему, значительно упрощается. Мок-объект, представляющий подсистему, становится обычной табличной функцией, возвращающей предопределённый результат в ответ на сложное событие.

#### Пример разделения сценария

Пусть в рамках сценария нам требуется выполнить несколько действий в базе и в других сервисах:

1. Сохранить новую версию заказа (бд).
2. Записать в журнал сведения о пользователе (бд).
3. Записать заявку на списание/возмещение средств (бд).
4. Отправить sms-уведомление пользователю (сервис SMS).

Прямолинейная реализация сценария может выглядеть так:
```scala
def `изменение заказа`(заказ: Заказ, пользователь: String, момент: LocalDateTime)(using SmsService, DB): IO[Unit] =
  for
    id <- выполнитьВБазеIO(upsert(заказ))
    userId <- выполнитьВБазеIO(найтиПользователя(пользователь))
    _ <- выполнитьВБазеIO(logUserId(id, userId, IO{момент}))
    уведомление = `текст уведомления`(заказ, id)
    sms <- уведомить(пользователь, уведомление)
  yield
    ()
```

Эта реализация трижды обращается к базе данных для выполнения отдельных шагов алгоритма. При тестировании потребуется реализовать мок-объект, обрабатывающий три обращения и возвращающий содержательные ответы, достаточные для продолжения работа сценария. Причём, т.к. сам сценарий кроме базы данных использует и другие зависимости, то для его тестирования потребуется смоделировать и все остальные сервисы (в данном случае — SMS).

Если часть алгоритма, относящуюся к БД, выделить в отдельную функцию, то получим такой код:
```scala
case class Изменение[A](сущность: A, пользователь: String, момент: LocalDateTime)

def `изменение заказа2`(заказ: Заказ, пользователь: String, момент: LocalDateTime)(using SmsService, DB): IO[Unit] =
  val событиеИзмененияЗаказа = Изменение[Заказ](заказ, пользователь, момент)
  for
    квитанция <- выполнитьВБазеIO(обработатьСобытиеИзменения(событиеИзмененияЗаказа))
    уведомление = `текст уведомления`(заказ, квитанция.идентификатор)
    sms <- уведомить(пользователь, уведомление)
  yield
    ()
```
Такую функцию будет проще протестировать, так как база данных используется однократно, а сложная логика, определяемая схемой базы, изолирована. Вместо повторения части алгоритма при создании мок-объекта, достаточно указать одно возвращаемое значение в ответ на единственный вызов функции.

Закономерный вопрос, что происходит с логикой, исключённой из прикладного сценария. Как и раньше, логика может быть реализована в приложении, например, таким образом:
```scala
case class КвитанцияОбИзменении[A](идентификатор: Идентификатор[A], userId: UserId, строкаЖурнала: Идентификатор[СтрокаЖурнала])

def обработатьСобытиеИзменения[A: ТипФормы](изменение: Изменение[A]): ОперацияБД[КвитанцияОбИзменении[A]] =
  for 
    id <- upsert(изменение.сущность)
    userId <- найтиПользователя(изменение.пользователь)
    строкаЖурнала <- logUserId(id, userId, IO{изменение.момент})
  yield
    КвитанцияОбИзменении(id, userId, строкаЖурнала)
```
Либо эта логика может быть перенесена на уровень базы данных в виде хранимой процедуры без изменения сценария.

Вышеприведённая реализация явным образом отражает атомарность операции — все шаги, требуемые логикой приложения и схемой базы данных, объединены в одно действие.

Такая реализация обладает следующими преимуществами:
1. За счёт дженериков и наличия квитанции (то есть подтверждения результата в типе функции), корректность кода можно считать доказанной системой типов и код можно исключить из тестирования.
2. Поскольку код работает исключительно с базой данных, то он может быть протестирован в рамках интеграционного тестирования с тестовой базой без использования реального/поддельного sms-сервиса.
3. Даже если реализовывать мок-объект в классическом виде, здесь это сделать немного проще, т.к. требуется моделировать только БД, хотя по-прежнему потребуется реализовать несколько ответов мок-объекта.

# Заключение

В основе тестирования лежит идея проверки соответствия кода и требований. К сожалению, доказать это соответствие с помощью табличных тестов не представляется возможным.

Требования к программе при определённом усердии могут быть сформулированы как функция от абстрактных входных данных. А сама программа будет также являться некоторой функцией от какого-либо типа данных. Доказательство того, что программа соответствует требованиям, оказывается задачей доказательства эквивалентности этих двух функций, сформулированных на разных уровнях абстракции.

Использование некоторых особенностей функционального программирования делает формальное доказательство эквивалентности более доступным.


Вся серия заметок:
1. [Примеры тестов](https://habr.com/ru/articles/756776/).
2. Что делать?
   1. [Качество ПО](https://habr.com/ru/articles/756780/).
   2. ["Прямолинейность" кода](https://habr.com/ru/articles/756784/).
   3. [Эквивалентность функций](https://habr.com/ru/articles/756790/). 
   4. [Классификация ошибок](https://habr.com/ru/articles/756788/). 
   5. [Применимость тестов](https://habr.com/ru/articles/756792/).
3. [Исправление примеров](https://habr.com/ru/articles/756794/).
