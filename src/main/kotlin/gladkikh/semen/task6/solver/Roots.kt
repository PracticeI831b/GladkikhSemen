package gladkikh.semen.task6.solver

// Алгоритмы поиска корней

import gladkikh.semen.task6.solver.Equation.df
import gladkikh.semen.task6.solver.Equation.f
import kotlin.math.*
import gladkikh.semen.task6.ui.Plotter.createPlot
import org.jetbrains.letsPlot.Figure

// Структура результатов
data class RootResult(
    val chordRoots: List<Double>,          // Все корни методом хорд
    val newtonRoots: List<Double>,         // Все корни методом Ньютона
    val chordIterations: List<Int>,        // Итерации для каждого корня (хорды)
    val newtonIterations: List<Int>,       // Итерации для каждого корня (Ньютона)
    val chordIntervals: List<Pair<Double, Double>>,
    val newtonInitials: List<Double>,
    val fullPlot: Figure?,
    val zoomedPlot: Figure?,
    val fValuesChord: List<Double?>,
    val fValuesNewton: List<Double?>,
    val allRoots: List<Double>,             // Все найденные корни
    val rootsTooClose: List<Boolean>,
    val rootDifferences: List<Double>,
    val warning: String = "",
)

// Основная функция вычисления корней
fun computeRoots(
    a: String,
    b: String,
    callback: (RootResult?, String) -> Unit
) {
    try {
        // Нормализация ввода (замена запятых на точки)
        val normalizedA = a.replace(',', '.')
        val normalizedB = b.replace(',', '.')
        val aVal = normalizedA.toDoubleOrNull()
        val bVal = normalizedB.toDoubleOrNull()

        // Валидация параметров
        if (aVal == null || bVal == null) {
            callback(null, "Ошибка: параметры должны быть числами")
            return
        }

        // Определение максимального интервала поиска
        val maxXForSearch = min(10.0 * PI / abs(bVal), 100.0)
        val warningMessage = if (aVal == 0.0) {
            "При a=0 уравнение имеет вид -cos(bx)=0, что имеет бесконечно много корней. " +
                    "Показаны корни на ограниченном интервале."
        } else {
            ""
        }

        // Поиск интервалов с корнями (разные стратегии для a=0 и a≠0)
        val searchResult = if (aVal == 0.0) {
            // Для a=0 ищем корни на симметричном интервале [-maxX, maxX]
            findAllRootIntervals(aVal, bVal, maxXForSearch, searchNegative = true)
        } else {
            // Для a≠0 выбираем интервал в зависимости от знака a
            findAllRootIntervals(aVal, bVal, maxXForSearch, searchNegative = aVal < 0)
        }

        val rootEstimates = searchResult.rootEstimates
        val chordIntervals = searchResult.chordIntervals

        if (rootEstimates.isEmpty() || chordIntervals.isEmpty()) {
            val range = if (aVal < 0) "[-${"%.2f".format(maxXForSearch)}, 0]"
            else if (aVal > 0) "[0, ${"%.2f".format(maxXForSearch)}]"
            else "[-${"%.2f".format(maxXForSearch)}, ${"%.2f".format(maxXForSearch)}]"

            callback(null, "Не удалось найти корни на интервале $range")
            return
        }

        // Вычисление всех корней
        val chordResults = mutableListOf<Double>()
        val newtonResults = mutableListOf<Double>()
        val chordIterCounts = mutableListOf<Int>()
        val newtonIterCounts = mutableListOf<Int>()
        val fValuesChord = mutableListOf<Double?>()
        val fValuesNewton = mutableListOf<Double?>()
        val rootsTooClose = mutableListOf<Boolean>()
        val rootDifferences = mutableListOf<Double>()

        for (i in chordIntervals.indices) {
            // Вычисление корня методом хорд
            val chordResult = chordMethod(
                chordIntervals[i].first,
                chordIntervals[i].second,
                aVal,
                bVal
            )

            // Вычисление корня методом Ньютона
            val newtonResult = newtonMethod(
                rootEstimates[i],
                aVal,
                bVal
            )

            // Обработка результатов метода хорд
            chordResult.first?.let { chordRoot ->
                chordResults.add(chordRoot)
                chordIterCounts.add(chordResult.second)
                fValuesChord.add(Equation.f(chordRoot, aVal, bVal))

                // Сравнение с методом Ньютона
                newtonResult.first?.let { newtonRoot ->
                    val diff = abs(chordRoot - newtonRoot)
                    rootDifferences.add(diff)
                    rootsTooClose.add(diff < 0.001)
                }
            }

            // Обработка результатов метода Ньютона
            newtonResult.first?.let {
                newtonResults.add(it)
                newtonIterCounts.add(newtonResult.second)
                fValuesNewton.add(Equation.f(it, aVal, bVal))
            }
        }

        // Объединение близких корней
        val tolerance = 0.001
        val allRoots = mutableListOf<Double>()
        val sortedRoots = (chordResults + newtonResults).sorted()

        if (sortedRoots.isNotEmpty()) {
            var currentCluster = mutableListOf(sortedRoots[0])
            for (i in 1 until sortedRoots.size) {
                if (sortedRoots[i] - currentCluster.first() <= tolerance) {
                    currentCluster.add(sortedRoots[i])
                } else {
                    allRoots.add(currentCluster.average())
                    currentCluster = mutableListOf(sortedRoots[i])
                }
            }
            allRoots.add(currentCluster.average())
        }

        // Построение графиков
        val fullPlot = createPlot(
            searchResult.coarseData,
            chordResults,
            newtonResults,
            aVal,
            bVal,
            "f(x) = ${if (aVal >= 0) "√(${aVal}x)" else "√(${aVal}x)"} - cos(${bVal}x) (общий вид)",
            chordIntervals,
            rootEstimates
        )

        val zoomedPlot = createZoomedPlot(
            allRoots,
            aVal,
            bVal,
            chordResults,
            newtonResults,
            chordIntervals,
            rootEstimates
        )

        // Возврат результатов
        callback(RootResult(
            chordRoots = chordResults,
            newtonRoots = newtonResults,
            chordIterations = chordIterCounts,
            newtonIterations = newtonIterCounts,
            chordIntervals = chordIntervals,
            newtonInitials = rootEstimates,
            fullPlot = fullPlot,
            zoomedPlot = zoomedPlot,
            fValuesChord = fValuesChord,
            fValuesNewton = fValuesNewton,
            allRoots = allRoots,
            rootsTooClose = rootsTooClose,
            rootDifferences = rootDifferences,
            warning = warningMessage
        ), "")
    } catch (e: Exception) {
        callback(null, "Произошла ошибка: ${e.message}")
    }
}

