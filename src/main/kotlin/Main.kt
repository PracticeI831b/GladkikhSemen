import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.letsPlot.Figure
import kotlin.math.*
import org.jetbrains.letsPlot.geom.geomLine
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.ggplot
import org.jetbrains.letsPlot.intern.toSpec
import org.jetbrains.letsPlot.label.ggtitle
import org.jetbrains.letsPlot.scale.scaleXContinuous
import org.jetbrains.letsPlot.scale.scaleYContinuous
import org.jetbrains.letsPlot.skia.compose.PlotPanel


fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Графики функций") {
        App()
    }
}

// Определение математических функций
data class MathFunction(
    val name: String,
    val function: (Double) -> Double,
    val xRange: ClosedFloatingPointRange<Double>
)

@Composable
@Preview
fun App() {
    // Список функций для отображения
    val functions = listOf(
        MathFunction("y = x", { x -> x }, -10.0..10.0),
        MathFunction("y = x^2", { x -> x * x }, -5.0..5.0),
        MathFunction("y = sin(x)", { x -> sin(x) }, -2 * PI..2 * PI),
        MathFunction("y = √x", { x -> sqrt(x) }, 0.0..25.0),
        MathFunction("y = √x * cos(x)", { x -> sqrt(x) * cos(x) }, 0.0..20.0),
        MathFunction("y = cos(√x)", { x -> cos(sqrt(x)) }, 0.0..50.0)
    )

    var selectedFunction by remember { mutableStateOf(functions[0]) }
    val plotData by remember(selectedFunction) { derivedStateOf { generatePlotData(selectedFunction) } }
    val plot by remember(plotData, selectedFunction.name) {
        derivedStateOf { createPlot(plotData, selectedFunction.name) }
    }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Выбор функции
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp).wrapContentHeight()
            ) {
                Text("Функция:", modifier = Modifier.padding(end = 8.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    functions.chunked(3).forEach { rowFunctions ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            rowFunctions.forEach { function ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 16.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedFunction == function,
                                        onClick = { selectedFunction = function }
                                    )
                                    Text(
                                        text = function.name,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // График
            Card(
                elevation = 4.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                PlotPanel(
                    figure = plot,
                    modifier = Modifier.fillMaxSize(),
                    preserveAspectRatio = false, // Позволяет графику растягиваться
                    computationMessagesHandler = { messages ->
                        messages.forEach { println("Computation Message: $it") }
                    }
                )
            }

        }
    }
}

// Генерация данных для графика
private fun generatePlotData(function: MathFunction): Map<String, List<Double>> {
    val points = 1000
    val xValues = mutableListOf<Double>()
    val yValues = mutableListOf<Double>()

    val range = function.xRange
    val step = (range.endInclusive - range.start) / (points - 1)

    repeat(points) { i ->
        val x = range.start + i * step
        xValues.add(x)
        try {
            yValues.add(function.function(x))
        } catch (e: Exception) {
            yValues.add(Double.NaN)
        }
    }

    return mapOf("x" to xValues, "y" to yValues)
}

// Создание графика с помощью Lets-Plot
private fun createPlot(data: Map<String, List<Double>>, title: String): Figure {
    return ggplot(data) +
            geomLine(color = "dark_blue", size = 1.0) {
                x = "x"
                y = "y"
            } +
            ggtitle(title) +
            scaleXContinuous(name = "Ось X") +
            scaleYContinuous(name = "Ось Y") +
            ggsize(800, 600)
}