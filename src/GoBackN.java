import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;

public class GoBackN extends MultiSocket{

	
	
	public GoBackN(int port) throws SocketException {
		//this(new BadChannel(new UdpSocket(port)));
		this(new UdpSocket(port));
	}
	
	public GoBackN(String host, int port) throws SocketException, UnknownHostException {
		this(new UdpSocket(host, port));
	}
	
	public GoBackN(MultiSocket socket) {
		super("GoBackN", socket);
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
