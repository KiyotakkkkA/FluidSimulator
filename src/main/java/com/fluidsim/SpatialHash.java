package com.fluidsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpatialHash {
    private final float cellSize;
    private final Map<Long, List<Integer>> grid = new HashMap<>();
    
    public SpatialHash(float cellSize) {
        this.cellSize = cellSize;
    }
    
    private long hashPos(float x, float y) {
        int xi = (int)(x / cellSize);
        int yi = (int)(y / cellSize);
        return (((long)xi) << 32) | (yi & 0xffffffffL);
    }
    
    public void clear() {
        grid.clear();
    }
    
    public void addParticle(int index, float x, float y) {
        long hash = hashPos(x, y);
        grid.computeIfAbsent(hash, k -> new ArrayList<>()).add(index);
    }
    
    public List<Integer> getNeighbors(float x, float y, float radius) {
        List<Integer> result = new ArrayList<>();
        float r = radius + cellSize;
        int minX = (int)((x - r) / cellSize);
        int maxX = (int)((x + r) / cellSize);
        int minY = (int)((y - r) / cellSize);
        int maxY = (int)((y + r) / cellSize);
        
        for (int xi = minX; xi <= maxX; xi++) {
            for (int yi = minY; yi <= maxY; yi++) {
                long hash = (((long)xi) << 32) | (yi & 0xffffffffL);
                List<Integer> cell = grid.get(hash);
                if (cell != null) {
                    result.addAll(cell);
                }
            }
        }
        return result;
    }
} 