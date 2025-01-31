package com.fluidsim;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ProjectProperties {
    private static final Properties properties = new Properties();
    
    static {
        try {
            File propertiesFile = new File("lib/project.properties");
            if (propertiesFile.exists()) {
                properties.load(new FileInputStream(propertiesFile));
            } else {
                System.err.println("Warning: lib/project.properties not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String getVersion() {
        return properties.getProperty("version", "unknown");
    }
    
    public static String getName() {
        return properties.getProperty("name", "FluidSim");
    }
    
    public static String getFullName() {
        return getName() + " v" + getVersion();
    }
} 