# Chartly
基于Kotlin开发的可视化数据生成图表的Android App,通过用户输入对应数据可以生成多种图表.

## 目录

* [关于 Chartly](#关于-chartly)
    * [核心功能](#核心功能)
    * [技术栈](#技术栈)
* [快速开始](#快速开始)
    * [安装 Chartly](#安装-chartly)
* [使用方法](#使用方法)
* [图表类型](#图表类型)
* [许可证](#许可证)
* [联系方式](#联系方式)
* [致谢](#致谢)

---

## 关于 Chartly

在数据驱动的时代，理解和传达信息至关重要。**Chartly** 应运而生，致力于让数据可视化变得简单、快速且高效。无论你是数据分析师、学生，还是仅仅想更好地理解你的数据，Chartly 都能为你提供所需的工具。

### 核心功能

* **多格式数据导入：** 支持从多种常见数据格式导入数据，如 CSV、Excel 等。（**请根据实际支持的格式更新**）
* **丰富图表类型：** 将你的数据呈现为条形图、折线图、饼图、散点图等多种专业图表，满足不同分析需求。
* **实时预览：** 在输入数据和选择图表类型时，即时预览图表效果。
* **轻松导出：** 支持将生成的图表导出为高质量PNG图片文件，方便分享和报告。

### 技术栈

* **平台：** Android
* **主要语言：** Kotlin
* **图表库：** MPAndroidChart
* **数据处理：** Apache POI
* **其他：** 自定义view,DotsIndicator

---

## 快速开始

### 安装 Chartly

安装 Chartly 非常简单，**无需任何复杂的环境配置**！

1.  **下载 APK 文件：**
    * 点击这里下载最新版本的 Chartly APK：
        [**下载 Chartly 最新 APK**](https://github.com/11111-beep/Chartly/releases/download/v1.0.0/app-release.apk)
2.  **允许安装未知来源应用：**
    * 在下载并尝试安装 APK 时，Android 系统可能会提示你“**为了安全，你的手机目前不允许安装此来源的应用**”。你需要进入手机的“**设置**” -> “**安全**”或“**隐私**”（不同手机可能路径不同），找到“**安装未知应用**”或“**未知来源**”选项，并**允许**你的浏览器或文件管理器安装应用。
    * **温馨提示：** 完成安装后，为了安全考虑，你可以选择关闭此选项。
3.  **安装 APK：**
    * 找到你下载的 `app-release.apk` 文件，点击它进行安装。
4.  **启动 Chartly：**
    * 安装完成后，你就可以在你的应用列表中找到并启动 Chartly 了！

---

## 使用方法

使用 Chartly 直观便捷，只需几步即可将你的数据转化为精美图表：

1.  **选择图表类型：**
    * 在数据预览下方或侧边栏，选择你希望生成的图表类型，例如**条形图**、**折线图**、**饼图**或**散点图**等。
      <img src="https://raw.githubusercontent.com/11111-beep/Chartly/main/app/src/main/res/drawable/guide1.png" width="400">
      
2.  **导入数据：**
    * 启动应用后，短按/长按添加数据点,在编辑框上输入你的数据,点击主界面上的“**保存或刷新**”按钮，或者点击右上角左边的按钮选择你要处理的 CSV 或 Excel 文件。
      
      <img src="https://raw.githubusercontent.com/11111-beep/Chartly/main/app/src/main/res/drawable/guide2.png" width="400">
      
3.  **实时预览：**
    * 在配置过程中，你可以点击左上角右边的图表打开滑动目录切换图表,图表会实时更新预览，让你即时看到修改后的效果。
      <img src="https://raw.githubusercontent.com/11111-beep/Chartly/main/app/src/main/res/drawable/guide3.png" width="400">
      
4.  **导出图表：**
    * 当你对图表满意后，点击右上角“**导出**”图标。你可以选择将图表保存为高质量的PNG图片，方便分享或用于报告。

---

## 图表类型

Chartly 目前支持以下核心图表类型：

* **折线图 (Line Chart):** 
* **曲线图 (SmoothedLine Chart):**
* **柱状图 (Bar Chart):** 
* **水平柱状图 (Horizontal Bar Chart):**
* **饼状图 (Pie Chart):**
* **圆环图 (Doughnut Chart):**
* **雷达图 (Radar Chart):**
* **散点图 (Scatter Chart):**
* **气泡图 (Bubble Chart):**
* **K线图 (Candle Stick Chart):**
* **条形图 (Bar Line Chart):**
  
---

## 许可证

本项目采用 **Apache License2.0 许可证**。详见 `LICENSE` 文件。

---

## 联系方式

* 你的姓名 -苦~艾酒 [你的邮箱](1920634907@qq.com)
* 项目链接: [https://github.com/11111-beep/Chartly](https://github.com/11111-beep/Chartly)

---

## 致谢

* 感谢所有为本项目的开发提供帮助的开源项目和库。

