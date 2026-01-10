<div align="center">

<img src="images/app_ico.png" alt="RIA-J Logo" width="100"/>

# RIA-J：ImageJ/Fiji 专属的比率荧光分析神器
**(Ratio Imaging Analyzer for Java)**

[![Release](https://img.shields.io/github/v/release/Epivitae/RIA-J?style=flat-square&color=blue&label=版本)](https://github.com/Epivitae/RIA-J/releases)
[![Platform](https://img.shields.io/badge/平台-ImageJ%20%2F%20Fiji-brightgreen?style=flat-square&logo=imagej)](https://imagej.net/)
[![GitHub](https://img.shields.io/badge/GitHub-Source%20Code-181717?style=flat-square&logo=github)](https://github.com/Epivitae/RIA-J)

</div>

---

## 💡 简介

**RIA-J** 是一站式分析工具 [RIA 莉丫(Python)](https://github.com/Epivitae/RatioImagingAnalyzer) 项目的原生 **ImageJ/Fiji** 版本。

它专为**比率荧光成像（Ratiometric Imaging）**设计，将**背景扣除**、**动态掩膜（Masking）**和**实时比率计算**无缝集成到 Fiji 中。告别繁琐的手动计算，**v1.0 版本** 引入全新的“Direct Stack”工作流，让你的分析过程“所见即所得”。

<div align="center">
  <img src="images/ria-j-main.png" width="80%" alt="RIA-J Interface">
</div>

> 🐙 **GitHub 原项目地址**：[https://github.com/Epivitae/RIA-J](https://github.com/Epivitae/RIA-J)

## ✨ 核心亮点

* 🚀 **全栈实时预览 (Direct Stack)**
  无需点击“应用”，导入即计算。调整阈值或参数时，当前帧实时更新，导出时自动同步至整个 Time-lapse 序列。
* 🧠 **智能通道识别**
  自动扫描所有窗口，智能拆分 Composite 图像，自动匹配分子/分母通道。
* ⚡ **一键“发文级”导出**
  * **RGB 视频流**：一键导出带伪彩、高对比度的 MP4/Stack，直接用于 PPT 展示。
  * **高清快照**：瞬间抓取当前帧，满足论文作图需求。
  * **纯净色条**：自动生成独立白底色条（Legend），方便导入 AI/Inkscape 组图。
* 🧹 **自动文件名清洗**
  自动去除 `C1-` 等冗余前缀，生成的 `RIA-Result`（32-bit Raw数据）和 `RIA-RGB`（展示数据）井井有条。

## 📥 极速安装 (推荐)

最简单的方法是通过 Fiji 自带的 Update Site 安装，通过此方式可自动获得后续更新：

1. 打开 **Fiji**，点击菜单栏 `Help > Update...`
2. 点击 **Manage update sites**。
3. 在列表中找到 **RIA-J** 并勾选。
   * *如果没找到，点击 **Add Unlisted Site**，填写：*
   * **Name:** `RIA-J`
   * **URL:** `https://sites.imagej.net/RIA-J/`
4. 点击 **Apply and Close**，重启 Fiji。
5. 插件位置：`Plugins > RIA-J (Ratio Processor)`

*(也可以在 [Releases](https://github.com/Epivitae/RIA-J/releases) 页面直接下载 jar 包放入 plugins 文件夹)*

## 🛠️ 三步使用指南

1. **导入**：将你的荧光数据拖入 Fiji，点击插件面板的 **`Import / Refresh`**，程序会自动完成计算。
2. **调节**：拖动滑块调整背景阈值（去除噪声）和 Ratio 显示范围（优化对比度），效果实时呈现。
3. **导出**：
   * **数据分析**：直接使用生成的 `RIA-Result` 窗口进行 ROI 圈选和测量（32-bit 原始数据）。
   * **展示出图**：点击红色 **`Save as RGB`** 按钮，选择保存当前帧（Snapshot）或整个视频流（Stack）。

---
<div align="center">
Developed by Kui Wang.
<br>
🌐 <b>团队网址</b>: <a href="http://www.cns.ac.cn">www.cns.ac.cn</a>
<br><br>
开源项目，欢迎 Star ⭐️ 和 Fork！
</div>