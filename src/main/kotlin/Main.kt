import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import gladkikh.semen.task6.ui.App

fun main() = application {
    System.setProperty("java.awt.headless", "false")

    Window(
        onCloseRequest = ::exitApplication,
        title = "Решение уравнения √(ax) - cos(bx) = 0"
    ) {
        App()
    }
}