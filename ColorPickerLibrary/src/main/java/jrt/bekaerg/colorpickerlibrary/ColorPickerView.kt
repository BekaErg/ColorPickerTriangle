package com.jarti.ColorPickerView

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnLayout
import kotlin.math.sqrt

class ColorPickerView(context : Context, attrs : AttributeSet? = null) : View(context, attrs) {
    /**
     * Ratio of ring thickness and inner radius
     */
    var thicknessRatio = 5

    /**
     * Color of the hue thumb
     */
    var thumbColor = 0xFFDDDDDD.toInt()
    var padding = dpToPixel(5f)
    var mSliderThumbRadius = dpToPixel(10f)
    var mSatValThumbRadius = dpToPixel(10f)

    /**
     * Actual value of this variable is unused, it only exists for setter and getter
     * Once color is chosen, get function should be called to set it as previous color
     * Notice that when using listener, this will not happen automatically
     */
    var color : Int = 0
        set(value) {
            previousColor.color = currentColor.color
            currentColor.color = value
            field = value
            resetAccordingColor()
        }
        get() = run {
            previousColor.color = currentColor.color
            currentColor.color
        }

    private lateinit var mRadius : Radius
    private var mCenter = Pair(0f, 0f)
    private var rgbPanel = RectF()
    private var heightOverWidth = 1.4f
    private val mFrame = Path()

    //Triangle Vertices
    private lateinit var mVertexLT : Pair<Float, Float>
    private lateinit var mVertexLB : Pair<Float, Float>
    private lateinit var vertexRM : Pair<Float, Float>

    //Bitmaps and shapes
    private lateinit var mChromaTriangle : Path
    private lateinit var mHueRing : Bitmap
    private lateinit var mHueSatGradientFilter : Bitmap
    private lateinit var mShadowPath : Path
    private var contourPath = Path()

    //Colors and Paints
    private var previousColor = ColorLab(0xFF2222AA.toInt())
    private var currentColor = ColorLab(0xFF22AA22.toInt())

    private var mTouchLocation = TouchLocation.OUTSIDE
    private var mContourPaint = Paint().apply {
        isAntiAlias = true
        //TODO get rid of hardcoding
        color = 0xAA555555.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeJoin = Paint.Join.ROUND
    }
    private val mPaintText = Paint().apply {
        isAntiAlias = true
        textSize = mSatValThumbRadius
        textAlign = Paint.Align.CENTER
        color = thumbColor
        alpha = 220
        //textAlignment = TEXT_ALIGNMENT_TEXT_START
    }

    private var mColorChangeClosure : (previousColor : Int, currentColor : Int) -> Unit =
        { _, _ -> }

    //Thumbs
    private lateinit var hueThumb : Thumb
    private lateinit var satValThumb : Thumb
    private lateinit var hueThumbMirror : Thumb
    private lateinit var redThumb : Thumb
    private lateinit var greenThumb : Thumb
    private lateinit var blueThumb : Thumb

    //Sliders
    private lateinit var mRedSlider : RgbSlider
    private lateinit var mGreenSlider : RgbSlider
    private lateinit var mBlueSlider : RgbSlider

    init {
        doOnLayout {
            initDimensions()
            initVertexCoordinates()
            mChromaTriangle = initPathTriangle()

            contourPath.addCircle(mCenter.first, mCenter.second, mRadius.outer, Path.Direction.CCW)
            contourPath.addCircle(mCenter.first, mCenter.second, mRadius.inner, Path.Direction.CW)
            contourPath.addPath(mChromaTriangle)
            mHueRing = Palette.createHueRing(mRadius.outer, mRadius.inner)
            mHueSatGradientFilter = Palette.createSVTriangle(mRadius.inner)

            initSliders()
            initThumbs()
            initShadow()
            resetAccordingColor()
        }
    }

    /**
     * setter for colorChangeListener
     */
    @Suppress("unused")
    fun setOnColorChange(closure : (previousColor : Int, currentColor : Int) -> Unit) {
        mColorChangeClosure = closure
    }

