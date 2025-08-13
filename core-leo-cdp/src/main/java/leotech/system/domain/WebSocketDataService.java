package leotech.system.domain;

import java.util.Optional;

import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;

public class WebSocketDataService {
	private SharedData data;

	public WebSocketDataService(SharedData data) {
		this.data = data;
	}

	public Optional<Integer> get() {
		LocalMap<String, String> counter = data.getLocalMap("key");
		return Optional.of(counter).filter(map -> !map.isEmpty()).map(map -> Integer.valueOf(map.get("counter")));
	}

	public void update(Integer counter) {
		LocalMap<String, String> map = data.getLocalMap("key");
		map.put("counter", counter.toString());
	}
}
