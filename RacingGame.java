import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Main class
public class RacingGame extends JFrame {
    private GamePanel gamePanel;
    
    public RacingGame() {
        setTitle("Extreme Racing Championship");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        gamePanel = new GamePanel();
        add(gamePanel);
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        
        gamePanel.startGame();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RacingGame());
    }
}

// Game Panel with all game logic and rendering
class GamePanel extends JPanel implements ActionListener, KeyListener {
    // Screen dimensions
    private static final int SCREEN_WIDTH = 1000;
    private static final int SCREEN_HEIGHT = 700;
    
    // Road properties
    private static final int ROAD_WIDTH = 600;
    private static final int ROAD_X = (SCREEN_WIDTH - ROAD_WIDTH) / 2;
    private static final int LANE_COUNT = 8;
    private static final int LANE_WIDTH = ROAD_WIDTH / LANE_COUNT;
    
    // Game objects
    private PlayerCar playerCar;
    private List<OpponentCar> opponentCars;
    private List<RoadMarking> roadMarkings;
    private List<Particle> particles;
    private List<PowerUp> powerUps;
    
    // Game state
    private Timer gameTimer;
    private boolean gameRunning = false;
    private boolean gameOver = false;
    private int score = 0;
    private int speed = 0;
    private int maxSpeed = 223;
    private int acceleration = 2;
    private int brakingForce = 3;
    private int naturalDeceleration = 1;
    private int difficulty = 1;
    private int frameCount = 0;
    private long lastPowerUpTime = 0;
    
    // Player stats
    private int nitroFuel = 100;
    private int lives = 3;
    private boolean hasShield = false;
    private int shieldDuration = 0;
    
    // Input handling
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean nitroPressed = false;
    
    // Visual effects
    private float cameraShake = 0;
    private Random random = new Random();
    
    // Road scroll
    private int roadOffset = 0;
    
    // Colors
    private static final Color GRASS_COLOR = new Color(34, 139, 34);
    private static final Color GRASS_DARK = new Color(25, 100, 25);
    private static final Color ROAD_COLOR = new Color(60, 60, 60);
    private static final Color ROAD_MARKING_COLOR = new Color(255, 255, 255, 200);
    private static final Color SHOULDER_COLOR = new Color(200, 200, 200);
    
