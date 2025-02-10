package com.fluidsim.physics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fluidsim.GPUCalculator;

public class FluidSimulator {
    private static final int TARGET_FPS = 120;
    private final GPUCalculator gpuCalculator;
    private SimulationState currentState;
    private final List<SimulationListener> listeners;
    private boolean physicsEnabled = true;
    
    private float[] ghostParticles;
    private float[] ghostTemperatures;
    
    public FluidSimulator(GPUCalculator gpuCalculator) {
        this.gpuCalculator = gpuCalculator;
        this.listeners = new CopyOnWriteArrayList<>();
        this.currentState = new SimulationState();
    }
    
    public void update(float deltaTime) {
        if (currentState.hasParticles() && physicsEnabled) {
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
    
    public void setPhysicsEnabled(boolean enabled) {
        this.physicsEnabled = enabled;
    }

    public void updateGhosts(SimulationState state, float deltaTime) {
        if (state.hasParticles()) {
            ghostParticles = gpuCalculator.updateParticles(
                state.getParticles(),
                state.getTemperatures(),
                state.getMaterialIndices(),
                state.getMaterialProperties(),
                state.getWidth(),
                state.getHeight(),
                state.getMouseX(),
                state.getMouseY(),
                state.getMouseForce(),
                deltaTime,
                state.getViscosity(),
                state.getRepulsion(),
                state.getSurfaceTension(),
                state.getGravity(),
                state.getCurrentMouseForce()
            );
            ghostTemperatures = state.getTemperatures();
        }
    }

    public float[] getGhostParticles() {
        return ghostParticles;
    }

    public float[] getGhostTemperatures() {
        return ghostTemperatures;
    }

    private void startSimulationLoop() {
        Thread.startVirtualThread(() -> {
            while (!Thread.interrupted()) {
                long startTime = System.nanoTime();
                update(1.0f / TARGET_FPS);
                long endTime = System.nanoTime();
                
                long sleepTime = (1_000_000_000 / TARGET_FPS) - (endTime - startTime);
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime / 1_000_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
    }
} 