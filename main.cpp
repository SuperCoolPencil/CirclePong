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
const double radius = 25.0;
const double paddleSize = 0.523599;

class Game
{
private:
    double bx, by;
    double vx, vy;
    double paddle;
    int points;
    bool running;
    bool autoPlay;
    vector<string> screen;

    void gotoxy(int x, int y)
    {
        COORD pos{ (SHORT)x, (SHORT)y };
        SetConsoleCursorPosition(GetStdHandle(STD_OUTPUT_HANDLE), pos);
    }

    void hideCursor()
    {
        CONSOLE_CURSOR_INFO info;
        GetConsoleCursorInfo(GetStdHandle(STD_OUTPUT_HANDLE), &info);
        info.bVisible = false;
        SetConsoleCursorInfo(GetStdHandle(STD_OUTPUT_HANDLE), &info);
    }

public:
    Game(bool autoFlag)
        : bx(0), by(0), vx(1), vy(1), paddle(0), points(0), running(true), autoPlay(autoFlag)
    {
        screen.resize(HEIGHT);
        for (int i = 0; i < HEIGHT; ++i)
            screen[i] = string(WIDTH, ' ');
        hideCursor();
    }

    void clear()
    {
        for (int i = 0; i < HEIGHT; ++i)
            screen[i].assign(WIDTH, ' ');
    }

    void put(int x, int y, char c)
    {
        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT)
            screen[y][x] = c;
    }

    void drawCircle(int cx, int cy, int r, char c)
    {
        for (int angle = 0; angle < 360; angle += 3)
        {
            double rad = angle * pi / 180.0;
            int x = cx + int(r * cos(rad));
            int y = cy + int(r * sin(rad) * 0.5);
            put(x, y, c);
        }
    }

    double distance(double x, double y)
    {
        return sqrt(x * x + y * y);
    }

    bool paddleHit(double angle)
    {
        double diff = fabs(angle - paddle);
        if (diff > pi) diff = 2 * pi - diff;
        if (diff <= paddleSize / 2)
        {
            vx += 0.025;
            vy += 0.025;
            return true;
        }
        return false;
    }

    void updateBall()
    {
        bx += vx;
        by += vy;
        double dist = distance(bx, by);
        if (dist >= radius)
        {
            double angle = atan2(by, bx);
            if (angle < 0) angle += 2 * pi;
            if (paddleHit(angle))
            {
                double nx = bx / dist;
                double ny = by / dist;
                double dot = vx * nx + vy * ny;
                vx -= 2 * dot * nx;
                vy -= 2 * dot * ny;
                vx += (rand() % 21 - 10) * 0.01;
                vy += (rand() % 21 - 10) * 0.01;
                bx = nx * (radius - 0.5);
                by = ny * (radius - 0.5);
                points++;
            }
            else running = false;
        }
    }

    void input()
    {
        if (autoPlay)
        {
            double angleToBall = atan2(by, bx);
            if (angleToBall < 0) angleToBall += 2 * pi;
            double jitter = ((rand() % 201) - 100) * (pi / 1800.0);
            angleToBall += jitter;
            double diff = angleToBall - paddle;
            if (diff > pi) diff -= 2 * pi;
            if (diff < -pi) diff += 2 * pi;
            if (diff < -0.05) { paddle -= 0.15; if (paddle < 0) paddle += 2 * pi; }
            else if (diff > 0.05) { paddle += 0.15; if (paddle >= 2 * pi) paddle -= 2 * pi; }
        }
        else
        {
            if (_kbhit())
            {
                char ch = _getch();
                if (ch == 'a' || ch == 'A') { paddle -= 0.15; if (paddle < 0) paddle += 2 * pi; }
                if (ch == 'd' || ch == 'D') { paddle += 0.15; if (paddle >= 2 * pi) paddle -= 2 * pi; }
                if (ch == 'q' || ch == 'Q') running = false;
            }
        }
    }

    void draw()
    {
        clear();
        drawCircle(centerX, centerY, int(radius), '_');
        for (double a = paddle - paddleSize / 2; a <= paddle + paddleSize / 2; a += 0.05)
        {
            int px = centerX + int(radius * cos(a));
            int py = centerY + int(radius * sin(a) * 0.5);
            put(px, py, '#');
        }
        int ballX = centerX + int(bx);
        int ballY = centerY + int(by * 0.5);
        put(ballX, ballY, 'O');
        put(centerX, centerY, '+');
        gotoxy(0, 0);
        for (int i = 0; i < HEIGHT; ++i)
            cout << screen[i] << '\n';
        cout << "Score: " << points;
        if (!autoPlay) cout << " | A/D to move, Q to quit";
        if (!running) cout << " | GAME OVER!";
    }

    void run()
    {
        draw();
        while (running)
        {
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

int main()
{
    system("mode 82,45");
    system("title Circular Pong");
    cout << "Circular Pong Game" << endl;
    cout << "Select mode:" << endl;
    cout << "1. Manual play" << endl;
    cout << "2. Auto play" << endl;
    int mode;
    cin >> mode;
    bool autoFlag = (mode == 2);
    cout << "Press any key to start..." << endl;
    _getch();
    Game game(autoFlag);
    game.run();
    return 0;
}
