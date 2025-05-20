package com.example.chartly

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class CustomMarkerView(
    context: Context,
    layoutResource: Int
) : MarkerView(context, layoutResource) {

    // 声明显示内容的文本视图
    private val tvContent: TextView = findViewById(R.id.tvContent)

    /**
     * 更新标记内容
     * @param e 当前选中的数据条目，包含x/y坐标值
     * @param highlight 高亮信息（本实现未使用）
     */
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            // 格式化显示坐标值
            tvContent.text = "标签: ${it.data}, 数值: ${it.y}"
        }
        super.refreshContent(e, highlight) // 必须调用父类方法
    }

    /**
     * 计算标记视图的偏移量
     * @return 偏移量坐标点，使标记居中显示在数据点上方
     */
    override fun getOffset(): MPPointF {
        // X轴偏移：标记宽度的一半（水平居中）
        // Y轴偏移：标记高度的负值（完全显示在数据点上方）
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}
