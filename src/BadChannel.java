import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BadChannel extends MultiSocket{

	private static final double ERROR_RATE = 0.5;
	
	private static final double DROP_RATE = 0.25;
	
	private static final double DUPLICATE_RATE = 0.3;
	
	private boolean dublicate;
	
	private byte[] buffer;
	
	private int read;
	
	private final InputStream inputStream = new InputStream() {
		

		@Override
		public int read() throws IOException {
			return in().read();
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return bad(b, off, len);
		}

		@Override
		public int available() throws IOException {
			return in().available();
		}

		@Override
		public void close() throws IOException {
			close();
		}
		
		
	};

	public BadChannel(MultiSocket src) {
		super("BadChannel", src);
	}


	@Override
	public OutputStream getOutputStream() {
		return out();
	}

	@Override
	public InputStream getInputStream() {
		return inputStream;
	}


	private int bad(byte[] b, int off, int len) throws IOException {
		if (buffer != null) {
			return pass(b, off, len);
		}
		buffer = getRead();
		dublicate = isDublicate();
		if (isError()) {
			final double random = Math.random();
			buffer[(int)(random * 1000) % buffer.length] = (byte)random;
		}
		if (isDrop()) {
			buffer = getRead();
		}		
		return pass(b, off, len);
	}

	private int pass(byte[] b, int off, int len) {
		int remainLength = buffer.length - read;
		int minlength = Math.min(len, remainLength);
		System.arraycopy(buffer, read, b, off, minlength);
		if (minlength < remainLength) {
			read += minlength;
		} else {
			read = 0;
			if(dublicate) {
				dublicate = false;
			} else {
				buffer = null;
			}
		}
		return minlength;
	}
	
	private byte[] getRead() throws IOException {
		byte test = (byte) in().read();
		byte[] read = new byte[in().available()+1];
		read[0] = test;
		in().read(read, 1, read.length);
		return read;
	}
	
	private boolean isDublicate() {
		final double random = Math.random();
		if ((int)(DUPLICATE_RATE / random) > 0) {
			receive.printDBG("dublicate");
			return true;
		} 
		return false;
	}
	
	private boolean isError() {
		final double random = Math.random();
		if ((int)(ERROR_RATE / random) > 0) {  // TODO <- funktioniert das wirklich?
			receive.printDBG("error");
			return true;
		} 
		return false;
	}
	
	private boolean isDrop() {
		final double random = Math.random();
		if ((int)(DROP_RATE / random) > 0) {
			receive.printDBG("drop");
			return true;
		} 
		return false;
	}
	
}
