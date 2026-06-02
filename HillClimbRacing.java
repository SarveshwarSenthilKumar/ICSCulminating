import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;

public class HillClimbRacing extends JFrame {

    public HillClimbRacing() {
        setTitle("Hill Climb Racing (Java Swing)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HillClimbRacing game = new HillClimbRacing();
            game.setVisible(true);
        });
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {

    private final Timer timer;
    private final int WIDTH = 800;
    private final int HEIGHT = 600;

    // Car Physics Variables
    private double carX = 100;
    private double carY = 300;
    private double velocityX = 0;
    private double velocityY = 0;
    private double carAngle = 0; // in radians
    private double angularVelocity = 0;

    private final double GRAVITY = 0.3;
    private final double ACCELERATION = 0.5;
    private final double BRAKE = 0.3;
    private final double FRICTION = 0.98;
    private final double AIR_CONTROL = 0.05;

    // Input States
    private boolean keyUp = false;
    private boolean keyDown = false;
    private boolean keyLeft = false;
    private boolean keyRight = false;

    // Environment
    private final int GROUND_LEVEL = 450;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Sky Blue
        setFocusable(true);
        addKeyListener(this);

        // 60 FPS Game Loop
        timer = new Timer(16, this);
        timer.start();
    }

    // Procedural Hill Function: Returns Y coordinate for a given X coordinate
    private double getHillHeight(double x) {
        return GROUND_LEVEL 
            - Math.sin(x * 0.005) * 100 
            - Math.cos(x * 0.01) * 50 
            - Math.sin(x * 0.02) * 20;
    }

    // Get the slope angle of the hill at a given X coordinate
    private double getHillAngle(double x) {
        double delta = 1.0;
        double y1 = getHillHeight(x - delta);
        double y2 = getHillHeight(x + delta);
        return Math.atan2(y2 - y1, delta * 2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Camera offset to follow the car horizontally
        int cameraX = (int) carX - 200;

        // 1. Draw the Terrain / Hills
        g2d.setColor(new Color(34, 139, 34)); // Forest Green
        Polygon terrain = new Polygon();
        for (int x = 0; x <= WIDTH; x += 5) {
            int worldX = x + cameraX;
            int worldY = (int) getHillHeight(worldX);
            terrain.addPoint(x, worldY);
        }
        terrain.addPoint(WIDTH, HEIGHT);
        terrain.addPoint(0, HEIGHT);
        g2d.fillPolygon(terrain);

        // 2. Draw the Car
        AffineTransform oldTransform = g2d.getTransform();
        
        // Translate to car's screen position and rotate
        int screenCarX = (int) carX - cameraX;
        g2d.translate(screenCarX, (int) carY);
        g2d.rotate(carAngle);

        // Draw Car Body (Centered around 0,0)
        g2d.setColor(Color.RED);
        g2d.fillRect(-30, -20, 60, 15); // Main chassis
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(-15, -30, 30, 10); // Cabin

        // Draw Wheels
        g2d.setColor(Color.BLACK);
        g2d.fillOval(-25, -5, 16, 16); // Back wheel
        g2d.fillOval(9, -5, 16, 16);  // Front wheel
        
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillOval(-21, -1, 8, 8);
        g2d.fillOval(13, -1, 8, 8);

        // Reset transformation matrix
        g2d.setTransform(oldTransform);

        // 3. Draw UI / Instructions
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Controls: UP (Gas) | DOWN (Brake) | LEFT/RIGHT (Air Rotate)", 15, 25);
        g2d.drawString(String.format("Distance: %.1f m", carX / 10), 15, 50);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Fetch current hill stats beneath the car
        double hillY = getHillHeight(carX);
        double hillAngle = getHillAngle(carX);

        boolean isGrounded = carY >= hillY - 2;

        if (isGrounded) {
            // --- GROUND PHYSICS ---
            carY = hillY; // Snap to surface
            velocityY = 0;

            // Apply friction/drag naturally
            velocityX *= FRICTION;

            // Handle Driving Controls
            if (keyUp) {
                velocityX += ACCELERATION * Math.cos(hillAngle);
                velocityY += ACCELERATION * Math.sin(hillAngle);
            }
            if (keyDown) {
                velocityX -= BRAKE * Math.cos(hillAngle);
                velocityY -= BRAKE * Math.sin(hillAngle);
            }

            // Align car chassis angle smoothly to the terrain slope
            carAngle = carAngle * 0.7 + hillAngle * 0.3;
            angularVelocity = 0; 
        } else {
            // --- AIR PHYSICS ---
            velocityY += GRAVITY; // Gravity pulls down
            velocityX *= 0.99;    // Slight air resistance

            // Air rotation controls (flips)
            if (keyLeft) {
                angularVelocity -= AIR_CONTROL * 0.1;
            }
            if (keyRight) {
                angularVelocity += AIR_CONTROL * 0.1;
            }

            angularVelocity *= 0.95; // Angular damping
            carAngle += angularVelocity;
        }

        // Apply velocities to coordinates
        carX += velocityX;
        carY += velocityY;

        // Prevent driving backward off-screen
        if (carX < 50) {
            carX = 50;
            velocityX = 0;
        }

        // Crash check: If the car flips completely upside down on the ground, reset it
        if (isGrounded && Math.abs(carAngle - hillAngle) > Math.PI / 2) {
            // Reset Car
            carX = Math.max(100, carX - 200);
            carY = getHillHeight(carX) - 50;
            carAngle = getHillAngle(carX);
            velocityX = 0;
            velocityY = 0;
            angularVelocity = 0;
            JOptionPane.showMessageDialog(this, "You Crashed! Resetting to last safe hill.");
        }

        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> keyUp = true;
            case KeyEvent.VK_DOWN -> keyDown = true;
            case KeyEvent.VK_LEFT -> keyLeft = true;
            case KeyEvent.VK_RIGHT -> keyRight = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> keyUp = false;
            case KeyEvent.VK_DOWN -> keyDown = false;
            case KeyEvent.VK_LEFT -> keyLeft = false;
            case KeyEvent.VK_RIGHT -> keyRight = false;
        }
    }
}