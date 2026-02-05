package leotech.cdp.model.marketing;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class FlowchartDefinition {

    @SerializedName("type")
    private String type;

    @SerializedName("nodes")
    private Map<String, Node> nodes;

    @SerializedName("rules")
    private List<Rule> rules;

    // --- getters & setters ---

    public String getType() {
        return type;
    }

    public Map<String, Node> getNodes() {
        return nodes;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setNodes(Map<String, Node> nodes) {
        this.nodes = nodes;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }
}
