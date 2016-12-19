
/** 
 * Klasse zum sammeln und auswerten von Messdaten bei Datenuebertragungen.
 * @author D.Doering, dorina.doering@yahoo.de
 * @author D.Goller, goller@hm.edu
 * @version 29.11.2016
 */
public class MeasureTool {

	private static final long INIT_START = System.currentTimeMillis();
	
	private static final boolean DEBUG = false;
	
	/** Packetzaehler */
	private long count = 0;
	
	private int dropCount;
	
	/** Begin der Uebertragung in ms. */
	private long start = 0;
	
	/** Ende der Uebertragung in ms. */
	private long totalTime = 0;
	
	/** Die maximale Anzahl der Pakete. */
	private long totalSize = 0;
	
	/** Groesse eines Paketes in Byte. */
	private long currentSize;
	
	private long lastTime;
	
	private long lastSize;
	
	private long lastOutput;
	
	
	private final String className;
	
	private String output = "";
	
	/**
	 * Erzeugt ein neues MeasureTool Objekt.
	 * @param pSize Groesse eines Packetes in Byte.
	 */
	public MeasureTool(String className) {
		this.className = className;
	}
	
	public void updateOutput() {
		if (!DEBUG && System.currentTimeMillis() - lastOutput > 1000 ) {
			StringBuilder result = new StringBuilder();
			int percent = (int)currentRatePercent();
			result.append(percent + "% [");
			int off = i/2 - percent.length()/2;
			for (int i = 0; i < percent/5; i++) {				
				result.append(off < i || off + percent.length() < i ? percent. : '=');
			}
			for (int i = percent/5; i < 20; i++) {
				result.append(' ');
			}
			result.append("] ");
			result.append(currentRate() + " kbit/s ");
			result.append(remainTime() + " ms ");
			result.append(remainSize() + " Bytes");			
			clearOutput();
			output = result.toString();
			System.out.print(output);
			System.out.flush();
			lastOutput = System.currentTimeMillis();
		}
	}
	
	public void clearOutput() {
		StringBuilder clr = new StringBuilder();
		for (int i = 0; i < output.length(); i++) {
			clr.append("\b \b");
		}
		System.out.print(clr);
		System.out.flush();
		output = "";
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
	public void start() {
		start = System.currentTimeMillis();
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
	public void dropCount() {
		dropCount++;
	}
	
	public void addPSize(long size) {
		lastSize = size;
		this.currentSize += size;
	}
	
	/**
	 * Legt an Hand von Paketen
	 * die Maximalanzahl der zu uebertragenden Pakete fest.
	 * @param packet Ein Paket.
	 */
	public void totalSize(long fileSize) {
		totalSize = fileSize;
	}
	
	public double currentRatePercent() {
		return totalSize > 0 ?(((double)(currentSize)/totalSize) * 100) : 0;
	}
	
	public double currentRate() {
		return lastTime > 0 ?(lastSize * Byte.SIZE) / (double)lastTime : 0;
	}
	
	public long remainSize() {
		return totalSize - currentSize;
	}
	
	public int remainTime() {
		return (int) (remainSize() / currentRate());
	}
	
	/**
	 * Sichert den momentanen Zeitraum der Messung.
	 */
	public void saveTime() {
		long cTime = System.currentTimeMillis();
		lastTime = cTime - start;
		totalTime += lastTime;
	}
	
	/**
	 * Liefert die Anzahl der gezaehlten Pakete.
	 * @return Anzahl gezaehlter Pakete.
	 */
	public long getCount() {
		return count;
	}
	
	/**
	 * Liefert die Anzahl der maximal zu uebertragenden Pakete.
	 * @return Anzahl maximal zu uebertragenden Pakete.
	 */
	public long getTotal() {
		return totalSize;
	}
	
	/**
	 * Liefert die Anzahl der verlorenen Pakete in Prozent.
	 * @return verlorenen Pakete in Prozent.
	 */
	public double getLost() {
		return count > 0 ?(count - dropCount) / (count / 100.0) : 0;
	}
	
	
	/**
	 * Liefert die Uebertragungsrate in kbit/s.
	 * @return Die Uebertragungsrate.
	 */
	public double getTransmissionRate() {
		return getDuration() > 0 ? currentSize * Byte.SIZE / getDuration() : 0;
	}
	
	/**
	 * Liefert die Dauer der Messung in ms.
	 * @return Die Dauer in ms.
	 */
	public long getDuration() {
		return totalTime;
	}

	@Override
	public String toString() {
		return "S: " + getTransmissionRate() + " kbit/s " 
				+ "D: " + getDuration() + " ms "
				+ "P: " + getCount() + "/" + getTotal()
				+ " lost: " + getLost() + "%";
	}

	public void reset() {
		count = 0;
		start = 0;
		totalTime = 0;
		totalSize = 0;
		currentSize = 0;
		
	}
	
	
}
