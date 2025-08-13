package leotech.system.util;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class RandomCollection<E> {
	
	private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
	private final Random random;
	private double total = 0;

	public RandomCollection() {
		this(new Random());
	}

	public RandomCollection(Random random) {
		this.random = random;
	}

	public RandomCollection<E> add(double weight, E result) {
		if (result != null) {
			if (weight <= 0)
				return this;
			total += weight;
			map.put(total, result);
		}
		return this;
	}

	public boolean isEmpty() {
		return map.size() == 0;
	}
	
	public void clear() {
		this.map.clear();
	}

	public E next() {
		double value = random.nextDouble() * total;
		Entry<Double, E> higherEntry = map.higherEntry(value);
		if (higherEntry != null) {
			return higherEntry.getValue();
		}
		return null;
	}

	public Collection<E> values() {
		return map.values();
	}

	public int size() {
		return map.size();
	}
}
