package com.mybiolab;

import ij.IJ;
import ij.ImageJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GUI; 
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.ColorModel;
import java.net.URL;

/**
 * PROJECT: RIA-J (Ratio Imaging Analyzer - Java Edition)
 * VERSION: v0.8.0 (Robust Workflow & Logic Fixes)
 * AUTHOR: Kui Wang
 */
public class RIA_J extends PlugInFrame implements PlugIn, ActionListener, ItemListener, ImageListener {

    // --- GUI Constants ---
    private static final Color COLOR_THEME = new Color(0, 102, 204); 
    private static final Font FONT_NORMAL = new Font("Arial", Font.PLAIN, 12); 
    private static final Font FONT_BOLD   = new Font("Arial", Font.BOLD, 12);
    private static final Font FONT_TITLE  = new Font("Arial", Font.BOLD, 12); 
    private static final Font FONT_HEADER = new Font("Arial", Font.BOLD, 16); 
    private static final Font FONT_SMALL  = new Font("Arial", Font.PLAIN, 10); 
    private static final int COMPONENT_WIDTH = 180; 
    private static final int SLIDER_HEIGHT   = 18;  

    // --- Components ---
    private JButton btnRefresh; 
    private JComboBox<String> comboNum, comboDen;
    private JSlider sliderBg, sliderThresh, sliderMin, sliderMax;
    private JSpinner spinBg, spinThresh, spinMin, spinMax; 
    private JComboBox<String> comboLUT;
    private JButton btnBarShow, btnBarClose; 
    private JButton btnApply; 
    
    // --- Data Objects ---
    private ImagePlus[] availableImages;
    private ImagePlus imp1;           // Numerator (Source A)
    private ImagePlus imp2;           // Denominator (Source B)
    private ImagePlus resultImp;      // The Result (MUST be 32-bit Float)
    private ImagePlus impLegend;      // The Legend (MUST be RGB)
    
    // --- State Flags ---
    private boolean isUpdatingUI = false;      // UI正在自我更新，防止回环
    private boolean isBatchProcessing = false; // [重要] 正在批处理，禁止监听器干扰

    // --- Parameters ---
    private int valBg = 20;
    private int valThresh = 50;
    private double valMin = 0.0;
    private double valMax = 5.0;

    public RIA_J() {
        super("RIA-J (Ratio Processor)"); 
        ImagePlus.addImageListener(this); // Register Listener
    }

    @Override
    public void run(String arg) {
        buildGUI();
        if (WindowManager.getImageCount() > 0) {
            refreshImageList();
        }
    }

    // ========================================================================
    // LOGIC BLOCK 1: ImageListener (The Watchdog)
    // ========================================================================
    
    @Override
    public void imageOpened(ImagePlus imp) {}

    @Override
    public void imageClosed(ImagePlus imp) {
        if (imp == resultImp) resultImp = null;
        if (imp == impLegend) impLegend = null;
    }

    @Override
    public void imageUpdated(ImagePlus imp) {
        // [核心修复] 如果正在批处理，或者正在更新UI，监听器闭嘴
        if (isBatchProcessing || isUpdatingUI) return;

        // 只关心结果图的变化
        if (imp == resultImp) {
            double curDisplayMin = imp.getDisplayRangeMin();
            double curDisplayMax = imp.getDisplayRangeMax();

            // 检测 ImageJ 内部的 Contrast 变化 (如 Ctrl+Shift+C)
            if (Math.abs(curDisplayMin - valMin) > 0.001 || Math.abs(curDisplayMax - valMax) > 0.001) {
                isUpdatingUI = true; // 锁住UI

                // 1. 同步数据
                valMin = curDisplayMin;
                valMax = curDisplayMax;

                // 2. 同步 UI 控件
                spinMin.setValue(valMin);
                spinMax.setValue(valMax);
                syncSlidersFromValues();

                // 3. 同步 Legend (纯视觉)
                if (isValidLegendOpen()) updateLegend();

                isUpdatingUI = false; // 解锁
            }
        }
    }

    // ========================================================================
    // LOGIC BLOCK 2: Core Processing (Math vs Display)
    // ========================================================================

