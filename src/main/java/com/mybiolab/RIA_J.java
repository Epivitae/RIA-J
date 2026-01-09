package com.mybiolab;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.GUI;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;

/**
 * PROJECT: RIA-J (Ratio Imaging Analyzer - Java Edition)
 * VERSION: v0.3.0 (Modern UI & Color Bar Control)
 * AUTHOR: Kui Wang
 * DESCRIPTION: An interactive ImageJ plugin for ratiometric fluorescence analysis.
 */
public class RIA_J extends PlugInFrame implements PlugIn, ChangeListener, ActionListener, ItemListener {

    // --- Swing GUI Components ---
    // Using JSlider instead of AWT Scrollbar for a modern look
    private JSlider sliderBg, sliderThresh, sliderMin, sliderMax;
    private JLabel lblBg, lblThresh, lblMin, lblMax;
    private JComboBox<String> comboLUT;
    private JButton btnApply, btnAddBar, btnRemoveBar;
    
    // --- Data References ---
    private ImagePlus imp1; // Numerator
    private ImagePlus imp2; // Denominator
    private ImagePlus resultImp; // Result Window
    
    // --- Default Parameters ---
    private int valBg = 100;
    private int valThresh = 200;
    private double valMin = 0.0;
    private double valMax = 5.0;

    public RIA_J() {
        super("RIA-J Controller"); 
    }

    @Override
    public void run(String arg) {
        // 1. Safety Check
        if (WindowManager.getImageCount() < 2) {
            IJ.error("RIA-J Error", "Please open and split the dual-channel image first (Image > Color > Split Channels)!");
            return;
        }

        // 2. Select Channels
        int[] wList = WindowManager.getIDList();
        String[] titles = new String[wList.length];
        for (int i = 0; i < wList.length; i++) {
            titles[i] = WindowManager.getImage(wList[i]).getTitle();
        }

        GenericDialog gd = new GenericDialog("RIA-J: Initialize");
        gd.addChoice("Numerator (Ch1):", titles, titles[0]);
        gd.addChoice("Denominator (Ch2):", titles, titles.length > 1 ? titles[1] : titles[0]);
        gd.showDialog();
        if (gd.wasCanceled()) return;

        imp1 = WindowManager.getImage(wList[gd.getNextChoiceIndex()]);
        imp2 = WindowManager.getImage(wList[gd.getNextChoiceIndex()]);

        // 3. Create initial empty result window
        createInitialResult();

        // 4. Build the modern Swing GUI
        buildGUI();
        
        // 5. Trigger initial preview
        updatePreview();
    }

    /**
     * Build the modern Swing-based Interface
     */
    private void buildGUI() {
        // Set System Native Look and Feel (makes it look like a native Windows app)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { e.printStackTrace(); }

        // Main Container
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); // Padding

