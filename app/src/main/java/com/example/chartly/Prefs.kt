package com.example.chartly

import android.content.Context


// 定义一个名为Prefs的单例对象，用于管理应用的偏好设置
object Prefs {
    // 定义SharedPreferences的文件名，用于存储应用的偏好设置
    private const val NAME = "chartly_prefs"

    // 定义一个键，用于判断应用是否是第一次启动
    private const val KEY_FIRST = "is_first_launch"

    // 定义一个私有函数，返回一个SharedPreferences的实例
    // 使用全局的App.instance作为Context来获取SharedPreferences
    private fun prefs(): android.content.SharedPreferences =
        App.instance.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    // 定义一个属性isFirstLaunch，用于判断是否是第一次启动
    var isFirstLaunch: Boolean
        // 获取器，返回是否是第一次启动的状态，默认值为true
        get() = prefs().getBoolean(KEY_FIRST, true)
        // 设定器，设置是否是第一次启动的状态并保存
        set(v) = prefs().edit().putBoolean(KEY_FIRST, v).apply()
}