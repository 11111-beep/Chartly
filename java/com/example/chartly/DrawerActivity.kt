package com.example.chartly

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.github.mikephil.charting.charts.Chart
import com.google.android.material.navigation.NavigationView

open class DrawerActivity : AppCompatActivity() {
    private lateinit var _drawerLayout: DrawerLayout
    private lateinit var _navigationView: NavigationView
    private lateinit var contentContainer: ViewGroup

    // 子类可以通过这两个属性来操作 DrawerLayout / NavigationView
    val drawerLayout: DrawerLayout get() = _drawerLayout
    val navigationView: NavigationView get() = _navigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 先加载抽屉容器
        super.setContentView(R.layout.drawer_container)

        _drawerLayout     = findViewById(R.id.drawerLayout)
        _navigationView   = findViewById(R.id.navigationView)
        contentContainer  = findViewById(R.id.contentContainer)

        setupNavigationView()
        // **移除了这里对 customTitleBar 的绑定**
    }

    // 让子类的布局都 inflate 到 contentContainer 里
    override fun setContentView(layoutResID: Int) {
        View.inflate(this, layoutResID, contentContainer)
    }
    override fun setContentView(view: View) {
        contentContainer.addView(view)
    }
    override fun setContentView(view: View, params: ViewGroup.LayoutParams) {
        contentContainer.addView(view, params)
    }

    private fun setupNavigationView() {
        _navigationView.setNavigationItemSelectedListener { menuItem ->
            // … 你原来的导航跳转逻辑 …
            val chartData = collectChartData()
            val targetClass = when (menuItem.itemId) {
                R.id.nav_line_chart            -> LineChartActivity::class.java
                R.id.nav_smoothed_line_chart   -> SmoothedLineChartActivity::class.java
                R.id.nav_bar_chart             -> BarChartActivity::class.java
                R.id.nav_horizontal_bar_chart  -> HorizontalBarChartActivity::class.java
                R.id.nav_pie_chart             -> PieChartActivity::class.java
                R.id.nav_doughnut_chart        -> DoughnutChartActivity::class.java
                R.id.nav_radar_chart           -> RadarChartActivity::class.java
                R.id.nav_scatter_chart         -> ScatterChartActivity::class.java
                R.id.nav_bubble_chart          -> BubbleChartActivity::class.java
                R.id.nav_candle_stick_chart    -> CandleStickChartActivity::class.java
                R.id.nav_bar_line_chart        -> BarLineChartActivity::class.java
                else -> null
            }
            if (targetClass != null && !isCurrentActivity(targetClass)) {
                val intent = Intent(this, targetClass).apply {
                    chartData?.let { putExtra("CHART_DATA", it) }
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
                finish()
            }
            _drawerLayout.closeDrawers()
            true
        }
    }

    private fun isCurrentActivity(targetClass: Class<*>): Boolean =
        this.javaClass == targetClass

    /** 子类重写，返回当前页面要导出的 Chart 对象 */
    protected open fun getChart(): Chart<*>? = null

    /** 子类可重写：收集当前图表页的数据，用于侧边栏跳转时传递 */
    open fun collectChartData(): ArrayList<String>? = null

    /** 子类可重写：接收其他页面传来的数据 */
    open fun processIncomingData(data: ArrayList<String>?) {}

    override fun onStart() {
        super.onStart()

        // 1. 先处理 Intent 传入的数据
        intent.getStringArrayListExtra("CHART_DATA")?.let {
            processIncomingData(it)
        }

        // 2. 再绑定 CustomTitleBar 的按钮 —— 此时子类的布局已完成挂载
        findViewById<CustomTitleBar?>(R.id.customTitleBar)?.apply {
            // 导出按钮
            onDownloadClick = {
                getChart()?.let { chart ->
                    saveChartToPng(chart,
                        this@DrawerActivity,
                        "Chartly_${System.currentTimeMillis()}.png")
                } ?: Toast.makeText(
                    this@DrawerActivity,
                    "当前没有可导出的图表",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // 如果你有上传按钮，也可以在这里绑定：
            // onUploadClick = { … }
        }
    }

    /** 将 MPAndroidChart 图表保存为 PNG 到系统相册 */
    protected fun saveChartToPng(
        chart: Chart<*>,
        context: Context,
        fileName: String
    ) {
        // 1. 渲染 Bitmap
        val bitmap: Bitmap = chart.chartBitmap
        // 2. 准备 MediaStore 写入信息
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Chartly")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        // 3. 插入并写入文件
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            Toast.makeText(context, "创建文件失败", Toast.LENGTH_SHORT).show()
            return
        }
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        // 4. 更新状态
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        Toast.makeText(context, "已保存：$fileName", Toast.LENGTH_LONG).show()
    }
}
