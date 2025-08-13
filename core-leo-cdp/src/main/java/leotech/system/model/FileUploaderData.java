package leotech.system.model;

import java.util.ArrayList;
import java.util.List;

public class FileUploaderData {

    List<String> fileUrls = new ArrayList<>();
    
    public FileUploaderData() {
	// TODO Auto-generated constructor stub
    }

    public List<String> getFileUrls() {
        return fileUrls;
    }

    public void setFileUrls(List<String> fileUrls) {
        this.fileUrls = fileUrls;
    }
    

    public void setFileUrl(String fileUrl) {
        this.fileUrls.add(fileUrl);
    }
    
    
}
