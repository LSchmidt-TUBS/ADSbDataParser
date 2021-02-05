package de.tu_bs.iff.adsb.dataparser.lib;

/**
 * Class containing one trajectory-table for StateVectorsData4-trajectory (class: TrajectoryStateVectorsData4) to store trajectory data channels
 */
public class TableStateVectorsData4 {
	// channels (specific values are valid, if va[Value] is true): 
	public int[] time = null;
	public double[] lat = null;
	public double[] lon = null;
	public double[] velocity = null;
	public double[] heading = null;
	public double[] vertRate = null;
	public boolean[] onGround = null;
	public boolean[] alert = null;
	public boolean[] spi = null;
	public int[] squawk = null;
	public double[] baroAlt = null;
	public double[] geoAltitude = null;
	public double[] lastPosUpdate = null;
	public double[] lastContact = null;
	
	// va[Value] indicates, if at the specific sample (Array) a value is available/valid: 
	public boolean[] vaLat = null;
	public boolean[] vaLon = null;
	public boolean[] vaVelocity = null;
	public boolean[] vaHeading = null;
	public boolean[] vaVertRate = null;
	public boolean[] vaOnGround = null;
	public boolean[] vaAlert = null;
	public boolean[] vaSpi = null;
	public boolean[] vaSquawk = null;
	public boolean[] vaBaroAlt = null;
	public boolean[] vaGeoAltitude = null;
	public boolean[] vaLastPosUpdate = null;
	public boolean[] vaLastContact = null;
	
	public TableStateVectorsData4() {
	}
	
	public void allocateArrayMemory(int numberOfSamples) {
		time = new int[numberOfSamples];
		lat = new double[numberOfSamples];
		lon = new double[numberOfSamples];
		velocity = new double[numberOfSamples];
		heading = new double[numberOfSamples];
		vertRate = new double[numberOfSamples];
		onGround = new boolean[numberOfSamples];
		alert = new boolean[numberOfSamples];
		spi = new boolean[numberOfSamples];
		squawk = new int[numberOfSamples];
		baroAlt = new double[numberOfSamples];
		geoAltitude = new double[numberOfSamples];
		lastPosUpdate = new double[numberOfSamples];
		lastContact = new double[numberOfSamples];
		
		vaLat = new boolean[numberOfSamples];
		vaLon = new boolean[numberOfSamples];
		vaVelocity = new boolean[numberOfSamples];
		vaHeading = new boolean[numberOfSamples];
		vaVertRate = new boolean[numberOfSamples];
		vaOnGround = new boolean[numberOfSamples];
		vaAlert = new boolean[numberOfSamples];
		vaSpi = new boolean[numberOfSamples];
		vaSquawk = new boolean[numberOfSamples];
		vaBaroAlt = new boolean[numberOfSamples];
		vaGeoAltitude = new boolean[numberOfSamples];
		vaLastPosUpdate = new boolean[numberOfSamples];
		vaLastContact = new boolean[numberOfSamples];
	}

	public void freeArrayMemory() {
		time = null;
		lat = null;
		lon = null;
		velocity = null;
		heading = null;
		vertRate = null;
		onGround = null;
		alert = null;
		spi = null;
		squawk = null;
		baroAlt = null;
		geoAltitude = null;
		lastPosUpdate = null;
		lastContact = null;
		
		vaLat = null;
		vaLon = null;
		vaVelocity = null;
		vaHeading = null;
		vaVertRate = null;
		vaOnGround = null;
		vaAlert = null;
		vaSpi = null;
		vaSquawk = null;
		vaBaroAlt = null;
		vaGeoAltitude = null;
		vaLastPosUpdate = null;
		vaLastContact = null;
		
		System.gc();
	}
	
	/**
	 * Copies a line from source (with sourceIndex) to this (with index). 
	 * @param source Source table
	 * @param sourceIndex Line index of the line to copy from source
	 * @param index Line index of this object to copy line from source-table to
	 */
	public void copyTableLine(TableStateVectorsData4 source, int sourceIndex, int index) {
		this.time[index] = source.time[sourceIndex];
		this.lat[index] = source.lat[sourceIndex];
		this.lon[index] = source.lon[sourceIndex];
		this.velocity[index] = source.velocity[sourceIndex];
		this.heading[index] = source.heading[sourceIndex];
		this.vertRate[index] = source.vertRate[sourceIndex];
		this.onGround[index] = source.onGround[sourceIndex];
		this.alert[index] = source.alert[sourceIndex];
		this.spi[index] = source.spi[sourceIndex];
		this.squawk[index] = source.squawk[sourceIndex];
		this.baroAlt[index] = source.baroAlt[sourceIndex];
		this.geoAltitude[index] = source.geoAltitude[sourceIndex];
		this.lastPosUpdate[index] = source.lastPosUpdate[sourceIndex];
		this.lastContact[index] = source.lastContact[sourceIndex];

		this.vaLat[index] = source.vaLat[sourceIndex];
		this.vaLon[index] = source.vaLon[sourceIndex];
		this.vaVelocity[index] = source.vaVelocity[sourceIndex];
		this.vaHeading[index] = source.vaHeading[sourceIndex];
		this.vaVertRate[index] = source.vaVertRate[sourceIndex];
		this.vaOnGround[index] = source.vaOnGround[sourceIndex];
		this.vaAlert[index] = source.vaAlert[sourceIndex];
		this.vaSpi[index] = source.vaSpi[sourceIndex];
		this.vaSquawk[index] = source.vaSquawk[sourceIndex];
		this.vaBaroAlt[index] = source.vaBaroAlt[sourceIndex];
		this.vaGeoAltitude[index] = source.vaGeoAltitude[sourceIndex];
		this.vaLastPosUpdate[index] = source.vaLastPosUpdate[sourceIndex];
		this.vaLastContact[index] = source.vaLastContact[sourceIndex];
	}
	
}
