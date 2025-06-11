import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class EnhancedCirclePong extends JPanel implements Runnable {

    // Game Constants
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 800;
    static final int GAME_AREA_RADIUS = 300;
    private static final int PADDLE_WIDTH = 15;
    private static final int PADDLE_LENGTH = 70;
    private static final int BALL_DIAMETER = 15;
    private static final double PADDLE_MOVEMENT_SPEED = 0.045;
    static final double INITIAL_BALL_SPEED = 4.0;
    public static double SPEED_INCREMENT_ON_HIT = 0.2;
    public static double MAX_BALL_SPEED = 8.0;
    public static boolean SHOW_GHOST_BALL = false;

    // Game Components
    private Ball ball;
    private Paddle leftPaddle;
    private Paddle rightPaddle;
    private AiController leftAi;
    private AiController rightAi;
    private final List<Particle> particles = new ArrayList<>();

    // Game State
    private int leftPlayerScore = 0;
    private int rightPlayerScore = 0;
    private GameMode activeGameMode = GameMode.HUMAN_VS_AI;
    private final AtomicBoolean isGameRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);

    // Player Input
    private final boolean[] keyStates = new boolean[256];

    public EnhancedCirclePong() {
        setupWindow();
        initializeGameComponents();
        startGameLoop();
    }

    private void setupWindow() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(new Color(10, 10, 20));
        setFocusable(true);
        addKeyListener(new KeyInputAdapter());
        setBorder(BorderFactory.createEmptyBorder());
    }

    private void initializeGameComponents() {
        Point gameCenter = new Point(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2);
        ball = new Ball(gameCenter, INITIAL_BALL_SPEED, BALL_DIAMETER);

        if (activeGameMode == GameMode.AI_SOLO) {
            // Solo mode: one paddle, one AI, full 360 movement
            rightPaddle = new Paddle(gameCenter, GAME_AREA_RADIUS, PADDLE_LENGTH, PADDLE_WIDTH, PADDLE_MOVEMENT_SPEED, Color.CYAN, 0, 0); // PlayerID 0 for full movement
            rightAi = new AiController(0.12, 0.95, gameCenter, GAME_AREA_RADIUS); // A responsive AI for solo play
            leftPaddle = null; // No left paddle in this mode
            leftAi = null;
            leftPlayerScore = 0;
            rightPlayerScore = 0;
        } else {
            // All other modes: two paddles are initialized
            leftPaddle = new Paddle(gameCenter, GAME_AREA_RADIUS, PADDLE_LENGTH, PADDLE_WIDTH, PADDLE_MOVEMENT_SPEED, new Color(0, 200, 255), 1, Math.PI);
            rightPaddle = new Paddle(gameCenter, GAME_AREA_RADIUS, PADDLE_LENGTH, PADDLE_WIDTH, PADDLE_MOVEMENT_SPEED, new Color(255, 80, 120), 2, 0);

            leftAi = new AiController(0.1, 0.9, gameCenter, GAME_AREA_RADIUS);
            rightAi = new AiController(0.1, 0.9, gameCenter, GAME_AREA_RADIUS);
        }
    }

    private void startGameLoop() {
        isGameRunning.set(true);
        new Thread(this).start();
    }

    @Override
    public void run() {
        final long frameTime = 1000 / 60; // 60 FPS
        while (isGameRunning.get()) {
            long startTime = System.currentTimeMillis();

            if (!isPaused.get()) {
                updateGameState();
            }
            repaint();

            long elapsedTime = System.currentTimeMillis() - startTime;
            long sleepTime = frameTime - elapsedTime;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void updateGameState() {
        handlePlayerInput();
        updateAi();
        ball.update();
        handleCollisions();
        updateParticles();
    }

    private void handlePlayerInput() {
        // Human controls only apply if the left paddle exists and is player-controlled
        if (leftPaddle != null && (activeGameMode == GameMode.TWO_HUMAN || activeGameMode == GameMode.HUMAN_VS_AI)) {
            if (keyStates[KeyEvent.VK_W]) leftPaddle.move(1);
            if (keyStates[KeyEvent.VK_S]) leftPaddle.move(-1);
        }
        if (rightPaddle != null && activeGameMode == GameMode.TWO_HUMAN) {
            if (keyStates[KeyEvent.VK_UP]) rightPaddle.move(1);
            if (keyStates[KeyEvent.VK_DOWN]) rightPaddle.move(-1);
        }
    }

    private void updateAi() {
        // AI updates are handled based on the current game mode
        switch (activeGameMode) {
            case HUMAN_VS_AI:
                if (rightAi != null) rightAi.updatePaddle(rightPaddle, ball);
                break;
            case TWO_AI:
                if (leftAi != null) leftAi.updatePaddle(leftPaddle, ball);
                if (rightAi != null) rightAi.updatePaddle(rightPaddle, ball);
                break;
            case AI_SOLO:
                if (rightAi != null) rightAi.updatePaddle(rightPaddle, ball);
                break;
            default:
                // No AI action for TWO_HUMAN mode
                break;
        }
    }

    private void handleCollisions() {
        double ballDistance = ball.getDistanceFromCenter();
        if (ballDistance >= GAME_AREA_RADIUS - ball.getSize() / 2.0) {
            boolean collisionOccurred = false;

            if (activeGameMode == GameMode.AI_SOLO) {
                // --- SOLO MODE COLLISION LOGIC ---
                if (rightPaddle.isAngleWithinPaddle(ball.getAngleFromCenter())) {
                    ball.handlePaddleCollision(rightPaddle.getAngle());
                    createCollisionParticles(ball.getX(), ball.getY());
                    rightPlayerScore++; // Increment score on successful hit
                    collisionOccurred = true;
                }
            } else {
                // --- DUAL PADDLE COLLISION LOGIC ---
                double ballAngle = ball.getAngleFromCenter();
                Paddle paddleToCheck = ball.getX() < getWidth() / 2.0 ? leftPaddle : rightPaddle;
                if (paddleToCheck != null && paddleToCheck.isAngleWithinPaddle(ballAngle)) {
                    ball.handlePaddleCollision(paddleToCheck.getAngle());
                    createCollisionParticles(ball.getX(), ball.getY());
                    // Increment score for the player who hit the ball
                    if (ball.getX() < getWidth() / 2.0) {
                        leftPlayerScore++;
                    } else {
                        rightPlayerScore++;
                    }
                    collisionOccurred = true;
                }
            }

            // --- HANDLE A MISS ---
            if (!collisionOccurred) {
                if (activeGameMode == GameMode.AI_SOLO) {
                    rightPlayerScore = 0; // Reset score on miss
                }
                else {
                    rightPlayerScore = 0;
                    leftPlayerScore = 0;
                }
                ball.reset();
            }
        }
    }

    private void createCollisionParticles(double x, double y) {
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(x, y));
        }
    }

    private void updateParticles() {
        particles.removeIf(Particle::isFaded);
        particles.forEach(Particle::update);
    }

    private void resetGame() {
        leftPlayerScore = 0;
        rightPlayerScore = 0;
        ball.reset();
        isPaused.set(false);
        // Re-initializing ensures the correct setup for the current mode
        initializeGameComponents();
    }

    private void changeGameMode(GameMode newMode) {
        activeGameMode = newMode;
        resetGame();
    }

    private void adjustAIDifficulty(double responsivenessChange, double accuracyChange) {
        // Adjust the AI that is currently active
        AiController aiToAdjust = null;
        if (activeGameMode == GameMode.AI_SOLO || activeGameMode == GameMode.HUMAN_VS_AI) {
            aiToAdjust = rightAi;
        } else if (activeGameMode == GameMode.TWO_AI) {
            aiToAdjust = rightAi; // Adjust both AIs equally
            leftAi.setAccuracy(Math.max(0.2, Math.min(1.0, leftAi.getAccuracy() + accuracyChange)));
            leftAi.setResponsiveness(Math.max(0.04, Math.min(0.25, leftAi.getResponsiveness() + responsivenessChange)));
        }

        if (aiToAdjust != null) {
            double newAccuracy = Math.max(0.2, Math.min(1.0, aiToAdjust.getAccuracy() + accuracyChange));
            double newResponsiveness = Math.max(0.04, Math.min(0.25, aiToAdjust.getResponsiveness() + responsivenessChange));
            aiToAdjust.setAccuracy(newAccuracy);
            aiToAdjust.setResponsiveness(newResponsiveness);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGameArena(g2d);
        drawGameElements(g2d);
        drawUserInterface(g2d);
    }

    private void drawGameArena(Graphics2D g2d) {
        int centerX = WINDOW_WIDTH / 2;
        int centerY = WINDOW_HEIGHT / 2;
        g2d.setColor(new Color(30, 30, 40));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(centerX - GAME_AREA_RADIUS, centerY - GAME_AREA_RADIUS, GAME_AREA_RADIUS * 2, GAME_AREA_RADIUS * 2);

        // Only draw the center line if not in solo mode
        if (activeGameMode != GameMode.AI_SOLO) {
            g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
            g2d.drawLine(centerX, centerY - GAME_AREA_RADIUS, centerX, centerY + GAME_AREA_RADIUS);
        }
    }

    private void drawGameElements(Graphics2D g2d) {
        particles.forEach(p -> p.draw(g2d));

        if (leftPaddle != null) {
            leftPaddle.draw(g2d);
        }
        if (rightPaddle != null) {
            rightPaddle.draw(g2d);
        }

        // Use the active AI to draw the ghost ball prediction
        AiController activeAi = rightAi;
        double prediction = (activeAi != null) ? activeAi.predictBallInterceptAngle(ball) : -1;
        ball.draw(g2d, prediction);

    }

    private void drawUserInterface(Graphics2D g2d) {
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 28));
        g2d.setColor(Color.WHITE);

        AiController relevantAi = (rightAi != null) ? rightAi : leftAi;

        // Draw game stats
        if (activeGameMode == GameMode.AI_SOLO) {
            String scoreText = String.format("Score: %d", rightPlayerScore);
            g2d.drawString(scoreText, 30, 40);
            g2d.drawString(String.format("Ball Speed: %.2f", ball.getSpeed()), 30, 80);
        } else {
            String leftScoreText = String.format("%s: %d", activeGameMode.getLeftPlayerName(), leftPlayerScore);
            String rightScoreText = String.format("%s: %d", activeGameMode.getRightPlayerName(), rightPlayerScore);
            g2d.drawString(leftScoreText, 30, 40);
            g2d.drawString(rightScoreText, 30, 80);
            if (relevantAi != null) {
                g2d.drawString(String.format("Ball Speed: %.2f", ball.getSpeed()), 30, 120);
            }
        }

        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        g2d.setColor(Color.YELLOW);
        g2d.drawString("Mode: " + activeGameMode.getDisplayName(), getWidth() - 220, 40);
        assert relevantAi != null;
        g2d.drawString(String.format("AI Responsiveness: %.2f", relevantAi.getResponsiveness()), getWidth() - 220, 55);
        g2d.drawString(String.format("AI Accuracy: %.2f", relevantAi.getAccuracy()), getWidth() - 220,   70);
        g2d.drawString(String.format("Max Speed: %.2f", MAX_BALL_SPEED), getWidth() - 220,   85);
        g2d.drawString(String.format("Speed Increment: %.3f", SPEED_INCREMENT_ON_HIT), getWidth() - 220 , 100);


        if (isPaused.get()) {
            // Draw pause text
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 50));
            g2d.setColor(new Color(255, 255, 255, 200));
            String pauseText = "PAUSED";
            FontMetrics metrics = g2d.getFontMetrics();
            g2d.drawString(pauseText, (getWidth() - metrics.stringWidth(pauseText)) / 2, getHeight() / 2 - 100);

            // Draw controls and settings when paused
            if (relevantAi != null) {
                g2d.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                int centerX = getWidth() / 2;
                int startY = getHeight() / 2;

                g2d.drawString("=== CONTROLS ===", centerX - 100, startY);
                g2d.drawString("W/S: Move Left Paddle", centerX - 100, startY + 25);
                g2d.drawString("↑/↓: Move Right Paddle", centerX - 100, startY + 50);
                g2d.drawString("SPACE: Pause Game", centerX - 100, startY + 75);
                g2d.drawString("R: Reset Game", centerX - 100, startY + 100);
                g2d.drawString("1-4: Change Game Mode", centerX - 100, startY + 125);

                g2d.drawString("=== AI SETTINGS ===", centerX - 100, startY + 160);
                g2d.drawString("-/+: Adjust Difficulty", centerX - 100, startY + 185);
                g2d.drawString("[/]: Adjust Max Speed", centerX - 100, startY + 210);
                g2d.drawString(",/.: Adjust Increment", centerX - 100, startY + 235);
            }
        }
    }

    private class KeyInputAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            keyStates[e.getKeyCode()] = true;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_SPACE:
                    isPaused.set(!isPaused.get());
                    break;
                case KeyEvent.VK_R:
                    resetGame();
                    break;
                case KeyEvent.VK_1:
                    changeGameMode(GameMode.HUMAN_VS_AI);
                    break;
                case KeyEvent.VK_2:
                    changeGameMode(GameMode.TWO_HUMAN);
                    break;
                case KeyEvent.VK_3:
                    changeGameMode(GameMode.TWO_AI);
                    break;
                case KeyEvent.VK_4:
                    changeGameMode(GameMode.AI_SOLO);
                    break;
                case KeyEvent.VK_MINUS:
                    adjustAIDifficulty(-0.01, -0.05); // Decrease responsiveness and accuracy
                    break;
                case KeyEvent.VK_EQUALS:
                case KeyEvent.VK_PLUS:
                    adjustAIDifficulty(0.01, 0.05); // Increase responsiveness and accuracy
                    break;
                case KeyEvent.VK_OPEN_BRACKET:
                    MAX_BALL_SPEED = MAX_BALL_SPEED - 0.05;
                    break;
                case KeyEvent.VK_CLOSE_BRACKET:
                    MAX_BALL_SPEED = MAX_BALL_SPEED + 0.05;
                    break;
                case KeyEvent.VK_COMMA:
                    SPEED_INCREMENT_ON_HIT = SPEED_INCREMENT_ON_HIT - 0.005;
                    break;
                case KeyEvent.VK_PERIOD:
                    SPEED_INCREMENT_ON_HIT = SPEED_INCREMENT_ON_HIT + 0.005;
                    break;
                case KeyEvent.VK_H:
                    SHOW_GHOST_BALL = SHOW_GHOST_BALL ? false : true;
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            keyStates[e.getKeyCode()] = false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Enhanced Circle Pong");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new EnhancedCirclePong(), BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

