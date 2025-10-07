package io.github.agentSurvivor.sma.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;

public class CoordinatorAgent extends Agent {
    public static final String NAME = "coordinator";
    public static final String ONT_EVENT = "GAME_EVENT";
    public static final String ONT_CMD   = "GAME_CMD";

    @Override protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage msg = receive();
                if (msg == null) { block(); return; }

                if (ONT_EVENT.equals(msg.getOntology())) {
                    // Fan-out para agentes de domínio
                    broadcast(ONT_EVENT, msg.getContent(),
                        MonsterAgent.NAME, PlayerAgent.NAME, MusicAgent.NAME);
                } else if (ONT_CMD.equals(msg.getOntology())) {
                    // Comandos vão para o bridge -> jogo
                    ACLMessage out = new ACLMessage(ACLMessage.INFORM);
                    out.setOntology(ONT_CMD);
                    out.setContent(msg.getContent());
                    out.addReceiver(new AID(GameBridgeAgent.NAME, AID.ISLOCALNAME));
                    send(out);
                }
            }
        });
    }

    private void broadcast(String ontology, String content, String... targets) {
        for (String t : targets) {
            ACLMessage out = new ACLMessage(ACLMessage.INFORM);
            out.setOntology(ontology);
            out.setContent(content);
            out.addReceiver(new AID(t, AID.ISLOCALNAME));
            send(out);
        }
    }
}
