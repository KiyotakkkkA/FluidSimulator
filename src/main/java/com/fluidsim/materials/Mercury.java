package com.fluidsim.materials;

import java.awt.Color;

public class Mercury extends Material {
    public Mercury() {
        super("Ртуть", 13.534, new Color(192, 192, 192).getRGB(),
              1.5,
              0.487,
              0.2
        );
    }
} 