// ================================================================================= //
//                                HELPER CLASSES                                     //
// ================================================================================= //

/**
 * Represents the game ball, handling its movement, rendering, and collision physics.
 */
class Ball {
    private double x, y, velX, velY, speed;
    private final int size;
    private final Point center;
    private final Random random = new Random();

    public Ball(Point center, double speed, int size) {
        this.center = center;
        this.speed = speed;
        this.size = size;
        reset();
    }

    public void reset() {
        x = center.x;
        y = center.y;
        double angle = random.nextDouble() * 2 * Math.PI;
        speed = EnhancedCirclePong.INITIAL_BALL_SPEED;
        velX = Math.cos(angle) * speed;
        velY = Math.sin(angle) * speed;
    }

    public void update() {
        x += velX;
        y += velY;
    }

    public void draw(Graphics2D g2d, double predictedAngle) {
        // Draw ghost ball showing AI's predicted intercept point
        if (predictedAngle != -1 && EnhancedCirclePong.SHOW_GHOST_BALL ) {
            g2d.setColor(new Color(255, 255, 255, 60));
            double ghostX = center.x + Math.cos(predictedAngle) * EnhancedCirclePong.GAME_AREA_RADIUS;
            double ghostY = center.y + Math.sin(predictedAngle) * EnhancedCirclePong.GAME_AREA_RADIUS;
            g2d.fillOval((int) (ghostX - size / 2.0), (int) (ghostY - size / 2.0), size, size);
        }

        // Draw the actual ball
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int) (x - size / 2.0), (int) (y - size / 2.0), size, size);
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.drawOval((int) (x - size / 2.0), (int) (y - size / 2.0), size, size);
    }

    public void handlePaddleCollision(double paddleAngle) {
        double normalX = Math.cos(paddleAngle);
        double normalY = Math.sin(paddleAngle);
        double dotProduct = velX * normalX + velY * normalY;

        velX -= 2 * dotProduct * normalX;
        velY -= 2 * dotProduct * normalY;

        // Add slight randomness to the bounce
        velX += (random.nextDouble() - 0.5) * 0.2;
        velY += (random.nextDouble() - 0.5) * 0.2;

        // Increment speed on hit but cap at maximum
        speed = Math.min(EnhancedCirclePong.MAX_BALL_SPEED, speed + EnhancedCirclePong.SPEED_INCREMENT_ON_HIT);

        // Normalize speed
        double currentSpeed = Math.hypot(velX, velY);
        velX = (velX / currentSpeed) * speed;
        velY = (velY / currentSpeed) * speed;

        // Push the ball away from the boundary to prevent it getting stuck
        double distFromCenter = getDistanceFromCenter();
        if (distFromCenter > EnhancedCirclePong.GAME_AREA_RADIUS - size) {
            x = center.x + (x - center.x) * (EnhancedCirclePong.GAME_AREA_RADIUS - size) / distFromCenter;
            y = center.y + (y - center.y) * (EnhancedCirclePong.GAME_AREA_RADIUS - size) / distFromCenter;
        }
    }

    public double getDistanceFromCenter() { return Math.hypot(x - center.x, y - center.y); }
    public double getAngleFromCenter() { return Math.atan2(y - center.y, x - center.x); }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVelX() { return velX; }
    public double getVelY() { return velY; }
    public int getSize() { return size; }
    public double getSpeed() {
        return speed;
    }
}

