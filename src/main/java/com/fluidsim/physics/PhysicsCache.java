package com.fluidsim.physics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fluidsim.materials.Material;

public class PhysicsCache {
    private final Map<String, float[]> materialPropertiesCache = new ConcurrentHashMap<>();
    private float[] currentMaterialProperties;
    
    public float[] getMaterialProperties() {
        return currentMaterialProperties;
    }
    
    public void updateMaterialProperties(List<Material> materials) {
        String cacheKey = generateCacheKey(materials);
        currentMaterialProperties = materialPropertiesCache.computeIfAbsent(cacheKey,
            k -> calculateMaterialProperties(materials));
    }
    
    private String generateCacheKey(List<Material> materials) {
        StringBuilder key = new StringBuilder();
        for (Material m : materials) {
            key.append(m.getName())
               .append(m.getDensity())
               .append(m.getViscosity())
               .append(m.getSurfaceTension())
               .append(m.getElasticity())
               .append("|");
        }
        return key.toString();
    }
    
    private float[] calculateMaterialProperties(List<Material> materials) {
        float[] props = new float[materials.size() * 4];
        for (int i = 0; i < materials.size(); i++) {
            Material m = materials.get(i);
            int offset = i * 4;
            props[offset] = (float)m.getDensity();
            props[offset + 1] = (float)m.getViscosity();
            props[offset + 2] = (float)m.getSurfaceTension();
            props[offset + 3] = (float)m.getElasticity();
        }
        return props;
    }
} 