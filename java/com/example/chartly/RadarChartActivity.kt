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
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.button.MaterialButton

class RadarChartActivity : DrawerActivity() {

    companion object {
        private const val REQUEST_IMPORT_CSV = 5001
        private const val REQUEST_EXPORT_CSV = 5002
        const val EXTRA_CHART_DATA      = "com.example.chartly.EXTRA_CHART_DATA"
    }

    // UI 组件
    private lateinit var inputLayout: LinearLayout
    private lateinit var addRadarButton: MaterialButton
    private lateinit var saveChartButton: MaterialButton
    private lateinit var radarChart: RadarChart
    private lateinit var tvTitle: TextView
    private lateinit var markerView: CustomMarkerView

    // 数据
    private val entries = mutableListOf<RadarEntry>()
    private val labels  = mutableListOf<String>()
    private lateinit var dataSet: RadarDataSet

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_radar_chart)

        // 处理系统栏内边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<View>(R.id.main)) { v, insets ->
            val sys = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            WindowInsetsCompat.CONSUMED
        }

        inputLayout      = findViewById(R.id.inputLayout)
        addRadarButton   = findViewById(R.id.addRadarButton)
        saveChartButton  = findViewById(R.id.saveChartButton)
        radarChart       = findViewById(R.id.radarChart)
        tvTitle          = findViewById(R.id.tvTitle)

        markerView       = CustomMarkerView(this, R.layout.marker_view)
        markerView.chartView = radarChart
        radarChart.marker     = markerView

        tvTitle.text = "雷达图"

        setupChart()
        addInputFields()

        addRadarButton.setOnClickListener { addInputFields() }
        addRadarButton.setOnLongClickListener {
            showAddMultipleRowsDialog()
            true
        }
        saveChartButton.setOnClickListener { saveAllAndRefresh() }

        // 下载 & CSV 按钮
        findViewById<CustomTitleBar>(R.id.customTitleBar).apply {
            onDownloadClick = {
                getChart()?.let { chart ->
                    saveChartToPng(chart, this@RadarChartActivity,
                        "Chartly_${System.currentTimeMillis()}.png")
                } ?: Toast.makeText(
                    this@RadarChartActivity,
                    "当前没有可导出的图表",
                    Toast.LENGTH_SHORT
                ).show()
            }
            onUploadClick = {
                AlertDialog.Builder(this@RadarChartActivity)
                    .setTitle("CSV 操作")
                    .setItems(arrayOf("导入 CSV", "导出 CSV")) { _, which ->
                        if (which == 0) importCsv() else exportCsv()
                    }.show()
            }
        }

        // 如果 Intent 带来了上一个视图的数据，就恢复
        processIncomingData(intent.getStringArrayListExtra(EXTRA_CHART_DATA))
    }

    private fun setupChart() {
        dataSet = RadarDataSet(entries, "维度评分").apply {
            color = ColorTemplate.getHoloBlue()
            fillColor = ColorTemplate.getHoloBlue()
            setDrawFilled(true)
            lineWidth = 2f
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }
        radarChart.apply {
            description.isEnabled = false
            webLineWidth = 1f; webColor = Color.GRAY
            webLineWidthInner = 1f; webColorInner = Color.LTGRAY
            webAlpha = 100
            animateXY(500, 500)
            data = RadarData(dataSet)
        }
    }

    private fun addInputFields() {
        fun mk(hint: String): EditText =
            EditText(this).apply {
                this.hint = hint
                inputType = if (hint == "标签") {
                    InputType.TYPE_CLASS_TEXT
                } else {
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                }
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = 8
            }
            addView(mk("标签"))
            addView(mk("数值"))
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
                input.text.toString().toIntOrNull()?.takeIf { it > 0 }?.let { cnt ->
                    repeat(cnt) { addInputFields() }
                } ?: Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveAllAndRefresh() {
        entries.clear()
        labels.clear()
        for (i in 0 until inputLayout.childCount) {
            val row   = inputLayout.getChildAt(i) as LinearLayout
            val lab   = (row.getChildAt(0) as EditText).text.toString()
                .takeIf { it.isNotBlank() } ?: "维度${i+1}"
            val v     = (row.getChildAt(1) as EditText).text.toString().toFloatOrNull()
            if (v != null && v >= 0f) {
                entries.add(RadarEntry(v))
                labels.add(lab)
            }
        }
        if (entries.isEmpty()) {
            Toast.makeText(this, "请输入有效的数据", Toast.LENGTH_SHORT).show()
            radarChart.clear()
            return
        }
        dataSet.values = entries
        radarChart.data = RadarData(dataSet)
        radarChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        radarChart.invalidate()
        radarChart.animateXY(500, 500)
        Toast.makeText(this, "雷达图已刷新", Toast.LENGTH_SHORT).show()
    }

    // ─── CSV 导入/导出 ───

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
                    repeat(inputLayout.childCount) { i ->
                        val row = inputLayout.getChildAt(i) as LinearLayout
                        val lab = (row.getChildAt(0) as EditText).text.toString()
                        val v   = (row.getChildAt(1) as EditText).text.toString()
                        w.append("$lab,$v\n")
                    }
                }
                Toast.makeText(this, "已导出到 Documents/Chartly/$fn", Toast.LENGTH_LONG).show()
            } ?: Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show()
        } else {
            val dir = getExternalFilesDir(null)?.resolve("csv_exports")!!.apply { mkdirs() }
            val f = dir.resolve(fn)
            f.bufferedWriter().use { w ->
                repeat(inputLayout.childCount) { i ->
                    val row = inputLayout.getChildAt(i) as LinearLayout
                    val lab = (row.getChildAt(0) as EditText).text.toString()
                    val v   = (row.getChildAt(1) as EditText).text.toString()
                    w.append("$lab,$v\n")
                }
            }
            Toast.makeText(this, "已导出到 ${f.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMPORT_CSV && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            inputLayout.removeAllViews()
            var cnt = 0
            contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                lines.forEach { raw ->
                    val cols = raw.trim().split(",")
                    if (cols.size >= 2) {
                        addInputFields()
                        val row = inputLayout.getChildAt(inputLayout.childCount - 1) as LinearLayout
                        (row.getChildAt(0) as EditText).setText(cols[0].trim())
                        (row.getChildAt(1) as EditText).setText(cols[1].trim())
                        cnt++
                    }
                }
            }
            if (cnt > 0) {
                saveAllAndRefresh()
                Toast.makeText(this, "CSV 导入完成，共 $cnt 行", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "CSV 没有有效数据", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── 与侧边栏/上一个视图的数据传递 ───

    /** 导出当前所有行：“标签|数值” */
    override fun collectChartData(): ArrayList<String>? {
        val out = ArrayList<String>()
        repeat(inputLayout.childCount) { i ->
            val row = inputLayout.getChildAt(i) as LinearLayout
            val a   = (row.getChildAt(0) as EditText).text.toString()
            val b   = (row.getChildAt(1) as EditText).text.toString()
            out.add("$a|$b")
        }
        return out.ifEmpty { null }
    }

    /**
     * 从 Intent 或侧边栏收到上一视图的数据：
     * 列表格式 List<"标签|数值">，重建输入行并刷新图表
     */
    override fun processIncomingData(data: ArrayList<String>?) {
        data?.let {
            inputLayout.removeAllViews()
            it.forEach { line ->
                val parts = line.split("|", limit = 2)
                if (parts.size == 2) {
                    addInputFields()
                    val row = inputLayout.getChildAt(inputLayout.childCount - 1) as LinearLayout
                    (row.getChildAt(0) as EditText).setText(parts[0])
                    (row.getChildAt(1) as EditText).setText(parts[1])
                }
            }
            saveAllAndRefresh()
        }
    }

    override fun getChart(): Chart<*>? = radarChart
}