/**
 * Represents a player's paddle, handling its movement, rendering, and collision detection.
 */
class Paddle {
    private double angle;
    private final Point center;
    private final int radius, length, width;
    private final double speed; // Max speed for human players
    private final Color color;
    private final int playerId; // 0=Solo, 1=Left, 2=Right

    public Paddle(Point center, int radius, int length, int width, double speed, Color color, int playerId, double initialAngle) {
        this.center = center;
        this.radius = radius;
        this.length = length;
        this.width = width;
        this.speed = speed;
        this.color = color;
        this.playerId = playerId;
        this.angle = initialAngle;
    }

    public void move(int direction) {
        double newAngle = angle + direction * speed;
        if (isValidMove(newAngle)) {
            angle = newAngle;
            normalizeAngle();
        }
    }

    public void moveBy(double angleDelta) {
        // Clamp the AI's movement to a maximum speed
        double maxMove = this.speed; // Allow AI to move slightly faster than players
        angleDelta = Math.max(-maxMove, Math.min(maxMove, angleDelta));

        double newAngle = angle + angleDelta;

        if (isValidMove(newAngle)) {
            angle = newAngle;
            normalizeAngle();
        }
    }

    private boolean isValidMove(double testAngle) {
        // The solo AI (player 0) has no movement restrictions
        if (playerId == 0) return true;

        double normalized = normalizeAngleStatically(testAngle);
        if (playerId == 1) { // Player 1 (left side): π/2 to 3π/2
            return normalized >= Math.PI / 2 && normalized <= 3 * Math.PI / 2;
        } else { // Player 2 (right side): 3π/2 to π/2
            return normalized >= 3 * Math.PI / 2 || normalized <= Math.PI / 2;
        }
    }

