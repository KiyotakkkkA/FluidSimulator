package com.fluidsim.physics;

import java.io.*;

public class StateSerializer {
    public static void saveState(SimulationState state, String filename) {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(filename))) {
            out.writeObject(state);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static SimulationState loadState(String filename) {
        try (ObjectInputStream in = new ObjectInputStream(
                new FileInputStream(filename))) {
            return (SimulationState) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new SimulationState();
        }
    }
} 