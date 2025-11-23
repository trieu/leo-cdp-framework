package leotech.cdp.utils;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.InternetAddress;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 * Data Validator for Profile
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class ProfileDataValidator {
	// Default to Vietnam
	public final static String DEFAULT_PHONE_REGION = SystemMetaData.DEFAULT_DATA_REGION;

	// Regular expression for valid phone numbers
	final static String phoneNumberPattern = "^\\+?\\d{1,4}?[-.\\s]?\\(?(\\d{1,3})\\)?[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}$";

	/**
	 * @param phoneNumber
	 * @return boolean
	 */
	public static boolean isPhoneNumberWithRegionCode(String phoneNumber, String regionCode) {
		if (StringUtil.isNotEmpty(phoneNumber)) {
			PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
			try {
				PhoneNumber numberProto = phoneUtil.parse(phoneNumber, regionCode);
				return phoneUtil.isValidNumber(numberProto);
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

	/**
	 * Function to validate if a given string is a phone number using regular
	 * expression
	 * 
	 * @param phoneNumber
	 * @return true if a phone number
	 */
	public static boolean isValidPhoneNumber(String phoneNumber) {
		if (StringUtil.isNotEmpty(phoneNumber)) {
			// Compile the pattern
			Pattern pattern = Pattern.compile(phoneNumberPattern);
			Matcher matcher = pattern.matcher(phoneNumber);

			// Return true if the phone number matches the pattern, false otherwise
			return matcher.matches();
		}
		return false;
	}

	/**
	 * @param email
	 * @return boolean
	 */
	public static boolean isValidEmail(String email) {
		boolean result = true;
		if (StringUtil.isNotEmpty(email)) {
			try {
				InternetAddress emailAddr = new InternetAddress(email);
				emailAddr.validate();
			} catch (Exception ex) {
				result = false;
			}
		} else {
			result = false;
		}
		return result;
	}

	public static String extractUsernameFromEmail(String email) {
		if (email == null || !email.contains("@")) {
			throw new IllegalArgumentException("Invalid email: " + email);
		}
		return email.split("@", 2)[0];
	}

	/**
	 * @param birthDate
	 * @return boolean
	 */
	public static boolean isValidBirthDate(Date birthDate) {
		return birthDate == null || birthDate.compareTo(new Date()) <= 0;
	}

	public static String clean(String input) {
        if (input == null) {
            return null;
        }

        // 1. Remove non-ASCII
        String asciiOnly = input.replaceAll("[^\\x00-\\x7F]", "");

        // 2. Remove all spaces
        return asciiOnly.replace(" ", "");
    }
}
