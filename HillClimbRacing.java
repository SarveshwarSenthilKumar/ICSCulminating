import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class HillClimbRacing extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;
    private static final double GRAVITY = 0.3;
    private static final double FRICTION = 0.99;
    
    private Timer timer;
    private Bird bird;
    private List<Pig> pigs;
    private List<Block> blocks;
    private Slingshot slingshot;
    private boolean isDragging;
    private boolean birdLaunched;
    private int score;
    private int birdsRemaining;
    
    public HillClimbRacing() {
        initGame();
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addMouseListener(this);
        addMouseMotionListener(this);
        timer = new Timer(16, this);
        timer.start();
    }
    
    private void initGame() {
        bird = null;
        pigs = new ArrayList<>();
        blocks = new ArrayList<>();
        slingshot = new Slingshot(150, HEIGHT - 150);
        isDragging = false;
        birdLaunched = false;
        score = 0;
        birdsRemaining = 5;
        
        createLevel();
        spawnBird();
    }
    
    private void createLevel() {
        // Create ground
        blocks.add(new Block(0, HEIGHT - 20, WIDTH, 20, Color.GREEN));
        
        // Create structure with pigs
        int baseX = 600;
        int baseY = HEIGHT - 20;
        
        // Base blocks
        blocks.add(new Block(baseX, baseY - 40, 20, 40, Color.ORANGE));
        blocks.add(new Block(baseX + 80, baseY - 40, 20, 40, Color.ORANGE));
        blocks.add(new Block(baseX, baseY - 80, 100, 20, Color.ORANGE));
        
        // Pig on base
        pigs.add(new Pig(baseX + 50, baseY - 110));
        
        // Second level
        blocks.add(new Block(baseX + 20, baseY - 120, 20, 40, Color.ORANGE));
        blocks.add(new Block(baseX + 60, baseY - 120, 20, 40, Color.ORANGE));
        blocks.add(new Block(baseX + 20, baseY - 160, 60, 20, Color.ORANGE));
        
        // Pig on second level
        pigs.add(new Pig(baseX + 50, baseY - 190));
        
        // Top block
        blocks.add(new Block(baseX + 30, baseY - 200, 20, 40, Color.ORANGE));
        
        // Another structure
        int baseX2 = 800;
        blocks.add(new Block(baseX2, baseY - 60, 20, 60, Color.ORANGE));
        blocks.add(new Block(baseX2 + 60, baseY - 60, 20, 60, Color.ORANGE));
        blocks.add(new Block(baseX2, baseY - 120, 80, 20, Color.ORANGE));
        
        pigs.add(new Pig(baseX2 + 40, baseY - 150));
    }
    
    private void spawnBird() {
        if (birdsRemaining > 0) {
            bird = new Bird(slingshot.x, slingshot.y);
            birdLaunched = false;
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        repaint();
    }
    
    private void update() {
        if (bird != null && birdLaunched) {
            bird.update();
            
            // Check collision with blocks
            for (Block block : blocks) {
                if (bird.collidesWith(block)) {
                    bird.vx *= -0.5;
                    bird.vy *= -0.5;
                    block.hit();
                }
            }
            
            // Check collision with pigs
            for (int i = pigs.size() - 1; i >= 0; i--) {
                Pig pig = pigs.get(i);
                if (bird.collidesWith(pig)) {
                    pigs.remove(i);
                    score += 100;
                }
            }
            
            // Check if bird is out of bounds
            if (bird.x > WIDTH + 100 || bird.x < -100 || bird.y > HEIGHT + 100) {
                birdsRemaining--;
                if (pigs.isEmpty()) {
                    // Level complete
                    JOptionPane.showMessageDialog(this, "Level Complete! Score: " + score);
                    initGame();
                } else if (birdsRemaining <= 0) {
                    // Game over
                    JOptionPane.showMessageDialog(this, "Game Over! Score: " + score);
                    initGame();
                } else {
                    spawnBird();
                }
            }
        }
        
        // Update blocks (remove destroyed ones)
        blocks.removeIf(block -> block.destroyed);
        
        // Check if pigs are hit by falling blocks
        for (int i = pigs.size() - 1; i >= 0; i--) {
            Pig pig = pigs.get(i);
            for (Block block : blocks) {
                if (block.collidesWith(pig) && block.vy > 2) {
                    pigs.remove(i);
                    score += 100;
                    break;
                }
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
        g2d.fillOval(100, 80, 100, 50);
        g2d.fillOval(150, 60, 80, 40);
        g2d.fillOval(400, 100, 120, 60);
        g2d.fillOval(450, 80, 90, 45);
        
        // Draw slingshot
        slingshot.draw(g2d);
        
        // Draw blocks
        for (Block block : blocks) {
            block.draw(g2d);
        }
        
        // Draw pigs
        for (Pig pig : pigs) {
            pig.draw(g2d);
        }
        
        // Draw bird
        if (bird != null) {
            bird.draw(g2d);
        }
        
        // Draw slingshot band if dragging
        if (isDragging && bird != null && !birdLaunched) {
            g2d.setColor(new Color(139, 69, 19));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(slingshot.x, slingshot.y, (int)bird.x, (int)bird.y);
        }
        
        // Draw UI
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Score: " + score, 20, 30);
        g2d.drawString("Birds: " + birdsRemaining, 20, 60);
        
        if (pigs.isEmpty()) {
            g2d.setColor(Color.GREEN);
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            g2d.drawString("LEVEL COMPLETE!", WIDTH / 2 - 150, HEIGHT / 2);
        }
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        if (bird != null && !birdLaunched) {
            int dx = e.getX() - slingshot.x;
            int dy = e.getY() - slingshot.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance < 50) {
                isDragging = true;
            }
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        if (isDragging && bird != null && !birdLaunched) {
            isDragging = false;
            
            // Calculate launch velocity
            double dx = slingshot.x - bird.x;
            double dy = slingshot.y - bird.y;
            
            bird.vx = dx * 0.15;
            bird.vy = dy * 0.15;
            
            // Limit maximum velocity
            double speed = Math.sqrt(bird.vx * bird.vx + bird.vy * bird.vy);
            if (speed > 20) {
                bird.vx = (bird.vx / speed) * 20;
                bird.vy = (bird.vy / speed) * 20;
            }
            
            birdLaunched = true;
        }
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        if (isDragging && bird != null && !birdLaunched) {
            int dx = e.getX() - slingshot.x;
            int dy = e.getY() - slingshot.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            // Limit drag distance
            if (distance > 100) {
                double angle = Math.atan2(dy, dx);
                bird.x = slingshot.x + Math.cos(angle) * 100;
                bird.y = slingshot.y + Math.sin(angle) * 100;
            } else {
                bird.x = e.getX();
                bird.y = e.getY();
            }
            
            // Only allow dragging back
            if (bird.x > slingshot.x) {
                bird.x = slingshot.x;
            }
        }
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {}
    
    @Override
    public void mouseEntered(MouseEvent e) {}
    
    @Override
    public void mouseExited(MouseEvent e) {}
    
    @Override
    public void mouseMoved(MouseEvent e) {}
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Angry Birds");
        HillClimbRacing game = new HillClimbRacing();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
    
    private class Bird {
        double x, y;
        double vx, vy;
        int radius = 20;
        
        public Bird(double x, double y) {
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = 0;
        }
        
        public void update() {
            vy += GRAVITY;
            vx *= FRICTION;
            vy *= FRICTION;
            
            x += vx;
            y += vy;
            
            // Ground collision
            if (y + radius > HEIGHT - 20) {
                y = HEIGHT - 20 - radius;
                vy *= -0.5;
                vx *= 0.8;
            }
        }
        
        public boolean collidesWith(Block block) {
            return x + radius > block.x && x - radius < block.x + block.width &&
                   y + radius > block.y && y - radius < block.y + block.height;
        }
        
        public boolean collidesWith(Pig pig) {
            double dx = x - pig.x;
            double dy = y - pig.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            return distance < radius + pig.radius;
        }
        
        public void draw(Graphics2D g2d) {
            // Body
            g2d.setColor(Color.RED);
            g2d.fillOval((int)(x - radius), (int)(y - radius), radius * 2, radius * 2);
            
            // Eyes
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int)(x - 8), (int)(y - 8), 10, 10);
            g2d.fillOval((int)(x + 2), (int)(y - 8), 10, 10);
            
            // Pupils
            g2d.setColor(Color.BLACK);
            g2d.fillOval((int)(x - 5), (int)(y - 6), 4, 4);
            g2d.fillOval((int)(x + 5), (int)(y - 6), 4, 4);
            
            // Beak
            g2d.setColor(Color.ORANGE);
            g2d.fillPolygon(new int[]{(int)x, (int)(x + 15), (int)x}, 
                           new int[]{(int)(y - 2), (int)y, (int)(y + 6)}, 3);
            
            // Eyebrows
            g2d.setColor(Color.BLACK);
            g2d.fillRect((int)(x - 10), (int)(y - 15), 20, 4);
        }
    }
    
    private class Pig {
        double x, y;
        int radius = 25;
        
        public Pig(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        public void draw(Graphics2D g2d) {
            // Body
            g2d.setColor(Color.GREEN);
            g2d.fillOval((int)(x - radius), (int)(y - radius), radius * 2, radius * 2);
            
            // Snout
            g2d.setColor(new Color(0, 150, 0));
            g2d.fillOval((int)(x - 10), (int)(y + 5), 20, 15);
            
            // Nostrils
            g2d.setColor(Color.BLACK);
            g2d.fillOval((int)(x - 5), (int)(y + 10), 4, 4);
            g2d.fillOval((int)(x + 5), (int)(y + 10), 4, 4);
            
            // Eyes
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int)(x - 12), (int)(y - 10), 12, 12);
            g2d.fillOval((int)(x + 2), (int)(y - 10), 12, 12);
            
            // Pupils
            g2d.setColor(Color.BLACK);
            g2d.fillOval((int)(x - 8), (int)(y - 7), 5, 5);
            g2d.fillOval((int)(x + 6), (int)(y - 7), 5, 5);
        }
    }
    
    private class Block {
        double x, y;
        int width, height;
        Color color;
        boolean destroyed = false;
        double vy = 0;
        
        public Block(double x, double y, int width, int height, Color color) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
        }
        
        public void hit() {
            vy += 2;
            if (vy > 10) {
                destroyed = true;
            }
        }
        
        public boolean collidesWith(Pig pig) {
            return pig.x + pig.radius > x && pig.x - pig.radius < x + width &&
                   pig.y + pig.radius > y && pig.y - pig.radius < y + height;
        }
        
        public void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.fillRect((int)x, (int)y, width, height);
            
            // Draw border
            g2d.setColor(Color.BLACK);
            g2d.drawRect((int)x, (int)y, width, height);
            
            // Wood grain effect
            g2d.setColor(new Color(0, 0, 0, 50));
            for (int i = 0; i < width; i += 15) {
                g2d.drawLine((int)(x + i), (int)y, (int)(x + i), (int)(y + height));
            }
        }
    }
    
    private class Slingshot {
        int x, y;
        
        public Slingshot(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        public void draw(Graphics2D g2d) {
            // Base
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillRect(x - 15, y, 30, 80);
            
            // Left arm
            g2d.fillRect(x - 40, y - 40, 10, 50);
            
            // Right arm
            g2d.fillRect(x + 30, y - 40, 10, 50);
            
            // Y fork
            g2d.fillRect(x - 40, y - 50, 80, 15);
        }
    }
}