    public GamePanel() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        
        initializeGame();
    }
    
    private void initializeGame() {
        playerCar = new PlayerCar(SCREEN_WIDTH / 2 - 25, SCREEN_HEIGHT - 150);
        opponentCars = new ArrayList<>();
        roadMarkings = new ArrayList<>();
        particles = new ArrayList<>();
        powerUps = new ArrayList<>();
        
        // Initialize road markings
        for (int i = 0; i < 20; i++) {
            roadMarkings.add(new RoadMarking(i * 40));
        }
        
        gameTimer = new Timer(16, this); // ~60 FPS
    }
    
    public void startGame() {
        gameRunning = true;
        gameOver = false;
        score = 0;
        speed = 0;
        lives = 3;
        nitroFuel = 100;
        difficulty = 1;
        gameTimer.start();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning) return;
        
        update();
        repaint();
    }
    
    private void update() {
        frameCount++;
        
        // Camera shake decay
        if (cameraShake > 0) {
            cameraShake *= 0.9f;
            if (cameraShake < 0.1f) cameraShake = 0;
        }

        boolean ifSpeed = false;
        if (nitroPressed && nitroFuel > 0){
            ifSpeed = true;
        }

        // Slow down if not on nitro
        if (speed > maxSpeed && !ifSpeed ) {
            speed -= 2;
        }
        
        // Update speed
        if (upPressed && speed < maxSpeed) {
            speed += acceleration;
        }
        if (downPressed && speed > 0) {
            speed -= brakingForce;
            if (speed < 0) speed = 0;
        }
        if (!upPressed && !downPressed && speed > 0) {
            speed -= naturalDeceleration;
            if (speed < 0) speed = 0;
        }
        
        // Nitro boost
        if (nitroPressed && nitroFuel > 0 && speed < maxSpeed * 3) {
            speed += 2;
            nitroFuel--;
            createExhaustParticles();
        } else {
            if (nitroFuel < 100 && !nitroPressed) {
                nitroFuel = Math.min(100, nitroFuel + 1);
            }
        }
        
        // Update road scroll
        roadOffset = (roadOffset + speed / 10) % 40;
        
        // Update road markings
        for (RoadMarking marking : roadMarkings) {
            marking.y = (marking.y + speed / 10) % (SCREEN_HEIGHT + 40);
        }
        
        // Update player
        if (leftPressed) {
            playerCar.moveLeft(5, ROAD_X + 10);
        }
        if (rightPressed) {
            playerCar.moveRight(5, ROAD_X + ROAD_WIDTH - 10);
        }
        
        // Update opponent cars
        updateOpponents();
        
        // Update power-ups
        updatePowerUps();
        
        // Update particles
        updateParticles();
        
        // Check collisions
        checkCollisions();
        
        // Update shield
        if (hasShield) {
            shieldDuration--;
            if (shieldDuration <= 0) {
                hasShield = false;
            }
        }
        
        // Spawn opponents
        if (frameCount % Math.max(30, 100 - difficulty * 5) == 0) {
            spawnOpponent();
        }
        
        // Spawn power-ups
        if (System.currentTimeMillis() - lastPowerUpTime > 5000 + random.nextInt(5000)) {
            spawnPowerUp();
            lastPowerUpTime = System.currentTimeMillis();
        }
        
        // Increase difficulty
        if (frameCount % 300 == 0) { // Every 5 seconds
            difficulty = Math.min(20, difficulty + 1);
        }
        
        // Update score
        if (speed > 0) {
            score += speed / 10;
        }
        
        // Speed exhaust particles
        if (speed > 50 && frameCount % 5 == 0) {
            createExhaustParticles();
        }
    }
    
    private void updateOpponents() {
        for (int i = opponentCars.size() - 1; i >= 0; i--) {
            OpponentCar car = opponentCars.get(i);
            car.y += (speed / 10 + car.baseSpeed);
            
            // AI movement
            if (frameCount % 30 == 0 && random.nextBoolean()) {
                car.aiMove(LANE_WIDTH, ROAD_X);
            }
            
            if (car.y > SCREEN_HEIGHT + 100) {
                opponentCars.remove(i);
            }
        }
    }
    
    private void updatePowerUps() {
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp powerUp = powerUps.get(i);
            powerUp.y += speed / 8;
            powerUp.rotation += 0.05;
            
            if (powerUp.y > SCREEN_HEIGHT + 50) {
                powerUps.remove(i);
            }
        }
    }
    
    private void updateParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.life <= 0) {
                particles.remove(i);
            }
        }
    }
    
    private void spawnOpponent() {
        int lane = random.nextInt(LANE_COUNT);
        int x = ROAD_X + lane * LANE_WIDTH + LANE_WIDTH / 2;
        opponentCars.add(new OpponentCar(x - 25, -100, lane));
    }
    
    private void spawnPowerUp() {
        int lane = random.nextInt(LANE_COUNT);
        int x = ROAD_X + lane * LANE_WIDTH + LANE_WIDTH / 2;
        int type = random.nextInt(3); // 0: Nitro, 1: Shield, 2: Extra Life
        powerUps.add(new PowerUp(x - 15, -30, type));
    }
    
    private void createExhaustParticles() {
        for (int i = 0; i < 3; i++) {
            double angle = Math.PI / 2 + (random.nextDouble() - 0.5) * 0.5;
            particles.add(new Particle(
                playerCar.x + playerCar.width / 2 + (random.nextDouble() - 0.5) * 20,
                playerCar.y + playerCar.height,
                Math.cos(angle) * random.nextDouble() * 2,
                Math.sin(angle) * random.nextDouble() * 3 + 2,
                new Color(255, random.nextInt(100) + 100, 0, 200),
                30 + random.nextInt(20)
            ));
        }
    }
    
    private void checkCollisions() {
        Rectangle playerBounds = playerCar.getBounds();
        
        // Check opponent collisions
        for (int i = opponentCars.size() - 1; i >= 0; i--) {
            OpponentCar opponent = opponentCars.get(i);
            if (playerBounds.intersects(opponent.getBounds())) {
                if (hasShield) {
                    // Shield absorbs collision
                    opponentCars.remove(i);
                    hasShield = false;
                    shieldDuration = 0;
                    createExplosion(opponent.x + opponent.width/2, opponent.y + opponent.height/2);
                    cameraShake = 10;
                } else {
                    // Crash
                    lives--;
                    createExplosion(playerCar.x + playerCar.width/2, playerCar.y + playerCar.height/2);
                    cameraShake = 20;
                    opponentCars.remove(i);
                    
                    if (lives <= 0) {
                        gameOver = true;
                        gameRunning = false;
                        gameTimer.stop();
                    }
                }
                break;
            }
        }
        
        // Check power-up collisions
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp powerUp = powerUps.get(i);
            if (playerBounds.intersects(powerUp.getBounds())) {
                applyPowerUp(powerUp.type);
                powerUps.remove(i);
            }
        }
    }
    
    private void applyPowerUp(int type) {
        switch (type) {
            case 0: // Nitro
                nitroFuel = 100;
                break;
            case 1: // Shield
                hasShield = true;
                shieldDuration = 300; // 5 seconds at 60 FPS
                break;
            case 2: // Extra Life
                lives = Math.min(5, lives + 1);
                break;
        }
    }
    
    private void createExplosion(double x, double y) {
        for (int i = 0; i < 30; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = random.nextDouble() * 5 + 2;
            particles.add(new Particle(
                x, y,
                Math.cos(angle) * speed,
                Math.sin(angle) * speed,
                new Color(255, random.nextInt(150) + 100, 0, 200),
                40 + random.nextInt(30)
            ));
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Camera shake
        int shakeX = 0, shakeY = 0;
        if (cameraShake > 0) {
            shakeX = (int)(random.nextGaussian() * cameraShake);
            shakeY = (int)(random.nextGaussian() * cameraShake);
        }
        
        g2d.translate(shakeX, shakeY);
        
        // Draw environment
        drawBackground(g2d);
        drawRoad(g2d);
        drawRoadMarkings(g2d);
        
        // Draw power-ups
        for (PowerUp powerUp : powerUps) {
            powerUp.draw(g2d);
        }
        
        // Draw opponent cars
        for (OpponentCar car : opponentCars) {
            car.draw(g2d);
        }
        
        // Draw player car
        playerCar.draw(g2d, hasShield);
        
        // Draw particles
        for (Particle particle : particles) {
            particle.draw(g2d);
        }
        
        g2d.translate(-shakeX, -shakeY);
        
        // Draw HUD
        drawHUD(g2d);
        
        // Draw game over screen
        if (gameOver) {
            drawGameOver(g2d);
        }
    }
    
    private void drawBackground(Graphics2D g2d) {
        // Sky gradient
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(25, 25, 112), 
                                                       0, SCREEN_HEIGHT, new Color(70, 130, 180));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        
        // Grass
        g2d.setColor(GRASS_COLOR);
        g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        
        // Grass texture
        g2d.setColor(GRASS_DARK);
        for (int i = 0; i < SCREEN_WIDTH; i += 30) {
            for (int j = 0; j < SCREEN_HEIGHT; j += 30) {
                if ((i / 30 + j / 30) % 2 == 0) {
                    g2d.fillRect(i, j, 30, 30);
                }
            }
        }
    }
    
    private void drawRoad(Graphics2D g2d) {
        // Road
        g2d.setColor(ROAD_COLOR);
        g2d.fillRect(ROAD_X, 0, ROAD_WIDTH, SCREEN_HEIGHT);
        
        // Road shoulders
        g2d.setColor(SHOULDER_COLOR);
        g2d.fillRect(ROAD_X - 5, 0, 5, SCREEN_HEIGHT);
        g2d.fillRect(ROAD_X + ROAD_WIDTH, 0, 5, SCREEN_HEIGHT);
        
        // Road texture
        g2d.setColor(new Color(50, 50, 50));
        for (int i = 0; i < SCREEN_HEIGHT; i += 20) {
            for (int j = 0; j < ROAD_WIDTH; j += 40) {
                if ((i / 20 + j / 40) % 3 == 0) {
                    g2d.fillRect(ROAD_X + j, i, 2, 10);
                }
            }
        }
    }
    
    private void drawRoadMarkings(Graphics2D g2d) {
        // Lane markings
        g2d.setColor(ROAD_MARKING_COLOR);
        for (RoadMarking marking : roadMarkings) {
            // Dashed lane lines
            for (int i = 1; i < LANE_COUNT; i++) {
                int x = ROAD_X + i * LANE_WIDTH;
                g2d.fillRect(x - 2, (int)(marking.y + roadOffset) % SCREEN_HEIGHT, 4, 20);
            }
        }
        
        // Solid edge lines
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(ROAD_X + 10, 0, 3, SCREEN_HEIGHT);
        g2d.fillRect(ROAD_X + ROAD_WIDTH - 13, 0, 3, SCREEN_HEIGHT);
    }
    
    private void drawHUD(Graphics2D g2d) {
        // Semi-transparent panel
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 250, 120, 15, 15);
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(10, 10, 250, 120, 15, 15);
        
        // Score
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Score: " + score, 30, 45);
        
        // Speed
        g2d.drawString("Speed: " + speed + " km/h", 30, 75);
        
        // Lives
        g2d.drawString("Lives: ", 30, 105);
        for (int i = 0; i < lives; i++) {
            g2d.setColor(Color.RED);
            g2d.fillOval(95 + i * 25, 90, 15, 15);
            g2d.setColor(Color.WHITE);
            g2d.drawOval(95 + i * 25, 90, 15, 15);
        }
        
        // Nitro gauge
        int nitroX = SCREEN_WIDTH - 160;
        int nitroY = 20;
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(nitroX - 5, nitroY - 5, 110, 30, 10, 10);
        
        g2d.setColor(Color.GRAY);
        g2d.fillRect(nitroX, nitroY, 100, 20);
        
        Color nitroColor;
        if (nitroFuel > 50) nitroColor = Color.BLUE;
        else if (nitroFuel > 25) nitroColor = Color.YELLOW;
        else nitroColor = Color.RED;
        
        g2d.setColor(nitroColor);
        g2d.fillRect(nitroX, nitroY, nitroFuel, 20);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("NITRO", nitroX + 25, nitroY + 15);
        
        // Shield indicator
        if (hasShield) {
            g2d.setColor(new Color(0, 255, 255, 180));
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString("SHIELD ACTIVE", SCREEN_WIDTH - 200, SCREEN_HEIGHT - 20);
        }
    }
    
    private void drawGameOver(Graphics2D g2d) {
        // Overlay
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        
        // Game Over text
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        g2d.setColor(Color.RED);
        String text = "GAME OVER";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (SCREEN_WIDTH - fm.stringWidth(text)) / 2;
        g2d.drawString(text, textX, SCREEN_HEIGHT / 2 - 50);
        
        // Final score
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.setColor(Color.WHITE);
        String scoreText = "Final Score: " + score;
        fm = g2d.getFontMetrics();
        textX = (SCREEN_WIDTH - fm.stringWidth(scoreText)) / 2;
        g2d.drawString(scoreText, textX, SCREEN_HEIGHT / 2 + 20);
        
        // Restart instruction
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.setColor(Color.YELLOW);
        String restartText = "Press ENTER to restart";
        fm = g2d.getFontMetrics();
        textX = (SCREEN_WIDTH - fm.stringWidth(restartText)) / 2;
        g2d.drawString(restartText, textX, SCREEN_HEIGHT / 2 + 70);
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                leftPressed = true;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                rightPressed = true;
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                upPressed = true;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                downPressed = true;
                break;
            case KeyEvent.VK_SHIFT:
            case KeyEvent.VK_SPACE:
                nitroPressed = true;
                break;
            case KeyEvent.VK_ENTER:
                if (gameOver) {
                    initializeGame();
                    startGame();
                }
                break;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                leftPressed = false;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                rightPressed = false;
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                upPressed = false;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                downPressed = false;
                break;
            case KeyEvent.VK_SHIFT:
            case KeyEvent.VK_SPACE:
                nitroPressed = false;
                break;
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
}

