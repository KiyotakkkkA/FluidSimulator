package com.fluidsim;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
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
import com.fluidsim.ui.TabPanel;

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

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBackground(SimulationConstants.CONTROL_PANEL_COLOR);
        buttonPanel.add(Box.createHorizontalGlue());

        JButton clearButton = new JButton("Очистить") {
            {
                setBackground(new Color(60, 60, 70));
                setForeground(SimulationConstants.TEXT_COLOR);
                setBorderPainted(false);
                setFocusPainted(false);
                Dimension size = new Dimension(120, 25);
                setPreferredSize(size);
                setMaximumSize(size);
                setMinimumSize(size);
            }
        };
        clearButton.addMouseListener(new MouseListener() {
            @Override
            public void mouseEntered(MouseEvent e) {
                clearButton.setBackground(new Color(70, 70, 80));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                clearButton.setBackground(new Color(60, 60, 70));
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseClicked(MouseEvent e) {
                simulationPanel.clearParticles();
            }
        });
        
        buttonPanel.add(clearButton);
        buttonPanel.add(Box.createHorizontalGlue());
        
        add(buttonPanel);
        add(Box.createVerticalStrut(10));

        TabPanel advancedPanel = new TabPanel();
        
        JPanel physicsContent = new JPanel();
        physicsContent.setLayout(new BoxLayout(physicsContent, BoxLayout.Y_AXIS));
        physicsContent.setBackground(SimulationConstants.CONTROL_PANEL_COLOR);
        
        addPhysicsControls(physicsContent);
        
        JPanel effectsContent = new JPanel();
        effectsContent.setLayout(new BoxLayout(effectsContent, BoxLayout.Y_AXIS));
        effectsContent.setBackground(SimulationConstants.CONTROL_PANEL_COLOR);
        
        addTimeEffectsControls(effectsContent);
        
        advancedPanel.addTab("Физика", physicsContent);
        advancedPanel.addTab("Эффекты", effectsContent);
        
        add(advancedPanel);
        add(Box.createVerticalStrut(10));
    }

    private void addPhysicsControls(JPanel panel) {
        panel.add(createStyledLabel("Вязкость:"));
        JSlider viscositySlider = createStyledSlider(
            (int)(SimulationConstants.MIN_VISCOSITY * 100),
            (int)(SimulationConstants.MAX_VISCOSITY * 100),
            (int)(SimulationConstants.INITIAL_VISCOSITY * 100),
            e -> simulationPanel.setViscosity(((JSlider)e.getSource()).getValue() / 100f),
            new SliderIcons.ViscosityIcon(), null, true, true
        );
        panel.add(viscositySlider);
        
        panel.add(createStyledLabel("Отталкивание:"));
        panel.add(createStyledSlider(
            (int)SimulationConstants.MIN_REPULSION,
            (int)SimulationConstants.MAX_REPULSION,
            (int)SimulationConstants.INITIAL_REPULSION,
            e -> simulationPanel.setRepulsion(((JSlider)e.getSource()).getValue()),
            null, null, false, false
        ));
        
        panel.add(createStyledLabel("Поверх. натяжение:"));
        panel.add(createStyledSlider(
            (int)SimulationConstants.MIN_SURFACE_TENSION,
            (int)SimulationConstants.MAX_SURFACE_TENSION,
            (int)SimulationConstants.INITIAL_SURFACE_TENSION,
            e -> simulationPanel.setSurfaceTension(((JSlider)e.getSource()).getValue()),
            null, null, false, false
        ));
        
        panel.add(createStyledLabel("Гравитация:"));
        panel.add(createStyledSlider(
            (int)SimulationConstants.MIN_GRAVITY,
            (int)SimulationConstants.MAX_GRAVITY,
            (int)SimulationConstants.INITIAL_GRAVITY,
            e -> simulationPanel.setGravity(((JSlider)e.getSource()).getValue()),
            null, null, false, false
        ));
        
        panel.add(createStyledLabel("Температура:"));
        panel.add(createStyledSlider(
            (int)SimulationConstants.MIN_TEMPERATURE,
            (int)SimulationConstants.MAX_TEMPERATURE,
            (int)SimulationConstants.INITIAL_TEMPERATURE,
            e -> simulationPanel.setTemperature(((JSlider)e.getSource()).getValue()),
            null, null, false, false
        ));
    }

    private void addTimeEffectsControls(JPanel panel) {
        String[] effectsInfo = {
            "<html><b>Q</b> - Перемотка назад</html>",
            "<html><b>E</b> - Стирание времени</html>",
            "<html><b>R</b> - Ускорение времени</html>",
            "<html><b>Z</b> - Начать запись петли</html>",
            "<html><b>X</b> - Зафиксировать конец петли</html>",
            "<html><b>C</b> - Воспроизвести/остановить петлю</html>"
        };
        
        for (String info : effectsInfo) {
            panel.add(createStyledLabel(info));
        }
        
        panel.add(Box.createVerticalStrut(10));
        
        panel.add(createStyledLabel("Время перемотки (сек):"));
        panel.add(createStyledSlider(
            (int)(SimulationConstants.MIN_REWIND_TIME),
            (int)(SimulationConstants.MAX_REWIND_TIME),
            (int)(SimulationConstants.INITIAL_REWIND_TIME),
            e -> simulationPanel.setRewindTime(((JSlider)e.getSource()).getValue()),
            null, null, false, false
        ));
        
        panel.add(createStyledLabel("Макс. ускорение:"));
        JSpinner accelerationSpinner = createStyledSpinner(5.0, 1.0, 20.0, 0.5);
        accelerationSpinner.addChangeListener(e -> 
            simulationPanel.setMaxTimeAcceleration((double)accelerationSpinner.getValue()));
        panel.add(accelerationSpinner);
        
        panel.add(createStyledLabel("Длит. петли (сек):"));
        JSpinner loopDurationSpinner = createStyledSpinner(5.0, 1.0, 10.0, 0.5);
        loopDurationSpinner.addChangeListener(e -> 
            simulationPanel.setLoopDuration((double)loopDurationSpinner.getValue()));
        panel.add(loopDurationSpinner);
        
        panel.add(createStyledLabel("Скорость призраков:"));
        JSpinner ghostSpeedSpinner = createStyledSpinner(0.75, 0.1, 2.0, 0.05);
        ghostSpeedSpinner.addChangeListener(e -> 
            simulationPanel.setGhostSpeed((double)ghostSpeedSpinner.getValue()));
        panel.add(ghostSpeedSpinner);
    }

    private JSpinner createStyledSpinner(double value, double min, double max, double step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        spinner.setBackground(new Color(60, 60, 70));
        spinner.setForeground(SimulationConstants.TEXT_COLOR);
        ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setBackground(new Color(50, 50, 60));
        ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setForeground(SimulationConstants.TEXT_COLOR);
        spinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        Dimension size = new Dimension(70, 25);
        spinner.setPreferredSize(size);
        spinner.setMaximumSize(size);
        
        return spinner;
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
        
        if (diff >= 500_000_000) {
            currentFPS = frameCount / (diff / 1_000_000_000.0);
            frameCount = 0;
            lastFPSCheck = now;
            if (currentFPS > 0) {
                fpsLabel.setText(String.format("FPS: %.0f", Math.min(currentFPS, 1000.0)));
            }
        }
    }
} 