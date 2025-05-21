package com.example.chartly

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.drawerlayout.widget.DrawerLayout

class CustomTitleBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    private val btnBack: ImageView
    private val btnMenu: ImageView
    private val btnUpload: ImageView
    private val btnDownload: ImageView
    private val tvTitle: TextView

    /** 外部可以赋值的点击回调 */
    var onBackClick: (() -> Unit)? = null
    var onMenuClick: (() -> Unit)? = null
    var onUploadClick: (() -> Unit)? = null
    var onDownloadClick: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.fancy_title_bar, this, true)
        btnBack = findViewById(R.id.btnBack)
        btnMenu = findViewById(R.id.btnMenu)
        btnUpload = findViewById(R.id.btnUpload)
        btnDownload = findViewById(R.id.btnDownload)
        tvTitle = findViewById(R.id.tvTitle)


        // 返回按钮：优先调用外部回调，否则自动 finish Activity
        btnBack.setOnClickListener {
            if (onBackClick != null) {
                onBackClick!!.invoke()
            } else {
                (context as? Activity)?.finish()
            }
        }

        // 菜单按钮：打开滑动菜单
        btnMenu.setOnClickListener {
            Log.d("CustomTitleBar", "Menu button clicked")
            if (context is DrawerActivity) {
                val drawerActivity = context as DrawerActivity
                if (!drawerActivity.drawerLayout.isDrawerOpen(drawerActivity.navigationView)) {
                    drawerActivity.drawerLayout.openDrawer(drawerActivity.navigationView)
                } else {
                    drawerActivity.drawerLayout.closeDrawer(drawerActivity.navigationView)
                }
            }
            onMenuClick?.invoke()
        }

        btnUpload.setOnClickListener { onUploadClick?.invoke() }

        btnDownload.setOnClickListener { onDownloadClick?.invoke() }
    }

    /** 动态设置标题文字 */
    fun setTitle(title: String) {
        tvTitle.text = title
    }
}