// Player car class
class PlayerCar {
    double x, y;
    int width = 70;
    int height = 90;
    Color mainColor = new Color(220, 20, 60); // Crimson
    Color accentColor = new Color(255, 215, 0); // Gold
    
    public PlayerCar(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public void moveLeft(double amount, int minX) {
        x = Math.max(minX, x - amount);
    }
    
    public void moveRight(double amount, int maxX) {
        x = Math.min(maxX - width, x + amount);
    }
    
    public Rectangle getBounds() {
        return new Rectangle((int)x + 5, (int)y + 5, width - 10, height - 10);
    }
    
    public void draw(Graphics2D g2d, boolean hasShield) {
        // Shadow
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRoundRect((int)x + 3, (int)y + 3, width, height, 10, 10);
        
        // Car body
        g2d.setColor(mainColor);
        g2d.fillRoundRect((int)x, (int)y, width, height, 10, 10);
        
        // Windshield
        g2d.setColor(new Color(135, 206, 235, 180));
        int[] windshieldX = {(int)x + 12, (int)x + 22, (int)x + width - 22, (int)x + width - 12};
        int[] windshieldY = {(int)y + 15, (int)y + 5, (int)y + 5, (int)y + 15};
        g2d.fillPolygon(windshieldX, windshieldY, 4);
        
        // Racing stripes
        g2d.setColor(accentColor);
        g2d.fillRect((int)x + 10, (int)y + 20, 5, height - 30);
        g2d.fillRect((int)x + width - 15, (int)y + 20, 5, height - 30);
        
        // Headlights
        g2d.setColor(Color.YELLOW);
        g2d.fillOval((int)x + 5, (int)y + 2, 10, 8);
        g2d.fillOval((int)x + width - 15, (int)y + 2, 10, 8);
        
        // Taillights
        g2d.setColor(Color.RED);
        g2d.fillOval((int)x + 5, (int)y + height - 10, 10, 8);
        g2d.fillOval((int)x + width - 15, (int)y + height - 10, 10, 8);
        
        // Wheels
        g2d.setColor(Color.BLACK);
        g2d.fillRoundRect((int)x - 3, (int)y + 10, 8, 18, 4, 4);
        g2d.fillRoundRect((int)x + width - 5, (int)y + 10, 8, 18, 4, 4);
        g2d.fillRoundRect((int)x - 3, (int)y + height - 28, 8, 18, 4, 4);
        g2d.fillRoundRect((int)x + width - 5, (int)y + height - 28, 8, 18, 4, 4);
        
        // Shield effect
        if (hasShield) {
            g2d.setColor(new Color(0, 255, 255, 100));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval((int)x - 5, (int)y - 5, width + 10, height + 10);
        }
    }
}

// Opponent car class
class OpponentCar {
    double x, y;
    int width = 70;
    int height = 90;
    int lane;
    double baseSpeed;
    Color color;
    Random random = new Random();
    
