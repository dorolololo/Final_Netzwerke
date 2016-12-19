import java.io.IOException;
import java.net.SocketException;

public class BadChannel implements MultiSocket{

	private static final double ERROR_RATE = 0.5;
	
	private static final double DROP_RATE = 0.25;
	
	private static final double DUPLICATE_RATE = 0.3;
		
	private final MultiSocket src;
	
	private MeasureTool mTool = new MeasureTool("BadChannel");
	
	private byte[] dubData;

	public BadChannel(MultiSocket src) {
		this.src = src;
	}

	@Override
	public void close() throws IOException {
		src.close();
	}

	@Override
	public void connect() throws Exception {
		src.connect();
	}

	@Override
	public void send(byte[] data) throws IOException {
		src.send(data);
	}

	@Override
	public byte[] receive() throws IOException {
		byte[] result;
		if (dubData != null) {
			result = dubData;
			dubData = null;
		} else {
			result = src.receive();
		}
		final double random = Math.random();
		if ((int)(ERROR_RATE / random) > 0) {
			mTool.printDBG("error");
			result[(int)(random * 1000) % result.length] = (byte)random;
		}
		if ((int)(DROP_RATE / random) > 0) {
			mTool.printDBG("drop");
			result =  src.receive();
		}
		if ((int)(DUPLICATE_RATE / random) > 0) {
			mTool.printDBG("dublicate");
			dubData = result;
		}
		return result;
	}

	@Override
	public void setReadTimeout(int ms) throws SocketException {
		src.setReadTimeout(ms);
	}

}
