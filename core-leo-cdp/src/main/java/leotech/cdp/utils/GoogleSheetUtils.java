package leotech.cdp.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpStatus;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import leotech.cdp.model.file.FileApiResponse;
import leotech.system.exception.InvalidDataException;
import rfx.core.configs.WorkerConfigs;
import rfx.core.util.StringUtil;

/**
 * Google Sheet Utils
 * 
 * @author SangNguyen, Trieu Nguyen
 * @since 2024
 */
public class GoogleSheetUtils {
    private static final String APPLICATION_NAME = "Google Sheet export segment";
    private static final List<String> SCOPES = List.of(SheetsScopes.DRIVE_READONLY, SheetsScopes.SPREADSHEETS_READONLY, SheetsScopes.DRIVE_FILE);

    private static final String SERVICE_ACCOUNT_KEY_FILE_PATH = WorkerConfigs.load().getCustomConfig("SERVICE_ACCOUNT_KEY_FILE_PATH");
    private static final String GOOGLE_SHEET_URL = WorkerConfigs.load().getCustomConfig("GOOGLE_SHEET_URL");
    
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String drivePermissionType = "anyone";
    private static final String drivePermissionRole = "reader";

    /**
     * @return GoogleCredentials from SERVICE_ACCOUNT_KEY_FILE_PATH to use for interacting with Google services
     */
    public GoogleCredentials getCredentials() throws IOException {
        File file = new File(SERVICE_ACCOUNT_KEY_FILE_PATH);
        if(file.isFile()) {
        	FileInputStream serviceAccountStream = new FileInputStream(file);

            return GoogleCredentials
                    .fromStream(serviceAccountStream)
                    .createScoped(SCOPES);
        }
        else {
        	throw new InvalidDataException("GoogleSheetUtils can not get Credentials " + SERVICE_ACCOUNT_KEY_FILE_PATH);
        }
		
    }


    /**
     * @param title spreadsheet title
     * @param googleCredentials GoogleCredentials
     * @param allowPublic allow this file to be public or not
     * @return spreadsheet ID
     */
    public FileApiResponse createNewSheet(String title, long maxRow, int maxColumn, String defaultSheetName,  GoogleCredentials googleCredentials, boolean allowPublic) {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(googleCredentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            // Set up the grid properties (max rows and columns)
            GridProperties gridProperties = new GridProperties()
                    .setRowCount((int) maxRow)
                    .setColumnCount(maxColumn);

            // Create the sheet with grid properties
            Sheet sheet = new Sheet().setProperties(new SheetProperties()
                    .setGridProperties(gridProperties)
                    .setTitle(defaultSheetName));

            // Create the spreadsheet with the new sheet
            Spreadsheet spreadsheet = new Spreadsheet()
                    .setProperties(new SpreadsheetProperties().setTitle(title))
                    .setSheets(Collections.singletonList(sheet));

            // Execute the creation of the spreadsheet
            spreadsheet = service.spreadsheets()
                    .create(spreadsheet)
                    .setFields("spreadsheetId")
                    .execute();

            String id = spreadsheet.getSpreadsheetId();

            System.out.println("Created new spreadsheet ID: " + id);

            // set public for the spreadsheet
            if(allowPublic) {
                setAccessPermission(id, googleCredentials);
            }

            return new FileApiResponse(
                    HttpStatus.SC_OK,
                    getGoogleSheetUrl(id),
                    id
            );
        }
        catch (Exception e) {
            e.printStackTrace();
            return new FileApiResponse(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    null,
                    e.getMessage()
            );
        }
    }


