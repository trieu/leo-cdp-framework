package leotech.cdp.model.marketing;

import com.google.gson.annotations.SerializedName;

public class Rule {

    @SerializedName("start")
    private String start;

    @SerializedName("conditionResult")
    private String conditionResult; // can be "true", "false", or null

    @SerializedName("end")
    private String end;

    // --- getters & setters ---

    public String getStart() {
        return start;
    }

    public String getConditionResult() {
        return conditionResult;
    }

    public String getEnd() {
        return end;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public void setConditionResult(String conditionResult) {
        this.conditionResult = conditionResult;
    }

    public void setEnd(String end) {
        this.end = end;
    }
}
