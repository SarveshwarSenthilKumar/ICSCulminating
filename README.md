# Angry Birds - Java Swing Version

A 2D physics-based Angry Birds clone built with Java Swing.

## Features

- **Slingshot Mechanics**: Click and drag on the slingshot to launch birds
- **Physics Simulation**: Gravity, collision detection, and momentum
- **Destructible Structures**: Blocks that can be destroyed by bird impacts
- **Pig Targets**: Eliminate all pigs to complete the level
- **Score System**: Earn points for destroying pigs
- **Multiple Birds**: Limited number of birds per level

## Controls

- **Click and Drag**: Pull back on the slingshot to aim
- **Release**: Launch the bird
- The further you pull back, the more power the launch has

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

- Click on the slingshot and drag backward to aim
- Release to launch the bird toward the structure
- Birds follow realistic physics with gravity
- Hit blocks to damage them and potentially destroy the structure
- Destroy all pigs to complete the level
- You have 5 birds to complete the level
- If you run out of birds before destroying all pigs, game over

## Scoring

- **100 points** per pig destroyed

## Requirements

- Java 8 or higher
- No external dependencies (uses only Java Swing)
