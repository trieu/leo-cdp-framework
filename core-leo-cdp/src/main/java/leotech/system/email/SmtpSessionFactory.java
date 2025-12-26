package leotech.system.email;


import java.util.Properties;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import leotech.system.config.ActivationChannelConfigs;
import rfx.core.util.StringUtil;

public class SmtpSessionFactory {

    public Session createSession() {
        ActivationChannelConfigs configs =
                ActivationChannelConfigs.loadLocalSmtpMailServerConfigs();

        String username = configs.getValue("smtp_username");
        String password = configs.getValue("smtp_password");

        if (StringUtil.isEmpty(username) || StringUtil.isEmpty(password)) {
            throw new IllegalStateException("SMTP credentials are missing");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", configs.getValue("smtp_host"));
        props.put("mail.smtp.port", configs.getValue("smtp_port"));
        props.put("mail.smtp.auth", configs.getValue("smtp_auth"));
        props.put("mail.smtp.starttls.enable",
                configs.getValue("smtp_starttls_enable"));

        return Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }
}
