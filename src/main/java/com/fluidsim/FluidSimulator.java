package com.fluidsim;

import java.awt.BorderLayout;

import javax.swing.JPanel;

public class FluidSimulator extends JPanel {
    private static final int TARGET_FPS = 120;
    private static final long OPTIMAL_TIME = 1000000000 / TARGET_FPS;
    
    private final SimulationPanel simulationPanel;
    private final ControlPanel controlPanel;
    private final GPUCalculator gpuCalculator;

    public FluidSimulator() {
        setLayout(new BorderLayout());
        
        gpuCalculator = new GPUCalculator();
        simulationPanel = new SimulationPanel(gpuCalculator);
        controlPanel = new ControlPanel(simulationPanel);

        add(simulationPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);

        Thread animationThread = new Thread(() -> {
            long lastLoopTime = System.nanoTime();
            
            while (true) {
                long now = System.nanoTime();
                long updateLength = now - lastLoopTime;
                lastLoopTime = now;
                
                simulationPanel.repaint();
                controlPanel.updateFPS();
                
                try {
                    long sleepTime = (OPTIMAL_TIME - updateLength) / 1000000;
                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        animationThread.start();
    }
} 