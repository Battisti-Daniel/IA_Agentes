package io.github.agentSurvivor.sma.agents;

import java.util.concurrent.ConcurrentLinkedQueue;

/** Ponte simples (thread-safe) entre o jogo (LibGDX) e os agentes JADE. */
public final class SmaGateway {
    private static final ConcurrentLinkedQueue<String> eventsFromGame = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<String> commandsForGame = new ConcurrentLinkedQueue<>();

    private SmaGateway() {}

    /** Jogo → Agentes: chame isso no GameScreen (já está chamando). */
    public static void emitEventFromGame(String content) { if (content != null) eventsFromGame.add(content); }
    /** Agentes → Jogo: o GameScreen consome aqui dentro do render(). */
    public static String pollCommandForGame() { return commandsForGame.poll(); }

    /* ====== Uso interno pelos agentes (bridge/coordinator) ====== */
    static String pollEventFromGame() { return eventsFromGame.poll(); }
    static void emitCommandFromAgents(String content) { if (content != null) commandsForGame.add(content); }
}
