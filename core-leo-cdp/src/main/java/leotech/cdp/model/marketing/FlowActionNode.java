package leotech.cdp.model.marketing;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class FlowActionNode extends FlowNode {
	
	public FlowActionNode(String id, JsonNode node) {
		super(AutomatedFlow.STR_ACTION_NODE, id, node.get("label").asText());
		node.get("actions").forEach(action -> actions.add(action.asText()));
	}

	@Override
	public String execute(FlowFacts facts) {
		// TODO Implement action execution logic
		actions.forEach(action -> {
			System.out.println("\n ActionNode: " + action);
		});
		
		this.doActions(facts);
		actions.add("context.result = \"true\"");
		return AutomatedFlow.STR_ACTION_NODE;
	}

}