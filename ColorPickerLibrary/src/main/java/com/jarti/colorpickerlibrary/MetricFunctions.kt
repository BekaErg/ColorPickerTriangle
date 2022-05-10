package com.jarti.ColorPickerView

import kotlin.math.absoluteValue
import kotlin.math.sqrt

class MetricFunctions {
    companion object {
        fun distanceR2(x0 : Float, y0 : Float, x1 : Float, y1 : Float) : Float {
            return sqrt((x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 - y1))
        }

        fun distanceToLine(
            pX : Double,
            pY : Double,
            a : Pair<Double, Double>,
            b : Pair<Double, Double>
        ) : Double {
            val numerator =
                (b.first - a.first) * (a.second - pY) - (a.first - pX) * (b.second - a.second)
            val denominator =
                (b.first - a.first) * (b.first - a.first) + (b.second - a.second) * (b.second - a.second)
            return if (denominator != 0.0) {
                numerator.absoluteValue / sqrt(denominator)
            } else {
                0.0
            }
        }

        fun distanceToLine(
            pX : Float,
            pY : Float,
            a : Pair<Float, Float>,
            b : Pair<Float, Float>
        ) : Float {
            val numerator =
                (b.first - a.first) * (a.second - pY) - (a.first - pX) * (b.second - a.second)
            val denominator =
                (b.first - a.first) * (b.first - a.first) + (b.second - a.second) * (b.second - a.second)
            return if (denominator != 0.0f) {
                numerator.absoluteValue / sqrt(denominator)
            } else {
                0.0f
            }
        }

        fun mirror(x: Float, y: Float, centerX: Float, centerY: Float): Pair<Float, Float> {
            return Pair(2 * centerX - x, 2 * centerY - y)
        }
    }


}