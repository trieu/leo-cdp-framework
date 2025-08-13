package leotech.cdp.model.file;

public class FileApiResponse {
    private String fileUrl;
    private String message;
    private int statusCode;
    private Object additionalData;

    public FileApiResponse() {
    }

    public FileApiResponse(int statusCode, String fileUrl, String message) {
        this.fileUrl = fileUrl;
        this.message = message;
        this.statusCode = statusCode;
    }

    public FileApiResponse(int statusCode, String fileUrl, String message, Object additionalData) {
        this.fileUrl = fileUrl;
        this.message = message;
        this.statusCode = statusCode;
        this.additionalData = additionalData;
    }

    public void setFileUrl(String imageUrl) {
        this.fileUrl = imageUrl;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Object getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Object additionalData) {
        this.additionalData = additionalData;
    }

    @Override
    public String toString() {
        return "FileApiResponse{" +
                "fileUrl='" + fileUrl + '\'' +
                ", message='" + message + '\'' +
                ", statusCode=" + statusCode +
                ", additionalData=" + additionalData +
                '}';
    }
}
