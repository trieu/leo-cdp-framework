package leotech.cdp.model.marketing;

import java.util.ArrayList;
import java.util.List;

import org.mvel2.MVEL;
import org.mvel2.templates.TemplateRuntime;

import com.google.gson.Gson;

import rfx.core.util.StringUtil;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public abstract class FlowNode {
	protected String id;
	protected String label;
	protected String type;

	protected String name = "", description = "";
	protected int priority = 1;
	
	protected String condition = "true";

	protected List<String> actions = new ArrayList<>();
	
	public FlowNode() {
		// gson
	}

	public FlowNode(String type, String id, String label) {
		this.type = type;
		this.id = id;
		this.label = label;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}
	

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public int getPriority() {
		return priority;
	}

	public String getCondition() {
		return condition;
	}

	public List<String> getActions() {
		return actions;
	}
	

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * @param facts
	 */
	public String execute(FlowFacts facts) {
		// skip
		return null;
	}

	/**
	 * @param facts
	 * @param conditionResult
	 * @return
	 */
	public boolean evaluate(FlowFacts facts, String conditionResult) {
		return true;
	}

	protected boolean checkCondition(FlowFacts facts, String conditionResult) {
		try {
			if (StringUtil.isNotEmpty(condition) && StringUtil.isNotEmpty(conditionResult)) {
				
				String outputTemplate = (String) TemplateRuntime.eval(condition, facts.getContextMap());
				System.out.println(" checkCondition outputTemplate "+outputTemplate);
				Object eval = MVEL.eval(outputTemplate, facts.getContextMap());
				facts.setResult(eval);
			}
			return facts.getContext().checkConditionResult(conditionResult);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	protected void doActions(FlowFacts facts) {
		try {
			for (String action : actions) {
				if (StringUtil.isNotEmpty(action)) {
					String outputTemplate = (String) TemplateRuntime.eval(action, facts.getContextMap());
					MVEL.eval(outputTemplate, facts.getContextMap());
				}
			}
			System.out.println("context.getResult " + facts.getContext().getResult());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
	@Override
	public int hashCode() {
		return String.valueOf(id).hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return this.hashCode() == obj.hashCode();
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public void setActions(List<String> actions) {
		this.actions = actions;
	}
	
	
}
