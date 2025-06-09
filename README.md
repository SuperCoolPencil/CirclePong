# CirclePong

## Overview

CirclePong is a console-based reinterpretation of the classic Pong game, played on a circular field. Paddle move along the circumference of a circle, deflecting a ball that travels around the perimeter. 

## Controls

  * Move paddle counterclockwise: **A**
  * Move paddle clockwise: **D**


## How to Run

* Compile the C++ source code with a compiler supporting C++11 or later.
* Run the executable in a Windows console (uses `_kbhit()` and `_getch()` from `<conio.h>`).
* The game refreshes the display every 50 milliseconds.

## Gameplay

* Player controls a paddle moving on the circle’s circumference.
* The ball travels around the circle with constant angular velocity.
* Player bounces the ball back by aligning their paddle with the ball’s position.
* Missing the ball results in a score loss (optional extension).

## Notes

* This is a minimalist ASCII-based game without graphical libraries.
* The circular representation is approximate due to console character grid limitations.