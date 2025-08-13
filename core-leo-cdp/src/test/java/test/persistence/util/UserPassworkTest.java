package test.persistence.util;

import leotech.system.util.EncryptorAES;

public class UserPassworkTest {

    public static void main(String[] args) {
	 String orginalPass = EncryptorAES.passwordHash("cadmin", "123456");
	 System.out.println(orginalPass);
    }
}
