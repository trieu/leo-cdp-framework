package leotech.cdp.model.marketing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.google.gson.Gson;

import rfx.core.util.StringUtil;

public final class EmailMessage {

	// EmailValidation https://blog.mailtrap.io/java-email-validation/
	private static final String regex = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";

	String senderEmailAddress;
	String receiverEmailAddress;
	String profileName;
	String profileId;
	String subject;
	String content;
	List<String> attachments = new ArrayList<>();

	public EmailMessage(String fromEmailAddress, String toEmailAddress, String profileName, String profileId,
			String subject, String content) {
		super();

		// initialize the Pattern object
		Pattern pattern = Pattern.compile(regex);

		Matcher matcher1 = pattern.matcher(fromEmailAddress);
		if (!matcher1.matches()) {
			throw new IllegalArgumentException(fromEmailAddress + " is INVALID email address ");
		}

		Matcher matcher2 = pattern.matcher(toEmailAddress);
		if (!matcher2.matches()) {
			throw new IllegalArgumentException(toEmailAddress + " is INVALID email address ");
		}

		if (StringUtil.isEmpty(content)) {
			throw new IllegalArgumentException(" EmailMessage.content must be empty string ");
		}

		this.senderEmailAddress = fromEmailAddress;
		this.receiverEmailAddress = toEmailAddress;
		this.profileName = profileName;
		this.profileId = profileId;
		this.subject = subject;
		this.content = content;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	public String getSenderEmailAddress() {
		return senderEmailAddress;
	}

	public InternetAddress getParsedSenderEmailAddress() throws AddressException {
		return new InternetAddress(senderEmailAddress);
	}

	public void setSenderEmailAddress(String senderEmailAddress) {
		this.senderEmailAddress = senderEmailAddress;
	}

	public String getReceiverEmailAddress() {
		return receiverEmailAddress;
	}

	public InternetAddress getParsedReceiverEmailAddress() throws AddressException {
		return new InternetAddress(receiverEmailAddress);
	}

	public void setReceiverEmailAddress(String receiverEmailAddress) {
		this.receiverEmailAddress = receiverEmailAddress;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public List<String> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<String> attachments) {
		this.attachments = attachments;
	}
	
	

}
