package com.fluidsim.materials;

import java.awt.Color;

public class Gasoline extends Material {
    public Gasoline() {
        super("Бензин", 0.72, new Color(255, 222, 173).getRGB(),
              0.3,
              0.022,
              0.9
        );
    }
} 