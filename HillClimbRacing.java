import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * =============================================================================
 * HillClimbRacing.java — Complete Single-File Hill Climb Racing Game
 * =============================================================================
 *
 * Controls:
 *   →  / D   Accelerate (gas)
 *   ←  / A   Brake / reverse
 *   R        Restart
 *   Esc      Quit
 *
 * Gameplay:
 *   • Drive as far right as possible before fuel runs out or you flip
 *   • Collect fuel cans scattered along the terrain
 *   • Score = distance travelled in metres
 *   • Flipping upside-down ends the game
 *
 * Physics:
 *   • Per-wheel terrain collision with surface normals
 *   • Angular momentum / torque from engine and gravity
 *   • Suspension springs on each wheel
 *   • Fuel consumption proportional to throttle
 *
 * Compile & run:
 *   javac HillClimbRacing.java && java HillClimbRacing
 * =============================================================================
 */
public class HillClimbRacing extends JPanel implements ActionListener, KeyListener {

    // ── Window ────────────────────────────────────────────────────────────────
    static final int W = 1000;
    static final int H = 600;

    // ── Physics ───────────────────────────────────────────────────────────────
    static final double GRAVITY        = 0.55;
    static final double ENGINE_TORQUE  = 0.18;
    static final double BRAKE_TORQUE   = 0.12;
    static final double FRICTION       = 0.985;
    static final double ANGULAR_DRAG   = 0.92;
    static final double WHEEL_RADIUS   = 22;
    static final double SUSPENSION_K   = 0.45;   // spring stiffness
    static final double SUSPENSION_B   = 0.25;   // damping
    static final double SUSPENSION_REST= 38;      // rest length
    static final double MAX_SPEED      = 9.5;
    static final double FLIP_ANGLE     = Math.PI * 0.72; // flip if body tilted this much

    // ── Fuel ──────────────────────────────────────────────────────────────────
    static final double MAX_FUEL       = 100.0;
    static final double FUEL_BURN_RATE = 0.018;  // per throttle frame
    static final double FUEL_CAN_VALUE = 25.0;

    // ── Terrain ───────────────────────────────────────────────────────────────
    static final int TERRAIN_STEP   = 6;    // px between terrain samples
    static final int TERRAIN_AHEAD  = 3000; // px to generate ahead

    // ── Colours ───────────────────────────────────────────────────────────────
    static final Color SKY_TOP    = new Color( 30,  60, 120);
    static final Color SKY_BOT    = new Color( 80, 140, 200);
    static final Color GRASS_TOP  = new Color( 60, 160,  50);
    static final Color GRASS_MID  = new Color( 40, 120,  35);
    static final Color DIRT_TOP   = new Color(130,  90,  50);
    static final Color DIRT_BOT   = new Color( 80,  55,  30);
    static final Color COL_HUD_BG = new Color(  0,   0,   0, 160);
    static final Color COL_FUEL_G = new Color( 60, 220,  80);
    static final Color COL_FUEL_Y = new Color(230, 200,  20);
    static final Color COL_FUEL_R = new Color(220,  50,  50);
    static final Color COL_CAR    = new Color(220,  40,  40);
    static final Color COL_CABIN  = new Color(180,  30,  30);
    static final Color COL_GLASS  = new Color(160, 210, 255, 180);
    static final Color COL_WHEEL  = new Color( 30,  30,  30);
    static final Color COL_SPOKE  = new Color(180, 180, 180);
    static final Color COL_SPRING = new Color(200, 200,  60);

    // ── State ─────────────────────────────────────────────────────────────────
    // Car body (centre of mass)
    double carX, carY;
    double velX, velY;
    double carAngle;      // body rotation (radians)
    double angularVel;    // body angular velocity

    // Wheels (absolute world positions)
    double[] wheelX    = new double[2];
    double[] wheelY    = new double[2];
    double[] wheelVelY = new double[2];
    double[] wheelRot  = new double[2];   // visual rotation

