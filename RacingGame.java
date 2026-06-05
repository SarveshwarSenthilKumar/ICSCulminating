import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RacingGame extends JFrame {
    private GamePanel gamePanel;

    public RacingGame(String username, String scoresFile) { 

        setTitle("Cannonball Run: Highway Chase");
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
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int SCREEN_WIDTH = 1000;
    private static final int SCREEN_HEIGHT = 700;
    private static final int ROAD_WIDTH = 600;
    private static final int ROAD_X = (SCREEN_WIDTH - ROAD_WIDTH) / 2;
    private static final int LANE_COUNT = 8;
    private static final int LANE_WIDTH = ROAD_WIDTH / LANE_COUNT;
    
    private PlayerCar playerCar;
    private List<OpponentCar> opponentCars;
    private List<RoadMarking> roadMarkings;
    private List<Particle> particles;
    private List<PowerUp> powerUps;
    private List<SkidMark> skidMarks;

    private Timer gameTimer;
    private boolean gameRunning = false;
    private boolean gameOver = false;
    private boolean paused = false;

    private String username;
    private String scoresFile = "scores.txt";

    private int score = 0;
    private int speed = 62;
    private int maxSpeed = 223;
    private int acceleration = 2;
    private int brakingForce = 3;
    private double naturalDeceleration = 0.2;
    private int difficulty = 1;
    private int frameCount = 0;
    private long lastPowerUpTime = 0;
    
    private int nitroFuel = 120;
    private int lives = 3;
    private boolean hasShield = false;
    private int shieldDuration = 0;
    
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean nitroPressed = false;
    
    private float cameraShake = 0;
    private Random random = new Random();
    private int roadOffset = 0;
    
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
        skidMarks = new ArrayList<>();
        
        for (int i = 0; i < 20; i++) {
            roadMarkings.add(new RoadMarking(i * 40));
        }
        
        gameTimer = new Timer(16, this);
    }
    
    public void startGame() {
        gameRunning = true;
        gameOver = false;
        score = 0;
        speed = 62;
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
        
        if (cameraShake > 0) {
            cameraShake *= 0.9f;
            if (cameraShake < 0.1f) cameraShake = 0;
        }

        boolean ifSpeed = nitroPressed && nitroFuel > 0;

        if (speed > maxSpeed && !ifSpeed) {
            speed -= 2;
        }
        
        if (upPressed && speed < maxSpeed) {
            speed += acceleration;
        }
        if (downPressed && speed > 0) {
            speed -= brakingForce;
            if (speed < 62) speed = 62;
        }
        if (!upPressed && !downPressed && speed > 0) {
            speed -= naturalDeceleration;
            if (speed < 62) speed = 62;
        }
        
        if (nitroPressed && nitroFuel > 0 && speed < maxSpeed * 3) {
            speed += 2;
            nitroFuel--;
            createExhaustParticles();
        } else {
            if (nitroFuel < 120 && !nitroPressed) {
                nitroFuel = Math.min(100, nitroFuel + 1);
            }
        }
        
        roadOffset = (roadOffset + speed / 10) % 40;
        
        for (RoadMarking marking : roadMarkings) {
            marking.y = (marking.y + speed / 2) % (SCREEN_HEIGHT + 40);
        }
        
        double lateralForce = Math.max(2.5, 5.0 - speed / 100.0);
        if (leftPressed) {
            playerCar.velocityX = Math.max(-8, playerCar.velocityX - lateralForce * 0.35);
        } else if (rightPressed) {
            playerCar.velocityX = Math.min(8, playerCar.velocityX + lateralForce * 0.35);
        } else {
            playerCar.velocityX *= 0.78;
            if (Math.abs(playerCar.velocityX) < 0.1) playerCar.velocityX = 0;
        }
        playerCar.moveHorizontal(playerCar.velocityX, ROAD_X + 10, ROAD_X + ROAD_WIDTH - 10);

        if (leftPressed) {
            playerCar.tiltAngle = Math.max(-8, playerCar.tiltAngle - 0.7);
        } else if (rightPressed) {
            playerCar.tiltAngle = Math.min(8, playerCar.tiltAngle + 0.7);
        } else {
            playerCar.tiltAngle *= 0.88;
            if (Math.abs(playerCar.tiltAngle) < 0.1) playerCar.tiltAngle = 0;
        }

        if ((downPressed && speed > 40) || ((leftPressed || rightPressed) && speed > 70)) {
            if (frameCount % 3 == 0) {
                double rearY = playerCar.y + playerCar.height - 15;
                double cx = playerCar.x + playerCar.width / 2.0;
                skidMarks.add(new SkidMark(cx - 12, rearY, playerCar.tiltAngle));
                skidMarks.add(new SkidMark(cx + 12, rearY, playerCar.tiltAngle));
            }
        }

        for (int i = skidMarks.size() - 1; i >= 0; i--) {
            SkidMark s = skidMarks.get(i);
            s.update(speed);
            if (s.isDead()) skidMarks.remove(i);
        }

        updateOpponents();
        updatePowerUps();
        updateParticles();
        checkCollisions();
        checkOpponentCollisions();
        
        if (hasShield) {
            shieldDuration--;
            if (shieldDuration <= 0) {
                hasShield = false;
            }
        }
        
        if (frameCount % Math.max(30, 100 - difficulty * 5) == 0) {
            int opponents = 1 + (int)(Math.random() * 8);
            for (int i = 0; i < opponents; i++) {
                spawnOpponent();
            }
        }
        
        if (System.currentTimeMillis() - lastPowerUpTime > 5000 + random.nextInt(5000)) {
            spawnPowerUp();
            lastPowerUpTime = System.currentTimeMillis();
        }
        
        if (frameCount % 300 == 0) {
            difficulty = Math.min(20, difficulty + 1);
        }
        
        if (speed > 0) {
            score += speed / 10;
        }
        
        if (speed > 50 && frameCount % 5 == 0) {
            createExhaustParticles();
        }
    }
    
    private void updateOpponents() {
        for (int i = opponentCars.size() - 1; i >= 0; i--) {
            OpponentCar car = opponentCars.get(i);
            car.y += (speed / 10 + car.baseSpeed);
            
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
        int y = -100 - (int)(Math.random() * 100);
        opponentCars.add(new OpponentCar(x - 25, y, lane));
    }
    
    private void spawnPowerUp() {
        int lane = random.nextInt(LANE_COUNT);
        int x = ROAD_X + lane * LANE_WIDTH + LANE_WIDTH / 2;
        int y = random.nextInt(SCREEN_HEIGHT - 100);
        int type = random.nextInt(3);
        powerUps.add(new PowerUp(x - 15, y, type));
    }
    
    private void createExhaustParticles() {
        for (int i = 0; i < 3; i++) {
            double angle = Math.PI / 2 + (random.nextDouble() - 0.5) * 0.5;
            Color c = (nitroPressed && nitroFuel > 0)
                ? new Color(80 + random.nextInt(60), 160 + random.nextInt(60), 255, 200)
                : new Color(255, random.nextInt(100) + 100, 0, 200);
            particles.add(new Particle(
                playerCar.x + playerCar.width / 2 + (random.nextDouble() - 0.5) * 20,
                playerCar.y + playerCar.height,
                Math.cos(angle) * random.nextDouble() * 2,
                Math.sin(angle) * random.nextDouble() * 3 + 2,
                c,
                30 + random.nextInt(20)
            ));
        }
    }
    
    private void checkCollisions() {
        Rectangle playerBounds = playerCar.getBounds();
        
        for (int i = opponentCars.size() - 1; i >= 0; i--) {
            OpponentCar opponent = opponentCars.get(i);
            if (playerBounds.intersects(opponent.getBounds())) {
                if (hasShield) {
                    opponentCars.remove(i);
                    hasShield = false;
                    shieldDuration = 0;
                    createExplosion(opponent.x + opponent.width/2, opponent.y + opponent.height/2);
                    cameraShake = 10;
                } else {
                    lives--;
                    createExplosion(playerCar.x + playerCar.width/2, playerCar.y + playerCar.height/2);
                    cameraShake = 20;
                    opponentCars.remove(i);
                    
                    if (lives <= 0) {
                        gameOver = true;
                        ScoreManager.saveScore(scoresFile, username, score);
                        gameRunning = false;
                        gameTimer.stop();
                    }
                }
                break;
            }
        }
        
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
            case 0: nitroFuel = 100; break;
            case 1: hasShield = true; shieldDuration = 300; break;
            case 2: lives = Math.min(5, lives + 1); break;
        }
    }
    
    private void createExplosion(double x, double y) {
        for (int i = 0; i < 30; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double spd = random.nextDouble() * 5 + 2;
            particles.add(new Particle(
                x, y,
                Math.cos(angle) * spd,
                Math.sin(angle) * spd,
                new Color(255, random.nextInt(150) + 100, 0, 200),
                40 + random.nextInt(30)
            ));
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        int shakeX = 0, shakeY = 0;
        if (cameraShake > 0) {
            shakeX = (int)(random.nextGaussian() * cameraShake);
            shakeY = (int)(random.nextGaussian() * cameraShake);
        }
        
        g2d.translate(shakeX, shakeY);
        
        drawBackground(g2d);
        drawRoad(g2d);
        drawRoadMarkings(g2d);
        drawSpeedLines(g2d);

        for (SkidMark s : skidMarks) {
            s.draw(g2d);
        }
        
        for (PowerUp powerUp : powerUps) {
            powerUp.draw(g2d);
        }
        
        for (OpponentCar car : opponentCars) {
            car.draw(g2d);
        }
        
        playerCar.draw(g2d, hasShield);
        
        for (Particle particle : particles) {
            particle.draw(g2d);
        }
        
        g2d.translate(-shakeX, -shakeY);
        
        drawHUD(g2d);
        
        if (gameOver) {
            drawGameOver(g2d);
        }
    }
    
    private void drawBackground(Graphics2D g2d) {
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(25, 25, 112), 
                                                       0, SCREEN_HEIGHT, new Color(70, 130, 180));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        
        g2d.setColor(GRASS_COLOR);
        g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        
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
        g2d.setColor(ROAD_COLOR);
        g2d.fillRect(ROAD_X, 0, ROAD_WIDTH, SCREEN_HEIGHT);
        
        g2d.setColor(SHOULDER_COLOR);
        g2d.fillRect(ROAD_X - 5, 0, 5, SCREEN_HEIGHT);
        g2d.fillRect(ROAD_X + ROAD_WIDTH, 0, 5, SCREEN_HEIGHT);
        
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
        g2d.setColor(ROAD_MARKING_COLOR);
        for (RoadMarking marking : roadMarkings) {
            for (int i = 1; i < LANE_COUNT; i++) {
                int x = ROAD_X + i * LANE_WIDTH;
                g2d.fillRect(x - 2, (int)(marking.y + roadOffset) % SCREEN_HEIGHT, 4, 20);
            }
        }
        
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(ROAD_X + 10, 0, 3, SCREEN_HEIGHT);
        g2d.fillRect(ROAD_X + ROAD_WIDTH - 13, 0, 3, SCREEN_HEIGHT);
    }

    private void drawSpeedLines(Graphics2D g2d) {
        if (speed < 35) return;
        float t = Math.min(1.0f, (speed - 35) / 130f);
        int count = (int)(t * 22) + 4;
        g2d.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < count; i++) {
            int side = random.nextInt(2);
            int lx = side == 0
                ? random.nextInt(Math.max(1, ROAD_X - 5))
                : ROAD_X + ROAD_WIDTH + random.nextInt(Math.max(1, ROAD_X - 5));
            int ly = random.nextInt(SCREEN_HEIGHT);
            int len = (int)(t * 55) + 20;
            g2d.setColor(new Color(200, 220, 255, (int)(t * 85)));
            g2d.drawLine(lx, ly, lx, ly + len);
        }
    }
    
    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 250, 120, 15, 15);
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(10, 10, 250, 120, 15, 15);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Score: " + score, 30, 45);
        g2d.drawString("Speed: " + speed + " km/h", 30, 75);
        g2d.drawString("Lives: ", 30, 105);
        for (int i = 0; i < lives; i++) {
            g2d.setColor(Color.RED);
            g2d.fillOval(95 + i * 25, 90, 15, 15);
            g2d.setColor(Color.WHITE);
            g2d.drawOval(95 + i * 25, 90, 15, 15);
        }
        
        int nitroX = SCREEN_WIDTH - 130;
        int nitroY = 20;
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(nitroX - 5, nitroY - 5, 130, 30, 10, 10);
        
        g2d.setColor(Color.GRAY);
        g2d.fillRect(nitroX, nitroY, 120, 20);
        
        Color nitroColor;
        if (nitroFuel > 50) nitroColor = Color.BLUE;
        else if (nitroFuel > 25) nitroColor = Color.YELLOW;
        else nitroColor = Color.RED;
        
        g2d.setColor(nitroColor);
        g2d.fillRect(nitroX, nitroY, (int)(nitroFuel * 1.2), 20);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("NITRO", nitroX + 38, nitroY + 15);
        
        if (hasShield) {
            g2d.setColor(new Color(0, 255, 255, 180));
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString("SHIELD ACTIVE", SCREEN_WIDTH - 200, SCREEN_HEIGHT - 20);
        }
    }
    
    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        g2d.setColor(Color.RED);
        String text = "GAME OVER";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (SCREEN_WIDTH - fm.stringWidth(text)) / 2;
        g2d.drawString(text, textX, SCREEN_HEIGHT / 2 - 50);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.setColor(Color.WHITE);
        String scoreText = "Final Score: " + score;
        fm = g2d.getFontMetrics();
        textX = (SCREEN_WIDTH - fm.stringWidth(scoreText)) / 2;
        g2d.drawString(scoreText, textX, SCREEN_HEIGHT / 2 + 20);
        
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
            case KeyEvent.VK_P:
                if (paused) {
                    gameRunning = true;
                    gameTimer.start();
                } else {
                    gameRunning = false;
                    gameTimer.stop();
                }
                paused = !paused;
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

    private void checkOpponentCollisions() {
        for (int i = 0; i < opponentCars.size(); i++) {
            for (int j = i + 1; j < opponentCars.size(); j++) {
                OpponentCar car1 = opponentCars.get(i);
                OpponentCar car2 = opponentCars.get(j);
                if (car1.getBounds().intersects(car2.getBounds())) {
                    resolveOpponentCollision(car1, car2);
                }
            }
        }
    }

    private void resolveOpponentCollision(OpponentCar car1, OpponentCar car2) {
        double car1CenterX = car1.x + car1.width / 2;
        double car1CenterY = car1.y + car1.height / 2;
        double car2CenterX = car2.x + car2.width / 2;
        double car2CenterY = car2.y + car2.height / 2;
        
        double overlapX = Math.min(car1.x + car1.width, car2.x + car2.width) - 
                          Math.max(car1.x, car2.x);
        double overlapY = Math.min(car1.y + car1.height, car2.y + car2.height) - 
                          Math.max(car1.y, car2.y);
        
        if (overlapX < overlapY) {
            if (car1CenterX < car2CenterX) {
                car1.x -= overlapX / 2;
                car2.x += overlapX / 2;
            } else {
                car1.x += overlapX / 2;
                car2.x -= overlapX / 2;
            }
            car1.x = Math.max(ROAD_X + 5, Math.min(ROAD_X + ROAD_WIDTH - car1.width - 5, car1.x));
            car2.x = Math.max(ROAD_X + 5, Math.min(ROAD_X + ROAD_WIDTH - car2.width - 5, car2.x));
            createOpponentCollisionParticles(car1.x + car1.width/2, car1.y + car1.height/2);
        } else {
            if (car1CenterY < car2CenterY) {
                car1.y -= overlapY / 2;
                car2.y += overlapY / 2;
            } else {
                car1.y += overlapY / 2;
                car2.y -= overlapY / 2;
            }
            createOpponentCollisionParticles(car1.x + car1.width/2, car1.y + car1.height/2);
        }
        
        double tempSpeed = car1.baseSpeed;
        car1.baseSpeed = car2.baseSpeed * 0.9;
        car2.baseSpeed = tempSpeed * 0.9;
    }

    private void createOpponentCollisionParticles(double x, double y) {
        for (int i = 0; i < 15; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double spd = random.nextDouble() * 3 + 1;
            particles.add(new Particle(
                x, y,
                Math.cos(angle) * spd,
                Math.sin(angle) * spd,
                new Color(255, random.nextInt(100) + 155, 0, 200),
                20 + random.nextInt(15)
            ));
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
}

class PlayerCar {
    double x, y;
    double velocityX = 0;
    double tiltAngle = 0;
    int width = 70;
    int height = 90;
    Color mainColor = new Color(220, 20, 60);
    Color accentColor = new Color(255, 215, 0);
    
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

    public void moveHorizontal(double amount, int minX, int maxX) {
        x = Math.max(minX, Math.min(maxX - width, x + amount));
    }
    
    public Rectangle getBounds() {
        return new Rectangle((int)x + 5, (int)y + 5, width - 10, height - 10);
    }
    
    public void draw(Graphics2D g2d, boolean hasShield) {
        Graphics2D g = (Graphics2D) g2d.create();
        g.rotate(Math.toRadians(tiltAngle), x + width / 2.0, y + height / 2.0);

        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect((int)x + 3, (int)y + 3, width, height, 10, 10);
        
        g.setColor(mainColor);
        g.fillRoundRect((int)x, (int)y, width, height, 10, 10);
        
        g.setColor(new Color(135, 206, 235, 180));
        int[] windshieldX = {(int)x + 12, (int)x + 22, (int)x + width - 22, (int)x + width - 12};
        int[] windshieldY = {(int)y + 15, (int)y + 5, (int)y + 5, (int)y + 15};
        g.fillPolygon(windshieldX, windshieldY, 4);
        
        g.setColor(accentColor);
        g.fillRect((int)x + 10, (int)y + 20, 5, height - 30);
        g.fillRect((int)x + width - 15, (int)y + 20, 5, height - 30);
        
        g.setColor(Color.YELLOW);
        g.fillOval((int)x + 5, (int)y + 2, 10, 8);
        g.fillOval((int)x + width - 15, (int)y + 2, 10, 8);
        
        g.setColor(Color.RED);
        g.fillOval((int)x + 5, (int)y + height - 10, 10, 8);
        g.fillOval((int)x + width - 15, (int)y + height - 10, 10, 8);
        
        g.setColor(Color.BLACK);
        g.fillRoundRect((int)x - 3, (int)y + 10, 8, 18, 4, 4);
        g.fillRoundRect((int)x + width - 5, (int)y + 10, 8, 18, 4, 4);
        g.fillRoundRect((int)x - 3, (int)y + height - 28, 8, 18, 4, 4);
        g.fillRoundRect((int)x + width - 5, (int)y + height - 28, 8, 18, 4, 4);
        
        if (hasShield) {
            g.setColor(new Color(0, 255, 255, 100));
            g.setStroke(new BasicStroke(3));
            g.drawOval((int)x - 5, (int)y - 5, width + 10, height + 10);
        }

        g.dispose();
    }
}

class OpponentCar {
    double x, y;
    int width = 70;
    int height = 90;
    int lane;
    double baseSpeed;
    Color color;
    Random random = new Random();
    
    static Color[] colors = {
        new Color(0, 100, 200),
        new Color(200, 100, 0),
        new Color(100, 200, 0),
        new Color(200, 0, 200),
        new Color(200, 200, 0),
    };
    
    public OpponentCar(double x, double y, int lane) {
        this.x = x;
        this.y = y;
        this.lane = lane;
        this.baseSpeed = random.nextDouble() * 3 + 1;
        this.color = colors[random.nextInt(colors.length)];
    }
    
    public void aiMove(int laneWidth, int roadX) {
        int targetLane = random.nextInt(8);
        double targetX = roadX + targetLane * laneWidth + laneWidth / 2 - width / 2;
        
        if (Math.abs(targetX - x) > 5) {
            x += Math.signum(targetX - x) * 2;
        }
    }
    
    public Rectangle getBounds() {
        return new Rectangle((int)x + 5, (int)y + 5, width - 10, height - 10);
    }
    
    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRoundRect((int)x + 3, (int)y + 3, width, height, 10, 10);
        
        g2d.setColor(color);
        g2d.fillRoundRect((int)x, (int)y, width, height, 10, 10);
        
        g2d.setColor(new Color(135, 206, 235, 180));
        g2d.fillRect((int)x + 15, (int)y + 8, width - 30, 8);
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect((int)x + 15, (int)y + 25, 4, height - 40);
        g2d.fillRect((int)x + width - 19, (int)y + 25, 4, height - 40);
        
        g2d.setColor(Color.YELLOW);
        g2d.fillOval((int)x + 5, (int)y + 2, 10, 8);
        g2d.fillOval((int)x + width - 15, (int)y + 2, 10, 8);
        
        g2d.setColor(Color.RED);
        g2d.fillOval((int)x + 5, (int)y + height - 10, 10, 8);
        g2d.fillOval((int)x + width - 15, (int)y + height - 10, 10, 8);
        
        g2d.setColor(Color.BLACK);
        g2d.fillRoundRect((int)x - 3, (int)y + 10, 8, 18, 4, 4);
        g2d.fillRoundRect((int)x + width - 5, (int)y + 10, 8, 18, 4, 4);
        g2d.fillRoundRect((int)x - 3, (int)y + height - 28, 8, 18, 4, 4);
        g2d.fillRoundRect((int)x + width - 5, (int)y + height - 28, 8, 18, 4, 4);
    }
}

