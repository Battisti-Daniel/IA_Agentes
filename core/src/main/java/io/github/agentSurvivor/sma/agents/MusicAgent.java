package io.github.agentSurvivor.sma.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;

public class MusicAgent extends Agent {
    public static final String NAME = "music";

    @Override protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage m = receive();
                if (m == null) { block(); return; }
                if (!CoordinatorAgent.ONT_EVENT.equals(m.getOntology())) return;

                String c = m.getContent();
                ACLMessage out = new ACLMessage(ACLMessage.INFORM);
                out.setOntology(CoordinatorAgent.ONT_CMD);

                if (c.contains("\"type\":\"PLAYER_DIED\"")) {
                    out.setContent("{\"cmd\":\"MUSIC\",\"action\":\"sequence\"}");
                } else if (c.contains("\"type\":\"FINAL_BOSS_SPAWNED\"")) {
                out.setContent("{\"cmd\":\"MUSIC\",\"action\":\"play\",\"finalBoss\":true}");
                } else if (c.contains("\"type\":\"GAME_RESET\"")) {
                    out.setContent("{\"cmd\":\"MUSIC\",\"action\":\"resume-playlist\"}");
                } else {
                    return;
                }
                out.addReceiver(new AID(CoordinatorAgent.NAME, AID.ISLOCALNAME));
                send(out);
            }
        });
    }
}