    // Wheel offsets from car body centre (local space, before rotation)
    static final double[] WHL_OFF_X = {-32, 32};
    static final double[] WHL_OFF_Y = { 18, 18};

    double cameraX;
    double fuel = MAX_FUEL;
    double score = 0;      // metres
    double maxScore = 0;

    boolean gasPressed, brakePressed;
    boolean gameOver   = false;
    boolean flipped    = false;
    boolean outOfFuel  = false;

    // ── Terrain ───────────────────────────────────────────────────────────────
    // Stored as world-x → y pairs; generated on demand
    int terrainGenX = 0;
    ArrayList<int[]> terrainPoints = new ArrayList<>(); // [worldX, worldY]
    // Precomputed normals at each sample
    ArrayList<double[]> terrainNormals = new ArrayList<>();

    // ── Fuel cans ─────────────────────────────────────────────────────────────
    ArrayList<double[]> fuelCans = new ArrayList<>();  // [worldX, worldY, alive]
    Random rng = new Random(42);

    // ── Particles ─────────────────────────────────────────────────────────────
    ArrayList<Particle> particles = new ArrayList<>();

    // ── Timer ─────────────────────────────────────────────────────────────────
    javax.swing.Timer timer = new javax.swing.Timer(16, this);

    // ── Offscreen buffer ──────────────────────────────────────────────────────
    BufferedImage buffer;
    Graphics2D    bg2;