        // --- Section 1: Calculation Parameters ---
        JPanel pCalc = new JPanel(new GridBagLayout());
        pCalc.setBorder(new TitledBorder("Calculation Parameters")); 
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5); // Spacing
        gbc.weightx = 1.0;

        // Background Slider
        lblBg = new JLabel("Background: " + valBg);
        sliderBg = new JSlider(0, 1000, valBg);
        setupSlider(sliderBg);
        gbc.gridx = 0; gbc.gridy = 0; pCalc.add(lblBg, gbc);
        gbc.gridx = 0; gbc.gridy = 1; pCalc.add(sliderBg, gbc);

        // Threshold Slider
        lblThresh = new JLabel("NaN Threshold: " + valThresh);
        sliderThresh = new JSlider(0, 1000, valThresh);
        setupSlider(sliderThresh);
        gbc.gridx = 0; gbc.gridy = 2; pCalc.add(lblThresh, gbc);
        gbc.gridx = 0; gbc.gridy = 3; pCalc.add(sliderThresh, gbc);

        mainPanel.add(pCalc);
        mainPanel.add(Box.createVerticalStrut(10)); // Vertical spacer

        // --- Section 2: Visualization ---
        JPanel pVis = new JPanel(new GridBagLayout());
        pVis.setBorder(new TitledBorder("Visualization"));
        
        // Min/Max Ratio Sliders
        lblMin = new JLabel("Min Ratio: " + valMin);
        sliderMin = new JSlider(0, 1000, (int)(valMin*100));
        setupSlider(sliderMin);
        gbc.gridx = 0; gbc.gridy = 0; pVis.add(lblMin, gbc);
        gbc.gridx = 0; gbc.gridy = 1; pVis.add(sliderMin, gbc);

        lblMax = new JLabel("Max Ratio: " + valMax);
        sliderMax = new JSlider(0, 1000, (int)(valMax*100));
        setupSlider(sliderMax);
        gbc.gridx = 0; gbc.gridy = 2; pVis.add(lblMax, gbc);
        gbc.gridx = 0; gbc.gridy = 3; pVis.add(sliderMax, gbc);

        // LUT Selection
        JPanel pLut = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pLut.add(new JLabel("LUT Color: "));
        String[] luts = {"Fire", "Ice", "Physics", "Grays", "Spectrum", "Red/Green"};
        comboLUT = new JComboBox<>(luts);
        comboLUT.addItemListener(this);
        pLut.add(comboLUT);
        
        gbc.gridx = 0; gbc.gridy = 4; pVis.add(pLut, gbc);

        mainPanel.add(pVis);
        mainPanel.add(Box.createVerticalStrut(10));

        // --- Section 3: Actions ---
        JPanel pAction = new JPanel(new GridLayout(2, 2, 5, 5)); 
        pAction.setBorder(new TitledBorder("Actions"));

        btnAddBar = new JButton("Add Color Bar");
        btnAddBar.addActionListener(this);
        pAction.add(btnAddBar);

        btnRemoveBar = new JButton("Remove Bar");
        btnRemoveBar.addActionListener(this);
        pAction.add(btnRemoveBar);

        btnApply = new JButton("<html><b>Apply to Stack</b></html>"); // Bold text
        btnApply.setForeground(new Color(200, 0, 0)); // Red warning color
        btnApply.addActionListener(this);
        
        // Wrap 'Apply' button to take full width of the second row
        JPanel pApplyWrapper = new JPanel(new BorderLayout());
        pApplyWrapper.add(btnApply, BorderLayout.CENTER);
        pApplyWrapper.setBorder(new EmptyBorder(5, 0, 0, 0));

        mainPanel.add(pAction);
        mainPanel.add(pApplyWrapper);

        // Add Swing panel to ImageJ PlugInFrame
        add(mainPanel);
        pack();
        GUI.center(this);
        setVisible(true);
    }

    private void setupSlider(JSlider slider) {
        slider.setPreferredSize(new Dimension(250, 20)); 
        slider.addChangeListener(this);
    }

    private void createInitialResult() {
        int width = imp1.getWidth();
        int height = imp1.getHeight();
        FloatProcessor fp = new FloatProcessor(width, height);
        resultImp = new ImagePlus("RIA-J Preview", fp);
        resultImp.show();
        IJ.run(resultImp, "Fire", "");
    }

    // --- Event Listeners ---

    @Override
    public void stateChanged(ChangeEvent e) {
        // Handle slider dragging
        valBg = sliderBg.getValue();
        valThresh = sliderThresh.getValue();
        valMin = sliderMin.getValue() / 100.0;
        valMax = sliderMax.getValue() / 100.0;

        lblBg.setText("Background: " + valBg);
        lblThresh.setText("NaN Threshold: " + valThresh);
        lblMin.setText(String.format("Min Ratio: %.2f", valMin));
        lblMax.setText(String.format("Max Ratio: %.2f", valMax));

        // Update preview immediately (JSlider is efficient enough)
        updatePreview();
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            String lutName = (String) comboLUT.getSelectedItem();
            IJ.run(resultImp, lutName, "");
            resultImp.updateAndDraw();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnApply) {
            processEntireStack();
        } else if (src == btnAddBar) {
            addColorBar();
        } else if (src == btnRemoveBar) {
            removeColorBar();
        }
    }

    // --- Core Logic ---

    /**
     * Preview Mode: Calculates only the CURRENT frame.
     */
    private void updatePreview() {
        if (resultImp == null || resultImp.getWindow() == null) return;
        
        int currentZ = resultImp.getCurrentSlice();
        if (currentZ > imp1.getStackSize()) currentZ = 1;

        ImageProcessor ip1 = imp1.getStack().getProcessor(currentZ).convertToFloat();
        ImageProcessor ip2 = imp2.getStack().getProcessor(currentZ).convertToFloat();

        FloatProcessor fpResult = calculateSingleFrame(ip1, ip2, valBg, valThresh);
        fpResult.setMinAndMax(valMin, valMax);

        resultImp.setProcessor(fpResult);
        
        // Preserve Overlay (Color Bar) if it exists
        if (resultImp.getOverlay() != null) {
            resultImp.draw(); 
        }
        
        // Maintain LUT
        String lut = (String) comboLUT.getSelectedItem();
        if(lut == null) lut = "Fire";
        IJ.run(resultImp, lut, ""); 
    }

    /**
     * Batch Mode: Process the entire stack.
     */
    private void processEntireStack() {
        IJ.showStatus("Processing entire stack...");
        int width = imp1.getWidth();
        int height = imp1.getHeight();
        int nSlices = imp1.getStackSize();
        
        ImageStack finalStack = new ImageStack(width, height);

        for (int z = 1; z <= nSlices; z++) {
            IJ.showProgress(z, nSlices);
            ImageProcessor ip1 = imp1.getStack().getProcessor(z).convertToFloat();
            ImageProcessor ip2 = imp2.getStack().getProcessor(z).convertToFloat();
            
            FloatProcessor fp = calculateSingleFrame(ip1, ip2, valBg, valThresh);
            fp.setMinAndMax(valMin, valMax);
            finalStack.addSlice("Ratio " + z, fp);
        }

        resultImp.setStack(finalStack);
        resultImp.setTitle("RIA-J Result (Final)");
        
        String lut = (String) comboLUT.getSelectedItem();
        IJ.run(resultImp, lut != null ? lut : "Fire", ""); 
        IJ.showStatus("Finished!");
    }

    /**
     * Calculation Kernel: (Ch1 - Bg) / (Ch2 - Bg)
     */
    private FloatProcessor calculateSingleFrame(ImageProcessor ip1, ImageProcessor ip2, double bg, double thresh) {
        int width = ip1.getWidth();
        int height = ip1.getHeight();
        float[] p1 = (float[]) ip1.getPixels();
        float[] p2 = (float[]) ip2.getPixels();
        float[] pRes = new float[p1.length];

        for (int i = 0; i < p1.length; i++) {
            float v1 = p1[i] - (float)bg;
            float v2 = p2[i] - (float)bg;
            if (v1 < 0) v1 = 0;
            if (v2 < 0) v2 = 0;

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

    private void addColorBar() {
        if (resultImp == null) return;
        // Use Overlay mode to avoid modifying pixel data
        IJ.run(resultImp, "Calibration Bar...", "location=[Upper Right] fill=None label=White number=5 decimal=1 font=12 zoom=1 overlay");
    }

    private void removeColorBar() {
        if (resultImp == null) return;
        // Remove all overlays
        resultImp.setOverlay(null);
    }

    public static void main(String[] args) {
        try {
            // Development mode setup
            System.setProperty("plugins.dir", System.getProperty("user.dir") + "\\target\\classes");
            new ImageJ();
            
            // NOTE: Update this path to your local sample data for testing
            String imagePath = "D:\\2_Greek\\16_imagej\\MAX_K1555.tif"; 
            
            ImagePlus imp = IJ.openImage(imagePath);
            if (imp == null) imp = IJ.openImage();
            
            if (imp != null) {
                imp.show();
                IJ.run(imp, "Split Channels", "");
                Class<?> clazz = RIA_J.class;
                IJ.runPlugIn(clazz.getName(), "");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}