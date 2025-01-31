package com.fluidsim;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CollapsiblePanel extends JPanel {
    private final JPanel contentPanel;
    private final JLabel titleLabel;
    private final JPanel headerPanel;
    private boolean isExpanded = true;
    private final String title;

    public CollapsiblePanel(String title) {
        this.title = title;
        setLayout(new BorderLayout());
        setBackground(SimulationConstants.CONTROL_PANEL_COLOR);
        
        // Создаем заголовок
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(40, 40, 50));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        
        titleLabel = new JLabel(title + " ▼");
        titleLabel.setForeground(SimulationConstants.TEXT_COLOR);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Создаем панель содержимого с меньшими отступами
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(SimulationConstants.CONTROL_PANEL_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        
        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        
        // Добавляем обработчик клика
        headerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleExpanded();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                headerPanel.setBackground(new Color(50, 50, 60));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                headerPanel.setBackground(new Color(40, 40, 50));
            }
        });
    }
    
    public void addComponent(Component comp) {
        contentPanel.add(comp);
    }
    
    private void toggleExpanded() {
        isExpanded = !isExpanded;
        contentPanel.setVisible(isExpanded);
        titleLabel.setText(title + (isExpanded ? " ▼" : " ▶"));
        revalidate();
        repaint();
    }
} 