package leotech.cdp.model.journey;

public final class JourneyNode implements Comparable<JourneyNode> {
	
	int id;
	String name;
	String label;

	public JourneyNode() {

	}

	public JourneyNode(int id, String name, String label) {
		super();
		this.id = id;
		this.name = name;
		this.label = label;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public int compareTo(JourneyNode o) {
		if (this.id > o.getId()) {
			return 1;
		} else if (this.id < o.getId()) {
			return -1;
		}
		return 0;
	}

}