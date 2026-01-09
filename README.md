# RIA-J: Ratio Imaging Analyzer (Java Edition)

<div align="center">

**RIA-J** is a lightweight ImageJ/Fiji plugin designed for interactive ratiometric analysis. 
It serves as the native Java counterpart to the standalone [RIA (Python)](https://github.com/Epivitae/RatioImagingAnalyzer) software.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![Java](https://img.shields.io/badge/Java-8%2B-ed8b00?logo=java&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-ImageJ%20%2F%20Fiji-blue)

</div>

---

## 📖 Overview

**RIA-J** brings the rigorous ratiometric workflow of the "Ratio Imaging Analyzer" directly into the **ImageJ/Fiji** ecosystem. It is designed to replace complex, manual macro-writing with a user-friendly, interactive dashboard.

Unlike simple "image calculators," RIA-J focuses on **Data Fidelity**:
* **Smart Masking**: Uses dynamic intensity thresholding to set background pixels to `NaN` (Not a Number), preventing edge artifacts and noise amplification.
* **Normalized Convolution** (Optional): Preserves edges during smoothing, avoiding the "dark halo" effect common in standard Gaussian blurs.
* **Interactive Tuning**: A non-modal Swing dashboard lets you adjust parameters and see the result on the current frame instantly.

## ✨ Key Features

* **Modern Swing Dashboard**: A floating control panel that doesn't block the image view.
* **Real-time Preview**: Drag sliders for Background or Threshold and see the mask update instantly on the current frame.
* **Batch Processing**: One-click **"Apply to Stack"** to process high-dimensional time-lapse data (T-series).
* **Visualization Tools**: Integrated LUT selection (Fire, Ice, Physics, etc.) and Calibration Bar support.
* **Zero Dependencies**: Works with standard ImageJ 1.x or Fiji.

## 📥 Installation

1.  Download the latest **`RIA_J-x.x.x.jar`** file from the [Releases](https://github.com/YOUR_USERNAME/RIA-J/releases) page.
2.  **Drag and drop** the `.jar` file directly into the main toolbar of your Fiji/ImageJ.
3.  Restart ImageJ.
4.  The plugin will appear under: `Plugins > RIA-J > Ratio Processor`.

> **Note**: Alternatively, you can copy the jar file into your `Fiji.app/plugins/` folder.

## 📂 Sample Data

To test the plugin immediately, you can use the provided sample dataset:

1.  Navigate to the `sample_data/` folder in this repository.
2.  Download **`Composite.tif`**.
3.  Drag it into ImageJ and follow the workflow below.

## 🛠️ Usage Workflow

1.  **Open Image**: Drag your dual-channel time-lapse stack into ImageJ.
2.  **Split Channels**:
    * Go to `Image > Color > Split Channels`.
    * *Note: RIA-J requires two separate windows for the numerator and denominator.*
3.  **Launch Plugin**:
    * Go to `Plugins > RIA-J > Ratio Processor`.
    * Select the correct images for **Numerator (Ch1)** and **Denominator (Ch2)** in the popup dialog.
4.  **Tune Parameters (Interactive Mode)**:
    * **Background**: Adjust to subtract camera offset/scattered light.
    * **NaN Threshold**: Increase until the background noise disappears (becomes transparent/black).
    * **Visualization**: Adjust Min/Max sliders to change the contrast range (e.g., 0.5 to 5.0).
5.  **Finalize**:
    * Click **Apply to Stack** to process the entire video.
    * Click **Add Color Bar** to overlay a scale bar.

## ⚙️ Build from Source

If you are a developer and want to modify the code:

**Prerequisites**: JDK 8 (or higher) and Maven.

```bash
# 1. Clone the repository
git clone [https://github.com/YOUR_USERNAME/RIA-J.git](https://github.com/YOUR_USERNAME/RIA-J.git)
cd RIA-J

# 2. Build the package
mvn package
```

The compiled plugin will be generated in the `target/` directory (e.g., `target/RIA_J-0.3.0.jar`).

## 🤝 Contributing

Contributions are welcome! Please fork the repository and submit a Pull Request. 

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Developed by Kui Wang. Part of the RIA Project family.*