package leotech.cdp.model.customer;

import com.google.gson.Gson;

import rfx.core.util.StringUtil;

/**
 * the data container of basic contact with email, phone, firstName, lastName;
 * 
 * @author tantrieug31
 * @since 2023
 *
 */
public final class BasicContactData {

	public final String email, phone, firstName, lastName;

	public BasicContactData(String email, String phone, String firstName, String lastName) {
		super();
		this.email = email;
		this.phone = phone;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public BasicContactData() {
		super();
		this.email = "";
		this.phone = "";
		this.firstName = "";
		this.lastName = "";
	}

	public boolean hasData() {
		boolean c = StringUtil.isNotEmpty(this.email) || StringUtil.isNotEmpty(this.phone);
		c = c && (StringUtil.isNotEmpty(this.firstName) || StringUtil.isNotEmpty(this.lastName));
		return c;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
