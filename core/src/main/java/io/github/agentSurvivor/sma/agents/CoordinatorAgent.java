package io.github.agentSurvivor.sma.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class CoordinatorAgent extends Agent {

    public static final String NAME = "Coordenador dos eventos";

    public static final String ONT_EVENT = "ONT_EVENT";
    public static final String ONT_CMD   = "ONT_CMD";

    private static final AID A_GAME   = new AID(GameBridgeAgent.NAME, AID.ISLOCALNAME);
    private static final AID A_MON    = new AID(MonsterAgent.NAME,    AID.ISLOCALNAME);
    private static final AID A_MUSIC  = new AID(MusicAgent.NAME,      AID.ISLOCALNAME);
    private static final AID A_PLAYER = new AID(PlayerAgent.NAME,     AID.ISLOCALNAME);

    @Override
    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg == null) { block(); return; }

                final String content   = msg.getContent();
                final String ontology  = msg.getOntology();
                final String from      = msg.getSender() != null ? msg.getSender().getLocalName() : "?";

                if (content == null) return;

                if (ONT_CMD.equals(ontology) || content.startsWith("CMD|")) {
                    forwardCmdToGame(content);
                    logCompact("[coord] CMD → game", content, from);
                    return;
                }

                logPretty(content, from);

                String token = normalize(content);

                fanoutEventJson(content, A_MON, A_MUSIC, A_PLAYER);

                if (token.startsWith("PLAYER_DIED")) {
                    forwardCmdToGame("MUSIC|sequence");
                }
            }
        });
    }


    private void fanoutEventJson(String json, AID... receivers) {
        ACLMessage out = new ACLMessage(ACLMessage.INFORM);
        out.setOntology(ONT_EVENT);
        out.setContent(json);
        for (AID r : receivers) out.addReceiver(r);
        send(out);
    }

    private void forwardCmdToGame(String cmd) {
        ACLMessage out = new ACLMessage(ACLMessage.INFORM);
        out.setOntology(ONT_CMD);
        out.setContent(cmd);
        out.addReceiver(A_GAME);
        send(out);
    }


    private void logPretty(String content, String from) {
        if (content.startsWith("{\"type\":\"AGENT_DECISION\"")) {
            System.out.println("[agent] decisão " + content);
        } else if (content.startsWith("{\"type\":\"WORLD_SNAPSHOT\"")) {
            System.out.println("[world] " + content);
        } else if (content.contains("\"type\":\"MONSTER_SPAWNED\"")) {
            System.out.println("[Coordenador] evento: NASCIMENTO de monstro");
        } else if (content.contains("\"type\":\"MONSTER_DIED\"")) {
            System.out.println("[Coordenador] evento: MORTE de monstro");
        } else if (content.contains("\"type\":\"MUSIC_STARTED\"")) {
            System.out.println("[Coordenador] música: START " + content);
        } else if (content.contains("\"type\":\"MUSIC_STOPPED\"")) {
            System.out.println("[Coordenador] música: STOP " + content);
        } else {
            System.out.println("[" + getLocalName() + "] recebeu: " + content + " (de " + from + ")");
        }
    }

    private void logCompact(String tag, String content, String from) {
        System.out.println(tag + " | de=" + from + " | " + content);
    }

    private String normalize(String c) {
        if (c == null) return "";
        if (!c.startsWith("{")) return c;

        if (c.contains("\"type\":\"MONSTER_SPAWNED\"")) return "ENEMY_SPAWNED";
        if (c.contains("\"type\":\"MONSTER_DIED\""))   return "ENEMY_KILLED";
        if (c.contains("\"type\":\"BOSS_SPAWNED\""))   return "BOSS_SPAWNED";
        if (c.contains("\"type\":\"PLAYER_DIED\""))    return "PLAYER_DIED";
        if (c.contains("\"type\":\"PLAYER_SHOT\""))    return "PLAYER_SHOT";
        if (c.contains("\"type\":\"PLAYER_SCORED\""))  return "PLAYER_SCORED";
        if (c.contains("\"type\":\"PLAYER_UPGRADE\""))  return "PLAYER_UPGRADE";
        if (c.contains("\"type\":\"GAME_RESET\""))     return "GAME_RESET";
        if (c.contains("\"type\":\"AGENT_DECISION\"")) return "AGENT_DECISION";
        if (c.contains("\"type\":\"WORLD_SNAPSHOT\"")) return "WORLD_SNAPSHOT";
        if (c.contains("\"type\":\"MUSIC_STARTED\""))  return "MUSIC_STARTED";
        if (c.contains("\"type\":\"MUSIC_STOPPED\""))  return "MUSIC_STOPPED";
        return c;
    }
}
