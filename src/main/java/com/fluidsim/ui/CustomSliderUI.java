package com.fluidsim.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

public class CustomSliderUI extends BasicSliderUI {
    private static final Color TRACK_COLOR = new Color(30, 30, 35);
    private static final Color TRACK_HIGHLIGHT = new Color(45, 45, 50);
    private static final Color THUMB_COLOR = new Color(80, 140, 255);
    private static final Color THUMB_BORDER = new Color(100, 160, 255);
    private static final int TRACK_HEIGHT = 4;
    private static final int THUMB_SIZE = 14;
    private static final int TRACK_ARC = 5;
    private static final Color TICK_COLOR = new Color(100, 100, 120);
    private static final Color LABEL_COLOR = new Color(180, 180, 200);
    private final Icon startIcon;
    private final Icon endIcon;
    private final boolean showTicks;
    private final boolean showLabels;
    
    public CustomSliderUI(JSlider slider, Icon startIcon, Icon endIcon, boolean showTicks, boolean showLabels) {
        super(slider);
        this.startIcon = startIcon;
        this.endIcon = endIcon;
        this.showTicks = showTicks;
        this.showLabels = showLabels;
    }
    
    @Override
    protected void calculateThumbSize() {
        thumbRect.setSize(THUMB_SIZE, THUMB_SIZE);
    }
    
    @Override
    public void paintTrack(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        Rectangle trackBounds = trackRect;
        int cy = trackBounds.y + (trackBounds.height / 2) - (TRACK_HEIGHT / 2);
        
        RoundRectangle2D trackShape = new RoundRectangle2D.Float(
            trackBounds.x, cy, trackBounds.width, TRACK_HEIGHT, TRACK_ARC, TRACK_ARC);
        
        LinearGradientPaint trackGradient = new LinearGradientPaint(
            trackBounds.x, cy, trackBounds.x, cy + TRACK_HEIGHT,
            new float[]{0f, 0.5f, 1f},
            new Color[]{TRACK_COLOR, TRACK_HIGHLIGHT, TRACK_COLOR}
        );
        
        g2d.setPaint(trackGradient);
        g2d.fill(trackShape);
        
        if (slider.isEnabled()) {
            int thumbPos = thumbRect.x + (thumbRect.width / 2);
            RoundRectangle2D activeTrack = new RoundRectangle2D.Float(
                trackBounds.x, cy, thumbPos - trackBounds.x, TRACK_HEIGHT, TRACK_ARC, TRACK_ARC);
            
            LinearGradientPaint activeGradient = new LinearGradientPaint(
                trackBounds.x, cy, trackBounds.x, cy + TRACK_HEIGHT,
                new float[]{0f, 0.5f, 1f},
                new Color[]{THUMB_COLOR.darker(), THUMB_COLOR, THUMB_COLOR.darker()}
            );
            
            g2d.setPaint(activeGradient);
            g2d.fill(activeTrack);
        }
        
        if (showTicks) {
            paintTicks(g);
        }
        if (showLabels) {
            paintLabels(g);
        }
        if (startIcon != null) {
            startIcon.paintIcon(slider, g, trackBounds.x - 20, cy - 8);
        }
        if (endIcon != null) {
            endIcon.paintIcon(slider, g, trackBounds.x + trackBounds.width + 4, cy - 8);
        }
    }
    
    @Override
    public void paintTicks(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(TICK_COLOR);
        
        Rectangle trackBounds = trackRect;
        int cy = trackBounds.y + (trackBounds.height / 2);
        
        int majorTickSpacing = slider.getMajorTickSpacing();
        int minorTickSpacing = slider.getMinorTickSpacing();
        int max = slider.getMaximum();
        int min = slider.getMinimum();
        
        if (majorTickSpacing > 0) {
            for (int value = min; value <= max; value += majorTickSpacing) {
                int x = xPositionForValue(value);
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawLine(x, cy + 8, x, cy + 14);
            }
        }
        if (minorTickSpacing > 0) {
            g2d.setStroke(new BasicStroke(0.8f));
            for (int value = min; value <= max; value += minorTickSpacing) {
                if (value % majorTickSpacing != 0) {
                    int x = xPositionForValue(value);
                    g2d.drawLine(x, cy + 8, x, cy + 11);
                }
            }
        }
    }
    
    @Override
    public void paintLabels(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(LABEL_COLOR);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        
        Rectangle trackBounds = trackRect;
        int cy = trackBounds.y + (trackBounds.height / 2);
        
        int majorTickSpacing = slider.getMajorTickSpacing();
        int max = slider.getMaximum();
        int min = slider.getMinimum();
        
        for (int value = min; value <= max; value += majorTickSpacing) {
            int x = xPositionForValue(value);
            String label = String.valueOf(value);
            FontMetrics metrics = g2d.getFontMetrics();
            int labelWidth = metrics.stringWidth(label);
            g2d.drawString(label, x - labelWidth/2, cy + 26);
        }
    }
    
    @Override
    public void paintThumb(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillOval(thumbRect.x + 1, thumbRect.y + 1, THUMB_SIZE, THUMB_SIZE);
        
        g2d.setColor(THUMB_COLOR);
        g2d.fillOval(thumbRect.x, thumbRect.y, THUMB_SIZE - 1, THUMB_SIZE - 1);
        
        g2d.setColor(THUMB_BORDER);
        g2d.drawOval(thumbRect.x, thumbRect.y, THUMB_SIZE - 1, THUMB_SIZE - 1);
        
        g2d.setColor(new Color(255, 255, 255, 50));
        g2d.fillOval(thumbRect.x + 3, thumbRect.y + 3, THUMB_SIZE - 6, THUMB_SIZE - 6);
    }
    
    @Override
    public void paintFocus(Graphics g) {
    }
} 