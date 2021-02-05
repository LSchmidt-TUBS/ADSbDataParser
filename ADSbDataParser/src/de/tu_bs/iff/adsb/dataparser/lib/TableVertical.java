package de.tu_bs.iff.adsb.dataparser.lib;

/**
 * Class containing one trajectory-table from Vertical-trajectory (class TrajectoryVertical) to store vertical trajectory data channels
 */
public class TableVertical {
	// channels: 
	public double time[];
	public double baroAlt[];
	public double lastContact[];
	public boolean vaSample[];		// indicates whether the sample is valid (used for filtration of samples)
	public boolean vrSample[];		// value redundant (used for redundancy filtration)
	
	public TableVertical() {
	}

	public void allocateArrayMemory(int numberOfSamples) {
		time = new double[numberOfSamples];
		baroAlt = new double[numberOfSamples];
		lastContact = new double[numberOfSamples];
		vaSample = new boolean[numberOfSamples];
		vrSample = new boolean[numberOfSamples];
	}

	public void freeArrayMemory() {
		time = null;
		baroAlt = null;
		lastContact = null;
		vaSample = null;
		vrSample = null;
		
		System.gc();
	}

}
