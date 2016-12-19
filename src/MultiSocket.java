import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;

/** 
 * Interface welches Sockets mit unterschiedlichen Protokolle darstellt.
 * @author D.Doering, dorina.doering@yahoo.de
 * @author D.Goller, goller@hm.edu
 * @version 29.11.2016
 */
public interface MultiSocket extends java.io.Closeable{

	//TODO rückgabe von verbindungs ziel und ggf. local
	/**
	 * Baut eine Verbindung auf.
	 * @throws IOException wenn beim Verbindugsaufbau ein Fehler auftritt.
	 */
	void connect() throws Exception;
	
	//TODO OutputStream?
	//TODO send(null) 
	/**
	 * Sendet ein Byte Array.
	 * @param data Das zu sendente Byte Array.
	 * @throws IOException wenn beim senden ein Fehler auftritt.
	 */
	void send(byte[] data) throws IOException;
	
	default void send(byte data) throws IOException {
		byte[] buff = new byte[1];
		buff[0] = data;
		send(buff);
	}
	
	default void send(byte[] data, int length) throws IOException {
		send(data, 0, length);
	}
	
	default void send(byte[] data, int off, int length) throws IOException {
		send(Arrays.copyOfRange(data, off, length));
	}
	
	
	/**
	 * Empfangt ein Byte Array.
	 * @return Falls die Verbindung beendet wurde null, sonst as zu empfangende Byte Array.
	 * @throws IOException wenn beim empfangen ein Fehler auftritt.
	 */
	byte[] receive() throws IOException;
	
	//TODO InputStream?
	default int receive(byte[] data) throws IOException {
		return receive(data, data.length);
	}
	
	default int receive(byte[] data, int length) throws IOException {
		return receive(data, 0, length);
	}
	
	default int receive(byte[] data, int off, int length) throws IOException {
		byte[] buff = receive();
		if (buff == null) {
			return -1;
		}
		// TODO Problem: data zu klein (datenverlust) braucht bufferung
		System.arraycopy(buff, 0, data, off, length);
		return buff.length;
	}
	
	
	
	/**
	 * Setzt einen Timeout auf blockiernde Aufrufe von read().
	 * @param ms Timeout in ms.
	 * @throws SocketException wenn einn Fehler auftritt.
	 */
	void setReadTimeout(int ms) throws SocketException;
	
}
