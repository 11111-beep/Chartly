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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class PieChartActivity : DrawerActivity() {

    companion object {
        private const val REQUEST_IMPORT_CSV = 3001
        private const val REQUEST_EXPORT_CSV = 3002
    }

    private lateinit var inputLayout: LinearLayout
    private lateinit var addBarButton: View
    private lateinit var saveChartButton: View
    private lateinit var pieChart: PieChart
    private lateinit var tvTitle: TextView

    private val entries = mutableListOf<PieEntry>()
    private lateinit var dataSet: PieDataSet
    private var markerView: CustomMarkerView? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_chart)

        inputLayout     = findViewById(R.id.inputLayout)
        addBarButton    = findViewById(R.id.addBarButton)
        saveChartButton = findViewById(R.id.saveChartButton)
        pieChart        = findViewById(R.id.pieChart)
        tvTitle         = findViewById(R.id.tvTitle)

        tvTitle.text = "饼状图"
        markerView = CustomMarkerView(this, R.layout.marker_view)
        pieChart.marker = markerView
        markerView?.chartView = pieChart
        setupChart()
        addInputFields()

        // 添加一行
        addBarButton.setOnClickListener { addInputFields() }
        // 长按批量添加
        addBarButton.setOnLongClickListener {
            showAddMultipleRowsDialog()
            true
        }
        // 刷新图表
        saveChartButton.setOnClickListener { saveAllBarsAndRefreshChart() }

        // 导入/导出 CSV
        findViewById<CustomTitleBar>(R.id.customTitleBar).apply {
            onUploadClick = {
                AlertDialog.Builder(this@PieChartActivity)
                    .setTitle("CSV 操作")
                    .setItems(arrayOf("导入 CSV", "导出 CSV")) { _, which ->
                        when (which) {
                            0 -> importCsv()
                            1 -> exportCsv()
                        }
                    }
                    .show()
            }
            onDownloadClick = {
                getChart()?.let { chart ->
                    saveChartToPng(chart, this@PieChartActivity,
                        "Chartly_${System.currentTimeMillis()}.png")
                } ?: Toast.makeText(
                    this@PieChartActivity,
                    "当前没有可导出的图表",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // 配置 PieChart的样式
    private fun setupChart() {
        // 创建饼状图数据集
        dataSet = PieDataSet(entries, "数据分布").apply {  // 初始化数据集，并设置标签为"数据分布"
            sliceSpace = 3f  // 设置饼块之间的间距
            selectionShift = 5f  // 设置饼块被选中时的偏移量
            colors = ColorTemplate.MATERIAL_COLORS.toList()  // 使用预定义的Material配色方案
            // colors = ColorTemplate.JOYFUL_COLORS.toList() // 明快风格
            // colors = ColorTemplate.PASTEL_COLORS.toList() // 柔和风格
            valueTextSize = 12f  // 设置数值文本的大小
            valueTextColor = Color.WHITE  // 设置数值文本的颜色为白色
        }

        // 配置饼状图
        pieChart.apply {
            // 设置数据集
            data = PieData(dataSet).apply {  // 创建并设置饼状图的数据
                setDrawValues(true)  // 启用绘制数值
                setValueFormatter(PercentFormatter(pieChart))  // 设置数值格式化器为百分比格式
            }
            description.isEnabled = false  // 禁用饼状图的描述
            isDrawHoleEnabled = false  // 禁用环形图（甜甜圈效果）
            holeRadius = 40f  // 设置环形的半径
            transparentCircleRadius = 45f  // 设置透明圆的半径
            setUsePercentValues(true)  // 启用百分比值显示
            setEntryLabelColor(Color.BLACK)  // 设置饼块标签的颜色为黑色
            animateY(500)  // 以500毫秒的动画显示饼状图
        }
    }


    private fun addInputFields() {
        val etLabel = EditText(this).apply {
            hint         = "标签"
            inputType    = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val etValue = EditText(this).apply {
            hint         = "数值"
            inputType    = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val row = LinearLayout(this).apply {
            orientation  = LinearLayout.HORIZONTAL
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
            hint      = "请输入要添加的数据点数量"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(this)
            .setTitle("批量添加数据点")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                input.text.toString().toIntOrNull()
                    ?.takeIf { it > 0 }
                    ?.let { count ->
                        repeat(count) { addInputFields() }
                    }
                    ?: Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveAllBarsAndRefreshChart() {
        entries.clear()
        for (i in 0 until inputLayout.childCount) {
            val row   = inputLayout.getChildAt(i) as LinearLayout
            val label = (row.getChildAt(0) as EditText)
                .text.toString()
                .takeIf { it.isNotBlank() }
                ?: "＃${i + 1}"
            val value = (row.getChildAt(1) as EditText)
                .text.toString()
                .toFloatOrNull()
            if (value != null && value > 0f) {
                entries.add(PieEntry(value, label))
            }
        }
        if (entries.isEmpty()) {
            Toast.makeText(this, "请先输入有效的数据", Toast.LENGTH_SHORT).show()
            return
        }
        dataSet.values = entries
        pieChart.data  = PieData(dataSet).apply {
            setDrawValues(true)
            setValueFormatter(PercentFormatter(pieChart))
        }
        pieChart.invalidate()
        pieChart.animateY(500)
        Toast.makeText(this, "饼状图已刷新", Toast.LENGTH_SHORT).show()
    }

    // ───────── CSV Import/Export ─────────

    private fun importCsv() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/csv"; addCategory(Intent.CATEGORY_OPENABLE)
        }.also { startActivityForResult(it, REQUEST_IMPORT_CSV) }
    }

    private fun exportCsv() {
        val fileName = "Chartly_${System.currentTimeMillis()}.csv"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Chartly")
            }
            resolver.insert(MediaStore.Files.getContentUri("external"), values)
                ?.let { uri ->
                    resolver.openOutputStream(uri)?.bufferedWriter()?.use { w ->
                        entries.forEach { e ->
                            w.append("${e.value},${e.label}\n")
                        }
                    }
                    Toast.makeText(
                        this,
                        "已导出到 Documents/Chartly/$fileName",
                        Toast.LENGTH_LONG
                    ).show()
                }
                ?: Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show()
        } else {
            val dir  = getExternalFilesDir(null)?.resolve("csv_exports")!!.apply { mkdirs() }
            val file = dir.resolve(fileName)
            file.bufferedWriter().use { w ->
                entries.forEach { e ->
                    w.append("${e.value},${e.label}\n")
                }
            }
            Toast.makeText(this, "已导出到 ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMPORT_CSV && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                inputLayout.removeAllViews()
                var count = 0
                contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                    lines.forEach { line ->
                        val cols = line.split(",")
                        if (cols.size >= 2) {
                            addInputFields()
                            val idx = inputLayout.childCount - 1
                            val row = inputLayout.getChildAt(idx) as LinearLayout
                            // CSV assumed format "value,label"
                            (row.getChildAt(0) as EditText).setText(cols[1].trim())
                            (row.getChildAt(1) as EditText).setText(cols[0].trim())
                            count++
                        }
                    }
                }
                if (count > 0) {
                    saveAllBarsAndRefreshChart()
                    Toast.makeText(this, "已导入 $count 行", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "CSV 没有有效数据", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ───────── Sidebar Data Passing ─────────

    override fun collectChartData(): ArrayList<String>? {
        val data = ArrayList<String>()
        for (i in 0 until inputLayout.childCount) {
            val row   = inputLayout.getChildAt(i) as LinearLayout
            val label = (row.getChildAt(0) as EditText).text.toString()
            val value = (row.getChildAt(1) as EditText).text.toString()
            data.add("$label|$value")
        }
        return if (data.isNotEmpty()) data else null
    }

    override fun processIncomingData(data: ArrayList<String>?) {
        if (data.isNullOrEmpty()) return
        inputLayout.removeAllViews()
        data.forEach { item ->
            val parts = item.split("|", limit = 2)
            if (parts.size == 2) {
                addInputFields()
                val row = inputLayout.getChildAt(inputLayout.childCount - 1) as LinearLayout
                (row.getChildAt(0) as EditText).setText(parts[0])
                (row.getChildAt(1) as EditText).setText(parts[1])
            }
        }
        saveAllBarsAndRefreshChart()
    }

    override fun getChart(): PieChart? = pieChart
}