class RoadMarking {
    double y;
    
    public RoadMarking(double y) {
        this.y = y;
    }
}

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

class PowerUp {
    double x, y;
    int type;
    double rotation = 0;
    int width = 30;
    int height = 30;
    
    static Color[] colors = {
        new Color(0, 150, 255),
        new Color(0, 255, 255),
        new Color(255, 50, 50),
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
        
        g2dCopy.setColor(new Color(colors[type].getRed(), colors[type].getGreen(), 
                                   colors[type].getBlue(), 100));
        g2dCopy.fillOval(-width/2 - 5, -height/2 - 5, width + 10, height + 10);
        
        g2dCopy.setColor(colors[type]);
        g2dCopy.fillRoundRect(-width/2, -height/2, width, height, 8, 8);
        
        g2dCopy.setColor(Color.WHITE);
        g2dCopy.setStroke(new BasicStroke(2));
        g2dCopy.drawRoundRect(-width/2, -height/2, width, height, 8, 8);
        
        g2dCopy.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fm = g2dCopy.getFontMetrics();
        int textWidth = fm.stringWidth(labels[type]);
        g2dCopy.drawString(labels[type], -textWidth/2, 6);
        
        g2dCopy.dispose();
    }
}

class SkidMark {
    double x, y;
    double tiltAngle;
    float alpha = 170;

    public SkidMark(double x, double y, double tiltAngle) {
        this.x = x;
        this.y = y;
        this.tiltAngle = tiltAngle;
    }

    public void update(int speed) {
        y += speed / 10;
        alpha -= 0.35f;
    }

    public boolean isDead() {
        return alpha <= 0 || y > 750;
    }

    public void draw(Graphics2D g2d) {
        Graphics2D g = (Graphics2D) g2d.create();
        g.rotate(Math.toRadians(tiltAngle), x, y);
        g.setColor(new Color(15, 15, 15, Math.max(0, (int)alpha)));
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int)x, (int)y, (int)x, (int)y + 10);
        g.dispose();
    }
}