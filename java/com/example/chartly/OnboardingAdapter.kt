package com.example.chartly

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter


/**
 * 适配器类，用于在 Fragment 中显示引导（Onboarding）过程中的图像。
 * 它管理一个图像资源列表，并为每个图像创建一个 Fragment。
 */
class OnboardingAdapter(
    private val fa: FragmentActivity, // FragmentActivity，用来管理 Fragment 的状态
    private val images: List<Int> // 包含图像资源 ID 的列表
) : FragmentStateAdapter(fa) {

    /**
     * 获取适配器中 Fragment 的数量。
     * @return 图像资源列表的大小
     */
    override fun getItemCount() = images.size

    /**
     * 创建一个新的 Fragment，用于在指定位置显示图像。
     * @param position 当前位置，用于从图像列表中获取对应的资源 ID
     * @return 显示指定图像的 Fragment 实例
     */
    override fun createFragment(position: Int): Fragment {
        // 使用 ImageFragment 的工厂方法创建新的 Fragment 实例
        // 并传递当前位置的图像资源 ID
        return ImageFragment.newInstance(images[position])
    }
}