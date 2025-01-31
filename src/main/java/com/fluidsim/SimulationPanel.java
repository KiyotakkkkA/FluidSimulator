package com.fluidsim;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

public class SimulationPanel extends JPanel {
    private final GPUCalculator gpuCalculator;
    private int particleSize = SimulationConstants.INITIAL_PARTICLE_SIZE;
    private float[] particles;
    private final BufferedImage particleImage;
    private final int imageSize = 20;
    private Point mousePosition = new Point(0, 0);
    private float mouseForce = 0;
    private long lastUpdateTime = System.nanoTime();
    private float currentMouseForce = SimulationConstants.INITIAL_MOUSE_FORCE;
    private float currentViscosity = SimulationConstants.INITIAL_VISCOSITY;
    private float currentRepulsion = SimulationConstants.INITIAL_REPULSION;
    private float currentSurfaceTension = SimulationConstants.INITIAL_SURFACE_TENSION;
    private float currentGravity = SimulationConstants.INITIAL_GRAVITY;
    private List<float[]> particleHistory = new ArrayList<>();
    private float rewindTime = SimulationConstants.INITIAL_REWIND_TIME;
    private int maxHistorySize = (int)(SimulationConstants.INITIAL_REWIND_TIME * SimulationConstants.TARGET_FPS);
    private boolean isRewinding = false;
    private boolean isSpawning = false;
    private float spawnRadius = SimulationConstants.INITIAL_SPAWN_RADIUS;
    private float spawnRate = SimulationConstants.INITIAL_SPAWN_RATE;
    private boolean velocityColoring = false;
    private boolean temperatureColoring = false;
    private float currentTemperature = SimulationConstants.INITIAL_TEMPERATURE;
    private float[] particleTemperatures;

    public enum MouseMode {
        DRAWING,
        TEMPERATURE,
        VORTEX
    }
    
    private MouseMode currentMouseMode = MouseMode.DRAWING;
    private boolean fixedTemperature = false;
    private float temperatureChangeRate = 50.0f;

