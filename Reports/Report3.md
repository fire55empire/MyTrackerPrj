# Отчёт о реализации первой основной функции приложения MyTracker

## Обзор

В данном документе максимально подробно описана реализация первой основной функции приложения — **показ прогресса от начала цели и до конца её выполнения, возможность отмечать прогресс**.

---

## 1. Анализ требований

### Функциональные требования:

1. **Экран создания цели** (если цель не задана):
   - Поле для ввода названия цели
   - Поле для ввода количества дней
   - Кнопка создания цели

2. **Экран отслеживания прогресса** (если цель существует):
   - Отображение названия цели сверху
   - Отображение процента прогресса (сколько дней отмечено)
   - Возможность отметить цель один раз в день
   - Кнопка удаления цели внизу экрана
   - После удаления цели — возврат на экран создания

### Технические требования:

- Язык: Kotlin
- UI: Jetpack Compose
- Хранение данных: Preferences DataStore (как указано в техническом задании)
- Минимальная версия Android: API 24

---

## 2. Подготовка проекта: добавление зависимостей

### 2.1. Добавление DataStore

**Файл**: `gradle/libs.versions.toml`

**Что было сделано**:
1. Добавлена версия DataStore в секцию `[versions]`:
   ```toml
   datastore = "1.1.1"
   ```

2. Добавлена библиотека в секцию `[libraries]`:
   ```toml
   androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
   ```

**Зачем**: Preferences DataStore — это современный способ хранения простых данных (ключ-значение) в Android. Он асинхронный, типобезопасный и основан на Kotlin Coroutines и Flow.

**Файл**: `app/build.gradle.kts`

**Что было сделано**:
Добавлена зависимость в секцию `dependencies`:
```kotlin
implementation(libs.androidx.datastore.preferences)
```

### 2.2. Добавление десахаринга для java.time

**Проблема**: 
- `java.time` API доступен только начиная с Android API 26 (Android 8.0)
- Проект поддерживает API 24 (Android 7.0)
- Необходимо использовать десахаринг для обратной совместимости

**Решение**: Включение Core Library Desugaring

**Файл**: `gradle/libs.versions.toml`

**Что было сделано**:
1. Добавлена версия библиотеки десахаринга:
   ```toml
   desugarJdkLibs = "2.1.4"
   ```

2. Добавлена библиотека:
   ```toml
   androidx-desugar-jdk-libs = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugarJdkLibs" }
   ```

**Файл**: `app/build.gradle.kts`

**Что было сделано**:
1. Включён десахаринг в `compileOptions`:
   ```kotlin
   compileOptions {
       sourceCompatibility = JavaVersion.VERSION_17
       targetCompatibility = JavaVersion.VERSION_17
       isCoreLibraryDesugaringEnabled = true  // <-- Добавлено
   }
   ```

2. Добавлена зависимость:
   ```kotlin
   coreLibraryDesugaring(libs.androidx.desugar.jdk.libs)
   ```

**Зачем**: Десахаринг позволяет использовать современные Java API (включая `java.time`) на старых версиях Android, преобразуя их в совместимый код во время компиляции.

---

## 3. Создание модели данных

### 3.1. Файл `Goal.kt`

**Путь**: `app/src/main/java/com/example/mytracker/data/Goal.kt`

**Что было сделано**:
Создан data class для представления цели пользователя.

**Структура класса**:

```kotlin
data class Goal(
    val name: String,                    // Название цели
    val totalDays: Int,                  // Общее количество дней
    val startDate: LocalDate,            // Дата начала цели
    val markedDates: Set<LocalDate> = emptySet()  // Множество отмеченных дат
)
```

**Детальное описание полей**:

1. **`name: String`**
   - Название цели, введённое пользователем
   - Используется для отображения на экране прогресса

2. **`totalDays: Int`**
   - Общее количество дней, на которое рассчитана цель
   - Используется для расчёта процента прогресса

3. **`startDate: LocalDate`**
   - Дата начала отслеживания цели
   - Сохраняется при создании цели
   - Может использоваться для будущих функций (например, расчёт оставшихся дней)

