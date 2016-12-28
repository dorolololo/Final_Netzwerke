import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.function.BiFunction;

public class FileTransfer extends MultiSocket{
	
	public static final int PORT = 1337;
	
	private static final int BUFFER_SIZE = 65536;
	
	
	public FileTransfer() throws Exception {
		this(new AlternatingBit(PORT));
	}
	
	public FileTransfer(String host) throws Exception {
		this(new AlternatingBit(host, PORT));
	}
	
	public FileTransfer(MultiSocket socket) throws Exception {
		super("FileTransfer", socket);
	}
	
	public boolean send(File file, boolean outputUpdate) throws Exception {			
		if (file == null) {
			throw new NullPointerException("file is null");
		}
		try(FileInputStream inputStream = new FileInputStream(file)) {
			DataOutputStream dataOutputStream = new DataOutputStream(out());
			final long fileSize = file.length();
			dataOutputStream.writeUTF(file.getName());
			dataOutputStream.writeLong(fileSize);
			dataOutputStream.flush();
			DataInputStream dataInputStream = new DataInputStream(in());
			if (!dataInputStream.readBoolean()) {
				send.printDBG("file not accepted");
				return false; // datei nicht angenommen
			} else {
				send.printDBG("file accepted");
			}
			send.setTotalSize(fileSize);
			if (outputUpdate) {
				send.startUpdate();
			}
			byte[] buffer = new byte[BUFFER_SIZE]; // TODO swap buffer?
			int bytesRead;
			while (true) {
				send.startTimer();
				if ((bytesRead = inputStream.read(buffer)) < 0) {break;}
				send.printDBG("read and send data (" +  bytesRead + " Bytes)");			
				out().write(buffer, 0, bytesRead);
				send.count();
				send.addSizeEndTimer(bytesRead);
			}
			out().flush();
			if (outputUpdate) {
				send.endUpdate();
			}
			send.printDBG("finished");
		}
		return true;
	}

	// TODO etwas besseres als BiFunction ?
	public boolean receive(BiFunction<String, Long, File> chooseFile, boolean outputUpdate) throws Exception{
		DataInputStream dataInputStream = new DataInputStream(in());
		final String fileName = dataInputStream.readUTF();				
		final long fileSize = dataInputStream.readLong();
		File file = chooseFile.apply(fileName, fileSize);
		DataOutputStream dataOutputStream = new DataOutputStream(out());
		if (file == null) {
			dataOutputStream.writeBoolean(false); // datei nicht angenommen	
			dataOutputStream.flush();
			send.printDBG("file not accepted");
			return false;
		} else {
			dataOutputStream.writeBoolean(true);
			dataOutputStream.flush();
			send.printDBG("file accepted");
		}
		long remainLength = fileSize;
		try(FileOutputStream outputStream = new FileOutputStream(file)) {									
			receive.setTotalSize(fileSize);
			if (outputUpdate) {
				receive.startUpdate();
			}
			byte[] buffer = new byte[BUFFER_SIZE]; // TODO swap buffer?			
			while (remainLength > 0) {
				receive.startTimer();
				int bytesRead;
				if ((bytesRead = in().read(buffer)) < 0) {break;}
				receive.printDBG("receive and write data (" +  bytesRead + " Bytes)");
				remainLength -= bytesRead;
				outputStream.write(buffer, 0, bytesRead);
				receive.count();
				receive.addSizeEndTimer(bytesRead);
			}
			outputStream.flush();
		}
		if (outputUpdate) {
			receive.endUpdate();
		}
		if (remainLength > 0) {
			file.delete(); // der sender ist zu früh ausgestiegun une
			return false;
		}
		receive.printDBG("finished");
		return true;
	}
	
}
