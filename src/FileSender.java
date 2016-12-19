import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class FileSender {

	/** Encode Format fuer Dateinamen. */
	private static final String ENCODE = "UTF-8";
	
	private static final int PORT = 1337;
	
	private static final MeasureTool mTool = new MeasureTool("FileSender");
	
	/**
	 * Wird mit Dateinamen und Namen des Zielrechners als Parameter aufgerufen 
	 * und versendet die angegebene Datei zuverlässig an den angegebenen Zielrechner.
	 * @param Zielrechner Dateinamen
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("usage: java FileSender <hostname> <filename>");
			return;
		}
		File file = new File(args[1]);
		try(MultiSocket socket = new AlternatingBit(args[0], PORT);			
				FileInputStream inputStream = new FileInputStream(file)) {
			socket.connect();
			System.out.println("connected @ " + args[0] + ":" + PORT);
			final long fileSize = file.length();
			final String fileName = file.getName();
			socket.send(fileName.getBytes(ENCODE));
			byte[] size = new byte[Long.BYTES];
			for (int i = 0; i < size.length; ++i) {
				size[i] = (byte) (fileSize >> (size.length - i - 1 << 3));
			}
			socket.send(size);
			System.out.println("accepted file: " + fileName + " (" + fileSize + " Bytes)");
			//TODO datei nicht angenommen
			mTool.totalSize(fileSize);
			byte[] buffer = new byte[1024];
			int bytesRead;
			while (true) {
				mTool.start();
				if ((bytesRead = inputStream.read(buffer)) < 0) {break;}
				mTool.printDBG("read and send data (" +  bytesRead + " Bytes)");
				//TODO send mit off length
				socket.send( bytesRead < buffer.length ? Arrays.copyOf(buffer, bytesRead) : buffer);
				mTool.saveTime();
				mTool.addPSize(bytesRead);
				mTool.updateOutput();
			}
			mTool.clearOutput();
			mTool.reset();
			System.out.println("file sended:");
			System.out.println("avr rate: " + "? Mbit/s | time: " + "? min");
			mTool.printDBG("finished");
		}
		

	}

}
