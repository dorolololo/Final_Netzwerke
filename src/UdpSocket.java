import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/** 
 * Klasse welche das UDP Protokoll als MultiSocket implementiert.
 * @author D.Doering, dorina.doering@yahoo.de
 * @author D.Goller, goller@hm.edu
 * @version 29.11.2016
 */
public class UdpSocket extends MultiSocket{
	
	public static final int MAX_PACKET_SIZE = 65535; 
	
	/** Socket für die aufgebaute Verbindung. */
	private final DatagramSocket socket;
	
	/** DatagramPacket für die aufgebaute Verbindung. */
	private final DatagramPacket udpPacket;
	
	private final InputStream inputStream = new InputStream() {
		
		ByteBuffer buffer = (ByteBuffer) ByteBuffer.wrap(new byte[MAX_PACKET_SIZE]).limit(0);
		
		private void initBuffer() throws IOException {
			if (!buffer.hasRemaining()) {
				receive.startTimer();
				udpPacket.setData(buffer.array());
				socket.receive(udpPacket);
				receive.count();
				buffer.clear();
				final int length = udpPacket.getLength();
				buffer.limit(length);
				receive.addSizeEndTimer(length);
			}
		}
		
		@Override
		public int read() throws IOException {
			initBuffer();
			return buffer.get();
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			initBuffer();
			final int size = Math.min(len, buffer.remaining());
			buffer.get(b, off, size);
			return size;
		}

		@Override
		public int available() throws IOException {
			return buffer.remaining();
		}

		@Override
		public void close() throws IOException {
			close();
		}
		
		
	};
	
	private final OutputStream outputStream = new OutputStream() {
		
		ByteBuffer buffer = ByteBuffer.wrap(new byte[MAX_PACKET_SIZE]);
				
		@Override
		public void write(int b) throws IOException {
			buffer.put((byte)b);			
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			buffer.put(b, off, len);
		}
		
		@Override
		public void flush() throws IOException {
			buffer.flip();
			send.startTimer();
			udpPacket.setData(buffer.array(), buffer.position(), buffer.limit());
			socket.send(udpPacket);
			send.count();
			send.addSizeEndTimer(buffer.limit());
			buffer.clear();
		}

		@Override
		public void close() throws IOException {
			close();
		}	
		
	};
	
	/**
	 * Erzeugt ein neues UdpSocket Objekt.
	 * @param port Die Portnummer an welcher dieser Socket auf Verbindungen wartet.
	 * @throws IOException
	 */
	public UdpSocket(int port) throws SocketException {
		super("UdpSocket", true);
		socket = new DatagramSocket(port);
		udpPacket = new DatagramPacket(new byte[0], 0);
	}
	
	/**
	 * Erzeugt ein neues UdpSocket Objekt.
	 * @param host Die Zieladresse mit welcher sich diese Socket verbinden soll.
	 * @param port Der Zielport mit welcher sich diese Socket verbinden soll.
	 * @throws IOException
	 */
	public UdpSocket(String host, int port) throws SocketException, UnknownHostException {
		super("UdpSocket", false);
		socket = new DatagramSocket(null);
		udpPacket = new DatagramPacket(new byte[0], 0, InetAddress.getByName(host), port);
	}	
	
	@Override
	public void connect() throws IOException {
		// Kein Verbindungsaufbau bei UDP.
	}
	
//	@Override
//	public void send(byte[] data) throws IOException {
//		udpPacket.setData(data);
//		send.start();
//		socket.send(udpPacket);
//		send.addPSize(data.length);
//	}
//
//	@Override
//	public byte[] receive() throws IOException {
//		byte[] buffer = new byte[MAX_PACKET_SIZE];
//		udpPacket.setData(buffer);
//		receive.start();
//		socket.receive(udpPacket);
//		receive.addPSize(udpPacket.getLength());
//		return Arrays.copyOf(buffer, udpPacket.getLength());
//	}
	
	
	
	@Override
	public OutputStream getOutputStream() {
		return outputStream;
	}

	@Override
	public InputStream getInputStream() {
		return inputStream;
	}

	
	@Override
	public String localAdress() {
		InetAddress address = socket.getLocalAddress();
		return address == null ? "N/A" : address.getHostAddress();
	}
	
	@Override
	public String destAdress() {
		InetAddress address = udpPacket.getAddress();
		return address == null ? "N/A" : address.getHostAddress();
	}

	@Override
	public void setReadTimeout(int ms) throws SocketException {
		socket.setSoTimeout(ms);
	}
	
	@Override
	protected int getReadTimeout() throws SocketException {
		return socket.getSoTimeout();
	}

	@Override
	public void close() {
		socket.close();		
	}

	

}