    override fun onDraw(canvas : Canvas) {
        super.onDraw(canvas)
        mColorChangeClosure(previousColor.color, currentColor.color)

        if (this.background == null) {
            canvas.clipPath(mFrame)
            canvas.drawColor(0xFF333333.toInt())
        }

        canvas.drawPath(mShadowPath, currentColor.paintShadow)
        canvas.drawBitmap(
            mHueRing,
            mCenter.first - mRadius.outer,
            mCenter.second - mRadius.outer,
            null
        )
        canvas.drawPath(mChromaTriangle, currentColor.paintChroma)
        canvas.drawBitmap(
            mHueSatGradientFilter,
            mCenter.first - mRadius.inner,
            mCenter.second - mRadius.inner,
            null
        )
        canvas.drawPath(contourPath, mContourPaint)

        drawPanel(canvas)

        satValThumb.fillColor = currentColor.color
        hueThumb.drawThumbStripe(
            canvas,
            mCenter.first,
            mCenter.second,
            mRadius.inner,
            mRadius.outer
        )
        satValThumb.drawThumb(canvas)
        hueThumbMirror.drawThumb(canvas)
        redThumb.drawThumb(canvas)
        greenThumb.drawThumb(canvas)
        blueThumb.drawThumb(canvas)

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event : MotionEvent) : Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mTouchLocation = touchLocation(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
            }
            MotionEvent.ACTION_UP -> {
            }
            else -> return false
        }

        when (mTouchLocation) {
            TouchLocation.THUMB -> {
                val (pX, pY) = projectToTriangle(x, y)
                satValFromXY(pX, pY)
            }
            TouchLocation.TRIANGLE -> {
                val (pX, pY) = projectToTriangle(x, y)
                satValFromXY(pX, pY)
            }
            TouchLocation.RING -> currentColor.setHueFromXY(x - mCenter.first, y - mCenter.second)
            TouchLocation.RED_SLIDER -> currentColor.red = mRedSlider.colorChannelVal(x)
            TouchLocation.GREEN_SLIDER -> currentColor.green = mGreenSlider.colorChannelVal(x)
            TouchLocation.BLUE_SLIDER -> currentColor.blue = mBlueSlider.colorChannelVal(x)
            else -> {
            }
        }
        resetAccordingColor()
        return true
    }

    private fun initDimensions() {
        heightOverWidth =
            (height.toFloat() / width.toFloat()).coerceAtLeast(1.25f).coerceAtMost(1.45f)
        val frameH =
            (height.toFloat() - 2 * padding).coerceAtMost((width) * heightOverWidth - 2 * padding)
        val frameW = frameH / heightOverWidth
        val frameLeft = (width - frameW) / 2
        val frameTop = (height - frameH) / 2

        val panelHeight = frameH - frameW
        val outerRadius = (frameH - panelHeight).coerceAtMost(frameW) / 2
        val innerRadius = (thicknessRatio - 1) * outerRadius / thicknessRatio
        val midRadius = (innerRadius + outerRadius) / 2

        mFrame.addRoundRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            dpToPixel(5f),
            dpToPixel(5f),
            Path.Direction.CW
        )
        //mFrame.addRoundRect(frameLeft - padding, frameTop - padding, frameLeft + frameW + padding, frameTop + frameH + padding, dpToPixel(5f), dpToPixel(5f), Path.Direction.CW)
        mRadius = Radius(outerRadius, innerRadius, midRadius)
        mCenter = Pair(frameLeft + mRadius.outer, frameTop + mRadius.outer)
        //Panel is a rectangle containing rgb sliders
        rgbPanel = RectF(
            frameLeft + 0,
            frameTop + frameH - panelHeight,
            frameLeft + frameW,
            frameTop + frameH
        )
        mSliderThumbRadius = mSliderThumbRadius.coerceAtMost(panelHeight / 6f)
    }

    //Initialize coordinates of vertices of the satVal triangle
    private fun initVertexCoordinates() {
        mVertexLT = Pair(
            mCenter.first - 0.5f * mRadius.inner,
            mCenter.second - 0.5f * sqrt(3f) * mRadius.inner
        )
        mVertexLB = Pair(
            mCenter.first - 0.5f * mRadius.inner,
            mCenter.second + 0.5f * sqrt(3f) * mRadius.inner
        )
        vertexRM = Pair(mCenter.first + mRadius.inner, mCenter.second)
    }

    //Initialize triangle bounding satVal
    private fun initPathTriangle() : Path {
        val path = Path()

        path.moveTo(mVertexLT.first, mVertexLT.second) // Top
        path.lineTo(vertexRM.first, vertexRM.second)
        path.lineTo(mVertexLB.first, mVertexLB.second)
        path.lineTo(mVertexLT.first, mVertexLT.second) // Back to Top
        path.close()

        path.fillType = Path.FillType.EVEN_ODD
        return path
    }

    //Initialize RGB sliders
    private fun initSliders() {
        val stepSize = rgbPanel.height() / 4
        val curMid = rgbPanel.top + stepSize
        mPaintText.textSize = stepSize / 2

        val lx = rgbPanel.left + 2 * mSliderThumbRadius + mPaintText.textSize
        val rx = rgbPanel.right - 2 * mSliderThumbRadius

        mRedSlider = RgbSlider(lx, rx, curMid).apply {
            colorEnd = Color.RED
            height = mSliderThumbRadius / 2
        }
        mGreenSlider = RgbSlider(lx, rx, curMid + stepSize).apply {
            colorEnd = Color.GREEN
            height = mSliderThumbRadius / 2
        }
        mBlueSlider = RgbSlider(lx, rx, curMid + 2 * stepSize).apply {
            colorEnd = Color.BLUE
            height = mSliderThumbRadius / 2
        }
    }

    private fun initThumbs() {

        hueThumb = Thumb(thumbColor, 38f, Thumb.HUE_THUMB_STYLE)
        hueThumbMirror = Thumb(0x88FFFFFF.toInt(), dpToPixel(4f), Thumb.HUE_THUMB_MIRROR_STYLE)
        satValThumb = Thumb(Color.WHITE, mSatValThumbRadius, Thumb.SAT_VAL_THUMB_STYLE)

        redThumb = Thumb(thumbColor, mSliderThumbRadius, Thumb.RGB_SLIDER_THUMB_STYLE)
        greenThumb = Thumb(thumbColor, mSliderThumbRadius, Thumb.RGB_SLIDER_THUMB_STYLE)
        blueThumb = Thumb(thumbColor, mSliderThumbRadius, Thumb.RGB_SLIDER_THUMB_STYLE)

        redThumb.posY = mRedSlider.y
        greenThumb.posY = mGreenSlider.y
        blueThumb.posY = mBlueSlider.y
    }

    private fun initShadow() {
        mShadowPath = initPathTriangle()
        mShadowPath.addCircle(mCenter.first, mCenter.second, mRadius.inner, Path.Direction.CW)
        mShadowPath.addCircle(mCenter.first, mCenter.second, mRadius.outer, Path.Direction.CCW)
        val translateMatrix = Matrix()
        translateMatrix.setTranslate(7f, 5f)
        mShadowPath.transform(translateMatrix)
    }

    //Updates coordinates of Thumbs according to current color
    private fun resetAccordingColor() {
        val (a, b) = getHueXY()
        val (aM, bM) = MetricFunctions.mirror(a, b, mCenter.first, mCenter.second)
        hueThumb.setPosition(a, b)
        hueThumbMirror.setPosition(aM, bM)
        satValThumb.setPosition(getSatValXY())

        redThumb.posX = mRedSlider.colorToX(currentColor.red)
        greenThumb.posX = mGreenSlider.colorToX(currentColor.green)
        blueThumb.posX = mBlueSlider.colorToX(currentColor.blue)
        invalidate()
    }

    //Determines in what region was touch made
    private fun touchLocation(x : Float, y : Float) : TouchLocation {
        return when {
            satValThumb.contains(x, y) -> TouchLocation.THUMB
            isInTriangle(x, y) -> TouchLocation.TRIANGLE
            isInHueRing(x, y) -> TouchLocation.RING
            isInSlider(mRedSlider, x, y) -> TouchLocation.RED_SLIDER
            isInSlider(mGreenSlider, x, y) -> TouchLocation.GREEN_SLIDER
            isInSlider(mBlueSlider, x, y) -> TouchLocation.BLUE_SLIDER
            else -> TouchLocation.OUTSIDE
        }
    }

    private fun isInSlider(slider : RgbSlider, x : Float, y : Float) : Boolean {
        val r = blueThumb.radius
        if (y > slider.y + r || y < slider.y - r) return false
        if (x > slider.rightX + r || x < slider.leftX - r) return false
        return true
    }

    private fun isInHueRing(x : Float, y : Float) : Boolean {
        val d = MetricFunctions.distanceR2(x, y, mCenter.first, mCenter.second)
        return d < mRadius.outer && d > mRadius.inner
    }

    private fun isInTriangle(x : Float, y : Float) : Boolean {
        if (x < mVertexLB.first || x > vertexRM.first || y > projectY(
                x,
                mVertexLB,
                vertexRM
            ) || y < projectY(x, mVertexLT, vertexRM)
        ) return false
        return true
    }

    private fun getHueXY() : Pair<Float, Float> {
        //projects coordinates on the middle of the ring
        val (x, y) = currentColor.getRingXY(mRadius.mid)
        return Pair(x + mCenter.first, y + mCenter.second)
    }

    //Projects coordinate inside satVal Triangle. Used to keep thumb in triangle, when finger slides outside
    private fun projectToTriangle(x : Float, y : Float) : Pair<Float, Float> {
        val px = x.coerceAtLeast(mVertexLB.first).coerceAtMost(vertexRM.first)
        val py = y.coerceAtMost(projectY(px, mVertexLB, vertexRM))
            .coerceAtLeast(projectY(px, mVertexLT, vertexRM))
        return Pair(px, py)
    }

    //determines Saturation and Value from coordinate
    private fun satValFromXY(x : Float, y : Float) {
        val triangleHeight = 1.5f * mRadius.inner
        val d0 = MetricFunctions.distanceToLine(x, y, mVertexLB, mVertexLT)
        val d1 = MetricFunctions.distanceToLine(x, y, mVertexLB, vertexRM)
        val d2 = MetricFunctions.distanceToLine(x, y, mVertexLT, vertexRM)
        val blackAlpha = if (d0 + d2 != 0f) {
            (d2 / (d0 + d2))
        } else {
            0f
        }
        val whiteAlpha = (d1 / triangleHeight)
        val background = currentColor.chroma

        currentColor.setColorLockHue(ColorUtils.blendARGB(background, Color.BLACK, blackAlpha))
        currentColor.setColorLockHue(
            ColorUtils.blendARGB(
                currentColor.color,
                Color.WHITE,
                whiteAlpha
            )
        )
    }

    //Uses current color to determine position of thumb on satVal triangle
    private fun getSatValXY() : Pair<Float, Float> {
        val triangleHeight = 1.5f * mRadius.inner

        val h1 = currentColor.red.coerceAtMost(currentColor.green).coerceAtMost(currentColor.blue)
        val h2 = (255 - currentColor.red).coerceAtMost(255 - currentColor.green)
            .coerceAtMost(255 - currentColor.blue)

        val d1 = h1.toFloat() * triangleHeight / 255
        val alpha = if (h1 < 255) {
            h2.toFloat() / (255 - h1)
        } else {
            1f
        }

        val aY = mVertexLB.second - d1 * 2 / sqrt(3f)
        val bX = mVertexLT.first + (aY - mVertexLT.second) * sqrt(3f) / 2
        val bY = (aY + mVertexLT.second) / 2

        val x = alpha * mVertexLT.first + (1 - alpha) * bX
        val y = alpha * aY + (1 - alpha) * bY
        return Pair(x, y)
    }

    //Finds y coordinate so that the point is in the triangle
    private fun projectY(x : Float, a : Pair<Float, Float>, b : Pair<Float, Float>) : Float {
        val alpha = (x - a.first) / (b.first - a.first)
        return (a.second * (1 - alpha) + b.second * alpha)
    }

    private fun dpToPixel(dps : Float) : Float {
        val scale = context.resources.displayMetrics.density
        return (dps * scale + 0.5f)
    }

    //Draws Rgb Panel
    private fun drawPanel(canvas : Canvas) {
        val radius = ((sqrt(2f) - 1) * mRadius.outer) / (sqrt(2f) + 1) / 1.08f
        val colorDisplay = Path()
        colorDisplay.addCircle(
            mCenter.first - mRadius.outer + radius,
            mCenter.second + mRadius.outer - radius,
            radius,
            Path.Direction.CW
        )

        val clip = Rect((mCenter.first - mRadius.outer + radius).toInt(), 0, width, height)
        //Draw prev and cur Color semiCircles
        canvas.drawPath(colorDisplay, previousColor.paintMain)
        canvas.save()
        canvas.clipRect(clip)
        canvas.drawPath(colorDisplay, currentColor.paintMain)
        canvas.restore()
        canvas.drawPath(colorDisplay, mContourPaint)

        mRedSlider.drawSlider(canvas)
        mGreenSlider.drawSlider(canvas)
        mBlueSlider.drawSlider(canvas)

        canvas.drawText(
            "R",
            (rgbPanel.left + mRedSlider.leftX) / 2,
            mRedSlider.y + mPaintText.textSize / 3,
            mPaintText
        )
        canvas.drawText(
            "G",
            (rgbPanel.left + mRedSlider.leftX) / 2,
            mGreenSlider.y + mPaintText.textSize / 3,
            mPaintText
        )
        canvas.drawText(
            "B",
            (rgbPanel.left + mRedSlider.leftX) / 2,
            mBlueSlider.y + mPaintText.textSize / 3,
            mPaintText
        )
    }

    //Radiuses of HueRing
    class Radius(
        val outer : Float,
        val inner : Float,
        val mid : Float
    )

    enum class TouchLocation {
        THUMB,
        TRIANGLE,
        RING,
        RED_SLIDER,
        GREEN_SLIDER,
        BLUE_SLIDER,
        OUTSIDE
    }
}





