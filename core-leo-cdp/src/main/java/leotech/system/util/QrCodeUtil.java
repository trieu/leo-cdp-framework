package leotech.system.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

import javax.imageio.ImageIO;

import io.nayuki.qrcodegen.QrCode;
import io.nayuki.qrcodegen.QrCode.Ecc;

/**
 * Utility class for
 * <a href="https://www.nayuki.io/page/qr-code-generator-library"> QR Code
 * generator library </a>
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class QrCodeUtil {

	private static final String PUBLIC_QRCODE = "./public/qrcode/";

	private static BufferedImage toImage(QrCode qr, int scale, int border) {
		return toImage(qr, scale, border, 0xFFFFFF, 0x000000);
	}

	/**
	 * Returns a raster image depicting the specified QR Code, with the specified
	 * module scale, border modules, and module colors.
	 * <p>
	 * For example, scale=10 and border=4 means to pad the QR Code with 4 light
	 * border modules on all four sides, and use 10&#xD7;10 pixels to represent each
	 * module.
	 * 
	 * @param qr         the QR Code to render (not {@code null})
	 * @param scale      the side length (measured in pixels, must be positive) of
	 *                   each module
	 * @param border     the number of border modules to add, which must be
	 *                   non-negative
	 * @param lightColor the color to use for light modules, in 0xRRGGBB format
	 * @param darkColor  the color to use for dark modules, in 0xRRGGBB format
	 * @return a new image representing the QR Code, with padding and scaling
	 * @throws NullPointerException     if the QR Code is {@code null}
	 * @throws IllegalArgumentException if the scale or border is out of range, or
	 *                                  if {scale, border, size} cause the image
	 *                                  dimensions to exceed Integer.MAX_VALUE
	 */
	private static BufferedImage toImage(QrCode qr, int scale, int border, int lightColor, int darkColor) {
		Objects.requireNonNull(qr);
		if (scale <= 0 || border < 0)
			throw new IllegalArgumentException("Value out of range");
		if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale)
			throw new IllegalArgumentException("Scale or border too large");

		BufferedImage result = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale,
				BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < result.getHeight(); y++) {
			for (int x = 0; x < result.getWidth(); x++) {
				boolean color = qr.getModule(x / scale - border, y / scale - border);
				result.setRGB(x, y, color ? darkColor : lightColor);
			}
		}
		return result;
	}

	/**
	 * @param data
	 * @return
	 */
	public final static String generate(String data) {
		return generate("", data);
	}

	/**
	 * @param prefix
	 * @param data
	 * @return
	 */
	public final static String generate(String prefix, String data) {
		try {
			File qrCodeFolder = new File(PUBLIC_QRCODE);
			boolean readyToCreateQR = false;
			if (qrCodeFolder.isDirectory()) {
				readyToCreateQR = true;
			} else {
				readyToCreateQR = qrCodeFolder.mkdir();
			}
			if (readyToCreateQR) {
				QrCode qr = QrCode.encodeText(data, QrCode.Ecc.HIGH);
				BufferedImage img = toImage(qr, 4, 10);
				String pathname = PUBLIC_QRCODE + prefix + "-" + IdGenerator.createHashedId(data) + ".png";
				ImageIO.write(img, "png", new File(pathname));
				return pathname;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static BufferedImage generateQRCodeImage(String text, int size) {
		QrCode qrCode = QrCode.encodeText(text, Ecc.HIGH);

		int scale = size / qrCode.size; // Scale the image
		int qrSize = qrCode.size * scale;

		BufferedImage qrImage = new BufferedImage(qrSize, qrSize, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = qrImage.createGraphics();

		// Set background color to white
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, qrSize, qrSize);

		// Draw QR code in black
		g.setColor(Color.BLACK);
		for (int y = 0; y < qrCode.size; y++) {
			for (int x = 0; x < qrCode.size; x++) {
				if (qrCode.getModule(x, y)) {
					g.fillRect(x * scale, y * scale, scale, scale);
				}
			}
		}

		return qrImage;
	}

	public static void addLogo(BufferedImage qrImage, String logoPath) throws Exception {
		Graphics2D g = qrImage.createGraphics();

		BufferedImage logo = ImageIO.read(new File(logoPath));
		int logoWidth = qrImage.getWidth() / 5;
		int logoHeight = qrImage.getHeight() / 5;
		int logoX = (qrImage.getWidth() - logoWidth) / 2;
		int logoY = (qrImage.getHeight() - logoHeight) / 2;
		g.drawImage(logo, logoX, logoY, logoWidth, logoHeight, null);

		g.dispose();
	}

	public static void createQrCodeWithLogo(String data, String logoUri, String outputFileUri) {
		try {
			BufferedImage qrImage = QrCodeUtil.generateQRCodeImage(data,500);
			QrCodeUtil.addLogo(qrImage, logoUri);
			ImageIO.write(qrImage, "PNG", new File(outputFileUri));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

}
