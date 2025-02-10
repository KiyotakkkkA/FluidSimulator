package com.fluidsim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;

import com.fluidsim.materials.Gasoline;
import com.fluidsim.materials.Glycerin;
import com.fluidsim.materials.Material;
import com.fluidsim.materials.Mercury;
import com.fluidsim.materials.Oil;
import com.fluidsim.materials.Water;
import com.fluidsim.physics.FluidSimulator;
import com.fluidsim.physics.SimulationListener;
import com.fluidsim.physics.SimulationState;

import jdk.incubator.vector.FloatVector;

public class SimulationPanel extends JPanel implements SimulationListener {
    private final GPUCalculator gpuCalculator;
    private int particleSize = SimulationConstants.INITIAL_PARTICLE_SIZE;
    private float[] particles;
    private BufferedImage particleImage;
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
    private Material currentMaterial;
    private int[] particleColors;
    private int[] particleMaterials;
    private int currentMaterialIndex;

    public enum MouseMode {
        DRAWING,
        TEMPERATURE,
        VORTEX
    }
    
    private MouseMode currentMouseMode = MouseMode.DRAWING;
    private boolean fixedTemperature = false;
    private float temperatureChangeRate = 50.0f;

    private final FluidSimulator simulator;
    private boolean physicsEnabled = true;

    private float[] ghostParticles;
    private float[] ghostTemperatures;
    private boolean ghostsActive = false;

    private long lastBlinkTime = System.nanoTime();
    private boolean rewindBlinkState = true;
    private boolean eraseBlinkState = true;

    private Random random = new Random();

    private static final int NUM_CRACKS = 25;
    private static final int NUM_CRACK_POINTS = 8;
    private List<List<float[]>> cracks = new ArrayList<>();
    private float[][] stars = null;
    private long effectStartTime;
    private static final int NUM_STARS = 200;

    private boolean isAccelerating = false;
    private float timeAcceleration = 1.0f;
    private float MAX_TIME_ACCELERATION = 10.0f;
    private static final float ACCELERATION_RATE = 2.0f;
    
    private float deltaTime;
    
    private BufferedImage motionBlurBuffer;

    private List<Float> accelerationWaves = new ArrayList<>();
    private static final float WAVE_SPEED = 500.0f;
    private static final int MAX_WAVES = 3;

    private boolean isRecordingLoop = false;
    private boolean isPlayingLoop = false;
    private float[] loopStartPoints;
    private float[] loopEndPoints;
    private float[] loopParticles;
    private float loopTimer = 0;
    private float loopDisplayTimer = 0;
    private long lastLoopTime = System.nanoTime();
    private float LOOP_DURATION = 5.0f;
    private static final Color LOOP_START_COLOR = new Color(147, 112, 219, 200);
    private static final Color LOOP_END_COLOR = new Color(218, 112, 214, 200);
    private static final Color LOOP_COLOR = new Color(147, 112, 219);
    private float LOOP_GHOST_SPEED = 0.75f;

    private static final int CHROMATIC_OFFSET = 3;

