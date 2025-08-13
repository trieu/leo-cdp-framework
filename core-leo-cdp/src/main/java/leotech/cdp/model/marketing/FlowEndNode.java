package leotech.cdp.model.marketing;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class FlowEndNode extends FlowNode {
	private String action;
	
	public FlowEndNode() {
		// gson
	}

	public FlowEndNode(String id, JsonNode node) {
		super(AutomatedFlow.STR_END_NODE, id, node.get("label").asText());
		this.action = node.get("actions").asText();
	}

	@Override
	public String execute(FlowFacts facts) {
		// TODO Implement end action logic
		System.out.println("Executing end action: " + action);
		this.doActions(facts);
		actions.add("context.result = \"end\"");
		return AutomatedFlow.STR_END_NODE;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}
	
	
}

