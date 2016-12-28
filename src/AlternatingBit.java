import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class AlternatingBit extends MultiSocket{

	// TODO dynamischer timeout
	private static final int TIMEOUT = 100; 
	
	//private boolean connected = false;
	
	private boolean sendSeq = false;
	
	private boolean receiveSeq = false;
	
private final InputStream inputStream = new InputStream() {
		
	ByteBuffer buffer = (ByteBuffer) ByteBuffer.wrap(new byte[Packet.MAX_PACKET_SIZE]).limit(0);
	
	private void initBuffer() throws IOException {
		if (!buffer.hasRemaining()) {
			final int length = receive(buffer.array());
			buffer.clear(); // keine internen Makierungen vor dem erhalt des Arrays! -> timeout 
			buffer.position(Packet.HEADER_SIZE);
			buffer.limit(length);
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
		
		ByteBuffer buffer = ByteBuffer.wrap(new byte[Packet.DATA_SIZE]);
		
		@Override
		public void write(int b) throws IOException {
			buffer.put((byte)b);
			if (!buffer.hasRemaining()) {
				flush();
			}
		}

		private int fillBuf(byte[] b, int off, int len) throws IOException {			
			int nextLen = Math.min(len, buffer.remaining());
			buffer.put(b, off, nextLen);
			if (!buffer.hasRemaining()) {
				flush();
			}
			return nextLen;		
		}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			int nextOff = (buffer.remaining() < buffer.capacity()) ? fillBuf(b, off, len) + off : off;
			final int upLimit = len + off; // exklusiv
			for (; nextOff + Packet.DATA_SIZE <= upLimit; nextOff+= Packet.DATA_SIZE) {
				send(b, nextOff, Packet.DATA_SIZE);
			}
			fillBuf(b, nextOff, upLimit - nextOff);
		}
		
		@Override
		public void flush() throws IOException {
			buffer.flip();
			send(buffer.array(),buffer.position(),buffer.limit());
			buffer.clear();
		}

		@Override
		public void close() throws IOException {
			close();
		}	
		
	};
	
	/**
	 * Erzeugt ein neues AlternatingBit Objekt.
	 * @param port Die Portnummer an welcher dieser Socket auf Verbindungen wartet.
	 * @throws IOException
	 */
	public AlternatingBit(int port) throws SocketException {
		//this(new BadChannel(new UdpSocket(port)));
		this(new UdpSocket(port));
	}
	
	/**
	 * Erzeugt ein neues AlternatingBit Objekt.
	 * @param host Die Zieladresse mit welcher sich diese Socket verbinden soll.
	 * @param port Der Zielport mit welcher sich diese Socket verbinden soll.
	 * @throws IOException
	 */
	public AlternatingBit(String host, int port) throws SocketException, UnknownHostException {
		this(new UdpSocket(host, port));
	}
	
	public AlternatingBit(MultiSocket socket) {
		super("AlternatingBit", socket);
	}
	

	@Override
	public void connect() throws Exception {
		super.connect();
		byte[] buff = new byte[]{'b','l','a'};
		// TODO Errorhandling , meherere gleichzeitige connectons
		send.printDBG("connecting host(" + isHost() + ")");
		if (isHost()) {
			outputStream.write(buff, 0, inputStream.read(buff)); // Verbindungspacket empfangen und bestaetigen 
			outputStream.flush();
			send.printDBG("received and send conPacket");
		} else {
			int lastTimeout = getReadTimeout();
			setReadTimeout(1000); // Alle paar Sekunden versuchen den Server zu erreichen
			while (true) {
				outputStream.write(buff);
				outputStream.flush();
				send.printDBG("send conPacket");
				try {
					inputStream.read(buff);
					send.printDBG("received conPacket");
					break;
				} catch (SocketTimeoutException e) {
					send.printDBG("timeout");
					// try again
				}	
			}					
			setReadTimeout(lastTimeout);
		}
		//connected = true;
	}
	
	private void send(byte[] b, int off, int len) throws IOException {
		final int seq = sendSeq ? 1:0;
		Packet sendPacket = new Packet(seq, Packet.DAT, b, off, len);
		Packet recPacket = null;
		byte[] recBuff = new byte[Packet.MAX_PACKET_SIZE];
		int recLength = 0;
		int lastTimeout = getReadTimeout();
		send.startTimer();
		while (recPacket == null) {
			out().write(sendPacket.getHeader());
			out().write(b, off, len);
			out().flush();
			send.count();
			send.printDBG("send Packet seq(" + seq + ")");//, data(" + Arrays.toString(data) + ")");
			try {
				long start = System.currentTimeMillis(); // start timeout timer
				do {
					// TODO try some rounds and give up after
					int remainTime = (TIMEOUT - (int)(System.currentTimeMillis() - start));
					setReadTimeout(remainTime > 0 ? remainTime : 1); // TODO bessere Lösung
					recLength = in().read(recBuff);
					recPacket = new Packet(recBuff,0,recLength);
					send.printDBG("recived ackPacket seq(" + recPacket.getSeq() + "), supposed seq(" + seq + "), valid(" + recPacket.isValid() + ")");
				} while (!recPacket.isValid() || recPacket.getSeq()!= seq);
			} catch (SocketTimeoutException e) {
				send.printDBG("timeout");
				send.addDrop();
				recPacket = null;
			}
		}
		send.addSizeEndTimer(len);
		sendSeq = !sendSeq;
		setReadTimeout(lastTimeout);
	}
	
	private int receive(byte[] b) throws IOException {
		final int seq = receiveSeq ? 1:0;
		Packet recPacket = null;
		int recLength = 0;
		receive.startTimer();
		while (recPacket == null) {
			recLength = in().read(b);
			recPacket = new Packet(b,0,recLength);
			receive.count();
			receive.printDBG("recived Packet seq(" + recPacket.getSeq() + "), supposed seq(" + seq + "), valid(" + recPacket.isValid() + ")");
			if (recPacket.isValid()) {
				final Packet sendPacket = new Packet(recPacket.getSeq(), Packet.ACK);
				out().write(sendPacket.getHeader());
				out().flush();
				receive.printDBG("send ackPacket seq(" + recPacket.getSeq() + ")");
				if (recPacket.getSeq()!= seq) {
					receive.count();
					receive.addDrop();
					recPacket = null;
				}
			} else {
				receive.addDrop();
				recPacket = null;
			}
			
		}	
		receive.addSizeEndTimer(recLength - Packet.HEADER_SIZE);
		receiveSeq = !receiveSeq;
		return recLength;
	}
	
	@Override
	public OutputStream getOutputStream() {
		return outputStream;
	}

	@Override
	public InputStream getInputStream() {
		return inputStream;
	}
}
