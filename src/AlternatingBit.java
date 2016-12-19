import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class AlternatingBit implements MultiSocket{

	// TODO dynamischer timeout
	private static final int TIMEOUT = 500; 
	
	// TODO Abstarkte Klasse?
	private final MultiSocket socket;

	// Timeout zur Sicherheit sichern
	private int cTimeout = 0;
	
	private boolean connected = false;
	
	private final boolean host;
	
	private boolean sendSeq = false;
	
	private boolean receiveSeq = false;
	
	private MeasureTool mTool = new MeasureTool("AlternatingBit");
	
	/**
	 * Erzeugt ein neues AlternatingBit Objekt.
	 * @param port Die Portnummer an welcher dieser Socket auf Verbindungen wartet.
	 * @throws IOException
	 */
	public AlternatingBit(int port) throws SocketException {
		//socket = new BadChannel(new UdpSocket(port));
		socket = new UdpSocket(port);
		host = true;
	}
	
	/**
	 * Erzeugt ein neues AlternatingBit Objekt.
	 * @param host Die Zieladresse mit welcher sich diese Socket verbinden soll.
	 * @param port Der Zielport mit welcher sich diese Socket verbinden soll.
	 * @throws IOException
	 */
	public AlternatingBit(String host, int port) throws SocketException, UnknownHostException {
		socket = new UdpSocket(host, port);
		this.host = false;
	}
	
	@Override
	public void close() throws IOException {
		socket.close();
	}

	@Override
	public void connect() throws Exception {
		// TODO Errorhandling , meherere gleichzeitige connectons
		mTool.printDBG("connecting host(" + host + ")");
		if (host) {
			send(receive());  // Verbindungspacket empfangen und bestaetigen 
			mTool.printDBG("received and send conPacket");
		} else {
			socket.setReadTimeout(1000); // Alle paar Sekunden versuchen den Server zu erreichen
			while (true) {
				send(new byte[0]);
				mTool.printDBG("send conPacket");
				try {
					receive();
					mTool.printDBG("received conPacket");
					break;
				} catch (SocketTimeoutException e) {
					// try again
				}	
			}					
			socket.setReadTimeout(cTimeout);
		}
		connected = true;
	}

	@Override
	public void send(byte[] data) throws IOException {
		final int seq = sendSeq ? 1:0;
		Packet sendPacket = new Packet(seq, data);
		Packet recPacket = null;
		while (recPacket == null) {
			socket.send(sendPacket.getBuffer());
			mTool.printDBG("send Packet seq(" + seq + ")");//, data(" + Arrays.toString(data) + ")");
			try {
				long start = System.currentTimeMillis();
				do {
					// TODO try some rounds and give up after
					int remainTime = (TIMEOUT - (int)(System.currentTimeMillis() - start));
					socket.setReadTimeout(remainTime);
					recPacket = new Packet(socket.receive());
					mTool.printDBG("recived ackPacket seq(" + recPacket.getSeq() + "), supposed seq(" + seq + "), valid(" + recPacket.isValid() + ")");
				} while (!recPacket.isValid() || recPacket.getSeq()!= seq);
			} catch (SocketTimeoutException e) {
				recPacket = null;
			}
		}
		sendSeq = !sendSeq;
		socket.setReadTimeout(cTimeout);
	}

	@Override
	public byte[] receive() throws IOException {
		final int seq = receiveSeq ? 1:0;
		Packet recPacket = null;
		while (recPacket == null) {
			recPacket = new Packet(socket.receive());
			mTool.printDBG("recived Packet seq(" + recPacket.getSeq() + "), supposed seq(" + seq + "), valid(" + recPacket.isValid() + ")");
			if (recPacket.isValid()) {
				final Packet sendPacket = new Packet(recPacket.getSeq(), new byte[0]);
				socket.send(sendPacket.getBuffer());
				mTool.printDBG("send ackPacket seq(" + recPacket.getSeq() + ")");
				if (recPacket.getSeq()!= seq) {
					recPacket = null;
				}
			} else {
				recPacket = null;
			}
		}	
		receiveSeq = !receiveSeq;
		return recPacket.getData();
	}

	@Override
	public void setReadTimeout(int ms) throws SocketException {
		cTimeout = ms;
		socket.setReadTimeout(ms);
		
	}

}
