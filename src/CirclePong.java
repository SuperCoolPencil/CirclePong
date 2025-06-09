import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CirclePong extends JPanel implements KeyListener, Runnable {
    private static final int WINDOW_SIZE = 800;
    private static final int CIRCLE_RADIUS = 300;
    private static final int PADDLE_LENGTH = 60;
    private static final int PADDLE_WIDTH = 10;
    private static final int BALL_SIZE = 12;
    private static final double PADDLE_SPEED = 0.05; // radians per frame
    private static final double BALL_SPEED = 3.0;

    private final Point center = new Point(WINDOW_SIZE / 2, WINDOW_SIZE / 2);

    // Paddle positions (in radians)
    private double paddle1Angle = 0; // Player 1 (bottom)
    private double paddle2Angle = Math.PI; // Player 2 (top)

    // Ball properties
    private double ballX, ballY;
    private double ballVelX, ballVelY;

    // Input handling
    private boolean leftPressed, rightPressed, aPressed, dPressed;

    // Game state
    private int player1Score = 0, player2Score = 0;
    private boolean gameRunning = true;
    private AtomicBoolean gameStarted = new AtomicBoolean(false);

    // Game modes
    private enum GameMode { TWO_PLAYER, SINGLE_PLAYER, AUTO_PLAY }
    private GameMode currentMode = GameMode.TWO_PLAYER;

    // AI settings
    private double aiReactionTime = 0.02; // Lower = faster AI
    private double aiAccuracy = 0.85; // 0.0 to 1.0, higher = more accurate

    public CirclePong() {
        setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        resetBall();
    }

    private void resetBall() {
        ballX = center.x;
        ballY = center.y;

        // Random initial direction
        double angle = Math.random() * 2 * Math.PI;
        ballVelX = Math.cos(angle) * BALL_SPEED;
        ballVelY = Math.sin(angle) * BALL_SPEED;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw circle boundary
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(center.x - CIRCLE_RADIUS, center.y - CIRCLE_RADIUS,
                CIRCLE_RADIUS * 2, CIRCLE_RADIUS * 2);

        // Draw center line (dashed)
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                0, new float[]{5}, 0));
        g2d.drawLine(center.x - CIRCLE_RADIUS, center.y, center.x + CIRCLE_RADIUS, center.y);

        // Draw paddles
        if (currentMode == GameMode.TWO_PLAYER) {
            drawPaddle(g2d, paddle1Angle, Color.CYAN);
        }
        drawPaddle(g2d, paddle2Angle, Color.MAGENTA);

        // Draw ball
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int)(ballX - BALL_SIZE/2), (int)(ballY - BALL_SIZE/2), BALL_SIZE, BALL_SIZE);

        // Draw scores and mode
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Player 1: " + player1Score, 20, 30);
        if (currentMode == GameMode.TWO_PLAYER) {
            g2d.drawString("Player 2: " + player2Score, 20, 60);
        } else if (currentMode == GameMode.SINGLE_PLAYER) {
            g2d.drawString("AI: " + player2Score, 20, 60);
        }

        // Draw mode indicator
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.YELLOW);
        String modeText = "";
        switch (currentMode) {
            case TWO_PLAYER: modeText = "TWO PLAYER"; break;
            case SINGLE_PLAYER: modeText = "SINGLE PLAYER"; break;
            case AUTO_PLAY: modeText = "AUTO PLAY"; break;
        }
        g2d.drawString("Mode: " + modeText, WINDOW_SIZE - 200, 30);

        // Draw instructions
        if (!gameStarted.get()) {
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.setColor(Color.YELLOW);
            if (currentMode == GameMode.TWO_PLAYER) {
                g2d.drawString("Player 2: A/D keys", 20, WINDOW_SIZE - 60);
            }
            g2d.drawString("SPACE: Start/Pause  R: Reset  M: Change Mode", 20, WINDOW_SIZE - 40);
            g2d.drawString("1: Two Player  2: Single Player  3: Auto Play", 20, WINDOW_SIZE - 20);
        }
    }

    private void drawPaddle(Graphics2D g2d, double angle, Color color) {
        // Calculate paddle position on circle circumference
        int paddleX = (int)(center.x + Math.cos(angle) * CIRCLE_RADIUS);
        int paddleY = (int)(center.y + Math.sin(angle) * CIRCLE_RADIUS);

        // Calculate paddle endpoints
        double perpAngle = angle + Math.PI / 2;
        int x1 = (int)(paddleX + Math.cos(perpAngle) * PADDLE_LENGTH / 2);
        int y1 = (int)(paddleY + Math.sin(perpAngle) * PADDLE_LENGTH / 2);
        int x2 = (int)(paddleX - Math.cos(perpAngle) * PADDLE_LENGTH / 2);
        int y2 = (int)(paddleY - Math.sin(perpAngle) * PADDLE_LENGTH / 2);

        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(PADDLE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1, y1, x2, y2);
    }

    private void updateGame() {
        if (!gameStarted.get()) return;

        // Update paddle positions
        if (currentMode == GameMode.AUTO_PLAY) {
            // AI controls both paddles
            updateAIPaddle(1);
            updateAIPaddle(2);
        } else if (currentMode == GameMode.SINGLE_PLAYER) {
            // Player 1 manual, Player 2 AI
            if (leftPressed) paddle1Angle -= PADDLE_SPEED;
            if (rightPressed) paddle1Angle += PADDLE_SPEED;
            updateAIPaddle(2);
        } else {
            // Two player mode
            if (leftPressed) paddle1Angle -= PADDLE_SPEED;
            if (rightPressed) paddle1Angle += PADDLE_SPEED;
            if (aPressed) paddle2Angle -= PADDLE_SPEED;
            if (dPressed) paddle2Angle += PADDLE_SPEED;
        }

        // Normalize angles
        paddle1Angle = normalizeAngle(paddle1Angle);
        paddle2Angle = normalizeAngle(paddle2Angle);

        // Update ball position
        ballX += ballVelX;
        ballY += ballVelY;

        // Check collision with circle boundary
        double distFromCenter = Math.sqrt(Math.pow(ballX - center.x, 2) + Math.pow(ballY - center.y, 2));

        if (distFromCenter >= CIRCLE_RADIUS - BALL_SIZE/2) {
            // Check paddle collisions
            double ballAngle = Math.atan2(ballY - center.y, ballX - center.x);
            ballAngle = normalizeAngle(ballAngle);

            boolean hitPaddle = false;

            // Check collision with paddle 1
            if (isAngleNearPaddle(ballAngle, paddle1Angle)) {
                reflectBallOffPaddle(paddle1Angle);
                hitPaddle = true;
            }
            // Check collision with paddle 2
            else if (isAngleNearPaddle(ballAngle, paddle2Angle)) {
                reflectBallOffPaddle(paddle2Angle);
                hitPaddle = true;
            }

            if (!hitPaddle) {
                // Score based on which half the ball is in
                if (ballY < center.y) {
                    player1Score++; // Ball hit top half, player 1 scores
                } else {
                    player2Score++; // Ball hit bottom half, player 2 scores
                }
                resetBall();
            }
        }
    }

    private void updateAIPaddle(int playerNum) {
        double currentPaddleAngle = (playerNum == 1) ? paddle1Angle : paddle2Angle;

        // Predict where the ball will be when it reaches the circle edge
        double predictedBallAngle = predictBallInterception();

        if (predictedBallAngle != -1) {
            // Calculate angle difference
            double angleDiff = predictedBallAngle - currentPaddleAngle;

            // Normalize angle difference to [-π, π]
            while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
            while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

            // Add some imperfection to AI movement based on accuracy setting
            if (Math.random() > aiAccuracy) {
                angleDiff += (Math.random() - 0.5) * 0.5; // Add some error
            }

            // Move paddle towards predicted position with reaction time delay
            if (Math.abs(angleDiff) > aiReactionTime) {
                double moveDirection = angleDiff > 0 ? 1 : -1;
                double moveSpeed = PADDLE_SPEED * (0.5 + 0.5 * aiAccuracy); // AI speed based on accuracy

                if (playerNum == 1) {
                    paddle1Angle += moveDirection * moveSpeed;
                } else {
                    paddle2Angle += moveDirection * moveSpeed;
                }
            }
        }
    }

    private double predictBallInterception() {
        // Simple prediction: calculate where ball will hit circle boundary
        double futureX = ballX;
        double futureY = ballY;
        double futureVelX = ballVelX;
        double futureVelY = ballVelY;

        // Simulate ball movement for several steps
        for (int i = 0; i < 100; i++) {
            futureX += futureVelX;
            futureY += futureVelY;

            double distFromCenter = Math.sqrt(Math.pow(futureX - center.x, 2) + Math.pow(futureY - center.y, 2));

            if (distFromCenter >= CIRCLE_RADIUS - BALL_SIZE/2) {
                // Ball will hit boundary here
                double interceptAngle = Math.atan2(futureY - center.y, futureX - center.x);
                return normalizeAngle(interceptAngle);
            }
        }

        return -1; // Could not predict interception
    }

    private double normalizeAngle(double angle) {
        while (angle < 0) angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
        return angle;
    }

    private boolean isAngleNearPaddle(double ballAngle, double paddleAngle) {
        double paddleArcLength = (double)PADDLE_LENGTH / CIRCLE_RADIUS;
        double diff = Math.abs(normalizeAngle(ballAngle) - normalizeAngle(paddleAngle));
        if (diff > Math.PI) diff = 2 * Math.PI - diff;
        return diff <= paddleArcLength / 2;
    }

    private void reflectBallOffPaddle(double paddleAngle) {
        // Calculate reflection vector
        double normalX = Math.cos(paddleAngle);
        double normalY = Math.sin(paddleAngle);

        // Reflect velocity vector
        double dot = ballVelX * normalX + ballVelY * normalY;
        ballVelX = ballVelX - 2 * dot * normalX;
        ballVelY = ballVelY - 2 * dot * normalY;

        // Add some randomness to make gameplay more interesting
        double randomFactor = 0.1;
        ballVelX += (Math.random() - 0.5) * randomFactor;
        ballVelY += (Math.random() - 0.5) * randomFactor;

        // Maintain ball speed
        double speed = Math.sqrt(ballVelX * ballVelX + ballVelY * ballVelY);
        ballVelX = (ballVelX / speed) * BALL_SPEED;
        ballVelY = (ballVelY / speed) * BALL_SPEED;

        // Move ball slightly away from boundary to prevent sticking
        double distFromCenter = Math.sqrt(Math.pow(ballX - center.x, 2) + Math.pow(ballY - center.y, 2));
        ballX = center.x + (ballX - center.x) * (CIRCLE_RADIUS - BALL_SIZE) / distFromCenter;
        ballY = center.y + (ballY - center.y) * (CIRCLE_RADIUS - BALL_SIZE) / distFromCenter;
    }

    @Override
    public void run() {
        while (gameRunning) {
            updateGame();
            repaint();

            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                leftPressed = true;
                break;
            case KeyEvent.VK_RIGHT:
                rightPressed = true;
                break;
            case KeyEvent.VK_A:
                aPressed = true;
                break;
            case KeyEvent.VK_D:
                dPressed = true;
                break;
            case KeyEvent.VK_SPACE:
                gameStarted.set(!gameStarted.get());
                break;
            case KeyEvent.VK_R:
                // Reset game
                player1Score = 0;
                player2Score = 0;
                resetBall();
                break;
            case KeyEvent.VK_M:
                // Cycle through game modes
                switch (currentMode) {
                    case TWO_PLAYER:
                        currentMode = GameMode.SINGLE_PLAYER;
                        break;
                    case SINGLE_PLAYER:
                        currentMode = GameMode.AUTO_PLAY;
                        break;
                    case AUTO_PLAY:
                        currentMode = GameMode.TWO_PLAYER;
                        break;
                }
                player1Score = 0;
                player2Score = 0;
                resetBall();
                break;
            case KeyEvent.VK_1:
                currentMode = GameMode.TWO_PLAYER;
                player1Score = 0;
                player2Score = 0;
                resetBall();
                break;
            case KeyEvent.VK_2:
                currentMode = GameMode.SINGLE_PLAYER;
                player1Score = 0;
                player2Score = 0;
                resetBall();
                break;
            case KeyEvent.VK_3:
                currentMode = GameMode.AUTO_PLAY;
                player1Score = 0;
                player2Score = 0;
                resetBall();
                break;
            case KeyEvent.VK_MINUS:
                // Decrease AI difficulty
                aiAccuracy = Math.max(0.1, aiAccuracy - 0.1);
                aiReactionTime = Math.min(0.1, aiReactionTime + 0.01);
                break;
            case KeyEvent.VK_EQUALS: // Plus key
                // Increase AI difficulty
                aiAccuracy = Math.min(1.0, aiAccuracy + 0.1);
                aiReactionTime = Math.max(0.005, aiReactionTime - 0.01);
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                leftPressed = false;
                break;
            case KeyEvent.VK_RIGHT:
                rightPressed = false;
                break;
            case KeyEvent.VK_A:
                aPressed = false;
                break;
            case KeyEvent.VK_D:
                dPressed = false;
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("CirclePong");
        CirclePong game = new CirclePong();

        frame.add(game);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Thread gameThread = new Thread(game);
        gameThread.start();
    }
}