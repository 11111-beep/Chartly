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
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.model.GradientColor

class LineChartActivity : DrawerActivity() {

    companion object {
        private const val REQUEST_IMPORT_CSV = 7001
        private const val REQUEST_EXPORT_CSV = 7002
        const val EXTRA_CHART_DATA      = "com.example.chartly.EXTRA_CHART_DATA"
    }

    private lateinit var inputLayout: LinearLayout
    private lateinit var addPointButton: TextView
    private lateinit var saveChartButton: TextView
    private lateinit var lineChart: LineChart
    private lateinit var tvTitle: TextView
    private lateinit var markerView: CustomMarkerView

    private val entries = mutableListOf<Entry>()
    private val labels  = mutableListOf<String>()
    private lateinit var dataSet: LineDataSet

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_line_chart)

        inputLayout     = findViewById(R.id.inputLayout)
        addPointButton  = findViewById(R.id.addPointButton)
        saveChartButton = findViewById(R.id.saveChartButton)
        lineChart       = findViewById(R.id.lineChart)
        tvTitle         = findViewById(R.id.tvTitle)

        tvTitle.text = "折线图"
        markerView = CustomMarkerView(this, R.layout.marker_view)
        markerView.chartView = lineChart
        lineChart.marker = markerView

        setupChart()
        addInputFields()

        addPointButton.setOnClickListener { addInputFields() }
        addPointButton.setOnLongClickListener {
            showAddMultipleRowsDialog()
            true
        }
        saveChartButton.setOnClickListener { saveAllPointsAndRefreshChart() }

        // 下载 & CSV 按钮
        findViewById<CustomTitleBar>(R.id.customTitleBar).apply {
            onDownloadClick = {
                getChart()?.let { chart ->
                    saveChartToPng(chart, this@LineChartActivity,
                        "Chartly_${System.currentTimeMillis()}.png")
                } ?: Toast.makeText(
                    this@LineChartActivity,
                    "当前没有可导出的图表",
                    Toast.LENGTH_SHORT
                ).show()
            }
            onUploadClick = {
                AlertDialog.Builder(this@LineChartActivity)
                    .setTitle("CSV 操作")
                    .setItems(arrayOf("导入 CSV", "导出 CSV")) { _, which ->
                        if (which == 0) importCsv() else exportCsv()
                    }
                    .show()
            }
        }

        // 如果启动 Intent 带来了上一页面数据，就立刻恢复
        processIncomingData(intent.getStringArrayListExtra(EXTRA_CHART_DATA))
    }

    private fun setupChart() {
        val grad = GradientColor(
            Color.rgb(224, 234, 252),
            Color.rgb(207, 222, 243)
        )
        dataSet = LineDataSet(entries, "数据点").apply {
            setGradientColors(List(entries.size) { grad })
            val lilac = Color.parseColor("#D1C4E9")
            color = lilac; setCircleColor(lilac)
            lineWidth = 2f; mode = LineDataSet.Mode.LINEAR
            valueTextSize = 12f
        }
        lineChart.apply {
            data = LineData(dataSet)
            animateX(500)
            setScaleEnabled(true); isDragEnabled = true; setPinchZoom(true)
            axisRight.isEnabled = false
            description.isEnabled = false; legend.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f; axisLeft.granularity = 1f
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }

    private fun addInputFields() {
        val etLabel = EditText(this).apply {
            hint = "标签"; inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val etValue = EditText(this).apply {
            hint = "数值"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = 8
            }
            addView(etLabel); addView(etValue)
        }
        inputLayout.addView(row)
    }

    private fun showAddMultipleRowsDialog() {
        val input = EditText(this).apply {
            hint = "要添加的数据数量"; inputType = InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(this)
            .setTitle("批量添加数据")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                input.text.toString().toIntOrNull()?.takeIf { it>0 }?.let{ cnt->
                    repeat(cnt){ addInputFields() }
                } ?: Toast.makeText(this,"请输入有效数字",Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消",null)
            .show()
    }

    private fun saveAllPointsAndRefreshChart() {
        entries.clear(); labels.clear()
        for(i in 0 until inputLayout.childCount){
            val row = inputLayout.getChildAt(i) as LinearLayout
            val lab = (row.getChildAt(0) as EditText).text.toString()
                .takeIf{it.isNotBlank()} ?: "＃${i+1}"
            val v = (row.getChildAt(1) as EditText).text.toString().toFloatOrNull()
            if(v!=null){
                entries.add(Entry(i.toFloat(), v).apply{ data=lab })
                labels.add(lab)
            }
        }
        if(entries.isEmpty()){
            Toast.makeText(this,"请先输入有效的数据",Toast.LENGTH_SHORT).show()
            return
        }
        dataSet.values = entries
        lineChart.data = LineData(dataSet)
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        lineChart.notifyDataSetChanged()
        lineChart.animateY(300)
        lineChart.invalidate()
        Toast.makeText(this,"折线图已刷新",Toast.LENGTH_SHORT).show()
    }

    // ─── CSV 导入/导出 ───

    private fun importCsv(){
        Intent(Intent.ACTION_GET_CONTENT).apply{
            type="text/csv"; addCategory(Intent.CATEGORY_OPENABLE)
        }.also{ startActivityForResult(it, REQUEST_IMPORT_CSV) }
    }
    private fun exportCsv(){
        val fn="Chartly_${System.currentTimeMillis()}.csv"
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
            val r=contentResolver; val cv=ContentValues().apply{
                put(MediaStore.MediaColumns.DISPLAY_NAME,fn)
                put(MediaStore.MediaColumns.MIME_TYPE,"text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH,"Documents/Chartly")
            }
            r.insert(MediaStore.Files.getContentUri("external"),cv)?.let{ uri->
                r.openOutputStream(uri)?.bufferedWriter()?.use{ w->
                    repeat(inputLayout.childCount){ i->
                        val row=inputLayout.getChildAt(i) as LinearLayout
                        val lab=(row.getChildAt(0) as EditText).text.toString()
                        val v  =(row.getChildAt(1) as EditText).text.toString()
                        w.append("$lab,$v\n")
                    }
                }
                Toast.makeText(this,"已导出到 Documents/Chartly/$fn",Toast.LENGTH_LONG).show()
            }?:Toast.makeText(this,"导出失败",Toast.LENGTH_SHORT).show()
        } else {
            val dir=getExternalFilesDir(null)?.resolve("csv_exports")!!.apply{ mkdirs() }
            val f=dir.resolve(fn)
            f.bufferedWriter().use{ w->
                repeat(inputLayout.childCount){ i->
                    val row=inputLayout.getChildAt(i) as LinearLayout
                    val lab=(row.getChildAt(0) as EditText).text.toString()
                    val v  =(row.getChildAt(1) as EditText).text.toString()
                    w.append("$lab,$v\n")
                }
            }
            Toast.makeText(this,"已导出到 ${f.absolutePath}",Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==REQUEST_IMPORT_CSV && resultCode==Activity.RESULT_OK){
            data?.data?.let{ uri->
                inputLayout.removeAllViews()
                var cnt=0
                contentResolver.openInputStream(uri)?.bufferedReader()?.useLines{ lines->
                    lines.forEach{ raw->
                        val cols=raw.trim().split(",")
                        if(cols.size>=2){
                            addInputFields()
                            val idx=inputLayout.childCount-1
                            val row=inputLayout.getChildAt(idx) as LinearLayout
                            (row.getChildAt(0) as EditText).setText(cols[0].trim())
                            (row.getChildAt(1) as EditText).setText(cols[1].trim())
                            cnt++
                        }
                    }
                }
                if(cnt>0){
                    saveAllPointsAndRefreshChart()
                    Toast.makeText(this,"CSV 导入完成，共 $cnt 行",Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this,"CSV 没有有效数据",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ─── 数据传递 ───

    override fun collectChartData(): ArrayList<String>? {
        val out=ArrayList<String>()
        repeat(inputLayout.childCount){ i->
            val row=inputLayout.getChildAt(i) as LinearLayout
            val a=(row.getChildAt(0) as EditText).text.toString()
            val b=(row.getChildAt(1) as EditText).text.toString()
            out.add("$a|$b")
        }
        return out.ifEmpty{ null }
    }

    override fun processIncomingData(data: ArrayList<String>?) {
        data?.let{
            inputLayout.removeAllViews()
            it.forEach{ line->
                val parts=line.split("|",limit=2)
                if(parts.size==2){
                    addInputFields()
                    val idx=inputLayout.childCount-1
                    val row=inputLayout.getChildAt(idx) as LinearLayout
                    (row.getChildAt(0) as EditText).setText(parts[0])
                    (row.getChildAt(1) as EditText).setText(parts[1])
                }
            }
            saveAllPointsAndRefreshChart()
        }
    }

    override fun getChart(): Chart<*>? = lineChart
}
