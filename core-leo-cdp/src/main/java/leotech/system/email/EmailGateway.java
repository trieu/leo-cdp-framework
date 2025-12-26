package leotech.system.email;

import leotech.cdp.model.marketing.EmailMessage;

/**
 * Static facade for simple usage. Internally OOP, externally dead simple.
 */
public final class EmailGateway {

	// ---- singleton wiring (thread-safe, lazy) ----
	private static final EmailService EMAIL_SERVICE = buildEmailService();

	private EmailGateway() {
	}

	private static EmailService buildEmailService() {
		EmailQueuePublisher queuePublisher = new EmailQueuePublisher();

		SmtpSessionFactory sessionFactory = new SmtpSessionFactory();

		EmailMessageBuilder builder = new EmailMessageBuilder();

		SmtpEmailSender smtpSender = new SmtpEmailSender(sessionFactory, builder);

		return new EmailService(queuePublisher, smtpSender);
	}

	// ---- static API ----

	public static void enqueue(EmailMessage message) {
		EMAIL_SERVICE.enqueue(message);
	}

	public static void sendNow(EmailMessage message) {
		EMAIL_SERVICE.sendImmediately(message);
	}
}
