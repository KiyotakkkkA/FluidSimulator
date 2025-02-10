package com.fluidsim.ui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.fluidsim.SimulationConstants;

public class TabPanel extends JPanel {
    private final JPanel tabButtons = new JPanel();
    private final JPanel content = new JPanel();
    private final CardLayout cardLayout = new CardLayout();
    private final ButtonGroup buttonGroup = new ButtonGroup();
    
    public TabPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(SimulationConstants.CONTROL_PANEL_COLOR);
        
        tabButtons.setLayout(new BoxLayout(tabButtons, BoxLayout.X_AXIS));
        tabButtons.setBackground(SimulationConstants.CONTROL_PANEL_COLOR);
        content.setLayout(cardLayout);
        content.setBackground(SimulationConstants.CONTROL_PANEL_COLOR);
        
        add(tabButtons);
        add(content);
    }
    
    public void addTab(String name, JPanel panel) {
        JToggleButton tabButton = new JToggleButton(name) {
            {
                setBackground(new Color(50, 50, 60));
                setForeground(SimulationConstants.TEXT_COLOR);
                setBorderPainted(false);
                setFocusPainted(false);
                setFont(new Font("Arial", Font.BOLD, 12));
            }
            
            @Override
            public void setSelected(boolean selected) {
                super.setSelected(selected);
                setForeground(selected ? Color.BLACK : SimulationConstants.TEXT_COLOR);
                setBackground(selected ? new Color(70, 70, 80) : new Color(50, 50, 60));
            }
        };
        
        tabButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!tabButton.isSelected()) {
                    tabButton.setBackground(new Color(60, 60, 70));
                }
            }
            public void mouseExited(MouseEvent e) {
                if (!tabButton.isSelected()) {
                    tabButton.setBackground(new Color(50, 50, 60));
                }
            }
        });
        
        tabButton.addActionListener(e -> {
            cardLayout.show(content, name);
            for (Component c : tabButtons.getComponents()) {
                if (c instanceof JToggleButton) {
                    JToggleButton btn = (JToggleButton)c;
                    btn.setForeground(btn.isSelected() ? Color.BLACK : SimulationConstants.TEXT_COLOR);
                    btn.setBackground(btn.isSelected() ? new Color(70, 70, 80) : new Color(50, 50, 60));
                }
            }
        });
        
        buttonGroup.add(tabButton);
        tabButtons.add(tabButton);
        
        if (buttonGroup.getButtonCount() == 1) {
            tabButton.setSelected(true);
            tabButton.setBackground(new Color(70, 70, 80));
            tabButton.setForeground(Color.BLACK);
        }
        
        content.add(panel, name);
        cardLayout.show(content, name);
    }
} 