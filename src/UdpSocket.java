import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

/** 
 * Klasse welche das UDP Protokoll als MultiSocket implementiert.
 * @author D.Doering, dorina.doering@yahoo.de
 * @author D.Goller, goller@hm.edu
 * @version 29.11.2016
 */
public class UdpSocket implements MultiSocket{
		// TODO RootSocket statt MultiSocket?
	
	
	/** Socket für die aufgebaute Verbindung. */
	private final DatagramSocket socket;
	
	/** DatagramPacket für die aufgebaute Verbindung. */
	private final DatagramPacket udpPacket;
	
	/**
	 * Erzeugt ein neues UdpSocket Objekt.
	 * @param port Die Portnummer an welcher dieser Socket auf Verbindungen wartet.
	 * @throws IOException
	 */
	public UdpSocket(int port) throws SocketException {
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
		socket = new DatagramSocket(null);
		udpPacket = new DatagramPacket(new byte[0], 0, InetAddress.getByName(host), port);
	}	
	
	@Override
	public void connect() throws IOException {
		// Kein Verbindungsaufbau bei UDP.
	}
	
	@Override
	public void send(byte[] data) throws IOException {
		udpPacket.setData(data);
		socket.send(udpPacket);
	}

	@Override
	public byte[] receive() throws IOException {
		byte[] buffer = new byte[Packet.MAX_PACKET_SIZE];
		udpPacket.setData(buffer);
		socket.receive(udpPacket);
		return Arrays.copyOf(buffer, udpPacket.getLength());
	}
	
	@Override
	public void setReadTimeout(int ms) throws SocketException {
		socket.setSoTimeout(ms);
		
	}
	
	@Override
	public void close() {
		socket.close();		
	}

	

}
