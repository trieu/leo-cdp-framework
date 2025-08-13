package test.util;

import leotech.system.util.HashIds;

public class TestHashIds {

	public static void main(String[] args) {
		HashIds hashids = new HashIds("this is my salt");
		String hash = hashids.encode(1234500808655455L);
		System.out.println(hash);
	}
}
