package com.jarti.ColorPickerView

import android.graphics.*

class RgbSlider(val leftX: Float, val rightX: Float, val y: Float) {

    fun colorToX (color: Int): Float {
        val x = leftX +  (rightX - leftX) * color / 255
        return x
    }

    fun colorChannelVal(x: Float): Int {
        val xx = x.coerceAtLeast(leftX).coerceAtMost(rightX)
        return ( (xx - leftX)/ (rightX - leftX) * 255).toInt()
    }

    var colorStart = 0x88000000.toInt()
        set(value) {
            field = value
            setGrad()
        }
    var colorEnd = Color.WHITE
        set(value) {
            field = value
            setGrad()
        }

    var height = 8f
    var cornerRadius = 5f
    var mPath = Path()
    var contourWidth = 2f
        set(value) {
            field = value
            mStrokePaint.strokeWidth = value
        }


    var mPaintGrad = Paint()
    var mStrokePaint = Paint()

    private fun setGrad() {
        val grad = LinearGradient(leftX, y, rightX, y, intArrayOf(colorStart, colorEnd), null, Shader.TileMode.CLAMP )
        mPaintGrad.shader = grad
    }

    init {
        mStrokePaint.isAntiAlias = true
        mStrokePaint.isDither = true
        mStrokePaint.style = Paint.Style.STROKE
        mStrokePaint.strokeWidth = contourWidth
        mStrokePaint.color = 0xAA888888.toInt()
        mPaintGrad.isAntiAlias = true
        mPaintGrad.isDither = true
        setGrad()
    }

    fun drawSlider(canvas: Canvas) {
        mPath.reset()
        mPath.addRoundRect(leftX, y - height/2, rightX, y + height/2, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.drawPath(mPath, mPaintGrad)
    }

}