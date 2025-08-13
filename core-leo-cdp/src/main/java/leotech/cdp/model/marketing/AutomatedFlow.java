package leotech.cdp.model.marketing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

/**
 * the Automated Flow Data Model
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class AutomatedFlow {

	public static final String STR_CONTEXT = "context";
	public static final String STR_RULES = "rules";
	public static final String STR_NODES = "nodes";
	public static final String STR_TYPE = "type";
	public static final String STR_FLOWCHART = "flowchart";
	public static final String STR_END = "end";
	public static final String STR_CONDITION_RESULT = "conditionResult";
	public static final String STR_START = "start";

	public static final String STR_END_NODE = "end_node";
	public static final String STR_ACTION_NODE = "action_node";
	public static final String STR_CONDITION_NODE = "condition_node";
	public static final String STR_DATA_NODE = "data_node";

	private String type = "flowchart";
	private Map<String, FlowNode> nodes = new HashMap<>();
	private List<FlowRule> rules = new ArrayList<>();

	public AutomatedFlow() {
	}

	public AutomatedFlow(String type, Map<String, FlowNode> nodes, List<FlowRule> rules) {
		super();
		this.type = type;
		this.nodes = nodes;
		this.rules = rules;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, FlowNode> getNodes() {
		return nodes;
	}

	public void setNodes(Map<String, FlowNode> nodes) {
		this.nodes = nodes;
	}

	public List<FlowRule> getRules() {
		return rules;
	}

	public void setRules(List<FlowRule> rules) {
		this.rules = rules;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
