import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlin.math.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    var a by remember { mutableStateOf("1.0") }
    var b by remember { mutableStateOf("1.0") }
    var chordRoot by remember { mutableStateOf(0.0) }
    var newtonRoot by remember { mutableStateOf(0.0) }
    var error by remember { mutableStateOf("") }
    var points by remember { mutableStateOf(listOf<Pair<Double, Double>>()) }
    var visibleMinX by remember { mutableStateOf(0.0) }
    var visibleMaxX by remember { mutableStateOf(10.0) }
    var isDragging by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf(0f) }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input fields
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Уравнение: √(ax) - cos(bx) = 0")
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("a:")
                OutlinedTextField(
                    value = a,
                    onValueChange = { a = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(100.dp)
                )

                Text("b:")
                OutlinedTextField(
                    value = b,
                    onValueChange = { b = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(100.dp)
                )

                Button(onClick = {
                    try {
                        val aVal = a.toDouble()
                        val bVal = b.toDouble()

                        if (aVal <= 0) {
                            error = "a должен быть положительным"
                            return@Button
                        }

                        // Calculate function points
                        val newPoints = mutableListOf<Pair<Double, Double>>()
                        for (x in 0..200) {
                            val xVal = visibleMinX + (x / 200.0) * (visibleMaxX - visibleMinX)
                            val yVal = f(xVal, aVal, bVal)
                            newPoints.add(Pair(xVal, yVal))
                        }
                        points = newPoints

                        // Find intervals with roots
                        val intervals = findIntervals(aVal, bVal, visibleMinX, visibleMaxX, 0.1)
                        if (intervals.isEmpty()) {
                            error = "Корни не найдены в видимой области"
                            return@Button
                        }

                        // Solve using methods
                        val (left, right) = intervals[0]
                        chordRoot = chordMethod(left, right, aVal, bVal, 0.001)
                        newtonRoot = newtonMethod((left + right) / 2, aVal, bVal, 0.001)
                        error = ""
                    } catch (e: Exception) {
                        error = "Ошибка: ${e.message}"
                    }
                }) {
                    Text("Решить")
                }
            }

            // Error and results display
            if (error.isNotEmpty()) {
                Text(error, color = Color.Red)
            } else {
                Column {
                    Text("Метод хорд: x = ${"%.4f".format(chordRoot)}")
                    Text("Метод касательных: x = ${"%.4f".format(newtonRoot)}")
                }
            }

            // Graphing area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .onPointerEvent(PointerEventType.Press) {
                        isDragging = true
                        dragStart = it.changes.first().position.x
                    }
                    .onPointerEvent(PointerEventType.Release) {
                        isDragging = false
                    }
                    .onPointerEvent(PointerEventType.Move) {
                        if (isDragging) {
                            val dragEnd = it.changes.first().position.x
                            val dragDelta = (dragEnd - dragStart) / 100f
                            visibleMinX -= dragDelta.toDouble()
                            visibleMaxX -= dragDelta.toDouble()
                            dragStart = dragEnd
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawGraph(points, chordRoot, newtonRoot, visibleMinX, visibleMaxX)
                }
            }
        }
    }
}

// Mathematical functions
fun f(x: Double, a: Double, b: Double): Double {
    return sqrt(a * x) - cos(b * x)
}

fun derivative(x: Double, a: Double, b: Double): Double {
    return a / (2 * sqrt(a * x)) + b * sin(b * x)
}

fun findIntervals(
    a: Double,
    b: Double,
    start: Double,
    end: Double,
    step: Double
): List<Pair<Double, Double>> {
    val intervals = mutableListOf<Pair<Double, Double>>()
    var x1 = start
    var f1 = f(x1, a, b)

    var x2 = x1 + step
    while (x2 <= end) {
        val f2 = f(x2, a, b)
        if (f1.sign != f2.sign) {
            intervals.add(Pair(x1, x2))
        }
        x1 = x2
        f1 = f2
        x2 += step
    }
    return intervals
}

