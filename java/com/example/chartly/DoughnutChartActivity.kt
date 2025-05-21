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
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class DoughnutChartActivity : DrawerActivity() {

    companion object {
        private const val REQUEST_IMPORT_CSV = 5001
        private const val REQUEST_EXPORT_CSV = 5002
    }

    private lateinit var inputLayout: LinearLayout
    private lateinit var addBarButton: Button
    private lateinit var saveChartButton: Button
    private lateinit var pieChart: PieChart
    private lateinit var tvTitle: TextView

    private val entries = mutableListOf<PieEntry>()
    private lateinit var dataSet: PieDataSet
    private lateinit var markerView: CustomMarkerView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_doughnut_chart)

        // 处理系统栏内边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            WindowInsetsCompat.CONSUMED
        }

        inputLayout     = findViewById(R.id.inputLayout)
        addBarButton    = findViewById(R.id.addBarButton)
        saveChartButton = findViewById(R.id.saveChartButton)
        pieChart        = findViewById(R.id.pieChart)
        tvTitle         = findViewById(R.id.tvTitle)
        tvTitle.text    = "圆环图"
        markerView = CustomMarkerView(this, R.layout.marker_view)
        pieChart.marker = markerView
        markerView.chartView = pieChart

        setupChart()
        addInputFields()

        addBarButton.setOnClickListener { addInputFields() }
        addBarButton.setOnLongClickListener {
            showAddMultipleRowsDialog()
            true
        }
        saveChartButton.setOnClickListener { saveAllBarsAndRefreshChart() }

        findViewById<CustomTitleBar>(R.id.customTitleBar).apply {
            // 下载图标
            onDownloadClick = {
                getChart()?.let { chart ->
                    saveChartToPng(
                        chart,
                        this@DoughnutChartActivity,
                        "Chartly_${System.currentTimeMillis()}.png"
                    )
                } ?: Toast.makeText(
                    this@DoughnutChartActivity,
                    "当前没有可导出的图表",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // 上传图标：CSV 导入/导出
            onUploadClick = {
                AlertDialog.Builder(this@DoughnutChartActivity)
                    .setTitle("CSV 操作")
                    .setItems(arrayOf("导入 CSV", "导出 CSV")) { _, which ->
                        if (which == 0) importCsv() else exportCsv()
                    }
                    .show()
            }
        }
    }

    private fun setupChart() {
        dataSet = PieDataSet(entries, "数据分布").apply {
            sliceSpace = 3f
            selectionShift = 5f
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }
        pieChart.apply {
            data = PieData(dataSet).apply {
                setDrawValues(true)
                setValueFormatter(PercentFormatter(pieChart))
            }
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            transparentCircleRadius = 45f
            setUsePercentValues(true)
            setEntryLabelColor(Color.BLACK)
            animateY(500)
        }
    }

    private fun addInputFields() {
        val etLabel = EditText(this).apply {
            hint = "标签"
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val etValue = EditText(this).apply {
            hint = "数值"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8 }
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
                input.text.toString().toIntOrNull()?.let { c ->
                    if (c > 0) repeat(c) { addInputFields() }
                    else Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show()
                } ?: Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveAllBarsAndRefreshChart() {
        entries.clear()
        for (i in 0 until inputLayout.childCount) {
            val row = inputLayout.getChildAt(i) as LinearLayout
            val label = (row.getChildAt(0) as EditText).text.toString()
                .takeIf { it.isNotBlank() } ?: "#${i+1}"
            val value = (row.getChildAt(1) as EditText).text
                .toString().toFloatOrNull() ?: 0f
            if (value > 0f) {
                entries.add(PieEntry(value, label))
            }
        }
        if (entries.isEmpty()) {
            Toast.makeText(this, "请先输入有效的数据", Toast.LENGTH_SHORT).show()
            return
        }
        dataSet.values = entries
        pieChart.data = PieData(dataSet).apply {
            setDrawValues(true)
            setValueFormatter(PercentFormatter(pieChart))
        }
        pieChart.invalidate()
        pieChart.animateY(500)
        val total = entries.sumOf { it.value.toDouble() }.toFloat()
        pieChart.centerText = "总计\n$total"
        pieChart.setCenterTextSize(16f)
        pieChart.setCenterTextColor(Color.BLACK)
        Toast.makeText(this, "圆环图已刷新", Toast.LENGTH_SHORT).show()
    }

    // ================= CSV 导入导出 =================

    private fun importCsv() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/csv"
            addCategory(Intent.CATEGORY_OPENABLE)
        }.also { startActivityForResult(it, REQUEST_IMPORT_CSV) }
    }

    private fun exportCsv() {
        val fn = "Chartly_${System.currentTimeMillis()}.csv"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fn)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Chartly")
            }
            resolver.insert(MediaStore.Files.getContentUri("external"), cv)?.let { uri ->
                resolver.openOutputStream(uri)?.bufferedWriter()?.use { w ->
                    repeat(inputLayout.childCount) { i ->
                        val row = inputLayout.getChildAt(i) as LinearLayout
                        val label = (row.getChildAt(0) as EditText).text.toString()
                        val value = (row.getChildAt(1) as EditText).text.toString()
                        w.append("$label,$value\n")
                    }
                }
                Toast.makeText(this, "已导出到 Documents/Chartly/$fn", Toast.LENGTH_LONG).show()
            } ?: Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show()
        } else {
            val dir = getExternalFilesDir(null)?.resolve("csv_exports")!!.apply { mkdirs() }
            val file = dir.resolve(fn)
            file.bufferedWriter().use { w ->
                repeat(inputLayout.childCount) { i ->
                    val row = inputLayout.getChildAt(i) as LinearLayout
                    val label = (row.getChildAt(0) as EditText).text.toString()
                    val value = (row.getChildAt(1) as EditText).text.toString()
                    w.append("$label,$value\n")
                }
            }
            Toast.makeText(this, "已导出到 ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMPORT_CSV && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            inputLayout.removeAllViews()
            var valid = 0
            contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                lines.forEach { raw ->
                    val cols = raw.trim().split(",")
                    if (cols.size >= 2) {
                        addInputFields()
                        val idx = inputLayout.childCount - 1
                        val row = inputLayout.getChildAt(idx) as LinearLayout
                        (row.getChildAt(0) as EditText).setText(cols[0].trim())
                        (row.getChildAt(1) as EditText).setText(cols[1].trim())
                        valid++
                    }
                }
            }
            if (valid == 0) {
                Toast.makeText(this, "导入的 CSV 中没有有效数据", Toast.LENGTH_SHORT).show()
            } else {
                saveAllBarsAndRefreshChart()
                Toast.makeText(this, "CSV 导入完成，共 $valid 行", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 收集数据供侧边栏传递 */
    override fun collectChartData(): ArrayList<String>? {
        val list = ArrayList<String>()
        for (i in 0 until inputLayout.childCount) {
            val row = inputLayout.getChildAt(i) as LinearLayout
            val label = (row.getChildAt(0) as EditText).text.toString().ifBlank { "#${i+1}" }
            val value = (row.getChildAt(1) as EditText).text.toString().toFloatOrNull() ?: continue
            list.add("$label|$value")
        }
        return list.ifEmpty { null }
    }

    /** 接收来自侧边栏的传入数据 */
    override fun processIncomingData(data: ArrayList<String>?) {
        data ?: return
        inputLayout.removeAllViews()
        data.forEach { item ->
            val (label, value) = item.split("|", limit = 2)
            addInputFields()
            val row = inputLayout.getChildAt(inputLayout.childCount - 1) as LinearLayout
            (row.getChildAt(0) as EditText).setText(label)
            (row.getChildAt(1) as EditText).setText(value)
        }
        saveAllBarsAndRefreshChart()
    }


    override fun getChart(): Chart<*>? = pieChart
}
