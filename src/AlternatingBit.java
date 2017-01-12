import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class AlternatingBit extends MultiSocket{

//	private static final int RETRANS_TIMEOUT = 200; 
//	
//	private static final int CONNECT_TIMEOUT = 1000;
//	
//	private static final int CONNECT_TRYS = 120;
//	
//	private static final int SEND_TRYS = 120; 
//	
//	private int currentTimeout = RETRANS_TIMEOUT;
//	
//	private boolean sendSeq = false;
//	
//	private boolean receiveSeq = false;
	
	private final AlternatingBitFSM fsm;
	
	private final byte[] buff = new byte[Packet.MAX_PACKET_SIZE];
	
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
		//this(new BadChannel(new UdpSocket(host, port)));
		this(new UdpSocket(host, port));
	}
	
	public AlternatingBit(MultiSocket socket) {
		super("AlternatingBit", socket);
		fsm = new AlternatingBitFSM(socket);
	}
	
//	private void continueTimer(long start) throws IOException {
//		long ctime = System.currentTimeMillis();
//		long durration = ctime - start;
//		long remain = currentTimeout - durration;
//		if (remain > 0) {
//			setReadTimeout((int)remain);
//		} else {
//			throw new SocketTimeoutException();
//		}		
//	}
//	
//	private void updateTimer(long rtt) {
//		if (rtt < 0) {
//			currentTimeout += currentTimeout/2;
//		} else {
//			currentTimeout = (int)((rtt+1)*1.1);
//		}
//	}
//	
//	private boolean isValidACK(Packet rec, Packet send) {
//		return rec.getSeq() == send.getSeq() && rec.checkFlags(Packet.ACK);
//	}
//	
//	private boolean isOldDAT(Packet rec) {
//		final int notRecSeq = !receiveSeq ? 1:0;
//		return rec.checkFlags(Packet.DAT) && notRecSeq == rec.getSeq();
//	}
//	
//	private void sndPkt(Packet packet) throws IOException {
//		out().write(packet.getHeader());
//		ByteBuffer data = packet.getData();
//		if (data != null) {
//			int off = data.arrayOffset();
//			byte[] buffer = data.array();
//			out().write(buffer, off + data.position(), data.remaining());
//		}
//		out().flush();
//	}
//	
//	private void sendPacket(int flags) throws IOException {
//		sendPacket(flags, null, 0, 0);
//	}
//	
//	private void sendPacket(int flags, byte[] data, int off, int len) throws IOException {
//		int seq = sendSeq ? 1:0;
//		Packet sendPacket = new Packet(seq, flags, data, off, len);
//		Packet recPacket = null;
//		byte[] recBuff = new byte[Packet.MAX_PACKET_SIZE];
//		int lastTimeout = getReadTimeout();
//		for(int i = 0; i < SEND_TRYS && recPacket == null; i++) {
//			sndPkt(sendPacket);
//			send.count();
//			send.printDBG("send Packet seq(" + sendPacket.getSeq() + ")");
//			try {
//				long start = System.currentTimeMillis(); // start timeout timer
//				do {
//					continueTimer(start);
//					recPacket = rcvPkt(recBuff);
//					send.printDBG("recived ackPacket: valid(" + recPacket.isValid() + ")");
//					send.printDBG("seq(" + recPacket.getSeq() + "), supposed seq(" + sendPacket.getSeq() + ")");
//					send.printDBG("checkflags(" + recPacket.checkFlags(Packet.ACK) + "), supposed flags(" + Integer.toString(Packet.ACK, 2) + ")");
//					if (recPacket.isValid()) {
//						if (!isValidACK(recPacket, sendPacket)) {							
//							if (isOldDAT(recPacket)) {
//								sndPkt(new Packet(!receiveSeq ? 1:0, Packet.ACK));
//							}
//							recPacket = null;
//						}
//					} else {
//						recPacket = null;
//					}
//				} while (recPacket == null);
//				updateTimer(System.currentTimeMillis() - start);
//			} catch (SocketTimeoutException e) {
//				updateTimer(-1);
//				send.printDBG("timeout");
//				send.addDrop();
//				recPacket = null;
//				send.printDBG("Packet droped");
//			}
//		}
//		sendSeq = !sendSeq;
//		setReadTimeout(lastTimeout);
//		if (recPacket == null) {
//			throw new SocketException("destination no longer reachable");
//		}
//	}
//	
//	private Packet rcvPkt(byte[] buff) throws IOException {
//		if (buff.length < Packet.MAX_PACKET_SIZE) {
//			throw new IllegalArgumentException("buff length: " + buff.length + " < " + Packet.MAX_PACKET_SIZE);
//		}
//		int bytesRead = 0;
//		bytesRead = in().read(buff);
//		if (bytesRead < 0) {
//			return null;
//		}
//		return new Packet(buff, 0, bytesRead);
//	}
//	
//	private int receivePkt(int flags, byte[] buff) throws IOException {
//		final int seq = receiveSeq ? 1:0;
//		Packet recPacket = null;
//		while (recPacket == null) {
//			recPacket = rcvPkt(buff);
//			if (recPacket == null) {
//				throw new SocketException("unexpected EOF");
//			}
//			receive.count();
//			receive.printDBG("recived Packet: valid(" + recPacket.isValid() + ")");
//			receive.printDBG("seq(" + recPacket.getSeq() + "), supposed seq(" + seq + ")");
//			receive.printDBG("checkflags(" + recPacket.checkFlags(flags) + "), supposed flags(" + Integer.toString(flags, 2) + ")");			
//			if (recPacket.isValid()) {
//				final Packet sendPacket = new Packet(recPacket.getSeq(), Packet.ACK);
//				sndPkt(sendPacket);
//				receive.printDBG("send ackPacket seq(" + recPacket.getSeq() + ")");
//				if (!(recPacket.getSeq()== seq && recPacket.checkFlags(flags))) {
//					receive.addDrop();
//					recPacket = null;
//					receive.printDBG("Packet droped");
//				}
//			} else {
//				receive.addDrop();
//				recPacket = null;
//				receive.printDBG("Packet droped");
//			}			
//		}
//		receiveSeq = !receiveSeq;
//		return recPacket.getLength();
//	}
//	
//	private void connectClient() throws IOException {
//		byte[] recBuff = new byte[Packet.MAX_PACKET_SIZE];
//		Packet sendPacket = new Packet(0, Packet.SYN);
//		int lastTimeout = getReadTimeout();
//		setReadTimeout(CONNECT_TIMEOUT); // Alle paar Sekunden versuchen den Server zu erreichen
//		for(int i = 0; i < CONNECT_TRYS; i++) {
//			sndPkt(sendPacket);
//			send.printDBG("send SYN Packet");
//			try {
//				send.printDBG("wait for SYN|ACK Packet");
//				int recLength = receivePkt(Packet.SYN|Packet.ACK, recBuff);
//				if (recLength < 0) {
//					throw new ConnectException("connection refused");
//				} else {
//					send.printDBG("received SYN|ACK Packet");
//					setReadTimeout(lastTimeout);
//					return;
//				}
//			} catch (SocketTimeoutException e) {
//				send.printDBG("timeout");
//				// try again
//			}	
//		}					
//		throw new ConnectException("connection timed out");
//	}
//	
//	private void connectHost() throws IOException {
//		byte[] recBuff = new byte[Packet.MAX_PACKET_SIZE];
//		Packet recPacket = null;
//		do {
//			send.printDBG("wait for SYN Packet");
//			recPacket = rcvPkt(recBuff);
//			if (recPacket == null) {
//				throw new ConnectException("unexpected EOF");
//			}
//		} while (!(recPacket.isValid() && recPacket.checkFlags(Packet.SYN) && recPacket.getSeq() == 0));
//		send.printDBG("received SYN Packet");
//		sendPacket(Packet.SYN|Packet.ACK); // Verbindungspacket empfangen und bestaetigen 
//		send.printDBG("send SYN|ACK Packet");
//	}
	
	@Override
	public void connect() throws Exception {
		super.connect();
		send.printDBG("connecting isHost(" + isHost() + ")");
		if (isHost()) {
			//connectHost();
			fsm.conHost(buff);
		} else {
			//connectClient();
			fsm.conClient(buff);
		}
	}
	
	private void send(byte[] b, int off, int len) throws IOException {
		send.startTimer();
		send.printDBG("sending data Packet: length(" + (len + Packet.HEADER_SIZE) + ")");
		//sendPacket(Packet.DAT, b, off, len);
		ByteBuffer dat = ByteBuffer.wrap(b, off, len);
		fsm.send(dat, buff);
		send.printDBG("send data Packet");
		send.addSizeEndTimer(len);
	}
	
	
	private int receive(byte[] b) throws IOException {
		receive.startTimer();
		receive.printDBG("receiving data Packet");
		//int recLength = receivePkt(Packet.DAT, b);
		ByteBuffer dat = fsm.receive(b);
		int datLength = dat.remaining();
		int buffLength = datLength + Packet.HEADER_SIZE;
		receive.printDBG("received data Packet: length(" + buffLength + ")");
		receive.addSizeEndTimer(datLength);
		return buffLength;
		//receive.printDBG("received data Packet: length(" + recLength + ")");
		//receive.addSizeEndTimer(recLength - Packet.HEADER_SIZE);
		//return recLength;
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
