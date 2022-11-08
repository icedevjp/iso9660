package icedev.io;

import java.io.*;

public class LittleEndianInputStream extends FilterInputStream {
	public LittleEndianInputStream(InputStream in) {
		super(in);
	}
	
	public char readUint8() throws IOException {
        int b1 = in.read();
        if ((b1) < 0)
            throw new EOFException();
        return (char) (b1);
	}

	public char readUint16() throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        if ((b1 | b2) < 0)
            throw new EOFException();
        return (char) (b1 | (b2 << 8));
	}

	public char readUint16BE() throws IOException {
        int b2 = in.read();
        int b1 = in.read();
        if ((b1 | b2) < 0)
            throw new EOFException();
        return (char) (b1 | (b2 << 8));
	}

	public int readInt32() throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        int b3 = in.read();
        int b4 = in.read();
        if ((b1 | b2 | b3 | b4) < 0)
            throw new EOFException();
        return (b1 | (b2 << 8) | (b3 << 16) | (b4 << 24));
	}
	
	public int readInt32BE() throws IOException {
        int b4 = in.read();
        int b3 = in.read();
        int b2 = in.read();
        int b1 = in.read();
        if ((b1 | b2 | b3 | b4) < 0)
            throw new EOFException();
        return (b1 | (b2 << 8) | (b3 << 16) | (b4 << 24));
	}
	
	public char[] readASCI(char[] array, int offset, int num) throws IOException {
		for(int i=0; i<num; i++) {
			int read = in.read();
			if(read<0) {
	            throw new EOFException();
			}
			array[offset + i] = (char) read;
		}
		return array;
	}

	public char[] readASCI(char[] array) throws IOException {
		return readASCI(array, 0, array.length);
	}
	
	public char[] readASCI(int num) throws IOException {
		return readASCI(new char[num], 0, num);
	}
	public String readASCIIString(int length) throws IOException {
		byte[] buffer = in.readNBytes(length);
		return new String(buffer, 0, strlen(buffer)).stripTrailing();
	}
	
	private static int strlen(byte[] buf) {
		int strlen = 0;
		while(strlen < buf.length) {
			if(buf[strlen] == 0)
				break;
			strlen++;
		}
		return strlen;
	}
	
	public static LittleEndianInputStream wrap(File file) throws IOException {
		return new LittleEndianInputStream(new BufferedInputStream(new FileInputStream(file), 2048));
	}
}
