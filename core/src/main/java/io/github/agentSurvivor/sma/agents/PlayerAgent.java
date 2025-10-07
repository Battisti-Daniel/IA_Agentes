package io.github.agentSurvivor.sma.agents;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;

public class PlayerAgent extends Agent {
    public static final String NAME = "player";

    private int kills = 0, score = 0;

    @Override protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage m = receive();
                if (m == null) { block(); return; }
                if (!CoordinatorAgent.ONT_EVENT.equals(m.getOntology())) return;

                String c = m.getContent();
                if (c.contains("\"type\":\"PLAYER_KILL\"")) kills++;
                if (c.contains("\"type\":\"PLAYER_SCORED\"")) {
                    int i = c.indexOf("\"score\":");
                    if (i > 0) {
                        try {
                            String n = c.substring(i + 8).replaceAll("[^0-9].*$", "");
                            score = Integer.parseInt(n);
                        } catch (Exception ignored) {}
                    }
                }
            }
        });
    }
}
