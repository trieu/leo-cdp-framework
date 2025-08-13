package leotech.cdp.model.analytics;

public class ComputableField implements Comparable<ComputableField> {

	String name;
	String model;
	Object result;
	
	public ComputableField() {
		
	}

	public ComputableField(String name, String model, Object result) {
		super();
		this.name = name;
		this.model = model;
		this.result = result;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(name != null) {
			return name.equals(obj);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		if(name != null) {
			return name.hashCode();
		}
		return 0;
	}

	@Override
	public int compareTo(ComputableField o) {
		int h2 = o.hashCode();
		int h1 = this.hashCode();
		if(h1 > h2) {
			return 1;
		}
		else if(h1 < h2) {
			return -1;
		}
		return 0;
	}
}
