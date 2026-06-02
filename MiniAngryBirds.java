import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Iterator;

public class MiniAngryBirds extends JPanel implements ActionListener,
        MouseListener, MouseMotionListener {

    private final Timer timer = new Timer(16, this);

    private Bird bird;
    private final ArrayList<Block> blocks = new ArrayList<>();

    private boolean dragging = false;
    private Point dragPoint;

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;

    public MiniAngryBirds() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235));

        addMouseListener(this);
        addMouseMotionListener(this);

        resetLevel();
        timer.start();
    }

    private void resetLevel() {
        bird = new Bird(150, 450);

        blocks.clear();

        blocks.add(new Block(750, 420, 40, 120));
        blocks.add(new Block(800, 420, 40, 120));
        blocks.add(new Block(725, 380, 140, 40));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        // Ground
        g2.setColor(new Color(80, 180, 80));
        g2.fillRect(0, 500, WIDTH, 100);

        // Slingshot
        g2.setColor(new Color(120, 70, 20));
        g2.fillRect(130, 400, 15, 100);
        g2.fillRect(165, 400, 15, 100);

        if (dragging) {
            g2.setColor(Color.BLACK);
            g2.drawLine(137, 420, bird.x, bird.y);
            g2.drawLine(172, 420, bird.x, bird.y);
        }

        // Blocks
        for (Block b : blocks) {
            b.draw(g2);
        }

        // Bird
        bird.draw(g2);

        if (blocks.isEmpty()) {
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            g2.setColor(Color.BLACK);
            g2.drawString("YOU WIN!", 380, 150);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        bird.update();

        Iterator<Block> it = blocks.iterator();

        while (it.hasNext()) {
            Block b = it.next();

            if (b.intersects(bird)) {

                double impact =
                        Math.sqrt(bird.vx * bird.vx + bird.vy * bird.vy);

                if (impact > 5) {
                    it.remove();
                }

                bird.vx *= 0.7;
                bird.vy *= -0.4;
            }
        }

        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {

        double dx = e.getX() - bird.x;
        double dy = e.getY() - bird.y;

        if (Math.sqrt(dx * dx + dy * dy) < bird.radius) {
            dragging = true;
            dragPoint = e.getPoint();
            bird.launched = false;
            bird.vx = 0;
            bird.vy = 0;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {

        if (!dragging)
            return;

        int maxStretch = 100;

        int dx = e.getX() - 150;
        int dy = e.getY() - 420;

        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > maxStretch) {
            dx = (int) (dx * maxStretch / dist);
            dy = (int) (dy * maxStretch / dist);
        }

        bird.x = 150 + dx;
        bird.y = 420 + dy;
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        if (!dragging)
            return;

        dragging = false;

        bird.vx = (150 - bird.x) * 0.25;
        bird.vy = (420 - bird.y) * 0.25;

        bird.launched = true;
    }

    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    static class Bird {

        double x;
        double y;

        double vx;
        double vy;

        final int radius = 20;

        boolean launched = false;

        Bird(double x, double y) {
            this.x = x;
            this.y = y;
        }

        void update() {

            if (!launched)
                return;

            vy += 0.35;

            x += vx;
            y += vy;

            if (y + radius > 500) {
                y = 500 - radius;

                vy *= -0.45;
                vx *= 0.92;

                if (Math.abs(vy) < 1) {
                    vy = 0;
                }
            }
        }

        void draw(Graphics2D g) {
            g.setColor(Color.RED);
            g.fill(new Ellipse2D.Double(
                    x - radius,
                    y - radius,
                    radius * 2,
                    radius * 2));
        }

        Rectangle bounds() {
            return new Rectangle(
                    (int) (x - radius),
                    (int) (y - radius),
                    radius * 2,
                    radius * 2);
        }
    }

    static class Block {

        int x, y, w, h;

        Block(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        void draw(Graphics2D g) {
            g.setColor(new Color(160, 110, 60));
            g.fillRect(x, y, w, h);

            g.setColor(Color.BLACK);
            g.drawRect(x, y, w, h);
        }

        boolean intersects(Bird bird) {
            return new Rectangle(x, y, w, h)
                    .intersects(bird.bounds());
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame("Mini Angry Birds");

            frame.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);

            frame.add(new MiniAngryBirds());
            frame.pack();

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}