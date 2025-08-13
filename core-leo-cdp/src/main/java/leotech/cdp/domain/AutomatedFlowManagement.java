package leotech.cdp.domain;

import static leotech.cdp.model.marketing.AutomatedFlow.STR_ACTION_NODE;
import static leotech.cdp.model.marketing.AutomatedFlow.STR_CONDITION_NODE;
import static leotech.cdp.model.marketing.AutomatedFlow.STR_CONDITION_RESULT;
import static leotech.cdp.model.marketing.AutomatedFlow.STR_DATA_NODE;
import static leotech.cdp.model.marketing.AutomatedFlow.STR_END;
import static leotech.cdp.model.marketing.AutomatedFlow.STR_END_NODE;
import static leotech.cdp.model.marketing.AutomatedFlow.STR_FLOWCHART;
import static leotech.cdp.model.marketing.AutomatedFlow.STR_NODES;
import static leotech.cdp.model.marketing.AutomatedFlow.STR_RULES;
import static leotech.cdp.model.marketing.AutomatedFlow.STR_START;
import static leotech.cdp.model.marketing.AutomatedFlow.STR_TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import leotech.cdp.model.marketing.FlowActionNode;
import leotech.cdp.model.marketing.FlowConditionNode;
import leotech.cdp.model.marketing.FlowDataNode;
import leotech.cdp.model.marketing.FlowEndNode;
import leotech.cdp.model.marketing.FlowFacts;
import leotech.cdp.model.marketing.FlowNode;
import leotech.cdp.model.marketing.FlowRule;
import leotech.system.util.LogUtil;

/**
 * 
 * Automated Flow Management for Marketing Automation 
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class AutomatedFlowManagement {

	/**
	 * @param jsonStr
	 * @param facts
	 * @throws IOException
	 */
	public static void processFlowchartJson(String jsonStr, FlowFacts facts)  {
		// System.out.println(readFileAsString);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode rootNode = objectMapper.readTree(jsonStr.getBytes());
			// Parse nodes and rules
			String type = rootNode.get(STR_TYPE).asText().trim();
			if (type.startsWith(STR_FLOWCHART)) {
				Map<String, FlowNode> nodes = parseNodes(rootNode.get(STR_NODES));
				List<FlowRule> flowRules = parseRules(rootNode.get(STR_RULES), nodes);
				// Execute rules
				for (FlowRule flowRule : flowRules) {
					boolean goNext = flowRule.execute(facts);
					if(!goNext) {
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.logError(e.getClass(), "processFlowchartJson failed with \n " + jsonStr + " facts \n " + facts);
		}
	}

	/**
	 * @param nodesNode
	 * @return
	 */
	public static Map<String, FlowNode> parseNodes(JsonNode nodesNode) {
		System.out.println("nodesNode " + nodesNode);
		Map<String, FlowNode> nodes = new HashMap<>();
		if (nodesNode != null) {
			nodesNode.fields().forEachRemaining(entry -> {
				String id = entry.getKey();
				JsonNode node = entry.getValue();
				String type = node.get(STR_TYPE).asText();
				switch (type) {
				case STR_DATA_NODE:
					nodes.put(id, new FlowDataNode(id, node));
					break;
				case STR_CONDITION_NODE:
					nodes.put(id, new FlowConditionNode(id, node));
					break;
				case STR_ACTION_NODE:
					nodes.put(id, new FlowActionNode(id, node));
					break;
				case STR_END_NODE:
					nodes.put(id, new FlowEndNode(id, node));
					break;
				}
			});
		}
		return nodes;
	}

	/**
	 * @param rulesNode
	 * @param nodes
	 * @return
	 */
	private static List<FlowRule> parseRules(JsonNode rulesNode, Map<String, FlowNode> nodes) {
		List<FlowRule> flowRules = new ArrayList<>();
		if (rulesNode != null) {
			rulesNode.forEach(ruleNode -> {
				String startNodeId = ruleNode.get(STR_START).asText();
				FlowNode startNode = nodes.get(startNodeId);
				
				String endNodeId = ruleNode.get(STR_END).asText();
				FlowNode endNode = nodes.get(endNodeId);
				
				JsonNode conditionNode = ruleNode.get(STR_CONDITION_RESULT);
				String conditionResult = conditionNode.isNull() ? null : conditionNode.asText();
				
				if (startNode != null && endNode != null) {
					FlowRule flowRule = new FlowRule(startNode, conditionResult, endNode);
					flowRules.add(flowRule);
				}
				else {
					System.err.println(startNodeId + " startNode " + startNode);
					System.err.println(endNodeId + " endNode " + endNode);
				}
			});
		}
		return flowRules;
	}
}
