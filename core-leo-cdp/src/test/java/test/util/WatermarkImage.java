package test.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.docx4j.org.capaxit.imagegenerator.Margin;
import org.docx4j.org.capaxit.imagegenerator.Style;
import org.docx4j.org.capaxit.imagegenerator.TextImage;
import org.docx4j.org.capaxit.imagegenerator.imageexporter.ImageType;
import org.docx4j.org.capaxit.imagegenerator.imageexporter.ImageWriterFactory;
import org.docx4j.org.capaxit.imagegenerator.impl.TextImageImpl;

public class WatermarkImage {

	static void test1() throws Exception {

		TextImage testImage = new TextImageImpl(1000, 900, new Margin(4, 10));

		// Declare or read the fonts you need
		Font header = new Font("Tahoma", Font.BOLD, 32);
		Font plain = new Font("Tahoma", Font.PLAIN, 24);

		// Read in any images you need

		// BufferedImage warning = ImageIO.read(new
		// URL("https://i-kinhdoanh.vnecdn.net/2018/11/09/n3-1541746749_r_680x0.jpg"));
		BufferedImage logo = ImageIO.read(new File("./public/images/leotech-logo.png"));
		BufferedImage robot = ImageIO
				.read(new URL("http://sciencenordic.com/sites/default/files/imagecache/620x/nao_robot_laerer-2.jpg"));

		// 1. specify font and write text with a newline
		testImage.withFont(header).writeLine("This Headline Image is made by Content Bot").newLine();
		// 2. enabled the wrapping of text and write text which is automatically wrapped

		testImage.withFont(plain).wrap(true).write("Do you think ").newLine();
		testImage.withFont(plain).wrap(true).write("Ngôi nhà rao bán đắt nhất nước Mỹ giá 245 triệu USD!").newLine(2);

		// 3. Disable text-wrapping again. Write underlined text.
		testImage.wrap(false).withColor(Color.BLUE).withFontStyle(Style.UNDERLINED)
				.write("This line of text is underlinedand blue.").withFontStyle(Style.PLAIN).newLine();
		testImage.writeLine("You can also embed images in your generated image:");
		// 4. embed the actual image. Here we use absolute positioning
		testImage.write(logo, 400, 300);
		testImage.write(robot, 100, 450);

		org.docx4j.org.capaxit.imagegenerator.imageexporter.ImageWriter imageWriter = ImageWriterFactory
				.getImageWriter(ImageType.PNG);

		imageWriter.writeImageToFile(testImage, new File("test1.png"));
	}

	public static void main(String[] args) {
		try {
			test1();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void test0() {
		try {
			final BufferedImage image = ImageIO
					.read(new URL("https://i-kinhdoanh.vnecdn.net/2018/11/09/n3-1541746749_r_680x0.jpg"));

			Graphics g = image.getGraphics();
			g.setFont(g.getFont().deriveFont(30f));
			g.drawString("Ngôi nhà rao bán đắt nhất nước Mỹ giá 245 triệu USD! \n abcbs", 100, 100);
			g.dispose();

			ImageIO.write(image, "png", new File("test.png"));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
