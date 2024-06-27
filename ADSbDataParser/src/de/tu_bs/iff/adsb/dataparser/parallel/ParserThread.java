package de.tu_bs.iff.adsb.dataparser.parallel;

import de.tu_bs.iff.adsb.dataparser.lib.*;

public class ParserThread extends Thread {
	
	ParallelParser parent;
	
	private boolean filterRedundantSamples;
	
	private TrajectoryStateVectorsData4 trajectoryStateVectorsData4;
	private TrajectoryVertical trajectoryVertical;
	private TrajectoryHorizontal trajectoryHorizontal;
	private TrajectoryMerged trajectoryMerged;
	private String dir;
	
	public void setParent(ParallelParser parent) {
		this.parent = parent;
		this.filterRedundantSamples = parent.filterRedundantSamples;
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
				parent.completenessMetrics[trajectoryIndex] = trajectoryMerged.getCompletenessMetric(parent.airportDatabase);
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
		errorCode = trajectoryVertical.parseTrajectory(filterRedundantSamples);
		if(errorCode < 0)
			return errorCode;
		
		errorCode = trajectoryHorizontal.setRawTrajectoryFromStateVectorsData4(trajectoryStateVectorsData4);
		if(errorCode < 0)
			return errorCode;
		errorCode = trajectoryHorizontal.parseTrajectory(filterRedundantSamples);
		if(errorCode < 0)
			return errorCode;
		
		trajectoryMerged.setTrajectories(trajectoryVertical, trajectoryHorizontal);
		errorCode = trajectoryMerged.mergeTrajectories();
		if(errorCode < 0)
			return errorCode;
		
		return errorCode;
	}

}
