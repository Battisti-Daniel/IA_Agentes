package io.github.agentSurvivor.sma.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class PlayerAgent extends Agent {
    public static final String NAME = "jogador";

    @Override
    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage m = myAgent.receive();
                if (m == null) { block(); return; }
                String ev = m.getContent();

                System.out.println("[" + getLocalName() + "] recebeu: " + ev);

                if (ev != null && ev.contains("\"type\":\"PLAYER_SCORED\"")) {
                    ACLMessage cmd = new ACLMessage(ACLMessage.INFORM);
                    cmd.setContent("Got a coin");
                    cmd.addReceiver(new AID(GameBridgeAgent.NAME, AID.ISLOCALNAME));
                    send(cmd);
                }
                if (ev != null && ev.contains("\"type\":\"PLAYER_UPGRADE\"")) {
                    ACLMessage cmd = new ACLMessage(ACLMessage.INFORM);
                    cmd.setContent("Update Skill");
                    cmd.addReceiver(new AID(GameBridgeAgent.NAME, AID.ISLOCALNAME));
                    send(cmd);

                    ACLMessage out = new ACLMessage(ACLMessage.INFORM);
                    out.setOntology(CoordinatorAgent.ONT_CMD);
                    out.setContent("\"cmd\":\"upgrade\"");
                    out.addReceiver(new AID(GameBridgeAgent.NAME, AID.ISLOCALNAME));
                    send(out);
                }
            }
        });
    }
}
