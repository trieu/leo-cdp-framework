package leotech.system.template;

import java.util.HashMap;
import java.util.Map;

public class MustacheServiceModel extends HashMap<String, Object> implements DataModel{
	static final String classpath = MustacheServiceModel.class.getName();
	private static final long serialVersionUID = 5072021270346354454L;
	
	public MustacheServiceModel(){}

	public MustacheServiceModel(int numberField){
		super(numberField);
	}
	
	public MustacheServiceModel(Map<String, Object> obj){
		super(obj);
	}
	
	public void set(String name, Object val){
		this.put(name, val);
	}

	@Override
	public void freeResource() {
		clear();
	}

	@Override
	public boolean isOutputable() {		
		return false;
	}

	@Override
	public String getClasspath() {
		return classpath;
	}

		
}
