package com.example.chartly

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.model.GradientColor

class BarLineChartActivity : DrawerActivity() {

    companion object {
        private const val REQUEST_IMPORT_CSV = 2001
        private const val REQUEST_EXPORT_CSV = 2002
    }

    // UI 组件
    private lateinit var inputLayout: LinearLayout
    private lateinit var addRowButton: Button
    private lateinit var saveChartButton: Button
    private lateinit var combinedChart: CombinedChart
    private lateinit var tvTitle: TextView
    private lateinit var markerView: CustomMarkerView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-Edge 适配
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_bar_line_chart)

        // 处理系统栏内边距
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById<View>(R.id.main),
            OnApplyWindowInsetsListener { v, insets ->
                val sys = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                v!!.setPadding(sys.left, sys.top, sys.right, sys.bottom)
                insets
            }
        )

        // 初始化 UI
        inputLayout     = findViewById(R.id.inputLayout)
        addRowButton    = findViewById(R.id.addBarLineButton)
        saveChartButton = findViewById(R.id.saveChartButton)
        combinedChart   = findViewById(R.id.barLineChart)
        markerView      = CustomMarkerView(this, R.layout.marker_view)
        tvTitle         = findViewById(R.id.tvTitle)
        tvTitle.text    = "条形图"

        // 关联 MarkerView
        markerView.chartView = combinedChart
        combinedChart.marker = markerView

        // 初始一行输入
        addInputRow()

        // 按钮事件
        addRowButton.setOnClickListener { addInputRow() }
        addRowButton.setOnLongClickListener {
            showAddMultipleRowsDialog()
            true
        }
        saveChartButton.setOnClickListener { refreshCombinedChart() }

        // 图表默认配置
        setupChartDefaults()

        // CustomTitleBar：下载与 CSV 操作
        findViewById<CustomTitleBar>(R.id.customTitleBar).apply {
            onDownloadClick = {
                getChart()?.let { chart ->
                    saveChartToPng(
                        chart,
                        this@BarLineChartActivity,
                        "Chartly_${System.currentTimeMillis()}.png"
                    )
                } ?: Toast.makeText(
                    this@BarLineChartActivity,
                    "当前没有可导出的图表",
                    Toast.LENGTH_SHORT
                ).show()
            }
            onUploadClick = {
                AlertDialog.Builder(this@BarLineChartActivity)
                    .setTitle("CSV 操作")
                    .setItems(arrayOf("导入 CSV", "导出 CSV")) { _, which ->
                        when (which) {
                            0 -> importCsv()
                            1 -> exportCsv()
                        }
                    }.show()
            }
        }
    }

    /** 配置 CombinedChart 的基础样式 */
    private fun setupChartDefaults() {
        combinedChart.apply {
            description.isEnabled    = false
            axisRight.isEnabled       = false
            legend.isEnabled          = true
            xAxis.position            = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity         = 1f
            axisLeft.granularity      = 1f
            animateY(600)
            setDrawOrder(arrayOf(
                CombinedChart.DrawOrder.BAR,
                CombinedChart.DrawOrder.LINE
            ))
            // 强制软件渲染渐变
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }

    /** 添加一行“标签+柱状+折线”输入 */
    private fun addInputRow() {
        val etLabel = EditText(this).apply {
            hint = "标签"
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val etBar = EditText(this).apply {
            hint = "柱状值"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val etLine = EditText(this).apply {
            hint = "折线值"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = 8
            }
            addView(etLabel)
            addView(etBar)
            addView(etLine)
        }
        inputLayout.addView(row)
    }

    /** 长按批量添加对话框 */
    private fun showAddMultipleRowsDialog() {
        val input = EditText(this).apply {
            hint = "要添加的数据数量"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(this)
            .setTitle("批量添加数据")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                input.text.toString().toIntOrNull()?.let { count ->
                    if (count > 0) repeat(count) { addInputRow() }
                    else Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show()
                } ?: Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 刷新图表：读取所有输入，组装 BarData & LineData */
    /** 刷新图表：读取所有输入，组装 BarData & LineData */
    private fun refreshCombinedChart() {
        val barEntries  = mutableListOf<BarEntry>()
        val lineEntries = mutableListOf<Entry>()
        val labels      = mutableListOf<String>()

        for (i in 0 until inputLayout.childCount) {
            val row   = inputLayout.getChildAt(i) as LinearLayout
            val label = (row.getChildAt(0) as EditText).text.toString()
                .takeIf { it.isNotBlank() } ?: "#${i + 1}"
            val barVal  = (row.getChildAt(1) as EditText).text.toString().toFloatOrNull()
            val lineVal = (row.getChildAt(2) as EditText).text.toString().toFloatOrNull()

            // 只要柱状值不为空，就添加；折线值为空用0f
            if (barVal != null) {
                val lv = lineVal ?: 0f
                barEntries.add(
                    BarEntry(i.toFloat(), barVal).apply { setData(label) }
                )
                lineEntries.add(
                    Entry(i.toFloat(), lv).apply { setData(label) }
                )
                labels.add(label)
            }
        }

        if (barEntries.isEmpty()) {
            Toast.makeText(this, "请先输入有效的数据", Toast.LENGTH_SHORT).show()
            return
        }

        val grad = GradientColor(Color.rgb(252, 0, 255), Color.rgb(0, 219, 222))
        val barSet = BarDataSet(barEntries, "柱状").apply {
            setGradientColors(List(barEntries.size) { grad })
            valueTextSize = 12f
        }
        val lineSet = LineDataSet(lineEntries, "折线").apply {
            val c = Color.parseColor("#D1C4E9")
            color = c; setCircleColor(c)
            lineWidth = 2f; setDrawCircles(true)
            valueTextSize = 12f
        }

        val combinedData = CombinedData().apply {
            setData(BarData(barSet))
            setData(LineData(lineSet))
        }

        combinedChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        combinedChart.xAxis.axisMinimum    = -1f
        combinedChart.xAxis.axisMaximum    = labels.size.toFloat()

        combinedChart.data = combinedData
        combinedChart.notifyDataSetChanged()
        combinedChart.animateY(400)
        combinedChart.invalidate()

        Toast.makeText(this, "混合图已刷新", Toast.LENGTH_SHORT).show()
    }

    /** 启动系统文件选取器，选 CSV 导入 */
    private fun importCsv() {
        val importIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/csv"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(importIntent, REQUEST_IMPORT_CSV)
    }

    /** 导出当前输入为 CSV 并保存 */
    private fun exportCsv() {
        val fileName = "Chartly_${System.currentTimeMillis()}.csv"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Chartly")
            }
            val targetUri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
            if (targetUri != null) {
                resolver.openOutputStream(targetUri)?.bufferedWriter()?.use { writer ->
                    repeat(inputLayout.childCount) { i ->
                        val row    = inputLayout.getChildAt(i) as LinearLayout
                        val label  = (row.getChildAt(0) as EditText).text.toString()
                        val barVal = (row.getChildAt(1) as EditText).text.toString()
                        val lineVal= (row.getChildAt(2) as EditText).text.toString()
                        writer.append("$label,$barVal,$lineVal\n")
                    }
                }
                Toast.makeText(this, "已导出到 Documents/Chartly/$fileName", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            val exportDir = getExternalFilesDir(null)
                ?.resolve("csv_exports")
                ?.apply { mkdirs() }
            val outFile = exportDir!!.resolve(fileName)
            outFile.bufferedWriter().use { writer ->
                repeat(inputLayout.childCount) { i ->
                    val row    = inputLayout.getChildAt(i) as LinearLayout
                    val label  = (row.getChildAt(0) as EditText).text.toString()
                    val barVal = (row.getChildAt(1) as EditText).text.toString()
                    val lineVal= (row.getChildAt(2) as EditText).text.toString()
                    writer.append("$label,$barVal,$lineVal\n")
                }
            }
            Toast.makeText(this, "已导出到 ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMPORT_CSV && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return

            // 先清空所有输入行
            inputLayout.removeAllViews()

            // 读 CSV，逐行添加输入行并填充
            contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                lines.forEach { line ->
                    val cols = line.split(",")
                    if (cols.size >= 3) {
                        addInputRow()
                        val idx = inputLayout.childCount - 1
                        val row = inputLayout.getChildAt(idx) as LinearLayout
                        (row.getChildAt(0) as EditText).setText(cols[0].trim())
                        (row.getChildAt(1) as EditText).setText(cols[1].trim())
                        (row.getChildAt(2) as EditText).setText(cols[2].trim())
                    }
                }
            }

            // 导入完毕，直接刷新图表
            refreshCombinedChart()

            Toast.makeText(this, "CSV 导入完成", Toast.LENGTH_SHORT).show()
        }
    }


    override fun getChart(): Chart<*>? = combinedChart

    /** 可选：序列化输入、接收外部数据 */
    override fun collectChartData(): ArrayList<String>? {
        val data = ArrayList<String>()
        repeat(inputLayout.childCount) { i ->
            val row = inputLayout.getChildAt(i) as LinearLayout
            val l   = (row.getChildAt(0) as EditText).text.toString()
            val b   = (row.getChildAt(1) as EditText).text.toString()
            val ln  = (row.getChildAt(2) as EditText).text.toString()
            data.add("$l|$b|$ln")
        }
        return if (data.isNotEmpty()) data else null
    }

    override fun processIncomingData(data: ArrayList<String>?) {
        if (data.isNullOrEmpty()) return
        inputLayout.removeAllViews()
        data.forEach { item ->
            val parts = item.split("|")
            if (parts.size == 3) {
                addInputRow()
                val row = inputLayout.getChildAt(inputLayout.childCount - 1) as LinearLayout
                (row.getChildAt(0) as EditText).setText(parts[0])
                (row.getChildAt(1) as EditText).setText(parts[1])
                (row.getChildAt(2) as EditText).setText(parts[2])
            }
        }
        refreshCombinedChart()
    }
}
