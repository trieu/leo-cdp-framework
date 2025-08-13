package test.cdp.profile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import leotech.cdp.utils.ProfileDataValidator;



public class ProfileDataValidatorTest {

    // Test case for phoneNumber1: "+1 (123) 456-7890"
    @Test
    public void testPhoneNumber1() {
        String phoneNumber1 = "+1 (123) 456-7890";
        assertTrue(ProfileDataValidator.isValidPhoneNumber(phoneNumber1));
    }

    // Test case for phoneNumber2: "123-456-7890"
    @Test
    public void testPhoneNumber2() {
        String phoneNumber2 = "123-456-7890";
        assertTrue(ProfileDataValidator.isValidPhoneNumber(phoneNumber2));
    }

    // Test case for phoneNumber3: "1234567890"
    @Test
    public void testPhoneNumber3() {
        String phoneNumber3 = "1234567890";
        assertTrue(ProfileDataValidator.isValidPhoneNumber(phoneNumber3));
    }

    // Test case for phoneNumber4: "0903122290"
    @Test
    public void testPhoneNumber4() {
        String phoneNumber4 = "0903122290";
        assertTrue(ProfileDataValidator.isValidPhoneNumber(phoneNumber4));
    }

    // Test case for phoneNumber5: "113" (invalid)
    @Test
    public void testPhoneNumber5() {
        String phoneNumber5 = "113";
        assertFalse(ProfileDataValidator.isValidPhoneNumber(phoneNumber5));
    }
    
    // Test case for phoneNumber5: "09824722***" (invalid)
    @Test
    public void testPhoneNumber6() {
        String phoneNumber = "0967719***";
        assertFalse(ProfileDataValidator.isValidPhoneNumber(phoneNumber));
    }
    
    
}
