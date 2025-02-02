package com.fluidsim;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.fluidsim.materials.Material;

public class CollapsiblePanel extends JPanel {
    private final JPanel contentPanel;
    private final JLabel titleLabel;
    private boolean isCollapsed = false;
    private final String title;
    private final List<Material> availableMaterials;
    private Material selectedMaterial;
    private MaterialComboBox materialComboBox;

    public CollapsiblePanel(String title) {
        this.title = title;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(SimulationConstants.CONTROL_PANEL_COLOR);
        setBorder(new EmptyBorder(2, 0, 2, 0));
        
        titleLabel = new JLabel(title + " ▼");
        titleLabel.setForeground(SimulationConstants.TEXT_COLOR);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(35, 35, 45));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleCollapse();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                titleLabel.setForeground(new Color(150, 150, 255));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                titleLabel.setForeground(SimulationConstants.TEXT_COLOR);
            }
        });
        
        add(titleLabel);
        add(contentPanel);
        
        availableMaterials = new ArrayList<>();
    }
    
    public void initializeMaterialSelector() {
        if (!"Отображение".equals(title)) {
            return;
        }
        
        JPanel materialPanel = new JPanel();
        materialPanel.setLayout(new BoxLayout(materialPanel, BoxLayout.Y_AXIS));
        materialPanel.setBackground(SimulationConstants.CONTROL_PANEL_COLOR);
        materialPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        materialComboBox = new MaterialComboBox();
        materialComboBox.setBackground(new Color(45, 45, 55));
        materialComboBox.setForeground(SimulationConstants.TEXT_COLOR);
        materialComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        materialComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        for (Material material : availableMaterials) {
            materialComboBox.addItem(material);
        }
        
        materialComboBox.addActionListener(e -> {
            Material oldMaterial = selectedMaterial;
            selectedMaterial = (Material) materialComboBox.getSelectedItem();
            firePropertyChange("selectedMaterial", oldMaterial, selectedMaterial);
        });
        
        contentPanel.add(materialPanel);
        contentPanel.add(materialComboBox);
        contentPanel.add(Box.createVerticalStrut(10));
    }
    
    public void setSelectedMaterial(Material material) {
        if (materialComboBox != null) {
            materialComboBox.setSelectedItem(material);
        }
        selectedMaterial = material;
    }
    
    public Material getSelectedMaterial() {
        return selectedMaterial;
    }
    
    public void addComponent(Component component) {
        contentPanel.add(component);
        revalidate();
    }
    
    private void toggleCollapse() {
        isCollapsed = !isCollapsed;
        contentPanel.setVisible(!isCollapsed);
        titleLabel.setText(titleLabel.getText().substring(0, titleLabel.getText().length() - 2) 
            + (isCollapsed ? " ▶" : " ▼"));
        revalidate();
    }
} 