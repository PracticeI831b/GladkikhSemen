import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.ZoomIn
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
import org.jetbrains.letsPlot.core.plot.base.Aes
import org.jetbrains.letsPlot.core.spec.Option
import org.jetbrains.letsPlot.geom.geomLine
import org.jetbrains.letsPlot.geom.geomPoint
import org.jetbrains.letsPlot.geom.geomHLine
import org.jetbrains.letsPlot.geom.geomVLine
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.ggplot
import org.jetbrains.letsPlot.label.ggtitle
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.scale.scaleColorManual
import org.jetbrains.letsPlot.scale.scaleXContinuous
import org.jetbrains.letsPlot.scale.scaleYContinuous
import org.jetbrains.letsPlot.skia.compose.PlotPanel
import kotlin.math.*

fun main() = application {
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
    var a by remember { mutableStateOf("1.0") }
    var b by remember { mutableStateOf("1.0") }

    var chordRoot by remember { mutableStateOf<Double?>(null) }
    var newtonRoot by remember { mutableStateOf<Double?>(null) }
    var chordIterations by remember { mutableStateOf(0) }
    var newtonIterations by remember { mutableStateOf(0) }

    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var hasCalculated by remember { mutableStateOf(false) }

    var fullPlot by remember { mutableStateOf<Figure?>(null) }
    var zoomedPlot by remember { mutableStateOf<Figure?>(null) }
    var isZoomed by remember { mutableStateOf(true) }

    var chordInterval by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var newtonInitial by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(a, b) {
        if (hasCalculated) {
            hasCalculated = false
            chordRoot = null
            newtonRoot = null
            fullPlot = null
            zoomedPlot = null
            errorMessage = ""
            chordIterations = 0
            newtonIterations = 0
            chordInterval = null
            newtonInitial = null
        }
    }

    fun f(x: Double, aVal: Double, bVal: Double): Double? {
        if (x <= 0) return null
        return try {
            sqrt(aVal * x) - cos(bVal * x)
        } catch (e: Exception) {
            null
        }
    }

    fun df(x: Double, aVal: Double, bVal: Double): Double? {
        if (x <= 0) return null
        return try {
            (aVal / (2 * sqrt(aVal * x))) + bVal * sin(bVal * x)
        } catch (e: Exception) {
            null
        }
    }

    fun chordMethod(x0: Double, x1: Double, aVal: Double, bVal: Double, eps: Double = 0.001): Pair<Double?, Int> {
        var a = x0
        var b = x1

        var fa = f(a, aVal, bVal) ?: return null to 0
        var fb = f(b, aVal, bVal) ?: return null to 0

        if (fa * fb > 0) {
            return null to 0
        }

        var iterations = 0
        val maxIterations = 1000
        var c: Double
        var fc: Double?

        do {
            c = (a * fb - b * fa) / (fb - fa)
            fc = f(c, aVal, bVal) ?: return null to 0

            if (fa * fc < 0) {
                b = c
                fb = fc
            } else {
                a = c
                fa = fc
            }

            iterations++
        } while (abs(fc) > eps && iterations < maxIterations)

        return if (iterations < maxIterations) c to iterations else null to 0
    }

    fun newtonMethod(x0: Double, aVal: Double, bVal: Double, eps: Double = 0.001): Pair<Double?, Int> {
        var x = max(x0, 1e-5)
        var iterations = 0
        val maxIterations = 1000
        var delta: Double

        do {
            val fx = f(x, aVal, bVal) ?: return null to 0
            val dfx = df(x, aVal, bVal) ?: return null to 0

            if (abs(dfx) < 1e-10) return null to 0

            delta = fx / dfx
            x -= delta
            iterations++
        } while (abs(delta) > eps && iterations < maxIterations)

        return if (iterations < maxIterations) x to iterations else null to 0
    }

    fun plotFunction() {
        errorMessage = ""
        chordRoot = null
        newtonRoot = null
        isLoading = true
        hasCalculated = true
        chordIterations = 0
        newtonIterations = 0
        chordInterval = null
        newtonInitial = null

        try {
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

            val maxXForSearch = min(10.0 * PI / abs(bVal), 100.0)
            val coarsePoints = 100
            val coarseXValues = mutableListOf<Double>()
            val coarseYValues = mutableListOf<Double>()

            for (i in 0 until coarsePoints) {
                val x = i * maxXForSearch / coarsePoints
                val y = f(x, aVal, bVal)
                if (y != null) {
                    coarseXValues.add(x)
                    coarseYValues.add(y)
                }
            }

            if (coarseXValues.isEmpty()) {
                errorMessage = "Не удалось вычислить значения функции"
                return
            }

            var rootEstimate: Double? = null
            var leftIndex = -1

            for (i in 1 until coarseXValues.size) {
                val y1 = coarseYValues[i-1]
                val y2 = coarseYValues[i]
                if (y1 * y2 <= 0) {
                    rootEstimate = (coarseXValues[i-1] + coarseXValues[i]) / 2
                    leftIndex = i-1
                    break
                }
            }

            if (rootEstimate == null) {
                errorMessage = "Не удалось найти корни на интервале [0, ${"%.2f".format(maxXForSearch)}]"
                return
            }

            chordInterval = coarseXValues[leftIndex] to coarseXValues[leftIndex + 1]
            val (chordResult, chordIters) = chordMethod(
                coarseXValues[leftIndex],
                coarseXValues[leftIndex + 1],
                aVal,
                bVal
            )
            chordRoot = chordResult
            chordIterations = chordIters

            if (chordRoot == null) {
                errorMessage = "Ошибка в методе хорд"
                return
            }

            newtonInitial = rootEstimate
            val (newtonResult, newtonIters) = newtonMethod(rootEstimate, aVal, bVal)
            newtonRoot = newtonResult
            newtonIterations = newtonIters

            if (newtonRoot == null) {
                errorMessage = "Ошибка в методе Ньютона"
                return
            }

            // Общий график
            val fullPlotData = mapOf("x" to coarseXValues, "y" to coarseYValues)
            fullPlot = createPlot(
                fullPlotData,
                chordRoot,
                newtonRoot,
                aVal,
                bVal,
                "f(x) = √(${aVal}x) - cos(${bVal}x) (общий вид)",
                chordInterval,
                newtonInitial
            )

            // Увеличенный график с динамической подстройкой отступа
            val minRoot = min(chordRoot ?: 0.0, newtonRoot ?: 0.0)
            val maxRoot = max(chordRoot ?: 0.0, newtonRoot ?: 0.0)
            val range = maxRoot - minRoot
            val padding = if (range > 0) range * 1.5 else 2.0
            val viewMinX = max(0.0, minRoot - padding)
            val viewMaxX = maxRoot + padding

            val points = 500
            val xValues = mutableListOf<Double>()
            val yValues = mutableListOf<Double>()

            for (i in 0 until points) {
                val x = viewMinX + i * (viewMaxX - viewMinX) / points
                val y = f(x, aVal, bVal)
                if (y != null) {
                    xValues.add(x)
                    yValues.add(y)
                }
            }

            val zoomedPlotData = mapOf("x" to xValues, "y" to yValues)
            zoomedPlot = createPlot(
                zoomedPlotData,
                chordRoot,
                newtonRoot,
                aVal,
                bVal,
                "f(x) = √(${aVal}x) - cos(${bVal}x) (увеличенный вид)",
                chordInterval,
                newtonInitial
            )

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
            Text(
                text = "Решение уравнения √(ax) - cos(bx) = 0",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            )

            Text(
                text = "Точность вычисления корней: 0.001",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

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
                        Icon(Icons.Default.Calculate, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Вычислить")
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

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
                        Text("Корень методом хорд: ${"%.5f".format(chordRoot)} (итераций: $chordIterations)")
                        Text("Значение функции: ${"%.7f".format(f(chordRoot!!, a.replace(',', '.').toDouble(), b.replace(',', '.').toDouble()) ?: "N/A")}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Корень методом касательных: ${"%.5f".format(newtonRoot)} (итераций: $newtonIterations)")
                        Text("Значение функции: ${"%.7f".format(f(newtonRoot!!, a.replace(',', '.').toDouble(), b.replace(',', '.').toDouble()) ?: "N/A")}")

                        Spacer(modifier = Modifier.height(12.dp))
                        chordInterval?.let {
                            Text("Интервал для метода хорд: [${"%.5f".format(it.first)}, ${"%.5f".format(it.second)}]")
                        }
                        newtonInitial?.let {
                            Text("Начальное приближение Ньютона: ${"%.5f".format(it)}")
                        }
                    }
                }
            }

            if (hasCalculated) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "График функции:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { isZoomed = !isZoomed },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isZoomed) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                            contentDescription = if (isZoomed) "Общий вид" else "Увеличенный вид",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                    Text(
                        text = if (isZoomed) "Увеличенный вид" else "Общий вид",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            if (hasCalculated) {
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
                    } else {
                        val currentPlot = if (isZoomed) zoomedPlot else fullPlot

                        if (currentPlot != null) {
                            PlotPanel(
                                figure = currentPlot,
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
                                    text = "График не доступен",
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            } else {
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

private fun createPlot(
    data: Map<String, List<Double>>,
    chordRoot: Double?,
    newtonRoot: Double?,
    aVal: Double,
    bVal: Double,
    title: String,
    chordInterval: Pair<Double, Double>?,
    newtonInitial: Double?
): Figure {
    // Базовый график
    var plot = ggplot(data) +
            geomLine(color = "dark_blue", size = 1.0) {
                x = "x"
                y = "y"
            } +
            geomHLine(yintercept = 0.0, color = "gray", linetype = "dashed", size = 0.5) +
            geomVLine(xintercept = 0.0, color = "gray", linetype = "dashed", size = 0.5) +
            ggtitle(title) +
            scaleXContinuous(name = "Ось X") +
            scaleYContinuous(name = "Ось Y")

    // Желтые пунктирные линии интервала метода хорд
    chordInterval?.let { (left, right) ->
        plot = plot +
                geomVLine(
                    xintercept = left,
                    color = "#FFA000",
                    linetype = "dashed",
                    size = 1.0
                ) {} +
                geomVLine(
                    xintercept = right,
                    color = "#FFA000",
                    linetype = "dashed",
                    size = 1.0
                ) {}
    }

    // Создаем данные для точек корней и легенды
    val rootPoints = mutableListOf<Pair<Double, Double>>()
    val rootLabels = mutableListOf<String>()
    val rootColors = mutableListOf<String>()

    // Добавляем корень методом хорд (красный)
    chordRoot?.let { root ->
        f(root, aVal, bVal)?.let { y ->
            rootPoints.add(root to y)
            rootLabels.add("Метод хорд")
            rootColors.add("red")
        }
    }

    // Добавляем корень методом Ньютона (зеленый)
    newtonRoot?.let { root ->
        f(root, aVal, bVal)?.let { y ->
            rootPoints.add(root to y)
            rootLabels.add("Метод Ньютона")
            rootColors.add("#00CC33")
        }
    }

    // Если есть корни, добавляем их на график
    if (rootPoints.isNotEmpty()) {
        val pointsData = mapOf(
            "x" to rootPoints.map { it.first },
            "y" to rootPoints.map { it.second },
            "method" to rootLabels,
            "color" to rootColors
        )

        plot = plot +
                geomPoint(
                    data = pointsData,
                    size = 4.5
                ) {
                    x = "x"
                    y = "y"
                    color = "method"
                } +
                scaleColorManual(
                    values = listOf("red", "#00CC33"),
                    labels = listOf("Метод хорд", "Метод Ньютона"),
                    name = "Методы решения"
                )
    }

    return plot + ggsize(800, 500)
}

// Вспомогательная функция для добавления точки в данные легенды
private fun addLegendPoint(
    data: MutableMap<String, MutableList<Any>>,
    x: Double,
    y: Double,
    label: String,
    color: String
) {
    data.getOrPut("x") { mutableListOf() }.add(x)
    data.getOrPut("y") { mutableListOf() }.add(y)
    data.getOrPut("label") { mutableListOf() }.add(label)
    data.getOrPut("color") { mutableListOf() }.add(color)
}

// Вычисление функции
private fun f(x: Double, aVal: Double, bVal: Double): Double? {
    if (x <= 0) return null
    return try {
        sqrt(aVal * x) - cos(bVal * x)
    } catch (e: Exception) {
        null
    }
}