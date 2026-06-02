import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

public class HillClimbRacing extends JPanel implements ActionListener, KeyListener {

    Timer timer = new Timer(16, this);

    double carX = 200;
    double carY = 0;

    double velocityX = 0;
    double velocityY = 0;

    double wheelRotation = 0;

    boolean leftPressed = false;
    boolean rightPressed = false;

    double cameraX = 0;

    public HillClimbRacing() {
        setPreferredSize(new Dimension(1000, 600));
        setBackground(new Color(135, 206, 235));

        JFrame frame = new JFrame("Hill Climb Racing");
        frame.add(this);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        frame.addKeyListener(this);

        frame.setVisible(true);

        timer.start();
    }

    public double terrain(double x) {
        return 450
                + 60 * Math.sin(x * 0.01)
                + 40 * Math.sin(x * 0.02)
                + 20 * Math.sin(x * 0.05);
    }

    public double terrainSlope(double x) {
        return (terrain(x + 1) - terrain(x - 1)) / 2.0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (rightPressed) {
            velocityX += 0.08;
        }

        if (leftPressed) {
            velocityX -= 0.08;
        }

        velocityX *= 0.99;

        velocityY += 0.5;

        carX += velocityX;
        carY += velocityY;

        double ground = terrain(carX);

        if (carY > ground - 35) {
            carY = ground - 35;
            velocityY = 0;
        }

        wheelRotation += velocityX * 0.08;

        cameraX = carX - 300;

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.translate(-cameraX, 0);

        drawTerrain(g2);
        drawCar(g2);
    }

    private void drawTerrain(Graphics2D g2) {

        Polygon ground = new Polygon();

        ground.addPoint((int) cameraX - 100, 600);

        for (int x = (int) cameraX - 100;
             x <= cameraX + getWidth() + 100;
             x++) {

            ground.addPoint(x, (int) terrain(x));
        }

        ground.addPoint(
                (int) (cameraX + getWidth() + 100),
                600
        );

        g2.setColor(new Color(50, 180, 75));
        g2.fillPolygon(ground);
    }

    private void drawWheel(Graphics2D g2, int x, int y) {

        AffineTransform old = g2.getTransform();

        g2.translate(x, y);
        g2.rotate(wheelRotation);

        g2.setColor(Color.BLACK);
        g2.fillOval(-15, -15, 30, 30);

        g2.setColor(Color.WHITE);

        g2.drawLine(0, 0, 12, 0);
        g2.drawLine(0, 0, -12, 0);

        g2.drawLine(0, 0, 0, 12);
        g2.drawLine(0, 0, 0, -12);

        g2.setTransform(old);
    }

    private void drawCar(Graphics2D g2) {

        double slope = terrainSlope(carX);

        double angle = Math.atan(slope);

        AffineTransform old = g2.getTransform();

        g2.translate(carX, carY);
        g2.rotate(angle);

        g2.setColor(Color.RED);
        g2.fillRoundRect(-40, -35, 80, 25, 10, 10);

        g2.setColor(Color.ORANGE);
        g2.fillRoundRect(-15, -55, 40, 20, 10, 10);

        drawWheel(g2, -25, 0);
        drawWheel(g2, 25, 0);

        g2.setTransform(old);
    }

    @Override
    public void keyPressed(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            rightPressed = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            leftPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            rightPressed = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            leftPressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HillClimbRacing::new);
    }
}