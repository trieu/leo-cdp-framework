package test.cdp.profile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.model.customer.FinanceCreditEvent;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.system.util.GeoLocationUtil;
import net.datafaker.Faker;
import rfx.core.util.DateTimeUtil;
import rfx.core.util.FileUtils;
import rfx.core.util.RandomUtil;
import rfx.core.util.StringUtil;
import rfx.core.util.Utils;

public class TestImportProfile {

	static List<String[]> parseGermanCreditDataFile(String pathname) throws FileNotFoundException {
		CsvParserSettings settings = new CsvParserSettings();
		settings.setHeaderExtractionEnabled(true);
		CsvParser csvParser = new CsvParser(settings);
		List<String[]> allRows = csvParser.parseAll(new FileReader(new File(pathname)));
		return allRows;
	}
	
	static List<String> loadAllStatesInUS(){
		try {
			String json = FileUtils.readFileAsString("./data/states_hash.json");
			Map<String, String> map = new Gson().fromJson(json, HashMap.class);
			return new ArrayList<>(map.values());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {

		doImport();
		//generateFakeDataInFinance();
		
//		Device leoCdpApiDevice = Device.LEO_CDP_API_DEVICE;
//		System.out.println(leoCdpApiDevice.getId());
//		DeviceDaoUtil.save(leoCdpApiDevice);

		Utils.exitSystemAfterTimeout(3000);
	}

	protected static void doImport() {

		String importFileName = "/data/sample-fake-customer.csv";
		List<ProfileSingleView> profiles = ProfileDataManagement.parseToImportProfiles(importFileName, true);
		System.out.println(profiles.size());
		for (ProfileSingleView profile : profiles) {
			profile.loadGenderAndMarialStatus();
			String ss = profile.getId() + " " + profile.getFirstName() + " " + profile.getLastName() + " Email: " + profile.getPrimaryEmail() + " " + profile.getPrimaryPhone();
			System.out.println(ss);
		}

	}

	protected static void generateFakeDataInFinance() {
		try {
			Faker faker = new Faker(new Locale("en-US"));
			// 0 ID,1 Age, 2 Sex, 3 Job, 4 Housing, 5 Saving accounts,6 Checking account, 7 Credit amount, 8 Duration, 9 Purpose,10 Risk
			
			List<String[]> table = parseGermanCreditDataFile("./data/german_credit_data.csv");
			List<String> usStates = loadAllStatesInUS();
			Random rand = new Random();
			
			for (String[] row : table) {
				
				int age = StringUtil.safeParseInt(row[1]);
				String genderStr = row[2];
				int jobType =  StringUtil.safeParseInt(row[3]);
				String housingType = row[4];
				
				String savingAccountStatus = row[5];
				String checkingAccountStatus = row[6];
				long creditAmount = StringUtil.safeParseLong(row[7]);
				int duration = StringUtil.safeParseInt(row[8]);
				String purpose = row[9];
				String risk = row[10];
				
				System.out.println(Arrays.toString(row));
				String firstName = faker.name().firstName();
				String lastName = faker.name().lastName();
				String phone = faker.phoneNumber().cellPhone();
				String emailHint = (firstName.toLowerCase() + "-" + lastName.toLowerCase()).replaceAll("[^\\x00-\\x7F]", "") + "-####@gmail.com";
				String email = faker.bothify(emailHint);
				System.out.println(firstName + " " + lastName);
				System.out.println(email);
				System.out.println(phone);

				String livingLocation = usStates.get(rand.nextInt(usStates.size())) + ", US";

				String locationCode = GeoLocationUtil.getLocationCodeFromLocationQuery(livingLocation);
				System.out.println(locationCode);

				Calendar calendar = Calendar.getInstance();
				int year = calendar.get(Calendar.YEAR) - age;
				int month = RandomUtil.getRandomInteger(12, 1);
				int date = RandomUtil.getRandomInteger(28, 1);
				calendar.set(year, month, date);
				
				String dateOfBirth = DateTimeUtil.formatDate(calendar.getTime(),DateTimeUtil.DATE_FORMAT_PATTERN);
				ProfileSingleView profile = ProfileSingleView.newImportedProfile("", firstName, lastName, email, phone, genderStr, age, dateOfBirth, livingLocation, ""); 
				profile.setLocationCode(locationCode);
				
				profile.setJobType(jobType);
				profile.setHousingType(housingType);
				
				FinanceCreditEvent fne = new FinanceCreditEvent(savingAccountStatus, checkingAccountStatus, creditAmount, duration, purpose, risk);
				profile.setFinanceCreditEvent(fne);
				
				System.out.println(profile.toJson());
				
				ProfileDaoUtil.insertAsAsynchronousJob(profile);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
