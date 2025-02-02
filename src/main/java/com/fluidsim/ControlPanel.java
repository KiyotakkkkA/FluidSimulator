package com.fluidsim;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;

import com.fluidsim.materials.Gasoline;
import com.fluidsim.materials.Glycerin;
import com.fluidsim.materials.Material;
import com.fluidsim.materials.Mercury;
import com.fluidsim.materials.Oil;
import com.fluidsim.materials.Water;
import com.fluidsim.ui.CustomSliderUI;
import com.fluidsim.ui.SliderIcons;

public class ControlPanel extends JPanel {
    private final JLabel fpsLabel;
    private final JLabel versionLabel;
    private long frameCount = 0;
    private long lastFPSCheck = System.nanoTime();
    private double currentFPS = 0;
    private final SimulationPanel simulationPanel;
    private final CollapsiblePanel displayPanel;
    private final CollapsiblePanel interactionPanel;
    private final CollapsiblePanel physicsPanel;
    private final CollapsiblePanel controlPanel;

    public ControlPanel(SimulationPanel simulationPanel) {
        this.simulationPanel = simulationPanel;
        this.fpsLabel = createStyledLabel("FPS: 0.0");
        this.versionLabel = createStyledLabel("v" + ProjectProperties.getVersion());
        
        setPreferredSize(new Dimension(SimulationConstants.CONTROL_PANEL_WIDTH, 0));
        setBackground(SimulationConstants.CONTROL_PANEL_COLOR);
        setBorder(BorderFactory.createEmptyBorder(
            SimulationConstants.CONTROL_PANEL_PADDING,
            SimulationConstants.CONTROL_PANEL_PADDING,
            SimulationConstants.CONTROL_PANEL_PADDING,
            SimulationConstants.CONTROL_PANEL_PADDING
        ));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        displayPanel = new CollapsiblePanel("Отображение");
        interactionPanel = new CollapsiblePanel("Взаимодействие");
        physicsPanel = new CollapsiblePanel("Физика жидкости");
        controlPanel = new CollapsiblePanel("Управление");
        
        displayPanel.addComponent(fpsLabel);
        displayPanel.addComponent(versionLabel);
        displayPanel.addComponent(Box.createVerticalStrut(10));
        
        displayPanel.addComponent(createStyledLabel("Размер частиц:"));
        displayPanel.addComponent(createStyledSlider(
            SimulationConstants.MIN_PARTICLE_SIZE,
            SimulationConstants.MAX_PARTICLE_SIZE,
            SimulationConstants.INITIAL_PARTICLE_SIZE,
            e -> simulationPanel.setParticleSize(((JSlider)e.getSource()).getValue()),
            null, null, false, false
        ));
        
        displayPanel.addComponent(Box.createVerticalStrut(5));
        displayPanel.addComponent(createStyledLabel("Материал:"));
        
        List<Material> materials = new ArrayList<>();
        materials.add(new Water());
        materials.add(new Oil());
        materials.add(new Mercury());
        materials.add(new Gasoline());
        materials.add(new Glycerin());
        materials.sort((a, b) -> Double.compare(a.getDensity(), b.getDensity()));
        
        MaterialComboBox materialComboBox = new MaterialComboBox();
        materialComboBox.setBackground(new Color(45, 45, 55));
        materialComboBox.setForeground(SimulationConstants.TEXT_COLOR);
        materialComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        materialComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        for (Material material : materials) {
            materialComboBox.addItem(material);
        }
        
        materialComboBox.addActionListener(e -> {
            Material selectedMaterial = (Material) materialComboBox.getSelectedItem();
            simulationPanel.setMaterial(selectedMaterial);
        });
        
        displayPanel.addComponent(materialComboBox);
        displayPanel.addComponent(Box.createVerticalStrut(5));
        
        JCheckBox velocityColoringBox = createStyledCheckBox("Градиент скорости",
            e -> simulationPanel.setVelocityColoring(((JCheckBox)e.getSource()).isSelected()));
        displayPanel.addComponent(velocityColoringBox);
        
        JCheckBox temperatureColoringBox = createStyledCheckBox("Градиент температуры",
            e -> simulationPanel.setTemperatureColoring(((JCheckBox)e.getSource()).isSelected()));
        displayPanel.addComponent(temperatureColoringBox);

        displayPanel.addPropertyChangeListener("selectedMaterial", evt -> {
            Material selectedMaterial = (Material) evt.getNewValue();
            interactionPanel.setSelectedMaterial(selectedMaterial);
            physicsPanel.setSelectedMaterial(selectedMaterial);
            controlPanel.setSelectedMaterial(selectedMaterial);
            simulationPanel.setMaterial(selectedMaterial);
        });
        
        add(displayPanel);
        add(Box.createVerticalStrut(2));

        interactionPanel.addComponent(createStyledLabel("Сила мыши:"));
        interactionPanel.addComponent(createStyledSlider(
            (int)(SimulationConstants.MIN_MOUSE_FORCE),
            (int)(SimulationConstants.MAX_MOUSE_FORCE),
            (int)(SimulationConstants.INITIAL_MOUSE_FORCE),
            e -> simulationPanel.setMouseForce(((JSlider)e.getSource()).getValue()),
            null, null, false, false
        ));
        
        interactionPanel.addComponent(createStyledLabel("Скорость спавна:"));
        interactionPanel.addComponent(createStyledSlider(
            (int)(SimulationConstants.MIN_SPAWN_RATE * 10),
            (int)(SimulationConstants.MAX_SPAWN_RATE * 10),
            (int)(SimulationConstants.INITIAL_SPAWN_RATE * 10),
            e -> simulationPanel.setSpawnRate(((JSlider)e.getSource()).getValue() / 10.0f),
            null, null, false, false
        ));
        
        add(interactionPanel);
        add(Box.createVerticalStrut(2));

        physicsPanel.addComponent(createStyledLabel("Вязкость:"));
        JSlider viscositySlider = createStyledSlider(
            (int)(SimulationConstants.MIN_VISCOSITY * 100),
            (int)(SimulationConstants.MAX_VISCOSITY * 100),
            (int)(SimulationConstants.INITIAL_VISCOSITY * 100),
            e -> simulationPanel.setViscosity(((JSlider)e.getSource()).getValue() / 100f),
            new SliderIcons.ViscosityIcon(), null, true, true
        );
        physicsPanel.addComponent(viscositySlider);
        
        physicsPanel.addComponent(createStyledLabel("Отталкивание:"));
        physicsPanel.addComponent(createStyledSlider(
            (int)SimulationConstants.MIN_REPULSION,
            (int)SimulationConstants.MAX_REPULSION,
            (int)SimulationConstants.INITIAL_REPULSION,
            e -> simulationPanel.setRepulsion(((JSlider)e.getSource()).getValue()),
            null, null, false, false
        ));
        
        physicsPanel.addComponent(createStyledLabel("Поверх. натяжение:"));
        physicsPanel.addComponent(createStyledSlider(
            (int)SimulationConstants.MIN_SURFACE_TENSION,
            (int)SimulationConstants.MAX_SURFACE_TENSION,
            (int)SimulationConstants.INITIAL_SURFACE_TENSION,
            e -> simulationPanel.setSurfaceTension(((JSlider)e.getSource()).getValue()),
            null, null, false, false
        ));
        
        physicsPanel.addComponent(createStyledLabel("Гравитация:"));
        physicsPanel.addComponent(createStyledSlider(
            (int)SimulationConstants.MIN_GRAVITY,
            (int)SimulationConstants.MAX_GRAVITY,
            (int)SimulationConstants.INITIAL_GRAVITY,
            e -> simulationPanel.setGravity(((JSlider)e.getSource()).getValue()),
            null, null, false, false
        ));
        
        physicsPanel.addComponent(createStyledLabel("Температура:"));
        physicsPanel.addComponent(createStyledSlider(
            (int)SimulationConstants.MIN_TEMPERATURE,
            (int)SimulationConstants.MAX_TEMPERATURE,
            (int)SimulationConstants.INITIAL_TEMPERATURE,
            e -> simulationPanel.setTemperature(((JSlider)e.getSource()).getValue()),
            null, null, false, false
        ));
        
        add(physicsPanel);
        add(Box.createVerticalStrut(2));

        controlPanel.addComponent(createStyledLabel("Время перемотки (сек):"));
        controlPanel.addComponent(createStyledSlider(
            (int)(SimulationConstants.MIN_REWIND_TIME),
            (int)(SimulationConstants.MAX_REWIND_TIME),
            (int)(SimulationConstants.INITIAL_REWIND_TIME),
            e -> simulationPanel.setRewindTime(((JSlider)e.getSource()).getValue()),
            null, null, false, false
        ));
        
        JButton clearButton = new JButton("Очистить") {
            {
                setBackground(new Color(60, 60, 70));
                setForeground(SimulationConstants.TEXT_COLOR);
                setFocusPainted(false);
                setBorderPainted(false);
                setFont(new Font("Arial", Font.BOLD, 14));
                
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        setBackground(new Color(70, 70, 80));
                    }
                    public void mouseExited(MouseEvent e) {
                        setBackground(new Color(60, 60, 70));
                    }
                });
            }
        };
        
        controlPanel.addComponent(createStyledLabel("Режим мыши:"));
        
        ButtonGroup mouseGroup = new ButtonGroup();
        
        JRadioButton drawingMode = createStyledRadioButton("Рисование",
            e -> simulationPanel.setMouseMode(SimulationPanel.MouseMode.DRAWING));
        JRadioButton tempMode = createStyledRadioButton("Температура",
            e -> simulationPanel.setMouseMode(SimulationPanel.MouseMode.TEMPERATURE));
        JRadioButton vortexMode = createStyledRadioButton("Вихрь",
            e -> simulationPanel.setMouseMode(SimulationPanel.MouseMode.VORTEX));
        
        mouseGroup.add(drawingMode);
        mouseGroup.add(tempMode);
        mouseGroup.add(vortexMode);
        
        drawingMode.setSelected(true);
        
        controlPanel.addComponent(drawingMode);
        controlPanel.addComponent(tempMode);
        controlPanel.addComponent(vortexMode);

        clearButton.addActionListener(e -> simulationPanel.clearParticles());
        controlPanel.addComponent(clearButton);
        
        add(controlPanel);
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(SimulationConstants.TEXT_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(5, 0, 5, 0));
        if (text.startsWith("===")) {
            label.setFont(label.getFont().deriveFont(Font.BOLD));
        }
        return label;
    }

    private JSlider createStyledSlider(int min, int max, int value, ChangeListener listener, 
                                     Icon startIcon, Icon endIcon, boolean showTicks, boolean showLabels) {
        JSlider slider = new JSlider(min, max, value);
        slider.setBackground(new Color(45, 45, 55));
        slider.setForeground(new Color(200, 200, 220));
        
        if (showTicks || showLabels) {
            int range = max - min;
            slider.setMajorTickSpacing(range / 5);
            slider.setMinorTickSpacing(range / 25);
        }
        
        slider.setUI(new CustomSliderUI(slider, startIcon, endIcon, showTicks, showLabels));
        slider.addChangeListener(listener);
        return slider;
    }

    private JCheckBox createStyledCheckBox(String text, ActionListener listener) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setBackground(SimulationConstants.CONTROL_PANEL_COLOR);
        checkBox.setForeground(SimulationConstants.TEXT_COLOR);
        checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkBox.addActionListener(listener);
        return checkBox;
    }

    private JRadioButton createStyledRadioButton(String text, ActionListener listener) {
        JRadioButton radioButton = new JRadioButton(text);
        radioButton.setBackground(SimulationConstants.CONTROL_PANEL_COLOR);
        radioButton.setForeground(SimulationConstants.TEXT_COLOR);
        radioButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        radioButton.addActionListener(listener);
        return radioButton;
    }

    public void updateFPS() {
        frameCount++;
        long now = System.nanoTime();
        long diff = now - lastFPSCheck;
        
        if (diff >= 500_000_000) { // Обновляем каждые 0.5 секунды для более стабильного показания
            currentFPS = frameCount / (diff / 1_000_000_000.0);
            frameCount = 0;
            lastFPSCheck = now;
            if (currentFPS > 0) { // Защита от отрицательных значений
                fpsLabel.setText(String.format("FPS: %.0f", Math.min(currentFPS, 1000.0)));
            }
        }
    }
} 