package com.example.chartly

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class ScatterChartActivity : DrawerActivity() {

    companion object {
        private const val REQUEST_IMPORT_CSV = 11001
        private const val REQUEST_EXPORT_CSV = 11002
    }

    private lateinit var inputLayout: LinearLayout
    private lateinit var addScatterButton: Button
    private lateinit var saveChartButton: Button
    private lateinit var scatterChart: ScatterChart
    private lateinit var tvTitle: TextView
    private lateinit var markerView: CustomMarkerView

    private val entries = mutableListOf<Entry>()
    private lateinit var dataSet: ScatterDataSet

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scatter_chart)

        inputLayout      = findViewById(R.id.inputLayout)
        addScatterButton = findViewById(R.id.addScatterButton)
        saveChartButton  = findViewById(R.id.saveChartButton)
        scatterChart     = findViewById(R.id.scatterChart)
        tvTitle          = findViewById(R.id.tvTitle)
        markerView       = CustomMarkerView(this, R.layout.marker_view)

        tvTitle.text = "散点图"

        markerView.chartView = scatterChart
        scatterChart.marker   = markerView

        setupChart()
        addInputFields()

        addScatterButton.setOnClickListener { addInputFields() }
        addScatterButton.setOnLongClickListener {
            showAddMultipleRowsDialog()
            true
        }
        saveChartButton.setOnClickListener { saveAllPointsAndRefreshChart() }

        findViewById<CustomTitleBar>(R.id.customTitleBar).apply {
            onDownloadClick = {
                getChart()?.let {
                    saveChartToPng(it, this@ScatterChartActivity,
                        "Chartly_${System.currentTimeMillis()}.png")
                } ?: Toast.makeText(this@ScatterChartActivity,
                    "当前没有可导出的图表", Toast.LENGTH_SHORT).show()
            }
            onUploadClick = {
                AlertDialog.Builder(this@ScatterChartActivity)
                    .setTitle("CSV 操作")
                    .setItems(arrayOf("导入 CSV","导出 CSV")) { _, which ->
                        if (which == 0) importCsv() else exportCsv()
                    }.show()
            }
        }
    }

    private fun setupChart() {
        dataSet = ScatterDataSet(entries, "散点数据").apply {
            setScatterShape(ScatterChart.ScatterShape.CIRCLE)
            scatterShapeSize = 12f
            color = getColor(R.color.macaron_lavender)
            valueTextSize = 10f
        }
        scatterChart.apply {
            data = ScatterData(dataSet)
            description.isEnabled = false
            legend.isEnabled = true
            setScaleEnabled(true)
            isDragEnabled = true
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            axisLeft.granularity = 1f
            setExtraOffsets(20f,10f,20f,10f)
            animateY(400)
        }
    }

    private fun addInputFields() {
        val etLabel = EditText(this).apply {
            hint = "标签"; inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val etX = EditText(this).apply {
            hint = "X 值"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val etY = EditText(this).apply {
            hint = "Y 值"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                .apply { topMargin = 8 }
            addView(etLabel); addView(etX); addView(etY)
        }
        inputLayout.addView(row)
    }

    private fun showAddMultipleRowsDialog() {
        val input = EditText(this).apply {
            hint = "要添加的行数"; inputType = InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(this)
            .setTitle("批量添加数据行")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                input.text.toString().toIntOrNull()?.let { c ->
                    if (c>0) repeat(c){ addInputFields() }
                    else Toast.makeText(this,"请输入有效数字",Toast.LENGTH_SHORT).show()
                } ?: Toast.makeText(this,"请输入有效数字",Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消",null)
            .show()
    }

    private fun saveAllPointsAndRefreshChart() {
        entries.clear()
        val labels = mutableListOf<String>()
        repeat(inputLayout.childCount) { i ->
            val row = inputLayout.getChildAt(i) as LinearLayout
            val label = (row.getChildAt(0) as EditText).text.toString().takeIf{it.isNotBlank()}?:"#${i+1}"
            val xVal  = (row.getChildAt(1) as EditText).text.toString().toFloatOrNull()
            val yVal  = (row.getChildAt(2) as EditText).text.toString().toFloatOrNull()
            if (xVal!=null && yVal!=null) {
                entries.add(Entry(xVal, yVal).apply{ data = label })
                labels.add(label!!)
            }
        }
        if (entries.isEmpty()) {
            Toast.makeText(this,"请先输入有效的散点数据",Toast.LENGTH_SHORT).show()
            return
        }
        dataSet.values = entries
        scatterChart.data = ScatterData(dataSet)
        scatterChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        // X 轴缓冲
        val xs = entries.map{it.x}
        val minX = xs.minOrNull() ?:0f
        val maxX = xs.maxOrNull() ?:0f
        val buf = (maxX-minX)*0.1f
        scatterChart.xAxis.axisMinimum = minX-buf
        scatterChart.xAxis.axisMaximum = maxX+buf

        scatterChart.notifyDataSetChanged()
        scatterChart.animateY(300)
        scatterChart.invalidate()
        Toast.makeText(this,"散点图已刷新",Toast.LENGTH_SHORT).show()
    }

    /** 导入 CSV */
    private fun importCsv() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/csv"; addCategory(Intent.CATEGORY_OPENABLE)
        }.also { startActivityForResult(it, REQUEST_IMPORT_CSV) }
    }

    /** 导出 CSV */
    private fun exportCsv() {
        val fileName = "Chartly_${System.currentTimeMillis()}.csv"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Chartly")
            }
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
            if (uri!=null) {
                resolver.openOutputStream(uri)?.bufferedWriter()?.use { w ->
                    repeat(inputLayout.childCount){ i->
                        val row = inputLayout.getChildAt(i) as LinearLayout
                        val a = (row.getChildAt(0) as EditText).text.toString()
                        val b = (row.getChildAt(1) as EditText).text.toString()
                        val c = (row.getChildAt(2) as EditText).text.toString()
                        w.append("$a,$b,$c\n")
                    }
                }
                Toast.makeText(this,"已导出到 Documents/Chartly/$fileName",Toast.LENGTH_LONG).show()
            } else Toast.makeText(this,"导出失败",Toast.LENGTH_SHORT).show()
        } else {
            val dir = getExternalFilesDir(null)?.resolve("csv_exports")?.apply{ mkdirs() }
            val f = dir!!.resolve(fileName)
            f.bufferedWriter().use { w ->
                repeat(inputLayout.childCount){ i->
                    val row = inputLayout.getChildAt(i) as LinearLayout
                    val a = (row.getChildAt(0) as EditText).text.toString()
                    val b = (row.getChildAt(1) as EditText).text.toString()
                    val c = (row.getChildAt(2) as EditText).text.toString()
                    w.append("$a,$b,$c\n")
                }
            }
            Toast.makeText(this,"已导出到 ${f.absolutePath}",Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if (requestCode==REQUEST_IMPORT_CSV && resultCode==Activity.RESULT_OK) {
            val uri = data?.data ?: return
            inputLayout.removeAllViews()
            var count=0
            contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.useLines { lines ->
                    lines.forEach { raw ->
                        val cols = raw.trim().split(",")
                        if (cols.size>=3) {
                            addInputFields()
                            val row = inputLayout.getChildAt(inputLayout.childCount-1) as LinearLayout
                            (row.getChildAt(0) as EditText).setText(cols[0].trim())
                            (row.getChildAt(1) as EditText).setText(cols[1].trim())
                            (row.getChildAt(2) as EditText).setText(cols[2].trim())
                            count++
                        }
                    }
                }
            if (count>0) {
                saveAllPointsAndRefreshChart()
                Toast.makeText(this,"CSV 导入完成，共 $count 行",Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this,"导入的 CSV 中没有有效数据",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun collectChartData(): ArrayList<String>? {
        val data = ArrayList<String>()
        repeat(inputLayout.childCount) { i->
            val row = inputLayout.getChildAt(i) as LinearLayout
            val a = (row.getChildAt(0) as EditText).text.toString()
            val b = (row.getChildAt(1) as EditText).text.toString()
            val c = (row.getChildAt(2) as EditText).text.toString()
            data.add("$a|$b|$c")
        }
        return data.ifEmpty { null }
    }

    override fun processIncomingData(data: ArrayList<String>?) {
        if (data.isNullOrEmpty()) return
        inputLayout.removeAllViews()
        data.forEach { item ->
            val parts = item.split("|")
            if (parts.size==3) {
                addInputFields()
                val row = inputLayout.getChildAt(inputLayout.childCount-1) as LinearLayout
                (row.getChildAt(0) as EditText).setText(parts[0])
                (row.getChildAt(1) as EditText).setText(parts[1])
                (row.getChildAt(2) as EditText).setText(parts[2])
            }
        }
        saveAllPointsAndRefreshChart()
    }

    override fun getChart(): Chart<*>? = scatterChart
}
