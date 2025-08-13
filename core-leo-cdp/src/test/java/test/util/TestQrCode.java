package test.util;

import leotech.system.util.QrCodeUtil;

public class TestQrCode {

	private static final String QR_CODE_IMAGE_PATH = "./MyQRCode.png";

	public static void main(String[] args) {
		try {
			QrCodeUtil.createQrCodeWithLogo("https://everon.com/",  "/home/thomas/Pictures/everon-logo.png", QR_CODE_IMAGE_PATH);
			System.out.println("QR Code generated successfully.");
		} catch (Exception e) {
			System.err.println("Could not generate QR Code, " + e.getMessage());
		}
	}
}
