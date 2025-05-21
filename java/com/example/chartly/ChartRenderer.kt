package com.example.chartly

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlin.random.Random


// 定义一个ChartRenderer类，用于渲染不同类型的图表
class ChartRenderer(private val context: Context) {

    // 显示线性图表
    fun showLineChart(): LineChart {
        // 创建一个新的线性图表视图
        val chart = LineChart(context)
        // 生成10个随机数据点，x坐标为0到9，y坐标为随机的0到100之间的浮点数
        val entries = List(10) { Entry(it.toFloat(), Random.nextFloat() * 100f) }
        // 创建一个线数据集，传入数据点和标签，设置线宽和圆点半径
        val set = LineDataSet(entries, "折线图").apply {
            lineWidth = 2f // 设置线条宽度
            circleRadius = 4f // 设置圆点半径
        }
        // 配置图表的基本属性
        chart.apply {
            // 设置图表的数据源
            data = LineData(set)
            // 禁用右侧的Y轴
            axisRight.isEnabled = false
            // 隐藏图表的描述信息
            description.isEnabled = false
            // 设置Y轴动画效果，动画时长500毫秒
            animateY(500)
        }
        // 返回配置好的线性图表
        return chart
    }

    // 显示平滑曲线图
    fun showSmoothedLineChart(): LineChart {
        // 创建一个新的线性图表视图
        val chart = LineChart(context)
        // 生成10个随机数据点，x坐标为0到9，y坐标为随机的0到100之间的浮点数
        val entries = List(10) { Entry(it.toFloat(), Random.nextFloat() * 100f) }
        // 创建一个线数据集，传入数据点和标签，设置平滑模式和线条样式
        val set = LineDataSet(entries, "曲线图").apply {
            // 设置曲线模式为三次贝塞尔曲线
            mode = LineDataSet.Mode.CUBIC_BEZIER
            // 设置贝塞尔曲线的平滑度
            cubicIntensity = 0.2f
            // 设置线条宽度
            lineWidth = 2f
            // 禁止绘制圆点
            setDrawCircles(false)
        }
        // 配置图表的基本属性
        chart.apply {
            // 设置图表的数据源
            data = LineData(set)
            // 禁用右侧的Y轴
            axisRight.isEnabled = false
            // 隐藏图表的描述信息
            description.isEnabled = false
            // 设置Y轴动画效果，动画时长500毫秒
            animateY(500)
        }
        // 返回配置好的平滑曲线图表
        return chart
    }

    // 显示柱状图，可以是垂直或水平方向
    fun showBarChart(vertical: Boolean): BarChart {
        // 根据vertical参数选择创建垂直或水平柱状图
        val chart = if (vertical) BarChart(context) else HorizontalBarChart(context)
        // 生成7个随机数据点，x坐标为0到6，y坐标为随机的0到80之间的浮点数
        val entries = List(7) { BarEntry(it.toFloat(), Random.nextFloat() * 80f) }
        // 创建一个柱状数据集，传入数据点和标签
        val set = BarDataSet(entries, "柱状图")
        // 配置图表的基本属性
        chart.apply {
            // 设置图表的数据源
            data = BarData(set)
            // 禁用右侧的Y轴
            axisRight?.isEnabled = false
            // 隐藏图表的描述信息
            description?.isEnabled = false
            // 设置Y轴动画效果，动画时长500毫秒
            animateY(500)
        }
        // 返回配置好的柱状图表
        return chart
    }

