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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.model.GradientColor

class SmoothedLineChartActivity : DrawerActivity() {

    companion object {
        private const val REQUEST_IMPORT_CSV = 12001
        private const val REQUEST_EXPORT_CSV = 12002
    }

    private lateinit var inputLayout: LinearLayout
    private lateinit var addPointButton: View      // 你的按钮类型
    private lateinit var saveChartButton: View
    private lateinit var lineChart: LineChart
    private lateinit var tvTitle: TextView
    private lateinit var markerView: CustomMarkerView
    private val entries = mutableListOf<Entry>()
    private lateinit var dataSet: LineDataSet

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smoothed_line_chart)

        inputLayout     = findViewById(R.id.inputLayout)
        addPointButton  = findViewById(R.id.addPointButton)
        saveChartButton = findViewById(R.id.saveChartButton)
        lineChart       = findViewById(R.id.lineChart)
        tvTitle          = findViewById(R.id.tvTitle)
        markerView       = CustomMarkerView(this, R.layout.marker_view)

        tvTitle.text = "曲线图"

        markerView.chartView = lineChart
        lineChart.marker   = markerView

        setupChart()
        addInputFields()

        addPointButton.setOnClickListener { addInputFields() }
        addPointButton.setOnLongClickListener {
            showBatchDialog(); true
        }
        saveChartButton.setOnClickListener { saveAllAndRefresh() }

        // --- 绑定自定义标题栏导入导出 ---
        findViewById<CustomTitleBar>(R.id.customTitleBar).apply {
            onDownloadClick = {
                getChart()?.let {
                    saveChartToPng(
                        it,
                        this@SmoothedLineChartActivity,
                        "Chartly_${System.currentTimeMillis()}.png"
                    )
                } ?: Toast.makeText(
                    this@SmoothedLineChartActivity,
                    "当前没有可导出的图表",
                    Toast.LENGTH_SHORT
                ).show()
            }
            onUploadClick = {
                AlertDialog.Builder(this@SmoothedLineChartActivity)
                    .setTitle("CSV 操作")
                    .setItems(arrayOf("导入 CSV", "导出 CSV")) { _, which ->
                        if (which == 0) importCsv() else exportCsv()
                    }
                    .show()
            }
        }
    }

    private fun setupChart() {
        val grad = GradientColor(
            Color.rgb(224, 234, 252),
            Color.rgb(207, 222, 243)
        )
        dataSet = LineDataSet(entries, "数据点").apply {
            setGradientColors(List(entries.size) { grad })
            color = Color.parseColor("#D1C4E9")
            setCircleColor(color)
            valueTextSize = 12f
            lineWidth = 2f
            setDrawCircles(true)
            setDrawValues(true)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        lineChart.apply {
            data = LineData(dataSet)
            animateX(500)
            setLayerType(View.LAYER_TYPE_SOFTWARE,null)
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            axisLeft.axisMinimum = 0f
        }
    }

    private fun addInputFields() {
        val etX = EditText(this).apply {
            hint = "标签"; inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val etY = EditText(this).apply {
            hint = "数值"; inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                .apply { topMargin = 8 }
            addView(etX); addView(etY)
        }
        inputLayout.addView(row)
    }

    private fun showBatchDialog() {
        val et = EditText(this).apply {
            hint = "要添加的数据行数"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(this)
            .setTitle("批量添加")
            .setView(et)
            .setPositiveButton("确定") { _,_ ->
                et.text.toString().toIntOrNull()?.let { c ->
                    if (c>0) repeat(c){ addInputFields() }
                    else Toast.makeText(this,"请输入有效数字",Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消",null)
            .show()
    }

    private fun saveAllAndRefresh() {
        entries.clear()
        val labels = mutableListOf<String>()
        repeat(inputLayout.childCount){ i ->
            val row = inputLayout.getChildAt(i) as LinearLayout
            val label = (row.getChildAt(0) as EditText).text
                .toString().takeIf{it.isNotBlank()}?:"#${i+1}"
            val y = (row.getChildAt(1) as EditText).text.toString().toFloatOrNull()
            if (y!=null) {
                entries.add(Entry(i.toFloat(),y).apply{ data = label })
                labels.add(label!!)
            }
        }
        if (entries.isEmpty()) {
            Toast.makeText(this,"请输入有效数据",Toast.LENGTH_SHORT).show()
            return
        }
        dataSet.values = entries
        lineChart.data = LineData(dataSet)
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        lineChart.notifyDataSetChanged()
        lineChart.animateY(300)
        lineChart.invalidate()
        Toast.makeText(this,"曲线图已刷新",Toast.LENGTH_SHORT).show()
    }

    /** 启动文件选取器 导入 CSV */
    private fun importCsv() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/csv"; addCategory(Intent.CATEGORY_OPENABLE)
        }.also { startActivityForResult(it, REQUEST_IMPORT_CSV) }
    }

    /** 导出当前输入到 CSV */
    private fun exportCsv() {
        val fileName = "Chartly_${System.currentTimeMillis()}.csv"
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q) {
            val r = contentResolver
            val cv = ContentValues().apply{
                put(MediaStore.MediaColumns.DISPLAY_NAME,fileName)
                put(MediaStore.MediaColumns.MIME_TYPE,"text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH,"Documents/Chartly")
            }
            r.insert(MediaStore.Files.getContentUri("external"),cv)?.let{ uri->
                r.openOutputStream(uri)?.bufferedWriter()?.use{ w ->
                    repeat(inputLayout.childCount){ i->
                        val row = inputLayout.getChildAt(i) as LinearLayout
                        val a = (row.getChildAt(0) as EditText).text.toString()
                        val b = (row.getChildAt(1) as EditText).text.toString()
                        w.append("$a,$b\n")
                    }
                }
                Toast.makeText(this,"已导出到 Documents/Chartly/$fileName",Toast.LENGTH_LONG).show()
            } ?: Toast.makeText(this,"导出失败",Toast.LENGTH_SHORT).show()
        } else {
            val dir = getExternalFilesDir(null)?.resolve("csv")?.apply{ mkdirs() }
            val f = dir!!.resolve(fileName)
            f.bufferedWriter().use{ w->
                repeat(inputLayout.childCount){ i->
                    val row = inputLayout.getChildAt(i) as LinearLayout
                    val a = (row.getChildAt(0) as EditText).text.toString()
                    val b = (row.getChildAt(1) as EditText).text.toString()
                    w.append("$a,$b\n")
                }
            }
            Toast.makeText(this,"已导出到 ${f.absolutePath}",Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if (requestCode==REQUEST_IMPORT_CSV && resultCode==Activity.RESULT_OK) {
            val uri = data?.data?:return
            inputLayout.removeAllViews()
            var count=0
            contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.useLines{ lines->
                    lines.forEach{ line->
                        val cols = line.split(",")
                        if (cols.size>=2) {
                            addInputFields()
                            val row = inputLayout.getChildAt(inputLayout.childCount-1) as LinearLayout
                            (row.getChildAt(0) as EditText).setText(cols[0].trim())
                            (row.getChildAt(1) as EditText).setText(cols[1].trim())
                            count++
                        }
                    }
                }
            if (count>0) {
                saveAllAndRefresh()
                Toast.makeText(this,"导入完成，共 $count 行",Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this,"没有有效数据",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun collectChartData(): ArrayList<String>? {
        val data = ArrayList<String>()
        repeat(inputLayout.childCount){ i->
            val row = inputLayout.getChildAt(i) as LinearLayout
            val a = (row.getChildAt(0) as EditText).text.toString()
            val b = (row.getChildAt(1) as EditText).text.toString()
            data.add("$a|$b")
        }
        return data.ifEmpty{null}
    }

    override fun processIncomingData(data:ArrayList<String>?) {
        data?.forEach{ item->
            val parts = item.split("|")
            if (parts.size==2){
                addInputFields()
                val row = inputLayout.getChildAt(inputLayout.childCount-1) as LinearLayout
                (row.getChildAt(0) as EditText).setText(parts[0])
                (row.getChildAt(1) as EditText).setText(parts[1])
            }
        }
        saveAllAndRefresh()
    }

    override fun getChart(): Chart<*>? = lineChart
}
