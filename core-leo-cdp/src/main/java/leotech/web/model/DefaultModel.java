package leotech.web.model;

import leotech.system.template.DataModel;

public class DefaultModel implements DataModel {

	static String classpath = DefaultModel.class.getName();

	@Override
	public void freeResource() {
	}

	@Override
	public String getClasspath() {
		return classpath;
	}

	@Override
	public boolean isOutputable() {
		return true;
	}

}
