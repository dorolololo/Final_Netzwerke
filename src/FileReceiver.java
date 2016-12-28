import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.function.BiFunction;

public class FileReceiver {
	
	//private static final int TIMEOUT = 60000;

	/**
	 * Wartet auf eingehende Dateiübertragungen. Empfangene Dateien werden lokal gespeichert.
	 * Nach dem Empfang einer Datei bleibt das Programm weiter aktiv 
	 * und wartet auf die nächste eingehende Datenverbindung.
	 * @param args keine.
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("local Adress is: " + InetAddress.getLocalHost().getHostAddress());
		while (true) {
			try (FileTransfer transfer = new FileTransfer()) {
				transfer.connect();
				System.out.println("connected @ " + transfer.destAdress() + ":" + FileTransfer.PORT);				
				final boolean success = transfer.receive(new BiFunction<String, Long, File>() {				
					@Override
					public File apply(String name, Long size) {
						BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
						String fileName = name;
						try {

							while (true) {
								System.out.println("file: " + fileName + " (" + MeasureTool.fileSize(size) + ")");
								System.out.println("accept?: yes(y), no(n), rename(r)");
								String an = in.readLine();							
								if ("y".equals(an)) {
									File file = new File(fileName);
									File parent = file.getAbsoluteFile().getParentFile();
									long space = parent.getFreeSpace();
									if (space < size) {
										System.out.println("not enough space available: (" + MeasureTool.fileSize(space - size) + ")");
									} else if (file.isFile()) { // TODO <- das richtige?
										while (true) {
											System.out.println("file already exist");
											System.out.println("override?: yes(y), no(n)");
											an = in.readLine();
											if ("y".equals(an)) {
												return file;
											} else if ("n".equals(an)) {
												break;
											}
										}
									} else {
										return file;
									}							
								} else if ("n".equals(an)) {
									return null;
								} else if ("r".equals(an)) {
									System.out.println("set name: (path)<filename>");
									an = in.readLine();
									fileName = an;
								}
							}
						} catch (IOException e) {
							return null;
						}
					}
				}, true);
				if (success) {
					System.out.println("file received:");
					System.out.println(transfer);	
				} else {
					System.out.println("file not received");
				}							
			}
		}
	}

}
