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
import com.github.mikephil.charting.charts.BubbleChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class BubbleChartActivity : DrawerActivity() {

    companion object {
        private const val REQUEST_IMPORT_CSV = 3001
        private const val REQUEST_EXPORT_CSV = 3002
    }

    private lateinit var inputLayout: LinearLayout
    private lateinit var addBubbleButton: Button
    private lateinit var saveChartButton: Button
    private lateinit var bubbleChart: BubbleChart
    private lateinit var tvTitle: TextView
    private lateinit var markerView: CustomMarkerView

    private val entries = mutableListOf<BubbleEntry>()
    private lateinit var dataSet: BubbleDataSet

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_bubble_chart)

        // 处理系统栏内边距
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById<View>(R.id.main),
            OnApplyWindowInsetsListener { v, insets ->
                val sys = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                v!!.setPadding(sys.left, sys.top, sys.right, sys.bottom)
                insets
            }
        )

        inputLayout     = findViewById(R.id.inputLayout)
        addBubbleButton = findViewById(R.id.addBubbleButton)
        saveChartButton = findViewById(R.id.saveChartButton)
        bubbleChart     = findViewById(R.id.bubbleChart)
        tvTitle         = findViewById(R.id.tvTitle)
        tvTitle.text    = "气泡图"

        markerView      = CustomMarkerView(this, R.layout.marker_view)
        markerView.chartView = bubbleChart
        bubbleChart.marker   = markerView

        setupChart()

        // 初始一行
        addInputFields()
        addBubbleButton.setOnClickListener { addInputFields() }
        addBubbleButton.setOnLongClickListener {
            showAddMultipleRowsDialog()
            true
        }
        saveChartButton.setOnClickListener { saveAllBubblesAndRefreshChart() }

        findViewById<CustomTitleBar>(R.id.customTitleBar).apply {
            onDownloadClick = {
                getChart()?.let { chart ->
                    saveChartToPng(chart, this@BubbleChartActivity,
                        "Chartly_${System.currentTimeMillis()}.png")
                } ?: Toast.makeText(this@BubbleChartActivity,
                    "当前没有可导出的图表", Toast.LENGTH_SHORT).show()
            }
            onUploadClick = {
                AlertDialog.Builder(this@BubbleChartActivity)
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

    private fun setupChart() {
        dataSet = BubbleDataSet(entries, "气泡数据").apply {
            valueTextSize = 12f
        }

        bubbleChart.apply {
            data = BubbleData(dataSet)
            animateY(500)
            setScaleEnabled(true)
            isDragEnabled = true
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            axisLeft.granularity = 1f
            // 避免气泡裁剪
            setExtraOffsets(20f, 10f, 20f, 10f)
        }
    }

    /** 动态生成标签、X 值、Y 值 三个输入框 */
    private fun addInputFields() {
        val etLabel = EditText(this).apply {
            hint = "标签"
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val etX = EditText(this).apply {
            hint = "X 值"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val etY = EditText(this).apply {
            hint = "Y 值"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = 8
            }
            addView(etLabel)
            addView(etX)
            addView(etY)
        }
        inputLayout.addView(row)
    }

    /** 保存并刷新气泡图：用 X、Y，size = Y */
    private fun saveAllBubblesAndRefreshChart() {
        entries.clear()
        val labels = mutableListOf<String>()

        for (i in 0 until inputLayout.childCount) {
            val row   = inputLayout.getChildAt(i) as LinearLayout
            val label = (row.getChildAt(0) as EditText).text.toString()
                .takeIf { it.isNotBlank() } ?: "#${i+1}"
            val xVal  = (row.getChildAt(1) as EditText).text.toString().toFloatOrNull()
            val yVal  = (row.getChildAt(2) as EditText).text.toString().toFloatOrNull()
            if (xVal != null && yVal != null) {
                // 用 sqrt(y) 控制气泡大小
                val size = kotlin.math.sqrt(yVal)
                entries.add(BubbleEntry(xVal, yVal, size).apply { setData(label) })
                labels.add(label)
            }
        }

        if (entries.isEmpty()) {
            Toast.makeText(this, "请先输入有效的气泡数据", Toast.LENGTH_SHORT).show()
            return
        }

        dataSet.values = entries
        bubbleChart.data = BubbleData(dataSet)
        bubbleChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        // X 轴范围
        val xs = entries.map { it.x }
        bubbleChart.xAxis.axisMinimum = (xs.minOrNull() ?: 0f) - 1f
        bubbleChart.xAxis.axisMaximum = (xs.maxOrNull() ?: 0f) + 1f

        // Y 轴范围
        val ys = entries.map { it.y }
        val yMin = ys.minOrNull() ?: 0f
        val yMax = ys.maxOrNull() ?: 0f
        val range = (yMax - yMin).takeIf { it>0 } ?: 1f
        bubbleChart.axisLeft.axisMinimum  = yMin - range*0.1f
        bubbleChart.axisLeft.axisMaximum  = yMax + range*0.1f

        bubbleChart.notifyDataSetChanged()
        bubbleChart.animateY(300)
        bubbleChart.invalidate()

        Toast.makeText(this, "气泡图已刷新", Toast.LENGTH_SHORT).show()
    }

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
                    if (count > 0) repeat(count) { addInputFields() }
                    else Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show()
                } ?: Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 启动 CSV 导入 */
    private fun importCsv() {
        val importIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/csv"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(importIntent, REQUEST_IMPORT_CSV)
    }

    /** 导出为 CSV (标签,X,Y) */
    private fun exportCsv() {
        val fileName = "Chartly_${System.currentTimeMillis()}.csv"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Chartly")
            }
            resolver.insert(MediaStore.Files.getContentUri("external"), values)?.let { uri ->
                resolver.openOutputStream(uri)?.bufferedWriter()?.use { w ->
                    repeat(inputLayout.childCount) { i ->
                        val row = inputLayout.getChildAt(i) as LinearLayout
                        val lab = (row.getChildAt(0) as EditText).text.toString()
                        val x   = (row.getChildAt(1) as EditText).text.toString()
                        val y   = (row.getChildAt(2) as EditText).text.toString()
                        w.append("$lab,$x,$y\n")
                    }
                }
                Toast.makeText(this, "已导出到 Documents/Chartly/$fileName", Toast.LENGTH_LONG).show()
            } ?: Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show()
        } else {
            val dir = getExternalFilesDir(null)?.resolve("csv_exports")?.apply { mkdirs() }
            val out = dir!!.resolve(fileName)
            out.bufferedWriter().use { w ->
                repeat(inputLayout.childCount) { i ->
                    val row = inputLayout.getChildAt(i) as LinearLayout
                    val lab = (row.getChildAt(0) as EditText).text.toString()
                    val x   = (row.getChildAt(1) as EditText).text.toString()
                    val y   = (row.getChildAt(2) as EditText).text.toString()
                    w.append("$lab,$x,$y\n")
                }
            }
            Toast.makeText(this, "已导出到 ${out.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMPORT_CSV && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            inputLayout.removeAllViews()
            contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                lines.forEach { raw ->
                    val line = raw.trim()
                    if (line.isEmpty()) return@forEach
                    val cols = line.split(",")
                    if (cols.size < 3) return@forEach

                    addInputFields()
                    val idx = inputLayout.childCount - 1
                    val row = inputLayout.getChildAt(idx) as LinearLayout
                    (row.getChildAt(0) as EditText).setText(cols[0].trim())
                    (row.getChildAt(1) as EditText).setText(cols[1].trim())
                    (row.getChildAt(2) as EditText).setText(cols[2].trim())
                }
            }
            saveAllBubblesAndRefreshChart()  // 导入后立即刷新
            Toast.makeText(this, "CSV 导入完成", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getChart(): Chart<*>? = bubbleChart
}