4. **`markedDates: Set<LocalDate>`**
   - Множество дат, когда пользователь отметил выполнение цели
   - Используется `Set` для автоматического исключения дубликатов
   - По умолчанию пустое множество

**Вычисляемые свойства**:

1. **`progress: Float`**
   ```kotlin
   val progress: Float
       get() = if (totalDays > 0) {
           markedDates.size.toFloat() / totalDays.toFloat()
       } else {
           0f
       }
   ```
   - **Назначение**: Вычисляет прогресс как дробное число от 0.0 до 1.0
   - **Логика**: Количество отмеченных дней делится на общее количество дней
   - **Защита от деления на ноль**: Если `totalDays == 0`, возвращается 0.0

2. **`progressPercent: Int`**
   ```kotlin
   val progressPercent: Int
       get() = (progress * 100).toInt()
   ```
   - **Назначение**: Преобразует прогресс в проценты (0-100)
   - **Использование**: Отображается на экране прогресса

3. **`daysCompleted: Int`**
   ```kotlin
   val daysCompleted: Int
       get() = markedDates.size
   ```
   - **Назначение**: Возвращает количество отмеченных дней
   - **Использование**: Отображается в формате "Выполнено: X из Y дней"

**Почему data class**:
- Автоматическая генерация `equals()`, `hashCode()`, `toString()`, `copy()`
- Удобство для работы с неизменяемыми данными
- Поддержка деструктуризации

**Почему `Set<LocalDate>` для markedDates**:
- Автоматическое исключение дубликатов
- Быстрая проверка наличия элемента (`contains()`)
- Эффективная работа с датами

---

## 4. Создание репозитория для работы с данными

### 4.1. Файл `GoalRepository.kt`

**Путь**: `app/src/main/java/com/example/mytracker/data/GoalRepository.kt`

**Назначение**: Класс для работы с хранением и получением данных о цели через DataStore.

### 4.2. Инициализация DataStore

```kotlin
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "goal_preferences")
```

**Что это**:
- Расширение для `Context`, создающее экземпляр DataStore
- Используется делегат `by` для ленивой инициализации
- Имя файла: `goal_preferences.preferences_pb` (бинарный формат Protocol Buffers)

**Почему расширение**:
- Удобный доступ из любого контекста
- Единый экземпляр DataStore на приложение
- Автоматическое управление жизненным циклом

### 4.3. Ключи для хранения данных

```kotlin
private val goalNameKey = stringPreferencesKey("goal_name")
private val totalDaysKey = stringPreferencesKey("total_days")
private val startDateKey = stringPreferencesKey("start_date")
private val markedDatesKey = stringPreferencesKey("marked_dates")
```

**Что это**:
- Типобезопасные ключи для доступа к данным в DataStore
- Каждый ключ имеет тип (`stringPreferencesKey` — для строк)
- Имена ключей используются как идентификаторы в хранилище

**Почему отдельные ключи**:
- Типобезопасность (компилятор проверяет типы)
- Удобство работы (автодополнение в IDE)
- Читаемость кода

### 4.4. Форматтер дат

```kotlin
private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
```

**Что это**:
- Форматтер для преобразования `LocalDate` в строку и обратно
- Формат: `YYYY-MM-DD` (например, "2025-11-16")

**Почему ISO_LOCAL_DATE**:
- Стандартный формат ISO 8601
- Однозначное представление даты
- Легко парсится и читается

### 4.5. Flow для наблюдения за изменениями

```kotlin
val goalFlow: Flow<Goal?> = context.dataStore.data.map { preferences ->
    // ... преобразование preferences в Goal
}
```

**Что это**:
- Реактивный поток данных, который автоматически обновляется при изменении в DataStore
- Возвращает `Goal?` (nullable) — `null` если цель не создана

**Детальная логика преобразования**:

1. **Проверка наличия обязательных данных**:
   ```kotlin
   val name = preferences[goalNameKey]
   val totalDaysStr = preferences[stringPreferencesKey("total_days")]
   
   if (name == null || totalDaysStr == null) {
       null  // Цель не создана
   }
   ```

