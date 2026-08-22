package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import leotech.cdp.model.customer.Profile;
import leotech.cdp.query.filters.ProfileFilter;
import leotech.cdp.utils.ProfileDataValidator;
import rfx.core.util.StringUtil;

/**
 * Creates the identity fields used to find profiles that can be merged.
 */
public final class ProfileDeduplicationFilterFactory {

	public ProfileFilter create(Profile sourceProfile) {
		Objects.requireNonNull(sourceProfile, "sourceProfile");

		ProfileFilter filter = new ProfileFilter(sourceProfile.getId());
		filter.setEmails(validPrimaryValueWithSecondaryValues(
				sourceProfile.getPrimaryEmail(), sourceProfile.getSecondaryEmails(), true));
		filter.setPhones(validPrimaryValueWithSecondaryValues(
				sourceProfile.getPrimaryPhone(), sourceProfile.getSecondaryPhones(), false));

		setIfNotEmpty(sourceProfile.getCrmRefId(), filter::setCrmRefId);
		setIfNotEmpty(sourceProfile.getVisitorId(), filter::setVisitorId);
		setFingerprintFields(sourceProfile, filter);
		setIfNotEmpty(sourceProfile.getApplicationIDs(), filter::setApplicationIDs);
		setIfNotEmpty(sourceProfile.getFintechSystemIDs(), filter::setFintechSystemIDs);
		setIfNotEmpty(sourceProfile.getGovernmentIssuedIDs(), filter::setGovernmentIssuedIDs);
		setIfNotEmpty(sourceProfile.getLoyaltyIDs(), filter::setLoyaltyIDs);

		return filter;
	}

	private List<String> validPrimaryValueWithSecondaryValues(String primaryValue,
			Set<String> secondaryValues, boolean email) {
		List<String> values = new ArrayList<>();
		boolean validPrimaryValue = email
				? ProfileDataValidator.isValidEmail(primaryValue)
				: ProfileDataValidator.isValidPhoneNumber(primaryValue);
		if (validPrimaryValue) {
			values.add(primaryValue);
		}
		values.addAll(secondaryValues);
		return values;
	}

	private void setFingerprintFields(Profile sourceProfile, ProfileFilter filter) {
		if (StringUtil.isNotEmpty(sourceProfile.getFingerprintId())
				&& StringUtil.isNotEmpty(sourceProfile.getLastSeenIp())
				&& StringUtil.isNotEmpty(sourceProfile.getLastUsedDeviceId())) {
			filter.setFingerprintId(sourceProfile.getFingerprintId());
			filter.setLastSeenIp(sourceProfile.getLastSeenIp());
			filter.setLastUsedDeviceId(sourceProfile.getLastUsedDeviceId());
		}
	}

	private void setIfNotEmpty(String value, java.util.function.Consumer<String> setter) {
		if (StringUtil.isNotEmpty(value)) {
			setter.accept(value);
		}
	}

	private void setIfNotEmpty(Set<String> values, java.util.function.Consumer<List<String>> setter) {
		if (!values.isEmpty()) {
			setter.accept(new ArrayList<>(values));
		}
	}
}