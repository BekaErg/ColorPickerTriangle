package com.jarti.ColorPickerView

import android.graphics.*
import kotlin.math.atan2

class Thumb(thumbColor : Int, var radius : Float = 38f, thumbStyle : Int = DEFAULT_THUMB_STYLE) {
    var posX : Float = 0f
    var posY : Float = 0f
    private var thickness = 10f
        set(value) {
            field = value
            mStrokePaint.strokeWidth = thickness
            shadowDistance = thickness / 2
        }
    private var shadowDistance = thickness / 2
    private var mPath = Path()

    var fillColor : Int
        get() = mFillPaint.color
        set(value) {
            mFillPaint.color = value
        }

    private val mStrokePaint = Paint().apply {
        isAntiAlias = true
        color = 0xFFFFFFFF.toInt()
        strokeWidth = thickness
        style = Paint.Style.STROKE
    }
    private val mFillPaint = Paint().apply {
        isAntiAlias = true
        color = 0x00FFFFFF
    }
    private val mShadowPaint = Paint().apply {
        isAntiAlias = true
        color = 0x33000000
        strokeWidth = thickness
        style = Paint.Style.STROKE
        maskFilter = BlurMaskFilter(5f, BlurMaskFilter.Blur.NORMAL)
    }
    private val stripePaint = Paint().apply {
        isAntiAlias = true
        color = 0xFFFFFFFF.toInt()
        alpha = 120
        strokeWidth = thickness
        style = Paint.Style.STROKE
    }

    init {
        when (thumbStyle) {
            RGB_SLIDER_THUMB_STYLE -> {
                this.setColors(thumbColor, 0, 0x00000000)
            }
            SAT_VAL_THUMB_STYLE -> {
                thickness = radius / 8
                setColors(Color.WHITE, 0xFFFFFFFF.toInt(), 0x55000000)
            }
            HUE_THUMB_STYLE -> {
                setColors(0x99FFFFFF.toInt(), thumbColor, 0x55000000)
            }
            HUE_THUMB_MIRROR_STYLE -> {
                thickness = radius / 7
                setColors(thumbColor, 0x22000000, 0x00000000)
            }
        }
    }


    fun setColors(center : Int, ring : Int, shadow : Int = 0x88000000.toInt()) {
        mFillPaint.color = center
        mStrokePaint.color = ring
        stripePaint.color = ring
        mShadowPaint.color = shadow
    }

    fun setPosition(x : Float, y : Float) {
        posY = y
        posX = x
    }

    fun setPosition(pair : Pair<Float, Float>) {
        posY = pair.second
        posX = pair.first
    }

    fun contains(x : Float, y : Float) : Boolean {
        return MetricFunctions.distanceR2(x, y, posX, posY) < radius + thickness
    }

    fun contains(pair : Pair<Float, Float>) : Boolean {
        return MetricFunctions.distanceR2(pair.first, pair.second, posX, posY) < radius + thickness
    }

    fun drawThumb(canvas : Canvas) {
        mPath.reset()
        mPath.addCircle(posX + shadowDistance, posY + shadowDistance, radius, Path.Direction.CCW)
        canvas.drawPath(mPath, mShadowPaint)

        mPath.reset()
        mPath.addCircle(posX, posY, radius - thickness / 2, Path.Direction.CCW)
        canvas.drawPath(mPath, mFillPaint)

        mPath.reset()
        mPath.addCircle(posX, posY, radius, Path.Direction.CCW)
        canvas.drawPath(mPath, mStrokePaint)
    }

    fun drawThumbStripe(
        canvas : Canvas,
        centerX : Float,
        centerY : Float,
        innerRadius : Float,
        outRadius : Float
    ) {
        mPath.reset()
        val shadow = Path()

        val width = (outRadius - innerRadius) / 2
        val height = width / 2
        val alpha = atan2(posY - centerY, posX - centerX) * 180f / Math.PI

        val rotate = Matrix().apply { setRotate(alpha.toFloat(), posX, posY) }
        val shift = Matrix().apply { setTranslate(shadowDistance, shadowDistance) }

        mPath.addRoundRect(
            posX - width,
            posY + height,
            posX + width,
            posY - height,
            5f,
            3f,
            Path.Direction.CW
        )
        mPath.transform(rotate)
        shadow.addPath(mPath)
        shadow.transform(shift)

        canvas.drawPath(shadow, mShadowPaint)
        canvas.drawPath(mPath, stripePaint)
    }

    companion object {
        const val RGB_SLIDER_THUMB_STYLE = 0
        const val SAT_VAL_THUMB_STYLE = 1
        const val HUE_THUMB_STYLE = 2
        const val DEFAULT_THUMB_STYLE = 3
        const val HUE_THUMB_MIRROR_STYLE = 4
    }

}