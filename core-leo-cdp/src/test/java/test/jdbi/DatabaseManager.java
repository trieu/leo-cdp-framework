package test.jdbi;

import java.time.LocalDateTime;
import java.util.List;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;

public class DatabaseManager {
	private final Jdbi jdbi;

	public DatabaseManager(String dbUrl) {
		jdbi = Jdbi.create(dbUrl);
	}

	public void createTable() {
		jdbi.useHandle(handle -> {
			handle.execute("CREATE TABLE IF NOT EXISTS people (" + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "first_name TEXT NOT NULL," + "last_name TEXT," + "created_at DATETIME," + "updated_at DATETIME"
					+ ")");
		});
	}

	public int insertPerson(Person person) {
		return jdbi.withHandle(handle -> {
			Update update = handle.createUpdate("INSERT INTO people (first_name, last_name, created_at, updated_at) "
					+ "VALUES (:firstName, :lastName, :createdAt, :updatedAt)");
			update.bind("firstName", person.getFirstName());
			update.bind("lastName", person.getLastName());
			update.bind("createdAt", person.getCreatedAt());
			update.bind("updatedAt", person.getUpdatedAt());
			return update.executeAndReturnGeneratedKeys("id").mapTo(int.class).findOnly();
		});
	}

	public List<Person> listPeople(int limit, int offset) {
		return jdbi.withHandle(handle -> handle
				.createQuery("SELECT id, first_name, last_name, created_at, updated_at FROM people "
						+ "ORDER BY created_at DESC " + "LIMIT :limit OFFSET :offset")
				.bind("limit", limit).bind("offset", offset).map((rs, ctx) -> {
					Person person = new Person();
					person.setId(rs.getInt("id"));
					person.setFirstName(rs.getString("first_name"));
					person.setLastName(rs.getString("last_name"));
					person.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
					person.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
					return person;
				}).list());
	}
}
