package de.tu_bs.iff.adsb.dataparser.lib;

/**
 * Class containing one trajectory-table from Horizontal-trajectory (class TrajectoryHorizontal) to store horizontal trajectory data channels
 */
public class TableHorizontal {
	// channels: 
	public double time[];
	public double lat[];
	public double lon[];
	public double lastPosUpdate[];
	public boolean vaSample[];		// indicates whether the sample is valid (used for filtration of samples)
	public boolean vrSample[];		// value redundant (used for redundancy filtration)
	
	public TableHorizontal() {
	}

	public void allocateArrayMemory(int numberOfSamples) {
		time = new double[numberOfSamples];
		lat = new double[numberOfSamples];
		lon = new double[numberOfSamples];
		lastPosUpdate = new double[numberOfSamples];
		vaSample = new boolean[numberOfSamples];
		vrSample = new boolean[numberOfSamples];
	}

	public void freeArrayMemory() {
		time = null;
		lat = null;
		lon = null;
		lastPosUpdate = null;
		vaSample = null;
		vrSample = null;
		
		System.gc();
	}

}