    public SimulationPanel(GPUCalculator gpuCalculator) {
        this.gpuCalculator = gpuCalculator;
        setBackground(SimulationConstants.BACKGROUND_COLOR);
        initializeParticles();
        particleImage = createParticleImage();
        particleTemperatures = new float[particles.length / 4];
        for (int i = 0; i < particleTemperatures.length; i++) {
            particleTemperatures[i] = SimulationConstants.INITIAL_TEMPERATURE;
        }
        
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePosition = e.getPoint();
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                mousePosition = e.getPoint();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentMouseMode == MouseMode.DRAWING) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        mouseForce = -currentMouseForce;
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        mouseForce = currentMouseForce;
                    }
                } else if (currentMouseMode == MouseMode.TEMPERATURE) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        mouseForce = currentMouseForce * 2.5f;
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        mouseForce = -currentMouseForce * 2.5f;
                    }
                } else if (currentMouseMode == MouseMode.VORTEX) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        mouseForce = currentMouseForce * 4.5f;
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        mouseForce = -currentMouseForce * 4.5f;
                    }
                }
                
                if (e.getButton() == MouseEvent.BUTTON2) {
                    isSpawning = true;
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    isSpawning = false;
                } else {
                    mouseForce = 0;
                }
            }
        };
        
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(e -> {
                if (e.getKeyCode() == KeyEvent.VK_Q) {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        isRewinding = true;
                    } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                        isRewinding = false;
                    }
                }
                return false;
            });
    }

    private void initializeParticles() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int)screenSize.getWidth();
        int screenHeight = (int)screenSize.getHeight();
        
        int margin = SimulationConstants.SCREEN_MARGIN;
        int usableWidth = screenWidth - 2 * margin - SimulationConstants.CONTROL_PANEL_WIDTH;
        int usableHeight = screenHeight - 2 * margin;
        
        int cols = usableWidth / SimulationConstants.PARTICLE_SPACING;
        int rows = usableHeight / SimulationConstants.PARTICLE_SPACING;
        
        particles = new float[cols * rows * 4];
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int idx = (i * cols + j) * 4;
                particles[idx] = margin + j * SimulationConstants.PARTICLE_SPACING;
                particles[idx + 1] = margin + i * SimulationConstants.PARTICLE_SPACING;
                particles[idx + 2] = 0;
                particles[idx + 3] = 0;
            }
        }
    }

    private BufferedImage createParticleImage() {
        BufferedImage img = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(SimulationConstants.PARTICLE_COLOR);
        g2.fillOval(0, 0, imageSize-1, imageSize-1);
        g2.dispose();
        return img;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());

        long currentTime = System.nanoTime();
        float deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0f;
        lastUpdateTime = currentTime;

        if (!isRewinding) {
            if (currentMouseMode == MouseMode.DRAWING && isSpawning) {
                addParticlesAtMouse(mousePosition);
            } else if (currentMouseMode == MouseMode.TEMPERATURE && mouseForce != 0) {
                float influence = 100.0f;
                float influence2 = influence * influence;
                
                for (int i = 0; i < particles.length; i += 4) {
                    float dx = particles[i] - mousePosition.x;
                    float dy = particles[i + 1] - mousePosition.y;
                    float dist2 = dx * dx + dy * dy;
                    
                    if (dist2 < influence2) {
                        int particleIndex = i / 4;
                        float factor = 1.0f - (float)Math.sqrt(dist2) / influence;
                        float tempChange = mouseForce > 0 ? temperatureChangeRate : -temperatureChangeRate;
                        float newTemp = particleTemperatures[particleIndex] + tempChange * factor * deltaTime;
                        particleTemperatures[particleIndex] = Math.max(SimulationConstants.MIN_TEMPERATURE,
                            Math.min(SimulationConstants.MAX_TEMPERATURE, newTemp));
                    }
                }
            }

            if (particles.length > 0) {
                particles = gpuCalculator.updateParticles(
                    particles,
                    particleTemperatures,
                    getWidth(),
                    getHeight(),
                    mousePosition.x,
                    mousePosition.y,
                    mouseForce,
                    deltaTime,
                    currentViscosity,
                    currentRepulsion,
                    currentSurfaceTension,
                    currentGravity,
                    currentMouseForce
                );
                
                float[] historyCopy = new float[particles.length];
                System.arraycopy(particles, 0, historyCopy, 0, particles.length);
                particleHistory.add(historyCopy);
                while (particleHistory.size() > maxHistorySize) {
                    particleHistory.remove(0);
                }
            }
        } else if (!particleHistory.isEmpty()) {
            particles = particleHistory.remove(particleHistory.size() - 1);
            if (particles.length > 0) {
                gpuCalculator.updateWorkSize(particles.length / 4);
            }
        }

        if (particles.length > 0) {
            double scale = particleSize / (double)imageSize;
            AffineTransform transform = new AffineTransform();
            
            for (int i = 0; i < particles.length; i += 4) {
                float x = particles[i] - particleSize/2;
                float y = particles[i + 1] - particleSize/2;
                float vx = particles[i + 2];
                float vy = particles[i + 3];
                
                if (temperatureColoring) {
                    float tempRatio = (particleTemperatures[i/4] - SimulationConstants.MIN_TEMPERATURE) / 
                        (SimulationConstants.MAX_TEMPERATURE - SimulationConstants.MIN_TEMPERATURE);
                    Color particleColor;
                    if (tempRatio <= 0.5f) {
                        particleColor = interpolateColor(
                            SimulationConstants.COLD_COLOR,
                            SimulationConstants.NORMAL_COLOR,
                            tempRatio * 2
                        );
                    } else {
                        particleColor = interpolateColor(
                            SimulationConstants.NORMAL_COLOR,
                            SimulationConstants.HOT_COLOR,
                            (tempRatio - 0.5f) * 2
                        );
                    }
                    g2d.setColor(particleColor);
                    transform.setToTranslation(x, y);
                    transform.scale(scale, scale);
                    g2d.fillOval((int)x, (int)y, particleSize, particleSize);
                } else if (velocityColoring) {
                    float velocity = (float)Math.sqrt(vx*vx + vy*vy);
                    float colorRatio = Math.min(velocity / SimulationConstants.VELOCITY_COLOR_THRESHOLD, 1.0f);
                    Color particleColor = interpolateColor(
                        SimulationConstants.PARTICLE_COLOR_SLOW,
                        SimulationConstants.PARTICLE_COLOR_FAST,
                        colorRatio
                    );
                    g2d.setColor(particleColor);
                    transform.setToTranslation(x, y);
                    transform.scale(scale, scale);
                    g2d.fillOval((int)x, (int)y, particleSize, particleSize);
                } else {
                    transform.setToTranslation(x, y);
                    transform.scale(scale, scale);
                    g2d.drawImage(particleImage, transform, null);
                }
            }
        }
    }

    public void setParticleSize(int size) {
        this.particleSize = size;
        repaint();
    }

    public void setMouseForce(float force) {
        this.currentMouseForce = force;
    }

    public void setViscosity(float viscosity) {
        this.currentViscosity = viscosity;
    }

    public void setRepulsion(float repulsion) {
        this.currentRepulsion = repulsion;
    }
    
    public void setSurfaceTension(float tension) {
        this.currentSurfaceTension = tension;
    }

    public void setSpawnRadius(float radius) {
        this.spawnRadius = radius;
    }

    public void setSpawnRate(float rate) {
        this.spawnRate = rate;
    }

    public void setRewindTime(float seconds) {
        this.rewindTime = seconds;
        this.maxHistorySize = (int)(seconds * SimulationConstants.TARGET_FPS);
        while (particleHistory.size() > maxHistorySize) {
            particleHistory.remove(0);
        }
    }

    public void setGravity(float gravity) {
        this.currentGravity = gravity;
    }

    private void addParticlesAtMouse(Point position) {
        int newParticles = (int)spawnRate;
        float[] newArray = new float[(particles.length + newParticles * 4)];
        float[] newTemperatures = new float[particleTemperatures.length + newParticles];
        
        System.arraycopy(particles, 0, newArray, 0, particles.length);
        System.arraycopy(particleTemperatures, 0, newTemperatures, 0, particleTemperatures.length);
        
        for(int i = 0; i < newParticles; i++) {
            int idx = particles.length + i * 4;
            double angle = Math.random() * 2 * Math.PI;
            double radius = Math.random() * spawnRadius;
            
            newArray[idx] = position.x + (float)(Math.cos(angle) * radius);
            newArray[idx + 1] = position.y + (float)(Math.sin(angle) * radius);
            newArray[idx + 2] = 0;
            newArray[idx + 3] = 0;
            
            newTemperatures[particleTemperatures.length + i] = SimulationConstants.INITIAL_TEMPERATURE;
        }
        
        particles = newArray;
        particleTemperatures = newTemperatures;
        gpuCalculator.updateWorkSize(particles.length / 4);
    }

    public void clearParticles() {
        particles = new float[0];
        particleTemperatures = new float[0];
        particleHistory.clear();
        gpuCalculator.updateWorkSize(0);
    }

    public void setVelocityColoring(boolean enabled) {
        this.velocityColoring = enabled;
        repaint();
    }

    public void setTemperatureColoring(boolean enabled) {
        this.temperatureColoring = enabled;
        repaint();
    }

    public void setTemperature(float temperature) {
        for (int i = 0; i < particleTemperatures.length; i++) {
            particleTemperatures[i] = temperature;
        }
    }

    public void setMouseMode(MouseMode mode) {
        this.currentMouseMode = mode;
    }

    public void setFixedTemperature(boolean fixed) {
        this.fixedTemperature = fixed;
    }

    private Color interpolateColor(Color c1, Color c2, float ratio) {
        int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * ratio);
        int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio);
        int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio);
        return new Color(r, g, b);
    }
} 