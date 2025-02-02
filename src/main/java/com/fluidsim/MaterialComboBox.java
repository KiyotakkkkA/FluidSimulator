package com.fluidsim;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

import com.fluidsim.materials.Material;

public class MaterialComboBox extends JComboBox<Material> {
    private static final int ICON_SIZE = 16;
    
    public MaterialComboBox() {
        setRenderer(new MaterialListRenderer());
    }
    
    private static class MaterialListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Material) {
                Material material = (Material) value;
                label.setIcon(createColorIcon(material.getColor()));
                label.setText(material.getName());
                label.setIconTextGap(10);
            }
            
            return label;
        }
        
        private Icon createColorIcon(int rgb) {
            return new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setColor(new Color(rgb));
                    g2d.fillRect(x, y, ICON_SIZE, ICON_SIZE);
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.drawRect(x, y, ICON_SIZE-1, ICON_SIZE-1);
                    g2d.dispose();
                }
                
                @Override
                public int getIconWidth() {
                    return ICON_SIZE;
                }
                
                @Override
                public int getIconHeight() {
                    return ICON_SIZE;
                }
            };
        }
    }
} 