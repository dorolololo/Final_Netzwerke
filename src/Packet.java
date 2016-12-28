import java.nio.ByteBuffer;
import java.util.Arrays;

/** 
 * Klasse welche ein send und empfangbares Paket mit fester Groesse darstellt.
 * @author D.Doering, dorina.doering@yahoo.de
 * @author D.Goller, goller@hm.edu
 * @version 29.11.2016
 */
public class Packet {
	
	/* Flags */
	public static final byte DAT = 0;
	public static final byte FIN = 1;
	public static final byte SYN = 2;
	public static final byte ACK = 4;
	public static final byte WND = 8;
	public static final byte PSH = 16;
	public static final byte PRB = 32;
	
	/** Die maximale Groesse eines Paketes in Byte. */
	public static final int MAX_PACKET_SIZE = 1400;
	
	/** Der Offset der Pruefsumme im Paket Buffer. */
	private static final int CHECKSUM_OFFSET = 0;
	
	private static final int LENGTH_OFFSET = CHECKSUM_OFFSET + Integer.BYTES;
	
	/** Der Offset der Sequenznummer im Paket Buffer. */
	private static final int SEQ_OFFSET = LENGTH_OFFSET + Integer.BYTES;

	private static final int FLAGS_OFFSET = SEQ_OFFSET + Integer.BYTES;
	
	/** Die Groesse des Headers eines Paketes in Byte. */
	public static final int HEADER_SIZE = FLAGS_OFFSET + Byte.BYTES;
	
	/** Die Groesse an Daten die ein Pakete aufnehmen kann in Byte. */
	public static final int DATA_SIZE = MAX_PACKET_SIZE - HEADER_SIZE;
	
	private final byte flags;
	
	/** Vortlaufender Zähler zur Paketverlust Erkennung. */
	private final int seq;
	
	/** Pruefsumme um Bitfehler zu erkennen. */
	private final int checksum;
	
//	/** Der Byte Buffer dieses Paketes. */
//	private ByteBuffer buffer;
	
	/** Der Byte Buffer der Daten von diesem Paket. */
	private final ByteBuffer data;
	
	private final ByteBuffer header;
	
	private final boolean valid;
	
	//private final byte[] packet;
	
	private byte[] packetBuffer;
	
	private byte[] headerBuffer;
	
	private byte[] dataBuffer;
	
	/**
	 * Erzeugt ein neues Packet Objekt zum Versand.
	 * @param seq Sequenznummer von diesem Paket.
	 * @param data Byte Array der zu versendende daten.
	 */
	public Packet(int seq, byte flags, byte[] data) {
		this(seq, flags, data, 0, (data == null ? 0 : data.length));
	}
	
	public Packet(int seq, byte flags) {
		this(seq, flags, null);
	}
	
	public Packet(int seq, byte flags, byte[] data, int off, int len) {
		if (data != null) { 
			if (off < 0 || len < 0 || len > data.length - off) {
				throw new IndexOutOfBoundsException("data length: " + data.length + " off: " + off + " len: " + len);
			}
			if (len > DATA_SIZE) {
				throw new IllegalArgumentException("data length > " + DATA_SIZE + " : " + len);
			}
		}
		header = ByteBuffer.wrap(new byte[HEADER_SIZE]);
		header.putInt(CHECKSUM_OFFSET, 0);
		header.putInt(LENGTH_OFFSET,len);
		header.putInt(SEQ_OFFSET,seq);
		header.put(FLAGS_OFFSET,flags);
		final int newChecksum = calcChecksum(header.array()) + (data == null ? 0 : calcChecksum(data, off, len));
		header.putInt(CHECKSUM_OFFSET, newChecksum);
		this.data = data == null ? null : ByteBuffer.wrap(data, off, len);
		this.seq = seq;
		this.flags = flags;
		this.checksum = newChecksum;
		this.valid = true;
	}
	
	public Packet(byte[] buffer, int off, int len) {
		if (buffer == null) {
			throw new NullPointerException("buffer is null");
		}
		if (off < 0 || len < 0 || len > buffer.length - off) {
            throw new IndexOutOfBoundsException("buffer length: " + buffer.length + "off: " + off + "len: " + len);
        }
		if (len > MAX_PACKET_SIZE || len < HEADER_SIZE) {
			throw new IllegalArgumentException("invalid buffer length: " + len);
		}
		//this.buffer = ByteBuffer.wrap(buffer,off,len);
		this.data = ByteBuffer.wrap(buffer,off + HEADER_SIZE,len - HEADER_SIZE);
		this.header = ByteBuffer.wrap(buffer,off,HEADER_SIZE);
		this.seq = header.getInt(SEQ_OFFSET);
		this.flags = header.get(FLAGS_OFFSET);
		this.checksum = calcChecksum(buffer, off + Integer.BYTES, len - Integer.BYTES); // Die Checksumme steht am Anfang des Buffers und wird nicht mitberechnet.
		this.valid = (len - HEADER_SIZE) == header.getInt(LENGTH_OFFSET) && checksum == header.getInt(CHECKSUM_OFFSET);
	}
	
	/**
	 * Erzeugt ein neues Packet Objekt zum Empfang.
	 * @param buffer Byte Array des empfangenen Paketes.
	 */
	public Packet(byte[] buffer) {
		this(buffer, 0, buffer.length);
	}
	
	private static int calcChecksum(byte[] data) {
		return calcChecksum(data, 0, data.length);
	}
	
	private static int calcChecksum(byte[] data, int off, int len) {
		if (data == null) {
			throw new NullPointerException("data is null");
		}
		if (off < 0 || len < 0 || len > data.length - off) {
            throw new IndexOutOfBoundsException("data length: " + data.length + " off: " + off + " len: " + len);
        }
		int value = 0;
		for (int i = off; i < len + off; i++) {
			value += data[i];
		}
		return value;
	}
	
	public boolean isValid() {
		return valid;
	}
	
	public boolean checkFlags(byte flags) {
		return (this.flags & flags) > 0;
	}
	
	/**
	 * Liefert die Sequenznummer von diesem Paket.
	 * @return Die Sequenznummer.
	 */
	public int getSeq() {
		return seq;
	}
	
	
	/**
	 * Liefert die Daten von diesem Paket.
	 * @return Die Daten.
	 */
	public byte[] getData() {
		if (dataBuffer == null) {
			final int arrayOffset = data.arrayOffset();
			final int off = arrayOffset + data.position();
			final int len = arrayOffset + data.limit();
			dataBuffer = Arrays.copyOfRange(data.array(), off, len);
		}
		return dataBuffer;
	}
	
	/**
	 * Liefert den Buffer des Paketes.
	 * @return der Paketbuffer als Bayte Array.
	 */
	public byte[] getPacket() {
		if (packetBuffer == null) {
			packetBuffer = Arrays.copyOf(getHeader(), HEADER_SIZE + getHeader().length);
			System.arraycopy(getData(), 0, packetBuffer, HEADER_SIZE, getData().length);
		}
		return packetBuffer;
	}

	
	public byte[] getHeader() {
		if (headerBuffer == null) {
			final int arrayOffset = header.arrayOffset();
			final int off = arrayOffset + header.position();
			final int len = arrayOffset + header.limit();
			headerBuffer = Arrays.copyOfRange(header.array(), off, len);
		}
		return headerBuffer;
	}
	
}
