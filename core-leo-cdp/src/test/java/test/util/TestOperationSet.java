package test.util;

import java.util.Set;

import com.google.common.collect.Sets;

public class TestOperationSet {

	public static void main(String[] args) {
		// Creating first set named set1
		Set<String> old = Sets.newHashSet("demo1", "demo2", "demo3");

		// Creating second set named set2
		Set<String> update1 = Sets.newHashSet("demo4");

		// Creating second set named set2
		Set<String> update2 = Sets.newHashSet();

		// Using Guava's Sets.difference() method
		Set<String> diff1 = Sets.difference(old, update1);
		Set<String> union1 = Sets.union(old, update1);

		// Using Guava's Sets.difference() method
		Set<String> diff2 = Sets.difference(old, update2);
		Set<String> union2 = Sets.union(old, update2);

		// Displaying the unmodifiable view of
		// the difference of two sets.
		System.out.println("old: " + old);
		System.out.println("update1: " + update1);
		System.out.println("update2: " + update2);
		System.out.println("Difference between " + "old and update1: " + diff1);
		System.out.println("Union between " + "old and update1: " + union1);
		
		System.out.println("Difference between " + "old and update2: " + diff2);
		System.out.println("Union between " + "old and update2: " + union2);
	}
}
