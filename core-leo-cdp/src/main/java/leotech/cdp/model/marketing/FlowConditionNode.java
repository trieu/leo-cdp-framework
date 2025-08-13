package leotech.cdp.model.marketing;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class FlowConditionNode extends FlowNode {

	public FlowConditionNode(String id, JsonNode node) {
		super(AutomatedFlow.STR_CONDITION_NODE, id, node.get("label").asText());
		this.condition = node.get("condition").asText();
	}

	@Override
	public boolean evaluate(FlowFacts facts, String conditionResult) {
		// Implement condition evaluation logic
		System.out.println("\n FlowConditionNode evaluating condition: " + condition);
		System.out.println(" facts: " + facts);
		System.out.println(" conditionResult: " + conditionResult);
		
		boolean checkCondition = this.checkCondition(facts, conditionResult);
		return checkCondition; 
	}

}
