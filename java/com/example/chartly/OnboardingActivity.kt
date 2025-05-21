package com.example.chartly

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import kotlin.jvm.java


/**
 * OnboardingActivity 类，用于展示应用的引导页面。
 * 它使用 ViewPager2 滑动展示多个引导图页，并提供“立即体验”按钮跳转到主界面。
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: SpringDotsIndicator
    private lateinit var btnStart: Button

    // Activity 创建时的回调方法
    override fun onCreate(savedInstanceState: Bundle?) {
        // 调用父类的 onCreate 方法，传入保存的状态
        super.onCreate(savedInstanceState)
        // 隐藏 ActionBar，以获得全屏布局
        supportActionBar?.hide()
        // 设置 ContentView 为 activity_onboarding 布局
        setContentView(R.layout.activity_onboarding)

        // 1. 初始化控件，通过 findViewById 获取布局中的视图
        viewPager     = findViewById(R.id.vpOnboarding) // 获取 ViewPager2 实例
        dotsIndicator = findViewById(R.id.dotsIndicator) // 获取 SpringDotsIndicator 实例
        btnStart      = findViewById(R.id.btnStart) // 获取“立即体验”按钮实例

        // 2. 准备图像资源列表，并为 ViewPager 设置适配器
        val imgs = listOf(
            R.drawable.guide1, // 引导图1
            R.drawable.guide2, // 引导图2
            R.drawable.guide3  // 引导图3
        )
        // 为 ViewPager2 设置适配器，传入当前 Activity 和图像列表
        viewPager.adapter = OnboardingAdapter(this, imgs)

        // 3. 将 SpringDotsIndicator 与 ViewPager2 绑定，显示圆点指示器
        dotsIndicator.setViewPager2(viewPager)

        // 4. 监听 ViewPager2 的页面切换事件
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            // 当某个页面被选中时回调
            override fun onPageSelected(position: Int) {
                // 如果当前选中的是最后一个页面，显示“立即体验”按钮；否则隐藏
                btnStart.visibility = if (position == imgs.lastIndex) View.VISIBLE else View.GONE
            }
        })

        // 5. 设置“立即体验”按钮的点击监听器
        btnStart.setOnClickListener {
            // 将 isFirstLaunch 标记为 false，表示引导页面已显示过
            Prefs.isFirstLaunch = false
            // 启动 MainActivity，进入主界面
            startActivity(Intent(this, MainActivity::class.java))
            // 销毁当前 Activity，避免返回
            finish()
        }
    }
}