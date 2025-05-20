package com.example.chartly

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout

abstract class BaseChartActivity : AppCompatActivity() {
    protected lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base_chart_activity)
        
        drawerLayout = findViewById(R.id.drawerLayout)
        
        // 加载子Activity的布局
        val contentView = LayoutInflater.from(this).inflate(getContentLayoutId(), null)
        val container = findViewById<ViewGroup>(R.id.contentContainer)
        container.addView(contentView)
    }

    // 子类必须实现此方法来提供自己的布局ID
    abstract fun getContentLayoutId(): Int
} 