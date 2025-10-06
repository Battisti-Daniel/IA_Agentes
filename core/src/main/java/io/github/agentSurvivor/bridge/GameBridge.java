package io.github.agentSurvivor.bridge;

import com.badlogic.gdx.math.RandomXS128;

public class GameBridge {
    private static final GameBridge I = new GameBridge();
    private boolean agentMode = false;
    private final RandomXS128 rng = new RandomXS128();

    private GameBridge() {}

    public static GameBridge get() { return I; }

    public void setAgentMode(boolean on) { agentMode = on; }

    public boolean isAgentMode() { return agentMode; }

    public RandomXS128 rng() { return rng; }
}
