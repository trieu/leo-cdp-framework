package test.util;

import com.google.auth.oauth2.GoogleCredentials;
import leotech.cdp.model.file.FileApiResponse;
import leotech.cdp.utils.GoogleSheetUtils;
import org.jetbrains.annotations.TestOnly;

import java.util.List;

public class GoogleSheetUtil {
    public static void main(String... args) throws Exception {
//        String range = "Sheet1!A1:AZ2";
//        String valueInputOption = "RAW";
//        List<Object> row1 = List.of("uid","email","phone","fn","ln","dob","ct","st","zip","country","value","acquisition","prospect","engagement","transaction","loyalty","cfs","ces","nps","csat","web-visitor-id","crm-id","labels","in-segments","notes","exported-date","top-100-events");
//        List<Object> row2 = List.of("uid","email","phone","fn","ln","dob","ct","st","zip","country","value","acquisition","prospect","engagement","transaction","loyalty","cfs","ces","nps","csat","web-visitor-id","crm-id","labels","in-segments","notes","exported-date","top-100-events");
//        List<List<Object>> values = List.of(row1, row2);
//
//        // create new credentials
//        GoogleSheetUtils googleSheetUtils = new GoogleSheetUtils();
//        GoogleCredentials credential = googleSheetUtils.getCredentials();
//
//        // create new test sheet
//        FileApiResponse response = googleSheetUtils.createNewSheet("TEST", 2, row1.size(), "Sheet1", credential, true);
//        String sheetId = response.getMessage();
//
//        System.out.println(response);
//
//        // write to new test sheet
//        response = googleSheetUtils.writeToSheet(sheetId, range, valueInputOption, values, credential);
//        System.out.println(response.toString());

        // delete selected sheet
        GoogleSheetUtils googleSheetUtils = new GoogleSheetUtils();
        GoogleCredentials credential = googleSheetUtils.getCredentials();
        googleSheetUtils.deleteSheet("1uRS0IiegdedG5N8t4HaJTXy49vyFwanj6H4_m83mjjk",credential);
    }
}
