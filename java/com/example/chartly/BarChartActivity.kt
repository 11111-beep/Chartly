package com.example.chartly

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT        // ← 别忘了
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.model.GradientColor
import com.google.android.material.button.MaterialButton

class BarChartActivity : DrawerActivity() {

    // 静态成员变量
    companion object {
        private const val REQUEST_IMPORT_CSV = 1001 //导入csv请求码
        private const val REQUEST_EXPORT_CSV = 1002 // 导出csv请求码
        const val EXTRA_CHART_DATA = "com.example.chartly.EXTRA_CHART_DATA" //传递或接收图表的键名
    }

    private lateinit var inputLayout: LinearLayout
    private lateinit var addBarButton: MaterialButton
    private lateinit var saveChartButton: MaterialButton
    private lateinit var barChart: BarChart
    private lateinit var tvTitle: TextView
    private lateinit var markerView: CustomMarkerView

    private val entries = mutableListOf<BarEntry>() //储存每一个柱子的BarEntry对象(x,y)
    private val labels  = mutableListOf<String>() // 每一个柱子的标签
    private lateinit var dataSet: BarDataSet // 柱状图的数据

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bar_chart)

        inputLayout     = findViewById(R.id.inputLayout)
        addBarButton    = findViewById(R.id.addBarButton)
        saveChartButton = findViewById(R.id.saveChartButton)
        barChart        = findViewById(R.id.barChart)
        tvTitle         = findViewById(R.id.tvTitle)
        markerView      = CustomMarkerView(this, R.layout.marker_view)

        tvTitle.text = "柱状图"
        barChart.marker = markerView
        markerView.chartView = barChart

        setupChart()
        addInputFields()

        addBarButton.setOnClickListener { addInputFields() }
        addBarButton.setOnLongClickListener {
            showAddMultipleRowsDialog()
            true
        }
        saveChartButton.setOnClickListener { saveAllBarsAndRefreshChart() }

        // 绑定下载和 CSV 操作
        findViewById<CustomTitleBar>(R.id.customTitleBar).apply {
            onDownloadClick = {
                getChart()?.let { c ->
                    saveChartToPng(c, App.instance,
                        "Chartly_${System.currentTimeMillis()}.png")
                } ?: Toast.makeText(App.instance,
                    "当前没有可导出的图表", Toast.LENGTH_SHORT).show()
            }
            onUploadClick = {
                AlertDialog.Builder(this@BarChartActivity)
                    .setTitle("CSV 操作")
                    .setItems(arrayOf("导入 CSV","导出 CSV")) { _, which ->
                        if (which == 0) importCsv() else exportCsv()
                    }
                    .show()
            }
        }

        // 如果有来自上一个页面的数据，立即加载并刷新
        processIncomingData(intent.getStringArrayListExtra(EXTRA_CHART_DATA))
    }

    private fun setupChart() {
        dataSet = BarDataSet(entries, "数据柱").apply {
            valueTextSize = 12f
        }
        barChart.apply {
            data = BarData(dataSet) // 图表的数据属性

            animateY(500) // y轴方向的动画
            setScaleEnabled(true) // 允许缩放
            isDragEnabled = true //  允许拖拽
            axisRight.isEnabled = false // 右侧的y轴不显示
            description.isEnabled = false // 描述
            legend.isEnabled = true // 图例
            xAxis.position = XAxis.XAxisPosition.BOTTOM // x轴在底部
            xAxis.granularity = 1f
            axisLeft.granularity = 1f
            setFitBars(true) // 柱子紧贴
            setLayerType(View.LAYER_TYPE_SOFTWARE, null) //  解决阴影不显示等兼容性问题
        }
    }

    // 命令式程序化布局
    private fun addInputFields() {
        val etLabel = EditText(this).apply {
            hint = "标签"
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val etValue = EditText(this).apply {
            hint = "数值"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = 8
            }
            addView(etLabel)
            addView(etValue)
        }
        inputLayout.addView(row)
    }

    private fun showAddMultipleRowsDialog() {
        val input = EditText(this).apply {
            hint = "请输入要添加的数据点数量"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(this)
            .setTitle("批量添加数据点")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                input.text.toString().toIntOrNull()?.takeIf { it > 0 }?.let { cnt ->
                    repeat(cnt) { addInputFields() } // 转化为int，并判断是否大于0
                } ?: Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveAllBarsAndRefreshChart() {
        entries.clear()
        labels.clear()
        repeat(inputLayout.childCount) { i ->
            val row = inputLayout.getChildAt(i) as LinearLayout
            val label = (row.getChildAt(0) as EditText)
                .text.toString().ifBlank { "＃${i+1}" }
            val value = (row.getChildAt(1) as EditText)
                .text.toString().toFloatOrNull()
            if (value != null) {
                entries.add(BarEntry(i.toFloat(), value).apply { data = label })
                labels.add(label)
            }
        }

        if (entries.isEmpty()) {
            Toast.makeText(App.instance, "请先输入有效的柱状数据", Toast.LENGTH_SHORT).show()
            barChart.clear()
            return
        }

        dataSet.values = entries
        val grad = GradientColor(Color.rgb(252,0,255), Color.rgb(0,219,222))
        dataSet.setGradientColors(List(entries.size) { grad })
        barChart.apply {
            data = BarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            notifyDataSetChanged()
            animateY(300)
            invalidate()
        }
    /*  barChart.data = BarData(dataSet)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.notifyDataSetChanged()
        barChart.animateY(300)
        barChart.invalidate()*/
        Toast.makeText(this, "柱状图已刷新", Toast.LENGTH_SHORT).show()
    }

    /** 在侧边栏导航中被调用，返回序列化后的所有行："标签|数值" */
    override fun collectChartData(): ArrayList<String>? {
        val out = ArrayList<String>()
        repeat(inputLayout.childCount) { i ->
            val row = inputLayout.getChildAt(i) as LinearLayout // 获取每行数据,强制转化为LinearLayout
            val a = (row.getChildAt(0) as EditText).text.toString()
            val b = (row.getChildAt(1) as EditText).text.toString()
            out.add("$a|$b") // 以竖线分割添加到out容器中
        }
        return out.ifEmpty { null }
    }

    /**
     * 接收从上一个视图传来的数据。
     * data 格式：List<"标签|数值">，会重建输入行并立即刷新图表。
     */
    override fun processIncomingData(data: ArrayList<String>?) {
        data?.let {
            inputLayout.removeAllViews() // 清空输入行
            it.forEach { line -> // 处理每一行数据
                val parts = line.split("|", limit = 2) // 分割标签和数值的字符串列表
                if (parts.size == 2) {
                    addInputFields()
                    val row = inputLayout.getChildAt(inputLayout.childCount - 1) as LinearLayout
                    (row.getChildAt(0) as EditText).setText(parts[0])
                    (row.getChildAt(1) as EditText).setText(parts[1])
                }
            }
            saveAllBarsAndRefreshChart()
        }
    }

    // ─────── CSV 导入/导出 ───────

    private fun importCsv() {
        // 创建一个 Intent，用于请求获取内容
        Intent(Intent.ACTION_GET_CONTENT).apply {
            // 设置要获取的数据类型为 CSV 文件
            type = "text/csv"
            // 添加 CATEGORY_OPENABLE，表明返回的数据应该可以作为流打开
            addCategory(Intent.CATEGORY_OPENABLE)
        }.also { intent ->
            // 启动一个 Activity 来选择内容，并期望返回结果
            // REQUEST_IMPORT_CSV 是一个请求码，用于在 onActivityResult 中识别这个请求
            startActivityForResult(intent, REQUEST_IMPORT_CSV)
        }
    }

    private fun exportCsv() {
        // 生成一个基于当前时间戳的文件名，确保文件名唯一
        val fileName = "Chartly_${System.currentTimeMillis()}.csv"

        // 根据 Android 版本处理文件导出逻辑
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android Q (API 29) 及更高版本使用 MediaStore API
            val resolver = contentResolver
            val cv = ContentValues().apply {
                // 设置要保存的文件名
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                // 设置文件的 MIME 类型为 CSV
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                // 设置文件在外部存储中的相对路径
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Chartly")
            }

            // 向 MediaStore 插入文件信息，获取文件的 Content URI
            resolver.insert(MediaStore.Files.getContentUri("external"), cv)?.let { uri ->
                // 使用 ContentResolver 打开一个输出流来写入文件内容
                resolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    // 遍历 entries 列表和 labels 列表，将数据写入 CSV 文件
                    entries.forEachIndexed { i, entry ->
                        // 每行写入 label 和对应的 y 值，用逗号分隔，并换行
                        writer.append("${labels[i]},${entry.y}\n")
                    }
                }
                // 导出成功后显示 Toast 消息，告知用户文件保存路径
                Toast.makeText(this, "已导出到 Documents/Chartly/$fileName", Toast.LENGTH_LONG).show()
            } ?: Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show()
            // 如果插入文件信息失败，显示导出失败的 Toast 消息
        } else {
            // Android Q 之前的版本使用传统的文件系统操作
            // 获取应用程序外部文件目录下的 "csv_exports" 子目录，如果不存在则创建
            val dir = getExternalFilesDir(null)?.resolve("csv_exports")!!.apply { mkdirs() }
            // 创建要导出的 CSV 文件对象
            val file = dir.resolve(fileName)
            // 使用 BufferedWriter 将数据写入文件
            file.bufferedWriter().use { writer ->
                // 遍历 entries 列表和 labels 列表，将数据写入 CSV 文件
                entries.forEachIndexed { i, entry ->
                    // 每行写入 label 和对应的 y 值，用逗号分隔，并换行
                    writer.append("${labels[i]},${entry.y}\n")
                }
            }
            // 导出成功后显示 Toast 消息，告知用户文件保存的绝对路径
            Toast.makeText(this, "已导出到 ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // 调用父类的 onActivityResult 方法，以确保父类的逻辑得到执行
        super.onActivityResult(requestCode, resultCode, data)

        // 检查请求码是否是我们启动文件选择器时使用的 REQUEST_IMPORT_CSV
        // 并且结果码是否是 Activity.RESULT_OK，表示用户成功选择了文件
        if (requestCode == REQUEST_IMPORT_CSV && resultCode == Activity.RESULT_OK) {
            // data 是包含返回数据的 Intent，可能包含用户选择的文件的 URI
            data?.data?.let { uri ->
                // 如果 data 和 data.data (文件的 URI) 不为空，则执行以下操作

                // 移除 inputLayout 中的所有现有视图，为导入的数据创建新的输入字段
                inputLayout.removeAllViews()
                // 初始化导入的行数计数器
                var count = 0

                // 使用 contentResolver 打开所选 URI 对应的输入流
                contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                    // useLines 扩展函数可以方便地逐行读取文件内容，并在读取完成后自动关闭流

                    // 遍历文件中的每一行
                    lines.forEach { line ->
                        // 使用逗号作为分隔符将每一行分割成列
                        val cols = line.split(",")
                        // 检查分割后的列数是否至少为 2 (假设 CSV 格式为 "label,value")
                        if (cols.size >= 2) {
                            // 调用 addInputFields() 方法动态添加一对新的输入框 (用于 label 和 value)
                            addInputFields()
                            // 获取当前添加的输入框的索引 (inputLayout 的最后一个子视图)
                            val idx = inputLayout.childCount - 1
                            // 获取新添加的 LinearLayout (包含两个 EditText)
                            val row = inputLayout.getChildAt(idx) as LinearLayout
                            // CSV 格式假定为 "label,value"

                            // 获取 LinearLayout 中的第一个 EditText (用于 label) 并设置其文本为 CSV 的第一列 (去除首尾空格)
                            (row.getChildAt(0) as EditText).setText(cols[0].trim())
                            // 获取 LinearLayout 中的第二个 EditText (用于 value) 并设置其文本为 CSV 的第二列 (去除首尾空格)
                            (row.getChildAt(1) as EditText).setText(cols[1].trim())
                            // 成功导入一行，增加计数器
                            count++
                        }
                        // 如果当前行分割后的列数小于 2，则认为该行数据无效，跳过
                    }
                }

                // 在读取完所有行后，检查是否成功导入了至少一行数据
                if (count > 0) {
                    // 如果成功导入了数据，则调用 saveAllBarsAndRefreshChart() 方法保存数据并刷新图表
                    saveAllBarsAndRefreshChart()
                    // 显示一个 Toast 消息，告知用户已导入的行数
                    Toast.makeText(this, "已导入 $count 行", Toast.LENGTH_SHORT).show()
                } else {
                    // 如果没有导入任何有效数据，则显示一个 Toast 消息，告知用户 CSV 文件没有有效数据
                    Toast.makeText(this, "CSV 没有有效数据", Toast.LENGTH_SHORT).show()
                }
            }
            // 如果 data 或 data.data 为空，则表示用户可能取消了文件选择，或者发生了其他错误，这里没有做具体的错误处理
        }
        // 如果请求码不是 REQUEST_IMPORT_CSV 或者结果码不是 Activity.RESULT_OK，则表示不是我们期望的文件导入结果，父类的逻辑会处理其他情况
    }

    override fun getChart(): Chart<*>? = barChart
}
