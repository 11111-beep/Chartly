package com.example.chartly

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView


// 自定义的Spinner适配器，用于显示图表类型的选择项
class ChartSpinnerAdapter(
    context: Context,
    private val items: List<String> // 数据源，包含Spinner的选项文本
) : ArrayAdapter<String>(context, R.layout.spinner_item, items) {

    // 用于将布局文件转换为视图的LayoutInflater实例
    private val inflater = LayoutInflater.from(context)

    // 记录当前选中的位置
    private var selectedPosition = 0

    // 重写getView方法，返回Spinner的主视图项
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // 调用createItemView方法创建视图，参数false表示非下拉视图
        return createItemView(position, convertView, parent, false)
    }

    // 重写getDropDownView方法，返回下拉列表的视图项
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        // 调用createItemView方法创建视图，参数true表示下拉视图
        return createItemView(position, convertView, parent, true)
    }

    // 私有方法，创建单个视图项
    private fun createItemView(position: Int, convertView: View?, parent: ViewGroup, isDropdown: Boolean): View {
        // 根据isDropdown参数选择不同的布局文件
        val layout = if (isDropdown) R.layout.spinner_dropdown_item else R.layout.spinner_item
        // 复用convertView，如果null则创建新的视图
        val view = convertView ?: inflater.inflate(layout, parent, false)

        // 从视图中找到图标、文本和单选按钮
        val icon = view.findViewById<ImageView>(R.id.ivChartIcon)
        val text = view.findViewById<TextView>(R.id.tvChartName)
        val radio = view.findViewById<RadioButton?>(R.id.rbSelected)

        // 设置当前项的文本
        text.text = items[position]
        // 设置当前项的图标
        icon.setImageResource(getIconForPosition(position))
        // 设置单选按钮的选中状态
        radio?.isChecked = position == selectedPosition

        // 返回创建或复用的视图
        return view
    }

    // 设置当前选中的位置，并通知适配器刷新视图
    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        // 通知适配器数据发生变化，刷新视图
        notifyDataSetChanged()
    }

    // 根据位置返回对应的图标资源ID
    private fun getIconForPosition(position: Int): Int {
        return when (position) {
            0 -> R.drawable.ic_chart_line // 折线图图标
            1 -> R.drawable.ic_chart_curve // 曲线图图标
            2 -> R.drawable.ic_chart_bar // 条形图图标
            3 -> R.drawable.ic_chart_bar_horizontal // 水平条形图图标
            4 -> R.drawable.ic_chart_pie // 饼图图标
            5 -> R.drawable.ic_chart_doughnut // 甜甜圈图图标
            6 -> R.drawable.ic_chart_radar // 雷达图图标
            7 -> R.drawable.ic_chart_scatter // 散点图图标
            8 -> R.drawable.ic_chart_bubble // 气泡图图标
            9 -> R.drawable.ic_chart_candlestick // 蜡烛图图标
            10 -> R.drawable.ic_chart_combined // 组合图图标
            else -> R.drawable.ic_chart_line // 默认图标，折线图
        }
    }
}