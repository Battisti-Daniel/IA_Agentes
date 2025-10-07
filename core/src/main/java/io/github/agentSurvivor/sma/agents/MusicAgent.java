package io.github.agentSurvivor.sma.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/** Decide música conforme eventos (boss final, morte do player, etc.). */
public class MusicAgent extends Agent {
    public static final String NAME = "music";

    @Override
    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg == null) { block(); return; }
                String ev = msg.getContent();

                if ("GAME_RESET".equals(ev)) {
                    sendToGame("MUSIC|stop");                // para qualquer música
                } else if ("BOSS_SPAWNED".equals(ev)) {
                    sendToGame("MUSIC|play|finalBoss");      // só toca no boss final real
                } else if ("PLAYER_DIED".equals(ev)) {
                    sendToGame("MUSIC|sequence");            // bridge → lastCastle
                } else if ("ENEMY_KILLED".equals(ev)) {
                    // nada por enquanto; aqui daria para aumentar volume dinamicamente, etc.
                }
            }
        });
    }

    private void sendToGame(String cmd) {
        ACLMessage out = new ACLMessage(ACLMessage.INFORM);
        out.setContent(cmd);
        out.addReceiver(new AID(GameBridgeAgent.NAME, AID.ISLOCALNAME));
        send(out);
    }
}
