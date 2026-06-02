# Hill Climb Racing - Java Swing Version

A 2D physics-based driving game built with Java Swing.

## Features

- **Arrow Key Controls**: Use arrow keys to control the car
- **Procedural Terrain**: Randomly generated hilly terrain
- **Physics Simulation**: Gravity, acceleration, friction, and rotation
- **Camera Tracking**: Camera follows the car as it progresses
- **Score System**: Track your distance traveled
- **Game Over Detection**: Detects when car falls off the terrain

## Controls

- **Right Arrow / Up Arrow**: Accelerate forward
- **Left Arrow**: Brake/Reverse
- **R**: Restart game (when game over)

## How to Run

1. Compile the game:
   ```bash
   javac HillClimbRacing.java
   ```

2. Run the game:
   ```bash
   java HillClimbRacing
   ```

## Game Mechanics

- The car follows the terrain using physics simulation
- Gravity pulls the car down
- Acceleration moves the car forward based on the terrain angle
- When in the air, you can rotate the car using acceleration/brake
- If the car falls off the screen, the game ends
- Score is based on distance traveled

## Requirements

- Java 8 or higher
- No external dependencies (uses only Java Swing)
