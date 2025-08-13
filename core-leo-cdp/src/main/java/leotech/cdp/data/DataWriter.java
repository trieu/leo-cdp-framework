package leotech.cdp.data;

import java.util.List;

/**
 * abstract Data Persistence class
 * 
 * @author tantrieuf31
 * @since 2022
 */
public abstract class DataWriter<T> {

	public static interface CallbackProcessor<T> {
		abstract public void process(T obj);
	}

	protected boolean autoWriteData = true;

	public final boolean isAutoWriteData() {
		return autoWriteData;
	}

	public final void setAutoWriteData(boolean autoSave) {
		this.autoWriteData = autoSave;
	}

	abstract public void process(List<T> list, CallbackProcessor<T> callback);

	abstract protected void writeData(T p);
}
