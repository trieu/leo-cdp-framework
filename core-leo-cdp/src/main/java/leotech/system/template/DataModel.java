package leotech.system.template;

/**
 * the base model for HttpProcessor implementations
 * 
 * @author trieu
 *
 */
public interface DataModel {	
	/**
	 * free closable resources, better for JVM GC
	 */
	public void freeResource();
	
	/**
	 * @return String the class-path of implemented class 
	 */
	public String getClasspath();	
	
	/**
	 * @return true if the model can be processed and rendered to text
	 */
	public boolean isOutputable();
	
	public static String getClasspath(DataModel e){
		String classpath = e.getClass().getName();
		//System.out.println("---buildClasspath "+classpath);
		return classpath;
	}
	
	public static String getClasspath(Class<?> e){
		return e.getName();
	}
	
}
