package leotech.system.email;



import org.apache.commons.validator.routines.EmailValidator;

import rfx.core.util.StringUtil;

public final class EmailValidatorService {

    private static final EmailValidator VALIDATOR =
            EmailValidator.getInstance();

    private EmailValidatorService() {}

    public static boolean isValid(String email) {
        return StringUtil.isNotEmpty(email) && VALIDATOR.isValid(email);
    }
}
