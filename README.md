<div align="center">

<img src="images/RIA-J.png" alt="RIA-J Logo" width="130"/>

# RIA-J: Ratio Imaging Analyzer (Java)

**The native ImageJ/Fiji counterpart to the [RIA (Python)](https://github.com/Epivitae/RatioImagingAnalyzer) project.**

[![Release](https://img.shields.io/github/v/release/Epivitae/RIA-J?style=flat-square&color=blue)](https://github.com/Epivitae/RIA-J/releases)
[![Downloads](https://img.shields.io/github/downloads/Epivitae/RIA-J/total?style=flat-square&color=success)](https://github.com/Epivitae/RIA-J/releases)
[![Platform](https://img.shields.io/badge/Platform-ImageJ%20%2F%20Fiji-brightgreen?style=flat-square&logo=imagej)](https://imagej.net/)
[![Java](https://img.shields.io/badge/Java-8%2B-orange?style=flat-square&logo=openjdk)](https://www.java.com/)
[![License](https://img.shields.io/github/license/Epivitae/RIA-J?style=flat-square)](LICENSE)
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.18204761.svg)](https://doi.org/10.5281/zenodo.18204761)
</div>

---

## 📖 Overview

**RIA-J** is a lightweight, publication-grade tool designed to bring the rigorous workflow of ratiometric fluorescence analysis directly into the **ImageJ/Fiji** ecosystem. 

It empowers researchers to perform **Background Subtraction**, **Dynamic Masking**, and **Real-time Ratio Calculation** through a modern, clutter-free dashboard. Featuring a **"Silent Processing" engine**, RIA-J handles multi-channel datasets entirely in memory, offering a fluid **"Single-Window"** experience without populating your workspace with unnecessary intermediate windows.

<div align="center">
  <img src="images/ria-j-main.png" width="60%" alt="RIA-J Interface">
</div>

*(The modern interface with streamlined inline controls)*

## ✨ Key Features

* **🤫 Silent & Clean Workflow**: Say goodbye to window clutter. RIA-J processes multi-channel composite images in memory. It intelligently splits channels and computes ratios without spawning "C1-" or "C2-" windows.
* **🎨 Compact Inline UI**: The interface features a space-saving "Inline Layout" (Label-Slider-Input), ensuring a clean look and consistent experience across all operating systems and high-DPI screens.
* **⚡ Instant Preview System**: 
    * **Real-time Tuning**: Adjusting sliders (Background, Threshold) triggers a millisecond-level update of the **current frame**, allowing for lag-free parameter optimization.
    * **Batch Recalculate**: A dedicated button applies your finalized settings to the entire Time-lapse or Z-stack in one go.
* **📸 Publication-Ready Export**:
    * **RGB Stack**: One-click export of your analysis as a high-quality RGB video/stack.
    * **Auto-Sync**: Ensures your current visualization settings (LUT, Contrast) are applied to every frame in the exported video.
    * **AI-Friendly Legend**: Generates a clean, independent color bar window (pure white background) for easy import into Adobe Illustrator.
* **🏷️ Smart Dynamic Naming**: Result windows are automatically named based on active channels (e.g., `RIA-C1_C2-Result...`). If you swap channels, the filename updates dynamically to ensure data traceability.

## 📥 Installation

### Method 1: Via Fiji Update Site (Recommended ⭐)
This is the easiest way to install RIA-J and keep it updated automatically.

1.  Open **Fiji / ImageJ**.
2.  Navigate to **Help > Update...**
3.  Click the **Manage update sites** button.
4.  Find **RIA-J** in the list and check the box.
    * *If RIA-J is not in the list, click **Add Unlisted Site** and enter:*
    * **Name**: `RIA-J`
    * **URL**: `https://sites.imagej.net/RIA-J/`
5.  Click **Apply and Close**, then click **Apply changes** in the main updater window.
6.  Restart Fiji. You will find it under `Plugins > RIA-J (Ratio Processor)`.

### Method 2: Manual Installation
If you prefer to install a specific version manually:

1.  **Download**: Get the latest **`RIA_J-x.x.x.jar`** file from the [Releases](https://github.com/Epivitae/RIA-J/releases) page.
2.  **Copy**: Move the `.jar` file into your `Fiji.app/plugins/` folder.
3.  **Restart**: Restart ImageJ.
4.  **Run**: Access the tool via `Plugins > RIA-J (Ratio Processor)`.

## 🛠️ Usage Workflow

1.  **Open Images**: Drag your raw data (Composite Stack or Split Files) into ImageJ.
2.  **Launch Plugin**: Go to `Plugins > RIA-J > Ratio Analyzer`.
3.  **One-Click Import**: 
    * Click **`Import / Refresh`**. 
    * *The plugin silently detects channels in the background. No extra windows will appear.*
4.  **Interactive Tuning**:
    * Adjust **Background** and **NaN Threshold**.
    * Tune **Min/Max Ratio** sliders to optimize the dynamic range.
    * *Changes are reflected immediately on the **current frame only** for maximum speed.*
5.  **Process Stack**:
    * **Single Image**: You are done!
    * **Time-lapse/Z-Stack**: Click the **`Recalculate`** button to apply your tuned parameters to the entire stack.
6.  **Export Results**:
    * **Data**: The `RIA-Result` window contains the 32-bit raw data for quantification.
    * **Publication**: Click **`Save as RGB`** (Red Button) to export the **Current Frame (Snapshot)** or the **Entire Stack (Movie)**.

## 📂 Output Files

RIA-J enforces a strict naming convention to keep your data organized:

| File Type | Naming Convention | Bit-Depth | Purpose |
| :--- | :--- | :--- | :--- |
| **Raw Result** | `RIA-[ChA_ChB]-Result...` | 32-bit Float | Measurement, Quantification, ROI Analysis |
| **RGB Stack** | `RIA-RGB-Stack-[Name]` | 24-bit RGB | Video presentation, PowerPoint, Visual inspection |
| **RGB Snap** | `RIA-RGB-Snap-[Name]` | 24-bit RGB | Figure creation (Adobe Illustrator/Inkscape) |

## ⚙️ Build from Source

**Prerequisites**: JDK 8+ and Maven.

```bash
git clone [https://github.com/Epivitae/RIA-J.git](https://github.com/Epivitae/RIA-J.git)
cd RIA-J
mvn clean package
```

The compiled plugin will be generated in `target/RIA_J-2.0.0.jar`.

## 🤝 Contributing

Contributions are welcome! Please fork the repository and submit a Pull Request.

## 🖊️ Citation

If you use RIA-J in your research, please cite:

> **Wang, K. (2026). RIA-J: Ratio Imaging Analyzer (Java) - Publication-Grade Ratiometric Analysis for ImageJ/Fiji (v2.0.0). Zenodo. https://doi.org/10.5281/zenodo.18204761**

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
<div align="center">
Developed by Kui Wang. Part of the RIA Project family.
</div>