fun chordMethod(
    x0: Double,
    x1: Double,
    a: Double,
    b: Double,
    eps: Double
): Double {
    var a0 = x0
    var b0 = x1
    var c: Double

    do {
        val fa = f(a0, a, b)
        val fb = f(b0, a, b)
        c = (a0 * fb - b0 * fa) / (fb - fa)
        val fc = f(c, a, b)

        if (fa * fc < 0) {
            b0 = c
        } else {
            a0 = c
        }
    } while (abs(b0 - a0) > eps)

    return c
}

fun newtonMethod(
    x0: Double,
    a: Double,
    b: Double,
    eps: Double
): Double {
    var x = x0
    var delta: Double

    do {
        val fx = f(x, a, b)
        val dfx = derivative(x, a, b)
        if (abs(dfx) < 1e-10) break
        delta = fx / dfx
        x -= delta
    } while (abs(delta) > eps)

    return x
}

private fun DrawScope.drawGraph(
    points: List<Pair<Double, Double>>,
    chordRoot: Double,
    newtonRoot: Double,
    minX: Double,
    maxX: Double
) {
    if (points.isEmpty()) return

    // Calculate min/max Y values
    val minY = points.minOf { it.second }
    val maxY = points.maxOf { it.second }
    val yRange = maxY - minY

    // Convert to Float for drawing
    val zeroXFloat = if (minX <= 0.0 && maxX >= 0.0) {
        ((-minX) / (maxX - minX) * size.width).toFloat()
    } else 0f

    val zeroYFloat = if (minY <= 0.0 && maxY >= 0.0) {
        ((1 - (0 - minY) / yRange) * size.height).toFloat()
    } else size.height

    // Draw axes
    drawLine(
        color = Color.Black,
        start = Offset(zeroXFloat, 0f),
        end = Offset(zeroXFloat, size.height),
        strokeWidth = 2f
    )

    drawLine(
        color = Color.Black,
        start = Offset(0f, zeroYFloat),
        end = Offset(size.width, zeroYFloat),
        strokeWidth = 2f
    )

    // Draw grid
    for (i in 1..5) {
        val xPos = (i * size.width / 5).toFloat()
        drawLine(
            color = Color.LightGray,
            start = Offset(xPos, 0f),
            end = Offset(xPos, size.height),
            strokeWidth = 1f
        )
    }

    // Draw function plot
    for (i in 0 until points.size - 1) {
        val x1 = ((points[i].first - minX) / (maxX - minX) * size.width).toFloat()
        val y1 = (size.height - (points[i].second - minY) / yRange * size.height).toFloat()
        val x2 = ((points[i+1].first - minX) / (maxX - minX) * size.width).toFloat()
        val y2 = (size.height - (points[i+1].second - minY) / yRange * size.height).toFloat()

        drawLine(
            color = Color.Blue,
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = 2f
        )
    }

    // Draw roots
    val rootX = ((chordRoot - minX) / (maxX - minX) * size.width).toFloat()
    val rootY = (size.height - (f(chordRoot, 1.0, 1.0) - minY) / yRange * size.height).toFloat()
    drawCircle(
        color = Color.Red,
        radius = 8f,
        center = Offset(rootX, rootY),
        style = Stroke(width = 2f)
    )

    val newtonX = ((newtonRoot - minX) / (maxX - minX) * size.width).toFloat()
    val newtonY = (size.height - (f(newtonRoot, 1.0, 1.0) - minY) / yRange * size.height).toFloat()
    drawCircle(
        color = Color.Green,
        radius = 8f,
        center = Offset(newtonX, newtonY),
        style = Stroke(width = 2f)
    )
}

fun main() = application {
    Window(
        title = "Решение нелинейных уравнений",
        onCloseRequest = ::exitApplication
    ) {
        App()
    }
}