import java.util.Arrays;

/** 
 * Klasse welche ein send und empfangbares Paket mit fester Groesse darstellt.
 * @author D.Doering, dorina.doering@yahoo.de
 * @author D.Goller, goller@hm.edu
 * @version 29.11.2016
 */
public class Packet {
	
	/** Die maximale Groesse eines Paketes in Byte. */
	public static final int MAX_PACKET_SIZE = 1400;
	
	/** Der Offset der Pruefsumme im Paket Buffer. */
	public static final int CHECKSUM_OFFSET = 0;
	
	/** Der Offset der Sequenznummer im Paket Buffer. */
	public static final int SEQ_OFFSET = Integer.BYTES;

	/** Die Groesse des Headers eines Paketes in Byte. */
	public static final int HEADER_SIZE = Integer.BYTES *2;
	
	/** Die Groesse an Daten die ein Pakete aufnehmen kann in Byte. */
	public static final int DATA_SIZE = MAX_PACKET_SIZE - HEADER_SIZE;
	
	/** Vortlaufender Zähler zur Paketverlust Erkennung. */
	private final int seq;
	
	/** Pruefsumme um Bitfehler zu erkennen. */
	private final int checksum;
	
	/** Der Byte Buffer dieses Paketes. */
	private final byte[] buffer;
	
	/** Der Byte Buffer der Daten von diesem Paket. */
	private final byte[] data;
	
	// TODO flags(con, ack...)
	
	/**
	 * Erzeugt ein neues Packet Objekt zum Versand.
	 * @param seq Sequenznummer von diesem Paket.
	 * @param data Byte Array der zu versendende daten.
	 */
	public Packet(int seq, byte[] data) {
		if (data.length > DATA_SIZE) {
			throw new IllegalArgumentException();
		}		
		buffer = new byte[HEADER_SIZE + data.length];
		setBuffInt(SEQ_OFFSET, seq);
		System.arraycopy(data, 0, buffer, HEADER_SIZE, data.length);
		int newChecksum = 0;
		for (int i = SEQ_OFFSET; i < buffer.length; i++) {
			newChecksum += buffer[i]; // Die Checksumme steht am Anfang des Buffers und wird nicht mitberechnet.
		}
		setBuffInt(CHECKSUM_OFFSET, newChecksum);
		this.data = data;
		this.seq = seq;
		this.checksum = newChecksum;
	}
	
	/**
	 * Erzeugt ein neues Packet Objekt zum Empfang.
	 * @param buffer Byte Array des empfangenen Paketes.
	 */
	public Packet(byte[] buffer) {
		this.buffer = buffer;
		this.data =  Arrays.copyOfRange(buffer, HEADER_SIZE, buffer.length);
		this.seq = getBuffInt(SEQ_OFFSET);
		int newChecksum = 0;
		for (int i = SEQ_OFFSET; i < buffer.length; i++) {
			newChecksum += buffer[i]; // Die Checksumme steht am Anfang des Buffers und wird nicht mitberechnet.
		}
		this.checksum = newChecksum;
	}
	
	public boolean isValid() {
		return checksum == getBuffInt(CHECKSUM_OFFSET);
	}
	
	/**
	 * Liefert die Sequenznummer von diesem Paket.
	 * @return Die Sequenznummer.
	 */
	public int getSeq() {
		return seq;
	}
	
	/**
	 * Setzt eine Nummer vom Typ Integer im Byte Buffer.
	 * @param offset Der Offset wo die Nummer im Buffer gesichert wird.
	 * @param numb Die Nummer.
	 */
	private void setBuffInt(int offset, int numb) {
		if (offset < 0 || buffer.length < offset) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < Integer.BYTES; ++i) {
			buffer[i + offset] = (byte) (numb >> (Integer.BYTES - i - 1 << 3));
		}
	}
	
	/**
	 * Liefert eine Nummer vom Typ Integer im diesem Byte Buffer.
	 * @param offset Der Offset wo die Nummer im Buffer extrahiert wird.
	 * @return Die Nummer als Integer.
	 */
	public int getBuffInt(int offset) {
		if (offset < 0 || buffer.length < offset) {
			throw new IllegalArgumentException();
		}
		int value = 0;
		for (int i = 0; i < Integer.BYTES; i++) {
		   value = (value << Byte.SIZE) + (buffer[i + offset] & 0xFF);
		}
		return value;
	}
	
	/**
	 * Liefert die Daten von diesem Paket.
	 * @return Die Daten.
	 */
	public byte[] getData() {
		return data;
	}
	
	/**
	 * Liefert den Buffer des Paketes.
	 * @return der Paketbuffer als Bayte Array.
	 */
	public byte[] getBuffer() {
		return buffer;
	}
	
}
