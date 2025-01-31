package com.fluidsim;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;

public class ControlPanel extends JPanel {
    private final JLabel fpsLabel;
    private final JLabel versionLabel;
    private long frameCount = 0;
    private long lastFPSCheck = System.nanoTime();
    private double currentFPS = 0;
    private final SimulationPanel simulationPanel;

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

        // === Секция отображения ===
        CollapsiblePanel displayPanel = new CollapsiblePanel("Отображение");
        
        displayPanel.addComponent(fpsLabel);
        displayPanel.addComponent(versionLabel);
        displayPanel.addComponent(Box.createVerticalStrut(10));
        
        displayPanel.addComponent(createStyledLabel("Размер частиц:"));
        displayPanel.addComponent(createStyledSlider(
            SimulationConstants.MIN_PARTICLE_SIZE,
            SimulationConstants.MAX_PARTICLE_SIZE,
            SimulationConstants.INITIAL_PARTICLE_SIZE,
            e -> simulationPanel.setParticleSize(((JSlider)e.getSource()).getValue())
        ));
        
        JCheckBox velocityColoringBox = createStyledCheckBox("Градиент скорости",
            e -> simulationPanel.setVelocityColoring(((JCheckBox)e.getSource()).isSelected()));
        displayPanel.addComponent(velocityColoringBox);
        
        JCheckBox temperatureColoringBox = createStyledCheckBox("Градиент температуры",
            e -> simulationPanel.setTemperatureColoring(((JCheckBox)e.getSource()).isSelected()));
        displayPanel.addComponent(temperatureColoringBox);
        
        add(displayPanel);
        add(Box.createVerticalStrut(2));

        // === Секция взаимодействия ===
        CollapsiblePanel interactionPanel = new CollapsiblePanel("Взаимодействие");
        
        interactionPanel.addComponent(createStyledLabel("Сила мыши:"));
        interactionPanel.addComponent(createStyledSlider(
            (int)SimulationConstants.MIN_MOUSE_FORCE,
            (int)SimulationConstants.MAX_MOUSE_FORCE,
            (int)SimulationConstants.INITIAL_MOUSE_FORCE,
            e -> simulationPanel.setMouseForce(((JSlider)e.getSource()).getValue())
        ));
        
        interactionPanel.addComponent(createStyledLabel("Скорость спавна:"));
        interactionPanel.addComponent(createStyledSlider(
            (int)(SimulationConstants.MIN_SPAWN_RATE * 10),
            (int)(SimulationConstants.MAX_SPAWN_RATE * 10),
            (int)(SimulationConstants.INITIAL_SPAWN_RATE * 10),
            e -> simulationPanel.setSpawnRate(((JSlider)e.getSource()).getValue() / 10.0f)
        ));
        
        add(interactionPanel);
        add(Box.createVerticalStrut(2));

        // === Секция физики жидкости ===
        CollapsiblePanel physicsPanel = new CollapsiblePanel("Физика жидкости");
        
        physicsPanel.addComponent(createStyledLabel("Вязкость:"));
        physicsPanel.addComponent(createStyledSlider(
            (int)(SimulationConstants.MIN_VISCOSITY * 100),
            (int)(SimulationConstants.MAX_VISCOSITY * 100),
            (int)(SimulationConstants.INITIAL_VISCOSITY * 100),
            e -> simulationPanel.setViscosity(((JSlider)e.getSource()).getValue() / 100.0f)
        ));
        
        physicsPanel.addComponent(createStyledLabel("Отталкивание:"));
        physicsPanel.addComponent(createStyledSlider(
            (int)SimulationConstants.MIN_REPULSION,
            (int)SimulationConstants.MAX_REPULSION,
            (int)SimulationConstants.INITIAL_REPULSION,
            e -> simulationPanel.setRepulsion(((JSlider)e.getSource()).getValue())
        ));
        
        physicsPanel.addComponent(createStyledLabel("Поверх. натяжение:"));
        physicsPanel.addComponent(createStyledSlider(
            (int)SimulationConstants.MIN_SURFACE_TENSION,
            (int)SimulationConstants.MAX_SURFACE_TENSION,
            (int)SimulationConstants.INITIAL_SURFACE_TENSION,
            e -> simulationPanel.setSurfaceTension(((JSlider)e.getSource()).getValue())
        ));
        
        physicsPanel.addComponent(createStyledLabel("Гравитация:"));
        physicsPanel.addComponent(createStyledSlider(
            (int)SimulationConstants.MIN_GRAVITY,
            (int)SimulationConstants.MAX_GRAVITY,
            (int)SimulationConstants.INITIAL_GRAVITY,
            e -> simulationPanel.setGravity(((JSlider)e.getSource()).getValue())
        ));
        
        physicsPanel.addComponent(createStyledLabel("Температура:"));
        physicsPanel.addComponent(createStyledSlider(
            (int)SimulationConstants.MIN_TEMPERATURE,
            (int)SimulationConstants.MAX_TEMPERATURE,
            (int)SimulationConstants.INITIAL_TEMPERATURE,
            e -> simulationPanel.setTemperature(((JSlider)e.getSource()).getValue())
        ));
        
        add(physicsPanel);
        add(Box.createVerticalStrut(2));

        // === Секция управления ===
        CollapsiblePanel controlPanel = new CollapsiblePanel("Управление");
        
        controlPanel.addComponent(createStyledLabel("Время перемотки (сек):"));
        controlPanel.addComponent(createStyledSlider(
            (int)(SimulationConstants.MIN_REWIND_TIME),
            (int)(SimulationConstants.MAX_REWIND_TIME),
            (int)(SimulationConstants.INITIAL_REWIND_TIME),
            e -> simulationPanel.setRewindTime(((JSlider)e.getSource()).getValue())
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

    private JSlider createStyledSlider(int min, int max, int value, ChangeListener listener) {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, value);
        slider.setBackground(SimulationConstants.CONTROL_PANEL_COLOR);
        slider.setForeground(SimulationConstants.TEXT_COLOR);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        
        int range = max - min;
        int majorTickSpacing = range / 5;
        int minorTickSpacing = majorTickSpacing / 5;
        
        slider.setMajorTickSpacing(majorTickSpacing);
        slider.setMinorTickSpacing(minorTickSpacing);
        
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        slider.setPreferredSize(new Dimension(
            SimulationConstants.CONTROL_PANEL_WIDTH - 2 * SimulationConstants.CONTROL_PANEL_PADDING, 
            50
        ));
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