    // 显示饼图，可以选择是否显示圆孔
    fun showPieChart(useHole: Boolean): PieChart {
        // 创建一个新的饼图视图
        val chart = PieChart(context)
        // 生成5个随机数据点，值为随机的0到30之间的浮点数，标签为1到5
        val entries = List(5) { PieEntry(Random.nextFloat() * 30f, "${it+1}") }
        // 创建一个饼图数据集，传入数据点和标签，设置颜色和其他样式
        val set = PieDataSet(entries, "").apply {
            // 为每个饼块生成随机颜色
            colors = entries.map { Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)) }
            // 禁止绘制数据值
            setDrawValues(false)
        }
        // 配置图表的基本属性
        chart.apply {
            // 设置图表的数据源
            data = PieData(set)
            // 是否启用圆孔效果
            isDrawHoleEnabled = useHole
            // 设置圆孔半径
            holeRadius = if (useHole) 50f else 0f
            // 隐藏图表的描述信息
            description.isEnabled = false
            // 设置Y轴动画效果，动画时长500毫秒
            animateY(500)
        }
        // 返回配置好的饼图表
        return chart
    }

    // 显示雷达图
    fun showRadarChart(): RadarChart {
        // 创建一个新的雷达图视图
        val chart = RadarChart(context)
        // 生成6个随机数据点，值为随机的0到100之间的浮点数
        val entries = List(6) { RadarEntry(Random.nextFloat() * 100f) }
        // 创建一个雷达数据集，传入数据点和标签，设置线条宽度
        val set = RadarDataSet(entries, "雷达图").apply { lineWidth = 1.5f }
        // 配置图表的基本属性
        chart.apply {
            // 设置图表的数据源
            data = RadarData(set)
            // 设置X轴的值格式化器
            xAxis.valueFormatter = object : ValueFormatter() {
                // 定义X轴标签内容
                private val labels = listOf("A","B","C","D","E","F")
                // 格式化值为对应的标签
                override fun getFormattedValue(value: Float) = labels.getOrNull(value.toInt()%labels.size)?:""
            }
            // 隐藏图表的描述信息
            description.isEnabled = false
            // 设置Y轴动画效果，动画时长500毫秒
            animateY(500)
        }
        // 返回配置好的雷达图表
        return chart
    }

    // 显示散点图
    fun showScatterChart(): ScatterChart {
        // 创建一个新的散点图视图
        val chart = ScatterChart(context)
        // 生成20个随机数据点，x和y坐标为随机的0到10之间的浮点数，并按x坐标排序
        val entries = List(20) { Entry(Random.nextFloat()*10f, Random.nextFloat()*10f) }.sortedBy { it.x }
        // 创建一个散点数据集，传入数据点和标签，禁用值绘制
        val set = ScatterDataSet(entries.toMutableList(), "散点图").apply { setDrawValues(false) }
        // 配置图表的基本属性
        chart.apply {
            // 设置图表的数据源
            data = ScatterData(set)
            // 禁用右侧的Y轴
            axisRight.isEnabled = false
            // 隐藏图表的描述信息
            description.isEnabled = false
            // 设置XY轴动画效果，动画时长500毫秒
            animateXY(500,500)
        }
        // 返回配置好的散点图表
        return chart
    }

    // 显示气泡图
    fun showBubbleChart(): BubbleChart {
        // 创建一个新的气泡图视图
        val chart = BubbleChart(context)
        // 生成10个随机数据点，x坐标为0到9，y值为随机的0到50之间的浮点数，气泡半径为随机的0到3之间的浮点数
        val entries = List(10) { BubbleEntry(it.toFloat(), Random.nextFloat()*50f, Random.nextFloat()*3f) }
        // 配置图表的基本属性
        chart.apply {
            // 设置图表的数据源
            data = BubbleData(BubbleDataSet(entries, "气泡图"))
            // 禁用右侧的Y轴
            axisRight.isEnabled = false
            // 隐藏图表的描述信息
            description.isEnabled = false
            // 设置Y轴动画效果，动画时长500毫秒
            animateY(500)
        }
        // 返回配置好的气泡图表
        return chart
    }

    // 显示K线图（烛台图）
    fun showCandleStickChart(): CandleStickChart {
        // 创建一个新的K线图视图
        val chart = CandleStickChart(context)
        // 生成8个随机K线数据点
        val entries = List(8) { i ->
            // 随机生成开盘价
            val open = Random.nextFloat()*80f+20f
            // 随机生成收盘价
            val close = Random.nextFloat()*80f+20f
            // 最高价为max(open, close)加上随机的0到10之间的浮点数
            val high = maxOf(open, close)+Random.nextFloat()*10f
            // 最低价为min(open, close)减去随机的0到10之间的浮点数
            val low = minOf(open, close)-Random.nextFloat()*10f
            // 创建一个CandleEntry
            CandleEntry(i.toFloat(), high, low, open, close)
        }
        // 创建一个K线数据集，传入数据点和标签，设置颜色和绘制样式
        val set = CandleDataSet(entries, "K线图").apply {
            // 设置线条颜色
            color = Color.rgb(80,80,80)
            // 设置阴影颜色和宽度
            shadowColor = Color.DKGRAY; shadowWidth = 0.8f
            // 设置下跌颜色和填充样式
            decreasingColor = Color.RED; decreasingPaintStyle = Paint.Style.FILL
            // 设置上涨颜色和填充样式
            increasingColor = Color.GREEN; increasingPaintStyle = Paint.Style.FILL
            // 设置平盘颜色
            neutralColor = Color.BLUE
        }
        // 配置图表的基本属性
        chart.apply {
            // 设置图表的数据源
            data = CandleData(set)
            // 禁用右侧的Y轴
            axisRight.isEnabled = false
            // 隐藏图表的描述信息
            description.isEnabled = false
            // 设置Y轴动画效果，动画时长500毫秒
            animateY(500)
        }
        // 返回配置好的K线图表
        return chart
    }

    // 显示组合图表（折线和条形组合）
    fun showBarLineChart(): CombinedChart {
        // 创建一个新的组合图表视图
        val chart = CombinedChart(context)
        // 创建一个折线数据集，传入10个随机数据点
        val line = LineDataSet(List(10) { Entry(it.toFloat(), Random.nextFloat()*120f) }, "折线")
        // 创建一个条形数据集，传入10个随机数据点
        val bar = BarDataSet(List(10) { BarEntry(it.toFloat(), Random.nextFloat()*120f) }, "条形")
        // 配置图表的基本属性
        chart.apply {
            // 设置组合数据源，包含折线和条形数据
            data = CombinedData().apply { setData(LineData(line)); setData(BarData(bar)) }
            // 禁用右侧的Y轴
            axisRight.isEnabled = false
            // 隐藏图表的描述信息
            description.isEnabled = false
            // 设置XY轴动画效果，动画时长500毫秒
            animateXY(500,500)
        }
        // 返回配置好的组合图表
        return chart
    }
}