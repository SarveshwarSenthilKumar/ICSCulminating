import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class DriftGame extends JPanel implements ActionListener, KeyListener {

    private Timer timer;

    private double carX = 300;
    private double carY = 500;

    private boolean leftHeld = false;
    private boolean rightHeld = false;

    private final int ROAD_WIDTH = 250;
    private int roadCenter = 350;

    private ArrayList<Rectangle> roadSegments = new ArrayList<>();

    private int score = 0;
    private boolean gameOver = false;

    private Random rand = new Random();

    public DriftGame() {
        setPreferredSize(new Dimension(700, 700));
        setBackground(Color.BLACK);

        addKeyListener(this);
        setFocusable(true);

        initializeRoad();

        timer = new Timer(16, this);
        timer.start();
    }

    private void initializeRoad() {
        int y = -100;

        while (y < 800) {
            roadSegments.add(new Rectangle(
                    roadCenter - ROAD_WIDTH / 2,
                    y,
                    ROAD_WIDTH,
                    100));
            y += 100;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (!gameOver) {

            if (leftHeld)
                carX -= 4;

            if (rightHeld)
                carX += 4;

            moveRoad();

            Rectangle carRect = new Rectangle(
                    (int) carX - 20,
                    (int) carY - 35,
                    40,
                    70);

            boolean onRoad = false;

            for (Rectangle seg : roadSegments) {
                if (seg.intersects(carRect)) {
                    onRoad = true;
                    break;
                }
            }

            if (!onRoad)
                gameOver = true;

            score++;
        }

        repaint();
    }

    private void moveRoad() {

        for (Rectangle seg : roadSegments) {
            seg.y += 5;
        }

        while (!roadSegments.isEmpty()
                && roadSegments.get(0).y > 700) {

            roadSegments.remove(0);

            Rectangle last = roadSegments.get(
                    roadSegments.size() - 1);

            int shift = rand.nextInt(81) - 40;

            roadCenter += shift;

            if (roadCenter < 180)
                roadCenter = 180;

            if (roadCenter > 520)
                roadCenter = 520;

            roadSegments.add(new Rectangle(
                    roadCenter - ROAD_WIDTH / 2,
                    last.y - 100,
                    ROAD_WIDTH,
                    100));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        for (Rectangle seg : roadSegments) {

            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(seg.x, seg.y, seg.width, seg.height);

            g2.setColor(Color.WHITE);

            for (int y = seg.y; y < seg.y + seg.height; y += 30) {
                g2.fillRect(seg.x + seg.width / 2 - 5, y, 10, 20);
            }
        }

        g2.setColor(Color.RED);

        int[] xPoints = {
                (int) carX,
                (int) carX - 20,
                (int) carX + 20
        };

        int[] yPoints = {
                (int) carY - 35,
                (int) carY + 35,
                (int) carY + 35
        };

        g2.fillPolygon(xPoints, yPoints, 3);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.drawString("Score: " + score, 20, 40);

        if (gameOver) {

            g2.setFont(new Font("Arial", Font.BOLD, 48));
            g2.drawString("GAME OVER", 190, 300);

            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.drawString(
                    "Press R to Restart",
                    240,
                    350);
        }
    }

    private void restart() {

        carX = 300;
        carY = 500;

        score = 0;
        gameOver = false;

        roadCenter = 350;

        roadSegments.clear();
        initializeRoad();
    }

    @Override
    public void keyPressed(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_LEFT)
            leftHeld = true;

        if (e.getKeyCode() == KeyEvent.VK_RIGHT)
            rightHeld = true;

        if (gameOver &&
                e.getKeyCode() == KeyEvent.VK_R)
            restart();
    }

    @Override
    public void keyReleased(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_LEFT)
            leftHeld = false;

        if (e.getKeyCode() == KeyEvent.VK_RIGHT)
            rightHeld = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame frame =
                    new JFrame("Drifting Game");

            frame.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);

            frame.add(new DriftGame());

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}