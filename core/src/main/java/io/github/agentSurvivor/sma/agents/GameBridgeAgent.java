package io.github.agentSurvivor.sma.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;

public class GameBridgeAgent extends Agent {
    public static final String NAME = "bridge";

    @Override protected void setup() {
        // Bombeia eventos do jogo para o Coordinator
        addBehaviour(new TickerBehaviour(this, 50) {
            @Override protected void onTick() {
                String ev;
                while ((ev = SmaGateway.pollEventForSMA()) != null) {
                    ACLMessage m = new ACLMessage(ACLMessage.INFORM);
                    m.setOntology(CoordinatorAgent.ONT_EVENT);
                    m.setContent(ev);
                    m.addReceiver(new AID(CoordinatorAgent.NAME, AID.ISLOCALNAME));
                    send(m);
                }
            }
        });

        // Recebe comandos do Coordinator e empurra para o jogo
        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage in = receive();
                if (in == null) { block(); return; }
                if (!CoordinatorAgent.ONT_CMD.equals(in.getOntology())) return;
                SmaGateway.emitCommandFromSMA(in.getContent());
            }
        });
    }
}
