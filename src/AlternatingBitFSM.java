import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

public class AlternatingBitFSM {

	private static final int RETRANS_TIMEOUT = 200; 
	
	private static final int CONNECT_TIMEOUT = 1000;
	
	private static final int CONNECT_TRYS = 50;
	
	private static final int SEND_TRYS = 20; 
	
	private int currentTimeout = RETRANS_TIMEOUT;
	
	private boolean sendSeq = false;
	
	private boolean receiveSeq = false;
	
	private final MultiSocket socket;
	
	private final MeasureTool tool = new MeasureTool("AlternatingBitFSM");
	
	public AlternatingBitFSM(MultiSocket socket) {
		this.socket = socket;
		initFSM();
	}
	
	
	// all states for this FSM
	enum State {
		IDLE, CONNECTED, ACK_WAIT, REC_WAIT, ERROR
	};
	// all messages/conditions which can occur
	enum Msg {
		CON_CLIENT, VALID_SYN,
		SEND, VALID_ACK, NOT_VALID_ACK, OLD_DAT, TIMEOUT, TRYS_OVER, EOF,
		RECEIVE, VALID_REC, NOT_VALID_REC, REQUESTED_REC
	}
	// current state of the FSM	
	private State currentState;
	// 2D array defining all transitions that can occur
	private Transition[][] transition;

	private void initFSM() {
		currentState = State.IDLE;
		// define all valid state transitions for our state machine
		// (undefined transitions will be ignored)
		transition = new Transition[State.values().length] [Msg.values().length];
		transition[State.IDLE.ordinal()] [Msg.CON_CLIENT.ordinal()] = new ClientStart();
		transition[State.IDLE.ordinal()] [Msg.VALID_SYN.ordinal()] = new HostStart();

		transition[State.CONNECTED.ordinal()] [Msg.SEND.ordinal()] = new SndDat();
		transition[State.CONNECTED.ordinal()] [Msg.RECEIVE.ordinal()] = new StartRcv();

		transition[State.ACK_WAIT.ordinal()] [Msg.VALID_ACK.ordinal()] = new ValidAck();
		transition[State.ACK_WAIT.ordinal()] [Msg.NOT_VALID_ACK.ordinal()] = new NotValidAck();
		transition[State.ACK_WAIT.ordinal()] [Msg.OLD_DAT.ordinal()] = new SndOldDatAck();
		transition[State.ACK_WAIT.ordinal()] [Msg.TIMEOUT.ordinal()] = new SndPkt();
		transition[State.ACK_WAIT.ordinal()] [Msg.TRYS_OVER.ordinal()] = new End();
		transition[State.ACK_WAIT.ordinal()] [Msg.EOF.ordinal()] = new End();

		transition[State.REC_WAIT.ordinal()] [Msg.VALID_REC.ordinal()] = new ValidRcv();
		transition[State.REC_WAIT.ordinal()] [Msg.NOT_VALID_REC.ordinal()] = new NotValidRcv();
		transition[State.REC_WAIT.ordinal()] [Msg.REQUESTED_REC.ordinal()] = new RcvDat();
		transition[State.REC_WAIT.ordinal()] [Msg.TIMEOUT.ordinal()] = new ConTimeout();
		transition[State.REC_WAIT.ordinal()] [Msg.TRYS_OVER.ordinal()] = new End();
		transition[State.REC_WAIT.ordinal()] [Msg.EOF.ordinal()] = new End();

		tool.printDBG("INFO FSM constructed, current state: "+currentState);
	}

	/**
	 * Process a message (a condition has occurred).
	 * @param input Message or condition that has occurred.
	 */
	public void processMsg(Msg input) throws IOException{
		tool.printDBG("INFO Received "+input+" in state "+currentState);
		Transition trans = transition[currentState.ordinal()][input.ordinal()];
		if(trans != null){
			currentState = trans.execute(input);
		}
		tool.printDBG("INFO State: "+currentState);
	}

