package com.example.chartly

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
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
import androidx.core.view.children
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class CandleStickChartActivity : DrawerActivity() {

    companion object {
        private const val REQUEST_IMPORT_CSV = 4001
        private const val REQUEST_EXPORT_CSV = 4002
    }

    private lateinit var inputLayout: LinearLayout
    private lateinit var addRowButton: Button
    private lateinit var refreshChartButton: Button
    private lateinit var candleChart: CandleStickChart
    private lateinit var tvTitle: TextView
    private lateinit var markerView: CustomMarkerView

    private val entries = mutableListOf<CandleEntry>()
    private lateinit var dataSet: CandleDataSet

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_candle_stick_chart)

        // 系统栏内边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            WindowInsetsCompat.CONSUMED
        }

        inputLayout       = findViewById(R.id.inputLayout)
        addRowButton      = findViewById(R.id.addCandleStickButton)
        refreshChartButton= findViewById(R.id.saveChartButton)
        candleChart       = findViewById(R.id.candleStickChart)
        tvTitle           = findViewById(R.id.tvTitle)
        tvTitle.text      = "K线图"

        markerView = CustomMarkerView(this, R.layout.marker_view)
        markerView.chartView = candleChart
        candleChart.marker   = markerView

        setupChart()   // 只做样式配置，不绑定空数据

        // 初始一行输入
        addInputRow()
        addRowButton.setOnClickListener { addInputRow() }
        addRowButton.setOnLongClickListener {
            showBatchAddDialog()
            true
        }
        // 点击“保存并刷新”，才会组装 data 并调用 setData()
        refreshChartButton.setOnClickListener { saveAllAndRefresh() }

        // 标题栏按钮绑定
        findViewById<CustomTitleBar>(R.id.customTitleBar).apply {
            onDownloadClick = {
                getChart()?.let { chart ->
                    saveChartToPng(
                        chart,
                        this@CandleStickChartActivity,
                        "Chartly_${System.currentTimeMillis()}.png"
                    )
                } ?: Toast.makeText(
                    this@CandleStickChartActivity,
                    "当前没有可导出的图表",
                    Toast.LENGTH_SHORT
                ).show()
            }
            onUploadClick = {
                AlertDialog.Builder(this@CandleStickChartActivity)
                    .setTitle("CSV 操作")
                    .setItems(arrayOf("导入 CSV", "导出 CSV")) { _, which ->
                        if (which == 0) importCsv() else exportCsv()
                    }
                    .show()
            }
        }
    }

    /** 只做样式配置，不绑定任何数据 */
    private fun setupChart() {
        dataSet = CandleDataSet(entries, "蜡烛数据").apply {
            color = Color.parseColor("#4F4F4F")
            shadowColor = Color.parseColor("#808080")
            shadowWidth = 1.5f
            increasingColor = Color.parseColor("#4CAF50")
            increasingPaintStyle = Paint.Style.FILL
            decreasingColor = Color.parseColor("#F44336")
            decreasingPaintStyle = Paint.Style.FILL
            neutralColor = Color.parseColor("#2196F3")
            setDrawValues(false)
            highLightColor = Color.parseColor("#AAAAAA")
        }

        candleChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setScaleEnabled(true)
            isDragEnabled = true
            setPinchZoom(true)
            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            axisLeft.apply {
                granularity = 1f
                setDrawGridLines(true)
            }
            setExtraOffsets(20f, 10f, 20f, 10f)
            animateY(500)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }

    private fun addInputRow() {
        fun mk(h: String, t: Int) = EditText(this).apply {
            hint = h; inputType = t
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginEnd = 8 }
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = 8 }
            addView(mk("标签", InputType.TYPE_CLASS_TEXT))
            addView(mk("开盘", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL))
            addView(mk("最高", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL))
            addView(mk("最低", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL))
            addView(mk("收盘", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL))
        }
        inputLayout.addView(row)
    }

    private fun showBatchAddDialog() {
        val et = EditText(this).apply {
            hint = "要添加的行数"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(this)
            .setTitle("批量添加行")
            .setView(et)
            .setPositiveButton("确定") { _, _ ->
                et.text.toString().toIntOrNull()?.let { c ->
                    if (c > 0) repeat(c) { addInputRow() }
                    else Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show()
                } ?: Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 读取所有输入，组装 CandleData 并刷新图表 */
    private fun saveAllAndRefresh() {
        entries.clear()
        val labels = mutableListOf<String>()

        for (i in 0 until inputLayout.childCount) {
            val row = inputLayout.getChildAt(i) as LinearLayout
            val lab = (row.getChildAt(0) as EditText)
                .text.toString().takeIf { it.isNotBlank() } ?: "#${i+1}"
            val open = (row.getChildAt(1) as EditText).text.toString().toFloatOrNull()
            val high = (row.getChildAt(2) as EditText).text.toString().toFloatOrNull()
            val low  = (row.getChildAt(3) as EditText).text.toString().toFloatOrNull()
            val close= (row.getChildAt(4) as EditText).text.toString().toFloatOrNull()

            if (open != null && high != null && low != null && close != null) {
                entries.add(CandleEntry(i.toFloat(), high, low, open, close).apply { data = lab })
                labels.add(lab)
            }
        }

        if (entries.isEmpty()) {
            Toast.makeText(this, "请先输入有效的蜡烛数据", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. 重新给 dataSet 赋值
        dataSet.values = entries

        // 2. 绑定到 CandleData 并刷新
        candleChart.data = CandleData(dataSet)

        // 3. 设置 X 轴标签和范围
        candleChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        candleChart.xAxis.axisMinimum = -0.5f
        candleChart.xAxis.axisMaximum = entries.size - 1 + 0.5f

        candleChart.notifyDataSetChanged()
        candleChart.invalidate()

        Toast.makeText(this, "蜡烛图已刷新", Toast.LENGTH_SHORT).show()
    }

    /** 导入 CSV */
    private fun importCsv() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/csv"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_IMPORT_CSV)
    }

    /** 导出 CSV */
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
                    inputLayout.children.forEach { v ->
                        val row = v as LinearLayout
                        val texts = (0 until 5).joinToString(",") { idx ->
                            (row.getChildAt(idx) as EditText).text.toString()
                        }
                        w.append(texts).append("\n")
                    }
                }
                Toast.makeText(this, "已导出到 Documents/Chartly/$fn", Toast.LENGTH_LONG).show()
            } ?: Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show()
        } else {
            val dir = getExternalFilesDir(null)?.resolve("csv_exports")!!.apply { mkdirs() }
            val file = dir.resolve(fn)
            file.bufferedWriter().use { w ->
                inputLayout.children.forEach { v ->
                    val row = v as LinearLayout
                    val texts = (0 until 5).joinToString(",") { idx ->
                        (row.getChildAt(idx) as EditText).text.toString()
                    }
                    w.append(texts).append("\n")
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
                    if (cols.size >= 5) {
                        addInputRow()
                        val idx = inputLayout.childCount - 1
                        val row = inputLayout.getChildAt(idx) as LinearLayout
                        for (j in 0 until 5) {
                            (row.getChildAt(j) as EditText).setText(cols[j].trim())
                        }
                        valid++
                    }
                }
            }
            if (valid == 0) {
                Toast.makeText(this, "导入的 CSV 中没有有效数据", Toast.LENGTH_SHORT).show()
            } else {
                saveAllAndRefresh()
                Toast.makeText(this, "CSV 导入完成，共 $valid 行", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getChart(): Chart<*>? = candleChart
}
