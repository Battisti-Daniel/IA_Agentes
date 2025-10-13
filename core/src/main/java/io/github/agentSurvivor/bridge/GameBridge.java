package io.github.agentSurvivor.bridge;

public final class GameBridge {
    private static final GameBridge INSTANCE = new GameBridge();
    private volatile boolean agentMode = false;

    private GameBridge() {}

    public static GameBridge get() { return INSTANCE; }

    public boolean isAgentMode() { return agentMode; }
    public void setAgentMode(boolean enabled) { this.agentMode = enabled; }
}