2. **Парсинг количества дней**:
   ```kotlin
   val totalDays = totalDaysStr.toIntOrNull() ?: return@map null
   ```
   - Безопасное преобразование строки в число
   - Если не удалось — возврат `null`

3. **Парсинг даты начала**:
   ```kotlin
   val startDateStr = preferences[startDateKey] ?: LocalDate.now().format(dateFormatter)
   val startDate = LocalDate.parse(startDateStr, dateFormatter)
   ```
   - Если дата не сохранена — используется текущая дата
   - Парсинг через `DateTimeFormatter`

4. **Парсинг отмеченных дат**:
   ```kotlin
   val markedDatesStr = preferences[markedDatesKey] ?: ""
   val markedDates = if (markedDatesStr.isEmpty()) {
       emptySet()
   } else {
       markedDatesStr.split(",")
           .mapNotNull { dateStr ->
               try {
                   LocalDate.parse(dateStr.trim(), dateFormatter)
               } catch (e: Exception) {
                   null  // Пропуск некорректных дат
               }
           }
           .toSet()
   }
   ```
   - **Формат хранения**: даты разделены запятыми (например, "2025-11-16,2025-11-17")
   - **Обработка ошибок**: некорректные даты игнорируются
   - **Преобразование**: список → множество (удаление дубликатов)

5. **Создание объекта Goal**:
   ```kotlin
   Goal(name, totalDays, startDate, markedDates)
   ```

**Почему Flow**:
- Реактивное программирование (автоматическое обновление UI)
- Интеграция с Compose через `collectAsState()`
- Асинхронная работа без блокировки UI

### 4.6. Метод сохранения цели

```kotlin
suspend fun saveGoal(goal: Goal) {
    context.dataStore.edit { preferences ->
        preferences[goalNameKey] = goal.name
        preferences[stringPreferencesKey("total_days")] = goal.totalDays.toString()
        preferences[startDateKey] = goal.startDate.format(dateFormatter)
        preferences[markedDatesKey] = goal.markedDates
            .map { it.format(dateFormatter) }
            .joinToString(",")
    }
}
```

**Что это**:
- Асинхронная функция (suspend) для сохранения цели
- Использует транзакционное редактирование DataStore

**Детальная логика**:

1. **`context.dataStore.edit { }`**:
   - Открывает транзакцию редактирования
   - Гарантирует атомарность операции (всё или ничего)
   - Автоматически сохраняет изменения

2. **Сохранение простых полей**:
   - `goal.name` → строка
   - `goal.totalDays` → строка (преобразование числа)

3. **Сохранение даты начала**:
   - `goal.startDate.format(dateFormatter)` → строка в формате ISO

4. **Сохранение отмеченных дат**:
   - Преобразование множества дат в строку
   - Каждая дата форматируется
   - Даты объединяются через запятую

**Почему suspend функция**:
- DataStore операции асинхронные
- Не блокируют UI поток
- Работают с корутинами

### 4.7. Метод отметки на сегодня

```kotlin
suspend fun markToday(): Boolean {
    val today = LocalDate.now()
    val preferences = context.dataStore.data.first()
    
    // ... получение текущей цели
    
    if (markedDates.contains(today)) {
        return false // Уже отмечено сегодня
    }
    
    val updatedGoal = Goal(name, totalDays, startDate, markedDates + today)
    saveGoal(updatedGoal)
    return true
}
```

**Что это**:
- Отмечает сегодняшний день как выполненный
- Возвращает `true` если успешно, `false` если уже отмечено или цель не существует

**Детальная логика**:

1. **Получение текущей даты**:
   ```kotlin
   val today = LocalDate.now()
   ```

2. **Чтение текущих данных**:
   ```kotlin
   val preferences = context.dataStore.data.first()
   ```
   - `first()` получает первое значение из Flow (текущее состояние)
   - Блокирующая операция, но в suspend функции это допустимо

3. **Проверка наличия цели**:
   ```kotlin
   val name = preferences[goalNameKey] ?: return false
   val totalDaysStr = preferences[stringPreferencesKey("total_days")] ?: return false
   ```
   - Если данных нет — возврат `false`

