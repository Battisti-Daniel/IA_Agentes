package io.github.agentSurvivor.sma.agents;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public final class SmaStarter {
    private static volatile boolean started = false;

    public static synchronized void startIfNeeded() {
        if (started) return;
        try {
            Runtime rt = Runtime.instance();
            Profile p = new ProfileImpl();
            p.setParameter(Profile.GUI, "true");
            ContainerController main = rt.createMainContainer(p);

            start(main, CoordinatorAgent.NAME, CoordinatorAgent.class.getName());
            start(main, GameBridgeAgent.NAME,  GameBridgeAgent.class.getName());
            start(main, MusicAgent.NAME,       MusicAgent.class.getName());
            start(main, PlayerAgent.NAME,     PlayerAgent.class.getName());
            start(main, MonsterAgent.NAME,     MonsterAgent.class.getName());

            started = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void start(ContainerController cc, String localName, String className) throws Exception {
        AgentController ac = cc.createNewAgent(localName, className, null);
        ac.start();
    }
}
