package com.fluidsim;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(ProjectProperties.getFullName());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setUndecorated(true);

            FluidSimulator simulator = new FluidSimulator();
            frame.add(simulator);
            
            frame.setVisible(true);
        });
    }
} 