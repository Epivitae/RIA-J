package com.mybiolab;

import ij.IJ;
import ij.ImageJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog; 
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
import java.util.ArrayList;

/**
 * PROJECT: RIA-J (Ratio Imaging Analyzer - Java Edition)
 * VERSION: v1.1.1 (UI Fix: Fixed Width & Layout Stability)
 * AUTHOR: Kui Wang
 */
public class RIA_J extends PlugInFrame implements PlugIn, ActionListener, ItemListener, ImageListener {

    // --- GUI Design Constants ---
    private static final Color COLOR_THEME_BLUE = new Color(0, 102, 204); 
    private static final Color COLOR_THEME_RED  = new Color(220, 50, 50); 

    // Fonts
    private static final Font FONT_NORMAL = new Font("SansSerif", Font.PLAIN, 12); 
    private static final Font FONT_BOLD   = new Font("SansSerif", Font.BOLD, 12);
    private static final Font FONT_TITLE  = new Font("SansSerif", Font.BOLD, 12); 
    private static final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 16); 
    private static final Font FONT_SMALL  = new Font("SansSerif", Font.PLAIN, 10); 
    
    // [UI FIX 1] Increased component width to force a wider, consistent window
    private static final int COMPONENT_WIDTH = 180; 
    private static final int SLIDER_HEIGHT   = 15;  
    
    // [UI FIX 2] Fixed Main Panel Width
    private static final int MAIN_PANEL_WIDTH = 340;

    // --- Components ---
    private JButton btnRefresh; 
    private JButton btnSwap; 
    private JComboBox<String> comboNum, comboDen;
    private JSlider sliderBg, sliderThresh, sliderMin, sliderMax;
    private JSpinner spinBg, spinThresh, spinMin, spinMax; 
    private JComboBox<String> comboLUT;
    private JButton btnBarShow, btnBarClose; 
    private JButton btnSnapshot; 
    private JButton btnRecalc; 
    
    // --- Data Objects ---
    private ImagePlus[] availableImages;
    private ImagePlus imp1;           
    private ImagePlus imp2;           
    private ImagePlus resultImp;      
    private ImagePlus impLegend;      
    
    // --- State Flags ---
    private boolean isUpdatingUI = false;      
    private boolean isBatchProcessing = false; 

    // --- Parameters ---
    private int valBg = 20;
    private int valThresh = 50;
    private double valMin = 0.0;
    private double valMax = 5.0;

    public RIA_J() {
        super("Ratio Analyzer"); 
        ImagePlus.addImageListener(this); 
    }

    @Override
    public void run(String arg) {
        buildGUI();
        if (WindowManager.getImageCount() > 0) {
            refreshImageList();
        }
    }

    // ========================================================================
    // LOGIC BLOCK 1: Auto-Recovery & Window Management
    // ========================================================================
    
    private void ensureResultImpExists() {
        if (resultImp != null && resultImp.isVisible()) return;
        attemptRecoverResultImp();
        if (resultImp != null) return;

        if (imp1 == null) return;
        int width = imp1.getWidth(); 
        int height = imp1.getHeight();
        FloatProcessor fp = new FloatProcessor(width, height);
        resultImp = new ImagePlus(generateResultTitle(), fp);
        resultImp.show();
    }

    private void attemptRecoverResultImp() {
        int[] ids = WindowManager.getIDList();
        if (ids == null) return;
        
        for (int id : ids) {
            ImagePlus imp = WindowManager.getImage(id);
            if (imp != null && imp.getTitle().startsWith("RIA-")) {
                resultImp = imp;
                IJ.showStatus("Recovered: " + imp.getTitle());
                isUpdatingUI = true;
                valMin = resultImp.getDisplayRangeMin();
                valMax = resultImp.getDisplayRangeMax();
                spinMin.setValue(valMin);
                spinMax.setValue(valMax);
                syncSlidersFromValues();
                isUpdatingUI = false;
                break; 
            }
        }
    }

    @Override
    public void imageOpened(ImagePlus imp) {}

    @Override
    public void imageClosed(ImagePlus imp) {
        if (imp == resultImp) resultImp = null;
        if (imp == impLegend) impLegend = null;
    }

    @Override
    public void imageUpdated(ImagePlus imp) {
        if (isBatchProcessing || isUpdatingUI) return;

        if (imp == resultImp) {
            double curDisplayMin = imp.getDisplayRangeMin();
            double curDisplayMax = imp.getDisplayRangeMax();

            if (Math.abs(curDisplayMin - valMin) > 0.001 || Math.abs(curDisplayMax - valMax) > 0.001) {
                isUpdatingUI = true; 
                valMin = curDisplayMin;
                valMax = curDisplayMax;
                spinMin.setValue(valMin);
                spinMax.setValue(valMax);
                syncSlidersFromValues();
                
                if (isValidLegendOpen()) updateLegend();
                isUpdatingUI = false; 
            }
        }
    }

    // ========================================================================
    // LOGIC BLOCK 2: Naming & Processing
    // ========================================================================

    private String getChannelTag(ImagePlus imp) {
        if (imp == null) return "?";
        String t = imp.getTitle();
        if (t.matches("^(?i)C\\d+-.*")) {
            return t.substring(0, t.indexOf('-'));
        }
        if (t.contains(".")) t = t.substring(0, t.lastIndexOf('.'));
        if (t.length() > 6) return t.substring(0, 4);
        return t;
    }

    private String getBaseName(ImagePlus imp) {
        if (imp == null) return "Image";
        String t = imp.getTitle();
        if (t.endsWith(".tif") || t.endsWith(".tiff") || t.endsWith(".nd2") || t.endsWith(".lsm")) {
            t = t.substring(0, t.lastIndexOf('.'));
        }
        return t.replaceAll("^(?i)C\\d+-", "");
    }

    private String generateResultTitle() {
        if (imp1 == null || imp2 == null) return "RIA-Result";
        String t1 = getChannelTag(imp1);
        String t2 = getChannelTag(imp2);
        String base = getBaseName(imp1);
        return "RIA-" + t1 + "_" + t2 + "-Result-" + base;
    }

    private void updatePreview(boolean reCalculateMath) {
        if (imp1 == null || imp2 == null) return;
        if (imp1 == imp2) {
            IJ.showStatus("Warning: Numerator and Denominator are the same!");
            return;
        }
        
        ensureResultImpExists(); 

        if (reCalculateMath) {
            int currentZ = resultImp.getCurrentSlice();
            int inputZ = currentZ; 
            if (currentZ > imp1.getStackSize()) inputZ = 1; 
            
            ImageProcessor ip1 = imp1.getStack().getProcessor(inputZ).convertToFloat();
            ImageProcessor ip2 = imp2.getStack().getProcessor(inputZ).convertToFloat();
            
            FloatProcessor fpResult = calculateRatioMath(ip1, ip2, valBg, valThresh);
            
            if (resultImp.getStackSize() > 1) {
                resultImp.getStack().setProcessor(fpResult, currentZ);
            } else {
                resultImp.setProcessor(fpResult);
            }
        }

        String lut = (String) comboLUT.getSelectedItem();
        if (lut != null && !lut.startsWith("---")) {
             IJ.run(resultImp, lut, ""); 
        }
        
        resultImp.setDisplayRange(valMin, valMax); 
        resultImp.setTitle(generateResultTitle());
        resultImp.updateAndDraw();
        
        if (isValidLegendOpen()) updateLegend();
    }

    private FloatProcessor calculateRatioMath(ImageProcessor ip1, ImageProcessor ip2, double bg, double thresh) {
        int width = ip1.getWidth(); int height = ip1.getHeight();
        float[] p1 = (float[]) ip1.getPixels(); float[] p2 = (float[]) ip2.getPixels();
        float[] pRes = new float[p1.length];
        
        for (int i = 0; i < p1.length; i++) {
            float v1 = p1[i] - (float)bg; float v2 = p2[i] - (float)bg;
            if (v1 < 0) v1 = 0; if (v2 < 0) v2 = 0; 
            if (v2 < thresh) pRes[i] = Float.NaN; 
            else {
                float r = v1 / v2;
                if (Float.isInfinite(r) || Float.isNaN(r)) pRes[i] = Float.NaN; 
                else pRes[i] = r;
            }
        }
        return new FloatProcessor(width, height, pRes);
    }

    private void processEntireStack() {
        if (imp1 == null || imp2 == null || imp1 == imp2) return;

        isBatchProcessing = true; 
        
        try {
            int width = imp1.getWidth(); 
            int height = imp1.getHeight(); 
            int nSlices = imp1.getStackSize();
            
            ImageStack finalStack = new ImageStack(width, height);
            
            for (int z = 1; z <= nSlices; z++) {
                if(SwingUtilities.isEventDispatchThread() == false) IJ.showProgress(z, nSlices);
                
                ImageProcessor ip1 = imp1.getStack().getProcessor(z).convertToFloat();
                ImageProcessor ip2 = imp2.getStack().getProcessor(z).convertToFloat();
                FloatProcessor fp = calculateRatioMath(ip1, ip2, valBg, valThresh);
                
                String label = imp1.getStack().getSliceLabel(z);
                if(label == null || label.isEmpty()) label = "Ratio-" + z;
                finalStack.addSlice(label, fp);
            }

            SwingUtilities.invokeLater(() -> {
                ensureResultImpExists();
                resultImp.setStack(finalStack);
                resultImp.setTitle(generateResultTitle());
                
                String lut = (String) comboLUT.getSelectedItem();
                if (lut != null && !lut.startsWith("---")) IJ.run(resultImp, lut, ""); 
                
                resultImp.setDisplayRange(valMin, valMax);
                resultImp.setSlice(1); 
                resultImp.updateAndDraw();
                
                if (isValidLegendOpen()) updateLegend();
                IJ.showStatus("Full stack recalculated!");
            });
            
        } catch (Exception e) {
            IJ.handleException(e);
        } finally {
            isBatchProcessing = false; 
        }
    }
    
    private void createRGBSnapshot() {
        if (resultImp == null) attemptRecoverResultImp();
        if (resultImp == null) { IJ.error("No result image found."); return; }

        boolean doStack = false;
        if (resultImp.getStackSize() > 1) {
            GenericDialog gd = new GenericDialog("Export Options");
            gd.addMessage("Export RGB Stack (Movie)?");
            gd.addCheckbox("Convert All Frames", true);
            gd.setOKLabel("Export RGB");
            gd.showDialog();
            if (gd.wasCanceled()) return;
            doStack = gd.getNextBoolean();
        }

        String currentTitle = resultImp.getTitle();
        String newTitle = currentTitle.replace("RIA-", "RIA-RGB-");

        if (doStack) {
            IJ.showStatus("Synchronizing stack...");
            isBatchProcessing = true;
            try {
                int nSlices = imp1.getStackSize();
                ImageStack stack = resultImp.getStack();
                for (int z = 1; z <= nSlices; z++) {
                    IJ.showProgress(z, nSlices);
                    ImageProcessor ip1 = imp1.getStack().getProcessor(z).convertToFloat();
                    ImageProcessor ip2 = imp2.getStack().getProcessor(z).convertToFloat();
                    FloatProcessor fp = calculateRatioMath(ip1, ip2, valBg, valThresh);
                    stack.setProcessor(fp, z);
                }
            } catch(Exception e) { e.printStackTrace(); } 
            finally { isBatchProcessing = false; }
            
            IJ.showStatus("Converting to RGB...");
            ImagePlus snapshot = resultImp.duplicate(); 
            snapshot.setTitle(newTitle);
            snapshot.setDisplayRange(valMin, valMax);
            IJ.run(snapshot, "RGB Color", ""); 
            snapshot.show();
            
        } else {
            ImageProcessor currentIp = resultImp.getProcessor().duplicate();
            ImagePlus snapshot = new ImagePlus(newTitle, currentIp);
            snapshot.setDisplayRange(valMin, valMax);
            snapshot.setLut(resultImp.getProcessor().getLut());
            IJ.run(snapshot, "RGB Color", "");
            snapshot.show();
        }
        IJ.showStatus("Done.");
    }

    // ========================================================================
    // LOGIC BLOCK 3: UI Events
    // ========================================================================

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        
        if (src == btnRefresh) {
            refreshImageList(); 
        } 
        else if (src == btnSwap) { 
            if (comboNum.getItemCount() > 1) {
                isUpdatingUI = true; 
                int idxNum = comboNum.getSelectedIndex();
                int idxDen = comboDen.getSelectedIndex();
                comboNum.setSelectedIndex(idxDen);
                comboDen.setSelectedIndex(idxNum);
                updateChannelReferences();
                isUpdatingUI = false;
                
                if (imp1 != null && imp2 != null) {
                    IJ.showStatus("Swapped. Showing preview...");
                    updatePreview(true);
                }
            }
        }
        else if (src == btnRecalc) { 
             new Thread(() -> {
                IJ.showStatus("Recalculating entire stack...");
                processEntireStack(); 
            }).start();
        }
        else if (src == comboNum || src == comboDen) {
            if (!isUpdatingUI) { 
                updateChannelReferences(); 
                updatePreview(true);   
            }
        } 
        else if (src == btnBarShow) {
            if (resultImp == null) attemptRecoverResultImp(); 
            if (resultImp != null) {
                if (!resultImp.isVisible()) resultImp.show(); 
                updateLegend();
            }
        } 
        else if (src == btnBarClose) {
            if (isValidLegendOpen()) { impLegend.close(); impLegend = null; }
        }
        else if (src == btnSnapshot) { 
            createRGBSnapshot();
        }
    }

    private void linkSliderAndSpinner(JSlider slider, JSpinner spinner, double scaleFactor) {
        ChangeListener cl = e -> {
            if (isUpdatingUI) return; 
            isUpdatingUI = true;      

            if (e.getSource() == slider) {
                int val = slider.getValue();
                if (scaleFactor == 1.0) spinner.setValue(val); 
                else spinner.setValue(val * scaleFactor);
            } else {
                if (scaleFactor == 1.0) slider.setValue((Integer) spinner.getValue());
                else slider.setValue((int)((Double) spinner.getValue() / scaleFactor));
            }
            updateParamsFromUI();
            boolean isMathChange = (slider == sliderBg || slider == sliderThresh);
            updatePreview(isMathChange);
            isUpdatingUI = false;     
        };
        slider.addChangeListener(cl); 
        spinner.addChangeListener(cl);
    }
    
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getSource() == comboLUT) {
            if (resultImp == null) attemptRecoverResultImp();
            if (resultImp != null) {
                isUpdatingUI = true;
                String lutName = (String) comboLUT.getSelectedItem();
                if (lutName == null || lutName.startsWith("---")) return;
                IJ.run(resultImp, lutName, "");
                resultImp.setDisplayRange(valMin, valMax); 
                resultImp.updateAndDraw(); 
                if (isValidLegendOpen()) updateLegend();
                isUpdatingUI = false;
            }
        }
    }

    // ========================================================================
    // LOGIC BLOCK 4: Init & Utils
    // ========================================================================

    private void refreshImageList() {
        int[] ids = WindowManager.getIDList();
        if (ids == null) ids = new int[0];

        ArrayList<ImagePlus> list = new ArrayList<>();
        
        for (int id : ids) {
            ImagePlus imp = WindowManager.getImage(id);
            if (imp == null) continue;
            
            if (imp == resultImp || imp == impLegend) continue;
            if (imp.getTitle().startsWith("RIA-") || imp.getTitle().startsWith("Legend")) continue;

            if (imp.isComposite()) {
                ImagePlus[] channels = ChannelSplitter.split(imp);
                if (channels != null) {
                    for (ImagePlus c : channels) {
                        list.add(c);
                    }
                }
            } else {
                list.add(imp);
            }
        }

        if (list.isEmpty()) {
            IJ.error("RIA-J", "No images found!"); 
            return;
        }

        availableImages = list.toArray(new ImagePlus[0]);
        
        if (resultImp == null) attemptRecoverResultImp();

        isUpdatingUI = true;
        comboNum.removeAllItems(); comboDen.removeAllItems();
        
        for (ImagePlus imp : availableImages) {
            String name = imp.getTitle();
            // Truncate logic handles logical string, panel width handles visual
            if (name.length() > 25) name = name.substring(0, 22) + "...";
            comboNum.addItem(name); comboDen.addItem(name);
        }
        
        comboNum.setSelectedIndex(0);
        if (availableImages.length > 1) {
            comboDen.setSelectedIndex(1);
        } else {
            comboDen.setSelectedIndex(0);
        }
        
        isUpdatingUI = false;
        
        updateChannelReferences();
    }
    
    private String[] buildLutList() {
        String[] preferred = {
            "Fire", "Viridis", "Magma", "Ice", "Grays", "Green Fire Blue", "HiLo", "Jet"
        };
        String[] installed = IJ.getLuts(); 
        if (installed == null) installed = new String[0];
        
        java.util.List<String> finalList = new java.util.ArrayList<>();
        java.util.Set<String> added = new java.util.HashSet<>();
        
        for (String s : preferred) {
            finalList.add(s);
            added.add(s.toLowerCase());
        }
        finalList.add("--------------------");
        for (String s : installed) {
            if (!added.contains(s.toLowerCase())) {
                finalList.add(s);
            }
        }
        return finalList.toArray(new String[0]);
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
        int sMin = (int)(valMin * 100);
        int sMax = (int)(valMax * 100);
        if (sMin >= 0 && sMin <= 1000) sliderMin.setValue(sMin);
        if (sMax >= 0 && sMax <= 1000) sliderMax.setValue(sMax);
    }

    // ========================================================================
    // LOGIC BLOCK 5: Visualization
    // ========================================================================

    // (Code for updateLegend, isValidLegendOpen, close omitted for brevity but presumed same)
    private void updateLegend() {
        if (resultImp == null) return;
        int barW = 20; int barH = 200;
        int padTop = 15; int padBot = 15; int padLeft = 10; int padRight = 60;
        int tickGap = 8; 
        
        int totalW = padLeft + barW + padRight; int totalH = padTop + barH + padBot;

        byte[] pixels = new byte[barW * barH];
        for (int y = 0; y < barH; y++) {
            for (int x = 0; x < barW; x++) pixels[y * barW + x] = (byte) (255 - (y * 255 / barH)); 
        }
        ByteProcessor ipBar = new ByteProcessor(barW, barH, pixels);
        ColorModel cm = resultImp.getProcessor().getColorModel();
        if(cm != null) ipBar.setColorModel(cm);
        ImageProcessor ipLegendCol = ipBar.convertToRGB();

        ColorProcessor ipFinal = new ColorProcessor(totalW, totalH);
        ipFinal.setColor(Color.WHITE); 
        ipFinal.fill();
        ipFinal.insert(ipLegendCol, padLeft, padTop);
        
        ipFinal.setColor(Color.BLACK);
        ipFinal.drawRect(padLeft - 1, padTop - 1, barW + 1, barH + 1);

        ipFinal.setColor(Color.BLACK);
        ipFinal.setFont(new Font("SansSerif", Font.PLAIN, 12));
        ipFinal.setAntialiasedText(true);
        int steps = 5;
        for (int i = 0; i <= steps; i++) {
            int yPos = padTop + barH - (int)((double)i / steps * barH);
            ipFinal.drawLine(padLeft + barW + 1, yPos, padLeft + barW + 4, yPos);
            double val = valMin + (valMax - valMin) * i / steps;
            ipFinal.drawString(String.format("%.2f", val), padLeft + barW + tickGap, yPos + 5);
        }

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
        ImagePlus.removeImageListener(this); 
        if (isValidLegendOpen()) impLegend.close();
        if (resultImp != null) resultImp.close();
    }

    // ========================================================================
    // GUI Construction
    // ========================================================================

    private void buildGUI() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(8, 8, 8, 8)); 
        
        // [UI FIX 2] Force fixed width for consistency
        mainPanel.setPreferredSize(new Dimension(MAIN_PANEL_WIDTH, 0));

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
    
    private void styleButton(JButton btn, Color fgColor) {
        btn.setFont(FONT_BOLD);
        btn.setForeground(fgColor);
        btn.setFocusPainted(false); 
    }

    private JPanel createHeaderPanel() {
        JPanel pMain = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        URL imgURL = getClass().getResource("/images/RIA-J-128.png");
        if (imgURL != null) {
            Image img = new ImageIcon(imgURL).getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH); 
            pMain.add(new JLabel(new ImageIcon(img)));
        }
        JPanel pText = new JPanel(); pText.setLayout(new BoxLayout(pText, BoxLayout.Y_AXIS));
        JLabel lblTitle = new JLabel("RIA-J Controller"); 
        lblTitle.setFont(FONT_HEADER); 
        lblTitle.setForeground(COLOR_THEME_BLUE); 
        pText.add(lblTitle); pText.add(Box.createVerticalStrut(2)); 
        JLabel lblCopy = new JLabel("© 2026 www.cns.ac.cn"); 
        lblCopy.setFont(FONT_SMALL); 
        lblCopy.setForeground(Color.GRAY);
        pText.add(lblCopy); pMain.add(pText);
        return pMain;
    }

    private JPanel createInputPanel() {
        JPanel p = createTitledPanel("Input Data");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(2, 2, 2, 2); gbc.weightx = 1.0;
        
        btnRefresh = new JButton("Import / Refresh");
        styleButton(btnRefresh, COLOR_THEME_BLUE); 
        btnRefresh.addActionListener(this);
        
        btnSwap = new JButton("Swap ⇄"); 
        styleButton(btnSwap, Color.DARK_GRAY);
        btnSwap.addActionListener(this);
        
        JPanel pTopBtns = new JPanel(new GridLayout(1, 2, 5, 0));
        pTopBtns.add(btnRefresh);
        pTopBtns.add(btnSwap);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; p.add(pTopBtns, gbc);
        
        gbc.gridwidth = 1;
        JLabel lblNum = new JLabel("Numerator:"); lblNum.setFont(FONT_NORMAL);
        gbc.gridx = 0; gbc.gridy = 1; p.add(lblNum, gbc);
        comboNum = new JComboBox<>(); comboNum.setFont(FONT_NORMAL); comboNum.addActionListener(this); 
        // [UI FIX 3] Limit dropdown width to avoid window explosion
        comboNum.setPreferredSize(new Dimension(150, 25));
        gbc.gridx = 1; gbc.gridy = 1; p.add(comboNum, gbc);
        
        JLabel lblDen = new JLabel("Denominator:"); lblDen.setFont(FONT_NORMAL);
        gbc.gridx = 0; gbc.gridy = 2; p.add(lblDen, gbc);
        comboDen = new JComboBox<>(); comboDen.setFont(FONT_NORMAL); comboDen.addActionListener(this); 
        comboDen.setPreferredSize(new Dimension(150, 25));
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

        // --- LUT Selection ---
        JPanel pLut = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel lblLut = new JLabel("LUT Color:  "); lblLut.setFont(FONT_NORMAL);
        pLut.add(lblLut);
        
        String[] allLuts = buildLutList();
        comboLUT = new JComboBox<>(allLuts); 
        comboLUT.setFont(FONT_NORMAL); 
        comboLUT.setMaximumRowCount(15); 
        comboLUT.addItemListener(this);
        pLut.add(comboLUT);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.insets = new Insets(4, 2, 2, 2);
        p.add(pLut, gbc);
        return p;
    }

    private JPanel createActionPanel() {
        JPanel p = createTitledPanel("Actions");
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        
        JPanel pBarBtns = new JPanel(new GridLayout(1, 2, 5, 0)); 
        pBarBtns.setBorder(new EmptyBorder(0, 2, 5, 2)); 
        
        btnBarShow = new JButton("Show Legend"); 
        btnBarShow.setFont(FONT_NORMAL); btnBarShow.setFocusPainted(false);
        btnBarShow.addActionListener(this);
        
        btnBarClose = new JButton("Close Legend"); 
        btnBarClose.setFont(FONT_NORMAL); btnBarClose.setFocusPainted(false);
        btnBarClose.addActionListener(this);
        
        pBarBtns.add(btnBarShow); pBarBtns.add(btnBarClose);
        p.add(pBarBtns);

        JPanel pExportBtns = new JPanel(new GridLayout(1, 2, 5, 0));
        pExportBtns.setBorder(new EmptyBorder(0, 2, 0, 2));
        
        btnRecalc = new JButton("Recalculate Stack"); 
        styleButton(btnRecalc, COLOR_THEME_BLUE); 
        btnRecalc.addActionListener(this);
        
        btnSnapshot = new JButton("Save as RGB");
        styleButton(btnSnapshot, COLOR_THEME_RED); 
        btnSnapshot.addActionListener(this);
        
        pExportBtns.add(btnRecalc);
        pExportBtns.add(btnSnapshot);
        
        p.add(Box.createVerticalStrut(5));
        p.add(pExportBtns);

        return p;
    }

    private JPanel createTitledPanel(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)), title); 
        border.setTitleFont(FONT_TITLE); 
        border.setTitleColor(COLOR_THEME_BLUE); 
        p.setBorder(border); 
        return p;
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