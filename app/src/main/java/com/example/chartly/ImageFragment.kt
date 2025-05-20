package com.example.chartly

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment


// 定义一个名为 ImageFragment 的 Fragment 类，用于显示图像
class ImageFragment : Fragment() {

    // 伴生对象，包含与 Fragment 相关的常量和工厂方法
    companion object {
        // 私有常量，作为键在 Bundle 中存储图像资源 ID
        private const val ARG_IMG = "arg_img"

        // 工厂方法，创建并返回带有指定资源 ID 的 ImageFragment 实例
        fun newInstance(resId: Int) = ImageFragment().apply {
            // 使用 apply 配置 Fragment 的 arguments
            arguments = bundleOf(ARG_IMG to resId)
        }
    }

    // 重写 onCreateView 方法，创建 Fragment 的视图
    override fun onCreateView(
        inflater: LayoutInflater, // 用于.inflate 布局文件
        container: ViewGroup?, // 视图的容器
        savedInstanceState: Bundle? // 保存的状态，如果有的话
    ): View = inflater.inflate(R.layout.fragment_image, container, false)
    // 使用 inflater.inflate 加载 fragment_image 布局，返回其根视图

    // 重写 onViewCreated 方法，初始化视图
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 从 arguments 中获取传递的图像资源 ID
        val imgRes = arguments?.getInt(ARG_IMG) ?: return
        // 查找 ImageView 并设置其图像资源
        view.findViewById<ImageView>(R.id.ivGuide).setImageResource(imgRes)
    }
}