4. **Проверка, не отмечено ли уже сегодня**:
   ```kotlin
   if (markedDates.contains(today)) {
       return false // Уже отмечено сегодня
   }
   ```
   - **Ключевое требование**: можно отметить только один раз в день
   - Используется `Set.contains()` для быстрой проверки

5. **Создание обновлённой цели**:
   ```kotlin
   val updatedGoal = Goal(name, totalDays, startDate, markedDates + today)
   ```
   - Создаётся новый объект с добавленной сегодняшней датой
   - Используется оператор `+` для добавления элемента в множество

6. **Сохранение**:
   ```kotlin
   saveGoal(updatedGoal)
   return true
   ```

**Почему возвращает Boolean**:
- Позволяет UI показать сообщение, если уже отмечено
- Можно использовать для логирования

**Почему дублирование кода парсинга**:
- В `markToday()` нужно получить текущее состояние синхронно
- `goalFlow` — это Flow, который нельзя использовать напрямую в suspend функции
- Можно было бы рефакторить, но для простоты оставлено так

### 4.8. Метод удаления цели

```kotlin
suspend fun deleteGoal() {
    context.dataStore.edit { preferences ->
        preferences.remove(goalNameKey)
        preferences.remove(stringPreferencesKey("total_days"))
        preferences.remove(startDateKey)
        preferences.remove(markedDatesKey)
    }
}
```

**Что это**:
- Удаляет все данные о цели из DataStore
- После удаления `goalFlow` автоматически вернёт `null`

**Детальная логика**:
- Удаляются все четыре ключа
- Используется транзакционное редактирование
- После удаления UI автоматически переключится на экран создания (через Flow)

**Почему удаление всех ключей**:
- Гарантирует полное удаление данных
- Предотвращает остаточные данные

---

## 5. Создание UI экранов

### 5.1. Экран создания цели

**Файл**: `app/src/main/java/com/example/mytracker/ui/screens/CreateGoalScreen.kt`

**Назначение**: Экран для ввода названия цели и количества дней.

#### Структура компонента

```kotlin
@Composable
fun CreateGoalScreen(
    onCreateGoal: (String, Int) -> Unit,
    modifier: Modifier = Modifier
)
```

**Параметры**:
- `onCreateGoal`: Callback функция, вызываемая при создании цели
- `modifier`: Модификатор для настройки внешнего вида

#### Состояние компонента

```kotlin
var goalName by remember { mutableStateOf("") }
var daysText by remember { mutableStateOf("") }
```

**Что это**:
- Два состояния для хранения введённых данных
- `remember` — сохраняет состояние между рекомпозициями
- `mutableStateOf` — создаёт наблюдаемое состояние

**Почему два отдельных состояния**:
- Разделение ответственности
- Удобная валидация каждого поля отдельно

#### Layout структура

```kotlin
Column(
    modifier = modifier
        .fillMaxSize()
        .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
)
```

**Что это**:
- Вертикальная компоновка элементов
- Центрирование по горизонтали и вертикали
- Отступы 24dp со всех сторон

#### Элементы UI

1. **Заголовок**:
   ```kotlin
   Text(
       text = "Создать цель",
       style = MaterialTheme.typography.headlineMedium,
       modifier = Modifier.padding(bottom = 32.dp)
   )
   ```
   - Использует тему Material Design 3
   - Отступ снизу 32dp

2. **Поле ввода названия**:
   ```kotlin
   OutlinedTextField(
       value = goalName,
       onValueChange = { goalName = it },
       label = { Text("Название цели") },
       modifier = Modifier
           .fillMaxWidth()
           .padding(bottom = 16.dp),
       singleLine = true
   )
   ```
   - **`value`**: Текущее значение из состояния
   - **`onValueChange`**: Обновление состояния при вводе
   - **`label`**: Подсказка внутри поля
   - **`singleLine`**: Запрет переноса строки

