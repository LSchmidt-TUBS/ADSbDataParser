package de.tu_bs.iff.adsb.dataparser.lib;

/**
 * Class containing one trajectory-table from Merged-trajectory (class TrajectoryMerged) to store merged trajectory data channels
 */
public class TableMerged {
	// channels: 
	public double[] time;
	public double[] baroAlt;
	public double[] lat;
	public double[] lon;
	
	public TableMerged() {
	}

	public void allocateArrayMemory(int numberOfSamples) {
		time = new double[numberOfSamples];
		baroAlt = new double[numberOfSamples];
		lat = new double[numberOfSamples];
		lon = new double[numberOfSamples];
	}

	public void freeArrayMemory() {
		time = null;
		baroAlt = null;
		lat = null;
		lon = null;
		
		System.gc();
	}

}
