import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class HillClimbRacing extends JPanel implements ActionListener, MouseListener, KeyListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final double CAR_SPEED = 3;
    private static final double TURN_SPEED = 0.08;
    private static final double DRIFT_FRICTION = 0.95;
    
    private Timer timer;
    private Car car;
    private List<Platform> platforms;
    private double cameraX, cameraY;
    private int score;
    private boolean gameOver;
    private boolean turningLeft;
    private boolean turningRight;
    
    public HillClimbRacing() {
        initGame();
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addMouseListener(this);
        addKeyListener(this);
        timer = new Timer(16, this);
        timer.start();
    }
    
    private void initGame() {
        car = new Car(0, 0);
        platforms = new ArrayList<>();
        cameraX = 0;
        cameraY = 0;
        score = 0;
        gameOver = false;
        turningLeft = false;
        turningRight = false;
        
        generatePlatforms();
    }
    
    private void generatePlatforms() {
        // Starting platform
        platforms.add(new Platform(0, 0, 300, 80, 0));
        
        double lastX = 300;
        double lastY = 0;
        double lastAngle = 0;
        double lastWidth = 300;
        
        for (int i = 0; i < 100; i++) {
            // Random turn direction
            int turnDirection = (int)(Math.random() * 3) - 1; // -1, 0, or 1
            
            // Calculate new angle
            double newAngle = lastAngle + turnDirection * Math.PI / 2;
            
            // Calculate new position
            double platformLength = 150 + Math.random() * 100;
            double newX = lastX + Math.cos(lastAngle) * lastWidth / 2 + Math.cos(newAngle) * platformLength / 2;
            double newY = lastY + Math.sin(lastAngle) * lastWidth / 2 + Math.sin(newAngle) * platformLength / 2;
            
            // Platform width
            double newWidth = 80 + Math.random() * 40;
            
            platforms.add(new Platform(newX, newY, platformLength, newWidth, newAngle));
            
            lastX = newX;
            lastY = newY;
            lastAngle = newAngle;
            lastWidth = platformLength;
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            update();
        }
        repaint();
    }
    
    private void update() {
        // Update car
        car.update(turningLeft, turningRight);
        
        // Update camera to follow car
        cameraX = car.x - WIDTH / 2;
        cameraY = car.y - HEIGHT / 2;
        
        // Update score
        score = Math.max(score, (int)(car.x / 10));
        
        // Check if car is on any platform
        boolean onPlatform = false;
        for (Platform platform : platforms) {
            if (isCarOnPlatform(car, platform)) {
                onPlatform = true;
                break;
            }
        }
        
        // Game over if not on any platform
        if (!onPlatform && car.y > 100) {
            gameOver = true;
        }
        
        // Generate more platforms if needed
        if (car.x > platforms.get(platforms.size() - 1).x - 500) {
            generateMorePlatforms();
        }
    }
    
    private boolean isCarOnPlatform(Car car, Platform platform) {
        // Transform car position to platform's local coordinate system
        double dx = car.x - platform.x;
        double dy = car.y - platform.y;
        
        // Rotate by negative platform angle
        double cos = Math.cos(-platform.angle);
        double sin = Math.sin(-platform.angle);
        double localX = dx * cos - dy * sin;
        double localY = dx * sin + dy * cos;
        
        // Check if car is within platform bounds
        return Math.abs(localX) < platform.length / 2 + car.width / 2 &&
               Math.abs(localY) < platform.width / 2 + car.height / 2;
    }
    
    private void generateMorePlatforms() {
        Platform last = platforms.get(platforms.size() - 1);
        double lastX = last.x;
        double lastY = last.y;
        double lastAngle = last.angle;
        double lastWidth = last.length;
        
        for (int i = 0; i < 20; i++) {
            int turnDirection = (int)(Math.random() * 3) - 1;
            double newAngle = lastAngle + turnDirection * Math.PI / 2;
            double platformLength = 150 + Math.random() * 100;
            double newX = lastX + Math.cos(lastAngle) * lastWidth / 2 + Math.cos(newAngle) * platformLength / 2;
            double newY = lastY + Math.sin(lastAngle) * lastWidth / 2 + Math.sin(newAngle) * platformLength / 2;
            double newWidth = 80 + Math.random() * 40;
            
            platforms.add(new Platform(newX, newY, platformLength, newWidth, newAngle));
            
            lastX = newX;
            lastY = newY;
            lastAngle = newAngle;
            lastWidth = platformLength;
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw background
        g2d.setColor(new Color(30, 30, 50));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Draw grid for depth effect
        g2d.setColor(new Color(50, 50, 80));
        for (int i = 0; i < WIDTH; i += 50) {
            g2d.drawLine(i, 0, i, HEIGHT);
        }
        for (int i = 0; i < HEIGHT; i += 50) {
            g2d.drawLine(0, i, WIDTH, i);
        }
        
        g2d.translate(-(int)cameraX, -(int)cameraY);
        
        // Draw platforms
        for (Platform platform : platforms) {
            platform.draw(g2d);
        }
        
        // Draw car
        car.draw(g2d);
        
        g2d.translate((int)cameraX, (int)cameraY);
        
        // Draw UI
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        g2d.drawString("Score: " + score, 20, 50);
        
        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.drawString("GAME OVER", WIDTH / 2 - 150, HEIGHT / 2 - 30);
            g2d.setFont(new Font("Arial", Font.PLAIN, 28));
            g2d.drawString("Final Score: " + score, WIDTH / 2 - 100, HEIGHT / 2 + 20);
            g2d.drawString("Click or press SPACE to restart", WIDTH / 2 - 160, HEIGHT / 2 + 70);
        }
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        if (gameOver) {
            initGame();
        } else {
            // Toggle turning based on which side of screen
            if (e.getX() < WIDTH / 2) {
                turningLeft = true;
                turningRight = false;
            } else {
                turningRight = true;
                turningLeft = false;
            }
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        turningLeft = false;
        turningRight = false;
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                initGame();
            }
        } else {
            if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A) {
                turningLeft = true;
                turningRight = false;
            }
            if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D) {
                turningRight = true;
                turningLeft = false;
            }
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A) {
            turningLeft = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D) {
            turningRight = false;
        }
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {}
    
    @Override
    public void mouseEntered(MouseEvent e) {}
    
    @Override
    public void mouseExited(MouseEvent e) {}
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Drift Boss");
        HillClimbRacing game = new HillClimbRacing();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
    
    private class Car {
        double x, y;
        double angle;
        double vx, vy;
        int width = 40;
        int height = 25;
        
        public Car(double x, double y) {
            this.x = x;
            this.y = y;
            this.angle = 0;
            this.vx = 0;
            this.vy = 0;
        }
        
        public void update(boolean turnLeft, boolean turnRight) {
            // Auto move forward
            vx += Math.cos(angle) * CAR_SPEED * 0.1;
            vy += Math.sin(angle) * CAR_SPEED * 0.1;
            
            // Apply turning
            if (turnLeft) {
                angle -= TURN_SPEED;
            }
            if (turnRight) {
                angle += TURN_SPEED;
            }
            
            // Apply drift friction
            vx *= DRIFT_FRICTION;
            vy *= DRIFT_FRICTION;
            
            // Update position
            x += vx;
            y += vy;
            
            // Keep angle in reasonable range
            while (angle > Math.PI * 2) angle -= Math.PI * 2;
            while (angle < 0) angle += Math.PI * 2;
        }
        
        public void draw(Graphics2D g2d) {
            g2d.translate(x, y);
            g2d.rotate(angle);
            
            // Car body
            g2d.setColor(new Color(255, 100, 0));
            g2d.fillRect(-width / 2, -height / 2, width, height);
            
            // Car roof
            g2d.setColor(new Color(200, 50, 0));
            g2d.fillRect(-width / 4, -height / 2 - 10, width / 2, 10);
            
            // Windows
            g2d.setColor(new Color(150, 200, 255));
            g2d.fillRect(-width / 4 + 3, -height / 2 - 8, width / 2 - 6, 7);
            
            // Wheels
            g2d.setColor(Color.BLACK);
            g2d.fillOval(-width / 2 - 3, height / 2 - 8, 12, 12);
            g2d.fillOval(width / 2 - 9, height / 2 - 8, 12, 12);
            
            // Wheel details
            g2d.setColor(Color.GRAY);
            g2d.fillOval(-width / 2, height / 2 - 5, 6, 6);
            g2d.fillOval(width / 2 - 6, height / 2 - 5, 6, 6);
            
            g2d.rotate(-angle);
            g2d.translate(-x, -y);
        }
    }
    
    private class Platform {
        double x, y;
        double length, width;
        double angle;
        Color color;
        
        public Platform(double x, double y, double length, double width, double angle) {
            this.x = x;
            this.y = y;
            this.length = length;
            this.width = width;
            this.angle = angle;
            
            // Random color
            int hue = (int)(Math.random() * 360);
            this.color = Color.getHSBColor(hue / 360f, 0.7f, 0.8f);
        }
        
        public void draw(Graphics2D g2d) {
            g2d.translate(x, y);
            g2d.rotate(angle);
            
            // Platform shadow
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillRect(-(int)length / 2 + 5, -(int)width / 2 + 5, (int)length, (int)width);
            
            // Platform
            g2d.setColor(color);
            g2d.fillRect(-(int)length / 2, -(int)width / 2, (int)length, (int)width);
            
            // Platform border
            g2d.setColor(Color.WHITE);
            g2d.drawRect(-(int)length / 2, -(int)width / 2, (int)length, (int)width);
            
            // Platform pattern
            g2d.setColor(new Color(255, 255, 255, 50));
            for (int i = 0; i < length; i += 20) {
                g2d.fillRect(-(int)length / 2 + (int)i, -(int)width / 2, 10, (int)width);
            }
            
            g2d.rotate(-angle);
            g2d.translate(-x, -y);
        }
    }
}
