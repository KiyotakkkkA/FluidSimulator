package com.fluidsim.materials;

import java.awt.Color;

public class Glycerin extends Material {
    public Glycerin() {
        super("Глицерин", 1.26, new Color(230, 230, 250).getRGB(),
              2.0,
              0.064,
              0.5
        );
    }
} 