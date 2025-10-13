package io.github.agentSurvivor.sma.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class GameBridgeAgent extends Agent {

    public static final String NAME = "game-bridge";

    @Override
    protected void setup() {
        addBehaviour(new TickerBehaviour(this, 80) {
            @Override protected void onTick() {
                String evt;
                while ((evt = SmaGateway.pollEventFromGame()) != null) {
                    ACLMessage out = new ACLMessage(ACLMessage.INFORM);
                    out.setContent(evt);
                    out.addReceiver(new AID(CoordinatorAgent.NAME, AID.ISLOCALNAME));
                    send(out);
                }
            }
        });

        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage in = myAgent.receive();
                if (in == null) { block(); return; }
                SmaGateway.emitCommandFromAgents(in.getContent());
            }
        });
    }
}
