package io.github.agentSurvivor.sma.agents;

import java.util.concurrent.ConcurrentLinkedQueue;

public final class SmaGateway {
    private static final ConcurrentLinkedQueue<String> eventsFromGame = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<String> commandsForGame = new ConcurrentLinkedQueue<>();

    public static void emitEventFromGame(String content) { if (content != null) eventsFromGame.add(content); }
    public static String pollCommandForGame() { return commandsForGame.poll(); }

    static String pollEventFromGame() { return eventsFromGame.poll(); }
    static void emitCommandFromAgents(String content) { if (content != null) commandsForGame.add(content); }
}