3. **Поле ввода количества дней**:
   ```kotlin
   OutlinedTextField(
       value = daysText,
       onValueChange = { newValue ->
           if (newValue.all { it.isDigit() }) {
               daysText = newValue
           }
       },
       label = { Text("Количество дней") },
       modifier = Modifier
           .fillMaxWidth()
           .padding(bottom = 24.dp),
       singleLine = true
   )
   ```
   - **Валидация**: Разрешены только цифры
   - **`newValue.all { it.isDigit() }`**: Проверка каждого символа
   - Если есть нецифровой символ — значение не обновляется

4. **Кнопка создания**:
   ```kotlin
   Button(
       onClick = {
           val days = daysText.toIntOrNull()
           if (goalName.isNotBlank() && days != null && days > 0) {
               onCreateGoal(goalName, days)
           }
       },
       modifier = Modifier.fillMaxWidth(),
       enabled = goalName.isNotBlank() && daysText.toIntOrNull()?.let { it > 0 } == true
   ) {
       Text("Создать")
   }
   ```
   - **Валидация в onClick**: Дополнительная проверка перед вызовом
   - **`enabled`**: Кнопка активна только при корректных данных
   - **Условия**:
     - Название не пустое (`isNotBlank()`)
     - Количество дней — положительное число

**Почему валидация в двух местах**:
- `enabled` — визуальная обратная связь (кнопка серая)
- `onClick` — защита от случайного вызова

### 5.2. Экран отслеживания прогресса

**Файл**: `app/src/main/java/com/example/mytracker/ui/screens/ProgressScreen.kt`

**Назначение**: Отображение прогресса цели и возможность отметить выполнение.

#### Структура компонента

