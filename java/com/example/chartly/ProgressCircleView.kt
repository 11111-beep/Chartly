package com.example.chartly

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils

/**
 * ProgressCircleView：
 * 自定义圆环进度视图，
 * 支持环宽、起始角度、背景/进度渐变色、文字显示及圆角样式，
 * 中心文字带“冷光”光晕效果。
 */
class ProgressCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint       = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // 默认冷光文字颜色（冰蓝）
        color = Color.parseColor("#00FFF7")
        textAlign = Paint.Align.CENTER
        // 设置冷光光晕（Glow）
        setShadowLayer(8f, 0f, 0f, Color.parseColor("#00FFF7"))
    }
    private val rectF           = RectF()

    private var _progress = 0

    /** 进度文字大小（px） */
    var textSize: Float = sp2px(20f)
        set(value) {
            field = value
            textPaint.textSize = value
            invalidate()
        }

    /** 进度（0..maxProgress），设置时将以动画平滑过渡 */
    var progress: Int
        get() = _progress
        set(value) {
            val target = value.coerceIn(0, maxProgress)
            if (target != _progress) {
                ValueAnimator.ofInt(_progress, target).apply {
                    duration = 1000  // 动画时长：1000ms
                    interpolator = LinearInterpolator()
                    addUpdateListener { anim ->
                        _progress = anim.animatedValue as Int
                        updateProgressColor()
                        invalidate()
                    }
                    start()
                }
            }
        }

    /** 最大进度，至少为 1 */
    var maxProgress: Int = 100
        set(value) {
            field = value.coerceAtLeast(1)
            progress = _progress
        }

    /** 圆环宽度（px），默认较小以突出文字 */
    var ringWidth: Float = dp2px(6f)
        set(value) {
            field = value
            backgroundPaint.strokeWidth = value
            progressPaint.strokeWidth   = value
            invalidate()
        }

    /** 起始绘制角度（默认：-90f，顶部开始） */
    var startAngle: Float = -90f
        set(value) {
            field = value
            invalidate()
        }

    /** 圆环背景颜色 */
    @ColorInt
    var ringBackgroundColor: Int = Color.LTGRAY
        set(value) {
            field = value
            backgroundPaint.color = value
            invalidate()
        }

    /** 进度渐变起始色 */
    @ColorInt
    var progressStartColor: Int = Color.GREEN
        set(value) {
            field = value
            updateProgressColor()
        }

    /** 进度渐变结束色 */
    @ColorInt
    var progressEndColor: Int = Color.RED
        set(value) {
            field = value
            updateProgressColor()
        }

    /** 是否显示进度文字 */
    var showText: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    /** 是否启用圆角线帽 */
    var roundedCorners: Boolean = true
        set(value) {
            field = value
            val cap = if (value) Paint.Cap.ROUND else Paint.Cap.BUTT
            backgroundPaint.strokeCap = cap
            progressPaint.strokeCap   = cap
            invalidate()
        }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ProgressCircleView).apply {
            try {
                _progress            = getInt(R.styleable.ProgressCircleView_progress, _progress)
                maxProgress          = getInt(R.styleable.ProgressCircleView_maxProgress, maxProgress)
                ringWidth            = getDimension(R.styleable.ProgressCircleView_ringWidth, ringWidth)
                startAngle           = getFloat(R.styleable.ProgressCircleView_startAngle, startAngle)
                ringBackgroundColor  = getColor(R.styleable.ProgressCircleView_backgroundColor, ringBackgroundColor)
                progressStartColor   = getColor(R.styleable.ProgressCircleView_progressStartColor, progressStartColor)
                progressEndColor     = getColor(R.styleable.ProgressCircleView_progressEndColor, progressEndColor)
                textPaint.color      = getColor(R.styleable.ProgressCircleView_textColor, textPaint.color)
                textSize             = getDimension(R.styleable.ProgressCircleView_textSize, textSize)
                showText             = getBoolean(R.styleable.ProgressCircleView_showText, showText)
                roundedCorners       = getBoolean(R.styleable.ProgressCircleView_roundedCorners, roundedCorners)
            } finally {
                recycle()
            }
        }
        setupPaints()
    }

    private fun setupPaints() {
        backgroundPaint.apply {
            style      = Paint.Style.STROKE
            strokeWidth = ringWidth
            color       = ringBackgroundColor
            strokeCap   = if (roundedCorners) Paint.Cap.ROUND else Paint.Cap.BUTT
        }
        progressPaint.apply {
            style      = Paint.Style.STROKE
            strokeWidth = ringWidth
            strokeCap   = if (roundedCorners) Paint.Cap.ROUND else Paint.Cap.BUTT
            updateProgressColor()
        }
        textPaint.textSize = textSize
    }

    private fun updateProgressColor() {
        val ratio = _progress.toFloat() / maxProgress
        progressPaint.color = ColorUtils.blendARGB(progressStartColor, progressEndColor, ratio)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = measuredWidth.coerceAtMost(measuredHeight)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val half = ringWidth / 2f
        rectF.set(
            paddingLeft + half,
            paddingTop + half,
            width - paddingRight - half,
            height - paddingBottom - half
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制背景环
        canvas.drawArc(rectF, 0f, 360f, false, backgroundPaint)
        // 绘制进度弧
        val sweep = _progress.toFloat() / maxProgress * 360f
        canvas.drawArc(rectF, startAngle, sweep, false, progressPaint)
        // 绘制进度文字
        if (showText) {
            val text = "${_progress}%"
            val fm   = textPaint.fontMetrics
            val centerY = rectF.centerY()
            val baseline = centerY - (fm.ascent + fm.descent) / 2
            canvas.drawText(text, rectF.centerX(), baseline, textPaint)
        }
    }

    private fun dp2px(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    private fun sp2px(sp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
}