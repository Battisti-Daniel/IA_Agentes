package io.github.agentSurvivor.sma.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/** PlayerAgent compatível com o novo modelo (sem ontology). */
public class PlayerAgent extends Agent {
    public static final String NAME = "player";

    @Override
    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage m = myAgent.receive();
                if (m == null) { block(); return; }
                String ev = m.getContent(); // tokens/JSON vindos do Coordinator

                // exemplo de reação simples (apenas log)
                System.out.println("[" + getLocalName() + "] recebeu: " + ev);

                // exemplo: pedir algo de volta ao jogo
                if ("PLAYER_SCORED".equals(ev)) {
                    ACLMessage cmd = new ACLMessage(ACLMessage.INFORM);
                    cmd.setContent("CMD|HUD|flash");
                    cmd.addReceiver(new AID(GameBridgeAgent.NAME, AID.ISLOCALNAME));
                    send(cmd);
                }
            }
        });
    }
}
