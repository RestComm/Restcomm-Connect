package org.restcomm.connect.rvd.upgrade;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ProjectUpgrader714To10 implements ProjectUpgrader {
    static final Logger logger = Logger.getLogger(ProjectUpgrader714To10.class.getName());

    public ProjectUpgrader714To10() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Stub function to upgrade directly from json string instead of JsonElement
     */
    public JsonElement upgrade(String source) {
        JsonParser parser = new JsonParser();
        JsonElement sourceRoot = parser.parse(source);

        JsonElement targetRoot = this.upgrade(sourceRoot);

        return targetRoot;
    }

    /**
     * Upgrades a ProjectState JsonElement to the next version in the version path
     */
    public JsonElement upgrade(JsonElement sourceElement) {
        JsonObject source = sourceElement.getAsJsonObject();
        JsonObject target = new JsonObject();

        // root
        target.add("lastStepId", source.get("lastStepId"));
        target.add("lastNodeId", source.get("lastNodeId"));

        // root.iface
        JsonObject t = new JsonObject();
        t.addProperty("activeNode", 0);
        target.add("iface", t);

        // root.header
        t = new JsonObject();
        t.addProperty("projectKind", "voice"); // only voice project in rvd714
        t.addProperty("version", "1.0");
        t.add("startNodeName", source.get("startNodeName") );
        target.add("header", t);

        // root.nodes
        JsonArray tNodes = new JsonArray();
        for ( JsonElement sourceNode : source.getAsJsonArray("nodes") ) {
            JsonObject tNode = new JsonObject();
            JsonObject s = sourceNode.getAsJsonObject();

            tNode.add("name", s.get("name"));
            tNode.add("label", s.get("label"));
            tNode.addProperty("kind", "voice"); // only voice modules supported in rvd714
            tNode.add("iface", new JsonObject()); // put nothing in there. There should be no problem...

            // root.nodes.steps
            JsonObject sourceSteps = s.getAsJsonObject("steps");
            JsonArray targetSteps = new JsonArray();
            for ( JsonElement stepNameElement : s.getAsJsonArray("stepnames") ) {
                String stepName = stepNameElement.getAsString();

                JsonElement tStep = upgradeStep( sourceSteps.get(stepName) );
                targetSteps.add( tStep );
            }
            tNode.add("steps", targetSteps);

            tNodes.add(tNode);
        }
        target.add("nodes", tNodes);

        //logger.debug(target.toString());

        return target;
    }

    private JsonElement upgradeStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        String kind =  o.get("kind").getAsString();
        if ( "say".equals(kind) ) {
            return upgradeSayStep(sourceStep);
        } else
        if ( "play".equals(kind) ) {
            return upgradePlayStep(sourceStep);
        } else
        if ( "gather".equals(kind) ) {
            return upgradeGatherStep(sourceStep);
        } else
        if ( "dial".equals(kind) ) {
            return upgradeDialStep(sourceStep);
        } else
        if ( "redirect".equals(kind) ) {
            return upgradeRedirectStep(sourceStep);
        } else
        if ( "hungup".equals(kind) ) {
            return upgradeHungupStep(sourceStep);
        } else
        if ( "externalService".equals(kind) ) {
            return upgradeExternalServiceStep(sourceStep);
        } else
        if ( "reject".equals(kind) ) {
            return upgradeRejectStep(sourceStep);
        } else
        if ( "pause".equals(kind) ) {
            return upgradePauseStep(sourceStep);
        } else
        if ( "sms".equals(kind) ) {
            return upgradeSmsStep(sourceStep);
        } else
        if ( "email".equals(kind) ) {
            return upgradeEmailStep(sourceStep);
        } else
        if ( "record".equals(kind) ) {
            return upgradeRecordStep(sourceStep);
        } else
        if ( "fax".equals(kind) ) {
            return upgradeFaxStep(sourceStep);
        }

        return sourceStep;
    }

    private JsonElement upgradeSayStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        JsonObject t = new JsonObject();

        t.add("name", o.get("name"));
        t.add("kind", o.get("kind"));
        t.add("label", o.get("label"));
        t.add("title", o.get("title"));
        t.add("phrase", o.get("phrase"));
        if ( o.get("voice") != null && !o.get("voice").isJsonNull() && !o.get("voice").getAsString().equals("") )
            t.add("voice", o.get("voice"));
        if ( o.get("loop") != null && !o.get("loop").isJsonNull() )
            t.add("loop", o.get("loop"));
        if ( o.get("language") != null && !o.get("language").isJsonNull() && !o.get("language").getAsString().equals("") )
            t.add("language", o.get("language"));
        t.add("iface", new JsonObject());

        return t;
    }

    private JsonElement upgradePlayStep(JsonElement sourceSay) {
        JsonObject o = sourceSay.getAsJsonObject();
        JsonObject t = new JsonObject();

        t.add("name", o.get("name"));
        t.add("kind", o.get("kind"));
        t.add("label", o.get("label"));
        t.add("title", o.get("title"));
        if ( o.get("loop") != null && !o.get("loop").isJsonNull() )
            t.add("loop", o.get("loop"));
        t.add("playType", o.get("playType"));

        String wavUrl = "";
        if ( o.get("wavUrl") != null && o.get("wavUrl").getAsJsonPrimitive().isString() )
            wavUrl = o.get("wavUrl").getAsJsonPrimitive().getAsString();
        JsonObject remote = new JsonObject();
        remote.addProperty("wavUrl", wavUrl);
        t.add("remote", remote);

        String wavLocalFilename = "";
        if ( o.get("wavLocalFilename") != null && o.get("wavLocalFilename").isJsonPrimitive() && o.get("wavLocalFilename").getAsJsonPrimitive().isString() )
            wavLocalFilename = o.get("wavLocalFilename").getAsJsonPrimitive().getAsString();
        JsonObject local = new JsonObject();
        remote.addProperty("wavLocalFilename", wavLocalFilename);
        t.add("local", local);

        t.add("iface", new JsonObject());

        return t;
    }

    private JsonElement upgradeGatherStep(JsonElement source) {
        JsonObject o = source.getAsJsonObject();
        JsonObject t = new JsonObject();

        t.add("name", o.get("name"));
        t.add("kind", o.get("kind"));
        t.add("label", o.get("label"));
        t.add("title", o.get("title"));
        t.add("method", o.get("method"));
        if ( o.get("timeout") != null && o.get("timeout").isJsonPrimitive() && o.get("timeout").getAsJsonPrimitive().isNumber() )
            t.add("timeout", o.get("timeout"));
        if ( o.get("finishOnKey") != null && !o.get("finishOnKey").isJsonNull()  && !"".equals(o.get("finishOnKey").getAsString()) )
            t.add("finishOnKey", o.get("finishOnKey"));
        if ( o.get("numDigits") != null && o.get("numDigits").isJsonPrimitive() && o.get("numDigits").getAsJsonPrimitive().isNumber() )
            t.add("numDigits", o.get("numDigits"));
        t.add("gatherType", o.get("gatherType"));

        JsonObject collectdigits = new JsonObject();
        collectdigits.add("next", o.get("next"));
        collectdigits.add("collectVariable", o.get("collectVariable"));
        collectdigits.addProperty("scope", "module");
        t.add("collectdigits", collectdigits);

        JsonObject menu = new JsonObject();
        menu.add("mappings", o.get("mappings"));
        t.add("menu", menu);

        JsonObject sourceSteps = o.getAsJsonObject("steps");
        JsonArray targetSteps = new JsonArray();
        for ( JsonElement stepNameElement : o.getAsJsonArray("stepnames") ) {
            String stepName = stepNameElement.getAsString();
            JsonElement tStep = upgradeStep( sourceSteps.get(stepName) );
            targetSteps.add( tStep );
        }
        t.add("steps", targetSteps);

        t.add("iface", new JsonObject());

        return t;
    }

    private JsonElement upgradeDialStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        JsonObject t = new JsonObject();

        t.add("name", o.get("name"));
        t.add("kind", o.get("kind"));
        t.add("label", o.get("label"));
        t.add("title", o.get("title"));

        String dialType = o.get("dialType").getAsString();
        JsonObject noun = new JsonObject();
        if ( "number".equals(dialType) ) {
            noun.addProperty("dialType","number");
            noun.add("destination", o.get("number"));
        } else
        if ( "client".equals(dialType) ) {
            noun.addProperty("dialType","client");
            noun.add("destination", o.get("client"));
        } else
        if ( "conference".equals(dialType) ) {
            noun.addProperty("dialType","conference");
            noun.add("destination", o.get("conference"));
        } else
        if ( "sipuri".equals(dialType) ) {
            noun.addProperty("dialType","sipuri");
            noun.add("destination", o.get("sipuri"));
        }
        JsonArray dialNouns = new JsonArray();
        dialNouns.add(noun);
        t.add("dialNouns", dialNouns);

        t.add("iface", new JsonObject());

        return t;
    }

    private JsonElement upgradeRedirectStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        JsonObject t = new JsonObject();

        t.add("name", o.get("name"));
        t.add("kind", o.get("kind"));
        t.add("label", o.get("label"));
        t.add("title", o.get("title"));

        String url = null;
        if ( o.get("url").isJsonPrimitive() && !"".equals(o.get("url").getAsString()) )
            url = o.get("url").getAsString();
        t.add("url", o.get(url));

        String method = null;
        if ( o.get("method") != null && o.get("method").isJsonPrimitive() && !"".equals(o.get("method").getAsString()) )
            method = o.get("method").getAsString();
        t.add("method", o.get(method));

        t.add("iface", new JsonObject());

        return t;
    }

    private JsonElement upgradeHungupStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        JsonObject t = new JsonObject();

        t.add("name", o.get("name"));
        t.add("kind", o.get("kind"));
        t.add("label", o.get("label"));
        t.add("title", o.get("title"));

        t.add("iface", new JsonObject());

        return t;
    }

    private JsonElement upgradeExternalServiceStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        JsonObject t = o;

        o.remove("nextVariable");
        t.add("iface", new JsonObject());

        return t;
    }

    private JsonElement upgradeRejectStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        JsonObject t = new JsonObject();

        t.add("name", o.get("name"));
        t.add("kind", o.get("kind"));
        t.add("label", o.get("label"));
        t.add("title", o.get("title"));

        if ( o.get("reason").isJsonPrimitive() &&  !"".equals(o.get("reason").getAsString()) )
            t.add("reason", o.get("reason"));

        t.add("iface", new JsonObject());

        return t;
    }

    private JsonElement upgradePauseStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        JsonObject t = new JsonObject();

        t.add("name", o.get("name"));
        t.add("kind", o.get("kind"));
        t.add("label", o.get("label"));
        t.add("title", o.get("title"));

        if ( o.get("length") != null && o.get("length").isJsonPrimitive() && o.get("length").getAsJsonPrimitive().isNumber() )
            t.add("length",o.get("length"));

        t.add("iface", new JsonObject());

        return t;
    }

    private JsonElement upgradeSmsStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        JsonObject t = o;

        if (t.get("next").isJsonPrimitive() && "".equals(t.get("next").getAsString()) )
            t.add("next", null);

        t.add("iface", new JsonObject());

        return t;
    }

    private JsonElement upgradeRecordStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        JsonObject t = new JsonObject();

        t.add("name", o.get("name"));
        t.add("kind", o.get("kind"));
        t.add("label", o.get("label"));
        t.add("title", o.get("title"));
        if (o.get("next") != null && o.get("next").isJsonPrimitive() && "".equals(o.get("next").getAsString()) )
            t.add("next", null);
        else
            t.add("next",o.get("next"));
        t.add("method", o.get("method"));
        if ( o.get("timeout") != null && !o.get("timeout").isJsonNull() )
            t.add("timeout", o.get("timeout"));
        if ( o.get("finishOnKey") != null && !o.get("finishOnKey").isJsonNull() )
            t.add("finishOnKey", o.get("finishOnKey"));
        if ( o.get("maxLength") != null && !o.get("maxLength").isJsonNull() )
            t.add("maxLength", o.get("maxLength"));
        if ( o.get("transcribe") != null && !o.get("transcribe").isJsonNull() )
            t.add("transcribe", o.get("transcribe"));
        if ( o.get("transcribeCallback") != null && !o.get("transcribeCallback").isJsonNull() )
            t.add("transcribeCallback", o.get("transcribeCallback"));

        // omit playbeep

        t.add("iface", new JsonObject());

        return t;
    }

    private JsonElement upgradeFaxStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        JsonObject t = o;

        if (t.get("next").isJsonPrimitive() && "".equals(t.get("next").getAsString()) )
            t.add("next", null);

        t.add("iface", new JsonObject());

        return t;
    }

    private JsonElement upgradeEmailStep(JsonElement sourceStep) {
        JsonObject o = sourceStep.getAsJsonObject();
        JsonObject t = o;

        if (t.get("next").isJsonPrimitive() && "".equals(t.get("next").getAsString()) )
            t.add("next", null);

        t.add("iface", new JsonObject());

        return t;
    }

    @Override
    public String getResultingVersion() {
        return "1.0";
    }

}