```kotlin
@Composable
fun ProgressScreen(
    goal: Goal,
    onMarkToday: () -> Unit,
    onDeleteGoal: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Параметры**:
- `goal`: Объект цели с данными
- `onMarkToday`: Callback для отметки на сегодня
- `onDeleteGoal`: Callback для удаления цели
- `modifier`: Модификатор для настройки

#### Проверка отметки на сегодня

```kotlin
val today = java.time.LocalDate.now()
val isMarkedToday = goal.markedDates.contains(today)
```

**Что это**:
- Получение текущей даты
- Проверка наличия в множестве отмеченных дат
- Используется для управления состоянием кнопки

#### Layout структура

```kotlin
Column(
    modifier = modifier
        .fillMaxSize()
        .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
)
```

**Что это**:
- Вертикальная компоновка
- Центрирование по горизонтали
- **`SpaceBetween`**: Распределение пространства между элементами (верхний блок и кнопка удаления)

#### Верхний блок (информация о цели)

```kotlin
Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Spacer(modifier = Modifier.height(32.dp))
    
    // Название цели
    Text(
        text = goal.name,
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.padding(bottom = 32.dp)
    )
    
    // Процент прогресса
    Text(
        text = "${goal.progressPercent}%",
        style = MaterialTheme.typography.displayMedium,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    // Количество дней
    Text(
        text = "Выполнено: ${goal.daysCompleted} из ${goal.totalDays} дней",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 48.dp)
    )
    
    // Кнопка отметки
    Button(
        onClick = onMarkToday,
        enabled = !isMarkedToday,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(
            text = if (isMarkedToday) "Уже отмечено сегодня" else "Отметить сегодня"
        )
    }
}
```

**Детали элементов**:

1. **Spacer**: Отступ сверху 32dp для визуального баланса

2. **Название цели**:
   - Стиль `headlineLarge` — крупный заголовок
   - Отступ снизу 32dp

3. **Процент прогресса**:
   - Стиль `displayMedium` — очень крупный текст для акцента
   - Форматирование: "X%"
   - Отступ снизу 16dp

4. **Количество дней**:
   - Стиль `bodyLarge` — обычный текст
   - Формат: "Выполнено: X из Y дней"
   - Отступ снизу 48dp (больше, чтобы отделить от кнопки)

5. **Кнопка отметки**:
   - **`enabled = !isMarkedToday`**: Неактивна, если уже отмечено
   - **Высота 56dp**: Стандартная высота кнопки Material Design
   - **Динамический текст**: Меняется в зависимости от состояния

#### Кнопка удаления

```kotlin
Button(
    onClick = onDeleteGoal,
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error
    ),
    modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)
)
{
    Text("Удалить цель")
}
```

**Что это**:
- Кнопка внизу экрана
- **Цвет ошибки**: Красный цвет для опасного действия
- **`error` цвет**: Из цветовой схемы Material Design 3

**Почему красный цвет**:
- Визуальное предупреждение о важности действия
- Соответствует принципам Material Design

**Почему внизу**:
- Требование из задания
- Логическое размещение (вторичное действие)

---

## 6. Интеграция в MainActivity

### 6.1. Файл `MainActivity.kt`

**Путь**: `app/src/main/java/com/example/mytracker/MainActivity.kt`

### 6.2. Инициализация репозитория

```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var goalRepository: GoalRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goalRepository = GoalRepository(this)
        // ...
    }
}
```

**Что это**:
- Создание экземпляра `GoalRepository` при создании Activity
- Передача контекста для доступа к DataStore

**Почему в Activity**:
- Единый экземпляр на всё приложение
- Доступен во всех Compose функциях

### 6.3. Главный компонент приложения

```kotlin
@Composable
fun MyTrackerApp(goalRepository: GoalRepository) {
    val goal by goalRepository.goalFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    
    // ...
}
```

**Что это**:
- Главный компонент, управляющий навигацией между экранами

**Детали**:

1. **`collectAsState(initial = null)`**:
   - Преобразует Flow в State для Compose
   - `initial = null` — начальное значение до загрузки данных
   - Автоматически вызывает рекомпозицию при изменении

2. **`rememberCoroutineScope()`**:
   - Создаёт CoroutineScope для запуска suspend функций
   - Привязан к жизненному циклу Composable
   - Отменяется при удалении компонента

### 6.4. Условная навигация

```kotlin
if (goal == null) {
    CreateGoalScreen(
        onCreateGoal = { name, days ->
            scope.launch {
                val newGoal = Goal(
                    name = name,
                    totalDays = days,
                    startDate = LocalDate.now()
                )
                goalRepository.saveGoal(newGoal)
            }
        }
    )
} else {
    ProgressScreen(
        goal = goal!!,
        onMarkToday = {
            scope.launch {
                goalRepository.markToday()
            }
        },
        onDeleteGoal = {
            scope.launch {
                goalRepository.deleteGoal()
            }
        }
    )
}
```

**Что это**:
- Условный рендеринг в зависимости от наличия цели

**Логика**:

1. **Если цель не создана** (`goal == null`):
   - Показывается `CreateGoalScreen`
   - При создании:
     - Создаётся объект `Goal` с текущей датой
     - Сохраняется через репозиторий
     - Flow автоматически обновляется → показывается `ProgressScreen`

2. **Если цель существует** (`goal != null`):
   - Показывается `ProgressScreen`
   - При отметке:
     - Вызывается `markToday()`
     - Flow обновляется → UI перерисовывается
   - При удалении:
     - Вызывается `deleteGoal()`
     - Flow возвращает `null` → показывается `CreateGoalScreen`

**Почему `goal!!`**:
- После проверки `if (goal == null)` компилятор знает, что `goal` не null
- `!!` — явное утверждение (safe, так как проверено выше)

**Почему `scope.launch`**:
- Все операции с DataStore — suspend функции
- Нужен CoroutineScope для запуска
- `launch` — запуск корутины без ожидания результата

### 6.5. Обёртка в тему

```kotlin
setContent {
    MyTrackerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MyTrackerApp(goalRepository)
        }
    }
}
```

**Что это**:
- Применение темы приложения
- `Surface` — контейнер с фоном из темы
- Обеспечивает единый стиль всего приложения

---

## 7. Технические решения и обоснования

### 7.1. Почему DataStore, а не SharedPreferences?

**DataStore**:
- ✅ Асинхронный API (не блокирует UI)
- ✅ Типобезопасность (ключи типизированы)
- ✅ Поддержка Flow (реактивное программирование)
- ✅ Обработка ошибок
- ✅ Современный подход (рекомендуется Google)

**SharedPreferences**:
- ❌ Синхронный API (может блокировать UI)
- ❌ Нет типобезопасности
- ❌ Нет реактивности
- ⚠️ Устаревший подход

### 7.2. Почему Set для markedDates?

**Преимущества Set**:
- ✅ Автоматическое исключение дубликатов
- ✅ Быстрая проверка наличия (`O(1)` в среднем)
- ✅ Семантически правильно (дата либо есть, либо нет)

**Альтернативы**:
- `List`: Допускает дубликаты, медленнее поиск
- `Array`: Фиксированный размер, сложнее работа

### 7.3. Почему LocalDate, а не Long (timestamp)?

**LocalDate**:
- ✅ Семантически правильно (работа с датами)
- ✅ Удобные методы (сравнение, форматирование)
- ✅ Независимость от часового пояса (для целей это важно)
- ✅ Читаемость кода

**Long (timestamp)**:
- ❌ Нужно преобразование
- ❌ Зависимость от часового пояса
- ❌ Менее читаемый код

### 7.4. Почему Flow для goalFlow?

**Flow**:
- ✅ Реактивное программирование
- ✅ Автоматическое обновление UI
- ✅ Интеграция с Compose (`collectAsState`)
- ✅ Асинхронность

**Альтернативы**:
- LiveData: Устаревший подход, не для Compose
- Callback: Не реактивно, сложнее управление

### 7.5. Почему один раз в день?

**Требование из задания**: "Цель можно отметить один раз за день"

**Реализация**:
```kotlin
if (markedDates.contains(today)) {
    return false // Уже отмечено сегодня
}
```

**Почему это важно**:
- Предотвращает случайное двойное нажатие
- Соответствует логике трекера привычек
- Один день = одно выполнение

### 7.6. Почему десахаринг?

**Проблема**: `java.time` доступен только с API 26, проект поддерживает API 24

**Решение**: Core Library Desugaring

**Как работает**:
- Во время компиляции код `java.time` преобразуется в совместимый код
- Используется библиотека десахаринга
- Прозрачно для разработчика

**Альтернативы**:
- ThreeTenABP: Дополнительная зависимость, больше кода
- Calendar: Устаревший API, менее удобный

---

## 8. Структура файлов проекта

После реализации структура проекта выглядит так:

```
app/src/main/java/com/example/mytracker/
├── data/
│   ├── Goal.kt                    # Модель данных цели
│   └── GoalRepository.kt          # Репозиторий для работы с DataStore
├── ui/
│   ├── screens/
│   │   ├── CreateGoalScreen.kt    # Экран создания цели
│   │   └── ProgressScreen.kt      # Экран отслеживания прогресса
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── MainActivity.kt                # Главная Activity с навигацией
```

---

## 9. Поток данных (Data Flow)

### 9.1. Создание цели

```
Пользователь вводит данные
    ↓
