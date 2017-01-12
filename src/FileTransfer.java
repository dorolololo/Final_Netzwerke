import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map.Entry;

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
	
	private void sendMeta(File file) throws IOException {
		DataOutputStream dataOutputStream = new DataOutputStream(out());
		dataOutputStream.writeUTF(file.getName());
		dataOutputStream.writeLong(file.length());
		dataOutputStream.flush();
	}
	
	private Entry<String, Long> receiveMeta() throws IOException {
		return new Entry<String, Long>() {
			DataInputStream dataInputStream = new DataInputStream(in());
			final String fileName = dataInputStream.readUTF();				
			final long fileSize = dataInputStream.readLong();
			
			@Override
			public Long setValue(Long value) {
				//nothing
				return null;
			}
			
			@Override
			public Long getValue() {
				return fileSize;
			}
			
			@Override
			public String getKey() {
				return fileName;
			}
		};
	}
	
	private boolean getAccept() throws IOException {
		DataInputStream dataInputStream = new DataInputStream(in());
		if (!dataInputStream.readBoolean()) {
			send.printDBG("file not accepted");
			return false; // datei nicht angenommen
		} else {
			send.printDBG("file accepted");
			return true;
		}
	}
	
	private boolean setAccept(boolean accept) throws IOException {
		DataOutputStream dataOutputStream = new DataOutputStream(out());
		if (!accept) {
			dataOutputStream.writeBoolean(false); // datei nicht angenommen	
			dataOutputStream.flush();
			send.printDBG("file not accepted");
			return false;
		} else {
			dataOutputStream.writeBoolean(true);
			dataOutputStream.flush();
			send.printDBG("file accepted");
			return true;
		}
	}
	
	private boolean send(InputStream fileInput, long size, boolean coutUpdate) throws Exception {
		long remainLength = size;
		if (coutUpdate) {
			send.startUpdate(size);
		}
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead;
		while (remainLength > 0) {
			send.startTimer();
			if ((bytesRead = fileInput.read(buffer)) < 0) {break;}
			send.printDBG("read and send data (" +  bytesRead + " Bytes)");
			remainLength -= bytesRead;
			out().write(buffer, 0, bytesRead);
			send.count();
			send.addSizeEndTimer(bytesRead);
		}
		out().flush();
		if (coutUpdate) {
			send.endUpdate();
		}
		return remainLength == 0;
	}
	
	private boolean receive(OutputStream fileOutput, long size, boolean coutUpdate) throws Exception {
		long remainLength = size;
		if (coutUpdate) {
			receive.startUpdate(size);
		}
		byte[] buffer = new byte[BUFFER_SIZE];		
		while (remainLength > 0) {
			receive.startTimer();
			int bytesRead;
			if ((bytesRead = in().read(buffer)) < 0) {break;}
			receive.printDBG("receive and write data (" +  bytesRead + " Bytes)");
			remainLength -= bytesRead;
			fileOutput.write(buffer, 0, bytesRead);
			receive.count();
			receive.addSizeEndTimer(bytesRead);
		}
		fileOutput.flush();
		if (coutUpdate) {
			receive.endUpdate();
		}
		return remainLength == 0;
	}
	
	public Entry<String, Long> getRequest() throws IOException {
		return receiveMeta();
	}
	
	public boolean send(File file, boolean coutUpdate) throws Exception {			
		if (file == null) {
			throw new NullPointerException("file is null");
		}
		sendMeta(file);
		if (getAccept()) {
			try(FileInputStream inputStream = new FileInputStream(file)) {
				return send(inputStream, file.length(), coutUpdate);
			}
		} else {
			return false;
		}
	}

	public boolean receive(File file, long size, boolean coutUpdate) throws Exception{
		if (setAccept(file != null)) {
			try(FileOutputStream outputStream = new FileOutputStream(file)) {
				return receive(outputStream, size, coutUpdate);
			}
		} else {
			return false;
		}
	}
	
}
