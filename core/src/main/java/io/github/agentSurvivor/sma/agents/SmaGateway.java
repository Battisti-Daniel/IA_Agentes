package io.github.agentSurvivor.sma.agents;

import java.util.concurrent.LinkedBlockingQueue;

public final class SmaGateway {
    private static final LinkedBlockingQueue<String> eventsToSMA   = new LinkedBlockingQueue<>();
    private static final LinkedBlockingQueue<String> commandsToGame = new LinkedBlockingQueue<>();

    private SmaGateway(){}

    // JOGO -> SMA
    public static void emitEventFromGame(String json) { eventsToSMA.offer(json); }
    public static String pollEventForSMA() { return eventsToSMA.poll(); }

    // SMA -> JOGO
    public static void emitCommandFromSMA(String json) { commandsToGame.offer(json); }
    public static String pollCommandForGame() { return commandsToGame.poll(); }
}
