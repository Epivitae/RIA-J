<div align="center">

<img src="images/app_ico.png" alt="RIA-J Logo" width="120"/>

# RIA-J: Ratio Imaging Analyzer (Java)

**The native ImageJ/Fiji counterpart to the [RIA (Python)](https://github.com/Epivitae/RatioImagingAnalyzer) project.**

[![Release](https://img.shields.io/github/v/release/Epivitae/RIA-J?style=flat-square&color=blue)](https://github.com/Epivitae/RIA-J/releases)
[![Downloads](https://img.shields.io/github/downloads/Epivitae/RIA-J/total?style=flat-square&color=success)](https://github.com/Epivitae/RIA-J/releases)
[![Platform](https://img.shields.io/badge/Platform-ImageJ%20%2F%20Fiji-brightgreen?style=flat-square&logo=imagej)](https://imagej.net/)
[![Java](https://img.shields.io/badge/Java-8%2B-orange?style=flat-square&logo=openjdk)](https://www.java.com/)
[![License](https://img.shields.io/github/license/Epivitae/RIA-J?style=flat-square)](LICENSE)
[![DOI](https://zenodo.org/badge/1131155665.svg)](https://doi.org/10.5281/zenodo.18200077)
</div>

---

## 📖 Overview

**RIA-J** is a lightweight, interactive plugin designed to bring the rigorous workflow of ratiometric fluorescence analysis directly into the **ImageJ/Fiji** ecosystem. 

It empowers researchers to perform **Background Subtraction**, **Dynamic Masking**, and **Real-time Ratio Calculation** through a modern, user-friendly dashboard, eliminating the need for complex macro scripting.

<div align="center">
  <img src="images/ria-j-main.png" width="80%">
</div>


*(The interactive dashboard allows real-time tuning of parameters)*

## ✨ Key Features

* **🎛️ Modern Swing Dashboard**: A non-modal, floating control panel that allows you to adjust parameters while interacting with the image.
* **⚡ Real-time Preview**: Instantly visualize how background subtraction and thresholding affect the ratio calculation on the current frame.
* **🧠 Smart Masking**: Dynamic `NaN` thresholding ensures that background noise is effectively removed without creating edge artifacts.
* **🎞️ Batch Processing**: One-click **"Apply to Stack"** functionality to process high-dimensional time-lapse data (T-series) efficiently.
* **🎨 Visualization**: Built-in support for scientific LUTs (Fire, Ice, Physics) and Calibration Bars.

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

## 📂 Sample Data

To test the plugin immediately, you can use the provided sample dataset:

1.  Navigate to the `sample_data/` folder in this repository.
2.  Download **`dual_channel_demo.tif`**.
3.  Drag it into ImageJ and follow the workflow below.

## 🛠️ Usage Workflow

1.  **Open Image**: Load your dual-channel time-lapse stack into ImageJ.
2.  **Split Channels**:
    * Go to `Image > Color > Split Channels`.
    * *(RIA-J requires two separate windows for the numerator and denominator)*.
3.  **Launch Plugin**:
    * Go to `Plugins > RIA-J > Ratio Processor`.
    * Select the **Numerator (Ch1)** and **Denominator (Ch2)**.
4.  **Interactive Tuning**:
    * **Background**: Adjust to subtract camera offset/scattered light.
    * **NaN Threshold**: Increase until the background becomes transparent (NaN).
    * **Visualization**: Adjust Min/Max sliders to optimize contrast.
5.  **Finalize**:
    * Click **Apply to Stack** to process the entire video.
    * Click **Add Color Bar** to overlay a scale bar.

## ⚙️ Build from Source

**Prerequisites**: JDK 8+ and Maven.

```bash
git clone [https://github.com/Epivitae/RIA-J.git](https://github.com/Epivitae/RIA-J.git)
cd RIA-J
mvnd package
```

The compiled plugin will be generated in `target/RIA_J-0.3.0.jar`.

## 🤝 Contributing

Contributions are welcome! Please fork the repository and submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
<div align="center">
Developed by Kui Wang. Part of the RIA Project family.
</div>