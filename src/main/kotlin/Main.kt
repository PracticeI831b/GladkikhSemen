import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.letsPlot.Figure
import org.jetbrains.letsPlot.geom.geomLine
import org.jetbrains.letsPlot.geom.geomPoint
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.ggplot
import org.jetbrains.letsPlot.label.ggtitle
import org.jetbrains.letsPlot.scale.scaleXContinuous
import org.jetbrains.letsPlot.scale.scaleYContinuous
import org.jetbrains.letsPlot.skia.compose.PlotPanel
import kotlin.math.*

fun main() = application {
    // Инициализация AWT для работы с Lets-Plot
    System.setProperty("java.awt.headless", "false")

    Window(
        onCloseRequest = ::exitApplication,
        title = "Решение уравнения √(ax) - cos(bx) = 0"
    ) {
        App()
    }
}

@Composable
@Preview
fun App() {
    // Состояния для параметров
    var a by remember { mutableStateOf("1.0") }
    var b by remember { mutableStateOf("1.0") }

    // Состояния для корней
    var chordRoot by remember { mutableStateOf<Double?>(null) }
    var newtonRoot by remember { mutableStateOf<Double?>(null) }

    // Ошибки и состояние загрузки
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var hasCalculated by remember { mutableStateOf(false) } // Флаг выполнения вычислений

    // График
    var plot by remember { mutableStateOf<Figure?>(null) }

    // Сброс состояния при изменении параметров
    LaunchedEffect(a, b) {
        if (hasCalculated) {
            hasCalculated = false
            chordRoot = null
            newtonRoot = null
            plot = null
            errorMessage = ""
        }
    }

    // Безопасное вычисление функции
    fun f(x: Double, aVal: Double, bVal: Double): Double? {
        if (x <= 0) return null
        return try {
            sqrt(aVal * x) - cos(bVal * x)
        } catch (e: Exception) {
            null
        }
    }

    // Безопасное вычисление производной
    fun df(x: Double, aVal: Double, bVal: Double): Double? {
        if (x <= 0) return null
        return try {
            (aVal / (2 * sqrt(aVal * x))) + bVal * sin(bVal * x)
        } catch (e: Exception) {
            null
        }
    }

    // Исправленный метод хорд
    fun chordMethod(x0: Double, x1: Double, aVal: Double, bVal: Double, eps: Double = 0.001): Double? {
        var a = x0
        var b = x1

        var fa = f(a, aVal, bVal) ?: return null
        var fb = f(b, aVal, bVal) ?: return null

        // Проверка разных знаков на концах интервала
        if (fa * fb > 0) {
            return null
        }

        var iterations = 0
        val maxIterations = 1000
        var c: Double
        var fc: Double?

        do {
            c = (a * fb - b * fa) / (fb - fa)
            fc = f(c, aVal, bVal) ?: return null

            // Выбор нового интервала
            if (fa * fc < 0) {
                b = c
                fb = fc
            } else {
                a = c
                fa = fc
            }

            iterations++
        } while (abs(fc) > eps && iterations < maxIterations)

        return if (iterations < maxIterations) c else null
    }

    // Исправленный метод Ньютона
    fun newtonMethod(x0: Double, aVal: Double, bVal: Double, eps: Double = 0.001): Double? {
        var x = max(x0, 1e-5)
        var iterations = 0
        val maxIterations = 1000
        var delta: Double

        do {
            val fx = f(x, aVal, bVal) ?: return null
            val dfx = df(x, aVal, bVal) ?: return null

            // Проверка производной на ноль
            if (abs(dfx) < 1e-10) return null

            delta = fx / dfx
            x -= delta
            iterations++
        } while (abs(delta) > eps && iterations < maxIterations)

        return if (iterations < maxIterations) x else null
    }

    // Построение графика с обработкой ошибок
    fun plotFunction() {
        errorMessage = ""
        chordRoot = null
        newtonRoot = null
        isLoading = true
        hasCalculated = true // Устанавливаем флаг выполнения вычислений

        try {
            // Нормализация разделителя дробных чисел
            val normalizedA = a.replace(',', '.')
            val normalizedB = b.replace(',', '.')

            val aVal = normalizedA.toDoubleOrNull()
            val bVal = normalizedB.toDoubleOrNull()

            if (aVal == null || bVal == null) {
                errorMessage = "Ошибка: параметры должны быть числами"
                return
            }

            if (aVal <= 0) {
                errorMessage = "Ошибка: параметр 'a' должен быть положительным"
                return
            }

            // Оптимизированное построение графика
            val maxX = min(10.0 * PI / abs(bVal), 100.0)
            val points = 300
            val xValues = mutableListOf<Double>()
            val yValues = mutableListOf<Double>()

            for (i in 0 until points) {
                val x = i * maxX / points
                val y = f(x, aVal, bVal)

                if (y != null) {
                    xValues.add(x)
                    yValues.add(y)
                }
            }

            if (xValues.isEmpty()) {
                errorMessage = "Не удалось вычислить значения функции"
                return
            }

            val plotData = mapOf("x" to xValues, "y" to yValues)

            // Поиск начального приближения для корней
            var rootEstimate: Double? = null
            var leftIndex = -1

            for (i in 1 until xValues.size) {
                val y1 = yValues[i-1]
                val y2 = yValues[i]
                if (y1 * y2 <= 0) {
                    rootEstimate = (xValues[i-1] + xValues[i]) / 2
                    leftIndex = i-1
                    break
                }
            }

            if (rootEstimate == null) {
                errorMessage = "Не удалось найти корни на интервале [0, ${"%.2f".format(maxX)}]"
                return
            }

            // Вычисление корней с использованием найденного интервала
            chordRoot = chordMethod(
                xValues[leftIndex],
                xValues[leftIndex + 1],
                aVal,
                bVal
            ) ?: run {
                errorMessage = "Ошибка в методе хорд"
                return
            }

            newtonRoot = newtonMethod(rootEstimate, aVal, bVal) ?: run {
                errorMessage = "Ошибка в методе Ньютона"
                return
            }

            // Создаем график с отображением корней
            plot = createPlot(plotData, chordRoot, newtonRoot, aVal, bVal, "f(x) = √(${aVal}x) - cos(${bVal}x)")

        } catch (e: Exception) {
            errorMessage = "Произошла ошибка: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    MaterialTheme(colors = lightColors(
        primary = Color(0xFF3F51B5),
        secondary = Color(0xFF7986CB),
        background = Color(0xFFF5F5F5),
        surface = Color.White
    )) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок
            Text(
                text = "Решение уравнения √(ax) - cos(bx) = 0",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            )

            // Информация о точности
            Text(
                text = "Точность вычисления корней: 0.001",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Параметры ввода
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = a,
                    onValueChange = { a = it },
                    label = { Text("Параметр a (положительное число)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("1.0 или 1,0") },
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.width(16.dp))

                OutlinedTextField(
                    value = b,
                    onValueChange = { b = it },
                    label = { Text("Параметр b (любое число)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("1.0 или 1,0") },
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { plotFunction() },
                    modifier = Modifier.height(56.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Вычислить")
                }
            }

            // Вывод ошибок
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Результаты (показываем только после выполнения вычислений)
            if (hasCalculated && chordRoot != null && newtonRoot != null) {
                Card(
                    elevation = 4.dp,
                    backgroundColor = MaterialTheme.colors.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Результаты вычислений:",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Корень методом хорд: ${"%.5f".format(chordRoot)}")
                        Text("Значение функции: ${"%.7f".format(f(chordRoot!!, a.replace(',', '.').toDouble(), b.replace(',', '.').toDouble()) ?: "N/A")}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Корень методом касательных: ${"%.5f".format(newtonRoot)}")
                        Text("Значение функции: ${"%.7f".format(f(newtonRoot!!, a.replace(',', '.').toDouble(), b.replace(',', '.').toDouble()) ?: "N/A")}")

                        // Уточнение точности
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Точность вычислений: 0.001 (заданная)",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // График (показываем только после выполнения вычислений)
            if (hasCalculated) {
                Text(
                    text = "График функции:",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                Card(
                    elevation = 8.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (plot != null) {
                        PlotPanel(
                            figure = plot!!,
                            modifier = Modifier.fillMaxSize(),
                            preserveAspectRatio = false,
                            computationMessagesHandler = { messages ->
                                messages.forEach { println("Computation Message: $it") }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Введите параметры и нажмите 'Вычислить'",
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                // Показываем пустую область с подсказкой
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Введите параметры и нажмите 'Вычислить'",
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

// Создание графика с корнями
private fun createPlot(
    data: Map<String, List<Double>>,
    chordRoot: Double?,
    newtonRoot: Double?,
    aVal: Double,
    bVal: Double,
    title: String
): Figure {
    // Создаем базовый график
    val basePlot = ggplot(data) +
            geomLine(color = "dark_blue", size = 1.0) {
                x = "x"
                y = "y"
            } +
            ggtitle(title) +
            scaleXContinuous(name = "Ось X") +
            scaleYContinuous(name = "Ось Y")

    // Создаем список для хранения точек корней
    val roots = mutableListOf<Pair<Double, Double>>()
    val labels = mutableListOf<String>()

    // Добавляем корень методом хорд, если он существует
    if (chordRoot != null) {
        val y = f(chordRoot, aVal, bVal)
        if (y != null) {
            roots.add(chordRoot to y)
            labels.add("Корень методом хорд")
        }
    }

    // Добавляем корень методом касательных, если он существует
    if (newtonRoot != null) {
        val y = f(newtonRoot, aVal, bVal)
        if (y != null) {
            roots.add(newtonRoot to y)
            labels.add("Корень методом касательных")
        }
    }

    // Если есть корни для отображения
    if (roots.isNotEmpty()) {
        // Создаем данные для точек корней
        val rootPoints = mapOf(
            "x" to roots.map { it.first },
            "y" to roots.map { it.second },
            "label" to labels
        )

        // Добавляем точки на график
        return basePlot +
                geomPoint(
                    data = rootPoints,
                    size = 5.0,
                    color = "red"
                ) {
                    x = "x"
                    y = "y"
                    fill = "label"
                } +
                ggsize(800, 500)
    }

    // Возвращаем базовый график, если нет корней
    return basePlot + ggsize(800, 500)
}

// Безопасное вычисление функции (должна быть доступна в этой области видимости)
private fun f(x: Double, aVal: Double, bVal: Double): Double? {
    if (x <= 0) return null
    return try {
        sqrt(aVal * x) - cos(bVal * x)
    } catch (e: Exception) {
        null
    }
}