package leotech.cdp.utils;

import java.io.File;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpStatus;

import io.vertx.core.http.HttpServerResponse;
import leotech.cdp.model.file.FileApiResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import rfx.core.configs.WorkerConfigs;
import rfx.core.util.StringUtil;

/**
 * Created by SangNguyen
 */
public class VngCloudUtil {
    public final static String VSTORAGE_GET_TOKEN_URL = WorkerConfigs.load().getCustomConfig("VSTORAGE_GET_TOKEN_URL");
    public final static String CDP_CORE_VSTORAGE_CONTAINER_URL = WorkerConfigs.load().getCustomConfig("CDP_CORE_VSTORAGE_CONTAINER_URL");
    final static String VSTORAGE_GET_TOKEN_BODY = WorkerConfigs.load().getCustomConfig("VSTORAGE_GET_TOKEN_BODY");
    
    public final static boolean isReadyToUpload() {
    	return StringUtil.isNotEmpty(VSTORAGE_GET_TOKEN_BODY) 
    			&& StringUtil.isNotEmpty(VSTORAGE_GET_TOKEN_URL)
    			&& StringUtil.isNotEmpty(CDP_CORE_VSTORAGE_CONTAINER_URL);
    }

    final OkHttpClient httpClient = new OkHttpClient();

    /**
     * @return the token that you will you must use to interact with Vstorage
     */
    public String getToken() {
        try {
        	if(StringUtil.safeString(VSTORAGE_GET_TOKEN_URL).startsWith("http")) {
        		 String token = null;
                 RequestBody body = RequestBody.create(VSTORAGE_GET_TOKEN_BODY, MediaType.get("application/json; charset=utf-8"));

                 Request request = new Request.Builder()
                         .url(VSTORAGE_GET_TOKEN_URL)
                         .post(body)
                         .build();

                 Response response = httpClient.newCall(request).execute();

                 System.out.println("Got Vstorage get-token response !!");
                 System.out.println(response.code());

                 if (response.isSuccessful()) {
                     System.out.println("Vstorage token: " + response.header("x-subject-token"));
                     token = response.header("x-subject-token");
                 }
                 response.close();

                 return token;
        	}	
           
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * @param vstorageFileUrl the file URL from Vstorage
     * @param token the token used to interact with Vstorage
     * @param serverResponse the Vertx response you want to insert file content after getting it from Vstorage
     * @return the Vertx response after being inserted with file content
     */
    public HttpServerResponse getFile(String vstorageFileUrl, String token, HttpServerResponse serverResponse) {
        try {
            Request request = new Request.Builder()
                    .url(vstorageFileUrl)
                    .header("X-Auth-Token", token)
                    .build();

            Response response = httpClient.newCall(request).execute();
            ResponseBody body = response.body();

            if(response.isSuccessful()) {
                System.out.println("**Got file from Vstorage successfully**");
                System.out.println(vstorageFileUrl);
                System.out.println(response.code());
            }
            else {
                System.out.println("**Got file from Vstorage failed**");
                System.out.println(vstorageFileUrl);
                System.out.println(response.code());
            }

            if (body != null) {
                serverResponse.setStatusCode(response.code());
                serverResponse.putHeader("Content-Type", response.header("Content-Type", "application/octet-stream"));

                serverResponse.end(cleanCsvContent(body.string()));
            } else {
                serverResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end("Get file from Vstorage failed");
            }
            
            return serverResponse;
        } catch (Exception e) {
            e.printStackTrace();
            return serverResponse;
        }
    }


    /**
     * @param filePath the path of the file you want to upload
     * @return the file's URL after getting uploaded to Vstorage
     */
    public FileApiResponse uploadFileToVstorage(String filePath) {
        try {
            System.out.println("Uploading file: " + filePath);

            String token = getToken();

            if(token != null) {
                File file = new File(filePath);
                String url = VngCloudUtil.CDP_CORE_VSTORAGE_CONTAINER_URL + file.getName();
                String extension = FilenameUtils.getExtension(filePath);
                String mediaType = getMediaType(extension);

                System.out.println("MediaType: " + mediaType);

                RequestBody body = RequestBody.create(file, MediaType.parse(mediaType));
                Request request = new Request.Builder()
                        .url(url)
                        .header("X-Auth-Token", token)
                        .put(body)
                        .build();
                Response response = httpClient.newCall(request).execute();

                if(response.isSuccessful()) {
                    System.out.println("**Uploaded file to Vstorage successfully**");
                    System.out.println(url);
                    System.out.println(response.code());
                    response.close();

                    return new FileApiResponse(response.code(), url, "Upload file to Vstorage successfully");
                }
                else {
                    System.out.println("**Uploaded file to Vstorage failed**");
                    System.out.println(response.code());
                    System.out.println(response.message());
                    response.close();

                    return new FileApiResponse(response.code(), null, "Upload file to Vstorage failed");
                }
            }

            return new FileApiResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "Can not get token to upload file");
        }
        catch (Exception e) {
            e.printStackTrace();
            return new FileApiResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "Error to upload file");
        }
    }