    /**
     * 预览更新：计算单帧 + 更新显示
     */
    private void updatePreview(boolean reCalculateMath) {
        if (resultImp == null || imp1 == null || imp2 == null) return;
        
        // 1. Math Step (只有参数变了才重算，如果是只改了LUT或MinMax，其实不需要这一步，但为了简单这里保留)
        if (reCalculateMath) {
            int currentZ = resultImp.getCurrentSlice();
            if (currentZ > imp1.getStackSize()) currentZ = 1;
            
            ImageProcessor ip1 = imp1.getStack().getProcessor(currentZ).convertToFloat();
            ImageProcessor ip2 = imp2.getStack().getProcessor(currentZ).convertToFloat();
            
            FloatProcessor fpResult = calculateRatioMath(ip1, ip2, valBg, valThresh);
            resultImp.setProcessor(fpResult);
        }

        // 2. Display Step (Pure Visuals)
        resultImp.setDisplayRange(valMin, valMax); // 核心：设置32位图的显示范围
        
        String lut = (String) comboLUT.getSelectedItem();
        IJ.run(resultImp, lut != null ? lut : "Fire", ""); 
        
        // 3. Legend Step
        if (isValidLegendOpen()) updateLegend();
    }

    /**
     * 纯数学计算：输入两个Processor，返回一个32-bit FloatProcessor
     */
    private FloatProcessor calculateRatioMath(ImageProcessor ip1, ImageProcessor ip2, double bg, double thresh) {
        int width = ip1.getWidth(); 
        int height = ip1.getHeight();
        float[] p1 = (float[]) ip1.getPixels(); 
        float[] p2 = (float[]) ip2.getPixels();
        float[] pRes = new float[p1.length];
        
        for (int i = 0; i < p1.length; i++) {
            float v1 = p1[i] - (float)bg; 
            float v2 = p2[i] - (float)bg;
            
            if (v1 < 0) v1 = 0; 
            if (v2 < 0) v2 = 0; // Negative handling
            
            if (v2 < thresh) {
                pRes[i] = Float.NaN; // Mask background
            } else {
                float r = v1 / v2;
                if (Float.isInfinite(r) || Float.isNaN(r)) pRes[i] = Float.NaN; 
                else pRes[i] = r;
            }
        }
        return new FloatProcessor(width, height, pRes);
    }

    /**
     * 批处理逻辑：处理整个栈
     */
    private void processEntireStack() {
        // [逻辑锁] 标记开始
        isBatchProcessing = true;
        
        try {
            if (imp1 == null || imp2 == null) { IJ.showMessage("Error", "No images selected!"); return; }
            if (imp1.getStackSize() != imp2.getStackSize()) { IJ.showMessage("Error", "Stack size mismatch!"); return; }

            IJ.showStatus("Processing entire stack...");
            int width = imp1.getWidth(); 
            int height = imp1.getHeight(); 
            int nSlices = imp1.getStackSize();
            
            ImageStack finalStack = new ImageStack(width, height);
            
            for (int z = 1; z <= nSlices; z++) {
                IJ.showProgress(z, nSlices);
                
                ImageProcessor ip1 = imp1.getStack().getProcessor(z).convertToFloat();
                ImageProcessor ip2 = imp2.getStack().getProcessor(z).convertToFloat();
                
                // Pure Math
                FloatProcessor fp = calculateRatioMath(ip1, ip2, valBg, valThresh);
                // 这里不要 setMinMax，等到最后统一设置，或者每张都设
                fp.setMinAndMax(valMin, valMax); 
                
                String label = imp1.getStack().getSliceLabel(z);
                if(label == null) label = "Ratio " + z;
                finalStack.addSlice(label, fp);
            }
            
            // 更新结果图 (32-bit)
            resultImp.setStack(finalStack);
            resultImp.setTitle("RIA-J Result (Final)");
            
            // 应用显示设置
            resultImp.setDisplayRange(valMin, valMax);
            String lut = (String) comboLUT.getSelectedItem();
            IJ.run(resultImp, lut != null ? lut : "Fire", ""); 
            resultImp.setSlice(1);
            
            // 更新图例
            if (isValidLegendOpen()) updateLegend();
            
            IJ.showStatus("Finished!");
            
        } catch (Exception e) {
            IJ.handleException(e);
        } finally {
            // [逻辑锁] 无论成功失败，必须释放锁，否则 Listener 永远失效
            isBatchProcessing = false; 
        }
    }

