package test.persistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import com.google.gson.Gson;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;

import leotech.system.config.DatabaseConfigs;
import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.model.AppMetadata;
import leotech.system.model.SystemUser;
import leotech.system.model.SystemUserRole;
import leotech.system.util.database.ArangoDbUtil;

public class UserProfileImporter {

	public static void main(String[] args) {
		try {
			ArangoDbUtil.setDbConfigs(DatabaseConfigs.load("dbConfigsBluescope"));

			TsvParserSettings settings = new TsvParserSettings();
			TsvParser parser = new TsvParser(settings);

			String pathname = "./data/SalesTeamList.tsv";
			// parses all rows in one go.
			List<String[]> allRows = parser.parseAll(new FileReader(new File(pathname)));
			allRows.stream().forEach(data -> {
				String displayName = data[0];
				String email = data[1];
				String department = data[2];
				String position = data[3];
				String userLogin = email.split("@")[0];
				String pass = "12345678";
				SystemUser user = new SystemUser(userLogin, pass, displayName, email, AppMetadata.DEFAULT_ID);
				user.addCustomData("position", position);
				user.addCustomData("department", department);
				user.setStatus(SystemUser.STATUS_ACTIVE);
				user.setRole(SystemUserRole.STANDARD_USER);
				SystemUserDaoUtil.deleteSystemUserByUserLogin(userLogin);
				user.setUserLogin(userLogin.toLowerCase());
				SystemUserDaoUtil.createNewSystemUser(user);
				System.out.println(new Gson().toJson(user));
			});
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