// Обновленная функция поиска интервалов
private fun findAllRootIntervals(
    aVal: Double,
    bVal: Double,
    maxX: Double,
    searchNegative: Boolean = false
): SearchResult {
    // Определение границ поиска
    val minX = if (searchNegative) -maxX else 0.0
    val actualMaxX = if (searchNegative) 0.0 else maxX

    val points = 1000
    val xValues = mutableListOf<Double>()
    val yValues = mutableListOf<Double>()
    val rootEstimates = mutableListOf<Double>()
    val chordIntervals = mutableListOf<Pair<Double, Double>>()

    // Сбор точек функции
    for (i in 0..points) {
        val x = minX + i * (actualMaxX - minX) / points
        val y = Equation.f(x, aVal, bVal)
        if (y != null) {
            xValues.add(x)
            yValues.add(y)
        }
    }

    // Поиск интервалов смены знака
    for (i in 1 until xValues.size) {
        val y1 = yValues[i-1]
        val y2 = yValues[i]

        if (y1 * y2 <= 0) {
            val estimate = (xValues[i-1] + xValues[i]) / 2
            val interval = xValues[i-1] to xValues[i]

            rootEstimates.add(estimate)
            chordIntervals.add(interval)
        }
    }

    return SearchResult(
        coarseData = mapOf("x" to xValues, "y" to yValues),
        rootEstimates = rootEstimates,
        chordIntervals = chordIntervals
    )
}

// Обновленная структура результата поиска
private data class SearchResult(
    val coarseData: Map<String, List<Double>>,
    val rootEstimates: List<Double>,              // Множество оценок корней
    val chordIntervals: List<Pair<Double, Double>> // Множество интервалов
)


// Метод хорд (с исправлением области видимости переменных)
fun chordMethod(
    x0: Double,
    x1: Double,
    aVal: Double,
    bVal: Double,
    eps: Double = 0.001
): Pair<Double?, Int> {
    var a = x0
    var b = x1

    var fa = f(a, aVal, bVal) ?: return null to 0
    var fb = f(b, aVal, bVal) ?: return null to 0

    if (fa * fb > 0) return null to 0

    var iterations = 0
    val maxIterations = 1000
    var c: Double = Double.NaN
    var fc: Double? = null
    var prevC = Double.MAX_VALUE

    do {
        c = (a * fb - b * fa) / (fb - fa)
        fc = f(c, aVal, bVal) ?: return null to 0

        // Критерии остановки
        if (abs(fc) < eps && abs(c - prevC) < eps) {
            return c to iterations
        }

        if (fa * fc < 0) {
            b = c
            fb = fc
        } else {
            a = c
            fa = fc
        }

        prevC = c
        iterations++
    } while (iterations < maxIterations)

    // Исправление: проверяем последнее вычисленное значение
    return if (abs(fc) < eps) c to iterations else null to 0
}

// Метод Ньютона
fun newtonMethod(
    x0: Double,
    aVal: Double,
    bVal: Double,
    eps: Double = 0.001
): Pair<Double?, Int> {
    var x = x0
    var iterations = 0
    val maxIterations = 1000
    var prevX = Double.MAX_VALUE

    do {
        val fx = f(x, aVal, bVal) ?: return null to 0
        val dfx = df(x, aVal, bVal) ?: return null to 0

        if (abs(dfx) < 1e-15) return null to 0

        val delta = fx / dfx
        x -= delta
        iterations++

        // Критерии остановки
        if (abs(delta) < eps && abs(fx) < eps) {
            return x to iterations
        }
        if (abs(x - prevX) < 1e-15) break

        prevX = x
    } while (iterations < maxIterations)

    return if (abs(f(x, aVal, bVal) ?: Double.MAX_VALUE) < eps)
        x to iterations else null to 0
}

// Создание увеличенного графика (обновленная)
private fun createZoomedPlot(
    allRoots: List<Double>,           // Все корни
    aVal: Double,
    bVal: Double,
    chordRoots: List<Double>,
    newtonRoots: List<Double>,
    chordIntervals: List<Pair<Double, Double>>?,
    newtonInitials: List<Double>?
): Figure? {
    if (allRoots.isEmpty()) return null

    val padding = 0.1
    val minRoot = allRoots.minOrNull() ?: 0.0
    val maxRoot = allRoots.maxOrNull() ?: 0.0
    val viewMinX = minRoot - padding
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

    return createPlot(
        data = mapOf("x" to xValues, "y" to yValues),
        chordRoots = chordRoots,
        newtonRoots = newtonRoots,
        aVal = aVal,
        bVal = bVal,
        title = "f(x) = √(${aVal}x) - cos(${bVal}x) (увеличенный вид)",
        chordIntervals = chordIntervals,
        newtonInitials = newtonInitials
    )
}