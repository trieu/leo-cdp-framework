package test.jdbi;

import java.time.LocalDateTime;
import java.util.List;

public class JdbiOrmExample {

	public static void main(String[] args) {
		String dbUrl = "jdbc:sqlite:/home/thomas/0-uspa/Django-Netflix-Clone/db.sqlite3";

		DatabaseManager dbManager = new DatabaseManager(dbUrl);
		dbManager.createTable();

		Person person = new Person();
		person.setFirstName("John");
		person.setLastName("Doe");
		person.setCreatedAt(LocalDateTime.now());
		person.setUpdatedAt(LocalDateTime.now());

		int generatedId = dbManager.insertPerson(person);
		System.out.println("Inserted person with ID: " + generatedId);

		List<Person> list = dbManager.listPeople(5, 0);
		for (Person p : list) {
			System.out.println(p);
		}
	}
}
