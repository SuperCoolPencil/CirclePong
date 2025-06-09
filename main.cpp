#include <iostream>
#include <cmath>
#include <vector>
#include <chrono>
#include <thread>
#include <cstdlib>
#ifdef _WIN32
    #include <conio.h>
    #include <windows.h>
#else
    #include <termios.h>
    #include <unistd.h>
    #include <fcntl.h>
#endif

const int FIELD_SIZE = 21;
const int CENTER = FIELD_SIZE / 2;
const double PI = 3.14159265359;
const double CIRCLE_RADIUS = 8.0;
const double PADDLE_SIZE = 1.0; // Angular size in radians

class CircularPong {
private:
    double ballX, ballY;
    double ballVX, ballVY;
    double paddleAngle;
    int score;
    bool gameRunning;
    
    // Cross-platform keyboard input
    bool kbhit() {
#ifdef _WIN32
        return _kbhit();
#else
        int ch = getchar();
        if (ch != EOF) {
            ungetc(ch, stdin);
            return true;
        }
        return false;
#endif
    }
    
    char getch() {
#ifdef _WIN32
        return _getch();
#else
        return getchar();
#endif
    }
    
    void setupTerminal() {
#ifndef _WIN32
        struct termios t;
        tcgetattr(STDIN_FILENO, &t);
        t.c_lflag &= ~ICANON;
        t.c_lflag &= ~ECHO;
        tcsetattr(STDIN_FILENO, TCSANOW, &t);
        fcntl(STDIN_FILENO, F_SETFL, O_NONBLOCK);
#endif
    }
    
    void restoreTerminal() {
#ifndef _WIN32
        struct termios t;
        tcgetattr(STDIN_FILENO, &t);
        t.c_lflag |= ICANON;
        t.c_lflag |= ECHO;
        tcsetattr(STDIN_FILENO, TCSANOW, &t);
#endif
    }

public:
    CircularPong() : ballX(0), ballY(0), ballVX(0.3), ballVY(0.2), 
                     paddleAngle(0), score(0), gameRunning(true) {
        setupTerminal();
    }
    
    ~CircularPong() {
        restoreTerminal();
    }
    
    void clearScreen() {
#ifdef _WIN32
        system("cls");
#else
        system("clear");
#endif
    }
    
    double distanceFromCenter(double x, double y) {
        return sqrt(x * x + y * y);
    }
    
    bool isPaddleAtPosition(double angle) {
        double angleDiff = fabs(angle - paddleAngle);
        if (angleDiff > PI) angleDiff = 2 * PI - angleDiff;
        return angleDiff <= PADDLE_SIZE / 2;
    }
    
    void updateBall() {
        ballX += ballVX;
        ballY += ballVY;
        
        double distFromCenter = distanceFromCenter(ballX, ballY);
        
        // Check collision with circular boundary
        if (distFromCenter >= CIRCLE_RADIUS) {
            double ballAngle = atan2(ballY, ballX);
            if (ballAngle < 0) ballAngle += 2 * PI;
            
            // Check if paddle is at collision point
            if (isPaddleAtPosition(ballAngle)) {
                // Bounce off paddle
                double normalX = ballX / distFromCenter;
                double normalY = ballY / distFromCenter;
                
                double dotProduct = ballVX * normalX + ballVY * normalY;
                ballVX -= 2 * dotProduct * normalX;
                ballVY -= 2 * dotProduct * normalY;
                
                // Move ball slightly inside to prevent sticking
                ballX = normalX * (CIRCLE_RADIUS - 0.1);
                ballY = normalY * (CIRCLE_RADIUS - 0.1);
                
                score++;
            } else {
                // Game over - ball hit wall without paddle
                gameRunning = false;
            }
        }
        
        // Ball can pass through center - no collision there
    }
    
    void handleInput() {
        if (kbhit()) {
            char key = getch();
            switch (key) {
                case 'a':
                case 'A':
                    paddleAngle -= 0.2;
                    if (paddleAngle < 0) paddleAngle += 2 * PI;
                    break;
                case 'd':
                case 'D':
                    paddleAngle += 0.2;
                    if (paddleAngle >= 2 * PI) paddleAngle -= 2 * PI;
                    break;
                case 'q':
                case 'Q':
                    gameRunning = false;
                    break;
            }
        }
    }
    
    void render() {
        clearScreen();
        
        std::vector<std::vector<char>> field(FIELD_SIZE, std::vector<char>(FIELD_SIZE, ' '));
        
        // Draw paddle
        for (double a = paddleAngle - PADDLE_SIZE/2; a <= paddleAngle + PADDLE_SIZE/2; a += 0.1) {
            int px = CENTER + (int)(CIRCLE_RADIUS * cos(a));
            int py = CENTER + (int)(CIRCLE_RADIUS * sin(a));
            if (px >= 0 && px < FIELD_SIZE && py >= 0 && py < FIELD_SIZE) {
                field[py][px] = '=';
            }
        }
        
        // Draw ball
        int bx = CENTER + (int)ballX;
        int by = CENTER + (int)ballY;
        if (bx >= 0 && bx < FIELD_SIZE && by >= 0 && by < FIELD_SIZE) {
            field[by][bx] = 'O';
        }
        
        // Draw center point
        field[CENTER][CENTER] = '+';
        
        // Output field
        for (int i = 0; i < FIELD_SIZE; i++) {
            for (int j = 0; j < FIELD_SIZE; j++) {
                std::cout << field[i][j];
            }
            std::cout << std::endl;
        }
        
        std::cout << "\nScore: " << score << std::endl;
        std::cout << "Controls: A/D to move paddle, Q to quit" << std::endl;
        if (!gameRunning) {
            std::cout << "GAME OVER! Final Score: " << score << std::endl;
        }
    }
    
    void run() {
        while (gameRunning) {
            handleInput();
            updateBall();
            render();
            
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
        
        std::cout << "\nPress any key to exit..." << std::endl;
        getch();
    }
};

int main() {
    std::cout << "Circular Pong Game" << std::endl;
    std::cout << "Use A and D keys to move the paddle around the circle" << std::endl;
    std::cout << "Keep the ball bouncing to increase your score!" << std::endl;
    std::cout << "Press any key to start..." << std::endl;
    
    CircularPong game;
    char dummy;
    std::cin >> dummy;
    
    game.run();
    
    return 0;
}