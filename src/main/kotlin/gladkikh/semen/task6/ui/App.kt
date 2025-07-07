package gladkikh.semen.task6.ui

// Основной UI компонент

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.material.icons.filled.Calculate
//import androidx.compose.material.icons.filled.ZoomIn
//import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gladkikh.semen.task6.solver.RootResult
import gladkikh.semen.task6.solver.computeRoots
import org.jetbrains.letsPlot.Figure

@Composable
fun App() {
    // Состояния приложения
    var a by remember { mutableStateOf("1.0") }
    var b by remember { mutableStateOf("1.0") }
    var results by remember { mutableStateOf<RootResult?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isZoomed by remember { mutableStateOf(true) }

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

            // Параметры точности
            Text(
                text = "Точность вычисления корней: 0.001",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Поля ввода и кнопка
            InputPanel(
                a = a,
                b = b,
                isLoading = isLoading,
                onAChanged = { a = it },
                onBChanged = { b = it },
                onCalculate = {
                    isLoading = true
                    errorMessage = ""
                    results = null
                    computeRoots(a, b) { result, error ->
                        results = result
                        errorMessage = error
                        isLoading = false
                    }
                }
            )

            // Отображение ошибок
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Отображение результатов
            results?.let { rootResult ->
                ResultsCard(
                    rootResult = rootResult
                )
            }

            // Управление графиком
            if (results != null) {
                GraphControls(
                    isZoomed = isZoomed,
                    onZoomToggle = { isZoomed = !isZoomed }
                )
            }

            // Область графика
            GraphArea(
                results = results,
                isZoomed = isZoomed,
                isLoading = isLoading
            )
        }
    }
}

@Composable
private fun InputPanel(
    a: String,
    b: String,
    isLoading: Boolean,
    onAChanged: (String) -> Unit,
    onBChanged: (String) -> Unit,
    onCalculate: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        OutlinedTextField(
            value = a,
            onValueChange = onAChanged,
            label = { Text("Параметр a (любое число)") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("1.0 или 1,0") },
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.width(16.dp))

        OutlinedTextField(
            value = b,
            onValueChange = onBChanged,
            label = { Text("Параметр b (любое число)") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("1.0 или 1,0") },
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.width(16.dp))

        Button(
            onClick = onCalculate,
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
}

@Composable
private fun ResultsCard(
    rootResult: RootResult
) {
    Card(
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Показываем предупреждение если оно есть
            if (rootResult.warning.isNotEmpty()) {
                Text(
                    text = rootResult.warning,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text(
                text = "Результаты вычислений:",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Информация о количестве корней
            Text("Найдено корней: ${rootResult.allRoots.size}")
            Spacer(modifier = Modifier.height(8.dp))

            // Результаты для каждого корня
            rootResult.chordRoots.forEachIndexed { index, root ->
                Text("Корень #${index + 1}:")
                Text("  Метод хорд: ${"%.5f".format(root)} (итераций: ${rootResult.chordIterations.getOrNull(index)})")
                rootResult.fValuesChord.getOrNull(index)?.let {
                    Text("  Значение функции: ${"%.7f".format(it)}")
                }

                rootResult.newtonRoots.getOrNull(index)?.let { newtonRoot ->
                    Text("  Метод касательных: ${"%.5f".format(newtonRoot)} (итераций: ${rootResult.newtonIterations.getOrNull(index)})")
                    rootResult.fValuesNewton.getOrNull(index)?.let {
                        Text("  Значение функции: ${"%.7f".format(it)}")
                    }
                }

                rootResult.chordIntervals.getOrNull(index)?.let {
                    Text("  Интервал для хорд: [${"%.5f".format(it.first)}, ${"%.5f".format(it.second)}]")
                }

                rootResult.newtonInitials.getOrNull(index)?.let {
                    Text("  Начальное приближение Ньютона: ${"%.5f".format(it)}")
                }

                // Предупреждение о близких корнях для этой пары
                if (index < rootResult.rootsTooClose.size && rootResult.rootsTooClose[index]) {
                    Text(
                        text = "  Примечание: корни методов очень близки (разница: ${"%.6f".format(rootResult.rootDifferences[index])})",
                        color = Color(0xFFFF9800),
                        style = MaterialTheme.typography.caption
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun GraphControls(
    isZoomed: Boolean,
    onZoomToggle: () -> Unit
) {
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
            onClick = onZoomToggle,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (isZoomed) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
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

@Composable
private fun GraphArea(
    results: RootResult?,
    isZoomed: Boolean,
    isLoading: Boolean
) {
    Card(
        elevation = 8.dp,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        when {
            isLoading -> LoadingIndicator()
            results == null -> Placeholder("Введите параметры и нажмите 'Вычислить'")
            else -> PlotDisplay(
                plot = if (isZoomed) results.zoomedPlot else results.fullPlot,
                rootResult = results
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun Placeholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            fontSize = 18.sp
        )
    }
}

@Composable
private fun PlotDisplay(plot: Figure?, rootResult: RootResult?) {
    if (plot == null) {
        Placeholder("График не доступен")
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Plotter.PlotPanel(figure = plot)

            // Общее предупреждение если есть близкие корни
            if (rootResult?.rootsTooClose?.any { it } == true) {
                WarningBanner("Некоторые корни методов очень близки - точки могут перекрываться")
            }
        }
    }
}

@Composable
private fun WarningBanner(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.Red,
            fontWeight = FontWeight.Bold
        )
    }
}