CreateGoalScreen.onCreateGoal()
    ↓
MainActivity: scope.launch { goalRepository.saveGoal() }
    ↓
GoalRepository.saveGoal()
    ↓
DataStore.edit { сохранение данных }
    ↓
goalFlow автоматически обновляется
    ↓
collectAsState() получает новое значение
    ↓
UI рекомпозируется → показывается ProgressScreen
```

### 9.2. Отметка на сегодня

```
Пользователь нажимает "Отметить сегодня"
    ↓
ProgressScreen.onMarkToday()
    ↓
MainActivity: scope.launch { goalRepository.markToday() }
    ↓
GoalRepository.markToday()
    ↓
Проверка: уже отмечено? → return false
    ↓ (если не отмечено)
Создание обновлённой цели с новой датой
    ↓
saveGoal(updatedGoal)
    ↓
DataStore обновляется
    ↓
goalFlow обновляется
    ↓
UI рекомпозируется → кнопка становится неактивной
```

### 9.3. Удаление цели

```
Пользователь нажимает "Удалить цель"
    ↓
ProgressScreen.onDeleteGoal()
    ↓
MainActivity: scope.launch { goalRepository.deleteGoal() }
    ↓
GoalRepository.deleteGoal()
    ↓
DataStore.edit { удаление всех ключей }
    ↓
goalFlow возвращает null
    ↓
collectAsState() получает null
    ↓
