import java.io.File;
import java.net.InetAddress;

public class FileSender {

	
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
		if (!file.canRead()) {
			System.out.println("the file specified by this pathname do not exist or can not be read:");
			System.out.println(args[1]);
			return;
		}
		System.out.println("local Adress is: " + InetAddress.getLocalHost().getHostAddress());
		try (FileTransfer transfer = new FileTransfer(args[0])) {
			transfer.connect();
			System.out.println("connected @ " + transfer.destAdress() + ":" + FileTransfer.PORT);
			System.out.println("sending file: " + file.getName() + " (" + MeasureTool.fileSize(file.length()) + ")");
			final boolean success = transfer.send(file,true);			
			if (success) {
				System.out.println("file sended:");
				System.out.println(transfer);
			} else {
				System.out.println("receiver refused file");
			}
		}
	}
}