	/**
	 * Abstract base class for all transitions.
	 * Derived classes need to override execute thereby defining the action
	 * to be performed whenever this transition occurs.
	 */
	abstract class Transition {
		abstract public State execute(Msg input) throws IOException;
	}

	private Packet pkt;
	private int trys;
	private long timerStart;
	private int lastTimeout;
	private ByteBuffer data;
	private int requestFlags;

	class HostStart extends Transition {
		@Override
		public State execute(Msg input) throws IOException {
			pkt = new Packet(getSendSeq(), Packet.SYN|Packet.ACK);
			sndPkt(pkt);
			incTrys();
			startTimer();
			return State.ACK_WAIT;
		}	
	}

	class ClientStart extends Transition {
		@Override
		public State execute(Msg input) throws IOException {
			pkt = new Packet(getSendSeq(), Packet.SYN);
			sndPkt(pkt);
			requestRcv(Packet.SYN|Packet.ACK);
			incTrys();
			startTimer(CONNECT_TIMEOUT);
			return State.REC_WAIT;
		}	
	}

	class SndDat extends Transition {
		@Override
		public State execute(Msg input) throws IOException {
			pkt = new Packet(getSendSeq(), Packet.DAT, data);
			sndPkt(pkt);
			incTrys();
			startTimer();
			return State.ACK_WAIT;
		}
	}

	class StartRcv extends Transition {
		@Override
		public State execute(Msg input) throws IOException {
			requestRcv(Packet.DAT);
			return State.REC_WAIT;
		}
	}

	class RcvDat extends Transition {
		@Override
		public State execute(Msg input) throws IOException {
			Packet ackPkt = new Packet(getReceiveSeq(), Packet.ACK);
			sndPkt(ackPkt);
			toggleRcvSeqBit();
			rstTrys();
			stopTimer();
			return State.CONNECTED;
		}
	}

	class ConTimeout extends Transition {
		@Override
		public State execute(Msg input) throws IOException {
			sndPkt(pkt);
			startTimer(CONNECT_TIMEOUT);
			incTrys();
			return State.REC_WAIT;
		}
	}

	class ValidRcv extends Transition {
		@Override
		public State execute(Msg input) throws IOException {
			Packet oldAckPkt = new Packet(getOldReceiveSeq(), Packet.ACK);
			sndPkt(oldAckPkt);
			return State.REC_WAIT;
		}
	}

	class NotValidRcv extends Transition {
		@Override
		public State execute(Msg input) throws IOException {
			return State.REC_WAIT;
		}
	}

	class SndPkt extends Transition {
		@Override
		public State execute(Msg input) throws IOException {	
			sndPkt(pkt);
			incTrys();
			updateTimer();
			startTimer();
			return State.ACK_WAIT;
		}
	}

	class SndOldDatAck extends Transition {
		@Override
		public State execute(Msg input) throws IOException {	
			Packet oldAckPkt = new Packet(getOldReceiveSeq(), Packet.ACK);
			sndPkt(oldAckPkt);
			continueTimer();
			return State.ACK_WAIT;
		}
	}

	class ValidAck extends Transition {
		@Override
		public State execute(Msg input) throws IOException {	
			toggleSndSeqBit();
			rstTrys();
			updateTimer();
			stopTimer();
			return State.CONNECTED;
		}
	}

	class NotValidAck extends Transition {
		@Override
		public State execute(Msg input) throws IOException {	
			continueTimer();
			return State.ACK_WAIT;
		}
	}

	class End extends Transition {
		@Override
		public State execute(Msg input) throws IOException {	
			return State.ERROR;
		}
	}

	private void sndPkt(Packet packet) throws IOException {
		OutputStream out = socket.getOutputStream();
		out.write(packet.getHeader());
		ByteBuffer data = packet.getData();
		if (data != null) {
			int off = data.arrayOffset();
			byte[] buffer = data.array();
			out.write(buffer, off + data.position(), data.remaining());
		}
		socket.getOutputStream().flush();
		tool.printDBG("send Packet: valid(" + packet.isValid() + ")");
		tool.printDBG("seq(" + packet.getSeq() + ")");
		tool.printDBG("flags(" + Integer.toString(packet.getFlags(),2) + ")");
		tool.printDBG("length(" + ((data == null ? 0 : data.remaining()) + Packet.HEADER_SIZE) + ")");
	}
	
