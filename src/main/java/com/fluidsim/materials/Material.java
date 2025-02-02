package com.fluidsim.materials;

public abstract class Material {
    private final String name;
    private final double density;
    private final int color;
    private final double viscosity;
    private final double surfaceTension;
    private final double elasticity;  // Коэффициент упругости для сохранения энергии

    public Material(String name, double density, int color, 
                   double viscosity, double surfaceTension, double elasticity) {
        this.name = name;
        this.density = density;
        this.color = color;
        this.viscosity = viscosity;
        this.surfaceTension = surfaceTension;
        this.elasticity = elasticity;
    }

    public String getName() {
        return name;
    }

    public double getDensity() {
        return density;
    }

    public int getColor() {
        return color;
    }

    public double getViscosity() {
        return viscosity;
    }

    public double getSurfaceTension() {
        return surfaceTension;
    }

    public double getElasticity() {
        return elasticity;
    }
} 