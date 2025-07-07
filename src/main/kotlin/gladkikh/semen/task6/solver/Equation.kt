package gladkikh.semen.task6.solver

// Математические функции

import kotlin.math.*

object Equation {
    // Вычисление значения функции √(a*x) - cos(b*x)
    fun f(x: Double, aVal: Double, bVal: Double): Double? {
        // Для a=0 функция определена везде
        if (aVal == 0.0) return -cos(bVal * x)

        val product = aVal * x
        // Разрешаем отрицательные значения при a < 0 и x < 0
        if (product < 0) return null

        return try {
            sqrt(aVal * x) - cos(bVal * x)
        } catch (e: Exception) {
            null
        }
    }

    // Вычисление производной функции
    fun df(x: Double, aVal: Double, bVal: Double): Double? {
        // Для a=0 производная = b*sin(b*x)
        if (aVal == 0.0) return bVal * sin(bVal * x)

        val product = aVal * x
        // Производная требует строго положительного произведения
        if (product <= 0) return null

        return try {
            (aVal / (2 * sqrt(aVal * x))) + bVal * sin(bVal * x)
        } catch (e: Exception) {
            null
        }
    }
}