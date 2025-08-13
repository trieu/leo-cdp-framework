package test.capcha;

import java.awt.image.BufferedImage;

import leotech.system.util.media.ImageUtil;
import net.logicsquad.nanocaptcha.content.FiveLetterFirstNameContentProducer;
import net.logicsquad.nanocaptcha.content.NumbersContentProducer;
import net.logicsquad.nanocaptcha.image.ImageCaptcha;

public class TestImageCapcha {

	

	public static void main(String[] args) {
		ImageCaptcha imageCaptcha = new ImageCaptcha.Builder(180, 60)
				.addContent(new NumbersContentProducer(3))
				.addContent(new FiveLetterFirstNameContentProducer())
				.addNoise()
				.addBorder().build();
		String str = imageCaptcha.getContent();
		System.out.println(str);
		BufferedImage img = imageCaptcha.getImage();
		String base64 = ImageUtil.imageToBase64String(img, "png");
		System.out.println(base64);

	}

}
