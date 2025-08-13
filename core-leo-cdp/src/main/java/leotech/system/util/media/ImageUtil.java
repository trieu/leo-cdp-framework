package leotech.system.util.media;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class ImageUtil {

	/**
	 * @param imageFile
	 * @param watermarkFile
	 */
	public static File addWatermark(File imageFile, File watermarkFile, int widthImageWatermark) {
		try {
			long t = System.currentTimeMillis();
			String imgFileName = imageFile.getName();
			String formatImg = FilenameUtils.getExtension(imgFileName).toLowerCase();
			String newName = imageFile.getAbsolutePath().replace(imgFileName, t + "-" + imgFileName);
			File destFile = new File(newName);
			FileUtils.copyFile(imageFile, destFile);

			BufferedImage image = ImageIO.read(destFile);
			BufferedImage watermark = ImageIO.read(watermarkFile);

			Graphics g = image.getGraphics();
			int w1 = watermark.getWidth();
			int h1 = watermark.getHeight();
			int w2 = widthImageWatermark;
			int h2 = Math.floorDiv(h1 * w2, w1);

			int x = (image.getWidth() / 2) - (w2 / 2);
			int y = image.getHeight() - h2 - 4;
			g.drawImage(watermark, x, y, w2, h2, null);

			ImageIO.write(image, formatImg, destFile);
			return destFile;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Resizes an image to a absolute width and height (the image may not be
	 * proportional)
	 * 
	 * @param inputImagePath  Path of the original image
	 * @param outputImagePath Path to save the resized image
	 * @param scaledWidth     absolute width in pixels
	 * @param scaledHeight    absolute height in pixels
	 * @throws IOException
	 */
	public static void resize(String inputImagePath, String outputImagePath, int scaledWidth, int scaledHeight)
			throws IOException {
		// reads input image
		File inputFile = new File(inputImagePath);
		BufferedImage inputImage = ImageIO.read(inputFile);

		// creates output image
		BufferedImage outputImage = new BufferedImage(scaledWidth, scaledHeight, inputImage.getType());

		// scales the input image to the output image
		Graphics2D g2d = outputImage.createGraphics();
		g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
		g2d.dispose();

		// extracts extension of output file
		String formatName = outputImagePath.substring(outputImagePath.lastIndexOf(".") + 1);

		// writes to output file
		File outputFile = new File(outputImagePath);
		outputFile.createNewFile();
		ImageIO.write(outputImage, formatName, outputFile);
	}

	/**
	 * Resizes an image by a percentage of original size (proportional).
	 * 
	 * @param inputImagePath  Path of the original image
	 * @param outputImagePath Path to save the resized image
	 * @param percent         a double number specifies percentage of the output
	 *                        image over the input image.
	 * @throws IOException
	 */
	public static void resize(String inputImagePath, String outputImagePath, double percent) throws IOException {
		File inputFile = new File(inputImagePath);
		BufferedImage inputImage = ImageIO.read(inputFile);
		int scaledWidth = (int) (inputImage.getWidth() * percent);
		int scaledHeight = (int) (inputImage.getHeight() * percent);
		resize(inputImagePath, outputImagePath, scaledWidth, scaledHeight);
	}
	
	/**
	 * @param RenderedImage img
	 * @param String formatName
	 * @return base 64 string
	 */
	public static String imageToBase64String(final RenderedImage img, final String formatName) {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();

		try {
			ImageIO.write(img, formatName, os);
			return Base64.getEncoder().encodeToString(os.toByteArray());
		} catch (final IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}
}
