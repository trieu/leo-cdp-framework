package leotech.cdp.model.customer;

import java.util.HashMap;
import java.util.Map;

import rfx.core.util.StringUtil;

/**
 * Utility class for handling gender codes and labels.
 * 
 * Codes:
 *  0 = Female
 *  1 = Male
 *  2 = LGBT
 *  3 = Lesbian
 *  4 = Gay
 *  5 = Bisexual
 *  6 = Transgender
 *  7 = Unknown
 *  
 * @author 
 * @since 2022
 */
public final class ProfileGenderCode {

    private ProfileGenderCode() {}

    public static final int FEMALE = 0;
    public static final int MALE = 1;
    public static final int LGBT = 2;
    public static final int LESBIAN = 3;
    public static final int GAY = 4;
    public static final int BISEXUAL = 5;
    public static final int TRANSGENDER = 6;
    public static final int UNKNOWN = 7;

    public static final String GENDER_FEMALE = "female";
    public static final String GENDER_MALE = "male";
    public static final String GENDER_LGBT = "lgbt";
    public static final String GENDER_LESBIAN = "lesbian";
    public static final String GENDER_GAY = "gay";
    public static final String GENDER_BISEXUAL = "bisexual";
    public static final String GENDER_TRANSGENDER = "transgender";
    public static final String GENDER_UNKNOWN = "unknown";

    private static final Map<String, Integer> STRING_TO_INT = new HashMap<>();
    private static final Map<Integer, String> INT_TO_STRING = new HashMap<>();

    static {
        STRING_TO_INT.put(GENDER_FEMALE, FEMALE);
        STRING_TO_INT.put(GENDER_MALE, MALE);
        STRING_TO_INT.put(GENDER_LGBT, LGBT);
        STRING_TO_INT.put(GENDER_LESBIAN, LESBIAN);
        STRING_TO_INT.put(GENDER_GAY, GAY);
        STRING_TO_INT.put(GENDER_BISEXUAL, BISEXUAL);
        STRING_TO_INT.put(GENDER_TRANSGENDER, TRANSGENDER);
        STRING_TO_INT.put(GENDER_UNKNOWN, UNKNOWN);

        // Inverse map for easy reverse lookup
        for (Map.Entry<String, Integer> e : STRING_TO_INT.entrySet()) {
            INT_TO_STRING.put(e.getValue(), e.getKey());
        }
    }

    /**
     * Convert a gender string (case-insensitive) into an integer code.
     * Defaults to UNKNOWN (7) if not matched or empty.
     */
    public static int getIntegerValue(String genderStr) {
        if (StringUtil.isEmpty(genderStr)) {
            return UNKNOWN;
        }
        String s = genderStr.trim().toLowerCase();
        return STRING_TO_INT.getOrDefault(s, UNKNOWN);
    }

    /**
     * Convert an integer code into a lowercase gender string.
     * Returns "unknown" for unrecognized codes.
     */
    public static String getStringValue(int genderInt) {
        return INT_TO_STRING.getOrDefault(genderInt, GENDER_UNKNOWN);
    }

    /**
     * Check whether the given integer is a valid gender code.
     */
    public static boolean isValidGenderCode(int code) {
        return INT_TO_STRING.containsKey(code);
    }
}
