package leotech.system.email;

import javax.mail.Message;
import javax.mail.Transport;

import leotech.cdp.model.marketing.EmailMessage;

public class SmtpEmailSender {

	private final SmtpSessionFactory sessionFactory;
	private final EmailMessageBuilder messageBuilder;

	public SmtpEmailSender(SmtpSessionFactory sessionFactory, EmailMessageBuilder messageBuilder) {
		this.sessionFactory = sessionFactory;
		this.messageBuilder = messageBuilder;
	}

	public void send(EmailMessage emailMessage) {
		try {
			Message msg = messageBuilder.build(emailMessage, sessionFactory.createSession());
			Transport.send(msg);
		} catch (Exception e) {
			// in real systems: log + retry + DLQ
			throw new RuntimeException("Failed to send email", e);
		}
	}
}
