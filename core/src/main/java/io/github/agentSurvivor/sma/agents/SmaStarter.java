package io.github.agentSurvivor.sma.agents;

import jade.core.Runtime;
import jade.core.ProfileImpl;
import jade.wrapper.*;

public final class SmaStarter {
    private static boolean started = false;

    private SmaStarter(){}

    public static synchronized void startIfNeeded() {
        if (started) return;
        try {
            Runtime rt = Runtime.instance();
            ProfileImpl p = new ProfileImpl(); // usa defaults (127.0.0.1, portas padrão)
            ContainerController cc = rt.createMainContainer(p);

            cc.createNewAgent(CoordinatorAgent.NAME, CoordinatorAgent.class.getName(), null).start();
            cc.createNewAgent(GameBridgeAgent.NAME,  GameBridgeAgent.class.getName(),  null).start();
            cc.createNewAgent(MonsterAgent.NAME,     MonsterAgent.class.getName(),     null).start();
            cc.createNewAgent(PlayerAgent.NAME,      PlayerAgent.class.getName(),      null).start();
            cc.createNewAgent(MusicAgent.NAME,       MusicAgent.class.getName(),       null).start();

            started = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
