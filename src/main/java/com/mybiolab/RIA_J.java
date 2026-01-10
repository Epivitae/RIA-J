package com.mybiolab;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.GUI; 
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;    // [FIXED] Correct Import
import javax.swing.event.ChangeListener; // [FIXED] Correct Import
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

/**
 * PROJECT: RIA-J (Ratio Imaging Analyzer - Java Edition)
 * VERSION: v0.5.2 (Smart Workflow - No Internal Load)
 * AUTHOR: Kui Wang
 */
public class RIA_J extends PlugInFrame implements PlugIn, ActionListener, ItemListener {

    // --- GUI Design Constants ---
    private static final Color COLOR_THEME = new Color(0, 102, 204); 

    // Fonts
    private static final Font FONT_NORMAL = new Font("Arial", Font.PLAIN, 12); 
    private static final Font FONT_BOLD   = new Font("Arial", Font.BOLD, 12);
    private static final Font FONT_TITLE  = new Font("Arial", Font.BOLD, 12); 
    private static final Font FONT_HEADER = new Font("Arial", Font.BOLD, 16); 
    private static final Font FONT_SMALL  = new Font("Arial", Font.PLAIN, 10); 

    // --- Dimensions ---
    private static final int COMPONENT_WIDTH = 180; 
    private static final int SLIDER_HEIGHT   = 18;  

    // --- Components ---
    // Input Section
    private JButton btnRefresh; // Changed from Load to Refresh
    private JComboBox<String> comboNum, comboDen;
    
    // Calculation & Vis Section
    private JSlider sliderBg, sliderThresh, sliderMin, sliderMax;
    private JSpinner spinBg, spinThresh, spinMin, spinMax; 
    private JComboBox<String> comboLUT;
    private JButton btnApply, btnAddBar, btnRemoveBar;
    
    // --- Data ---
    private ImagePlus[] availableImages; // List of potential images
    private ImagePlus imp1;           // Selected Numerator
    private ImagePlus imp2;           // Selected Denominator
    private ImagePlus resultImp;      // The preview window
    
    // --- Parameters ---
    private int valBg = 20;
    private int valThresh = 50;
    private double valMin = 0.0;
    private double valMax = 5.0;

    private boolean isUpdating = false;

    public RIA_J() {
        super("RIA-J (Ratio Processor)"); 
    }

    @Override
    public void run(String arg) {
        // Build GUI immediately. Do not block.
        buildGUI();
        
        // Try to auto-initialize if images are already open
        if (WindowManager.getImageCount() > 0) {
            refreshImageList();
        }
    }

