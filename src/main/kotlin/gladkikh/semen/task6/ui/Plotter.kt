package gladkikh.semen.task6.ui

// Логика построения графиков

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import org.jetbrains.letsPlot.Figure
import org.jetbrains.letsPlot.core.plot.base.Aes
import org.jetbrains.letsPlot.core.spec.Option
import org.jetbrains.letsPlot.geom.*
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.ggplot
import org.jetbrains.letsPlot.label.ggtitle
import org.jetbrains.letsPlot.scale.*
import org.jetbrains.letsPlot.skia.compose.PlotPanel
import org.jetbrains.letsPlot.letsPlot
import androidx.compose.ui.Modifier

import gladkikh.semen.task6.solver.Equation
import kotlin.math.abs

object Plotter {
    @Composable
    fun PlotPanel(figure: Figure) {
        PlotPanel(
            figure = figure,
            modifier = Modifier.fillMaxSize(),
            preserveAspectRatio = false,
            computationMessagesHandler = { messages ->
                messages.forEach { println("Computation Message: $it") }
            }
        )
    }

    fun createPlot(
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

        // Интервал метода хорд
        chordInterval?.let { (left, right) ->
            plot = plot +
                    geomVLine(xintercept = left, color = "#FFA000", linetype = "dashed", size = 1.0) +
                    geomVLine(xintercept = right, color = "#FFA000", linetype = "dashed", size = 1.0)
        }

        // Точки корней
        val rootPoints = mutableListOf<Pair<Double, Double>>()
        val rootLabels = mutableListOf<String>()
        val rootColors = mutableListOf<String>()

        chordRoot?.let { root ->
            Equation.f(root, aVal, bVal)?.let { y ->
                rootPoints.add(root to y)
                rootLabels.add("Метод хорд")
                rootColors.add("red")
            }
        }

        newtonRoot?.let { root ->
            Equation.f(root, aVal, bVal)?.let { y ->
                rootPoints.add(root to y)
                rootLabels.add("Метод Ньютона")
                rootColors.add("#00CC33")
            }
        }

        // Настройки отображения для близких корней
        val rootsAreClose = chordRoot != null && newtonRoot != null && abs(chordRoot - newtonRoot) < 0.001
        val pointSize = if (rootsAreClose) 6.0 else 4.5
        val pointAlpha = if (rootsAreClose) 0.5 else 1.0

        // Добавление точек на график
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
                        size = pointSize,
                        alpha = pointAlpha
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
}