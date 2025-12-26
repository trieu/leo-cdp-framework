package leotech.system.email;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import leotech.cdp.model.marketing.EmailMessage;

public class EmailMessageBuilder {

	private static final String CONTENT_TYPE_HTML = "text/html";

	public Message build(EmailMessage model, Session session) throws MessagingException {

		Message msg = new MimeMessage(session);
		msg.setFrom(model.getParsedSenderEmailAddress());
		msg.setRecipient(Message.RecipientType.TO, model.getParsedReceiverEmailAddress());
		msg.setSubject(model.getSubject());
		msg.setContent(model.getContent(), CONTENT_TYPE_HTML);

		return msg;
	}
}