    private void normalizeAngle() {
        angle = normalizeAngleStatically(angle);
    }

    private static double normalizeAngleStatically(double ang) {
        while (ang < 0) ang += 2 * Math.PI;
        while (ang >= 2 * Math.PI) ang -= 2 * Math.PI;
        return ang;
    }

    public void draw(Graphics2D g2d) {
        double halfArcLength = length / (2.0 * radius);
        double x1 = center.x + Math.cos(angle - halfArcLength) * radius;
        double y1 = center.y + Math.sin(angle - halfArcLength) * radius;
        double x2 = center.x + Math.cos(angle + halfArcLength) * radius;
        double y2 = center.y + Math.sin(angle + halfArcLength) * radius;

        // Draw the main paddle
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);

        // Draw a subtle "breathing" glow effect
        float alpha = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.002);
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 150)));
        g2d.setStroke(new BasicStroke(width + 6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
    }

    public boolean isAngleWithinPaddle(double ballAngle) {
        double paddleArc = (double) length / radius;
        double normalizedBallAngle = normalizeAngleStatically(ballAngle);
        double normalizedPaddleAngle = normalizeAngleStatically(angle);

        double diff = Math.abs(normalizedBallAngle - normalizedPaddleAngle);
        if (diff > Math.PI) diff = 2 * Math.PI - diff; // Get the shorter angle difference
        return diff <= paddleArc / 2;
    }

    public double getAngle() { return angle; }
    public void setAngle(double angle) { this.angle = angle; }
}

