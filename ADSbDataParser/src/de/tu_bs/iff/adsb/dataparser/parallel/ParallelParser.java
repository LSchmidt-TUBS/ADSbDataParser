package de.tu_bs.iff.adsb.dataparser.parallel;

import de.tu_bs.iff.adsb.dataparser.lib.*;

/**
 * Class for use as library for parallel parsing of multiple trajectories from files.
 */
public class ParallelParser {
	
	private int nextTrajectoryIndexToParse = 0;
	
	ParserThread[] parserThreads;
	
	public String airportDatabaseDir = null;
	
	public TrajectoryStateVectorsData4[] trajectoryStateVectorsData4Array;
	public TrajectoryVertical[] trajectoryVerticalArray;
	public TrajectoryHorizontal[] trajectoryHorizontalArray;
	public TrajectoryMerged[] trajectoryMergedArray;
	public double[] reliabilityMetrics;
	public double[] completenessMetrics;
	public double[] plausibilityMetrics;
	public String[] dirs;
	public int[] errorCodes;
	
	public ParallelParser() {
	}
	
	/**
	 * Sets the directory to the airport database used for completeness-metric determination.
	 * @param airportDatabaseDir Directory to the airport database.
	 */
	public void setAirportDatabaseDir(String airportDatabaseDir) {
		this.airportDatabaseDir = airportDatabaseDir;
	}
	
	/**
	 * Sets the directories to log-files of aircraft trajectories to be parsed.
	 * @param dirs Directories to log-files of aircraft trajectories to be parsed.
	 */
	public void setDirs(String[] dirs) {
		this.dirs = dirs;
		trajectoryStateVectorsData4Array = new TrajectoryStateVectorsData4[dirs.length];
		trajectoryVerticalArray = new TrajectoryVertical[dirs.length];
		trajectoryHorizontalArray = new TrajectoryHorizontal[dirs.length];
		trajectoryMergedArray = new TrajectoryMerged[dirs.length];
		reliabilityMetrics = new double[dirs.length];
		completenessMetrics = new double[dirs.length];
		plausibilityMetrics = new double[dirs.length];
		errorCodes = new int[dirs.length];
		
		for(int i=0; i<dirs.length; i++) {
			trajectoryStateVectorsData4Array[i] = new TrajectoryStateVectorsData4();
			trajectoryVerticalArray[i] = new TrajectoryVertical();
			trajectoryHorizontalArray[i] = new TrajectoryHorizontal();
			trajectoryMergedArray[i] = new TrajectoryMerged();
		}
	}
	
	/**
	 * Parse all Trajectories handed over by function setDirs(String[] dirs) in parallel mode.
	 * @param threadCount Number of Threads to be used for parallel parsing.
	 */
	public void parseAll(int threadCount) {
		if(airportDatabaseDir == null) {
			System.out.println("No Airport-Database directory set. Completenss-metric will be -1 for all trajectories. ");
		}
		
		nextTrajectoryIndexToParse = 0;
		parserThreads = new ParserThread[threadCount];
		
		for(int i=0; i<parserThreads.length; i++) {
			parserThreads[i] = new ParserThread();
			parserThreads[i].setParent(this);
			parserThreads[i].start();
		}
		
		for(int i=0; i<parserThreads.length; i++)
			try {
				parserThreads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
	
	public int getNextTrajectoryIndexToParse() {
		if(nextTrajectoryIndexToParse < dirs.length) {
			nextTrajectoryIndexToParse++;
			return nextTrajectoryIndexToParse-1;
		} else
			return -1;
	}

	/**
	 * Returns the error code of requested trajectory parsing process.
	 * @param index Index of trajectory to return error code.
	 * @return Error code of trajectory parsing process.
	 */
	public int getErrorCode(int index) {
		return errorCodes[index];
	}

	/**
	 * Returns the TrajectoryVertical object of the requested trajectory.
	 * @param index Index of trajectory to return TrajectoryVertical.
	 * @return TrajectoryVertical of parsed trajectory.
	 */
	public TrajectoryVertical getTrajectoryVertical(int index) {
		return trajectoryVerticalArray[index];
	}

	/**
	 * Returns the TrajectoryHorizontal object of the requested trajectory.
	 * @param index Index of trajectory to return TrajectoryHorizontal.
	 * @return TrajectoryHorizontal of parsed trajectory.
	 */
	public TrajectoryHorizontal getTrajectoryHorizontal(int index) {
		return trajectoryHorizontalArray[index];
	}

	/**
	 * Returns the TrajectoryMerged object of the requested trajectory.
	 * @param index Index of trajectory to return TrajectoryMerged.
	 * @return TrajectoryMerged of parsed trajectory.
	 */
	public TrajectoryMerged getTrajectoryMerged(int index) {
		return trajectoryMergedArray[index];
	}
	
	/**
	 * Returns the reliability metric of requested trajectory.
	 * @param index Index of trajectory to return reliability metric.
	 * @return Reliability metric of parsed trajectory.
	 */
	public double getReliabilityMetric(int index) {
		return reliabilityMetrics[index];
	}

	/**
	 * Returns the completeness metric of requested trajectory.
	 * @param index Index of trajectory to return completeness metric.
	 * @return Completeness metric of parsed trajectory.
	 */
	public double getCompletenessMetric(int index) {
		return completenessMetrics[index];
	}

	/**
	 * Returns the plausibility metric of requested trajectory.
	 * @param index Index of trajectory to return plausibility metric.
	 * @return Plausibility metric of parsed trajectory.
	 */
	public double getPlausibilityMetric(int index) {
		return plausibilityMetrics[index];
	}
}