    /**
     * @param spreadsheetId spreadsheet ID
     * @param range range of sheet to write with format _tabName_!_startCell_:_endCell_ (Ex: 'Sheet1!A1:Z1000')
     * @param valueInputOption option of input value [RAW, USER_ENTERED]
     * @param values two-sided List to store values
     * @param googleCredentials GoogleCredentials
     * @return response after editing the sheet
     */
    public FileApiResponse writeToSheet(String spreadsheetId, String range, String valueInputOption, List<List<Object>> values, GoogleCredentials googleCredentials) {
        try {
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(googleCredentials);

            // Create the sheets API client
            Sheets service = new Sheets.Builder(new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    requestInitializer)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            // Updates the values in the specified range.
            ValueRange body = new ValueRange().setValues(values);

            UpdateValuesResponse result = service.spreadsheets()
                    .values()
                    .update(spreadsheetId, range, body)
                    .setValueInputOption(valueInputOption)
                    .execute();

            System.out.printf("%d cells updated\n", result.getUpdatedCells());

            return new FileApiResponse(
                    HttpStatus.SC_OK,
                    getGoogleSheetUrl(spreadsheetId),
                    result.getUpdatedCells() + " cells updated"
            );
        }
        catch (Exception e) {
            e.printStackTrace();
            return new FileApiResponse(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    getGoogleSheetUrl(spreadsheetId),
                    e.getMessage()
            );
        }
    }


    /**
     * @param spreadsheetId spreadsheet ID you want to delete
     * @param googleCredentials GoogleCredentials
     * @return response after deleting the spreadsheet
     */
    public FileApiResponse deleteSheet(String spreadsheetId, GoogleCredentials googleCredentials) {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            // Create client in Google Drive API
            Drive driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(googleCredentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            // Delete spreadsheet
            driveService.files().delete(spreadsheetId).execute();

            System.out.println("Spreadsheet with ID " + spreadsheetId + " has been deleted.");

            return new FileApiResponse(
                    HttpStatus.SC_OK,
                    null,
                    "Deleted spreadsheet ID " + spreadsheetId + " successfully"
            );
        }
        catch (Exception e) {
            e.printStackTrace();
            return new FileApiResponse(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    null,
                    "Error to delete spreadsheet ID " + spreadsheetId
            );
        }
    }


    /**
     * @param sheetId spreadsheet ID
     * @param googleCredentials GoogleCredentials
     */
    public void setAccessPermission(String sheetId, GoogleCredentials googleCredentials) {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Drive driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(googleCredentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            Permission permission = new Permission()
                    .setType(drivePermissionType)
                    .setRole(drivePermissionRole);

            driveService.permissions().create(sheetId, permission).execute();

            System.out.printf("Set permission for spreadsheet ID %s with type %s and role %s successfully\n", sheetId, drivePermissionType, drivePermissionRole);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @param id spreadsheet ID
     * @return Google Sheet URL
     */
    public String getGoogleSheetUrl(String id) {
        return GOOGLE_SHEET_URL + id;
    }


    /**
     * @param sheetTabName sheet tab name
     * @param startRowNum start row number
     * @param values list values of sheet
     * @return range of sheet to write with format sheetTabNane!startCell:endCell (Ex: 'Sheet1!A1:Z1000')
     */
    public String getRange(String sheetTabName, int startRowNum, List<List<Object>> values) {
        String tabName = StringUtil.safeString(sheetTabName, "Sheet1");
        String start = "A" + startRowNum;
        String end = convertNumberToColumnLetter(values.get(0).size()) + (startRowNum + values.size());

        return tabName + "!" + start + ":" + end;
    }


    /**
     * @param num maximum length of a sheet row
     * @return a string to show the column letter from the row length (Ex: 1 -> A, 30 -> AE, 100 -> CY) using ASCII to calculate
     */
    private String convertNumberToColumnLetter(int num) {
        // ASCII decimal before letter 'A'
        int minAsciiDec = 64;
        int totalAlphabetLetter = 25;
        int tmp = num;
        // when the row length is more than 25, it will add an extra letter before the current letter (Ex: 30 -> AE), the extraCount is for calculate that extra letter
        int extraCount = 0;
        String res = "";

        if(num > totalAlphabetLetter) {
            while(tmp > totalAlphabetLetter) {
                extraCount++;
                tmp -= totalAlphabetLetter;
            }

            // add the extra letter first if there is any
            if(extraCount > 0) {
                res += String.valueOf((char) (minAsciiDec + extraCount));
            }
        }
        res += String.valueOf((char) (tmp + minAsciiDec));

        return res.toString();
    }



    public static double calculateUploadingPercentage(int completedCount, long total, boolean needCeiling) {
        double percentage = (double) completedCount / (double) total * 100;
        return percentage <= 99
                ? needCeiling
                ? Math.round(percentage)
                : Math.round(percentage * 10.0) / 10.0
                : 99;
    }
}
