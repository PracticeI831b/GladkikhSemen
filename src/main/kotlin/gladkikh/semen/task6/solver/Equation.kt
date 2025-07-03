package gladkikh.semen.task6.solver

// Математические функции

import kotlin.math.*

object Equation {
    // Вычисление значения функции √(a*x) - cos(b*x)
    fun f(x: Double, aVal: Double, bVal: Double): Double? {
        if (x < 0) return null
        return try {
            sqrt(aVal * x) - cos(bVal * x)
        } catch (e: Exception) {
            null
        }
    }

    // Вычисление производной функции
    fun df(x: Double, aVal: Double, bVal: Double): Double? {
        if (x <= 0) return null
        return try {
            (aVal / (2 * sqrt(aVal * x))) + bVal * sin(bVal * x)
        } catch (e: Exception) {
            null
        }
    }
}