package io.github.agentSurvivor.world;

public class WorldState {
    public float elapsed = 0f;
    public int score = 0;
    public boolean gameOver = false;
    public float delta;

    public void reset() {
        elapsed = 0f;
        score = 0;
        gameOver = false;
    }

    public void tick(float dt) {
        elapsed += dt;
    }
}
