package leotech.system.exception;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * the exception class is used for catch all invalid data from external data sources
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class InvalidDataException extends IllegalArgumentException {
	
	List<Object> causes;

	/**
	 * 
	 */
	private static final long serialVersionUID = 251445100483323434L;


	public InvalidDataException(String msg, Object ... causes) {
		super(msg);
		this.causes = Arrays.asList(causes);
	}
	
	public InvalidDataException(String msg) {
		super(msg);
	}


	public List<Object> getCauses() {
		if(causes == null) {
			causes = new ArrayList<>();
		}
		return causes;
	}


	public void setCauses(List<Object> causes) {
		this.causes = causes;
	}
	
	@Override
	public String toString() {
		return this.getMessage();
	}

}
