package leotech.system.util.media;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

public class ImageOptimizer {

	static String folderPath = "./public/uploaded-files/";

	public static boolean imageFilter(Path path) {
		File file = path.toFile();
		String ext = FilenameUtils.getExtension(file.getAbsolutePath()).toLowerCase();
		if ((ext.equals("jpg") || ext.equals("png"))) {
			return true;
		}
		return false;
	}

	public static boolean deleteImage(Path path) {
		File file = path.toFile();
		String ext = FilenameUtils.getExtension(file.getAbsolutePath()).toLowerCase();
		if ((ext.equals("jpg") || ext.equals("png"))) {
			return file.delete();
		}
		return false;
	}

	static void processFile(Path path) {
		File file = path.toFile();
		String fullPath = file.getAbsolutePath();
		String ext = FilenameUtils.getExtension(file.getAbsolutePath()).toLowerCase();

		long fileSizeInBytes = file.length();

		long fileSizeInKB = fileSizeInBytes / (1024);

		if (fileSizeInKB > 200) {
			System.out.println("fileSizeInKB > 200 ==> " + fileSizeInKB + " " + fullPath);
			// resize smaller by 50%
			double percent = 0.9;
			if (fileSizeInKB > 500) {
				percent = 0.75;
			}
			if (fileSizeInKB > 900) {
				percent = 0.4;
			}
			if (fileSizeInKB > 10000) {
				percent = 0.1;
			}
			String originImagePath = fullPath.toLowerCase().replace(ext, "-origin." + ext);

			try {
				File dest = new File(originImagePath);
				if (!dest.exists()) {
					file.renameTo(dest);
				}
				System.out.println(" originImagePath " + originImagePath);

				System.out.println(" fullPath " + fullPath);

				// if (!fullPath.contains("-origin."))
				{
					ImageUtil.resize(originImagePath, fullPath, percent);
				}
			} catch (Exception e) {
				System.err.println("fail at " + originImagePath);
				e.printStackTrace();
			}
		}

		// System.out.println(fullPath);
	}

	public static void main(String[] args) {
		try {
			Files.walk(Paths.get(folderPath)).filter(Files::isRegularFile).filter(ImageOptimizer::imageFilter)
					.forEach(ImageOptimizer::processFile);
			// Files.walk(Paths.get(folderPath)).filter(Files::isRegularFile).forEach(ImageOptimizer::deleteImage);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
