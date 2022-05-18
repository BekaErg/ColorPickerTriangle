package com.jarti.ColorPickerView

import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class ColorLab(inputColor: Int) {
    private val hueSatVal: FloatArray = FloatArray(3)
    var hue
        get() = hueSatVal[0]
        set(value) {
            hueSatVal[0] = value
        }
    var saturation
        get() = hueSatVal[1]
        set(value) {
            hueSatVal[1] = value
        }
    var brightness
        get() = hueSatVal[2]
        set(value) {
            hueSatVal[2] = value
        }

    val chroma: Int
        get() = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))

    var alpha: Int
    var red: Int
        get() = (color shr 16) and 0xFF
        set(value) {
            this.color = Color.rgb(value, this.green, this.blue)
        }
    var green: Int
        get() = (color shr 8) and 0xFF
        set(value) {
            this.color = Color.rgb(this.red, value, this.blue)
        }
    var blue: Int
        get() = (color shr 0) and 0xFF
        set(value) {
            this.color = Color.rgb(this.red, this.green, value)
        }

    var color
        get() = Color.HSVToColor(hueSatVal)
        set(value) {
            Color.colorToHSV(value, hueSatVal)
            alpha = value shr 24
        }

    val paintMain = Paint().apply{
        isAntiAlias = true
    }
        get() {
            field.color = this.color
            return field
        }

    val paintChroma = Paint().apply{
        isAntiAlias = true
    }
        get() {
            field.color = this.chroma
            return field
        }

    val paintShadow = Paint().apply{
        isAntiAlias = true
        color = 0x00000000
        maskFilter = BlurMaskFilter(5f, BlurMaskFilter.Blur.NORMAL)
    }

    //constructor
    init{
        Color.colorToHSV(inputColor, hueSatVal)
        paintChroma.color = this.color
        paintMain.color = this.chroma
        alpha = inputColor shr 24
    }

    fun setColorLockHue(c : Int) {
        val temp = hue
        this.color = c
        this.hue = temp
    }

    fun getRingXY(radius: Float): Pair<Float, Float> {
        val hueRadian = hue * Math.PI / 180f
        val x = cos(hueRadian) * radius
        val y = sin(hueRadian) * radius
        return Pair(x.toFloat(), y.toFloat())
    }

    fun setHueFromXY(x : Float, y : Float){
        var hue = atan2(y, x)
        if (hue < 0) {
            hue += 2 * Math.PI.toFloat()
        }
        this.hue = 180f * hue / Math.PI.toFloat()
    }
}