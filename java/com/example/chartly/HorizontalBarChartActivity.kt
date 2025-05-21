package com.example.chartly

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.google.android.material.button.MaterialButton

class HorizontalBarChartActivity : DrawerActivity() {

    companion object {
        private const val REQUEST_IMPORT_CSV = 5001
        private const val REQUEST_EXPORT_CSV = 5002
        const val EXTRA_CHART_DATA      = "com.example.chartly.EXTRA_CHART_DATA"
    }

    private lateinit var inputLayout: LinearLayout
    private lateinit var addBarButton: MaterialButton
    private lateinit var saveChartButton: MaterialButton
    private lateinit var horizontalBarChart: HorizontalBarChart
    private lateinit var tvTitle: TextView
    private lateinit var markerView: CustomMarkerView

    private val entries = mutableListOf<BarEntry>()
    private val labels  = mutableListOf<String>()
    private lateinit var dataSet: BarDataSet

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horizontal_bar_chart)

        inputLayout       = findViewById(R.id.inputLayout)
        addBarButton      = findViewById(R.id.addBarButton)
        saveChartButton   = findViewById(R.id.saveChartButton)
        horizontalBarChart= findViewById(R.id.horizontalbarChart)
        tvTitle           = findViewById(R.id.tvTitle)

        tvTitle.text = "水平柱状图"
        markerView = CustomMarkerView(this, R.layout.marker_view)
        horizontalBarChart.marker = markerView
        markerView.chartView = horizontalBarChart
        horizontalBarChart.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        // 在 setContentView(...) 之后
        findViewById<CustomTitleBar>(R.id.customTitleBar).apply {
            onDownloadClick = {
                getChart()?.let { chart ->
                    saveChartToPng(
                        chart,
                        App.instance,
                        "Chartly_${System.currentTimeMillis()}.png"
                    )
                } ?: Toast.makeText(
                    App.instance,
                    "当前没有可导出的图表",
                    Toast.LENGTH_SHORT
                ).show()
            }
            onUploadClick = {
                AlertDialog.Builder(this@HorizontalBarChartActivity)
                    .setTitle("CSV 操作")
                    .setItems(arrayOf("导入 CSV", "导出 CSV")) { _, which ->
                        if (which == 0) importCsv() else exportCsv()
                    }
                    .show()
            }
        }

        setupChart()
        addInputFields()

        addBarButton.setOnClickListener { addInputFields() }
        addBarButton.setOnLongClickListener {
            showAddMultipleRowsDialog()
            true
        }
        saveChartButton.setOnClickListener { saveAllBarsAndRefreshChart() }

        // 如果有上一个页面的数据，马上填充
        processIncomingData(intent.getStringArrayListExtra(EXTRA_CHART_DATA))
    }

    private fun setupChart() {
        dataSet = BarDataSet(entries, "数据柱").apply {
            valueTextSize = 12f
            setDrawValues(true)
        }

        horizontalBarChart.apply {
            data = BarData(dataSet).apply { barWidth = 0.8f }
            animateY(500)
            setDrawValueAboveBar(true)
            setScaleEnabled(true)
            isDragEnabled = true
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = true

            // —— 关键：先把 formatter 和刻度都配好 ——
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
                setDrawLabels(true)
            }

            axisLeft.granularity = 1f

            setFitBars(true)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)

            renderer = object : com.github.mikephil.charting.renderer.HorizontalBarChartRenderer(
                this, animator, viewPortHandler
            ) {
                override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
                    val w = (mChart as HorizontalBarChart).width.toFloat()
                    mRenderPaint.shader = LinearGradient(
                        0f, 0f, w, 0f,
                        Color.rgb(252,0,255),
                        Color.rgb(0,219,222),
                        Shader.TileMode.CLAMP
                    )
                    super.drawDataSet(c, dataSet, index)
                    mRenderPaint.shader = null
                }
            }

            // —— 关键：第一次也要刷新一次 ——
            data.notifyDataChanged()
            notifyDataSetChanged()
            invalidate()
        }
    }


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
            hint = "要添加的数据点数量"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(this)
            .setTitle("批量添加数据点")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                input.text.toString().toIntOrNull()
                    ?.takeIf { it > 0 }
                    ?.let { repeat(it) { addInputFields() } }
                    ?: Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "请先输入有效的数据", Toast.LENGTH_SHORT).show()
            horizontalBarChart.clear()
            return
        }
        dataSet.values = entries
        horizontalBarChart.data = BarData(dataSet).apply { /* 可加渐变等 */ }
        horizontalBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        horizontalBarChart.notifyDataSetChanged()
        horizontalBarChart.invalidate()
        Toast.makeText(this, "水平柱状图已刷新", Toast.LENGTH_SHORT).show()
    }

    // ─────── 数据传递 ───────

    override fun collectChartData(): ArrayList<String>? {
        val out = ArrayList<String>()
        repeat(inputLayout.childCount) { i ->
            val row = inputLayout.getChildAt(i) as LinearLayout
            val a = (row.getChildAt(0) as EditText).text.toString()
            val b = (row.getChildAt(1) as EditText).text.toString()
            out.add("$a|$b")
        }
        return out.ifEmpty { null }
    }

    override fun processIncomingData(data: ArrayList<String>?) {
        data?.let {
            inputLayout.removeAllViews()
            it.forEach { line ->
                val parts = line.split("|", limit = 2)
                if (parts.size == 2) {
                    addInputFields()
                    val idx = inputLayout.childCount - 1
                    val row = inputLayout.getChildAt(idx) as LinearLayout
                    (row.getChildAt(0) as EditText).setText(parts[0])
                    (row.getChildAt(1) as EditText).setText(parts[1])
                }
            }
            saveAllBarsAndRefreshChart()
        }
    }

    // ─────── CSV 导入/导出 ───────

    private fun importCsv() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/csv"; addCategory(Intent.CATEGORY_OPENABLE)
        }.also { startActivityForResult(it, REQUEST_IMPORT_CSV) }
    }

    private fun exportCsv() {
        val fn = "Chartly_${System.currentTimeMillis()}.csv"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val r = contentResolver
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fn)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Chartly")
            }
            r.insert(MediaStore.Files.getContentUri("external"), cv)?.let { uri ->
                r.openOutputStream(uri)?.bufferedWriter()?.use { w ->
                    entries.forEachIndexed { i, e ->
                        w.append("${labels[i]},${e.y}\n")
                    }
                }
                Toast.makeText(this, "已导出到 Documents/Chartly/$fn", Toast.LENGTH_LONG).show()
            } ?: Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show()
        } else {
            val dir = getExternalFilesDir(null)?.resolve("csv_exports")!!.apply { mkdirs() }
            val f   = dir.resolve(fn)
            f.bufferedWriter().use { w ->
                entries.forEachIndexed { i, e ->
                    w.append("${labels[i]},${e.y}\n")
                }
            }
            Toast.makeText(this, "已导出到 ${f.absolutePath}", Toast.LENGTH_LONG).show()
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
                            (row.getChildAt(0) as EditText).setText(cols[0].trim())
                            (row.getChildAt(1) as EditText).setText(cols[1].trim())
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

    override fun getChart(): Chart<*>? = horizontalBarChart
}