    // =========================================================================
    // Constructor
    // =========================================================================
    public HillClimbRacing() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);

        buffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        bg2    = buffer.createGraphics();
        bg2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        initGame();
        timer.start();
    }

    // =========================================================================
    // Initialisation
    // =========================================================================
    private void initGame() {
        // Generate initial terrain
        terrainPoints.clear();
        terrainNormals.clear();
        fuelCans.clear();
        particles.clear();
        terrainGenX = 0;
        generateTerrain(TERRAIN_AHEAD);

        // Place car at start (flat zone first ~300px)
        carX     = 150;
        carY     = terrainY(carX) - SUSPENSION_REST - WHEEL_RADIUS - 10;
        velX     = 0;
        velY     = 0;
        carAngle = 0;
        angularVel = 0;

        for (int i = 0; i < 2; i++) {
            wheelX[i]    = carX + WHL_OFF_X[i];
            wheelY[i]    = carY + WHL_OFF_Y[i];
            wheelVelY[i] = 0;
            wheelRot[i]  = 0;
        }

        cameraX  = 0;
        fuel     = MAX_FUEL;
        score    = 0;
        gameOver = false;
        flipped  = false;
        outOfFuel= false;
        gasPressed   = false;
        brakePressed = false;
    }

    // =========================================================================
    // Terrain Generation
    // =========================================================================
    /**
     * Generates terrain up to worldX = terrainGenX + amount.
     * First 300px is flat, then increasingly hilly.
     */
    private void generateTerrain(int amount) {
        int endX = terrainGenX + amount;
        while (terrainGenX <= endX) {
            int y;
            if (terrainGenX < 300) {
                // Flat start
                y = 400;
            } else {
                double x = terrainGenX;
                y = (int)(
                    400
                    + 70  * Math.sin(x * 0.008)
                    + 45  * Math.sin(x * 0.018 + 1.2)
                    + 25  * Math.sin(x * 0.04  + 0.5)
                    + 12  * Math.sin(x * 0.09  + 2.1)
                    + Math.min(terrainGenX / 800.0, 60)   // gradual difficulty
                    * Math.sin(x * 0.005)
                );
                y = Math.max(120, Math.min(500, y));  // clamp within screen
            }
            terrainPoints.add(new int[]{terrainGenX, y});

            // Place fuel cans every ~600–900px after start
            if (terrainGenX > 400 && terrainGenX % 700 < TERRAIN_STEP) {
                int offset = rng.nextInt(200) - 100;
                double canX = terrainGenX + offset;
                double canY = terrainY(canX) - 28;
                fuelCans.add(new double[]{canX, canY, 1}); // 1 = alive
            }

            terrainGenX += TERRAIN_STEP;
        }

        // Recompute normals for all points
        terrainNormals.clear();
        for (int i = 0; i < terrainPoints.size(); i++) {
            int prevIdx = Math.max(0, i - 1);
            int nextIdx = Math.min(terrainPoints.size()-1, i + 1);
            int[] prev = terrainPoints.get(prevIdx);
            int[] next = terrainPoints.get(nextIdx);
            double dx = next[0] - prev[0];
            double dy = next[1] - prev[1];
            double len = Math.sqrt(dx*dx + dy*dy);
            // Normal points upward (perpendicular to slope)
            terrainNormals.add(new double[]{-dy/len, dx/len});
        }
    }

    /** Interpolated terrain Y at world x. */
    double terrainY(double x) {
        // Extend generation if needed
        if (x + 200 > terrainGenX) generateTerrain(TERRAIN_AHEAD);

        int idx = (int)(x / TERRAIN_STEP);
        idx = Math.max(0, Math.min(idx, terrainPoints.size() - 2));
        int[] a = terrainPoints.get(idx);
        int[] b = terrainPoints.get(Math.min(idx+1, terrainPoints.size()-1));
        double t = (x - a[0]) / (double)(b[0] - a[0] + 0.001);
        t = Math.max(0, Math.min(1, t));
        return a[1] + (b[1] - a[1]) * t;
    }

    /** Surface normal (upward-pointing unit vector) at world x. */
    double[] terrainNormal(double x) {
        int idx = (int)(x / TERRAIN_STEP);
        idx = Math.max(0, Math.min(idx, terrainNormals.size() - 1));
        return terrainNormals.get(idx);
    }

    // =========================================================================
    // Game Loop
    // =========================================================================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            updatePhysics();
            updateFuelCans();
            updateParticles();
            checkGameOver();
            score = Math.max(score, (carX - 150) / 100.0);
            maxScore = Math.max(maxScore, score);
        }
        render();
        repaint();
    }

    // ── Physics ───────────────────────────────────────────────────────────────
    private void updatePhysics() {
        // ── Engine / brake torque ──
        boolean hasFuel = fuel > 0;
        double throttle = 0;
        if (gasPressed && hasFuel) {
            throttle = ENGINE_TORQUE;
            fuel = Math.max(0, fuel - FUEL_BURN_RATE);
            // Exhaust particles
            if (rng.nextInt(4) == 0) spawnExhaust();
        }
        if (brakePressed) throttle = -BRAKE_TORQUE;

        // Apply torque to angular velocity (gas makes car lean back)
        angularVel += throttle * 0.08;

        // ── Body gravity ──
        velY += GRAVITY;

        // ── Wheel spring suspension ──
        for (int i = 0; i < 2; i++) {
            // World position of wheel attachment (rotated local offset + body pos)
            double cos = Math.cos(carAngle);
            double sin = Math.sin(carAngle);
            double attachX = carX + WHL_OFF_X[i] * cos - WHL_OFF_Y[i] * sin;
            double attachY = carY + WHL_OFF_X[i] * sin + WHL_OFF_Y[i] * cos;

            // Desired wheel X follows attachment X
            wheelX[i] = attachX;

            // Ground contact
            double ground = terrainY(wheelX[i]);
            double groundContact = ground - WHEEL_RADIUS;

            // Suspension spring: push wheel down to ground
            double springLength = wheelY[i] - attachY;
            double springForce  = -(springLength - SUSPENSION_REST) * SUSPENSION_K;
            double dampForce    = -(wheelVelY[i] - velY) * SUSPENSION_B;
            double totalForce   = springForce + dampForce;

            wheelVelY[i] += totalForce;

            // Apply equal/opposite to car body
            velY -= totalForce * 0.5;

            // Move wheel
            wheelY[i] += wheelVelY[i];

            // Ground collision
            if (wheelY[i] > groundContact) {
                wheelY[i]    = groundContact;
                wheelVelY[i] = Math.min(0, wheelVelY[i]) * -0.3; // bounce

                // Friction: wheel on ground provides traction
                double[] normal = terrainNormal(wheelX[i]);
                if (gasPressed && hasFuel) {
                    // Drive force along surface tangent
                    velX += normal[1]  * throttle * 2.5;
                    velY += -normal[0] * throttle * 2.5;
                }
                if (brakePressed) {
                    velX *= 0.90;
                }

                // Torque from wheel grip (lean forward on gas)
                angularVel -= throttle * 0.15;
            }

            // Wheel visual rotation
            wheelRot[i] += velX * 0.045;
        }

        // ── Apply velocity ──
        velX *= FRICTION;
        velX = Math.max(-MAX_SPEED * 0.4, Math.min(MAX_SPEED, velX));
        carX += velX;
        carY += velY;

        // ── Angular ──
        angularVel *= ANGULAR_DRAG;
        carAngle   += angularVel;

        // ── Body ground collision (fallback) ──
        double bodyGround = terrainY(carX);
        if (carY > bodyGround - 15) {
            carY  = bodyGround - 15;
            velY *= -0.2;
            angularVel *= 0.7;
        }

        // ── Camera ──
        double targetCamX = carX - W * 0.35;
        cameraX += (targetCamX - cameraX) * 0.08;
        cameraX = Math.max(0, cameraX);
    }

    private void updateFuelCans() {
        for (double[] can : fuelCans) {
            if (can[2] < 0.5) continue; // collected
            double dx = carX - can[0];
            double dy = carY - can[1];
            if (Math.sqrt(dx*dx + dy*dy) < 45) {
                can[2] = 0; // collect
                fuel = Math.min(MAX_FUEL, fuel + FUEL_CAN_VALUE);
                spawnPickup(can[0], can[1]);
            }
        }
    }

    private void checkGameOver() {
        // Flipped: body angle too extreme
        double normAngle = carAngle % (Math.PI * 2);
        if (normAngle < 0) normAngle += Math.PI * 2;
        flipped = (normAngle > FLIP_ANGLE && normAngle < Math.PI * 2 - FLIP_ANGLE);

        outOfFuel = fuel <= 0 && Math.abs(velX) < 0.3;

        if (flipped || outOfFuel) {
            gameOver = true;
        }
    }

    // ── Particles ─────────────────────────────────────────────────────────────
    static class Particle {
        double x, y, vx, vy;
        int    life, maxLife;
        Color  color;
        int    size;
        Particle(double x, double y, double vx, double vy, Color c, int life, int size) {
            this.x=x; this.y=y; this.vx=vx; this.vy=vy;
            this.color=c; this.life=this.maxLife=life; this.size=size;
        }
        void update() { x+=vx; y+=vy; vy+=0.12; vx*=0.94; life--; }
        boolean dead() { return life <= 0; }
    }

    private void spawnExhaust() {
        double ex = carX - 45 * Math.cos(carAngle);
        double ey = carY - 45 * Math.sin(carAngle) + 5;
        for (int i = 0; i < 2; i++) {
            particles.add(new Particle(ex, ey,
                    -velX*0.3 + (rng.nextDouble()-0.5)*1.5,
                    -1.5 - rng.nextDouble()*1.5,
                    new Color(180,180,180, 160),
                    18 + rng.nextInt(12), 4 + rng.nextInt(4)));
        }
    }

    private void spawnPickup(double x, double y) {
        for (int i = 0; i < 18; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double s = 2 + rng.nextDouble() * 3;
            particles.add(new Particle(x, y,
                    Math.cos(a)*s, Math.sin(a)*s - 2,
                    new Color(80, 220, 80),
                    30 + rng.nextInt(20), 3 + rng.nextInt(4)));
        }
    }

    private void updateParticles() {
        particles.removeIf(p -> { p.update(); return p.dead(); });
    }

    // =========================================================================
    // Rendering
    // =========================================================================
    private void render() {
        // ── Sky gradient ──
        GradientPaint sky = new GradientPaint(0,0,SKY_TOP, 0,H,SKY_BOT);
        bg2.setPaint(sky);
        bg2.fillRect(0, 0, W, H);

        // Draw clouds
        drawClouds();

        // ── World transform ──
        bg2.translate(-cameraX, 0);

        drawTerrain();
        drawFuelCans();
        drawParticles();
        drawCar();

        // ── Reset transform ──
        bg2.translate(cameraX, 0);

        // ── HUD ──
        drawHUD();

        if (gameOver) drawGameOver();
    }

    private void drawClouds() {
        // Static decorative clouds (parallax layer - move at 20% of camera)
        bg2.setColor(new Color(255,255,255,120));
        long[] cloudSeeds = {100, 280, 490, 680, 850, 1050, 1220};
        for (long s : cloudSeeds) {
            int cx = (int)((s * 130 - cameraX * 0.15) % W);
            if (cx < -150) cx += W + 150;
            int cy = (int)(80 + (s % 7) * 22);
            int cw = 80 + (int)(s % 5) * 20;
            bg2.fillOval(cx,      cy,     cw,    40);
            bg2.fillOval(cx + 20, cy - 15, cw-20, 35);
            bg2.fillOval(cx + 40, cy + 5,  cw-30, 30);
        }
    }

    private void drawTerrain() {
        int startIdx = Math.max(0, (int)(cameraX / TERRAIN_STEP) - 2);
        int endIdx   = Math.min(terrainPoints.size()-1,
                (int)((cameraX + W) / TERRAIN_STEP) + 2);

        // ── Grass top layer ──
        bg2.setColor(GRASS_TOP);
        bg2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = startIdx; i < endIdx; i++) {
            int[] a = terrainPoints.get(i);
            int[] b = terrainPoints.get(i+1);
            bg2.drawLine(a[0], a[1], b[0], b[1]);
        }

        // ── Dirt fill below terrain ──
        Polygon dirt = new Polygon();
        dirt.addPoint((int)cameraX, H + 20);
        for (int i = startIdx; i <= endIdx && i < terrainPoints.size(); i++) {
            int[] p = terrainPoints.get(i);
            dirt.addPoint(p[0], p[1] + 3);
        }
        dirt.addPoint((int)(cameraX + W), H + 20);

        GradientPaint dirtGrad = new GradientPaint(0, 350, DIRT_TOP, 0, H+20, DIRT_BOT);
        bg2.setPaint(dirtGrad);
        bg2.fillPolygon(dirt);

        // ── Grass stripe ──
        Polygon grass = new Polygon();
        grass.addPoint((int)cameraX, H + 20);
        for (int i = startIdx; i <= endIdx && i < terrainPoints.size(); i++) {
            int[] p = terrainPoints.get(i);
            grass.addPoint(p[0], p[1] + 3);
        }
        grass.addPoint((int)(cameraX + W), H + 20);
        // Clip top 14px as grass
        GradientPaint grassGrad = new GradientPaint(0, 350, GRASS_MID, 0, 380, DIRT_TOP);
        bg2.setPaint(grassGrad);
        bg2.setStroke(new BasicStroke(14f));
        for (int i = startIdx; i < endIdx && i+1 < terrainPoints.size(); i++) {
            int[] a = terrainPoints.get(i);
            int[] b = terrainPoints.get(i+1);
            bg2.drawLine(a[0], a[1]+2, b[0], b[1]+2);
        }

        // ── Distance markers every 500m ──
        bg2.setColor(new Color(255,255,255,80));
        bg2.setFont(new Font("Monospaced", Font.BOLD, 11));
        for (int m = 0; m <= terrainGenX / 100; m += 5) {
            int markerX = 150 + m * 100;
            if (markerX < cameraX - 50 || markerX > cameraX + W + 50) continue;
            double my = terrainY(markerX);
            bg2.drawLine(markerX, (int)my - 30, markerX, (int)my);
            bg2.drawString(m/10 + "m", markerX - 8, (int)my - 35);
        }
    }

    private void drawFuelCans() {
        for (double[] can : fuelCans) {
            if (can[2] < 0.5) continue;
            int cx = (int)can[0];
            int cy = (int)can[1];
            // Can body
            bg2.setColor(new Color(220, 40, 40));
            bg2.fillRoundRect(cx - 12, cy - 18, 24, 28, 6, 6);
            // Cap
            bg2.setColor(new Color(160, 30, 30));
            bg2.fillRect(cx - 5, cy - 24, 10, 8);
            // Label stripe
            bg2.setColor(new Color(255, 200, 0));
            bg2.fillRect(cx - 12, cy - 6, 24, 6);
            // Outline
            bg2.setColor(Color.BLACK);
            bg2.setStroke(new BasicStroke(1.5f));
            bg2.drawRoundRect(cx - 12, cy - 18, 24, 28, 6, 6);
            // "F" label
            bg2.setColor(Color.WHITE);
            bg2.setFont(new Font("Arial", Font.BOLD, 10));
            bg2.drawString("FUEL", cx - 10, cy + 4);
        }
    }

    private void drawParticles() {
        for (Particle p : particles) {
            float alpha = Math.max(0f, Math.min(1f, (float)p.life / p.maxLife));
            Color c = new Color(
                    p.color.getRed()/255f,
                    p.color.getGreen()/255f,
                    p.color.getBlue()/255f, alpha);
            bg2.setColor(c);
            bg2.fillOval((int)p.x - p.size/2, (int)p.y - p.size/2, p.size, p.size);
        }
    }

    private void drawCar() {
        AffineTransform saved = bg2.getTransform();

        // ── Draw suspension springs ──
        bg2.setColor(COL_SPRING);
        bg2.setStroke(new BasicStroke(2.5f));
        for (int i = 0; i < 2; i++) {
            double cos = Math.cos(carAngle);
            double sin = Math.sin(carAngle);
            double attachX = carX + WHL_OFF_X[i]*cos - WHL_OFF_Y[i]*sin;
            double attachY = carY + WHL_OFF_X[i]*sin + WHL_OFF_Y[i]*cos;
            // Zigzag spring
            int steps = 5;
            double[] sx = new double[steps+2];
            double[] sy = new double[steps+2];
            sx[0] = attachX; sy[0] = attachY;
            sx[steps+1] = wheelX[i]; sy[steps+1] = wheelY[i];
            for (int s = 1; s <= steps; s++) {
                double t = (double)s / (steps+1);
                sx[s] = attachX + (wheelX[i]-attachX)*t + ((s%2==0)?6:-6);
                sy[s] = attachY + (wheelY[i]-attachY)*t;
            }
            for (int s = 0; s < steps+1; s++) {
                bg2.drawLine((int)sx[s],(int)sy[s],(int)sx[s+1],(int)sy[s+1]);
            }
        }

        // ── Draw wheels ──
        for (int i = 0; i < 2; i++) {
            drawWheel((int)wheelX[i], (int)wheelY[i], wheelRot[i]);
        }

        // ── Draw car body ──
        bg2.translate(carX, carY);
        bg2.rotate(carAngle);

        // Chassis base
        bg2.setColor(COL_CAR);
        bg2.fillRoundRect(-48, -22, 96, 28, 10, 10);

        // Cabin
        bg2.setColor(COL_CABIN);
        int[] cabinX = {-28, -32, 10, 24};
        int[] cabinY = {-22, -46, -46, -22};
        bg2.fillPolygon(cabinX, cabinY, 4);

        // Windscreen
        bg2.setColor(COL_GLASS);
        int[] windX = {-24, -28, 8, 20};
        int[] windY = {-24, -42, -42, -24};
        bg2.fillPolygon(windX, windY, 4);

        // Headlight
        bg2.setColor(new Color(255, 250, 180));
        bg2.fillOval(38, -16, 10, 8);
        bg2.setColor(new Color(255,255,255,100));
        bg2.fillOval(36, -14, 5, 5);

        // Exhaust pipe
        bg2.setColor(new Color(100,100,100));
        bg2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        bg2.drawLine(-42, 2, -50, 4);

        // Outline
        bg2.setColor(new Color(0,0,0,120));
        bg2.setStroke(new BasicStroke(1.5f));
        bg2.drawRoundRect(-48,-22,96,28,10,10);

        bg2.setTransform(saved);
    }

    private void drawWheel(int cx, int cy, double rot) {
        AffineTransform saved = bg2.getTransform();
        bg2.translate(cx, cy);
        bg2.rotate(rot);

        int r = (int)WHEEL_RADIUS;

        // Tyre
        bg2.setColor(COL_WHEEL);
        bg2.fillOval(-r, -r, r*2, r*2);

        // Tread pattern
        bg2.setColor(new Color(50,50,50));
        bg2.setStroke(new BasicStroke(3f));
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            bg2.drawLine(
                    (int)(Math.cos(a)*(r-5)), (int)(Math.sin(a)*(r-5)),
                    (int)(Math.cos(a)* r),    (int)(Math.sin(a)* r));
        }

        // Hub cap
        bg2.setColor(new Color(200,200,210));
        bg2.fillOval(-8, -8, 16, 16);

        // Spokes
        bg2.setColor(COL_SPOKE);
        bg2.setStroke(new BasicStroke(2f));
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2;
            bg2.drawLine(0, 0, (int)(Math.cos(a)*10), (int)(Math.sin(a)*10));
        }

        // Centre bolt
        bg2.setColor(new Color(80,80,80));
        bg2.fillOval(-3,-3,6,6);

        // Outline
        bg2.setColor(new Color(0,0,0,150));
        bg2.setStroke(new BasicStroke(2f));
        bg2.drawOval(-r, -r, r*2, r*2);

        bg2.setTransform(saved);
    }

    private void drawHUD() {
        // ── Background pill ──
        bg2.setColor(COL_HUD_BG);
        bg2.fillRoundRect(14, 14, 220, 110, 16, 16);
        bg2.setColor(new Color(255,255,255,30));
        bg2.setStroke(new BasicStroke(1f));
        bg2.drawRoundRect(14, 14, 220, 110, 16, 16);

        // ── Score ──
        bg2.setFont(new Font("Monospaced", Font.BOLD, 13));
        bg2.setColor(new Color(200,200,220));
        bg2.drawString("DISTANCE", 28, 36);
        bg2.setFont(new Font("Monospaced", Font.BOLD, 22));
        bg2.setColor(Color.WHITE);
        bg2.drawString(String.format("%.1f m", score * 10), 28, 60);

        // ── Best ──
        bg2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        bg2.setColor(new Color(255,200,60));
        bg2.drawString("BEST: " + String.format("%.1f m", maxScore * 10), 28, 78);

        // ── Fuel bar ──
        bg2.setFont(new Font("Monospaced", Font.BOLD, 11));
        bg2.setColor(new Color(200,200,220));
        bg2.drawString("FUEL", 28, 98);

        int barW = 150;
        int barH = 14;
        int barX = 75;
        int barY = 86;

        // Bar background
        bg2.setColor(new Color(40,40,40));
        bg2.fillRoundRect(barX, barY, barW, barH, 6, 6);

        // Bar fill
        double pct = fuel / MAX_FUEL;
        Color fuelCol = pct > 0.5 ? COL_FUEL_G : pct > 0.25 ? COL_FUEL_Y : COL_FUEL_R;
        if (pct < 0.2 && (System.currentTimeMillis() / 300) % 2 == 0) {
            fuelCol = new Color(255,80,80); // blink warning
        }
        int fillW = Math.max(0, (int)(barW * pct));
        bg2.setColor(fuelCol);
        bg2.fillRoundRect(barX, barY, fillW, barH, 6, 6);

        // Bar outline
        bg2.setColor(new Color(255,255,255,60));
        bg2.drawRoundRect(barX, barY, barW, barH, 6, 6);

        // ── Controls hint (top right) ──
        bg2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        bg2.setColor(new Color(255,255,255,100));
        bg2.drawString("→ GAS   ← BRAKE   R RESTART", W - 285, 30);

        // ── Speed indicator ──
        bg2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        bg2.setColor(new Color(200,200,220));
        int spd = (int)(Math.abs(velX) * 12);
        bg2.drawString("SPD " + spd + " km/h", 28, 116);
    }

    private void drawGameOver() {
        // Darken overlay
        bg2.setColor(new Color(0,0,0,160));
        bg2.fillRect(0,0,W,H);

        // Panel
        int pw = 420, ph = 240;
        int px = (W-pw)/2, py = (H-ph)/2;
        bg2.setColor(new Color(15,15,35,230));
        bg2.fillRoundRect(px, py, pw, ph, 20, 20);
        bg2.setColor(new Color(255,255,255,40));
        bg2.setStroke(new BasicStroke(2f));
        bg2.drawRoundRect(px, py, pw, ph, 20, 20);

        // Title
        String title = flipped ? "FLIPPED OVER!" : "OUT OF FUEL!";
        bg2.setFont(new Font("Monospaced", Font.BOLD, 32));
        bg2.setColor(flipped ? new Color(255,80,80) : new Color(255,180,0));
        FontMetrics fm = bg2.getFontMetrics();
        bg2.drawString(title, px + (pw - fm.stringWidth(title))/2, py + 60);

        // Score
        bg2.setFont(new Font("Monospaced", Font.BOLD, 20));
        bg2.setColor(Color.WHITE);
        String dist = String.format("Distance: %.1f m", score * 10);
        bg2.drawString(dist, px + (pw - bg2.getFontMetrics().stringWidth(dist))/2, py + 110);

        // Best
        bg2.setFont(new Font("Monospaced", Font.PLAIN, 15));
        bg2.setColor(new Color(255,200,60));
        String best = String.format("Best: %.1f m", maxScore * 10);
        bg2.drawString(best, px + (pw - bg2.getFontMetrics().stringWidth(best))/2, py + 140);

        // Restart prompt
        bg2.setFont(new Font("Monospaced", Font.BOLD, 16));
        bg2.setColor(new Color(100,220,100));
        String restart = "Press  R  to Play Again";
        bg2.drawString(restart, px + (pw - bg2.getFontMetrics().stringWidth(restart))/2, py + 200);
    }

    // =========================================================================
    // Swing paint — blit buffer
    // =========================================================================
    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(buffer, 0, 0, null);
    }

    // =========================================================================
    // KeyListener
    // =========================================================================
    @Override public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: gasPressed   = true;  break;
            case KeyEvent.VK_LEFT:  case KeyEvent.VK_A: brakePressed = true;  break;
            case KeyEvent.VK_R:
                if (gameOver) initGame();
                break;
            case KeyEvent.VK_ESCAPE: System.exit(0); break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: gasPressed   = false; break;
            case KeyEvent.VK_LEFT:  case KeyEvent.VK_A: brakePressed = false; break;
        }
    }

    // =========================================================================
    // Entry Point
    // =========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HillClimbRacing game = new HillClimbRacing();

            JFrame frame = new JFrame("Hill Climb Racing");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.addKeyListener(game);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}