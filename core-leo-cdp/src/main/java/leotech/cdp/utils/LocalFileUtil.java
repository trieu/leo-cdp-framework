package leotech.cdp.utils;

import java.io.File;

/**
 * Created by SangNguyen
 */
public class LocalFileUtil {
    /**
     * @param filePath the path of the file you want to remove
     */
    public void removeFile(String filePath) {
        File file = new File(filePath);

        if (file.exists()) {
            boolean isDeleted = file.delete();
            if (isDeleted) {
                System.out.println("**Local file deleted successfully: " + filePath + "**");
            } else {
                System.out.println("**Can not delete file: " + filePath + "**");
            }
        } else {
            System.out.println("**File does not exist: " + filePath + "**");
        }
    }
}
