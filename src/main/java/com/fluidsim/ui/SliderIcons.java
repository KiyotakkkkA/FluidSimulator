package com.fluidsim.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

public class SliderIcons {
    public static class ViscosityIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(100, 150, 255));
            
            // Рисуем каплю
            int[] xPoints = {x+8, x+4, x+12};
            int[] yPoints = {y+4, y+12, y+12};
            g2d.fillPolygon(xPoints, yPoints, 3);
            g2d.fillOval(x+4, y+8, 8, 8);
        }
        
        @Override
        public int getIconWidth() { return 16; }
        @Override
        public int getIconHeight() { return 16; }
    }
    
    public static class GravityIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(100, 150, 255));
            
            // Рисуем стрелку вниз
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(x+8, y+4, x+8, y+12);
            g2d.drawLine(x+8, y+12, x+5, y+9);
            g2d.drawLine(x+8, y+12, x+11, y+9);
        }
        
        @Override
        public int getIconWidth() { return 16; }
        @Override
        public int getIconHeight() { return 16; }
    }
    
    // Добавьте другие иконки для разных типов слайдеров
} 