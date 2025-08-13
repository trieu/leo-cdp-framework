package test.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

import com.github.slugify.Slugify;

public class ImageConverter {

    static String folderPath = "/home/platform/projects/bluescope/bluescope-content/";

    public static boolean shouldCopy(Path path) {
	boolean ok = !path.getFileName().toString().equals("Thumbs.db");
	return ok;
    }

    static void processFile(Path path) {
	File file = path.toFile();
	String fullPath = file.getAbsolutePath();
	String ext = FilenameUtils.getExtension(fullPath).toLowerCase();
	long fileSizeInBytes = file.length();

	if (ext.equals("jpg")) {
	    long fileSizeInKB = fileSizeInBytes / (1024);
	    System.out.println(" ==> " + fileSizeInKB);

	    if (fileSizeInKB > 3000) {
		// resize smaller by 50%
		double percent = 0.5;
		if (fileSizeInKB > 10000) {
		    percent = 0.1;
		}
		String newFilename = new Slugify().slugify(fullPath.replace(folderPath, "")).toLowerCase().replace(ext,"resized." + ext);
		String outputImagePath = folderPath + newFilename;

		try {
		    System.out.println(" outputImagePath " + outputImagePath);
		    File outputFile = new File(outputImagePath);
		    //outputFile.createNewFile();
		    System.out.println(" outputImagePath " + outputFile.length());

		    // resize(fullPath, outputImagePath, percent);
		} catch (Exception e) {

		    System.err.println(e.getMessage() + " outputImagePath " + outputImagePath);
		}
	    }
	} else if (ext.equals("nef") || ext.equals("xmp") || ext.equals("tif")
		|| path.getFileName().toString().equals("Thumbs.db")) {
	    boolean ok = path.toFile().delete();
	    System.out.println("deleted " + fullPath);
	}
	System.out.println(fullPath + " ==> " + ext);
    }

    public static void main(String[] args) {
	try {
	    Files.walk(Paths.get(folderPath)).filter(Files::isRegularFile).filter(ImageConverter::shouldCopy)
		    .forEach(ImageConverter::processFile);
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
}
