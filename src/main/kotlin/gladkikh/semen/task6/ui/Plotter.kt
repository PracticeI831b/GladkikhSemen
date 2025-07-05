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
        chordRoots: List<Double>,         // Все корни методом хорд
        newtonRoots: List<Double>,        // Все корни методом Ньютона
        aVal: Double,
        bVal: Double,
        title: String,
        chordIntervals: List<Pair<Double, Double>>?, // Все интервалы
        newtonInitials: List<Double>?     // Все начальные приближения
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

        // Все интервалы метода хорд
        chordIntervals?.forEach { interval ->
            plot = plot +
                    geomVLine(xintercept = interval.first, color = "#FFA000", linetype = "dashed", size = 0.7) +
                    geomVLine(xintercept = interval.second, color = "#FFA000", linetype = "dashed", size = 0.7)
        }

        // Точки корней
        val rootPoints = mutableListOf<Triple<Double, Double, String>>()
        val rootColors = mutableListOf<String>()

        // Корни методом хорд
        chordRoots.forEach { root ->
            Equation.f(root, aVal, bVal)?.let { y ->
                rootPoints.add(Triple(root, y, "Метод хорд"))
                rootColors.add("red")
            }
        }

        // Корни методом Ньютона
        newtonRoots.forEach { root ->
            Equation.f(root, aVal, bVal)?.let { y ->
                rootPoints.add(Triple(root, y, "Метод Ньютона"))
                rootColors.add("#00CC33")
            }
        }

        // Добавление точек на график
        if (rootPoints.isNotEmpty()) {
            val pointsData = mapOf(
                "x" to rootPoints.map { it.first },
                "y" to rootPoints.map { it.second },
                "method" to rootPoints.map { it.third },
                "color" to rootColors
            )

            plot = plot +
                    geomPoint(
                        data = pointsData,
                        size = 5.0,
                        alpha = 0.8
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