package com.fluidsim.materials;

import java.awt.Color;

public class Oil extends Material {
    public Oil() {
        super("Масло", 0.92, new Color(168, 140, 0).getRGB(),
              1.2,
              0.032,
              0.7
        );
    }
} 