	private Packet rcvPkt(byte[] buff) throws IOException {
		if (buff.length < Packet.MAX_PACKET_SIZE) {
			throw new IllegalArgumentException("buff length: " + buff.length + " < " + Packet.MAX_PACKET_SIZE);
		}
		int bytesRead = 0;
		bytesRead = socket.getInputStream().read(buff);
		if (bytesRead < 0) {
			return null;
		}
		Packet recPacket = new Packet(buff, 0, bytesRead);
		tool.printDBG("recived Packet: valid(" + recPacket.isValid() + ")");
		tool.printDBG("seq(" + recPacket.getSeq() + ")");
		tool.printDBG("flags(" + Integer.toString(recPacket.getFlags(),2) + ")");
		tool.printDBG("length(" + (recPacket.getData().remaining() + Packet.HEADER_SIZE) + ")");
		return recPacket;
	}
	
	private void startTimer() throws SocketException {
		startTimer(currentTimeout);
	}
	
	private void startTimer(int timeout) throws SocketException {
		if (lastTimeout < 0) {
			lastTimeout = socket.getReadTimeout();
		}
		timerStart = System.currentTimeMillis();
		socket.setReadTimeout(timeout);
	}

	private void stopTimer() throws SocketException {
		if (lastTimeout >= 0) {
			socket.setReadTimeout(lastTimeout);
			lastTimeout = -1;
		}
	}

	private void updateTimer() {
		long cTime = System.currentTimeMillis();
		long durration = cTime - timerStart;
		if (durration < currentTimeout) {
			updateTimer(durration);
		} else {
			updateTimer(-1);
		}
	}
	
	private void updateTimer(long rtt) {
		if (rtt < 0) {
			currentTimeout = (int) Math.round(currentTimeout*1.5);
		} else {
			currentTimeout = (int) Math.round((rtt+1)*1.1);
		}
	}

	private void continueTimer() throws IOException {
		continueTimer(timerStart);
	}
	
	private void continueTimer(long start) throws IOException {
		long ctime = System.currentTimeMillis();
		long durration = ctime - start;
		long remain = currentTimeout - durration;
		if (remain > 0) {
			socket.setReadTimeout((int)remain);
		} else {
			throw new SocketTimeoutException();
		}		
	}

	private void incTrys() {
		trys++;
	}

	private void rstTrys() {
		trys = 0;
	}

	private int getSendSeq() {
		return sendSeq? 1:0;
	}

	private void toggleSndSeqBit() {
		sendSeq = !sendSeq;
	}

	private int getReceiveSeq() {
		return receiveSeq? 1:0;
	}

	private void toggleRcvSeqBit() {
		receiveSeq = !receiveSeq;
	}

	private int getOldReceiveSeq() {
		return !receiveSeq? 1:0;
	}

	private boolean isVaidSYN(Packet packet) {
		return packet.isValid() && packet.checkFlags(Packet.SYN) && packet.getSeq() == 0;
	}

	private boolean isTrysOver(int maxTrys) {
		return trys >= maxTrys;
	}

	private boolean isOldRcvDat(Packet packet) {
		return isOldDAT(packet);
	}

	private boolean isOldDAT(Packet rec) {
		final int notRecSeq = !receiveSeq ? 1:0;
		return rec.checkFlags(Packet.DAT) && notRecSeq == rec.getSeq();
	}
	
	private boolean isValidAck(Packet packet) {
		return packet.isValid() && packet.getSeq() == getSendSeq();
	}

	private boolean timerStarted() {
		return lastTimeout > -1;
	}

