import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

/** 
 * Interface welches Sockets mit unterschiedlichen Protokolle darstellt.
 * @author D.Doering, dorina.doering@yahoo.de
 * @author D.Goller, goller@hm.edu
 * @version 29.11.2016
 */
public abstract class MultiSocket implements java.io.Closeable{

	protected final MultiSocket socket;
	
	protected final MeasureTool send;
	
	protected final MeasureTool receive;
	
	private final boolean host;
	
	
	public MultiSocket(String className, boolean host) {
		this.socket = null;
		this.host = host;
		send = new MeasureTool(className);
		receive = new MeasureTool(className);
	}
	
	public MultiSocket(String className, MultiSocket socket) {
		if (socket == null) {
			throw new NullPointerException("socket is null");
		}
		this.socket = socket;
		host = socket.host;
		send = new MeasureTool(className);
		receive = new MeasureTool(className);
	}
	
	public boolean isHost() {
		return host;
	}

	protected InputStream in() {
		if (socket == null) {
			throw new UnsupportedOperationException();
		} else {
			return socket.getInputStream();
		}		
	}
	
	protected OutputStream out() {
		if (socket == null) {
			throw new UnsupportedOperationException();
		} else {
			return socket.getOutputStream();
		}	
	}
	
	/**
	 * Baut eine Verbindung auf.
	 * @throws IOException wenn beim Verbindugsaufbau ein Fehler auftritt.
	 */
	public void connect() throws Exception {
		if (socket == null) {
			throw new UnsupportedOperationException();
		} else {
			socket.connect();
		}
	}
	
	public OutputStream getOutputStream() {
		throw new UnsupportedOperationException();
	}
	
	
	/**
	 * Sendet ein Byte Array.
	 * @param data Das zu sendente Byte Array.
	 * @throws IOException wenn beim senden ein Fehler auftritt.
	 */
//	public void send(byte[] data) throws IOException {
//		throw new UnsupportedOperationException();
//	}
	
//	default void send(byte data) throws IOException {
//		byte[] buff = new byte[1];
//		buff[0] = data;
//		send(buff);
//	}
//	
//	default void send(byte[] data, int length) throws IOException {
//		send(data, 0, length);
//	}
//	
//	default void send(byte[] data, int off, int length) throws IOException {
//		send(Arrays.copyOfRange(data, off, length));
//	}
	
	
//	/**
//	 * Empfangt ein Byte Array.
//	 * @return Falls die Verbindung beendet wurde null, sonst as zu empfangende Byte Array.
//	 * @throws IOException wenn beim empfangen ein Fehler auftritt.
//	 */
//	public byte[] receive() throws IOException {
//		throw new UnsupportedOperationException();
//	}
	
	public InputStream getInputStream() {
		throw new UnsupportedOperationException();
	}
	
	
	
//	
//	default int receive(byte[] data) throws IOException {
//		return receive(data, data.length);
//	}
//	
//	default int receive(byte[] data, int length) throws IOException {
//		return receive(data, 0, length);
//	}
//	
//	default int receive(byte[] data, int off, int length) throws IOException {
//		byte[] buff = receive();
//		if (buff == null) {
//			return -1;
//		}
//		
//		System.arraycopy(buff, 0, data, off, length);
//		return buff.length;
//	}
	
	
	
	/**
	 * Setzt einen Timeout auf blockiernde Aufrufe von read().
	 * @param ms Timeout in ms.
	 * @throws SocketException wenn einn Fehler auftritt.
	 */
	public void setReadTimeout(int ms) throws SocketException {
		if (socket == null) {
			throw new UnsupportedOperationException();
		} else {
			socket.setReadTimeout(ms);
		}
	}
	
	protected int getReadTimeout() throws SocketException {
		if (socket == null) {
			throw new UnsupportedOperationException();
		} else {
			return socket.getReadTimeout();
		}
	}


	private String getSendStats() {
		return send + (socket == null ? "" : System.lineSeparator() + socket.getSendStats());
	}
	
	private String getReceieStats() {
		return receive + (socket == null ? "" : System.lineSeparator() + socket.getReceieStats());
	}


	
	public String localAdress() {
		if (socket == null) {
			throw new UnsupportedOperationException();
		} else {
			return socket.localAdress();
		}
	}
	
	public String destAdress() {
		if (socket == null) {
			throw new UnsupportedOperationException();
		} else {
			return socket.destAdress();
		}
	}

	@Override
	public void close() throws IOException {
		if (socket != null) {
			socket.close();
		}	
	}

	@Override
	public String toString() {
		return "send:" + System.lineSeparator()
				+ getSendStats() + System.lineSeparator()
				+ "receive:" + System.lineSeparator()
				+ getReceieStats();
	}
	
	
}
