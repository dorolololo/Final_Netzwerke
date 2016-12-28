
/** 
 * Klasse zum sammeln und auswerten von Messdaten bei Datenuebertragungen.
 * @author D.Doering, dorina.doering@yahoo.de
 * @author D.Goller, goller@hm.edu
 * @version 29.11.2016
 */
public class MeasureTool {

	private static final long INIT_START = System.currentTimeMillis();
	
	private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("debug", "false")); // java -Ddebug=true <class> ...
	
	/** Packetzaehler */
	private long count = 0;
	
	private int dropCount;
	
	/** Begin der Uebertragung in ms. */
	private long start = 0;
	
	// TODO problem mit der zeit lösen
	/** Ende der Uebertragung in ms. */
	private long totalTime = 0;
	
	/** Die maximale Anzahl der Pakete. */
	private long totalSize = 0;
	
	/** Groesse eines Paketes in Byte. */
	private long currentSize;
	
	private long lastTime;

	private long lastSize;

	// TODO synchronized
	private final Runnable outputUpdate = new Runnable() {		
		String output = "";
		@Override
		public void run() {
			try {
				while (!DEBUG) {
					StringBuilder result = new StringBuilder();
					result.append(currentRatePercent());
					result.append(" speed: " + currentRate());
					result.append(" remain: " + remainSize());
					result.append(" " + remainTime());
					if (!output.equals(result)) {
						clearOutput();
						output = result.toString();
						System.out.print(output);
						System.out.flush();
					}
					lastSize = 0;
					Thread.sleep(5000);
				}
			} catch (InterruptedException e) { 
				// Exit
			}
			clearOutput();
		}
		private void clearOutput() {
			StringBuilder clr = new StringBuilder();
			for (int i = 0; i < output.length(); i++) {
				clr.append("\b \b");
			}
			System.out.print(clr);
			System.out.flush();
			output = "";
		}
	};
	
	private Thread updateThread = new Thread(outputUpdate);
	
	private final String className;
	
	
	/**
	 * Erzeugt ein neues MeasureTool Objekt.
	 * @param pSize Groesse eines Packetes in Byte.
	 */
	public MeasureTool(String className) {
		this.className = className;
		updateThread.setDaemon(true);
	}
	
		
	public void startUpdate() {
		if (outputUpdate!= null && !updateThread.isAlive()) {
			updateThread.start();
		}	
	}
	
	public void endUpdate() throws InterruptedException {
		if (updateThread!= null && updateThread.isAlive()) {
			updateThread.interrupt();
			updateThread.join();
			updateThread = null;
		}	
	}
	
	public void printDBG(String msg) {
		if (DEBUG) {
			long delay = System.currentTimeMillis() - INIT_START;
			System.out.println("[" + delay + " ms] " + className + ": " +  msg);
		}
	}
	
	/**
	 * Setzt den Begin der Messung.
	 */
	public void startTimer() {
		start = System.nanoTime();
	}
	
	/**
	 * Zaehlt das Paket.
	 */
	public void count() {
		count++;
	}
	
	
	/**
	 * Zaehlt das verlorene Paket.
	 */
	public void addDrop() {
		dropCount++;
	}
	
	
	public void addSizeEndTimer(int size) {
		long cTime = System.nanoTime();
		long duration = cTime - start;
		if ((lastTime/1e6d) < 5000) {
			lastTime += duration;
			lastSize += size;
		} else {
			lastTime = duration;
			lastSize = size;
		}
		totalTime += duration;		
		this.currentSize += size;
	}
	
	/**
	 * Legt an Hand von Paketen
	 * die Maximalanzahl der zu uebertragenden Pakete fest.
	 * @param packet Ein Paket.
	 */
	public void setTotalSize(long fileSize) {
		totalSize = fileSize;
	}
	
	private String currentRatePercent() {
		double rate = totalSize > 0 ?(((double)(currentSize)/totalSize) * 100) : 0;
		String str = (int)rate + "%";
		StringBuilder result = new StringBuilder();
		result.append("[");
		int off = 10 - str.length()/2;
		for (int i = 0; i < (int)rate/5; i++) {				
			result.append(off > i || off + str.length()-1 < i ? '=' : str.charAt(i - off));
		}
		for (int i = (int)rate/5; i < 20; i++) {
			result.append(off > i || off + str.length()-1 < i ? ' ' : str.charAt(i - off));
		}
		result.append("]");
		return result.toString();
	}	
	
	private String duration() {
		return time(totalTime);
	}
	
	private String remainTime() {
		if (lastTime == 0 || lastSize == 0) {return "N/A";}
		long remain = totalSize - currentSize; // Byte
		double rate = (double)lastSize / lastTime; // Byte/ns
		double time = remain / rate; // ns
		return time(Math.round(time));
	}
	
	public static String time(long time) { // time ns
		long newTime = Math.round(time/1e9d); // sec
		String[] timeUnit = new String[] {
				"s", "min ", "h "		
		};
		int[] timeCalc = new int[] {
				60, 60, 24
		};
		String result = "";
		for (int i = 0; i < timeUnit.length && Math.abs(newTime) > 0; i++) {
			final long t = newTime % timeCalc[i];
			result = t + timeUnit[i] + result;
			newTime -= t;
			newTime /= timeCalc[i];
		}
		if (Math.abs(newTime) > 0) {
			result = newTime + "d " + result;
		}
		return result.isEmpty() ? "0s" : result;
	}
	
	private String avrRate() {
		return rate(currentSize, totalTime);
	}
	
	private String currentRate() {
		return rate(lastSize, lastTime);
	}
	
	
	public static String rate(long size, long time) { // size byte / time ns
		if (time == 0) {return "N/A";}
		String[] rateUnit = new String[] {
				"bit/s", "Kbit/s", "Mbit/s", "Gbit/s"			
		};
		double rate = size * Byte.SIZE * 1000 / (time/1e6d); // bit/s
		rate = Math.round(rate * 10) / 10d; 
		for (int i = 0; i < rateUnit.length; i++) {
			if (Math.abs(rate)  < 1000) {
				return rate + rateUnit[i]; // ???.? ?bit/s
			}
			rate = Math.round(rate / 100) / 10d;
		}
		return rate + "Tbit/s";
	}
	
	
	public static String fileSize(long size) {
		double remain = size; // Byte
		String[] sizeUnit = new String[] {
				"Byte", "KB", "MB", "GB"			
		};
		for (int i = 0; i < sizeUnit.length; i++) {
			if (Math.abs(remain) < 1000) {
				return remain + sizeUnit[i];
			}
			remain = Math.round(remain / 100) / 10d;
		}
		return remain + "TB";
	}
	
	private String remainSize() {
		return fileSize(totalSize - currentSize);
	}
	
	
	private String packetsStats() {
		return (count - dropCount) + "/" + count;
	}
	
	
	/**
	 * Liefert die Anzahl der verlorenen Pakete in Prozent.
	 * @return verlorenen Pakete in Prozent.
	 */
	private String lostPerc() {
		double value = count > 0 ? dropCount * 100d / count : 0;
		return (Math.round(value * 10) / 10d) + "%";
	}

	@Override
	public String toString() {
		return className + ": [avr speed: " + avrRate()
				+ " time: " + duration()
				+ " packets: " + packetsStats()
				+ " lost: " + lostPerc() + "]";
	}
	
	
}
