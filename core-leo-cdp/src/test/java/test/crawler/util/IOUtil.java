package test.crawler.util;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

public class IOUtil {
	
	/*
	 *  check if the (piped/file) is (writer/reader) or (outputstream/inputstream)
	 */
	public static boolean isCharBasedMedium(Object ioMedium) throws Exception {
		if (Writer.class.isAssignableFrom(ioMedium.getClass()) || Reader.class.isAssignableFrom(ioMedium.getClass()))
			return true;
		else if (OutputStream.class.isAssignableFrom(ioMedium.getClass()) || InputStream.class.isAssignableFrom(ioMedium.getClass())) {
			return false;
		} else {
			throw new Exception("Invalid Argument! Expect io medium object !");
		}
	}
}
