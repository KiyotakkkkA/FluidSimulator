package com.fluidsim.physics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fluidsim.GPUCalculator;

public class FluidSimulator {
    private final GPUCalculator gpuCalculator;
    private SimulationState currentState;
    private final List<SimulationListener> listeners;
    
    public FluidSimulator(GPUCalculator gpuCalculator) {
        this.gpuCalculator = gpuCalculator;
        this.listeners = new CopyOnWriteArrayList<>();
        this.currentState = new SimulationState();
    }
    
    public void update(float deltaTime) {
        // Обновление физики
        if (currentState.hasParticles()) {
            float[] newParticles = gpuCalculator.updateParticles(
                currentState.getParticles(),
                currentState.getTemperatures(),
                currentState.getMaterialIndices(),
                currentState.getMaterialProperties(),
                currentState.getWidth(),
                currentState.getHeight(),
                currentState.getMouseX(),
                currentState.getMouseY(),
                currentState.getMouseForce(),
                deltaTime,
                currentState.getViscosity(),
                currentState.getRepulsion(),
                currentState.getSurfaceTension(),
                currentState.getGravity(),
                currentState.getCurrentMouseForce()
            );
            
            currentState = currentState.withParticles(newParticles);
            notifyListeners();
        }
    }
    
    public void addListener(SimulationListener listener) {
        listeners.add(listener);
    }
    
    private void notifyListeners() {
        for (SimulationListener listener : listeners) {
            listener.onSimulationUpdated(currentState);
        }
    }
    
    public void saveState(String filename) {
        StateSerializer.saveState(currentState, filename);
    }
    
    public void loadState(String filename) {
        currentState = StateSerializer.loadState(filename);
        notifyListeners();
    }
    
    public void setState(SimulationState state) {
        this.currentState = state;
    }
    
    public SimulationState getCurrentState() {
        return currentState;
    }
} 