    public SimulationPanel(GPUCalculator gpuCalculator) {
        this.gpuCalculator = gpuCalculator;
        this.simulator = new FluidSimulator(gpuCalculator);
        simulator.addListener(this);
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
                if (e.getKeyCode() == KeyEvent.VK_E) {
                    if (e.getID() == KeyEvent.KEY_PRESSED && !ghostsActive) {
                        physicsEnabled = false;
                        ghostParticles = Arrays.copyOf(particles, particles.length);
                        ghostTemperatures = Arrays.copyOf(particleTemperatures, particleTemperatures.length);
                        ghostsActive = true;
                        effectStartTime = System.nanoTime();
                        initializeCracks();
                    } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                        physicsEnabled = true;
                        ghostsActive = false;
                        ghostParticles = null;
                        ghostTemperatures = null;
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_R) {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        isAccelerating = true;
                    } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                        isAccelerating = false;
                        timeAcceleration = 1.0f;
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_Z) {
                    if (e.getID() == KeyEvent.KEY_PRESSED && !isPlayingLoop) {
                        isRecordingLoop = true;
                        loopStartPoints = Arrays.copyOf(particles, particles.length);
                        loopTimer = 0;
                        loopDisplayTimer = 0;
                        lastLoopTime = System.nanoTime();
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_X) {
                    if (e.getID() == KeyEvent.KEY_PRESSED && isRecordingLoop) {
                        loopEndPoints = Arrays.copyOf(particles, particles.length);
                        isRecordingLoop = false;
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_C) {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        if (loopStartPoints != null && loopEndPoints != null) {
                            isPlayingLoop = !isPlayingLoop;
                            if (isPlayingLoop) {
                                loopParticles = Arrays.copyOf(loopStartPoints, loopStartPoints.length);
                                loopTimer = 0;
                            }
                        }
                        if (!isPlayingLoop) {
                            loopStartPoints = null;
                            loopEndPoints = null;
                            loopParticles = null;
                        }
                    }
                }
                return false;
            });

        setFocusable(true);
        requestFocusInWindow();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0f;
        lastUpdateTime = currentTime;

        if (isAccelerating) {
            if (accelerationWaves.isEmpty() || 
                (accelerationWaves.get(accelerationWaves.size() - 1) > getWidth() * 0.2f)) {
                accelerationWaves.add(0f);
            }

            g2d.setStroke(new BasicStroke(2));
            for (int i = accelerationWaves.size() - 1; i >= 0; i--) {
                float radius = accelerationWaves.get(i);
                float alpha = Math.max(0, 1 - radius / getWidth());
                
                g2d.setColor(new Color(255, 215, 0, (int)(100 * alpha)));
                g2d.drawOval(
                    getWidth()/2 - (int)radius,
                    getHeight()/2 - (int)radius,
                    (int)radius * 2,
                    (int)radius * 2
                );
                
                radius += WAVE_SPEED * deltaTime;
                if (radius > Math.max(getWidth(), getHeight())) {
                    accelerationWaves.remove(i);
                } else {
                    accelerationWaves.set(i, radius);
                }
            }

            while (accelerationWaves.size() > MAX_WAVES) {
                accelerationWaves.remove(0);
            }

            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            String text = String.format("УСКОРЕНИЕ ВРЕМЕНИ (%.1fx)", timeAcceleration);
            g2d.drawString(text, 20, 35);

            Graphics2D blurG2d = getMotionBlurBuffer().createGraphics();
            blurG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            blurG2d.drawImage(g2d.getDeviceConfiguration().createCompatibleImage(
                getWidth(), getHeight(), Transparency.TRANSLUCENT), 0, 0, null);
            blurG2d.dispose();
        } else {
            accelerationWaves.clear();
        }

        if (!isRewinding) {
            if (physicsEnabled) {
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

                List<Material> materials = Arrays.asList(
                    new Water(), new Oil(), new Mercury(),
                    new Gasoline(), new Glycerin()
                );
                
                float[] materialProps = new float[materials.size() * 4];
                for (int i = 0; i < materials.size(); i++) {
                    Material m = materials.get(i);
                    int offset = i * 4;
                    materialProps[offset] = (float)m.getDensity();
                    materialProps[offset + 1] = (float)m.getViscosity();
                    materialProps[offset + 2] = (float)m.getSurfaceTension();
                    materialProps[offset + 3] = (float)m.getElasticity();
                }

                float effectiveDeltaTime = deltaTime;
                if (isAccelerating) {
                    timeAcceleration = Math.min(timeAcceleration + ACCELERATION_RATE * deltaTime, MAX_TIME_ACCELERATION);
                    effectiveDeltaTime *= timeAcceleration;
                }

                SimulationState state = new SimulationState(
                    particles,
                    particleTemperatures,
                    particleMaterials,
                    materialProps,
                    getWidth(),
                    getHeight(),
                    mouseForce,
                    currentViscosity,
                    currentRepulsion,
                    currentSurfaceTension,
                    currentGravity,
                    mousePosition.x,
                    mousePosition.y,
                    currentMouseForce
                );

                simulator.setState(state);
                simulator.update(effectiveDeltaTime);
                
                float[] historyCopy = new float[particles.length];
                System.arraycopy(particles, 0, historyCopy, 0, particles.length);
                particleHistory.add(historyCopy);
                while (particleHistory.size() > maxHistorySize) {
                    particleHistory.remove(0);
                }
                
                particles = simulator.getCurrentState().getParticles();
            }
        } else if (!particleHistory.isEmpty()) {
            particles = particleHistory.remove(particleHistory.size() - 1);
            if (particles.length > 0) {
                gpuCalculator.updateWorkSize(particles.length / 4);
            }
        }

        if (ghostsActive) {
            initializeStars();
            g2d.setColor(new Color(0, 0, 20));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            
            for (float[] star : stars) {
                int alpha = (int)(star[2] * 255);
                float size = star[2] * 3;
                g2d.setColor(new Color(255, 255, 255, alpha));
                g2d.fillOval((int)star[0], (int)star[1], (int)size, (int)size);
                
                star[2] = Math.max(0.2f, Math.min(1.0f, 
                    star[2] + (random.nextFloat() - 0.5f) * 0.05f));
            }

            if (System.nanoTime() - effectStartTime < 500_000_000) {
                float progress = Math.min(1.0f, 
                    (System.nanoTime() - effectStartTime) / 500_000_000.0f);
                
                for (List<float[]> crack : cracks) {
                    for (int i = 1; i < crack.size(); i++) {
                        float[] p1 = crack.get(i-1);
                        float[] p2 = crack.get(i);
                        
                        float currentLength = progress * p1[3];
                        float alpha = (int)(200 * (1 - progress * (i / (float)crack.size())));
                        
                        for (int w = 3; w >= 0; w--) {
                            float strokeWidth = p1[4] * (w + 1) * 0.5f;
                            g2d.setStroke(new BasicStroke(strokeWidth));
                            g2d.setColor(new Color(255, 0, 0, (int)(alpha / (w + 1))));
                            g2d.drawLine(
                                (int)p1[0], (int)p1[1],
                                (int)(p1[0] + (p2[0] - p1[0]) * progress),
                                (int)(p1[1] + (p2[1] - p1[1]) * progress)
                            );
                        }
                        
                        if (random.nextFloat() < 0.3f) {
                            float microAngle = p1[2] + (random.nextFloat() - 0.5f) * 2.0f;
                            float microLength = currentLength * 0.3f;
                            int mx = (int)(p1[0] + Math.cos(microAngle) * microLength);
                            int my = (int)(p1[1] + Math.sin(microAngle) * microLength);
                            g2d.setStroke(new BasicStroke(p1[4] * 0.5f));
                            g2d.setColor(new Color(255, 0, 0, (int)(alpha * 0.7f)));
                            g2d.drawLine((int)p1[0], (int)p1[1], mx, my);
                        }
                    }
                }
            }

            if (currentTime - lastBlinkTime > 500_000_000) {
                eraseBlinkState = !eraseBlinkState;
                lastBlinkTime = currentTime;
            }

            if (eraseBlinkState) {
                g2d.setColor(new Color(255, 0, 0, 200));
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                String text = "< СТИРАНИЕ ВРЕМЕНИ >";
                int textX = 20;
                int textY = 35;
                g2d.drawString(text, textX, textY);
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
                
                Color particleColor;
                if (velocityColoring) {
                    float velocity = (float)Math.sqrt(vx*vx + vy*vy);
                    float colorRatio = Math.min(velocity / SimulationConstants.VELOCITY_COLOR_THRESHOLD, 1.0f);
                    particleColor = interpolateColor(
                        SimulationConstants.PARTICLE_COLOR_SLOW,
                        SimulationConstants.PARTICLE_COLOR_FAST,
                        colorRatio
                    );
                } else if (temperatureColoring) {
                    float tempRatio = (particleTemperatures[i/4] - SimulationConstants.MIN_TEMPERATURE) / 
                        (SimulationConstants.MAX_TEMPERATURE - SimulationConstants.MIN_TEMPERATURE);
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
                } else {
                    particleColor = new Color(particleColors[i/4]);
                }
                
                g2d.setColor(particleColor);
                g2d.fillOval((int)x, (int)y, particleSize, particleSize);
            }
        }

        if (ghostsActive && ghostParticles != null && ghostParticles.length == particles.length) {
            for (int i = 0; i < ghostParticles.length; i += 4) {
                float x = ghostParticles[i] - particleSize/2;
                float y = ghostParticles[i + 1] - particleSize/2;
                
                Color particleColor;
                if (temperatureColoring) {
                    float tempRatio = (ghostTemperatures[i/4] - SimulationConstants.MIN_TEMPERATURE) / 
                        (SimulationConstants.MAX_TEMPERATURE - SimulationConstants.MIN_TEMPERATURE);
                    particleColor = tempRatio <= 0.5f ?
                        interpolateColor(SimulationConstants.COLD_COLOR, SimulationConstants.NORMAL_COLOR, tempRatio * 2) :
                        interpolateColor(SimulationConstants.NORMAL_COLOR, SimulationConstants.HOT_COLOR, (tempRatio - 0.5f) * 2);
                } else {
                    particleColor = new Color(particleColors[i/4]);
                }
                
                g2d.setColor(new Color(particleColor.getRed(), 0, 0, 80));
                g2d.fillOval((int)(x - CHROMATIC_OFFSET), (int)(y - CHROMATIC_OFFSET), 
                            particleSize, particleSize);
                
                g2d.setColor(new Color(0, particleColor.getGreen(), 0, 80));
                g2d.fillOval((int)x, (int)y, particleSize, particleSize);
                
                g2d.setColor(new Color(0, 0, particleColor.getBlue(), 80));
                g2d.fillOval((int)(x + CHROMATIC_OFFSET), (int)(y + CHROMATIC_OFFSET), 
                            particleSize, particleSize);
            }
            
            SimulationState ghostState = new SimulationState(
                ghostParticles,
                ghostTemperatures,
                particleMaterials,
                simulator.getCurrentState().getMaterialProperties(),
                getWidth(),
                getHeight(),
                mouseForce,
                currentViscosity,
                currentRepulsion,
                currentSurfaceTension,
                currentGravity,
                mousePosition.x,
                mousePosition.y,
                currentMouseForce
            );
            
            simulator.updateGhosts(ghostState, deltaTime);
            ghostParticles = simulator.getGhostParticles();
            ghostTemperatures = simulator.getGhostTemperatures();
            
            repaint();
        }

        if (isRewinding) {
            g2d.setColor(new Color(0, 0, 255, 30));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            
            g2d.setColor(new Color(255, 255, 255, 100));
            int numDots = 50;
            for (int i = 0; i < numDots; i++) {
                int x = random.nextInt(getWidth());
                int y = random.nextInt(getHeight());
                int size = random.nextInt(3) + 1;
                g2d.fillOval(x, y, size, size);
            }

            if (currentTime - lastBlinkTime > 500_000_000) {
                rewindBlinkState = !rewindBlinkState;
                lastBlinkTime = currentTime;
            }
            
            if (rewindBlinkState) {
                g2d.setColor(new Color(100, 100, 255, 200));
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                
                int textX = 20;
                int textY = 35;
                int triangleSize = 12;
                int triangleGap = 4;
                int triangleY = textY - triangleSize;
                
                int[] xPoints1 = {textX, textX, textX + triangleSize};
                int[] yPoints1 = {triangleY, triangleY + triangleSize, triangleY + triangleSize/2};
                g2d.fillPolygon(xPoints1, yPoints1, 3);
                
                int[] xPoints2 = {textX + triangleSize + triangleGap, textX + triangleSize + triangleGap, 
                                textX + triangleSize * 2 + triangleGap};
                int[] yPoints2 = {triangleY, triangleY + triangleSize, triangleY + triangleSize/2};
                g2d.fillPolygon(xPoints2, yPoints2, 3);
                
                g2d.drawString("ПЕРЕМОТКА", textX + triangleSize * 2 + triangleGap + 10, textY);
            }
        }

        if (isRecordingLoop || isPlayingLoop) {
            g2d.setColor(new Color(147, 112, 219, 30));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            long currentLoopTime = System.nanoTime();
            float loopDeltaTime = (currentLoopTime - lastLoopTime) / 1_000_000_000.0f;
            lastLoopTime = currentLoopTime;

            if (isRecordingLoop) {
                loopDisplayTimer += loopDeltaTime;
                if (loopDisplayTimer >= LOOP_DURATION) {
                    loopEndPoints = Arrays.copyOf(particles, particles.length);
                    isRecordingLoop = false;
                }
            }

            if (isPlayingLoop) {
                loopTimer = (loopTimer + loopDeltaTime) % LOOP_DURATION;
            }

            if (loopStartPoints != null) {
                g2d.setColor(LOOP_START_COLOR);
                for (int i = 0; i < loopStartPoints.length; i += 4) {
                    g2d.fillOval(
                        (int)(loopStartPoints[i] - particleSize/2),
                        (int)(loopStartPoints[i + 1] - particleSize/2),
                        particleSize, particleSize
                    );
                }
            }

            if (loopEndPoints != null) {
                g2d.setColor(LOOP_END_COLOR);
                for (int i = 0; i < loopEndPoints.length; i += 4) {
                    g2d.fillOval(
                        (int)(loopEndPoints[i] - particleSize/2),
                        (int)(loopEndPoints[i + 1] - particleSize/2),
                        particleSize, particleSize
                    );
                }
            }

            if (isPlayingLoop && loopParticles != null) {
                float progress = (loopTimer % LOOP_DURATION) / LOOP_DURATION;
                for (int i = 0; i < loopParticles.length; i += 4) {
                    float ghostProgress = progress < 0.99f ? progress * LOOP_GHOST_SPEED : 1.0f;
                    loopParticles[i] = loopStartPoints[i] + (loopEndPoints[i] - loopStartPoints[i]) * ghostProgress;
                    loopParticles[i + 1] = loopStartPoints[i + 1] + (loopEndPoints[i + 1] - loopStartPoints[i + 1]) * ghostProgress;
                }
                
                g2d.setColor(new Color(255, 255, 255, 150));
                for (int i = 0; i < loopParticles.length; i += 4) {
                    g2d.fillOval(
                        (int)(loopParticles[i] - particleSize/2),
                        (int)(loopParticles[i + 1] - particleSize/2),
                        particleSize, particleSize
                    );
                }
            }

            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            String text = isRecordingLoop ? 
                String.format("ЗАПИСЬ ПЕТЛИ (%.1f сек)", LOOP_DURATION - loopDisplayTimer) :
                (isPlayingLoop ? "ВОСПРОИЗВЕДЕНИЕ ПЕТЛИ" : "ПЕТЛЯ ЗАФИКСИРОВАНА");
            g2d.setColor(LOOP_COLOR);
            g2d.drawString(text, 20, 35);
        }
    }

    @Override
    public void onSimulationUpdated(SimulationState state) {
        if (!physicsEnabled) {
            return;
        }
        
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0f;
        lastUpdateTime = currentTime;
        
        if (state.hasParticles()) {
            particles = state.getParticles();
            particleTemperatures = state.getTemperatures();
        }
        
        repaint();
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
        int[] newColors = new int[particleColors.length + newParticles];
        int[] newMaterials = new int[particleMaterials.length + newParticles];
        
        System.arraycopy(particles, 0, newArray, 0, particles.length);
        System.arraycopy(particleTemperatures, 0, newTemperatures, 0, particleTemperatures.length);
        System.arraycopy(particleColors, 0, newColors, 0, particleColors.length);
        System.arraycopy(particleMaterials, 0, newMaterials, 0, particleMaterials.length);
        
        int currentColor = currentMaterial != null ? 
            currentMaterial.getColor() : SimulationConstants.PARTICLE_COLOR.getRGB();
            
        for(int i = 0; i < newParticles; i++) {
            int idx = particles.length / 4 + i;
            int pidx = particles.length + i * 4;
            
            double angle = Math.random() * 2 * Math.PI;
            double radius = Math.random() * spawnRadius;
            
            newArray[pidx] = position.x + (float)(Math.cos(angle) * radius);
            newArray[pidx + 1] = position.y + (float)(Math.sin(angle) * radius);
            newArray[pidx + 2] = 0;
            newArray[pidx + 3] = 0;
            
            newTemperatures[particleTemperatures.length + i] = SimulationConstants.INITIAL_TEMPERATURE;
            newColors[idx] = currentColor;
            newMaterials[idx] = currentMaterialIndex;
        }
        
        particles = newArray;
        particleTemperatures = newTemperatures;
        particleColors = newColors;
        particleMaterials = newMaterials;
        gpuCalculator.updateWorkSize(particles.length / 4);
    }

    public void clearParticles() {
        particles = new float[0];
        particleTemperatures = new float[0];
        particleColors = new int[0];
        particleMaterials = new int[0];
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

    public void setMaterial(Material material) {
        this.currentMaterial = material;
        this.currentMaterialIndex = getMaterialIndex(material);
        gpuCalculator.setDensity(material.getDensity());
        this.particleImage = createParticleImage();
        repaint();
    }

    private int getMaterialIndex(Material material) {
        if (material instanceof Water) return 0;
        if (material instanceof Oil) return 1;
        if (material instanceof Mercury) return 2;
        if (material instanceof Gasoline) return 3;
        if (material instanceof Glycerin) return 4;
        return 0;
    }

    private void initializeStars() {
        if (stars == null) {
            stars = new float[NUM_STARS][3];
            for (int i = 0; i < NUM_STARS; i++) {
                stars[i][0] = random.nextFloat() * getWidth();
                stars[i][1] = random.nextFloat() * getHeight();
                stars[i][2] = random.nextFloat();
            }
        }
    }

    private void initializeCracks() {
        cracks.clear();
        int screenDivisions = 4;
        int secWidth = getWidth() / screenDivisions;
        int secHeight = getHeight() / screenDivisions;
        
        for (int i = 0; i < NUM_CRACKS; i++) {
            List<float[]> crackPoints = new ArrayList<>();
            
            int secX = random.nextInt(screenDivisions);
            int secY = random.nextInt(screenDivisions);
            float startX = secX * secWidth + random.nextFloat() * secWidth;
            float startY = secY * secHeight + random.nextFloat() * secHeight;
            
            float angle = random.nextFloat() * 2 * (float)Math.PI;
            float length = 30 + random.nextFloat() * 100;
            float width = 1 + random.nextFloat() * 2;
            
            crackPoints.add(new float[]{startX, startY, angle, length, width});
            
            for (int j = 1; j < NUM_CRACK_POINTS; j++) {
                float progress = j / (float)(NUM_CRACK_POINTS - 1);
                float x = startX + (float)Math.cos(angle) * (length * progress);
                float y = startY + (float)Math.sin(angle) * (length * progress);
                
                x += (random.nextFloat() - 0.5f) * 20;
                y += (random.nextFloat() - 0.5f) * 20;
                
                angle += (random.nextFloat() - 0.5f) * 0.5f;
                width *= 0.9f + random.nextFloat() * 0.2f;
                
                crackPoints.add(new float[]{x, y, angle, length * (1 - progress), width});
            }
            
            cracks.add(crackPoints);
        }
    }

    private BufferedImage getMotionBlurBuffer() {
        if (motionBlurBuffer == null || 
            motionBlurBuffer.getWidth() != getWidth() || 
            motionBlurBuffer.getHeight() != getHeight()) {
            
            motionBlurBuffer = new BufferedImage(
                Math.max(1, getWidth()), 
                Math.max(1, getHeight()), 
                BufferedImage.TYPE_INT_ARGB
            );
        }
        return motionBlurBuffer;
    }

    public void setMaxTimeAcceleration(double value) {
        MAX_TIME_ACCELERATION = (float)value;
    }
    
    public void setLoopDuration(double value) {
        LOOP_DURATION = (float)value;
    }
    
    public void setGhostSpeed(double value) {
        LOOP_GHOST_SPEED = (float)value;
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
        particleColors = new int[cols * rows];
        particleMaterials = new int[cols * rows];
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int idx = (i * cols + j);
                int pidx = idx * 4;
                particles[pidx] = margin + j * SimulationConstants.PARTICLE_SPACING;
                particles[pidx + 1] = margin + i * SimulationConstants.PARTICLE_SPACING;
                particles[pidx + 2] = 0;
                particles[pidx + 3] = 0;
                particleColors[idx] = currentMaterial != null ? 
                    currentMaterial.getColor() : SimulationConstants.PARTICLE_COLOR.getRGB();
                particleMaterials[idx] = 0;
            }
        }
    }

    private BufferedImage createParticleImage() {
        BufferedImage img = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        Color color = currentMaterial != null ? new Color(currentMaterial.getColor()) : SimulationConstants.PARTICLE_COLOR;
        g2.setColor(color);
        
        g2.fillOval(0, 0, imageSize-1, imageSize-1);
        g2.dispose();
        return img;
    }

    private void updateParticlePhysics() {
        var species = FloatVector.SPECIES_256;
        int vectorSize = species.length();
        
        for (int i = 0; i < particles.length; i += vectorSize * 4) {
            var posX = FloatVector.fromArray(species, particles, i);
            var posY = FloatVector.fromArray(species, particles, i + vectorSize);
            var velX = FloatVector.fromArray(species, particles, i + vectorSize * 2);
            var velY = FloatVector.fromArray(species, particles, i + vectorSize * 3);
            
            velX = velX.mul(0.99f);
            velY = velY.mul(0.99f).add(9.81f);
            
            posX = posX.add(velX.mul(deltaTime));
            posY = posY.add(velY.mul(deltaTime));
            
            posX.intoArray(particles, i);
            posY.intoArray(particles, i + vectorSize);
            velX.intoArray(particles, i + vectorSize * 2);
            velY.intoArray(particles, i + vectorSize * 3);
        }
    }

    private void handleMouseEvent(MouseEvent e) {
        mousePosition = new Point(e.getX(), e.getY());
        
        if (e.getButton() == MouseEvent.BUTTON1) {
            mouseForce = -currentMouseForce;
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            mouseForce = currentMouseForce;
        }
    }

    private Color getMaterialColor(Material material) {
        if (material == null) return SimulationConstants.PARTICLE_COLOR;
        if (material instanceof Water) return new Color(0, 119, 190);
        if (material instanceof Oil) return new Color(139, 69, 19);
        if (material instanceof Mercury) return new Color(192, 192, 192);
        if (material instanceof Gasoline) return new Color(255, 222, 173);
        if (material instanceof Glycerin) return new Color(230, 230, 250);
        return SimulationConstants.PARTICLE_COLOR;
    }
} 