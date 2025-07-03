package gladkikh.semen.task6.solver

// Алгоритмы поиска корней

import kotlin.math.*
import gladkikh.semen.task6.ui.Plotter.createPlot
import org.jetbrains.letsPlot.Figure

// Результаты вычислений
data class RootResult(
    val chordRoot: Double?,
    val newtonRoot: Double?,
    val chordIterations: Int,
    val newtonIterations: Int,
    val chordInterval: Pair<Double, Double>?,
    val newtonInitial: Double?,
    val fullPlot: Figure?,
    val zoomedPlot: Figure?,
    val fValueChord: Double?,
    val fValueNewton: Double?,
    val rootsTooClose: Boolean,
    val rootDifference: Double
)

// Основная функция вычисления корней
fun computeRoots(
    a: String,
    b: String,
    callback: (RootResult?, String) -> Unit
) {
    try {
        val normalizedA = a.replace(',', '.')
        val normalizedB = b.replace(',', '.')
        val aVal = normalizedA.toDoubleOrNull()
        val bVal = normalizedB.toDoubleOrNull()

        // Валидация параметров
        if (aVal == null || bVal == null) {
            callback(null, "Ошибка: параметры должны быть числами")
            return
        }

        if (aVal <= 0) {
            callback(null, "Ошибка: параметр 'a' должен быть положительным")
            return
        }

        // Поиск интервала с корнем
        val maxXForSearch = min(10.0 * PI / abs(bVal), 100.0)
        val searchResult = findRootInterval(aVal, bVal, maxXForSearch)
        val rootEstimate = searchResult.rootEstimate
        val chordInterval = searchResult.chordInterval

        if (rootEstimate == null || chordInterval == null) {
            callback(null, "Не удалось найти корни на интервале [0, ${"%.2f".format(maxXForSearch)}]")
            return
        }

        // Вычисление корней
        val chordResult = chordMethod(chordInterval.first, chordInterval.second, aVal, bVal)
        val newtonResult = newtonMethod(rootEstimate, aVal, bVal)

        if (chordResult.first == null || newtonResult.first == null) {
            callback(null, "Ошибка в вычислении корней")
            return
        }

        // Расчет значений функций в найденных корнях
        val fValueChord = Equation.f(chordResult.first!!, aVal, bVal)
        val fValueNewton = Equation.f(newtonResult.first!!, aVal, bVal)

        // Проверка близости корней
        val rootDifference = abs(chordResult.first!! - newtonResult.first!!)
        val rootsTooClose = rootDifference < 0.001

        // Построение графиков
        val fullPlot = createPlot(
            searchResult.coarseData,
            chordResult.first,
            newtonResult.first,
            aVal,
            bVal,
            "f(x) = √(${aVal}x) - cos(${bVal}x) (общий вид)",
            chordInterval,
            rootEstimate
        )

        val zoomedPlot = createZoomedPlot(
            chordResult.first!!,
            newtonResult.first!!,
            aVal,
            bVal,
            chordResult.first,
            newtonResult.first,
            chordInterval,
            rootEstimate
        )

        // Возврат результатов
        callback(RootResult(
            chordRoot = chordResult.first,
            newtonRoot = newtonResult.first,
            chordIterations = chordResult.second,
            newtonIterations = newtonResult.second,
            chordInterval = chordInterval,
            newtonInitial = rootEstimate,
            fullPlot = fullPlot,
            zoomedPlot = zoomedPlot,
            fValueChord = fValueChord,
            fValueNewton = fValueNewton,
            rootsTooClose = rootsTooClose,
            rootDifference = rootDifference
        ), "")
    } catch (e: Exception) {
        callback(null, "Произошла ошибка: ${e.message}")
    }
}

// Поиск интервала с корнем
private fun findRootInterval(
    aVal: Double,
    bVal: Double,
    maxX: Double
): SearchResult {
    val points = 1000
    val xValues = mutableListOf<Double>()
    val yValues = mutableListOf<Double>()
    var rootEstimate: Double? = null
    var chordInterval: Pair<Double, Double>? = null

    for (i in 0 until points) {
        val x = i * maxX / points
        val y = Equation.f(x, aVal, bVal)
        if (y != null) {
            xValues.add(x)
            yValues.add(y)

            // Поиск смены знака (признак корня)
            if (i > 0 && yValues[i-1] * y < 0) {
                rootEstimate = (xValues[i-1] + x) / 2
                chordInterval = xValues[i-1] to x
                break
            }
        }
    }

    return SearchResult(
        coarseData = mapOf("x" to xValues, "y" to yValues),
        rootEstimate = rootEstimate,
        chordInterval = chordInterval
    )
}

// Результат поиска интервала
private data class SearchResult(
    val coarseData: Map<String, List<Double>>,
    val rootEstimate: Double?,
    val chordInterval: Pair<Double, Double>?
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

    var fa = Equation.f(a, aVal, bVal) ?: return null to 0
    var fb = Equation.f(b, aVal, bVal) ?: return null to 0

    if (fa * fb > 0) return null to 0

    var iterations = 0
    val maxIterations = 1000
    var c: Double = Double.NaN
    var fc: Double? = null
    var prevC = Double.MAX_VALUE

    do {
        c = (a * fb - b * fa) / (fb - fa)
        fc = Equation.f(c, aVal, bVal) ?: return null to 0

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
    var x = max(x0, 1e-10)
    var iterations = 0
    val maxIterations = 1000
    var prevX = Double.MAX_VALUE

    do {
        val fx = Equation.f(x, aVal, bVal) ?: return null to 0
        val dfx = Equation.df(x, aVal, bVal) ?: return null to 0

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

    return if (abs(Equation.f(x, aVal, bVal) ?: Double.MAX_VALUE) < eps)
        x to iterations else null to 0
}

// Создание увеличенного графика (ИСПРАВЛЕННАЯ ВЕРСИЯ)
private fun createZoomedPlot(
    chordRoot: Double,
    newtonRoot: Double,
    aVal: Double,
    bVal: Double,
    chordRootPt: Double?,
    newtonRootPt: Double?,
    chordInterval: Pair<Double, Double>?,
    newtonInitial: Double?
): Figure? {
    val minRoot = min(chordRoot, newtonRoot)
    val maxRoot = max(chordRoot, newtonRoot)
    val range = maxRoot - minRoot

    // Адаптивный padding в зависимости от размера корня
    val padding = if (minRoot > 0.1) 0.1 else min(0.05, minRoot * 0.5)
    val viewMinX = max(0.0, minRoot - padding)
    val viewMaxX = maxRoot + padding

    val points = 500
    val xValues = mutableListOf<Double>()
    val yValues = mutableListOf<Double>()

    for (i in 0 until points) {
        val x = viewMinX + i * (viewMaxX - viewMinX) / points
        val y = Equation.f(x, aVal, bVal)
        if (y != null) {
            xValues.add(x)
            yValues.add(y)
        }
    }

    return createPlot(
        data = mapOf("x" to xValues, "y" to yValues),
        chordRoot = chordRootPt,
        newtonRoot = newtonRootPt,
        aVal = aVal,
        bVal = bVal,
        title = "f(x) = √(${aVal}x) - cos(${bVal}x) (увеличенный вид)",
        chordInterval = chordInterval,
        newtonInitial = newtonInitial
    )
}