    private void buildGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { e.printStackTrace(); }

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5)); 

        // --- Section 0: Header ---
        JPanel pHeader = createHeaderPanel();
        mainPanel.add(pHeader);
        mainPanel.add(Box.createVerticalStrut(5)); 

        // --- Section 1: Input Data (Smart Logic) ---
        JPanel pInput = createTitledPanel("Input Data");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2); 
        gbc.weightx = 1.0;

        // Refresh Button
        // Use a Refresh icon logic or just text "Initialize from ImageJ"
        btnRefresh = new JButton("Import / Refresh");
        btnRefresh.setFont(FONT_BOLD);
        btnRefresh.setForeground(new Color(0, 100, 0)); // Dark Green for safety
        btnRefresh.addActionListener(this);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        pInput.add(btnRefresh, gbc);

        // Channel Selectors
        gbc.gridwidth = 1;
        
        JLabel lblNum = new JLabel("Numerator:");
        lblNum.setFont(FONT_NORMAL);
        gbc.gridx = 0; gbc.gridy = 1; pInput.add(lblNum, gbc);

        comboNum = new JComboBox<>();
        comboNum.setFont(FONT_NORMAL);
        comboNum.addActionListener(this); 
        gbc.gridx = 1; gbc.gridy = 1; pInput.add(comboNum, gbc);

        JLabel lblDen = new JLabel("Denominator:");
        lblDen.setFont(FONT_NORMAL);
        gbc.gridx = 0; gbc.gridy = 2; pInput.add(lblDen, gbc);

        comboDen = new JComboBox<>();
        comboDen.setFont(FONT_NORMAL);
        comboDen.addActionListener(this); 
        gbc.gridx = 1; gbc.gridy = 2; pInput.add(comboDen, gbc);

        mainPanel.add(pInput);
        mainPanel.add(Box.createVerticalStrut(5));

        // --- Section 2: Calculation ---
        JPanel pCalc = createTitledPanel("Calculation Parameters");
        
        // Background
        JPanel pBgRow = createLabelSpinnerPanel("Background:", 0, 1000, valBg, false);
        spinBg = (JSpinner) pBgRow.getComponent(1);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; pCalc.add(pBgRow, gbc);
        
        sliderBg = new JSlider(0, 1000, valBg);
        setupSlider(sliderBg); 
        linkSliderAndSpinner(sliderBg, spinBg, 1.0);
        gbc.gridx = 0; gbc.gridy = 1; pCalc.add(sliderBg, gbc);

        // Threshold
        JPanel pThRow = createLabelSpinnerPanel("NaN Threshold:", 0, 5000, valThresh, false);
        spinThresh = (JSpinner) pThRow.getComponent(1);
        gbc.gridx = 0; gbc.gridy = 2; pCalc.add(pThRow, gbc);

        sliderThresh = new JSlider(0, 1000, valThresh);
        setupSlider(sliderThresh);
        linkSliderAndSpinner(sliderThresh, spinThresh, 1.0);
        gbc.gridx = 0; gbc.gridy = 3; pCalc.add(sliderThresh, gbc);

        mainPanel.add(pCalc);
        mainPanel.add(Box.createVerticalStrut(5)); 

        // --- Section 3: Visualization ---
        JPanel pVis = createTitledPanel("Visualization");
        
        // Min Ratio
        JPanel pMinRow = createLabelSpinnerPanel("Min Ratio:", 0.0, 10.0, valMin, true);
        spinMin = (JSpinner) pMinRow.getComponent(1);
        gbc.gridx = 0; gbc.gridy = 0; pVis.add(pMinRow, gbc);

        sliderMin = new JSlider(0, 1000, (int)(valMin * 100));
        setupSlider(sliderMin);
        linkSliderAndSpinner(sliderMin, spinMin, 0.01);
        gbc.gridx = 0; gbc.gridy = 1; pVis.add(sliderMin, gbc);

        // Max Ratio
        JPanel pMaxRow = createLabelSpinnerPanel("Max Ratio:", 0.0, 20.0, valMax, true);
        spinMax = (JSpinner) pMaxRow.getComponent(1);
        gbc.gridx = 0; gbc.gridy = 2; pVis.add(pMaxRow, gbc);

        sliderMax = new JSlider(0, 1000, (int)(valMax * 100));
        setupSlider(sliderMax);
        linkSliderAndSpinner(sliderMax, spinMax, 0.01);
        gbc.gridx = 0; gbc.gridy = 3; pVis.add(sliderMax, gbc);

        // LUT
        JPanel pLut = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel lblLut = new JLabel("LUT Color:  ");
        lblLut.setFont(FONT_NORMAL);
        pLut.add(lblLut);
        
        String[] luts = {"Fire", "Jet", "Ice", "Spectrum", "Grays", "HiLo", "Red/Green", "Green Fire Blue", "Royal", "Cool"};
        comboLUT = new JComboBox<>(luts);
        comboLUT.setFont(FONT_NORMAL);
        comboLUT.addItemListener(this);
        pLut.add(comboLUT);
        
        gbc.gridx = 0; gbc.gridy = 4; 
        gbc.insets = new Insets(4, 2, 2, 2);
        pVis.add(pLut, gbc);

        mainPanel.add(pVis);
        mainPanel.add(Box.createVerticalStrut(5));

        // --- Section 4: Actions ---
        JPanel pAction = createTitledPanel("Actions");
        pAction.setLayout(new GridBagLayout()); 
        
        btnAddBar = new JButton("Add Color Bar");
        btnAddBar.setFont(FONT_NORMAL);
        btnAddBar.addActionListener(this);
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(2,2,2,2);
        pAction.add(btnAddBar, gbc);

        btnRemoveBar = new JButton("Remove Bar");
        btnRemoveBar.setFont(FONT_NORMAL);
        btnRemoveBar.addActionListener(this);
        gbc.gridx = 1; gbc.gridy = 0;
        pAction.add(btnRemoveBar, gbc);

        btnApply = new JButton("<html><b>Apply to Stack</b></html>");
        btnApply.setFont(FONT_BOLD);
        btnApply.setForeground(new Color(200, 0, 0));
        btnApply.setPreferredSize(new Dimension(COMPONENT_WIDTH, 35)); 
        btnApply.addActionListener(this);
        
        JPanel pApplyWrapper = new JPanel();
        pApplyWrapper.add(btnApply);

        mainPanel.add(pAction);
        mainPanel.add(Box.createVerticalStrut(2));
        mainPanel.add(pApplyWrapper);

        add(mainPanel);
        pack();
        GUI.center(this);
        setVisible(true);
    }

    // --- Logic: Image Detection & Smart Split ---

    private void refreshImageList() {
        ImagePlus activeImp = IJ.getImage(); // Get the currently active image
        
        if (activeImp == null && WindowManager.getImageCount() == 0) {
            IJ.error("RIA-J", "No images found!\nPlease open your images in ImageJ first, then click Refresh.");
            return;
        }

        // SMART LOGIC:
        // If the active image is a Composite or Stack (multi-channel), split it automatically.
        if (activeImp != null && (activeImp.isComposite() || activeImp.getNChannels() > 1)) {
            // It's a multi-channel image. Let's split it for the user.
            IJ.showStatus("Auto-splitting multi-channel image...");
            availableImages = ChannelSplitter.split(activeImp);
            if (availableImages == null) return; // Split failed
        } else {
            // It's likely single images opened separately.
            // Let's gather all open single images.
            int[] ids = WindowManager.getIDList();
            if (ids == null) return;
            
            // Filter only valid images (ignore the result preview window itself!)
            java.util.List<ImagePlus> list = new java.util.ArrayList<>();
            for (int id : ids) {
                ImagePlus imp = WindowManager.getImage(id);
                // Don't include our own result window in the source list
                if (imp != resultImp && !imp.getTitle().contains("RIA-J Preview")) {
                    list.add(imp);
                }
            }
            availableImages = list.toArray(new ImagePlus[0]);
        }
        
        // Update UI Dropdowns
        isUpdating = true;
        comboNum.removeAllItems();
        comboDen.removeAllItems();
        
        if (availableImages != null) {
            for (int i = 0; i < availableImages.length; i++) {
                String name = availableImages[i].getTitle();
                if (name.length() > 22) name = name.substring(0, 19) + "...";
                comboNum.addItem(name);
                comboDen.addItem(name);
            }
            
            // Smart select: 1st and 2nd if available
            if (availableImages.length > 0) comboNum.setSelectedIndex(0);
            if (availableImages.length > 1) comboDen.setSelectedIndex(1);
            else if (availableImages.length > 0) comboDen.setSelectedIndex(0);
        }
        
        isUpdating = false;
        
        // Trigger calculation
        updateChannelReferences();
        createInitialResult();
        updatePreview();
        
        IJ.showStatus("Images loaded.");
    }

    private void updateChannelReferences() {
        if (availableImages == null || availableImages.length == 0) return;
        
        int idx1 = comboNum.getSelectedIndex();
        int idx2 = comboDen.getSelectedIndex();
        
        if (idx1 >= 0 && idx1 < availableImages.length) imp1 = availableImages[idx1];
        if (idx2 >= 0 && idx2 < availableImages.length) imp2 = availableImages[idx2];
    }

    // --- GUI Helper Methods (Same as before) ---

    private JPanel createHeaderPanel() {
        JPanel pMain = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        URL imgURL = getClass().getResource("/images/RIA-J.png");
        if (imgURL != null) {
            ImageIcon icon = new ImageIcon(imgURL);
            Image img = icon.getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH); 
            JLabel lblIcon = new JLabel(new ImageIcon(img));
            pMain.add(lblIcon);
        }
        JPanel pText = new JPanel();
        pText.setLayout(new BoxLayout(pText, BoxLayout.Y_AXIS));
        JLabel lblTitle = new JLabel("RIA-J Controller");
        lblTitle.setFont(FONT_HEADER);
        lblTitle.setForeground(COLOR_THEME);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblCopy = new JLabel("© 2026 www.cns.ac.cn");
        lblCopy.setFont(FONT_SMALL);
        lblCopy.setForeground(Color.GRAY);
        lblCopy.setAlignmentX(Component.LEFT_ALIGNMENT); 
        pText.add(lblTitle);
        pText.add(Box.createVerticalStrut(2)); 
        pText.add(lblCopy);
        pMain.add(pText);
        return pMain;
    }

    private JPanel createTitledPanel(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), title);
        border.setTitleFont(FONT_TITLE);
        border.setTitleColor(COLOR_THEME);
        p.setBorder(border);
        return p;
    }

    private void setupSlider(JSlider slider) {
        slider.setPreferredSize(new Dimension(COMPONENT_WIDTH, SLIDER_HEIGHT)); 
    }

    private JPanel createLabelSpinnerPanel(String text, double min, double max, double current, boolean isDouble) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_NORMAL);
        SpinnerModel model;
        if (isDouble) model = new SpinnerNumberModel(current, min, max, 0.1);
        else model = new SpinnerNumberModel((int)current, (int)min, (int)max, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.setFont(FONT_NORMAL);
        Component editor = spinner.getEditor();
        JFormattedTextField ftf = ((JSpinner.DefaultEditor) editor).getTextField();
        ftf.setColumns(4); 
        p.add(lbl, BorderLayout.WEST);
        p.add(spinner, BorderLayout.EAST);
        return p;
    }

    private void linkSliderAndSpinner(JSlider slider, JSpinner spinner, double scaleFactor) {
        ChangeListener cl = e -> {
            if (isUpdating) return;
            isUpdating = true;
            if (e.getSource() == slider) {
                int sliderVal = slider.getValue();
                if (scaleFactor == 1.0) spinner.setValue(sliderVal);
                else spinner.setValue(sliderVal * scaleFactor);
            } else {
                if (scaleFactor == 1.0) slider.setValue((Integer) spinner.getValue());
                else slider.setValue((int)((Double) spinner.getValue() / scaleFactor));
            }
            updateParamsFromUI();
            updatePreview();
            isUpdating = false;
        };
        slider.addChangeListener(cl);
        spinner.addChangeListener(cl);
    }

    private void createInitialResult() {
        if (imp1 == null) return;
        if (resultImp != null) {
            resultImp.changes = false;
            resultImp.close();
        }
        int width = imp1.getWidth();
        int height = imp1.getHeight();
        FloatProcessor fp = new FloatProcessor(width, height);
        resultImp = new ImagePlus("RIA-J Preview", fp);
        resultImp.show();
        IJ.run(resultImp, "Fire", "");
    }

    // --- Core Logic ---

    private void updateParamsFromUI() {
        valBg = (Integer) spinBg.getValue();
        valThresh = (Integer) spinThresh.getValue();
        valMin = (Double) spinMin.getValue();
        valMax = (Double) spinMax.getValue();
    }

    private void updatePreview() {
        if (resultImp == null || imp1 == null || imp2 == null) return;
        int currentZ = resultImp.getCurrentSlice();
        if (currentZ > imp1.getStackSize()) currentZ = 1;
        ImageProcessor ip1 = imp1.getStack().getProcessor(currentZ).convertToFloat();
        ImageProcessor ip2 = imp2.getStack().getProcessor(currentZ).convertToFloat();
        FloatProcessor fpResult = calculateSingleFrame(ip1, ip2, valBg, valThresh);
        fpResult.setMinAndMax(valMin, valMax);
        resultImp.setProcessor(fpResult);
        if (resultImp.getOverlay() != null) resultImp.draw();
        String lut = (String) comboLUT.getSelectedItem();
        IJ.run(resultImp, lut != null ? lut : "Fire", ""); 
    }

    private void processEntireStack() {
        if (imp1 == null || imp2 == null) {
            IJ.error("No images selected!");
            return;
        }
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
            if (v2 < thresh) pRes[i] = Float.NaN;
            else {
                float r = v1 / v2;
                if (Float.isInfinite(r) || Float.isNaN(r)) pRes[i] = Float.NaN;
                else pRes[i] = r;
            }
        }
        return new FloatProcessor(width, height, pRes);
    }

    private void addColorBar() {
        if (resultImp == null) return;
        IJ.run(resultImp, "Calibration Bar...", "location=[Upper Right] fill=None label=White number=5 decimal=1 font=12 zoom=1 overlay");
    }

    private void removeColorBar() {
        if (resultImp == null) return;
        resultImp.setOverlay(null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnRefresh) {
            refreshImageList();
        } else if (src == comboNum || src == comboDen) {
            if (!isUpdating) {
                updateChannelReferences();
                updatePreview();
            }
        } else if (src == btnApply) {
            processEntireStack();
        } else if (src == btnAddBar) {
            addColorBar();
        } else if (src == btnRemoveBar) {
            removeColorBar();
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getSource() == comboLUT) {
            if (resultImp != null) {
                String lutName = (String) comboLUT.getSelectedItem();
                IJ.run(resultImp, lutName, "");
                resultImp.updateAndDraw();
            }
        }
    }

    public static void main(String[] args) {
        try {
            System.setProperty("plugins.dir", System.getProperty("user.dir") + "\\target\\classes");
            new ImageJ();
            Class<?> clazz = RIA_J.class;
            IJ.runPlugIn(clazz.getName(), "");
        } catch (Exception e) { e.printStackTrace(); }
    }
}