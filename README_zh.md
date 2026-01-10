<div align="center">

<img src="images/RIA-J.png" alt="RIA-J Logo" width="130"/>

# RIA-J：ImageJ/Fiji 专属的比率荧光分析神器
**(Ratio Imaging Analyzer for Java)**

[![Release](https://img.shields.io/github/v/release/Epivitae/RIA-J?style=flat-square&color=blue&label=版本)](https://github.com/Epivitae/RIA-J/releases)
[![Downloads](https://img.shields.io/github/downloads/Epivitae/RIA-J/total?style=flat-square&color=success)](https://github.com/Epivitae/RIA-J/releases)
[![Platform](https://img.shields.io/badge/平台-ImageJ%20%2F%20Fiji-brightgreen?style=flat-square&logo=imagej)](https://imagej.net/)
[![Java](https://img.shields.io/badge/Java-8%2B-orange?style=flat-square&logo=openjdk)](https://www.java.com/)
[![License](https://img.shields.io/github/license/Epivitae/RIA-J?style=flat-square)](LICENSE)
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.18204761.svg)](https://doi.org/10.5281/zenodo.18204761)

</div>

---

## 💡 简介

**RIA-J** 是一站式分析工具 [RIA 莉丫(Python)](https://github.com/Epivitae/RatioImagingAnalyzer) 项目的原生 **ImageJ/Fiji** 版本。

它专为**比率荧光成像（Ratiometric Imaging）**设计，提供了一套**轻量化、现代化、零干扰**的分析工作流。通过独创的**“静默处理引擎”**，RIA-J 能够完全在内存中处理多通道数据，将背景扣除、动态掩膜（Masking）和实时比率计算集成在一个清爽的面板中，彻底告别 ImageJ 窗口满天飞的混乱局面。

<div align="center">
  <img src="images/ria-j-main.png" width="60%" alt="RIA-J Interface">
</div>

> 🐙 **GitHub 原项目地址**：[https://github.com/Epivitae/RIA-J](https://github.com/Epivitae/RIA-J)

## ✨ 核心亮点

* 🤫 **静默处理 & 单窗口交互**
  告别桌面 clutter。插件在内存中智能拆分多通道图像并进行计算，**不会**生成多余的 `C1-`、`C2-` 中间窗口。所有操作（切换通道、参数调整）均在同一个结果窗口内**原位更新**。
* 🎨 **全新紧凑型 UI**
  采用极简的 Inline 布局（标签-滑块-输入框同生），大幅节省屏幕垂直空间，在各类操作系统和高分屏下均能保持一致的精致外观。
* ⚡ **毫秒级即时预览**
  * **实时调参**：拖动背景或阈值滑块时，**当前帧**会以毫秒级速度实时响应，实现丝滑的参数微调体验。
  * **一键重算**：参数确定后，点击 **`Recalculate`** 按钮即可将设置应用至整个 Time-lapse 或 Z-Stack。
* 📸 **一键“发文级”导出**
  * **RGB 视频流**：一键导出带伪彩、高对比度的 MP4/Stack，直接用于 PPT 展示。
  * **智能同步**：自动将当前帧的 LUT 和对比度设置同步应用到导出视频的每一帧。
  * **纯净色条**：自动生成独立白底色条（Legend），方便导入 AI/Inkscape 组图。
* 🏷️ **动态智能命名**
  结果窗口根据当前参与计算的通道自动命名（如 `RIA-C1_C2-Result...`）。若中途交换分子/分母通道，文件名会自动更新，确保数据来源清晰可溯。

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

## 🛠️ 使用流程指南

1. **打开图像**：将你的荧光数据（Composite Stack 或 分离的通道文件）拖入 Fiji。
2. **启动插件**：点击 `Plugins > RIA-J > Ratio Analyzer`。
3. **一键导入**：
   * 点击 **`Import / Refresh`**。
   * *程序会在后台静默检测通道，不会弹出多余窗口。*
4. **实时调参 (Instant Preview)**：
   * 调整 **Background** 和 **NaN Threshold** 去除背景噪声。
   * 调节 **Min/Max Ratio** 优化显示对比度。
   * *此时仅**当前帧**会实时更新，保证操作流畅。*
5. **应用至序列**：
   * 如果是多帧图像（Time-lapse/Z-stack），点击 **`Recalculate`** 按钮，将当前优化的参数应用到所有帧。
6. **导出结果**：
   * **数据分析**：`RIA-Result` 窗口始终包含 32-bit 原始数据，可直接进行 ROI 圈选测量。
   * **展示出图**：点击红色 **`Save as RGB`** 按钮，选择导出当前帧（Snapshot）或完整视频（Stack）。

## 📂 输出文件说明

RIA-J 采用严格的命名规范以保持数据整洁：

| 文件类型 | 命名规则 | 位深 | 用途 |
| :--- | :--- | :--- | :--- |
| **Raw Result** | `RIA-[ChA_ChB]-Result...` | 32-bit Float | 定量测量、ROI 分析 |
| **RGB Stack** | `RIA-RGB-Stack-[Name]` | 24-bit RGB | 视频展示、PPT、肉眼观察 |
| **RGB Snap** | `RIA-RGB-Snap-[Name]` | 24-bit RGB | 论文作图 (Adobe Illustrator) |

## ⚙️ 源码编译

**前置要求**: JDK 8+ 和 Maven。

```bash
git clone [https://github.com/Epivitae/RIA-J.git](https://github.com/Epivitae/RIA-J.git)
cd RIA-J
mvn clean package
```

编译后的插件位于 `target/RIA_J-x.x.x.jar`。

## 🤝 参与贡献

本项目开源，欢迎提交 Pull Request 或 Issues。

## 🖊️ 引用 (Citation)

如果您在研究中使用了 RIA-J，请引用：

> **Wang, K. (2026). RIA-J: Ratio Imaging Analyzer (Java) - Publication-Grade Ratiometric Analysis for ImageJ/Fiji (v2.0.0). Zenodo. https://doi.org/10.5281/zenodo.18204761**

## 📄 许可证

本项目基于 MIT License 开源 - 详见 [LICENSE](LICENSE) 文件。

---
<div align="center">
Developed by Kui Wang.
<br>
🌐 <b>团队网址</b>: <a href="http://www.cns.ac.cn">www.cns.ac.cn</a>
<br><br>
开源项目，欢迎 Star ⭐️ 和 Fork！
</div>