UI рекомпозируется → показывается CreateGoalScreen
```

---

## 10. Особенности реализации

### 10.1. Обработка ошибок

**В GoalRepository**:
- Парсинг дат обёрнут в `try-catch`:
  ```kotlin
  try {
      LocalDate.parse(dateStr.trim(), dateFormatter)
  } catch (e: Exception) {
      null  // Некорректные даты игнорируются
  }
  ```
- Безопасное преобразование строк в числа:
  ```kotlin
  val totalDays = totalDaysStr.toIntOrNull() ?: return@map null
  ```

**Почему так**:
- Устойчивость к повреждённым данным
- Приложение не падает при ошибках парсинга
- Некорректные данные просто игнорируются

### 10.2. Производительность

**Оптимизации**:
- Использование `Set` для быстрого поиска (`O(1)`)
- Ленивая инициализация DataStore (делегат `by`)
- Flow с кэшированием (DataStore кэширует данные в памяти)

**Потенциальные улучшения**:
- Можно добавить кэширование объекта Goal в репозитории
- Можно оптимизировать парсинг дат (сейчас парсится при каждом чтении)

### 10.3. Безопасность типов

**Типобезопасность**:
- Все ключи DataStore типизированы
- Компилятор проверяет типы
- Невозможно случайно использовать неправильный ключ

**Пример**:
```kotlin
preferences[goalNameKey]  // ✅ Компилятор знает, что это String?
preferences[totalDaysKey] = "text"  // ✅ Ошибка компиляции, если типы не совпадают
```

---

## 11. Тестирование функциональности

### 11.1. Сценарии использования

1. **Первый запуск приложения**:
   - ✅ Показывается экран создания цели
   - ✅ Можно ввести название и количество дней
   - ✅ Кнопка "Создать" активна только при корректных данных

2. **Создание цели**:
   - ✅ После создания автоматически показывается экран прогресса
   - ✅ Отображается название цели
   - ✅ Процент прогресса = 0%
   - ✅ "Выполнено: 0 из X дней"

3. **Отметка на сегодня**:
   - ✅ Кнопка "Отметить сегодня" активна
   - ✅ После нажатия процент обновляется
   - ✅ Кнопка становится неактивной с текстом "Уже отмечено сегодня"
   - ✅ Повторное нажатие невозможно

4. **Удаление цели**:
   - ✅ Кнопка "Удалить цель" внизу экрана
   - ✅ После удаления возврат на экран создания
   - ✅ Все данные удаляются

5. **Валидация**:
   - ✅ Поле количества дней принимает только цифры
   - ✅ Кнопка создания неактивна при пустых полях
   - ✅ Кнопка создания неактивна при нулевом или отрицательном количестве дней

---

## 12. Заключение

### Что было реализовано:

1. ✅ **Модель данных** (`Goal.kt`) — структура для хранения информации о цели
2. ✅ **Репозиторий** (`GoalRepository.kt`) — работа с DataStore, CRUD операции
3. ✅ **Экран создания** (`CreateGoalScreen.kt`) — UI для ввода данных
4. ✅ **Экран прогресса** (`ProgressScreen.kt`) — UI для отображения и управления
5. ✅ **Интеграция** (`MainActivity.kt`) — навигация и управление состоянием
6. ✅ **Зависимости** — DataStore и десахаринг для java.time

### Ключевые особенности:

- **Реактивность**: Автоматическое обновление UI при изменении данных
- **Типобезопасность**: Использование типизированных ключей DataStore
- **Простота**: Минималистичный UI, одна цель
- **Надёжность**: Обработка ошибок, валидация данных
- **Современность**: Использование актуальных технологий (Compose, DataStore, Coroutines)

### Соответствие требованиям:

- ✅ Показ прогресса от начала до конца цели
- ✅ Возможность отмечать прогресс (один раз в день)
- ✅ Экран создания цели при отсутствии цели
- ✅ Экран отслеживания с названием, процентом и кнопками
- ✅ Кнопка удаления цели внизу экрана
- ✅ Возврат на экран создания после удаления
- ✅ Простой и понятный интерфейс

Функциональность полностью реализована и готова к использованию.

