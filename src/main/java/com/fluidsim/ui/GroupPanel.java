package com.fluidsim.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

public class GroupPanel extends JPanel {
    public GroupPanel(String title) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 120)),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12),
                new Color(200, 200, 220)
            ),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        setBackground(new Color(35, 35, 45));
    }
    
    @Override
    public Component add(Component comp) {
        if (comp instanceof JComponent) {
            ((JComponent)comp).setAlignmentX(LEFT_ALIGNMENT);
        }
        return super.add(comp);
    }
} 