    static Color[] colors = {
        new Color(0, 100, 200),    // Blue
        new Color(200, 100, 0),    // Orange
        new Color(100, 200, 0),    // Green
        new Color(200, 0, 200),    // Purple
        new Color(200, 200, 0),    // Yellow
    };
    
    public OpponentCar(double x, double y, int lane) {
        this.x = x;
        this.y = y;
        this.lane = lane;
        this.baseSpeed = random.nextDouble() * 3 + 1;
        this.color = colors[random.nextInt(colors.length)];
    }
    
    public void aiMove(int laneWidth, int roadX) {
        int targetLane = random.nextInt(3);
        double targetX = roadX + targetLane * laneWidth + laneWidth / 2 - width / 2;
        
        if (Math.abs(targetX - x) > 5) {
            x += Math.signum(targetX - x) * 2;
        }
    }
    
    public Rectangle getBounds() {
        return new Rectangle((int)x + 5, (int)y + 5, width - 10, height - 10);
    }
    
    public void draw(Graphics2D g2d) {
        // Shadow
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRoundRect((int)x + 3, (int)y + 3, width, height, 10, 10);
        
        // Car body
        g2d.setColor(color);
        g2d.fillRoundRect((int)x, (int)y, width, height, 10, 10);
        
        // Windshield
        g2d.setColor(new Color(135, 206, 235, 180));
        g2d.fillRect((int)x + 15, (int)y + 8, width - 30, 8);
        
        // Racing stripes
        g2d.setColor(Color.WHITE);
        g2d.fillRect((int)x + 15, (int)y + 25, 4, height - 40);
        g2d.fillRect((int)x + width - 19, (int)y + 25, 4, height - 40);
        
        // Headlights
        g2d.setColor(Color.YELLOW);
        g2d.fillOval((int)x + 5, (int)y + 2, 10, 8);
        g2d.fillOval((int)x + width - 15, (int)y + 2, 10, 8);
        
        // Taillights
        g2d.setColor(Color.RED);
        g2d.fillOval((int)x + 5, (int)y + height - 10, 10, 8);
        g2d.fillOval((int)x + width - 15, (int)y + height - 10, 10, 8);
        
        // Wheels
        g2d.setColor(Color.BLACK);
        g2d.fillRoundRect((int)x - 3, (int)y + 10, 8, 18, 4, 4);
        g2d.fillRoundRect((int)x + width - 5, (int)y + 10, 8, 18, 4, 4);
        g2d.fillRoundRect((int)x - 3, (int)y + height - 28, 8, 18, 4, 4);
        g2d.fillRoundRect((int)x + width - 5, (int)y + height - 28, 8, 18, 4, 4);
    }
}

// Road marking class
class RoadMarking {
    double y;
    
