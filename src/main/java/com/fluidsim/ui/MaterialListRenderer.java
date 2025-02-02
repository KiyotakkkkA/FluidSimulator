package com.fluidsim.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;

import com.fluidsim.materials.Material;

public class MaterialListRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel)super.getListCellRendererComponent(
            list, value, index, isSelected, cellHasFocus);
        
        if (value instanceof Material) {
            Material material = (Material)value;
            label.setIcon(new ColorIcon(material.getColor()));
            label.setText(material.getName());
            label.setFont(new Font("Arial", Font.PLAIN, 12));
            
            if (!isSelected) {
                label.setBackground(new Color(45, 45, 55));
                label.setForeground(new Color(200, 200, 220));
            }
        }
        return label;
    }
    
    private static class ColorIcon implements Icon {
        private final Color color;
        private static final int SIZE = 16;
        
        public ColorIcon(int rgb) {
            this.color = new Color(rgb);
        }
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D)g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);
            g2d.fillOval(x + 2, y + 2, SIZE - 4, SIZE - 4);
        }
        
        @Override
        public int getIconWidth() { return SIZE; }
        @Override
        public int getIconHeight() { return SIZE; }
    }
} 