package leotech.system.communication;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.validator.routines.EmailValidator;

import leotech.cdp.model.marketing.EmailMessage;
import leotech.system.config.ActivationChannelConfigs;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.configs.RedisConfigs;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.StringUtil;

/**
 * @author Trieu Nguyen
 * @since 2020
 *
 */
public final class EmailSender {

	public static boolean isValidEmail(String email) {
		if(StringUtil.isNotEmpty(email)) {
			// create the EmailValidator instance
			EmailValidator validator = EmailValidator.getInstance();
			// check for valid email addresses using isValid method
			return validator.isValid(email);
		}
		return false;
	}

	private static final String EMAIL_CONTENT_TEXT_HTML = "text/html";

	protected static final class EmailSenderQueue {
		final public static JedisPooled pubSubQueue = RedisConfigs.load().get("pubSubQueue").getJedisClient();
		static String channel = "leocdp_email_queue";

		public static long publishToRedisPubSubQueue(EmailMessage emailMsg) {
			String message = emailMsg.toString();
			long rs = new RedisCommand<Long>(pubSubQueue) {
				@Override
				protected Long build() throws JedisException {
					return jedis.publish(channel, message);
				}
			}.execute();
			return rs;
		}
	}

	// ------ BEGIN public API Email Service ------

	public static void sendToSmtpServer(EmailMessage messageModel) {
		System.out.println("sendToSmtpServer " + messageModel );
		flushEmailToSmtpServer(messageModel);
	}

	public static void pushToEmailQueue(EmailMessage messageModel) {
		EmailSenderQueue.publishToRedisPubSubQueue(messageModel);
	}

	// ------ END public API Email Service ------

	protected static int flushEmailToSmtpServer(EmailMessage messageModel) {
		try {
			Session session = getSmtpSessionFromSystemEmailService();
			if (session != null) {
				Message message = buildMessage(messageModel, session);
				Transport.send(message);
				System.out.println("flushEmailToSmtpServer OK " + session );
				return 1;
			} else {
				System.out.println("getSmtpSessionFromSystemEmailService is NULL, skip flushEmailToSmtpServer");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	protected static Message buildMessage(EmailMessage messageModel, Session session)
			throws MessagingException, AddressException {
		Message message = new MimeMessage(session);
		message.setFrom(messageModel.getParsedSenderEmailAddress());
		message.setRecipient(Message.RecipientType.TO, messageModel.getParsedReceiverEmailAddress());
		message.setSubject(messageModel.getSubject());
		message.setContent(messageModel.getContent(), EMAIL_CONTENT_TEXT_HTML);
		return message;
	}

	protected static Session getSmtpSessionFromSystemEmailService() {
		Session session = null;

		ActivationChannelConfigs configs = ActivationChannelConfigs.loadLocalSmtpMailServerConfigs();
		String username = configs.getValue("smtp_username");
		String password = configs.getValue("smtp_password");
		System.out.println("username " + username + " password " + password);

		if (StringUtil.isNotEmpty(username) && StringUtil.isNotEmpty(password)) {
			Properties prop = new Properties();
			prop.put("mail.smtp.host", configs.getValue("smtp_host"));
			prop.put("mail.smtp.port", configs.getValue("smtp_port"));
			prop.put("mail.smtp.auth", configs.getValue("smtp_auth"));
			prop.put("mail.smtp.starttls.enable", configs.getValue("smtp_starttls_enable")); // TLS

			System.out.println(prop);
			session = Session.getInstance(prop, new javax.mail.Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			});
		}
		else {
			throw new IllegalArgumentException("IllegalArgument, username " + username + " password " + password);
		}
		return session;
	}

}
