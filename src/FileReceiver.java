import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class FileReceiver {

	/** Encode Format fuer Dateinamen. */
	private static final String ENCODE = "UTF-8";
	
	private static final int PORT = 1337;
	
	private static final MeasureTool mTool = new MeasureTool("FileReceiver");
	
	/**
	 * Wartet auf eingehende Dateiübertragungen. Empfangene Dateien werden lokal gespeichert.
	 * Nach dem Empfang einer Datei bleibt das Programm weiter aktiv 
	 * und wartet auf die nächste eingehende Datenverbindung.
	 * @param args keine.
	 */
	public static void main(String[] args) throws Exception {
		while (true) {
			try(MultiSocket socket = new AlternatingBit(PORT)) {
				socket.connect();
				System.out.println("connected @ " + "?" + ":" + PORT);
				final String fileName = new String(socket.receive(), ENCODE);
				File file = new File(fileName);
				try(FileOutputStream outputStream = new FileOutputStream(file)) {
					byte[] size = socket.receive();					
					long fileSize = 0;
					for (int i = 0; i < Long.BYTES; i++) {
						fileSize = (fileSize << Byte.SIZE) + (size[i] & 0xFF);
						}
					System.out.println("accepted file: " + fileName + " (" + fileSize + " Bytes)");
					//TODO datei nicht angenommen
					
					mTool.totalSize(fileSize);
					while (fileSize > 0) {
						mTool.start();
						//TODO receive mit off length?
						byte[] buffer = socket.receive();
						mTool.printDBG("receive and write data (" +  buffer.length + " Bytes)");
						//System.out.println("FileReceiver: data(" + Arrays.toString(buffer) + ")");
						fileSize -= buffer.length;
						outputStream.write(buffer);
						mTool.saveTime();
						mTool.addPSize(buffer.length);
						mTool.updateOutput();
					}		
				}
			}
			mTool.clearOutput();
			mTool.reset();
			System.out.println("file received:");
			System.out.println("avr rate: " + "? Mbit/s | time: " + "? min");
			mTool.printDBG("finished");
		}


	}

}
