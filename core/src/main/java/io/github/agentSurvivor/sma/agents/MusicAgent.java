package io.github.agentSurvivor.sma.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

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
                    sendToGame("MUSIC|stop");
                } else if ("BOSS_SPAWNED".equals(ev)) {
                    sendToGame("MUSIC|play|finalBoss");
                } else if ("PLAYER_DIED".equals(ev)) {
                    sendToGame("MUSIC|sequence");
                }
                else if ("MUSIC_STARTED".equals(ev) || ev.contains("\"type\":\"MUSIC_STARTED\"")) {
                    System.out.println("[music] comecou a tocar: finalBoss");
                }
                else if ("MUSIC_STOPPED".equals(ev) || ev.contains("\"type\":\"MUSIC_STOPPED\"")) {
                    System.out.println("[music] musica parada: finalBoss");
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
