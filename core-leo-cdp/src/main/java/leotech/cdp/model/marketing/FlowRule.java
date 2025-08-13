package leotech.cdp.model.marketing;

import com.google.gson.Gson;

import rfx.core.util.StringUtil;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class FlowRule {
	
	private FlowNode startNode;
	private String conditionResult;
	private FlowNode endNode;
	
	public FlowRule() {
		// gson
	}

	public FlowRule(FlowNode startNode, String conditionResult, FlowNode endNode) {
		this.startNode = startNode;
		this.conditionResult = conditionResult;
		this.endNode = endNode;
	}
	
	public boolean execute(FlowFacts facts) {
		if (startNode != null) {
			facts.updateDateTimeContextMap();
			
			String type = startNode.getType();
			boolean check = false;
			if( AutomatedFlow.STR_CONDITION_NODE.equals(type) ) {
				check = startNode.checkCondition(facts, conditionResult);
			}
			else {
				check =  StringUtil.isNotEmpty(startNode.execute(facts));
			}
			
			String startNodeResult = String.valueOf(facts.getContext().getResult());
			System.out.println(" startNodeResult " + startNodeResult  );
			
			if ( check && endNode != null) {
				endNode.execute(facts);
				return true;
			}
			else if(AutomatedFlow.STR_END_NODE.equals(endNode.getType())) {
				endNode.execute(facts);
			}
			else {
				System.out.println(" \n SKIP execute node " + endNode.getId() + " profile " + facts.getContext().getProfile());
			}
		}
		return false;
	}
	
	

	public FlowNode getStartNode() {
		return startNode;
	}

	public void setStartNode(FlowNode startNode) {
		this.startNode = startNode;
	}

	public String getConditionResult() {
		return conditionResult;
	}

	public void setConditionResult(String conditionResult) {
		this.conditionResult = conditionResult;
	}

	public FlowNode getEndNode() {
		return endNode;
	}

	public void setEndNode(FlowNode endNode) {
		this.endNode = endNode;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