	private boolean notRequestedRcv(Packet packet) {
		return packet.getSeq()!= getReceiveSeq() || !packet.checkFlags(requestFlags);
	}

	private boolean isRequestedRcv(Packet packet) {
		return packet.getSeq()== getReceiveSeq() && packet.checkFlags(requestFlags);
	}

	private void requestRcv(int flags) {
		requestFlags = flags;
	}

	public void conHost(byte[] buff) throws IOException {
		Packet rcv;
		do {
			rcv = rcvPkt(buff);
			if (rcv == null) {
				processMsg(Msg.EOF);
				throw new SocketException("unexpected EOF");
			}
		} while (!isVaidSYN(rcv));
		processMsg(Msg.VALID_SYN);
		validAckWait(buff, CONNECT_TRYS);
	}

	public void conClient(byte[] buff) throws IOException {
		processMsg(Msg.CON_CLIENT);
		requestedRcvWait(buff);
	}

	public void send(ByteBuffer dat, byte[] buff) throws IOException {
		data = dat;
		processMsg(Msg.SEND);
		validAckWait(buff, SEND_TRYS);
	}

	public ByteBuffer receive(byte[] buff) throws IOException {
		processMsg(Msg.RECEIVE);
		requestedRcvWait(buff);
		return data;
	}

	private void validAckWait(byte[] buff, int trys) throws IOException {
		while (true) {
			tool.printDBG("valid_ack_wait:");
			tool.printDBG("currentTimeout(" + currentTimeout + ")");
			tool.printDBG("sendSeq(" + sendSeq + ")");
			tool.printDBG("receiveSeq(" + receiveSeq + ")");
			tool.printDBG("trys(" + this.trys + ")");
			tool.printDBG("lastTimeout(" + lastTimeout + ")");
			tool.printDBG("requestFlags(" + Integer.toString(requestFlags,2) + ")");
			try {
				if (isTrysOver(trys)) {
					processMsg(Msg.TRYS_OVER);
					throw new SocketException("destination no longer reachable");
				}
				Packet rcv = rcvPkt(buff);
				if (rcv == null) {
					processMsg(Msg.EOF);
					throw new SocketException("unexpected EOF");
				} else if (isOldRcvDat(rcv)) {
					processMsg(Msg.OLD_DAT);
				} else if (isValidAck(rcv)) {
					processMsg(Msg.VALID_ACK);
					return;
				} else {
					processMsg(Msg.NOT_VALID_ACK);
				}
			} catch (SocketTimeoutException e) {
				processMsg(Msg.TIMEOUT);
			}			
		}
	}

	private void requestedRcvWait(byte[] buff) throws IOException {
		while (true) {
			tool.printDBG("requested_rcv_wait:");
			tool.printDBG("currentTimeout(" + currentTimeout + ")");
			tool.printDBG("sendSeq(" + sendSeq + ")");
			tool.printDBG("receiveSeq(" + receiveSeq + ")");
			tool.printDBG("trys(" + trys + ")");
			tool.printDBG("lastTimeout(" + lastTimeout + ")");
			tool.printDBG("requestFlags(" + Integer.toString(requestFlags,2) + ")");
			try {
				if (isTrysOver(CONNECT_TRYS)) {
					processMsg(Msg.TRYS_OVER);
					throw new SocketException("destination no longer reachable");
				}
				Packet rcv = rcvPkt(buff);
				if (rcv == null) {
					processMsg(Msg.EOF);
					throw new SocketException("unexpected EOF");
				} else if (notRequestedRcv(rcv)) {
					processMsg(Msg.VALID_REC);
				} else if (isRequestedRcv(rcv)) {
					processMsg(Msg.REQUESTED_REC);
					data = rcv.getData();
					return;
				} else {
					processMsg(Msg.NOT_VALID_REC);
				}
			} catch (SocketTimeoutException e) {
				if (timerStarted()) {
					processMsg(Msg.TIMEOUT);
				} else {
					throw e;
				}				
			}			
		}
	}
}
