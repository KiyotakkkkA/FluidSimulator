package com.fluidsim.materials;

import java.awt.Color;

public class Water extends Material {
    public Water() {
        super(
            "Вода",
            1.0,
            new Color(0, 119, 190).getRGB(),
            1.0,
            0.073,
            0.8 
        );
    }
} 