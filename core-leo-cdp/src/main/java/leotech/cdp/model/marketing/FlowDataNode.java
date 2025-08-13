package leotech.cdp.model.marketing;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class FlowDataNode extends FlowNode {
	
	public FlowDataNode() {
		// gson
	}

	public FlowDataNode(String id, JsonNode node) {
		super(AutomatedFlow.STR_DATA_NODE, id, node.get("label").asText());
		node.get("actions").forEach(action -> actions.add(action.asText()));
	}

	@Override
	public String execute(FlowFacts facts) {
		// TODO Implement action execution logic
		actions.forEach(action -> System.out.println("\n LoadDataNode : " + action));
		this.doActions(facts);
		facts.setResult(AutomatedFlow.STR_DATA_NODE);
		return AutomatedFlow.STR_DATA_NODE;
	}
}