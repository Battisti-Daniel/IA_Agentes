package io.github.agentSurvivor.bridge;

/** Stub simples só para compilar e manter o menu/hud como estão. */
public final class GameBridge {
    private static final GameBridge INSTANCE = new GameBridge();
    private volatile boolean agentMode = false;

    private GameBridge() {}

    public static GameBridge get() { return INSTANCE; }

    public boolean isAgentMode() { return agentMode; }
    public void setAgentMode(boolean enabled) { this.agentMode = enabled; }
}
