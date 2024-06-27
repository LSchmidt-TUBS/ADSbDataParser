package de.tu_bs.iff.adsb.dataparser.parallel;

import de.tu_bs.iff.adsb.dataparser.lib.*;

/**
 * Class for use as library for parallel parsing of multiple trajectories from files.
 */
public class ParallelParser {
	
	private int nextTrajectoryIndexToParse = 0;
	private int joinedFinishedThreads = 0;
	
	private boolean stopParsing = false;
	
	ParserThread[] parserThreads = null;
	
	public AirportDatabase airportDatabase = null;
	
	public boolean filterRedundantSamples;
	
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
		airportDatabase = AirportDatabase.readInAirportDatabase(airportDatabaseDir);
	}
	
	/**
	 * Sets the airport database used for completeness-metric determination.
	 * @param airportDatabase Object of airport database.
	 */
	public void setAirportDatabase(AirportDatabase airportDatabase) {
		this.airportDatabase = airportDatabase;
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
	public void parseAll(int threadCount, boolean filterRedundantSamples) {
		this.filterRedundantSamples = filterRedundantSamples;
		
		stopParsing = false;
		joinedFinishedThreads = 0;
		
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
				joinedFinishedThreads++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
	
	public int getNextTrajectoryIndexToParse() {
		if(stopParsing)
			return -1;
		if(nextTrajectoryIndexToParse < dirs.length) {
			nextTrajectoryIndexToParse++;
			return nextTrajectoryIndexToParse-1;
		} else
			return -1;
	}
	
	/**
	 * Returns the number of already parsed Trajectories
	 * @return Number of already parsed Trajectories
	 */
	public int getNumberOfParsedTrajectories() {
		if(parserThreads == null)
			return 0;
		int estimatedNumberOfParsedTrajectories = nextTrajectoryIndexToParse - parserThreads.length + joinedFinishedThreads;
		if(estimatedNumberOfParsedTrajectories >= 0)
			return estimatedNumberOfParsedTrajectories;
		else
			return 0;
	}
	
	/**
	 * Returns status of trajectory parsing (started with function parseAll())
	 * @return True if parsing is finished or never started, else false
	 */
	public boolean isFinished() {
		if(parserThreads == null)
			return true;
		if(joinedFinishedThreads == parserThreads.length)
			return true;
		else
			return false;
	}
	
	/**
	 * Requests the parallel parser to stop/cancel parsing at the next possible time. 
	 * A resumption is not possible. 
	 */
	public void stopParsing() {
		stopParsing = true;
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
