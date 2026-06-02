import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Random;

public class HillClimbRacing extends JPanel implements ActionListener {
    
    // Game constants
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 500;
    private static final int GROUND_Y = 400;
    private static final int TIMER_DELAY = 20; // ms
    
    // Vehicle constants
    private static final int CAR_WIDTH = 40;
    private static final int CAR_HEIGHT = 25;
    private static final int WHEEL_RADIUS = 10;
    
    // Physics constants
    private static final double GRAVITY = 0.5;
    private static final double GROUND_FRICTION = 0.98;
    private static final double ENGINE_POWER = 0.8;
    private static final double BRAKE_POWER = 0.5;
    private static final double MAX_SPEED = 15;
    
    // Game objects
    private Car car;
    private ArrayList<Point> terrainPoints;
    private Random random;
    private Timer timer;
    private int cameraX;
    private int score;
    private boolean gameRunning;
    
    // Key states
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean upPressed;
    
    public HillClimbRacing() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky blue
        setFocusable(true);
        
        random = new Random();
        car = new Car();
        terrainPoints = new ArrayList<>();
        generateTerrain();
        
        cameraX = 0;
        score = 0;
        gameRunning = true;
        
        timer = new Timer(TIMER_DELAY, this);
        timer.start();
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                handleKeyRelease(e);
            }
        });
    }
    
    private void generateTerrain() {
        terrainPoints.clear();
        
        // Start from the left edge
        double currentHeight = GROUND_Y;
        double slope = 0;
        
        for (int x = 0; x <= WIDTH * 3; x += 20) { // Generate 3 screens worth of terrain
            // Randomly change slope occasionally
            if (random.nextInt(100) < 15) {
                slope = (random.nextDouble() - 0.5) * 0.3;
            }
            
            currentHeight += slope * 20;
            
            // Keep terrain within bounds
            if (currentHeight < GROUND_Y - 150) {
                currentHeight = GROUND_Y - 150;
                slope = 0.1;
            }
            if (currentHeight > GROUND_Y + 100) {
                currentHeight = GROUND_Y + 100;
                slope = -0.1;
            }
            
            terrainPoints.add(new Point(x, (int)currentHeight));
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable anti-aliasing for smoother graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw ground
        drawTerrain(g2d);
        
        // Draw car
        drawCar(g2d);
        
        // Draw score and speed
        drawUI(g2d);
        
        // Draw game over if needed
        if (!gameRunning) {
            drawGameOver(g2d);
        }
    }
    
    private void drawTerrain(Graphics2D g) {
        // Draw the ground
        g.setColor(new Color(101, 67, 33)); // Brown
        int[] xPoints = new int[terrainPoints.size() + 2];
        int[] yPoints = new int[terrainPoints.size() + 2];
        
        for (int i = 0; i < terrainPoints.size(); i++) {
            Point p = terrainPoints.get(i);
            xPoints[i] = p.x - cameraX;
            yPoints[i] = p.y;
        }
        
        // Add bottom corners
        xPoints[terrainPoints.size()] = WIDTH;
        xPoints[terrainPoints.size() + 1] = 0;
        yPoints[terrainPoints.size()] = HEIGHT;
        yPoints[terrainPoints.size() + 1] = HEIGHT;
        
        g.fillPolygon(xPoints, yPoints, terrainPoints.size() + 2);
        
        // Draw grass
        g.setColor(new Color(34, 139, 34));
        for (int i = 0; i < terrainPoints.size() - 1; i++) {
            Point p1 = terrainPoints.get(i);
            Point p2 = terrainPoints.get(i + 1);
            int x1 = p1.x - cameraX;
            int x2 = p2.x - cameraX;
            if (x1 >= 0 || x2 >= 0) {
                g.drawLine(x1, p1.y, x2, p2.y);
            }
        }
    }
    
    private void drawCar(Graphics2D g) {
        int carX = (int)(car.x - cameraX);
        int carY = (int)car.y - CAR_HEIGHT;
        
        // Car body
        g.setColor(Color.RED);
        g.fillRoundRect(carX, carY, CAR_WIDTH, CAR_HEIGHT, 10, 10);
        
        // Car roof
        g.setColor(Color.DARK_GRAY);
        g.fillRoundRect(carX + 10, carY - 15, 20, 18, 8, 8);
        
        // Windows
        g.setColor(Color.CYAN);
        g.fillRect(carX + 12, carY - 12, 7, 10);
        g.fillRect(carX + 21, carY - 12, 7, 10);
        
        // Wheels
        g.setColor(Color.BLACK);
        g.fillOval(carX + 5, carY + CAR_HEIGHT - WHEEL_RADIUS, WHEEL_RADIUS * 2, WHEEL_RADIUS * 2);
        g.fillOval(carX + CAR_WIDTH - 15, carY + CAR_HEIGHT - WHEEL_RADIUS, WHEEL_RADIUS * 2, WHEEL_RADIUS * 2);
        
        // Wheel rims
        g.setColor(Color.GRAY);
        g.fillOval(carX + 9, carY + CAR_HEIGHT - 6, 4, 4);
        g.fillOval(carX + CAR_WIDTH - 11, carY + CAR_HEIGHT - 6, 4, 4);
        
        // Draw rotation
        if (car.rotation != 0) {
            g2d.rotate(car.rotation, carX + CAR_WIDTH / 2, carY + CAR_HEIGHT / 2);
            g.setColor(Color.RED);
            g.fillRoundRect(carX, carY, CAR_WIDTH, CAR_HEIGHT, 10, 10);
            g2d.rotate(-car.rotation, carX + CAR_WIDTH / 2, carY + CAR_HEIGHT / 2);
        }
    }
    
    private void drawUI(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, 10, 30);
        g.drawString("Speed: " + String.format("%.1f", Math.abs(car.velocityX)), 10, 55);
        
        // Draw controls hint
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Controls: ← → to drive, ↑ to brake, R to restart", 10, HEIGHT - 10);
    }
    
    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        String gameOverText = "GAME OVER!";
        FontMetrics fm = g.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(gameOverText)) / 2;
        g.drawString(gameOverText, x, HEIGHT / 2 - 50);
        
        g.setFont(new Font("Arial", Font.BOLD, 24));
        String scoreText = "Final Score: " + score;
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(scoreText)) / 2;
        g.drawString(scoreText, x, HEIGHT / 2);
        
        String restartText = "Press R to restart";
        fm = g.getFontMetrics();
        x = (WIDTH - fm.stringWidth(restartText)) / 2;
        g.drawString(restartText, x, HEIGHT / 2 + 50);
    }
    
    private void handleKeyPress(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                leftPressed = true;
                break;
            case KeyEvent.VK_RIGHT:
                rightPressed = true;
                break;
            case KeyEvent.VK_UP:
                upPressed = true;
                break;
            case KeyEvent.VK_R:
                if (!gameRunning) {
                    restartGame();
                }
                break;
        }
    }
    
    private void handleKeyRelease(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                leftPressed = false;
                break;
            case KeyEvent.VK_RIGHT:
                rightPressed = false;
                break;
            case KeyEvent.VK_UP:
                upPressed = false;
                break;
        }
    }
    
    private void restartGame() {
        car = new Car();
        cameraX = 0;
        score = 0;
        gameRunning = true;
        generateTerrain();
        leftPressed = false;
        rightPressed = false;
        upPressed = false;
    }
    
    private void updatePhysics() {
        if (!gameRunning) return;
        
        // Apply engine force
        if (rightPressed && car.velocityX < MAX_SPEED) {
            car.velocityX += ENGINE_POWER;
        }
        if (leftPressed && car.velocityX > -MAX_SPEED) {
            car.velocityX -= ENGINE_POWER;
        }
        
        // Apply brake
        if (upPressed) {
            if (car.velocityX > 0) {
                car.velocityX -= BRAKE_POWER;
                if (car.velocityX < 0) car.velocityX = 0;
            } else if (car.velocityX < 0) {
                car.velocityX += BRAKE_POWER;
                if (car.velocityX > 0) car.velocityX = 0;
            }
        }
        
        // Apply friction
        car.velocityX *= GROUND_FRICTION;
        
        // Update position
        car.x += car.velocityX;
        
        // Update camera
        cameraX = (int)car.x - WIDTH / 3;
        if (cameraX < 0) cameraX = 0;
        
        // Update score (distance traveled)
        score = (int)car.x / 10;
        
        // Find current terrain height
        int terrainHeight = getTerrainHeight((int)car.x + CAR_WIDTH / 2);
        
        // Apply gravity
        car.velocityY += GRAVITY;
        car.y += car.velocityY;
        
        // Collision detection with ground
        if (car.y + CAR_HEIGHT >= terrainHeight) {
            car.y = terrainHeight - CAR_HEIGHT;
            
            // Impact damage
            if (Math.abs(car.velocityY) > 8) {
                gameRunning = false;
            }
            
            car.velocityY = 0;
            
            // Calculate rotation based on terrain slope
            int slope = getTerrainSlope((int)car.x + CAR_WIDTH / 2);
            car.rotation = Math.toRadians(slope) * 0.5;
        }
        
        // Keep car above ground
        if (car.y + CAR_HEIGHT > GROUND_Y + 100) {
            gameRunning = false; // Fell too deep
        }
        
        // Boundary checks
        if (car.x < 0) {
            car.x = 0;
            car.velocityX = 0;
        }
    }
    
    private int getTerrainHeight(int x) {
        if (terrainPoints.isEmpty()) return GROUND_Y;
        
        // Find the segment containing x
        for (int i = 0; i < terrainPoints.size() - 1; i++) {
            Point p1 = terrainPoints.get(i);
            Point p2 = terrainPoints.get(i + 1);
            
            if (x >= p1.x && x <= p2.x) {
                // Linear interpolation
                double t = (double)(x - p1.x) / (p2.x - p1.x);
                return (int)(p1.y + t * (p2.y - p1.y));
            }
        }
        
        return GROUND_Y;
    }
    
    private int getTerrainSlope(int x) {
        if (terrainPoints.isEmpty()) return 0;
        
        for (int i = 0; i < terrainPoints.size() - 1; i++) {
            Point p1 = terrainPoints.get(i);
            Point p2 = terrainPoints.get(i + 1);
            
            if (x >= p1.x && x <= p2.x) {
                double slope = (double)(p2.y - p1.y) / (p2.x - p1.x);
                return (int)Math.toDegrees(Math.atan(slope));
            }
        }
        
        return 0;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        updatePhysics();
        repaint();
    }
    
    // Car class to hold vehicle state
    private class Car {
        double x;
        double y;
        double velocityX;
        double velocityY;
        double rotation;
        
        Car() {
            this.x = 100;
            this.y = GROUND_Y - CAR_HEIGHT;
            this.velocityX = 0;
            this.velocityY = 0;
            this.rotation = 0;
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hill Climb Racing");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new HillClimbRacing());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}