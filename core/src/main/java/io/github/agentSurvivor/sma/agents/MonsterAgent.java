package io.github.agentSurvivor.sma.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;

public class MonsterAgent extends Agent {
    public static final String NAME = "monster";

    private int living = 0, totalSpawned = 0, totalDead = 0;

    @Override protected void setup() {
        // Lê eventos do jogo
        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage m = receive();
                if (m == null) { block(); return; }
                if (!CoordinatorAgent.ONT_EVENT.equals(m.getOntology())) return;

                String c = m.getContent();
                if (c.contains("\"type\":\"MONSTER_SPAWNED\"")) { living++; totalSpawned++; }
                else if (c.contains("\"type\":\"MONSTER_DIED\"")) { if (living > 0) living--; totalDead++; }
                else if (c.contains("\"type\":\"GAME_RESET\"")) { living = 0; totalSpawned = 0; totalDead = 0; requestSpawn(5); }
            }
        });

        // Mantém população mínima
        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override protected void onTick() {
                int min = 6;
                if (living < min) requestSpawn(Math.max(1, min - living));
            }
        });
    }

    private void requestSpawn(int count) {
        ACLMessage req = new ACLMessage(ACLMessage.INFORM);
        req.setOntology(CoordinatorAgent.ONT_CMD);
        req.setContent("{\"cmd\":\"REQUEST_SPAWN\",\"count\":" + count + "}");
        req.addReceiver(new AID(CoordinatorAgent.NAME, AID.ISLOCALNAME));
        send(req);
    }
}
