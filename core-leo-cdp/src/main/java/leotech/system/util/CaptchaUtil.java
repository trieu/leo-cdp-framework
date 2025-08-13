package leotech.system.util;

import java.awt.Color;
import java.awt.image.BufferedImage;

import leotech.system.util.media.ImageUtil;
import net.logicsquad.nanocaptcha.content.NumbersContentProducer;
import net.logicsquad.nanocaptcha.image.ImageCaptcha;
import net.logicsquad.nanocaptcha.image.backgrounds.GradiatedBackgroundProducer;
import net.logicsquad.nanocaptcha.image.noise.CurvedLineNoiseProducer;

/**
 * Simple Captcha for human verification
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public class CaptchaUtil {

	private static final int MAX_LEN_NUM = 8;
	private static final int HEIGHT = 82;
	private static final int WIDTH = 210;
	private static final int LINE_NOISE_WIDTH = 3;


	public static final class CaptchaData {
		final public String content;
		final public String base64Image;

		public CaptchaData(String content, String base64Image) {
			super();
			this.content = content;
			this.base64Image = base64Image;
		}
	}

	public static CaptchaData getRandomCaptcha() {
		ImageCaptcha imageCaptcha = new ImageCaptcha.Builder(WIDTH, HEIGHT)
				.addContent(new NumbersContentProducer(MAX_LEN_NUM))
				.addBackground(new GradiatedBackgroundProducer())
			    .addNoise(new CurvedLineNoiseProducer(Color.RED, LINE_NOISE_WIDTH))
			    .addNoise(new CurvedLineNoiseProducer(Color.BLUE, LINE_NOISE_WIDTH))
				.build();
		String str = imageCaptcha.getContent();
		BufferedImage img = imageCaptcha.getImage();
		String base64 = ImageUtil.imageToBase64String(img, "png");
		return new CaptchaData(str, base64);
	}
}
