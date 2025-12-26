package leotech.system.email;

import leotech.cdp.model.marketing.EmailMessage;

public class EmailService {

	private final EmailQueuePublisher queuePublisher;
	private final SmtpEmailSender smtpSender;

	public EmailService(EmailQueuePublisher queuePublisher, SmtpEmailSender smtpSender) {
		this.queuePublisher = queuePublisher;
		this.smtpSender = smtpSender;
	}

	public void sendImmediately(EmailMessage message) {
		smtpSender.send(message);
	}

	public void enqueue(EmailMessage message) {
		queuePublisher.publish(message);
	}
}
