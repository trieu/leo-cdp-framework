package leotech.system.util;


import java.util.List;

public class RoundRobin<T> {

    private List<T> values;
    private int N = 0;
    private int counter = -1;
    private T singleValue;

    public T next() {
	if (singleValue != null) {
	    return singleValue;
	} else {
	    counter = (counter + 1) % N; // % is the remainder operator
	    return values.get(counter);
	}
    }

    public RoundRobin(T value) {
	singleValue = value;
	N = 1;
    }

    public RoundRobin(List<T> list) {
	values = list;
	N = values.size();
    }

}
