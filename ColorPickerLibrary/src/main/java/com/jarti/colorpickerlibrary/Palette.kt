package com.jarti.ColorPickerView

import android.graphics.*
import kotlin.math.atan2
import kotlin.math.sqrt

class Palette {
    companion object {
        fun createHueRing(outerRadius : Float, innerRadius : Float) : Bitmap {
            val mEraser = Paint().apply {
                isAntiAlias = true
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            val mHueRing = Bitmap.createBitmap(
                2 * outerRadius.toInt(),
                2 * outerRadius.toInt(),
                Bitmap.Config.ARGB_8888
            )
            val mRadius = (outerRadius + innerRadius) / 2

            for (i in 0 until 2 * outerRadius.toInt()) {
                for (j in 0 until 2 * outerRadius.toInt()) {
                    var hue = atan2(j.toFloat() - outerRadius, i.toFloat() - outerRadius)
                    if (hue < 0) hue += 2 * Math.PI.toFloat()
                    hue = 180f * hue / Math.PI.toFloat()
                    var value = mRadius / MetricFunctions.distanceR2(
                        i.toFloat(),
                        j.toFloat(),
                        outerRadius,
                        outerRadius
                    )
                    value = value.coerceAtMost(1 / value)

                    val color = Color.HSVToColor(floatArrayOf(hue, sqrt(value), 1f))
                    mHueRing.setPixel(i, j, color)
                }
            }
            /**
             * Cut out inside and outside parts of the ring
             */
            val canvas = Canvas(mHueRing)
            val path = Path()
            path.addCircle(outerRadius, outerRadius, outerRadius, Path.Direction.CW)
            path.addCircle(outerRadius, outerRadius, innerRadius, Path.Direction.CCW)
            path.fillType = Path.FillType.INVERSE_EVEN_ODD
            canvas.drawPath(path, mEraser)
            return mHueRing
        }

        /**Create Saturation - Value triangle (Unmasked)
         * Creates black and white transparency mask
         */

        fun createSVTriangle(radius : Float) : Bitmap {
            val mEraser = Paint().apply {
                isAntiAlias = true
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }

            val w = 2 * radius.toInt()
            val h = 2 * radius.toInt()
            val whiteGradient = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val blackGradient = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val (leftTop, leftBottom, rightMid) = initVertexCoordinates(radius)

            val triangleHeight = 1.5 * radius
            for (i in 0 until w) {
                for (j in 0 until h) {
                    val d0 = MetricFunctions.distanceToLine(
                        i.toDouble(),
                        j.toDouble(),
                        leftBottom,
                        leftTop
                    )
                    val d1 = MetricFunctions.distanceToLine(
                        i.toDouble(),
                        j.toDouble(),
                        leftBottom,
                        rightMid
                    )
                    val d2 = MetricFunctions.distanceToLine(
                        i.toDouble(),
                        j.toDouble(),
                        leftTop,
                        rightMid
                    )
                    val blackAlpha = if (d0 + d2 != 0.0) {
                        255 * (d2 / (d0 + d2))
                    } else {
                        255.0
                    }
                    val whiteAlpha = 255 * (d1 / triangleHeight)
                    blackGradient.setPixel(i, j, 0x00000000 + (blackAlpha.toInt() shl 24))
                    whiteGradient.setPixel(i, j, 0x00FFFFFF + (whiteAlpha.toInt() shl 24)) //
                }
            }
            val triangleMask = createMaskTriangle(leftTop, leftBottom, rightMid)
            val canvas1 = Canvas(blackGradient)
            canvas1.drawBitmap(whiteGradient, 0f, 0f, null)
            canvas1.drawPath(triangleMask, mEraser)
            return blackGradient
        }

        /**
         * Returns contour path of the satVal triangle
         */
        private fun createMaskTriangle(
            vertexLeftTop : Pair<Double, Double>,
            vertexLeftBottom : Pair<Double, Double>,
            vertexRightMid : Pair<Double, Double>
        ) : Path {
            val mTriangleMask = Path()
            mTriangleMask.moveTo(
                vertexLeftTop.first.toFloat(),
                vertexLeftTop.second.toFloat()
            ) // Top
            mTriangleMask.lineTo(vertexRightMid.first.toFloat(), vertexRightMid.second.toFloat())
            mTriangleMask.lineTo(
                vertexLeftBottom.first.toFloat(),
                vertexLeftBottom.second.toFloat()
            )
            mTriangleMask.lineTo(
                vertexLeftTop.first.toFloat(),
                vertexLeftTop.second.toFloat()
            ) // Back to Top
            mTriangleMask.close()
            mTriangleMask.fillType = Path.FillType.INVERSE_EVEN_ODD
            return mTriangleMask
        }

        //Generate coordinates of triangle vertices
        private fun initVertexCoordinates(radius : Float) : Triple<Pair<Double, Double>, Pair<Double, Double>, Pair<Double, Double>> {
            val vertexLeftTop = Pair(
                radius - 0.5 * radius,
                radius - 0.5 * sqrt(3.0) * radius
            )
            val vertexLeftBottom = Pair(
                radius - 0.5 * radius,
                radius + 0.5 * sqrt(3.0) * radius
            )
            val vertexRightMid = Pair(radius + radius.toDouble(), radius.toDouble())
            return Triple(vertexLeftTop, vertexLeftBottom, vertexRightMid)
        }

    }


}

//Triangle Vertices
//private lateinit var vertexLeftTop : Pair<Double, Double>
//private lateinit var vertexLeftBottom : Pair<Double, Double>
//private lateinit var vertexRightMid : Pair<Double, Double>
