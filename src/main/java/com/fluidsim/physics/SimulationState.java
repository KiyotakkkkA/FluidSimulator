package com.fluidsim.physics;

import java.io.Serializable;

public class SimulationState implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final float[] particles;
    private final float[] temperatures;
    private final int[] materialIndices;
    private final float[] materialProperties;
    private final int width;
    private final int height;
    private final float mouseForce;
    private final float viscosity;
    private final float repulsion;
    private final float surfaceTension;
    private final float gravity;
    private final int mouseX;
    private final int mouseY;
    private final float currentMouseForce;

    public SimulationState() {
        this(new float[0], new float[0], new int[0], new float[0], 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    
    public SimulationState(float[] particles, float[] temperatures, int[] materialIndices,
                          float[] materialProperties, int width, int height, float mouseForce, 
                          float viscosity, float repulsion, float surfaceTension, float gravity,
                          int mouseX, int mouseY, float currentMouseForce) {
        this.particles = particles;
        this.temperatures = temperatures;
        this.materialIndices = materialIndices;
        this.materialProperties = materialProperties;
        this.width = width;
        this.height = height;
        this.mouseForce = mouseForce;
        this.viscosity = viscosity;
        this.repulsion = repulsion;
        this.surfaceTension = surfaceTension;
        this.gravity = gravity;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.currentMouseForce = currentMouseForce;
    }

    // Добавляем геттеры
    public float[] getParticles() { return particles; }
    public float[] getTemperatures() { return temperatures; }
    public int[] getMaterialIndices() { return materialIndices; }
    public float[] getMaterialProperties() { return materialProperties; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public float getMouseForce() { return mouseForce; }
    public float getViscosity() { return viscosity; }
    public float getRepulsion() { return repulsion; }
    public float getSurfaceTension() { return surfaceTension; }
    public float getGravity() { return gravity; }
    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }
    public float getCurrentMouseForce() { return currentMouseForce; }

    public SimulationState withParticles(float[] newParticles) {
        return new SimulationState(newParticles, temperatures, materialIndices, materialProperties,
            width, height, mouseForce, viscosity, repulsion, surfaceTension,
            gravity, mouseX, mouseY, currentMouseForce);
    }
    
    public boolean hasParticles() {
        return particles != null && particles.length > 0;
    }
} 