/**
 * Controls an AI paddle using smooth proportional movement and stable prediction.
 */
class AiController {
    private double responsiveness; // How quickly the AI reacts (higher is faster)
    private double accuracy;       // How precise the AI is (higher is more accurate)
    private final Point center;
    private final int radius;

    // A stable error offset to prevent jitter from random calculations each frame
    private double currentInaccuracyOffset = 0.0;
    private int framesUntilNextInaccuracyCheck = 0;

    public AiController(double responsiveness, double accuracy, Point center, int radius) {
        this.responsiveness = responsiveness;
        this.accuracy = accuracy;
        this.center = center;
        this.radius = radius;
    }

    public void updatePaddle(Paddle paddle, Ball ball) {
        double predictedAngle = predictBallInterceptAngle(ball);

        if (predictedAngle != -1) {
            // --- Stable Inaccuracy Logic ---
            // Only recalculate the AI's "mistake" periodically, not every frame.
            framesUntilNextInaccuracyCheck--;
            if (framesUntilNextInaccuracyCheck <= 0) {
                if (Math.random() > this.accuracy) {
                    // The size of the error depends on the AI's accuracy level.
                    double errorMagnitude = (1.0 - this.accuracy) * 0.6;
                    this.currentInaccuracyOffset = (Math.random() - 0.5) * errorMagnitude;
                } else {
                    this.currentInaccuracyOffset = 0.0; // Perfect accuracy on this check
                }
                framesUntilNextInaccuracyCheck = 10 + (int)(Math.random() * 15); // Check again in ~150-400ms
            }

            double targetAngle = predictedAngle + this.currentInaccuracyOffset;

            // --- Proportional Movement Logic ---
            double angleDifference = targetAngle - paddle.getAngle();
            while (angleDifference > Math.PI) angleDifference -= 2 * Math.PI;
            while (angleDifference < -Math.PI) angleDifference += 2 * Math.PI;

            double moveDelta = angleDifference * this.responsiveness;
            paddle.moveBy(moveDelta);
        }
    }

