package leotech.system.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class ReversedFileReader {
    private RandomAccessFile randomAccessFile = null;
    private long currentPosition;

    public ReversedFileReader(File file) throws IOException {
        this.randomAccessFile = new RandomAccessFile(file, "r");
        this.currentPosition = file.length();
    }

    public String readLine() throws IOException {
        if (currentPosition <= 0) {
            return null;
        }

        StringBuilder line = new StringBuilder();
        while (currentPosition > 0) {
            randomAccessFile.seek(--currentPosition);
            int readByte = randomAccessFile.readByte();

            if (readByte == '\n' && line.length() > 0) {
                break;
            } else if (readByte != '\n' && readByte != '\r') {
                line.append((char) readByte);
            }
        }

        if (line.length() == 0 && currentPosition <= 0) {
            return null;
        }

        return line.reverse().toString();
    }

    public void close() throws IOException {
    	if(randomAccessFile != null) {
    		 randomAccessFile.close();
    	}
    }


}

