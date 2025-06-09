#include <iostream>
#include <cmath>
#include <vector>
#include <chrono>
#include <thread>
#include <cstdlib>
#include <string>
#include <conio.h>
#include <windows.h>

using namespace std;

const int WIDTH = 80;
const int HEIGHT = 40;
const int centerX = WIDTH / 2;
const int centerY = HEIGHT / 2;
const double pi = 3.14159;
const double radius = 15.0;
const double paddleSize = 0.523599;

class Game {
private:
    double bx, by;
    double vx, vy;
    double paddle;
    int points;
    bool running;
    vector<string> screen;
    
    void gotoxy(int x, int y) {
        COORD pos;
        pos.X = x;
        pos.Y = y;
        SetConsoleCursorPosition(GetStdHandle(STD_OUTPUT_HANDLE), pos);
    }
    
    void hideCursor() {
        CONSOLE_CURSOR_INFO info;
        GetConsoleCursorInfo(GetStdHandle(STD_OUTPUT_HANDLE), &info);
        info.bVisible = false;
        SetConsoleCursorInfo(GetStdHandle(STD_OUTPUT_HANDLE), &info);
    }

public:
    Game() {
        bx = 0; by = 0;
        vx = 0.5; vy = 0.5;
        paddle = 0;
        points = 0;
        running = true;
        
        screen.resize(HEIGHT);
        for (int i = 0; i < HEIGHT; i++) {
            screen[i] = string(WIDTH, ' ');
        }
        hideCursor();
    }
    
    void clear() {
        for (int i = 0; i < HEIGHT; i++) {
            screen[i] = string(WIDTH, ' ');
        }
    }
    
    void put(int x, int y, char c) {
        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
            screen[y][x] = c;
        }
    }
    
    void drawCircle(int cx, int cy, int r, char c) {
        for (int angle = 0; angle < 360; angle += 3) {
            double rad = angle * pi / 180.0;
            int x = cx + (int)(r * cos(rad));
            int y = cy + (int)(r * sin(rad) * 0.5);
            put(x, y, c);
        }
    }
    
    double distance(double x, double y) {
        return sqrt(x * x + y * y);
    }
    
    bool paddleHit(double angle) {
        double diff = abs(angle - paddle);
        if (diff > pi) diff = 2 * pi - diff;
        return diff <= paddleSize / 2;
    }
    
    void updateBall() {
        bx += vx;
        by += vy;
        
        double dist = distance(bx, by);
        
        if (dist >= radius) {
            double angle = atan2(by, bx);
            if (angle < 0) angle += 2 * pi;
            
            if (paddleHit(angle)) {
                // bounce
                double nx = bx / dist;
                double ny = by / dist;
                
                double dot = vx * nx + vy * ny;
                vx -= 2 * dot * nx;
                vy -= 2 * dot * ny;
                
                // add some randomness
                vx += (rand() % 21 - 10) * 0.01;
                vy += (rand() % 21 - 10) * 0.01;
                
                bx = nx * (radius - 0.5);
                by = ny * (radius - 0.5);
                
                points++;
            } else {
                running = false;
            }
        }
    }
    
    void input() {
        if (_kbhit()) {
            char key = _getch();
            if (key == 'a' || key == 'A') {
                paddle -= 0.15;
                if (paddle < 0) paddle += 2 * pi;
            }
            if (key == 'd' || key == 'D') {
                paddle += 0.15;
                if (paddle >= 2 * pi) paddle -= 2 * pi;
            }
            if (key == 'q' || key == 'Q') {
                running = false;
            }
        }
    }
    
    void draw() {
        clear();
        
        // boundary
        drawCircle(centerX, centerY, (int)(radius * 0.5), '.');
        
        // paddle
        for (double a = paddle - paddleSize/2; a <= paddle + paddleSize/2; a += 0.05) {
            int px = centerX + (int)(radius * cos(a));
            int py = centerY + (int)(radius * sin(a) * 0.5);
            put(px, py, '#');
        }
        
        // ball
        int ballX = centerX + (int)bx;
        int ballY = centerY + (int)(by * 0.5);
        put(ballX, ballY, 'O');
        
        // center
        put(centerX, centerY, '+');
        
        // render to screen
        gotoxy(0, 0);
        for (int i = 0; i < HEIGHT; i++) {
            cout << screen[i] << endl;
        }
        
        cout << "Score: " << points << " | A/D to move, Q to quit";
        if (!running) {
            cout << " | GAME OVER!";
        }
    }
    
    void run() {
        draw();
        
        while (running) {
            input();
            updateBall();
            draw();
            
            this_thread::sleep_for(chrono::milliseconds(80));
        }
        
        gotoxy(0, HEIGHT + 2);
        cout << "\nPress any key to exit..." << endl;
        _getch();
    }
};

int main() {
    system("mode 82,45");
    system("title Circular Pong");
    
    cout << "Circular Pong Game" << endl;
    cout << "Use A and D to move paddle" << endl;
    cout << "Keep the ball bouncing!" << endl;
    cout << "Press any key to start..." << endl;
    
    char dummy;
    cin >> dummy;
    
    Game game;
    game.run();
    
    return 0;
}