    public RoadMarking(double y) {
        this.y = y;
    }
}

// Particle class for visual effects
class Particle {
    double x, y;
    double vx, vy;
    Color color;
    int life;
    int maxLife;
    double size;
    
    public Particle(double x, double y, double vx, double vy, Color color, int life) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.life = life;
        this.maxLife = life;
        this.size = 2 + Math.random() * 3;
    }
    
    public void update() {
        x += vx;
        y += vy;
        life--;
    }
    
    public void draw(Graphics2D g2d) {
        float alpha = (float)life / maxLife;
        g2d.setColor(new Color(
            color.getRed(),
            color.getGreen(),
            color.getBlue(),
            (int)(color.getAlpha() * alpha)
        ));
        g2d.fillOval((int)x, (int)y, (int)size, (int)size);
    }
}

// PowerUp class
class PowerUp {
    double x, y;
    int type; // 0: Nitro, 1: Shield, 2: Extra Life
    double rotation = 0;
    int width = 30;
    int height = 30;
    
    static Color[] colors = {
        new Color(0, 150, 255),  // Blue for Nitro
        new Color(0, 255, 255),  // Cyan for Shield
        new Color(255, 50, 50),  // Red for Life
    };
    
    static String[] labels = {"N", "S", "♥"};
    
    public PowerUp(double x, double y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
    
    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, width, height);
    }
    
    public void draw(Graphics2D g2d) {
        Graphics2D g2dCopy = (Graphics2D) g2d.create();
        g2dCopy.translate(x + width/2, y + height/2);
        g2dCopy.rotate(rotation);
        
        // Glow effect
        g2dCopy.setColor(new Color(colors[type].getRed(), colors[type].getGreen(), 
                                   colors[type].getBlue(), 100));
        g2dCopy.fillOval(-width/2 - 5, -height/2 - 5, width + 10, height + 10);
        
        // Power-up box
        g2dCopy.setColor(colors[type]);
        g2dCopy.fillRoundRect(-width/2, -height/2, width, height, 8, 8);
        
        // Border
        g2dCopy.setColor(Color.WHITE);
        g2dCopy.setStroke(new BasicStroke(2));
        g2dCopy.drawRoundRect(-width/2, -height/2, width, height, 8, 8);
        
        // Label
        g2dCopy.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fm = g2dCopy.getFontMetrics();
        int textWidth = fm.stringWidth(labels[type]);
        g2dCopy.drawString(labels[type], -textWidth/2, 6);
        
        g2dCopy.dispose();
    }
}