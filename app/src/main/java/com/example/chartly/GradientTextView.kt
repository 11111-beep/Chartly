package com.example.chartly

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.widget.TextView

class GradientTextView(context: Context, attrs: AttributeSet?) : androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    init{}

    override fun onDraw(canvas: Canvas) {
        val text = text.toString()

        // 设置渐变
        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), 0f, //渐变开始与结束坐标
            intArrayOf(0xFF00FFF7.toInt(), 0xFF9C27B0.toInt()),
            // 紫色到蓝色
            null, Shader.TileMode.CLAMP
        )
        // 将渐变效果应用到文本的Paint上
        paint.shader = gradient
        super.onDraw(canvas)
    }
}