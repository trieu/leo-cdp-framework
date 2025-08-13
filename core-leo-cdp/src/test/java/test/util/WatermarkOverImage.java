package test.util;

import java.io.File;
import java.io.IOException;

import leotech.system.util.media.ImageUtil;

public class WatermarkOverImage {

	public static void main(String[] args) throws IOException {
		File watermarkFile = new File("./public/images/product-logo/logo-lysaght.png");

		ImageUtil.addWatermark(new File("/home/platform/Pictures/bluescope/lysaght-page.png"), watermarkFile, 250);

	}
}
