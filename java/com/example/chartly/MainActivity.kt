package com.example.chartly

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.chartly.databinding.ActivityMainBinding
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlin.random.Random


// 主活动类，继承自 AppCompatActivity，负责显示图表选择、视频背景和图表渲染
class MainActivity : AppCompatActivity() {
    // 使用 view binding 来管理布局，减少 findViewById 的调用
    private lateinit var binding: ActivityMainBinding
    // 图表渲染器，用于生成不同类型的图表
    private lateinit var chartRenderer: ChartRenderer
    // Spinner 适配器，用于显示图表类型的选择项
    private lateinit var spinnerAdapter: ChartSpinnerAdapter

    // 图表类型列表，包含了所有支持的图表类型
    private val chartList = listOf(
        "折线图 (Line Chart)",
        "曲线图 (SmoothedLine Chart)",
        "柱状图 (Bar Chart)",
        "水平柱状图 (Horizontal Bar Chart)",
        "饼状图 (Pie Chart)",
        "圆环图 (Doughnut Chart)",
        "雷达图 (Radar Chart)",
        "散点图 (Scatter Chart)",
        "气泡图 (Bubble Chart)",
        "K线图 (Candle Stick Chart)",
        "条形图 (Bar Line Chart)"
    )
    // 记录当前选中的图表类型索引，初始为 0
    private var selectedIndex = 0

    // Activity 创建时的回调，用于初始化视图和组件
    override fun onCreate(savedInstanceState: Bundle?) {
        // 调用父类的 onCreate 方法，传入保存的状态
        super.onCreate(savedInstanceState)
        // 使用 View Binding 填充布局文件，并将其赋值给 binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        // 设置ContentView为绑定的根视图
        setContentView(binding.root)

        // 初始化图表渲染器，传入当前上下文
        chartRenderer = ChartRenderer(this)

        // 设置标题文本
        binding.tvTitle.text = "Chartly 可视化数据"

        // 初始化 Spinner 适配器，传入当前上下文和图表类型列表
        spinnerAdapter = ChartSpinnerAdapter(this, chartList)
        // 将适配器设置到 Spinner 中
        binding.spinnerChartType.adapter = spinnerAdapter

        // 设置 Spinner 下拉弹出框的背景drawable
        binding.spinnerChartType.setPopupBackgroundDrawable(
            // 使用 ContextCompat 获取 drawable 资源
            ContextCompat.getDrawable(this, R.drawable.spinner_dropdown_background)
        )
        // 或者使用 setPopupBackgroundResource 方法：
        // binding.spinnerChartType.setPopupBackgroundResource(R.drawable.spinner_dropdown_background)

        // 设置视频背景
        val uri = Uri.parse("android.resource://$packageName/${R.raw.video1}")
        // 将解析的 uri 设置给 VideoView
        binding.video.setVideoURI(uri)
        // 设置视频准备就绪的监听器
        binding.video.setOnPreparedListener { mp ->
            // 设置视频循环播放
            mp.isLooping = true
            // 设置音量为 0，实现无声播放
            mp.setVolume(0f, 0f)
            // 开始播放
            mp.start()
        }

        // 设置 Spinner 选项选择的监听器
        binding.spinnerChartType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            // 当某个选项被选中时回调
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                // 更新当前选中的索引
                selectedIndex = position
                // 通知适配器当前选中的位置
                spinnerAdapter.setSelectedPosition(position)
                // 移除 chartContainer 中的所有视图
                binding.chartContainer.removeAllViews()
                // 根据当前选中的位置，渲染对应的图表
                renderChart(position)
            }

            // 当没有选项被选中时回调
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 设置编辑按钮的点击监听器
        binding.btnEdit.setOnClickListener {
            // 根据当前选中的索引，启动对应的图表编辑 Activity
            val intent = when (selectedIndex) {
                0 -> Intent(this, LineChartActivity::class.java)
                1 -> Intent(this, SmoothedLineChartActivity::class.java)
                2 -> Intent(this, BarChartActivity::class.java)
                3 -> Intent(this, HorizontalBarChartActivity::class.java)
                4 -> Intent(this, PieChartActivity::class.java)
                5 -> Intent(this, DoughnutChartActivity::class.java)
                6 -> Intent(this, RadarChartActivity::class.java)
                7 -> Intent(this, ScatterChartActivity::class.java)
                8 -> Intent(this, BubbleChartActivity::class.java)
                9 -> Intent(this, CandleStickChartActivity::class.java)
                10 -> Intent(this, BarLineChartActivity::class.java)
                else -> null
            }
            intent?.let {
                // 👉 显示圆环动画
                val progressView = binding.progressCircle
                progressView.visibility = android.view.View.VISIBLE
                progressView.progress = 0 // 重置进度

                // 动画地设置到一个随机进度（或固定数值），视觉反馈
                progressView.progress = 100

                // 在动画完成后延迟跳转
                progressView.postDelayed({
                    progressView.visibility = android.view.View.GONE
                    startActivity(it)
                }, 1000) // 延迟 500ms 执行跳转
            }
        }
    }

    // 根据传入的图表类型，渲染对应的图表并显示在 chartContainer 中
    private fun renderChart(type: Int) {
        // 根据 type 获取对应的图表视图
        val chart = when (type) {
            0 -> chartRenderer.showLineChart()
            1 -> chartRenderer.showSmoothedLineChart()
            2 -> chartRenderer.showBarChart(true)
            3 -> chartRenderer.showBarChart(false)
            4 -> chartRenderer.showPieChart(false)
            5 -> chartRenderer.showPieChart(true)
            6 -> chartRenderer.showRadarChart()
            7 -> chartRenderer.showScatterChart()
            8 -> chartRenderer.showBubbleChart()
            9 -> chartRenderer.showCandleStickChart()
            10 -> chartRenderer.showBarLineChart()
            else -> return
        }
        // 设置图表的布局参数，占满容器
        chart.layoutParams = android.widget.FrameLayout.LayoutParams(
            MATCH_PARENT,
            MATCH_PARENT
        )
        // 将图表视图添加到 chartContainer 中
        binding.chartContainer.addView(chart)
    }
}