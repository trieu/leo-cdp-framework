package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import leotech.cdp.model.analytics.Notebook;

/**
 * Jupyter Notebook Management
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class NotebookManagement {

	static Map<String, Notebook> notebooks = new HashMap<String, Notebook>();
	static {
		//FIXME move to database
		
		Notebook n;
		
//		n = new Notebook("analytics", "Visual Customer Analytics");
//		n.setAccessToken("12345");
//		notebooks.put(n.getId(), n);
		
//		n = new Notebook("scoring", "Customer Scoring with RFM model");
//		n.setAccessToken("12345");
//		notebooks.put(n.getId(), n);
		
//		n = new Notebook("scoring", "Data Quality Scoring Model");
//		n.setAccessToken("12345");
//		notebooks.put(n.getId(), n);

//		n = new Notebook("processor", "Unified Customer Identity Resolution");
//		n.setAccessToken("12345");
//		notebooks.put(n.getId(), n);
	
		n = new Notebook("scoring", "Customer Lifetime Value Scoring");
		n.setAccessToken("12345");
		notebooks.put(n.getId(), n);
	}

	public static List<Notebook> getNotebooks(int startIndex, int numberResult){
		
		return new ArrayList<Notebook>(notebooks.values());
	}
	
	/**
	 * @param id
	 * @return
	 */
	public static String runAndExportToHtmlFile(String id) {
		Notebook notebook = notebooks.get(id);
		//TODO
		String nbName = notebook.getNotebookFileUri();
		String pyName = notebook.getPythonFileUri();
		String htmlName = notebook.getHtmlFileUri();
		
		String outputName = nbName.replace(".ipynb", "-output.ipynb");
		String runNbCommand = "papermill "+nbName+" output.ipynb "+nbName;
		String convertToHtmlCommand = "jupyter nbconvert --to html "+nbName;
		return htmlName;
	}
}
