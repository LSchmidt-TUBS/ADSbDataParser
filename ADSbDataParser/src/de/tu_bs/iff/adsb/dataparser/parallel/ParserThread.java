package de.tu_bs.iff.adsb.dataparser.parallel;

import de.tu_bs.iff.adsb.dataparser.lib.*;

public class ParserThread extends Thread {
	
	ParallelParser parent;
	
	private TrajectoryStateVectorsData4 trajectoryStateVectorsData4;
	private TrajectoryVertical trajectoryVertical;
	private TrajectoryHorizontal trajectoryHorizontal;
	private TrajectoryMerged trajectoryMerged;
	private String dir;
	
	private AirportDatabase airportDatabase = null;
	
	public void setParent(ParallelParser parent) {
		this.parent = parent;

		if(parent.airportDatabaseDir != null) {
			airportDatabase = new AirportDatabase();
			int readinError = airportDatabase.readInAirports(parent.airportDatabaseDir);
			if(readinError < 0)
				airportDatabase = null;
			if(readinError > 0)
				System.out.println(String.format("Airport database read in (%d faulty entries)", readinError));
		}
	}
	
	@Override public void run() {
		int trajectoryIndex;
		
		while((trajectoryIndex = parent.getNextTrajectoryIndexToParse()) >= 0) {
			trajectoryStateVectorsData4 = parent.trajectoryStateVectorsData4Array[trajectoryIndex];
			trajectoryVertical = parent.trajectoryVerticalArray[trajectoryIndex];
			trajectoryHorizontal = parent.trajectoryHorizontalArray[trajectoryIndex];
			trajectoryMerged = parent.trajectoryMergedArray[trajectoryIndex];
			dir = parent.dirs[trajectoryIndex];
			
			parent.errorCodes[trajectoryIndex] = parseTrajectory();
			if(parent.errorCodes[trajectoryIndex] >= 0) {
				parent.reliabilityMetrics[trajectoryIndex] = trajectoryMerged.getReliabilityMetric();
				parent.completenessMetrics[trajectoryIndex] = trajectoryMerged.getCompletenessMetric(airportDatabase);
				parent.plausibilityMetrics[trajectoryIndex] = trajectoryMerged.getPlausibilityMetric();
			}
		}
	}
	
	private int parseTrajectory() {
		int errorCode;
		
		errorCode = trajectoryStateVectorsData4.readInInterfaceDataFile(dir);
		if(errorCode < 0)
			return errorCode;
		
		errorCode = trajectoryVertical.setRawTrajectoryFromStateVectorsData4(trajectoryStateVectorsData4);
		if(errorCode < 0)
			return errorCode;
		errorCode = trajectoryVertical.parseTrajectory();
		if(errorCode < 0)
			return errorCode;
		
		errorCode = trajectoryHorizontal.setRawTrajectoryFromStateVectorsData4(trajectoryStateVectorsData4);
		if(errorCode < 0)
			return errorCode;
		errorCode = trajectoryHorizontal.parseTrajectory();
		if(errorCode < 0)
			return errorCode;
		
		trajectoryMerged.setTrajectories(trajectoryVertical, trajectoryHorizontal);
		errorCode = trajectoryMerged.mergeTrajectories();
		if(errorCode < 0)
			return errorCode;
		
		return errorCode;
	}

}
