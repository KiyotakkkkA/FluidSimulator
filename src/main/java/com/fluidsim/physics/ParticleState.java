package com.fluidsim.physics;

public record ParticleState(
    float x,
    float y,
    float velocityX,
    float velocityY,
    float temperature,
    int material
) {} 