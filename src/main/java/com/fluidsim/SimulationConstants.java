package com.fluidsim;

import java.awt.Color;

public class SimulationConstants {
    // Параметры симуляции
    public static final int INITIAL_PARTICLE_SIZE = 10;
    public static final int MIN_PARTICLE_SIZE = 2;
    public static final int MAX_PARTICLE_SIZE = 20;
    public static final int INITIAL_COLS = 50;
    public static final int INITIAL_ROWS = 30;
    public static final int PARTICLE_SPACING = 15;
    public static final int INITIAL_OFFSET = 50;
    
    // Визуальные константы
    public static final Color BACKGROUND_COLOR = new Color(20, 20, 30);
    public static final Color PARTICLE_COLOR = new Color(30, 144, 255);
    public static final Color CONTROL_PANEL_COLOR = new Color(30, 30, 40);
    public static final Color TEXT_COLOR = new Color(200, 200, 220);
    public static final int CONTROL_PANEL_WIDTH = 250;
    public static final int CONTROL_PANEL_PADDING = 15;
    
    // Параметры размещения частиц
    public static final int SCREEN_MARGIN = 50;
    
    // Параметры взаимодействия
    public static final float INITIAL_MOUSE_FORCE = 500.0f;
    public static final float MIN_MOUSE_FORCE = 100.0f;
    public static final float MAX_MOUSE_FORCE = 2000.0f;
    
    // Параметры жидкости
    public static final float INITIAL_VISCOSITY = 0.8f;
    public static final float MIN_VISCOSITY = 0.0f;
    public static final float MAX_VISCOSITY = 2.0f;
    public static final float INITIAL_REPULSION = 1000.0f;
    public static final float MIN_REPULSION = 500.0f;
    public static final float MAX_REPULSION = 5000.0f;
    public static final float INITIAL_SURFACE_TENSION = 100.0f;
    public static final float MIN_SURFACE_TENSION = 0.0f;
    public static final float MAX_SURFACE_TENSION = 200.0f;
    public static final float INTERACTION_RADIUS = 40.0f;
    
    // Параметры спавна и перемотки
    public static final float INITIAL_SPAWN_RATE = 5.0f;
    public static final float MIN_SPAWN_RATE = 1.0f;
    public static final float MAX_SPAWN_RATE = 20.0f;
    public static final float INITIAL_SPAWN_RADIUS = 10.0f;
    public static final float MIN_SPAWN_RADIUS = 5.0f;
    public static final float MAX_SPAWN_RADIUS = 50.0f;
    public static final float INITIAL_REWIND_TIME = 5.0f;
    public static final float MIN_REWIND_TIME = 0.0f;
    public static final float MAX_REWIND_TIME = 10.0f;
    public static final int TARGET_FPS = 60;
    
    // Параметры физики
    public static final float INITIAL_GRAVITY = 500.0f;
    public static final float MIN_GRAVITY = 0.0f;
    public static final float MAX_GRAVITY = 2000.0f;

    // Визуальные эффекты
    public static final Color PARTICLE_COLOR_SLOW = new Color(30, 144, 255);
    public static final Color PARTICLE_COLOR_FAST = new Color(255, 50, 50);
    public static final float VELOCITY_COLOR_THRESHOLD = 1000.0f;

    // Параметры температуры
    public static final float INITIAL_TEMPERATURE = 20.0f;
    public static final float MIN_TEMPERATURE = 0.0f;
    public static final float MAX_TEMPERATURE = 100.0f;
    public static final float TEMPERATURE_DIFFUSION = 0.1f;
    
    // Цвета для температуры
    public static final Color COLD_COLOR = new Color(0, 150, 255);
    public static final Color NORMAL_COLOR = new Color(50, 255, 50);
    public static final Color HOT_COLOR = new Color(255, 50, 0);
} 