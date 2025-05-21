package com.example.chartly

import android.app.Application

class App : Application() {
    companion object {
        // 伴随对象中保存全局实例
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // 在 Application 创建时，赋值 instance
        instance = this
    }
}
