import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SelectiveRepeat extends MultiSocket{

	public SelectiveRepeat(int port) throws SocketException {
		//this(new BadChannel(new UdpSocket(port)));
		this(new UdpSocket(port));
	}
	
	public SelectiveRepeat(String host, int port) throws SocketException, UnknownHostException {
		this(new UdpSocket(host, port));
	}
	
	public SelectiveRepeat(MultiSocket socket) {
		super("SelectiveRepeat", socket);
	}

	@Override
	public void connect() throws Exception {
		// TODO Auto-generated method stub
		super.connect();
	}

	@Override
	public OutputStream getOutputStream() {
		// TODO Auto-generated method stub
		return super.getOutputStream();
	}

	@Override
	public InputStream getInputStream() {
		// TODO Auto-generated method stub
		return super.getInputStream();
	}

	
}
