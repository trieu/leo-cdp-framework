package leotech.cdp.model.marketing;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Node {

    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private String type;

    @SerializedName("label")
    private String label;

    // Optional fields depending on node type
    @SerializedName("condition")
    private String condition;

    @SerializedName("actions")
    private List<String> actions;

    // --- getters & setters ---

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public String getCondition() {
        return condition;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }
}
