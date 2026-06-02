import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class HillClimbRacing extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;
    private static final double GRAVITY = 0.3;
    private static final double FRICTION = 0.98;
    private static final double ACCELERATION = 0.5;
    private static final double ROTATION_SPEED = 0.08;
    private static final double MAX_SPEED = 15;
    
    private Timer timer;
    private Car car;
    private List<Point> terrainPoints;
    private double cameraOffsetX;
    private boolean gameOver;
    private int score;
    
    public HillClimbRacing() {
        initGame();
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(16, this);
        timer.start();
    }
    
    private void initGame() {
        car = new Car(100, 300);
        terrainPoints = generateTerrain();
        cameraOffsetX = 0;
        gameOver = false;
        score = 0;
    }
    
    private List<Point> generateTerrain() {
        List<Point> points = new ArrayList<>();
        points.add(new Point(0, HEIGHT - 100));
        
        double x = 0;
        double y = HEIGHT - 100;
        
        while (x < 10000) {
            x += 50 + Math.random() * 30;
            y += (Math.random() - 0.5) * 80;
            y = Math.max(200, Math.min(HEIGHT - 50, y));
            points.add(new Point((int)x, (int)y));
        }
        
        return points;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            update();
        }
        repaint();
    }
    
    private void update() {
        // Update car physics
        car.update();
        
        // Check collision with terrain
        checkTerrainCollision();
        
        // Update camera
        cameraOffsetX = car.x - WIDTH / 3;
        
        // Update score
        score = (int) Math.max(score, car.x / 10);
        
        // Check if car fell off
        if (car.y > HEIGHT + 100) {
            gameOver = true;
        }
    }
    
    private void checkTerrainCollision() {
        // Find terrain segment under car
        for (int i = 0; i < terrainPoints.size() - 1; i++) {
            Point p1 = terrainPoints.get(i);
            Point p2 = terrainPoints.get(i + 1);
            
            if (car.x >= p1.x && car.x <= p2.x) {
                // Calculate terrain height at car position
                double t = (car.x - p1.x) / (p2.x - p1.x);
                double terrainY = p1.y + t * (p2.y - p1.y);
                
                // Calculate terrain angle
                double terrainAngle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
                
                // Check if car is below terrain
                if (car.y + car.height / 2 > terrainY) {
                    car.y = terrainY - car.height / 2;
                    car.vy = 0;
                    
                    // Apply friction and adjust angle
                    car.vx *= FRICTION;
                    car.angle = terrainAngle;
                    
                    // Prevent car from sinking
                    if (car.vx < 0.1 && car.vx > -0.1) {
                        car.vx = 0;
                    }
                }
                break;
            }
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw sky
        g2d.setColor(new Color(135, 206, 235));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Draw clouds
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < 5; i++) {
            int cloudX = (int)((i * 300 - cameraOffsetX * 0.3) % (WIDTH + 200)) - 100;
            if (cloudX < -100) cloudX += WIDTH + 200;
            g2d.fillOval(cloudX, 50 + i * 30, 80, 40);
            g2d.fillOval(cloudX + 30, 40 + i * 30, 60, 35);
        }
        
        g2d.translate(-(int)cameraOffsetX, 0);
        
        // Draw terrain
        g2d.setColor(new Color(34, 139, 34));
        Polygon terrain = new Polygon();
        terrain.addPoint((int)terrainPoints.get(0).x, HEIGHT);
        for (Point p : terrainPoints) {
            terrain.addPoint(p.x, p.y);
        }
        terrain.addPoint(terrainPoints.get(terrainPoints.size() - 1).x, HEIGHT);
        g2d.fillPolygon(terrain);
        
        // Draw terrain outline
        g2d.setColor(new Color(0, 100, 0));
        for (int i = 0; i < terrainPoints.size() - 1; i++) {
            Point p1 = terrainPoints.get(i);
            Point p2 = terrainPoints.get(i + 1);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
        
        // Draw car
        car.draw(g2d);
        
        g2d.translate((int)cameraOffsetX, 0);
        
        // Draw UI
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Score: " + score, 20, 30);
        g2d.drawString("Speed: " + String.format("%.1f", Math.abs(car.vx)), 20, 60);
        
        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.drawString("GAME OVER", WIDTH / 2 - 140, HEIGHT / 2);
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            g2d.drawString("Final Score: " + score, WIDTH / 2 - 80, HEIGHT / 2 + 50);
            g2d.drawString("Press R to restart", WIDTH / 2 - 90, HEIGHT / 2 + 90);
        }
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_R) {
                initGame();
            }
            return;
        }
        
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_RIGHT:
                car.accelerating = true;
                break;
            case KeyEvent.VK_LEFT:
                car.braking = true;
                break;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_RIGHT:
                car.accelerating = false;
                break;
            case KeyEvent.VK_LEFT:
                car.braking = false;
                break;
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Hill Climb Racing");
        HillClimbRacing game = new HillClimbRacing();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
    
    private class Car {
        private double x, y;
        private double vx, vy;
        private double angle;
        private int width = 60;
        private int height = 30;
        private boolean accelerating;
        private boolean braking;
        
        public Car(double x, double y) {
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = 0;
            this.angle = 0;
        }
        
        public void update() {
            // Apply gravity
            vy += GRAVITY;
            
            // Apply acceleration/braking
            if (accelerating) {
                vx += ACCELERATION * Math.cos(angle);
                vy += ACCELERATION * Math.sin(angle);
            }
            if (braking) {
                vx -= ACCELERATION * Math.cos(angle) * 0.5;
            }
            
            // Limit speed
            double speed = Math.sqrt(vx * vx + vy * vy);
            if (speed > MAX_SPEED) {
                vx = (vx / speed) * MAX_SPEED;
                vy = (vy / speed) * MAX_SPEED;
            }
            
            // Update position
            x += vx;
            y += vy;
            
            // Air rotation
            if (!isOnGround()) {
                if (accelerating) {
                    angle -= ROTATION_SPEED;
                }
                if (braking) {
                    angle += ROTATION_SPEED;
                }
            }
            
            // Keep angle reasonable
            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;
        }
        
        private boolean isOnGround() {
            for (int i = 0; i < terrainPoints.size() - 1; i++) {
                Point p1 = terrainPoints.get(i);
                Point p2 = terrainPoints.get(i + 1);
                
                if (x >= p1.x && x <= p2.x) {
                    double t = (x - p1.x) / (p2.x - p1.x);
                    double terrainY = p1.y + t * (p2.y - p1.y);
                    return y + height / 2 >= terrainY - 5;
                }
            }
            return false;
        }
        
        public void draw(Graphics2D g2d) {
            g2d.translate(x, y);
            g2d.rotate(angle);
            
            // Car body
            g2d.setColor(Color.RED);
            g2d.fillRect(-width / 2, -height / 2, width, height);
            
            // Car roof
            g2d.setColor(new Color(200, 0, 0));
            g2d.fillRect(-width / 4, -height / 2 - 15, width / 2, 15);
            
            // Windows
            g2d.setColor(new Color(135, 206, 250));
            g2d.fillRect(-width / 4 + 5, -height / 2 - 12, width / 2 - 10, 10);
            
            // Wheels
            g2d.setColor(Color.BLACK);
            g2d.fillOval(-width / 2 - 5, height / 2 - 10, 20, 20);
            g2d.fillOval(width / 2 - 15, height / 2 - 10, 20, 20);
            
            // Wheel hubs
            g2d.setColor(Color.GRAY);
            g2d.fillOval(-width / 2, height / 2 - 5, 10, 10);
            g2d.fillOval(width / 2 - 10, height / 2 - 5, 10, 10);
            
            g2d.rotate(-angle);
            g2d.translate(-x, -y);
        }
    }
}