    public double predictBallInterceptAngle(Ball ball) {
        double currentX = ball.getX();
        double currentY = ball.getY();
        double currentVelX = ball.getVelX();
        double currentVelY = ball.getVelY();

        for (int i = 0; i < 120; i++) {
            currentX += currentVelX;
            currentY += currentVelY;
            double dist = Math.hypot(currentX - center.x, currentY - center.y);
            if (dist >= radius - ball.getSize() / 2.0) {
                return Math.atan2(currentY - center.y, currentX - center.x);
            }
        }
        return -1; // Prediction failed
    }

    public void setResponsiveness(double responsiveness) { this.responsiveness = responsiveness; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    public double getAccuracy() { return accuracy; }
    public double getResponsiveness() { return responsiveness; }
}

/**
 * Represents a visual particle for effects like collisions.
 */
class Particle {
    private double x, y, velX, velY;
    private Color color;
    private float alpha = 1.0f;

    public Particle(double x, double y) {
        this.x = x;
        this.y = y;
        Random random = new Random();
        double angle = random.nextDouble() * 2 * Math.PI;
        double speed = random.nextDouble() * 2 + 1;
        velX = Math.cos(angle) * speed;
        velY = Math.sin(angle) * speed;
        color = new Color(255, 255, 255, 255);
    }

    public void update() {
        x += velX;
        y += velY;
        alpha -= 0.02f;
        if (alpha < 0) alpha = 0;
        color = new Color(1.0f, 1.0f, 1.0f, alpha);
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillRect((int) x, (int) y, 3, 3);
    }

    public boolean isFaded() {
        return alpha <= 0;
    }
}

/**
 * Enum for managing the different game modes and their properties.
 */
enum GameMode {
    HUMAN_VS_AI("Human vs AI", "Human", "AI"),
    TWO_HUMAN("Two Human", "Player 1", "Player 2"),
    TWO_AI("Two AI", "AI 1", "AI 2"),
    AI_SOLO("AI Solo", "AI", "AI"); // Player names aren't really used for Solo mode UI

    private final String displayName, leftPlayerName, rightPlayerName;

    GameMode(String displayName, String leftPlayerName, String rightPlayerName) {
        this.displayName = displayName;
        this.leftPlayerName = leftPlayerName;
        this.rightPlayerName = rightPlayerName;
    }

    public String getDisplayName() { return displayName; }
    public String getLeftPlayerName() { return leftPlayerName; }
    public String getRightPlayerName() { return rightPlayerName; }
}