    /**
     * @param fileName the name of the file you want to delete
     * @param token the token used to interact with Vstorage
     */
    public void deleteFile(String fileName, String token) {
        try{
            String url = CDP_CORE_VSTORAGE_CONTAINER_URL + fileName + "?multipart-manifest=delete";

            Request request = new Request.Builder()
                    .url(url)
                    .header("X-Auth-Token", token)
                    .delete()
                    .build();

            Response response = httpClient.newCall(request).execute();

            if(response.isSuccessful()) {
                System.out.println("**Deleted file from Vstorage successfully**");
                System.out.println(url);
                System.out.println(response.code());
            }
            else {
                System.out.println("**Deleted file from Vstorage failed**");
                System.out.println(url);
                System.out.println(response.code());
            }
            response.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @param fileName the name of the file you want to delete
     */
    public void deleteFile(String fileName) {
        try{
            String token = getToken();
            String url = CDP_CORE_VSTORAGE_CONTAINER_URL + fileName + "?multipart-manifest=delete";

            Request request = new Request.Builder()
                    .url(url)
                    .header("X-Auth-Token", token)
                    .delete()
                    .build();

            Response response = httpClient.newCall(request).execute();

            if(response.isSuccessful()) {
                System.out.println("**Deleted file from Vstorage successfully**");
                System.out.println(url);
                System.out.println(response.code());
            }
            else {
                System.out.println("**Deleted file from Vstorage failed**");
                System.out.println(url);
                System.out.println(response.code());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @param filePath the path of the file you want to get name
     * @return the file's name
     */
    public String getFileNameFromPath(String filePath) {
        int slashInd = filePath.lastIndexOf("/");
        return filePath.substring(slashInd + 1);
    }


    /**
     * @param extension the type of the file
     * @return the file's name
     */
    public String getMediaType(String extension) {
        Set<String> text = Set.of("csv", "excel");
        Set<String> image = Set.of("png", "jpeg", "jpg");

        if(text.contains(extension.toLowerCase())) {
            return "text/" + extension;
        }

        if(image.contains(extension.toLowerCase())) {
            return "image/" + extension;
        }

        return "data";
    }


    /**
     * @param content the content of a CSV file with redundant API header's data and  header - footer
     * @return the CSV content after removing redundant data
     */
    public String cleanCsvContent(String content) {
        String cleanedContent = content.replaceAll("(?s)(Content-Disposition:.*?filename=.*?\n)?(Content-Type:.*?\n)?(Content-Length:.*?\n)?", "");
        cleanedContent = cleanedContent.substring(cleanedContent.indexOf("\n"));
        cleanedContent = cleanedContent.substring(0, cleanedContent.indexOf("--"));

        return cleanedContent.trim();
    }


    /**
     * @param completedCount the number of completed rows
     * @param total the total number of rows
     * @return the percentage of completed rows - max 99%
     */
    public static double calculateUploadingPercentage(int completedCount, long total, boolean needCeiling) {
        double percentage = (double) completedCount / (double) total * 100;
        return percentage <= 99
                    ? needCeiling
                        ? Math.round(percentage)
                        : Math.round(percentage * 10.0) / 10.0
                    : 99;
    }
    
}