    // ========================================================================
    // LOGIC BLOCK 3: UI Event Handling
    // ========================================================================

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        
        if (src == btnRefresh) {
            refreshImageList();
        } 
        else if (src == comboNum || src == comboDen) {
            if (!isUpdatingUI) { 
                updateChannelReferences(); 
                createInitialResult(); // 切图了，重置结果窗口
                updatePreview(true); 
            }
        } 
        else if (src == btnApply) {
            // 启动新线程，但确保按钮状态管理
            new Thread(() -> {
                SwingUtilities.invokeLater(() -> {
                    btnApply.setEnabled(false); 
                    btnApply.setText("Processing...");
                });
                
                processEntireStack(); // 执行耗时任务
                
                SwingUtilities.invokeLater(() -> {
                    btnApply.setEnabled(true); 
                    btnApply.setText("<html><b>Apply to Stack</b></html>");
                });
            }).start();
        } 
        else if (src == btnBarShow) {
            updateLegend();
        } 
        else if (src == btnBarClose) {
            if (isValidLegendOpen()) { impLegend.close(); impLegend = null; }
        }
    }

    // Slider/Spinner 变动逻辑
    private void linkSliderAndSpinner(JSlider slider, JSpinner spinner, double scaleFactor) {
        ChangeListener cl = e -> {
            if (isUpdatingUI) return; // 正在自我更新，忽略
            isUpdatingUI = true;      // 上锁

            if (e.getSource() == slider) {
                int val = slider.getValue();
                if (scaleFactor == 1.0) spinner.setValue(val); 
                else spinner.setValue(val * scaleFactor);
            } else {
                if (scaleFactor == 1.0) slider.setValue((Integer) spinner.getValue());
                else slider.setValue((int)((Double) spinner.getValue() / scaleFactor));
            }
            
            // 更新参数变量
            updateParamsFromUI();
            
            // 判断是 Math 变了 还是 Visual 变了
            boolean isMathChange = (slider == sliderBg || slider == sliderThresh);
            updatePreview(isMathChange);
            
            isUpdatingUI = false;     // 解锁
        };
        slider.addChangeListener(cl); 
        spinner.addChangeListener(cl);
    }
    
    // LUT 变动逻辑
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getSource() == comboLUT) {
            if (resultImp != null) {
                isUpdatingUI = true;
                String lutName = (String) comboLUT.getSelectedItem();
                IJ.run(resultImp, lutName, "");
                if (isValidLegendOpen()) updateLegend();
                isUpdatingUI = false;
            }
        }
    }

    // ========================================================================
    // LOGIC BLOCK 4: Initialization & Helpers
    // ========================================================================

    private void refreshImageList() {
        ImagePlus activeImp = IJ.getImage(); 
        if (activeImp == null && WindowManager.getImageCount() == 0) {
            IJ.error("RIA-J", "No images found!"); return;
        }

        if (activeImp != null && (activeImp.isComposite() || activeImp.getNChannels() > 1)) {
            IJ.showStatus("Auto-splitting...");
            availableImages = ChannelSplitter.split(activeImp);
        } else {
            int[] ids = WindowManager.getIDList();
            if (ids == null) return;
            java.util.List<ImagePlus> list = new java.util.ArrayList<>();
            for (int id : ids) {
                ImagePlus imp = WindowManager.getImage(id);
                // 排除自己生成的窗口
                if (imp != resultImp && imp != impLegend && !imp.getTitle().contains("RIA-J")) {
                    list.add(imp);
                }
            }
            availableImages = list.toArray(new ImagePlus[0]);
        }
        
        isUpdatingUI = true;
        comboNum.removeAllItems(); comboDen.removeAllItems();
        if (availableImages != null && availableImages.length > 0) {
            for (ImagePlus imp : availableImages) {
                String name = imp.getTitle();
                if (name.length() > 20) name = name.substring(0, 17) + "...";
                comboNum.addItem(name); comboDen.addItem(name);
            }
            comboNum.setSelectedIndex(0);
            if (availableImages.length > 1) comboDen.setSelectedIndex(1);
            else comboDen.setSelectedIndex(0);
        }
        isUpdatingUI = false;
        
        updateChannelReferences();
        createInitialResult();
        updatePreview(true);
        IJ.showStatus("Ready.");
    }

    private void createInitialResult() {
        if (imp1 == null) return;
        // 如果已经有结果窗口，先把它关了（因为可能尺寸都变了）
        if (resultImp != null) { 
            resultImp.changes = false; 
            resultImp.close(); 
        }
        int width = imp1.getWidth(); 
        int height = imp1.getHeight();
        FloatProcessor fp = new FloatProcessor(width, height);
        resultImp = new ImagePlus("RIA-J Result", fp);
        resultImp.show();
    }
    
    private void updateChannelReferences() {
        if (availableImages == null || availableImages.length == 0) return;
        int idx1 = comboNum.getSelectedIndex();
        int idx2 = comboDen.getSelectedIndex();
        if (idx1 >= 0) imp1 = availableImages[idx1];
        if (idx2 >= 0) imp2 = availableImages[idx2];
    }
    
    private void updateParamsFromUI() {
        valBg = (Integer) spinBg.getValue();
        valThresh = (Integer) spinThresh.getValue();
        valMin = (Double) spinMin.getValue();
        valMax = (Double) spinMax.getValue();
    }
    
    private void syncSlidersFromValues() {
        // 当从 ImageJ 外部获取到 min/max 后，反向设置 slider
        int sMin = (int)(valMin * 100);
        int sMax = (int)(valMax * 100);
        if (sMin >= 0 && sMin <= 1000) sliderMin.setValue(sMin);
        if (sMax >= 0 && sMax <= 1000) sliderMax.setValue(sMax);
    }

    // ========================================================================
    // LOGIC BLOCK 5: Visualization (Legend)
    // ========================================================================

    private void updateLegend() {
        if (resultImp == null) return;
        int barW = 20; int barH = 200;
        int padTop = 15; int padBot = 15; int padLeft = 10; int padRight = 50;
        int totalW = padLeft + barW + padRight; int totalH = padTop + barH + padBot;

        // 1. Draw Gradient (0-255)
        byte[] pixels = new byte[barW * barH];
        for (int y = 0; y < barH; y++) {
            for (int x = 0; x < barW; x++) pixels[y * barW + x] = (byte) (255 - (y * 255 / barH)); 
        }
        ByteProcessor ipBar = new ByteProcessor(barW, barH, pixels);
        
        // 2. Map LUT from Result Image
        ColorModel cm = resultImp.getProcessor().getColorModel();
        if(cm != null) ipBar.setColorModel(cm);
        ImageProcessor ipLegendCol = ipBar.convertToRGB(); // 转RGB

        // 3. Compose Legend Image
        ColorProcessor ipFinal = new ColorProcessor(totalW, totalH);
        ipFinal.setColor(new Color(245, 245, 245)); 
        ipFinal.fill();
        ipFinal.insert(ipLegendCol, padLeft, padTop);
        ipFinal.setColor(Color.BLACK);
        ipFinal.drawRect(padLeft-1, padTop-1, barW+1, barH+1);

        // 4. Draw Text Labels based on valMin / valMax
        ipFinal.setFont(new Font("SansSerif", Font.PLAIN, 11));
        ipFinal.setAntialiasedText(true);
        int steps = 5;
        for (int i = 0; i <= steps; i++) {
            int yPos = padTop + barH - (int)((double)i / steps * barH);
            ipFinal.drawLine(padLeft + barW, yPos, padLeft + barW + 5, yPos);
            double val = valMin + (valMax - valMin) * i / steps;
            ipFinal.drawString(String.format("%.2f", val), padLeft + barW + 8, yPos + 4);
        }

        // 5. Show/Update
        if (isValidLegendOpen()) {
            impLegend.setProcessor(ipFinal);
            impLegend.updateAndDraw();
        } else {
            impLegend = new ImagePlus("Legend", ipFinal);
            impLegend.show();
            if (resultImp.getWindow() != null) {
                Point loc = resultImp.getWindow().getLocation();
                impLegend.getWindow().setLocation(loc.x + resultImp.getWidth() + 5, loc.y);
            }
        }
    }

    private boolean isValidLegendOpen() {
        return impLegend != null && impLegend.isVisible() && impLegend.getWindow() != null;
    }

    @Override
    public void close() {
        super.close();
        ImagePlus.removeImageListener(this); // Clean up
        if (isValidLegendOpen()) impLegend.close();
        if (resultImp != null) resultImp.close();
    }
    
    // ========================================================================
    // UI Helpers (Boilerplate)
    // ========================================================================

    private void buildGUI() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5)); 

        mainPanel.add(createHeaderPanel());
        mainPanel.add(Box.createVerticalStrut(5)); 
        mainPanel.add(createInputPanel());
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(createCalcPanel());
        mainPanel.add(Box.createVerticalStrut(5)); 
        mainPanel.add(createVisPanel());
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(createActionPanel());
        mainPanel.add(Box.createVerticalStrut(2));

        add(mainPanel);
        pack();
        GUI.center(this);
        setVisible(true);
    }
    // ... UI Creation methods (Input, Calc, Vis, Action, Header) similar to previous version ...
    // 为了节省篇幅，UI构建代码在下方补全，逻辑是一样的
    
    private JPanel createHeaderPanel() {
        JPanel pMain = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        URL imgURL = getClass().getResource("/images/RIA-J-128.png");
        if (imgURL != null) {
            Image img = new ImageIcon(imgURL).getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH); 
            pMain.add(new JLabel(new ImageIcon(img)));
        }
        JPanel pText = new JPanel(); pText.setLayout(new BoxLayout(pText, BoxLayout.Y_AXIS));
        JLabel lblTitle = new JLabel("RIA-J Controller"); lblTitle.setFont(FONT_HEADER); lblTitle.setForeground(COLOR_THEME);
        pText.add(lblTitle); pText.add(Box.createVerticalStrut(2)); 
        JLabel lblCopy = new JLabel("© 2026 www.cns.ac.cn"); lblCopy.setFont(FONT_SMALL); lblCopy.setForeground(Color.GRAY);
        pText.add(lblCopy); pMain.add(pText);
        return pMain;
    }
    private JPanel createInputPanel() {
        JPanel p = createTitledPanel("Input Data");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(2, 2, 2, 2); gbc.weightx = 1.0;
        btnRefresh = new JButton("Import / Refresh");
        btnRefresh.setFont(FONT_BOLD); btnRefresh.setForeground(new Color(0, 100, 0));
        btnRefresh.addActionListener(this);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; p.add(btnRefresh, gbc);
        gbc.gridwidth = 1;
        JLabel lblNum = new JLabel("Numerator:"); lblNum.setFont(FONT_NORMAL);
        gbc.gridx = 0; gbc.gridy = 1; p.add(lblNum, gbc);
        comboNum = new JComboBox<>(); comboNum.setFont(FONT_NORMAL); comboNum.addActionListener(this); 
        gbc.gridx = 1; gbc.gridy = 1; p.add(comboNum, gbc);
        JLabel lblDen = new JLabel("Denominator:"); lblDen.setFont(FONT_NORMAL);
        gbc.gridx = 0; gbc.gridy = 2; p.add(lblDen, gbc);
        comboDen = new JComboBox<>(); comboDen.setFont(FONT_NORMAL); comboDen.addActionListener(this); 
        gbc.gridx = 1; gbc.gridy = 2; p.add(comboDen, gbc);
        return p;
    }
    private JPanel createCalcPanel() {
        JPanel p = createTitledPanel("Calculation Parameters");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(2, 2, 2, 2); gbc.weightx = 1.0;
        JPanel pBgRow = createLabelSpinnerPanel("Background:", 0, 1000, valBg, false);
        spinBg = (JSpinner) pBgRow.getComponent(1);
        gbc.gridx = 0; gbc.gridy = 0; p.add(pBgRow, gbc);
        sliderBg = new JSlider(0, 1000, valBg);
        setupSlider(sliderBg); linkSliderAndSpinner(sliderBg, spinBg, 1.0);
        gbc.gridx = 0; gbc.gridy = 1; p.add(sliderBg, gbc);
        JPanel pThRow = createLabelSpinnerPanel("NaN Threshold:", 0, 5000, valThresh, false);
        spinThresh = (JSpinner) pThRow.getComponent(1);
        gbc.gridx = 0; gbc.gridy = 2; p.add(pThRow, gbc);
        sliderThresh = new JSlider(0, 1000, valThresh);
        setupSlider(sliderThresh); linkSliderAndSpinner(sliderThresh, spinThresh, 1.0);
        gbc.gridx = 0; gbc.gridy = 3; p.add(sliderThresh, gbc);
        return p;
    }
    private JPanel createVisPanel() {
        JPanel p = createTitledPanel("Visualization");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(2, 2, 2, 2); gbc.weightx = 1.0;
        JPanel pMinRow = createLabelSpinnerPanel("Min Ratio:", 0.0, 10.0, valMin, true);
        spinMin = (JSpinner) pMinRow.getComponent(1);
        gbc.gridx = 0; gbc.gridy = 0; p.add(pMinRow, gbc);
        sliderMin = new JSlider(0, 1000, (int)(valMin * 100));
        setupSlider(sliderMin); linkSliderAndSpinner(sliderMin, spinMin, 0.01);
        gbc.gridx = 0; gbc.gridy = 1; p.add(sliderMin, gbc);
        JPanel pMaxRow = createLabelSpinnerPanel("Max Ratio:", 0.0, 20.0, valMax, true);
        spinMax = (JSpinner) pMaxRow.getComponent(1);
        gbc.gridx = 0; gbc.gridy = 2; p.add(pMaxRow, gbc);
        sliderMax = new JSlider(0, 1000, (int)(valMax * 100));
        setupSlider(sliderMax); linkSliderAndSpinner(sliderMax, spinMax, 0.01);
        gbc.gridx = 0; gbc.gridy = 3; p.add(sliderMax, gbc);
        JPanel pLut = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel lblLut = new JLabel("LUT Color:  "); lblLut.setFont(FONT_NORMAL);
        pLut.add(lblLut);
        String[] luts = {"Fire", "Jet", "Ice", "Spectrum", "Grays", "HiLo", "Red/Green", "Green Fire Blue", "Royal", "Cool"};
        comboLUT = new JComboBox<>(luts); comboLUT.setFont(FONT_NORMAL); comboLUT.addItemListener(this);
        pLut.add(comboLUT);
        gbc.gridx = 0; gbc.gridy = 4; gbc.insets = new Insets(4, 2, 2, 2);
        p.add(pLut, gbc);
        return p;
    }
    private JPanel createActionPanel() {
        JPanel p = createTitledPanel("Actions");
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JPanel pBarBtns = new JPanel(new GridLayout(1, 2, 5, 0)); pBarBtns.setBorder(new EmptyBorder(0, 2, 5, 2)); 
        btnBarShow = new JButton("Show Legend"); btnBarShow.setFont(FONT_NORMAL); btnBarShow.addActionListener(this);
        btnBarClose = new JButton("Close Legend"); btnBarClose.setFont(FONT_NORMAL); btnBarClose.addActionListener(this);
        pBarBtns.add(btnBarShow); pBarBtns.add(btnBarClose);
        p.add(pBarBtns);
        btnApply = new JButton("<html><b>Apply to Stack</b></html>");
        btnApply.setFont(FONT_BOLD); btnApply.setForeground(new Color(200, 0, 0)); 
        btnApply.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnApply.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); 
        btnApply.setPreferredSize(new Dimension(COMPONENT_WIDTH, 35)); 
        btnApply.addActionListener(this);
        p.add(btnApply);
        return p;
    }
    private JPanel createTitledPanel(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title);
        border.setTitleFont(FONT_TITLE); border.setTitleColor(COLOR_THEME);
        p.setBorder(border); return p;
    }
    private void setupSlider(JSlider slider) { slider.setPreferredSize(new Dimension(COMPONENT_WIDTH, SLIDER_HEIGHT)); }
    private JPanel createLabelSpinnerPanel(String text, double min, double max, double current, boolean isDouble) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel(text); lbl.setFont(FONT_NORMAL);
        SpinnerModel model = isDouble ? new SpinnerNumberModel(current, min, max, 0.1) : new SpinnerNumberModel((int)current, (int)min, (int)max, 1);
        JSpinner spinner = new JSpinner(model); spinner.setFont(FONT_NORMAL);
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(4); 
        p.add(lbl, BorderLayout.WEST); p.add(spinner, BorderLayout.EAST); return p;
    }

    public static void main(String[] args) {
        try {
            System.setProperty("plugins.dir", System.getProperty("user.dir") + "\\target\\classes");
            System.setProperty("sun.java2d.uiScale", "2.0"); 
            new ImageJ();
            IJ.runPlugIn(RIA_J.class.getName(), "");
        } catch (Exception e) { e.printStackTrace(); }
    }
}