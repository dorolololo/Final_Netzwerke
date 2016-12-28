import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

/** 
 * Klasse welche das TCP Protokoll als MultiSocket implementiert.
 * @author D.Doering, dorina.doering@yahoo.de
 * @author D.Goller, goller@hm.edu
 * @version 29.11.2016
 */
public class TcpSocket extends MultiSocket{

	//public static final int MAX_PACKET_SIZE = 65535;
	
	/** Socket für Verbindungsakzeptiernede Server. */
	private final ServerSocket serverSocket;
	
	/** Socket für die aufgebaute Verbindung. */
	private Socket socket;
	
	/** Zieladresse für einen Client Socket. */
	private final SocketAddress address;
	
	/** InputStream für die aufgebaute Verbindung. */
	private InputStream inStream;
	
	/** OutputStream für die aufgebaute Verbindung. */
	private OutputStream outStream;
	
	/**
	 * Erzeugt ein neues TcpSocket Objekt.
	 * @param port Die Portnummer an welcher dieser Socket auf Verbindungen wartet.
	 * @throws IOException
	 */
	public TcpSocket(int port) throws IOException {
		super("TcpSocket", true);
		this.serverSocket = new ServerSocket(port);
		this.address = null;
	}
	
	/**
	 * Erzeugt ein neues TcpSocket Objekt.
	 * @param host Die Zieladresse mit welcher sich diese Socket verbinden soll.
	 * @param port Der Zielport mit welcher sich diese Socket verbinden soll.
	 */
	public TcpSocket(String host, int port) {
		super("TcpSocket", false);
		this.serverSocket = null;
		this.address = new InetSocketAddress(host, port);
	}
	
	@Override
	public void connect() throws IOException, InterruptedException {
		if (serverSocket == null) {
			boolean isConnected = false;
			while (!isConnected) {
				try {
					socket = new Socket();
					socket.connect(address);
					isConnected = true;
				} catch (ConnectException e) {
					Thread.sleep(1000);
					// try again
				}
			}
		} else {
			socket = serverSocket.accept();
		}
		inStream = socket.getInputStream();
		outStream = socket.getOutputStream();	
	}


	@Override
	public OutputStream getOutputStream() {
		return outStream;
	}

//	@Override
//	public void send(byte[] data) throws IOException {
//		send.start();
//		outStream.write(data);
//		outStream.flush();
//		send.addPSize(data.length);
//	}
//
//	@Override
//	public byte[] receive() throws IOException {
//		byte[] buffer = new byte[MAX_PACKET_SIZE];
//		receive.start();
//		int bytesRead = inStream.read(buffer);	
//		receive.addPSize(bytesRead);
//		return bytesRead > 0 ? Arrays.copyOf(buffer, bytesRead) : null;
//	}

	@Override
	public InputStream getInputStream() {
		return inStream;
	}

	
	@Override
	public String localAdress() {
		InetAddress address = socket.getLocalAddress();
		return address == null ? "N/A" : address.getHostAddress();
	}

	@Override
	public String destAdress() {
		InetAddress address = socket.getInetAddress();
		return address == null ? "N/A" : address.getHostAddress();
	}
	
	@Override
	protected int getReadTimeout() throws SocketException {
		return socket.getSoTimeout();
	}

	@Override
	public void setReadTimeout(int ms) throws SocketException{
		socket.setSoTimeout(ms);
	}

	
	@Override
	public void close() throws IOException {
		if (serverSocket != null) {
			serverSocket.close();
		}	
		if (socket != null) {
			socket.close();
		}	
	}

}
