package de.tu_bs.iff.adsb.dataparser.lib;

import java.util.ArrayList;

import de.tu_bs.iff.adsb.dataparser.cosmath.CosMath;

/**
 * Class containing a vertical trajectory and functions for parsing the vertical path. 
 * Considered channels of TableVertical: baroAlt, lastContact
 * Only samples of channels of TableVertical, which hold valid values (vaValue[] == true) for all channels mentioned above will be considered. 
 */
public class TrajectoryVertical {
	public String callsign = null;
	public String icao24 = null;
	
	private ArrayList<int[]> cruiseIndices;
	private ArrayList<int[]> levelIndices;
	private ArrayList<int[]> climbIndices = new ArrayList<int[]>();
	private ArrayList<int[]> descentIndices = new ArrayList<int[]>();
	public ArrayList<VerticalFlightPhase> verticalFlightPhases = new ArrayList<VerticalFlightPhase>();

	public double[] reliability;
	public double[] reliabilityTime;
	
	public TableVertical table = new TableVertical();		// table for parsed trajectory
	
	public TableVertical tableRaw = new TableVertical();	// table for initial trajectory
	
	public TrajectoryVertical() {
	}
	
	public String getCallsign() {
		return callsign;
	}
	public String getIcao24() {
		return icao24;
	}
	private int getVaVrSize() {
		int size = 0;
		for(int i=0; i<table.vaSample.length; i++)
			if(table.vaSample[i] && !table.vrSample[i])
				size++;
		return size;
	}
	private int getVaSize() {
		int size = 0;
		for(int i=0; i<table.vaSample.length; i++)
			if(table.vaSample[i])
				size++;
		return size;
	}
	private double[] getChannelVaVrSamples(double[] channel) {
		double[] channelVaVr;
		channelVaVr = new double[getVaVrSize()];
		int index = 0;
		for(int i=0; i<channel.length; i++)
			if(table.vaSample[i] && !table.vrSample[i]) {
				channelVaVr[index] = channel[i];
				index++;
			}
		return channelVaVr;
	}
	private double[] getChannelVaSamples(double[] channel) {
		double[] channelVa;
		channelVa = new double[getVaSize()];
		int index = 0;
		for(int i=0; i<channel.length; i++)
			if(table.vaSample[i]) {
				channelVa[index] = channel[i];
				index++;
			}
		return channelVa;
	}
	public double[] getTime() {
		return getChannelVaVrSamples(table.time);
	}
	public double[] getBaroAlt() {
		return getChannelVaVrSamples(table.baroAlt);
	}
	
	public double[] getReliability() {
		return reliability;
	}
	public double[] getReliabilityTime() {
		return reliabilityTime;
	}

	public double[] getSamplingTime() {
		return getChannelVaSamples(table.time);
	}
	
	public double[] getTimeRaw() {
		return tableRaw.time;
	}
	public double[] getBaroAltRaw() {
		return tableRaw.baroAlt;
	}

	/**
	 * Sets TrajectoryVerticals raw-trajectory (tableRaw) from StateVectorsData4-trajectory
	 * @param sourceTrajectory Source trajectory
	 * @return ErrorCode; -1: error; 0: successful
	 */
	public int setRawTrajectoryFromStateVectorsData4(TrajectoryStateVectorsData4 sourceTrajectory) {
		if(sourceTrajectory.table.time == null)
			return -1;
		
		// obtain number of samples, which contain (va = true): velocity, vertRate, squawk, baroAlt, lastContact: 
		int numberOfApplicableSamples = 0;
		for(int i=0; i<sourceTrajectory.table.time.length; i++)
			if(sourceTrajectory.table.vaBaroAlt[i])
				if(sourceTrajectory.table.vaLastContact[i])
					numberOfApplicableSamples++;
		
		// allocate trajectory variables with the number of samples
		tableRaw.allocateArrayMemory(numberOfApplicableSamples);
		
		// set callsign and icao24
		callsign = new String(sourceTrajectory.callsign);
		icao24 = new String(sourceTrajectory.icao24);

		// get applicable Samples from sourceTrajectory
		int currentTableRawIndex = 0;
		for(int i=0; i<sourceTrajectory.table.time.length; i++)
			if(sourceTrajectory.table.vaBaroAlt[i])
				if(sourceTrajectory.table.vaLastContact[i]) {
					tableRaw.time[currentTableRawIndex] = (double)sourceTrajectory.table.time[i];		// (tableRaw.time is double[], whereas sourceTrajectory.table.time is int[])
					tableRaw.baroAlt[currentTableRawIndex] = sourceTrajectory.table.baroAlt[i];
					tableRaw.lastContact[currentTableRawIndex] = sourceTrajectory.table.lastContact[i];
					tableRaw.vaSample[currentTableRawIndex] = true;
					tableRaw.vrSample[currentTableRawIndex] = false;
					currentTableRawIndex++;
				}
		return 0;
	}
	
	/**
	 * Parses the raw vertical trajectory in tableRaw and stores the result in table. 
	 * Data of tableRaw will not be changed. 
	 * @return Error code; -1: error; 0: successful
	 */
	public int parseTrajectory() {
		if(tableRaw.time == null)
			return -1;
		
		convertBaroAltToFeet(tableRaw);
		
		table.allocateArrayMemory(tableRaw.time.length);
		// copy trajectory-data from tableRaw to table; only content of table will be changed during parsing afterwards: 
		for(int i=0; i<table.time.length; i++) {
			table.time[i] = tableRaw.time[i];
			table.baroAlt[i] = tableRaw.baroAlt[i];
			table.lastContact[i] = tableRaw.lastContact[i];
			table.vaSample[i] = tableRaw.vaSample[i];
			table.vrSample[i] = tableRaw.vrSample[i];
		}
		
		// Filter Samples with consecutive lastContact value
		filterRedundantSamples(table, table.lastContact);
		
		// Filter BaroAlt with moving median
		filterMovingMedianBaroAlt(table, /*halfWindowSize*/(int)5);
		
		// Identification of cruise segments
		cruiseIndices = determineCruiseIndices(table, /*altitudeDeviationMargin*/(double)76, /*minSegmentSampleCount*/(int)30);
		
		// Identification of level segments
		levelIndices = determineLevelIndices(table, /*altitudeDeviationMargin*/(double)51, /*minSegmentSampleCount*/(int)30);
		
		// Filter unrealistic baro-altitudes within cuise- and level-phases
		filterCruiseLevelAltitude(table, cruiseIndices, levelIndices, /*maxDeviation*/(double)76);
		
		// Filter overlapping cruise- and level-phases
		filterLevelFromCruiseIndices(levelIndices, cruiseIndices);
		
		// Identification of climb/descent phases
		initiallyIdentifyClimbDescent(table, cruiseIndices, levelIndices, climbIndices, descentIndices);
		
		// Identification and filtering of climb/descent phases regarding vertRate
		filterClimbDescent(table, climbIndices, descentIndices, /*vertRateMax*/(double)6000, /*vertRateMin*/(double)-7000, /*minClimbDescentDuration*/(double)30, /*minClimbDescentSampleCount*/(int)10);
		
		// Merge cruise, level, climb and descent phases
		mergeCruiseLevelClimbDescent(table, verticalFlightPhases, cruiseIndices, levelIndices, climbIndices, descentIndices);
		
		// Filter transitions between UNDEFINED flight-phases regarding vertRate
		filterUndefinedFlightPhasesVertRate(table, verticalFlightPhases, /*vertRateMax*/(double)6000, /*vertRateMin*/(double)-7000);
		
		// Remove Samples of UNDEFINED flight-phases (as these samples are highly probably invalid)
		removeUndefinedFlightPhasesSamples(table, verticalFlightPhases);
		
		// Combine suitable cruise-/level-phases divided by UNDEFINED flight-phases
		combineBrokenCruiseLevelPhases(table, verticalFlightPhases, /*phasesBaroAltDeviationThreshold*/(double)50);
				
		// Determine reliability
		determineReliability(table, verticalFlightPhases);
		
		// Cut the end of Trajectory if certain conditions are met
		cutTrajectoryEnd(/*todArrivalRatio*/(double)0.3, /*minArrivalAltitude*/(double)10000, /*lowReliabilityThreshold*/(double)0.2, /*maxLowReliabilityTime*/(double)8*60, /*maxReclimb*/(double)1000);

		// Cut the beginning of Trajectory if certain conditions are met
		cutTrajectoryBeginning(/*todDepartureRatio*/(double)0.3, /*maxDepartureAltitude*/(double)10000, /*lowReliabilityThreshold*/(double)0.2, /*maxLowReliabilityTime*/(double)8*60);

		// Filter redundant baroAlt samples
		filterRedundantSamples(table, verticalFlightPhases, /*baroAltThreshold*/(double)26);
		
		return 0;
	}
	
	public int[][] getVerticalFlightPhases() {
		int[][] verticalFlightPhasesMatrix = new int[verticalFlightPhases.size()][2];
		for(int i=0; i<verticalFlightPhases.size(); i++) {
			verticalFlightPhasesMatrix[i][0] = verticalFlightPhases.get(i).startIndex;
			switch(verticalFlightPhases.get(i).phase) {
			case CRUISE:
				verticalFlightPhasesMatrix[i][1] = 0;
				break;
			case LEVEL:
				verticalFlightPhasesMatrix[i][1] = 1;
				break;
			case CLIMB:
				verticalFlightPhasesMatrix[i][1] = 2;
				break;
			case DESCENT:
				verticalFlightPhasesMatrix[i][1] = 3;
				break;
			default:
				verticalFlightPhasesMatrix[i][1] = -1;
				break;
			}
		}
		return verticalFlightPhasesMatrix;
	}
	
	public double[][] getCruiseTimestamps() {
		double[][] cruiseTimestamps = new double[cruiseIndices.size()][2];
		for(int i=0; i<cruiseIndices.size(); i++) {
			cruiseTimestamps[i][0] = table.time[cruiseIndices.get(i)[0]];
			cruiseTimestamps[i][1] = table.time[cruiseIndices.get(i)[1]];
		}
		return cruiseTimestamps;
	}
	
	public double[][] getLevelTimestamps() {
		double[][] levelTimestamps = new double[levelIndices.size()][2];
		for(int i=0; i<levelIndices.size(); i++) {
			levelTimestamps[i][0] = table.time[levelIndices.get(i)[0]];
			levelTimestamps[i][1] = table.time[levelIndices.get(i)[1]];
		}
		return levelTimestamps;
	}
	
	public double[][] getClimbTimestamps() {
		double[][] climbTimestamps = new double[climbIndices.size()][2];
		for(int i=0; i<climbIndices.size(); i++) {
			climbTimestamps[i][0] = table.time[climbIndices.get(i)[0]];
			climbTimestamps[i][1] = table.time[climbIndices.get(i)[1]];
		}
		return climbTimestamps;
	}
	
	public double[][] getDescentTimestamps() {
		double[][] descentTimestamps = new double[descentIndices.size()][2];
		for(int i=0; i<descentIndices.size(); i++) {
			descentTimestamps[i][0] = table.time[descentIndices.get(i)[0]];
			descentTimestamps[i][1] = table.time[descentIndices.get(i)[1]];
		}
		return descentTimestamps;
	}

	public double interpolateBaroAlt(double timestamp) {
		int leftIndex = -1;
		int rightIndex = -1;
		
		// Determine left and right index within array ...
		for(int i=0; i<table.vaSample.length; i++) {
			if(!table.vaSample[i] || table.vrSample[i])
				continue;
			if(table.time[i] >= timestamp) {
				rightIndex = i;
				break;
			}
			leftIndex = i;
		}
		// ... Determine left and right index within array
		
		if(rightIndex == -1)						// when timestamp lies right-off the channel-window
			return Double.MAX_VALUE;
		if(timestamp == table.time[rightIndex])		// when timestamp lies on a sample (including first and last sample)
			return table.baroAlt[rightIndex];
		if(leftIndex == -1)							// when timestamp lies left-off the channel-window
			return Double.MAX_VALUE;
		
		double baroAlt = table.baroAlt[leftIndex] + (table.baroAlt[rightIndex]-table.baroAlt[leftIndex])/(table.time[rightIndex]-table.time[leftIndex])*(timestamp-table.time[leftIndex]);
		
		return baroAlt;
	}
	
	public double interpolateReliability(double timestamp) {
		int leftIndex = -1;
		int rightIndex = -1;
		
		// Determine left and right index within array ...
		for(int i=0; i<reliabilityTime.length; i++) {
			if(reliabilityTime[i] >= timestamp) {
				rightIndex = i;
				break;
			}
			leftIndex = i;
		}
		// ... Determine left and right index within array
		
		if(rightIndex == -1)							// when timestamp lies right-off the channel-window
			return 0;
		if(timestamp == reliabilityTime[rightIndex])	// when timestamp lies on a sample (including first and last sample)
			return reliability[rightIndex];
		if(leftIndex == -1)								// when timestamp lies left-off the channel-window
			return 0;
		
		double reliabilityInterpolated = reliability[leftIndex] + (reliability[rightIndex]-reliability[leftIndex])/(reliabilityTime[rightIndex]-reliabilityTime[leftIndex])*(timestamp-reliabilityTime[leftIndex]);
		
		return reliabilityInterpolated;
	}

	private void initiallyIdentifyClimbDescent(TableVertical table, ArrayList<int[]> cruiseIndices, ArrayList<int[]> levelIndices, ArrayList<int[]> climbIndices, ArrayList<int[]> descentIndices) {
		// (only level phases in general of interest)
		ArrayList<int[]> generalLevelIndices = new ArrayList<int[]>();
		generalLevelIndices.addAll(levelIndices);
		generalLevelIndices.addAll(cruiseIndices);
		
		int lastGeneralLevelIndex = -1;
		int nextGeneralLevelIndex;
		
		int i=0;
		while(i<table.time.length) {
			if(!table.vaSample[i]) {
				i++;
				continue;
			}
			// look for following LevelIndex
			nextGeneralLevelIndex = -1;
			followingLevelIndexLoop:
			for(int j=i; j<table.time.length; j++) {
				for(int k=0; k<generalLevelIndices.size(); k++)
					if(j<generalLevelIndices.get(k)[1])
						if(j>generalLevelIndices.get(k)[0]) {
							nextGeneralLevelIndex = k;
							break followingLevelIndexLoop;
						}
			}
			if(nextGeneralLevelIndex == -1) {
				int climbCount = 0;
				int descentCount = 0;
				int lastValidSampleIndex = i;
				int endOfSegment = i;
				double vertRate;
				for(int j=i+1; j<table.time.length; j++) {
					if(!table.vaSample[j])
						continue;
					vertRate = (table.baroAlt[j]-table.baroAlt[lastValidSampleIndex])/((table.lastContact[j]-table.lastContact[lastValidSampleIndex])/60);
					if(vertRate > 0)
						climbCount++;
					if(vertRate < 0)
						descentCount++;
					endOfSegment = j;
				}
				if(endOfSegment == i)
					return;
				if(climbCount >= descentCount) {
					climbIndices.add(new int[] {i, endOfSegment});
					return;
				} else {
					descentIndices.add(new int[] {i, endOfSegment});
					return;
				}
			}

			if(lastGeneralLevelIndex == -1) {
				int climbCount = 0;
				int descentCount = 0;
				int lastValidSampleIndex = i;
				double vertRate;
				if(generalLevelIndices.get(nextGeneralLevelIndex)[0] == i) {
					i = generalLevelIndices.get(nextGeneralLevelIndex)[1];
					continue;
				}
				for(int j=i+1; j<=generalLevelIndices.get(nextGeneralLevelIndex)[0]; j++) {
					if(!table.vaSample[j])
						continue;
					vertRate = (table.baroAlt[j]-table.baroAlt[lastValidSampleIndex])/((table.lastContact[j]-table.lastContact[lastValidSampleIndex])/60);
					if(vertRate > 0)
						climbCount++;
					if(vertRate < 0)
						descentCount++;
				}
				if(climbCount >= descentCount) {
					climbIndices.add(new int[] {i, generalLevelIndices.get(nextGeneralLevelIndex)[0]});
					i = generalLevelIndices.get(nextGeneralLevelIndex)[1];
					continue;
				} else {
					descentIndices.add(new int[] {i, generalLevelIndices.get(nextGeneralLevelIndex)[0]});
					i = generalLevelIndices.get(nextGeneralLevelIndex)[1];
					continue;
				}
			}
			
			// lastGeneralLevelIndex != -1 && nextGeneralLevelIndex != -1
			double lastBaroAlt = table.baroAlt[generalLevelIndices.get(lastGeneralLevelIndex)[1]];
			double followingBaroAlt = table.baroAlt[generalLevelIndices.get(nextGeneralLevelIndex)[1]];
			
			if(followingBaroAlt >= lastBaroAlt)
				climbIndices.add(new int[] {generalLevelIndices.get(lastGeneralLevelIndex)[1], generalLevelIndices.get(nextGeneralLevelIndex)[0]});
			else
				descentIndices.add(new int[] {generalLevelIndices.get(lastGeneralLevelIndex)[1], generalLevelIndices.get(nextGeneralLevelIndex)[0]});
			i = generalLevelIndices.get(nextGeneralLevelIndex)[1];
		}
	}

	private void determineReliability(TableVertical table, ArrayList<VerticalFlightPhase> verticalFlightPhases) {
		double timeStep = 5;
		double timeWindow = 2*60;
		double requiredDensity = 0.5;
		double[] samplesWindowCount;

		int firstValidTimestampIndex = -1;
		int lastValidTimestampIndex = -1;
		for(int i=0; i<table.vaSample.length; i++)
			if(table.vaSample[i]) {
				firstValidTimestampIndex = i;
				break;
			}
		for(int i=table.vaSample.length-1; i>=0; i--)
			if(table.vaSample[i]) {
				lastValidTimestampIndex = i;
				break;
			}
		
		if((firstValidTimestampIndex == -1) || (lastValidTimestampIndex == -1))
			return;
		
		double firstTimestamp = table.time[firstValidTimestampIndex];
		double lastTimestamp = table.time[lastValidTimestampIndex];
		int size;
		if((lastTimestamp-firstTimestamp)/timeStep > Math.round((lastTimestamp-firstTimestamp)/timeStep))
			size = (int)Math.round((lastTimestamp-firstTimestamp)/timeStep)+2;
		else
			size = (int)Math.round((lastTimestamp-firstTimestamp)/timeStep)+1;
		
		reliability = new double[size];
		reliabilityTime = new double[size];
		samplesWindowCount = new double[size];
		for(int i=0; i<size; i++)
			reliabilityTime[i] = firstTimestamp+i*timeStep;
		reliabilityTime[size-1] = lastTimestamp;
		for(int i=0; i<size; i++)
			samplesWindowCount[i] = 0;
		
		// Fill samplesWindowCount-array with timeWindow-margin around valid samples ...
		for(int i=0; i<table.vaSample.length; i++) {
			if(!table.vaSample[i])
				continue;
			int reliabilityTimeIndex = (int)Math.round((table.time[i]-firstTimestamp)/timeStep);
			
			samplesWindowCount[reliabilityTimeIndex] += 1;			// Weight-function for timeWindow-samples at timeDeviation = 0
			double timeDeviation;
			for(int j=-1; (reliabilityTimeIndex+j>=0) && ((timeDeviation=table.time[i]-reliabilityTime[reliabilityTimeIndex+j])<=timeWindow); j--)
				samplesWindowCount[reliabilityTimeIndex+j] += 1/Math.pow((1+Math.pow(timeDeviation/timeWindow,2)),2);		// Weight-function for timeWindow-samples
			for(int j=1; (reliabilityTimeIndex+j<size) && ((timeDeviation=reliabilityTime[reliabilityTimeIndex+j]-table.time[i])<=timeWindow); j++)
				samplesWindowCount[reliabilityTimeIndex+j] += 1/Math.pow((1+Math.pow(timeDeviation/timeWindow,2)),2);		// Weight-function for timeWindow-samples
		}
		// ... Fill samplesWindowCount-array with timeWindow-margin around valid samples
		
		// Calculate reliability with samplesWindowCount ...
		for(int i=0; i<size; i++) {
			reliability[i] = (samplesWindowCount[i]/timeWindow)/requiredDensity;
			if(reliability[i] > 1)
				reliability[i] = 1;
		}
		// ... Calculate reliability with samplesWindowCount
	}

	private void filterRedundantSamples(TableVertical table, ArrayList<VerticalFlightPhase> verticalFlightPhases, double baroAltThreshold) {
		for(int i=0; i<verticalFlightPhases.size(); i++) {

			int phaseStartIndex = verticalFlightPhases.get(i).startIndex;
			int phaseEndIndex;
			if(i<verticalFlightPhases.size()-1)
				phaseEndIndex = verticalFlightPhases.get(i+1).startIndex;
			else
				phaseEndIndex = table.time.length-1;
			// only vaSamples as levelEndIndex
			while(!table.vaSample[phaseEndIndex])
				phaseEndIndex--;
			
			// Set all middle vrSample values to true
			for(int j=phaseStartIndex+1; j<phaseEndIndex; j++)
				if(table.vaSample[j])
					table.vrSample[j] = true;
			
			int lastValidIndex = phaseStartIndex;
			while(lastValidIndex < phaseEndIndex) {
				int nextVrIndex = -1;
				for(int j=lastValidIndex+1; j<=phaseEndIndex; j++)
					if(table.vaSample[j])
						if(!table.vrSample[j]) {
							nextVrIndex = j;
							break;
						}
				
				int requiredValueIndex = checkMaxBaroAltInterpolationDeviation(table, lastValidIndex, baroAltThreshold);
				if(requiredValueIndex == -1)
					lastValidIndex = nextVrIndex;
				else
					table.vrSample[requiredValueIndex] = false;
			}
		}
	}
	
	private int checkMaxBaroAltInterpolationDeviation(TableVertical table, int lastRequiredIndex, double baroAltThreshold) {
		int requiredValueIndex = -1;
		double maxDeviation = 0;
		
		int nextRequiredValueIndex = lastRequiredIndex;
		for(int i=lastRequiredIndex+1; i<table.time.length; i++)
			if(table.vaSample[i])
				if(!table.vrSample[i]) {
					nextRequiredValueIndex = i;
					break;
				}
		
		for(int i=lastRequiredIndex+1; i<nextRequiredValueIndex; i++)
			if(table.vaSample[i]) {
				double interpolationBaroAlt = table.baroAlt[lastRequiredIndex] + (table.baroAlt[nextRequiredValueIndex]-table.baroAlt[lastRequiredIndex])/(table.time[nextRequiredValueIndex]-table.time[lastRequiredIndex])*(table.time[i]-table.time[lastRequiredIndex]);
				double valueBaroAlt = table.baroAlt[i];
				if(Math.abs(interpolationBaroAlt-valueBaroAlt) > baroAltThreshold)
					if(Math.abs(interpolationBaroAlt-valueBaroAlt) > maxDeviation) {
						requiredValueIndex = i;
						maxDeviation = Math.abs(interpolationBaroAlt-valueBaroAlt);
					}
			}
		
		return requiredValueIndex;
	}
	
	private int determinePhaseEndIndex(TableVertical table, ArrayList<VerticalFlightPhase> verticalFlightPhases, int index) {
		int phaseEndIndex;
		if(index+1 < verticalFlightPhases.size())
			phaseEndIndex = verticalFlightPhases.get(index+1).startIndex;
		else
			for(phaseEndIndex = table.vaSample.length-1; phaseEndIndex>=0; phaseEndIndex--)
				if(table.vaSample[phaseEndIndex] && !table.vrSample[phaseEndIndex])
					break;
		return phaseEndIndex;
	}
	private double determineLevelPhaseAltitude(TableVertical table, ArrayList<VerticalFlightPhase> verticalFlightPhases, int index) {
		if(verticalFlightPhases.get(index).phase != VerticalFlightPhase.Phase.LEVEL) {
			System.err.println(String.format("Fatal Error - %s: Determination of LEVEL-phase-altitude of not LEVEL-phase", callsign));
			return 0;
		}
		int levelEndIndex = determinePhaseEndIndex(table, verticalFlightPhases, index);
		return determineLevelPhaseAltitude(table, verticalFlightPhases.get(index).startIndex, levelEndIndex);
	}
	private double determineLevelPhaseAltitude(TableVertical table, int levelStartIndex, int levelEndIndex) {
		double minAltitude = Double.MAX_VALUE;
		double maxAltitude = Double.MIN_VALUE;
		for(int i=levelStartIndex; i<levelEndIndex; i++) {
			if(!table.vaSample[i])
				continue;
			if(table.baroAlt[i] < minAltitude)
				minAltitude = table.baroAlt[i];
			if(table.baroAlt[i] > maxAltitude)
				maxAltitude = table.baroAlt[i];
		}
		return Math.round((minAltitude+maxAltitude)/2);
	}

	private void cutTrajectoryBeginning(double todDepartureRatio, double maxDepartureAltitude, double lowReliabilityThreshold, double maxLowReliabilityTime) {
		double maxAltitude = -1;
		int maxAltitudeStartIndex = -1;
		// Determine max cruise-/level-altitude ...
		double cruiseLevelAltitude;
		for(int i=0; i<verticalFlightPhases.size(); i++) {
			switch(verticalFlightPhases.get(i).phase) {
			case CRUISE:
				break;
			case LEVEL:
				break;
			case CLIMB:
				continue;
			case DESCENT:
				continue;
			case UNDEFINED:
				continue;
			default:
				break;
			}
			if(verticalFlightPhases.get(i).phase == VerticalFlightPhase.Phase.CRUISE)
				cruiseLevelAltitude = calcClosestCruisingLevel(table.baroAlt[verticalFlightPhases.get(i).startIndex]);
			else
				cruiseLevelAltitude = determineLevelPhaseAltitude(table, verticalFlightPhases, i);
			if(cruiseLevelAltitude >= maxAltitude) {
				maxAltitude = cruiseLevelAltitude;
				maxAltitudeStartIndex = verticalFlightPhases.get(i).startIndex;
			}
		}
		// ... Determine max cruise-/level-altitude
		
		if(maxAltitude == -1)
			return;				// No Top Of Descent detected
		
		double departureAltitudeThreshold = maxAltitude * todDepartureRatio;
		if(departureAltitudeThreshold < maxDepartureAltitude)
			departureAltitudeThreshold = maxDepartureAltitude;
		
		if(maxAltitude < departureAltitudeThreshold)
			return;				// Max altitude lower than departureAltitudeThreshold
		
		int departureThresholdIndex = -1;			// threshold-index at which an departure is highly probable
		for(int i=maxAltitudeStartIndex; i>=0; i--) {
			if(!table.vaSample[i] || table.vrSample[i])
				continue;
			if(table.baroAlt[i] <= departureAltitudeThreshold) {
				departureThresholdIndex = i;
				break;
			}
		}
		
		if(departureThresholdIndex == -1)
			return;
		
		// Determine Cut at sequence of low reliability before potential departure ...
		double cutLowReliabilityTimestamp = -1;
		double latestHighReliabilityTimestamp = -1;
		for(int i=reliabilityTime.length-1; i>=0; i--) {
			if(reliabilityTime[i] > table.time[departureThresholdIndex])
				continue;
			if(latestHighReliabilityTimestamp == -1)
				latestHighReliabilityTimestamp = reliabilityTime[i];
			if(reliability[i] > lowReliabilityThreshold) {
				latestHighReliabilityTimestamp = reliabilityTime[i];
				continue;
			}
			if(latestHighReliabilityTimestamp-reliabilityTime[i] > maxLowReliabilityTime) {
				cutLowReliabilityTimestamp = latestHighReliabilityTimestamp;
				break;
			}
		}
		// ... Determine Cut at sequence of low reliability before potential departure
		
		// If cutLowReliabilityTimestamp != -1, then the sequence before cutLowReliabilityTimestamp will be cut because of low reliability

		// Determine Cut at lowest altitude before potential arrival ...
		double cutLowestAltitudeTimestamp = -1;
		int lowestAltitudeIndex = -1;
		for(int i=table.vaSample.length-1; i>=0; i--) {
			if(!table.vaSample[i] || table.vrSample[i])
				continue;
			if(table.time[i] > table.time[departureThresholdIndex])
				continue;
			if(lowestAltitudeIndex == -1) {
				lowestAltitudeIndex = i;
				continue;
			}
			if(table.baroAlt[i] < table.baroAlt[lowestAltitudeIndex]) {
				lowestAltitudeIndex = i;
			}
		}
		if(lowestAltitudeIndex != 0)
			cutLowestAltitudeTimestamp = table.time[lowestAltitudeIndex];
		// ... Determine Cut at lowest altitude before potential arrival
		
		// If cutLowestAltitudeTimestamp != 1, then the sequence after cutLowestAltitudeTimestamp will be cut as the aircraft potentially already arrived

		double cutTimestamp = -1;
		if(cutLowReliabilityTimestamp != -1)
			cutTimestamp = cutLowReliabilityTimestamp;
		if(cutLowestAltitudeTimestamp != -1)
			if((cutTimestamp == -1) || (cutLowestAltitudeTimestamp > cutTimestamp))
				cutTimestamp = cutLowestAltitudeTimestamp;
		
		// Cut trajectory after potential arrival ...
		if(cutTimestamp != -1) {
			// Cutting trajectory before cutTimestamp:
			int firstValidSampleIndex = -1;
			// Remove samples:
			for(int i=table.vaSample.length-1; i>=0; i--) {
				if(table.time[i] >= cutTimestamp) {
					if(table.vaSample[i] && !table.vrSample[i])
						firstValidSampleIndex = i;
					continue;
				}
				table.vaSample[i] = false;
			}
			
			if(firstValidSampleIndex == -1) {
				System.err.println(String.format("Error - %s: No remaining samples for cutting trajectory at the beginning/ departure. Trajectory-cutting (beginning) will be skipped.", callsign));
				return;			// No remaining samples (Potential error, as previous cut-beginning-detections result in valid remaining samples)
			}
			
			// Update/Remove affected VerticalFlightPhases
			while((verticalFlightPhases.size()>=2) && (verticalFlightPhases.get(1).startIndex<=firstValidSampleIndex))
				verticalFlightPhases.remove(0);
			if(verticalFlightPhases.get(0).startIndex < firstValidSampleIndex)
				verticalFlightPhases.get(0).startIndex = firstValidSampleIndex;
			
			// Update reliability
			determineReliability(table, verticalFlightPhases);
		}
		// ... Cut trajectory after potential arrival
	}

	private void cutTrajectoryEnd(double todArrivalRatio, double minArrivalAltitude, double lowReliabilityThreshold, double maxLowReliabilityTime, double maxReclimb) {
		double maxAltitude = -1;
		int maxAltitudeStartIndex = -1;
		// Determine max cruise-/level-altitude ...
		double cruiseLevelAltitude;
		for(int i=0; i<verticalFlightPhases.size(); i++) {
			switch(verticalFlightPhases.get(i).phase) {
			case CRUISE:
				break;
			case LEVEL:
				break;
			case CLIMB:
				continue;
			case DESCENT:
				continue;
			case UNDEFINED:
				continue;
			default:
				break;
			}
			if(verticalFlightPhases.get(i).phase == VerticalFlightPhase.Phase.CRUISE)
				cruiseLevelAltitude = calcClosestCruisingLevel(table.baroAlt[verticalFlightPhases.get(i).startIndex]);
			else
				cruiseLevelAltitude = determineLevelPhaseAltitude(table, verticalFlightPhases, i);
			if(cruiseLevelAltitude >= maxAltitude) {
				maxAltitude = cruiseLevelAltitude;
				maxAltitudeStartIndex = verticalFlightPhases.get(i).startIndex;
			}
		}
		// ... Determine max cruise-/level-altitude
		
		if(maxAltitude == -1)
			return;				// No Top Of Descent detected
		
		double arrivalAltitudeThreshold = maxAltitude * todArrivalRatio;
		if(arrivalAltitudeThreshold < minArrivalAltitude)
			arrivalAltitudeThreshold = minArrivalAltitude;
		
		if(maxAltitude < arrivalAltitudeThreshold)
			return;				// Max altitude lower than arrivalAltitudeThreshold
		
		int arrivalThresholdIndex = -1;			// threshold-index at which an arrival is highly probable
		for(int i=maxAltitudeStartIndex; i<table.vaSample.length; i++) {
			if(!table.vaSample[i] || table.vrSample[i])
				continue;
			if(table.baroAlt[i] <= arrivalAltitudeThreshold) {
				arrivalThresholdIndex = i;
				break;
			}
		}
		
		if(arrivalThresholdIndex == -1)
			return;
		
		// Determine Cut at sequence of low reliability after potential arrival ...
		double cutLowReliabilityTimestamp = -1;
		double lastHighReliabilityTimestamp = -1;
		for(int i=0; i<reliabilityTime.length; i++) {
			if(reliabilityTime[i] < table.time[arrivalThresholdIndex])
				continue;
			if(lastHighReliabilityTimestamp == -1)
				lastHighReliabilityTimestamp = reliabilityTime[i];
			if(reliability[i] > lowReliabilityThreshold) {
				lastHighReliabilityTimestamp = reliabilityTime[i];
				continue;
			}
			if(reliabilityTime[i]-lastHighReliabilityTimestamp > maxLowReliabilityTime) {
				cutLowReliabilityTimestamp = lastHighReliabilityTimestamp;
				break;
			}
		}
		// ... Determine Cut at sequence of low reliability after potential arrival
		
		// If cutLowReliabilityTimestamp != -1, then sequence after cutLowReliabilityTimestamp will be cut because of low reliability

		// Determine Cut at reclimb after potential arrival ...
		double cutReclimbTimestamp = -1;
		int lowestAltitudeIndex = -1;
		for(int i=0; i<table.vaSample.length; i++) {
			if(!table.vaSample[i] || table.vrSample[i])
				continue;
			if(table.time[i] < table.time[arrivalThresholdIndex])
				continue;
			if(lowestAltitudeIndex == -1) {
				lowestAltitudeIndex = i;
				continue;
			}
			if(table.baroAlt[i] < table.baroAlt[lowestAltitudeIndex]) {
				lowestAltitudeIndex = i;
				continue;
			}
			if(table.baroAlt[i]-table.baroAlt[lowestAltitudeIndex] > maxReclimb) {
				cutReclimbTimestamp = table.time[lowestAltitudeIndex];
				break;
			}
		}
		// ... Determine Cut at reclimb after potential arrival
		
		// If cutReclimbTimestamp != -1, then sequence after cutReclimbTimestamp will be cut because of reclimb after potential arrival

		double cutTimestamp = -1;
		if(cutLowReliabilityTimestamp != -1)
			cutTimestamp = cutLowReliabilityTimestamp;
		if(cutReclimbTimestamp != -1)
			if((cutTimestamp == -1) || (cutReclimbTimestamp < cutTimestamp))
				cutTimestamp = cutReclimbTimestamp;
		
		// Cut trajectory after potential arrival ...
		if(cutTimestamp != -1) {
			// Cutting trajectory after cutTimestamp
			int lastValidSampleIndex = -1;
			// Remove samples:
			for(int i=0; i<table.vaSample.length; i++) {
				if(table.time[i] <= cutTimestamp) {
					if(table.vaSample[i] && !table.vrSample[i])
						lastValidSampleIndex = i;
					continue;
				}
				table.vaSample[i] = false;
			}
			
			if(lastValidSampleIndex == -1) {
				System.err.println(String.format("Error - %s: No remaining samples for cutting trajectory at the end/ arrival. Trajectory-cutting (end) will be skipped.", callsign));
				return;			// No remaining samples (Potential error, as previous cut-end-detections result in valid remaining samples)
			}
			
			// Update/Remove affected VerticalFlightPhases
			for(int i=verticalFlightPhases.size()-1; i>=0; i--)
				if(table.time[verticalFlightPhases.get(i).startIndex] >= table.time[lastValidSampleIndex])
					verticalFlightPhases.remove(i);
			
			// Update reliability
			determineReliability(table, verticalFlightPhases);
		}
		// ... Cut trajectory after potential arrival
	}

	private void combineBrokenCruiseLevelPhases(TableVertical table, ArrayList<VerticalFlightPhase> verticalFlightPhases, double phasesBaroAltDeviationThreshold) {
		int i = 1;
		while(i<verticalFlightPhases.size()-1) {
			if(verticalFlightPhases.get(i).phase != VerticalFlightPhase.Phase.UNDEFINED) {
				i++;
				continue;
			}
			// continue if previous flight-phase isn't cruise or level:
			if((verticalFlightPhases.get(i-1).phase != VerticalFlightPhase.Phase.CRUISE) && (verticalFlightPhases.get(i-1).phase != VerticalFlightPhase.Phase.LEVEL)) {
				i++;
				continue;
			}
			// continue if following flight-phase isn't cruise or level:
			if((verticalFlightPhases.get(i+1).phase != VerticalFlightPhase.Phase.CRUISE) && (verticalFlightPhases.get(i+1).phase != VerticalFlightPhase.Phase.LEVEL)) {
				i++;
				continue;
			}
			
			double previousPhaseAltitude;
			double followingPhaseAltitude;
			if(verticalFlightPhases.get(i-1).phase == VerticalFlightPhase.Phase.CRUISE)
				previousPhaseAltitude = calcClosestCruisingLevel(table.baroAlt[verticalFlightPhases.get(i-1).startIndex]);
			else
				previousPhaseAltitude = determineLevelPhaseAltitude(table, verticalFlightPhases.get(i-1).startIndex, verticalFlightPhases.get(i).startIndex);

			if(verticalFlightPhases.get(i+1).phase == VerticalFlightPhase.Phase.CRUISE)
				followingPhaseAltitude = calcClosestCruisingLevel(table.baroAlt[verticalFlightPhases.get(i+1).startIndex]);
			else {
				int levelEndIndex;
				if(i+2 < verticalFlightPhases.size())
					levelEndIndex = verticalFlightPhases.get(i+2).startIndex;
				else
					for(levelEndIndex = table.vaSample.length-1; levelEndIndex>=0; levelEndIndex--)
						if(table.vaSample[levelEndIndex])
							break;
				followingPhaseAltitude = determineLevelPhaseAltitude(table, verticalFlightPhases.get(i+1).startIndex, levelEndIndex);
			}
			
			if(Math.abs(previousPhaseAltitude-followingPhaseAltitude) <= phasesBaroAltDeviationThreshold) {
				if(verticalFlightPhases.get(i+1).phase == VerticalFlightPhase.Phase.CRUISE)
					verticalFlightPhases.get(i-1).phase = VerticalFlightPhase.Phase.CRUISE;
				verticalFlightPhases.remove(i+1);
				verticalFlightPhases.remove(i);
			} else
				i++;
		}
	}
	
	private void removeUndefinedFlightPhasesSamples(TableVertical table, ArrayList<VerticalFlightPhase> verticalFlightPhases) {
		for(int i=0; i<verticalFlightPhases.size(); i++) {
			if(verticalFlightPhases.get(i).phase == VerticalFlightPhase.Phase.UNDEFINED) {
				int phaseStartIndex = verticalFlightPhases.get(i).startIndex;
				if(i != 0) {
					// don't delete last sample of previous phase
					for(phaseStartIndex++; phaseStartIndex<table.vaSample.length; phaseStartIndex++)
						if(table.vaSample[phaseStartIndex])
							break;
				} else {
					// remove first UNDEFINED flight-phase
					verticalFlightPhases.remove(0);
					i--;
				}
				int phaseEndIndex;
				if(i+1 < verticalFlightPhases.size()) {
					// next phase is following
					phaseEndIndex = verticalFlightPhases.get(i+1).startIndex;
					// don't delete start sample of next phase:
					for(phaseEndIndex--; phaseEndIndex >= 0; phaseEndIndex--)
						if(table.vaSample[phaseEndIndex])
							break;
				} else {
					// no next phase
					for(phaseEndIndex = table.vaSample.length-1; phaseEndIndex >= 0; phaseEndIndex--)
						if(table.vaSample[phaseEndIndex])
							break;
					if(i > 0) {
						// only if current phase hasn't been deleted because of being the first UNDEFINED phase of the trajectory: delete this phase as last UNDEFINED phase of the trajectory:
						verticalFlightPhases.remove(i);
					}
				}
				for(int j=phaseStartIndex; j<=phaseEndIndex; j++)
					table.vaSample[j] = false;
			}
		}
	}

	private void filterUndefinedFlightPhasesVertRate(TableVertical table, ArrayList<VerticalFlightPhase> verticalFlightPhases, double vertRateMax, double vertRateMin) {
		// Filter unrealistic climb/descent around UNDEFINED flight-phases ...
		double phaseAverageVerticalRate;
		int i=0;
		mainFlightPhasesLoop:
		while(i<verticalFlightPhases.size()) {
			if(verticalFlightPhases.get(i).phase != VerticalFlightPhase.Phase.UNDEFINED) {
				i++;
				continue;
			}
			int phaseStartIndex = verticalFlightPhases.get(i).startIndex;
			int phaseEndIndex;
			if(i+1 < verticalFlightPhases.size())
				phaseEndIndex = verticalFlightPhases.get(i+1).startIndex;
			else
				for(phaseEndIndex = table.vaSample.length-1; phaseEndIndex>verticalFlightPhases.get(i).startIndex; phaseEndIndex--)
					if(table.vaSample[phaseEndIndex])
						break;
			phaseAverageVerticalRate = (table.baroAlt[phaseEndIndex]-table.baroAlt[phaseStartIndex]) / (table.time[phaseEndIndex]-table.time[phaseStartIndex]) * 60;
			if((phaseAverageVerticalRate > vertRateMax) || (phaseAverageVerticalRate < vertRateMin)) {
				// UNDEFINED flight-phase is completely invalid
				// delete this UNDEFINED flight-phase and as less as other flight-phases to resolve the vertical rates
				int futurePhasesDeleteCount = 0;
				double futurePhasesDeleteTotalTime = 0;
				boolean futurePhasesDeleteReachEnd = false;
				int pastPhasesDeleteCount = 0;
				double pastPhasesDeleteTotalTime = 0;
				boolean pastPhasesDeleteReachStart = false;
				
				// determine count and time of flight-phases to remove in the future ...
				double newFuturePhaseAverageVerticalRate = 0;
				for(futurePhasesDeleteCount=1; i+futurePhasesDeleteCount+1<verticalFlightPhases.size(); futurePhasesDeleteCount++) {
					futurePhasesDeleteTotalTime = (table.time[verticalFlightPhases.get(i+futurePhasesDeleteCount+1).startIndex]-table.time[phaseStartIndex]);
					newFuturePhaseAverageVerticalRate = (table.baroAlt[verticalFlightPhases.get(i+futurePhasesDeleteCount+1).startIndex]-table.baroAlt[phaseStartIndex]) / futurePhasesDeleteTotalTime * 60;
					if((newFuturePhaseAverageVerticalRate <= vertRateMax) && (newFuturePhaseAverageVerticalRate >= vertRateMin))
						break;
				}
				if(i+futurePhasesDeleteCount+1 > verticalFlightPhases.size())
					futurePhasesDeleteCount = 0;
				if(i+futurePhasesDeleteCount+1 == verticalFlightPhases.size()) {
					// previous for loop reached the end
					futurePhasesDeleteReachEnd = true;
					int totalEndIndex;
					for(totalEndIndex = table.vaSample.length-1; totalEndIndex>phaseStartIndex; totalEndIndex--)
						if(table.vaSample[totalEndIndex])
							break;
					futurePhasesDeleteTotalTime = (table.time[totalEndIndex]-table.time[phaseStartIndex]);
					newFuturePhaseAverageVerticalRate = (table.baroAlt[totalEndIndex]-table.baroAlt[phaseStartIndex]) / futurePhasesDeleteTotalTime * 60;
				}
				// ... determine count and time of flight-phases to remove in the future

				// determine count and time of flight-phases to remove in the past ...
				double newPastPhaseAverageVerticalRate = 0;
				for(pastPhasesDeleteCount=1; i-pastPhasesDeleteCount>=0; pastPhasesDeleteCount++) {
					pastPhasesDeleteTotalTime = (table.time[phaseEndIndex]-table.time[verticalFlightPhases.get(i-pastPhasesDeleteCount).startIndex]);
					newPastPhaseAverageVerticalRate = (table.baroAlt[phaseEndIndex]-table.baroAlt[verticalFlightPhases.get(i-pastPhasesDeleteCount).startIndex]) / pastPhasesDeleteTotalTime * 60;
					if((newPastPhaseAverageVerticalRate <= vertRateMax) && (newPastPhaseAverageVerticalRate >= vertRateMin))
						break;
				}
				if(i-pastPhasesDeleteCount <= 0) {
					pastPhasesDeleteReachStart = true;
					pastPhasesDeleteCount--;

				}
				// TO THE LEFT/PAST THE FOR LOOP COVERS ALL PHASES TO THE START OF THE TRAJECTORY
				// ... determine count and time of flight-phases to remove in the past
				
				if(futurePhasesDeleteTotalTime <= pastPhasesDeleteTotalTime) {
					// delete future UNDEFINED flight phases:
					int endIndex;
					if(futurePhasesDeleteReachEnd) {
						for(endIndex = table.vaSample.length-1; endIndex>phaseStartIndex; endIndex--)
							if(table.vaSample[endIndex])
								break;
						for(int j=verticalFlightPhases.get(i).startIndex+1; j<=endIndex; j++)
							table.vaSample[j] = false;
						for(int j=0; j<=futurePhasesDeleteCount; j++)
							verticalFlightPhases.remove(i+0);
						continue mainFlightPhasesLoop;		// equals breaking the loop mainFlightPhasesLoop
					} else {
						endIndex = verticalFlightPhases.get(i+futurePhasesDeleteCount+1).startIndex;
						for(int j=verticalFlightPhases.get(i).startIndex+1; j<endIndex; j++)
							table.vaSample[j] = false;
						for(int j=1; j<=futurePhasesDeleteCount; j++)
							verticalFlightPhases.remove(i+1);
						// if following flight-phase (i+1) is UNDEFINED, combine these two (i and i+1)
						if(verticalFlightPhases.get(i+1).phase == VerticalFlightPhase.Phase.UNDEFINED) {
							verticalFlightPhases.remove(i+1);
						}
						i++;
						continue mainFlightPhasesLoop;
					}
				} else {
					// delete past UNDEFINED flight phases:
					int startIndex;
					if(pastPhasesDeleteReachStart) {
						for(startIndex = 0; startIndex<phaseEndIndex; startIndex++)
							if(table.vaSample[startIndex])
								break;
						for(int j=startIndex; j<phaseEndIndex; j++)
							table.vaSample[j] = false;
						for(int j=0; j<=pastPhasesDeleteCount; j++)
							verticalFlightPhases.remove(0);
						i = 0;
						continue mainFlightPhasesLoop;
					} else {
						startIndex = verticalFlightPhases.get(i-pastPhasesDeleteCount).startIndex;
						for(int j=startIndex+1; j<phaseEndIndex; j++)
							table.vaSample[j] = false;
						for(int j=1; j<=pastPhasesDeleteCount; j++)
							verticalFlightPhases.remove(i-j);
						i = i-pastPhasesDeleteCount;
						verticalFlightPhases.get(i).startIndex = startIndex;
						// if previous flight-phase (i-1) is UNDEFINED, combine these two (i and i-1)
						if(verticalFlightPhases.get(i-1).phase == VerticalFlightPhase.Phase.UNDEFINED) {
							verticalFlightPhases.remove(i);
						} else
							i++;
						continue mainFlightPhasesLoop;
					}
				}
			} else {
				// UNDEFINED flight-phase entry and exit are valid regarding climb/descent-rates
				i++;
			}
		}
		// ... Filter unrealistic climb/descent around UNDEFINED flight-phases
	}

	private void mergeCruiseLevelClimbDescent(TableVertical table, ArrayList<VerticalFlightPhase> verticalFlightPhases, ArrayList<int[]> cruiseIndices, ArrayList<int[]> levelIndices, ArrayList<int[]> climbIndices, ArrayList<int[]> descentIndices) {
		int currentIndex = 0;
		mainLoop:
		while(currentIndex < table.time.length-1) {
			if(!table.vaSample[currentIndex]) {
				currentIndex++;
				continue;
			}
			VerticalFlightPhase.Phase phase = VerticalFlightPhase.Phase.UNDEFINED;
			for(int i=0; i<cruiseIndices.size(); i++)
				if((currentIndex >= cruiseIndices.get(i)[0]) && (currentIndex <= cruiseIndices.get(i)[1]-1)) {
					phase = VerticalFlightPhase.Phase.CRUISE;
					verticalFlightPhases.add(new VerticalFlightPhase(phase, currentIndex));
					currentIndex = cruiseIndices.get(i)[1];
					cruiseIndices.remove(i);
					continue mainLoop;
				}
			for(int i=0; i<levelIndices.size(); i++)
				if((currentIndex >= levelIndices.get(i)[0]) && (currentIndex <= levelIndices.get(i)[1]-1)) {
					phase = VerticalFlightPhase.Phase.LEVEL;
					verticalFlightPhases.add(new VerticalFlightPhase(phase, currentIndex));
					currentIndex = levelIndices.get(i)[1];
					levelIndices.remove(i);
					continue mainLoop;
				}
			for(int i=0; i<climbIndices.size(); i++)
				if((currentIndex >= climbIndices.get(i)[0]) && (currentIndex <= climbIndices.get(i)[1]-1)) {
					phase = VerticalFlightPhase.Phase.CLIMB;
					verticalFlightPhases.add(new VerticalFlightPhase(phase, currentIndex));
					currentIndex = climbIndices.get(i)[1];
					climbIndices.remove(i);
					continue mainLoop;
				}
			for(int i=0; i<descentIndices.size(); i++)
				if((currentIndex >= descentIndices.get(i)[0]) && (currentIndex <= descentIndices.get(i)[1]-1)) {
					phase = VerticalFlightPhase.Phase.DESCENT;
					verticalFlightPhases.add(new VerticalFlightPhase(phase, currentIndex));
					currentIndex = descentIndices.get(i)[1];
					descentIndices.remove(i);
					continue mainLoop;
				}
			if(verticalFlightPhases.size() != 0) {
				if(verticalFlightPhases.get(verticalFlightPhases.size()-1).phase != VerticalFlightPhase.Phase.UNDEFINED) {
					// only if vaSamples are following
					for(int i=currentIndex+1; i<table.time.length; i++)
						if(table.vaSample[i]) {
							verticalFlightPhases.add(new VerticalFlightPhase(phase, currentIndex));
							break;
						}
				}
			} else {
				verticalFlightPhases.add(new VerticalFlightPhase(phase, currentIndex));
			}
			currentIndex++;
		}
	}

	private void filterClimbDescent(TableVertical table, ArrayList<int[]> climbIndices, ArrayList<int[]> descentIndices, double vertRateMax, double vertRateMin, double minClimbDescentDuration, int minClimbDescentSampleCount) {
		int lastValidSampleIndex;
		double lastVertRate;
		
		// climb segments
		for(int i=0; i<climbIndices.size(); i++) {
			lastValidSampleIndex = -1;
			for(int j=climbIndices.get(i)[1]; j>=climbIndices.get(i)[0]; j--) {
				if(!table.vaSample[j])
					continue;
				if(lastValidSampleIndex == -1) {
					lastValidSampleIndex = j;
					continue;
				}
				lastVertRate = (table.baroAlt[lastValidSampleIndex]-table.baroAlt[j])/((table.lastContact[lastValidSampleIndex]-table.lastContact[j])/60);
				
				if(lastVertRate > vertRateMax) {
					table.vaSample[j] = false;
					if(j == climbIndices.get(i)[0])
						climbIndices.get(i)[0] = lastValidSampleIndex;
					continue;
				}
				if(lastVertRate <= 0) {
					table.vaSample[j] = false;
					if(j == climbIndices.get(i)[0])
						climbIndices.get(i)[0] = lastValidSampleIndex;
					continue;
				}
				
				lastValidSampleIndex = j;
			}
		}
		for(int i=0; i<climbIndices.size();)
			if(table.time[climbIndices.get(i)[1]]-table.time[climbIndices.get(i)[0]] < minClimbDescentDuration)
				climbIndices.remove(i);
			else
				i++;
		int vaCount;
		for(int i=0; i<climbIndices.size();) {
			vaCount = 0;
			for(int j=climbIndices.get(i)[0]; j<climbIndices.get(i)[1]; j++)
				if(table.vaSample[j])
					vaCount++;
			if(vaCount < minClimbDescentSampleCount)
				climbIndices.remove(i);
			else
				i++;
		}

		// descent segments
		for(int i=0; i<descentIndices.size(); i++) {
			lastValidSampleIndex = -1;
			for(int j=descentIndices.get(i)[0]; j<=descentIndices.get(i)[1]; j++) {
				if(!table.vaSample[j])
					continue;
				if(lastValidSampleIndex == -1) {
					lastValidSampleIndex = j;
					continue;
				}
				lastVertRate = (table.baroAlt[j]-table.baroAlt[lastValidSampleIndex])/((table.lastContact[j]-table.lastContact[lastValidSampleIndex])/60);
				
				if(lastVertRate >= 0) {
					table.vaSample[j] = false;
					if(j == descentIndices.get(i)[1])
						descentIndices.get(i)[1] = lastValidSampleIndex;
					continue;
				}
				if(lastVertRate < vertRateMin) {
					table.vaSample[j] = false;
					if(j == descentIndices.get(i)[1])
						descentIndices.get(i)[1] = lastValidSampleIndex;
					continue;
				}
				
				lastValidSampleIndex = j;
			}
		}
		for(int i=0; i<descentIndices.size();)
			if(table.time[descentIndices.get(i)[1]]-table.time[descentIndices.get(i)[0]] < minClimbDescentDuration)
				descentIndices.remove(i);
			else
				i++;
		for(int i=0; i<descentIndices.size();) {
			vaCount = 0;
			for(int j=descentIndices.get(i)[0]; j<descentIndices.get(i)[1]; j++)
				if(table.vaSample[j])
					vaCount++;
			if(vaCount < minClimbDescentSampleCount)
				descentIndices.remove(i);
			else
				i++;
		}
	}

	private void filterCruiseLevelAltitude(TableVertical table, ArrayList<int[]> cruiseIndices, ArrayList<int[]> levelIndices, double maxDeviation) {
		// filter cruise phases ...
		for(int i=0; i<cruiseIndices.size(); i++) {
			double cruiseAltitude = calcClosestCruisingLevel(table.baroAlt[cruiseIndices.get(i)[0]]);
			int lastValidIndex = -1;
			for(int j=cruiseIndices.get(i)[0]; j<=cruiseIndices.get(i)[1]; j++) {
				if(!table.vaSample[j])
					continue;
				if(Math.abs(table.baroAlt[j]-cruiseAltitude) > maxDeviation) {
					table.vaSample[j] = false;
					if(j == cruiseIndices.get(i)[0])
						for(int k=j+1; k<=cruiseIndices.get(i)[1]; k++)
							if(table.vaSample[k]) {
								cruiseIndices.get(i)[0] = k;
								break;
							}
					if(j == cruiseIndices.get(i)[1])
						if(lastValidIndex != -1)
							cruiseIndices.get(i)[1] = lastValidIndex;
						else
							cruiseIndices.get(i)[1] = cruiseIndices.get(i)[0];
				} else
					lastValidIndex = j;
			}
		}
		// remove empty cruise-phases
		for(int i=0; i<cruiseIndices.size();)
			if(cruiseIndices.get(i)[0] == cruiseIndices.get(i)[1])
				cruiseIndices.remove(i);
			else
				i++;
		// ... filter cruise phases

		// filter level phases ...
		for(int i=0; i<levelIndices.size(); i++) {
			double levelAltitude = determineLevelPhaseAltitude(table, levelIndices.get(i)[0], levelIndices.get(i)[1]);
			int lastValidIndex = -1;
			for(int j=levelIndices.get(i)[0]; j<=levelIndices.get(i)[1]; j++) {
				if(!table.vaSample[j])
					continue;
				if(Math.abs(table.baroAlt[j]-levelAltitude) > maxDeviation) {
					table.vaSample[j] = false;
					if(j == levelIndices.get(i)[0])
						for(int k=j+1; k<=levelIndices.get(i)[1]; k++)
							if(table.vaSample[k]) {
								levelIndices.get(i)[0] = k;
								break;
							}
					if(j == levelIndices.get(i)[1])
						if(lastValidIndex != -1)
							levelIndices.get(i)[1] = lastValidIndex;
						else
							levelIndices.get(i)[1] = levelIndices.get(i)[0];
				} else
					lastValidIndex = j;
			}
		}
		// remove empty level-phases
		for(int i=0; i<levelIndices.size();)
			if(levelIndices.get(i)[0] == levelIndices.get(i)[1])
				levelIndices.remove(i);
			else
				i++;
		// ... filter level phases
	}

	private void filterLevelFromCruiseIndices(ArrayList<int[]> levelIndices, ArrayList<int[]> cruiseIndices) {
		int i=0;
		int[] levelSegment;
		int[] cruiseSegment;
		levelIndicesLoop:
		while(i<levelIndices.size()) {
			levelSegment = levelIndices.get(i);
			for(int j=0; j<cruiseIndices.size(); j++) {
				cruiseSegment = cruiseIndices.get(j);
				if((levelSegment[0] >= cruiseSegment[0]) && (levelSegment[0] <= cruiseSegment[1])) {
					if(levelSegment[1] > cruiseSegment[1])
						cruiseIndices.get(j)[1] = levelSegment[1];
					levelIndices.remove(i);
					continue levelIndicesLoop;
				}
				if((levelSegment[1] >= cruiseSegment[0]) && (levelSegment[1] <= cruiseSegment[1])) {
					if(levelSegment[0] < cruiseSegment[0])
						cruiseIndices.get(j)[0] = levelSegment[0];
					levelIndices.remove(i);
					continue levelIndicesLoop;
				}
			}
			i++;
		}

		// overlapping cruise- and level-phases have already been solved by extending the cruise-phases
		// --> only the check if one cruise-timestamp is within a level-phase is sufficient to identify cruise-phases which are a subset of the level-phase
		cruiseIndicesLoop:
		while(i<cruiseIndices.size()) {
			cruiseSegment = cruiseIndices.get(i);
			for(int j=0; j<levelIndices.size(); j++) {
				levelSegment = levelIndices.get(j);
				if((cruiseSegment[0] >= levelSegment[0]) && (cruiseSegment[0] <= levelSegment[1])) {
					cruiseIndices.remove(i);
					continue cruiseIndicesLoop;
				}
				// one timestamp-check is sufficient, no need to check cruiseSegment[1]
/*				if((cruiseSegment[1] >= levelSegment[0]) && (cruiseSegment[1] <= levelSegment[1])) {
					cruiseIndices.remove(i);
					continue cruiseIndicesLoop;
				}*/
			}
			i++;
		}
	}

	private ArrayList<int[]> determineLevelIndices(TableVertical talbe, double altitudeDeviationMargin, int minSegmentSampleCount) {
		ArrayList<int[]> levelIndices = new ArrayList<int[]>();
		
		int segmentStartIndex = -1;
		int segmentLastLevelIndex = -1;
		int segmentSampleCount = 0;
		double segmentMinAltitude = Double.MAX_VALUE;
		double segmentMaxAltitude = Double.MIN_VALUE;
		
		for(int i=0; i<table.baroAlt.length; i++) {
			if(!table.vaSample[i])
				continue;
			if(i <= segmentLastLevelIndex)		// (gap was shifted)
				continue;
			if(segmentStartIndex == -1) {
				segmentStartIndex = i;
				segmentLastLevelIndex = i;
				segmentSampleCount = 1;
				segmentMinAltitude = table.baroAlt[i];
				segmentMaxAltitude = table.baroAlt[i];
			}
			if(table.baroAlt[i] < segmentMinAltitude)
				segmentMinAltitude = table.baroAlt[i];
			if(table.baroAlt[i] > segmentMaxAltitude)
				segmentMaxAltitude = table.baroAlt[i];
			
			if(segmentMaxAltitude-segmentMinAltitude > altitudeDeviationMargin) {
				if(segmentSampleCount >= minSegmentSampleCount) {
					if((double)segmentSampleCount/(table.time[segmentLastLevelIndex]-table.time[segmentStartIndex]) >= (double)1/4) {
						levelIndices.add(new int[] {segmentStartIndex, segmentLastLevelIndex});
					}
				}
				segmentStartIndex = i;
				segmentLastLevelIndex = i;
				segmentSampleCount = 1;
				segmentMinAltitude = table.baroAlt[i];
				segmentMaxAltitude = table.baroAlt[i];
			} else {
				segmentLastLevelIndex = i;
				segmentSampleCount++;
			}
		}
		
		return levelIndices;
	}
	
	private ArrayList<int[]> determineCruiseIndices(TableVertical table, double altitudeDeviationMargin, int minSegmentSampleCount) {
		ArrayList<int[]> cruiseIndices = new ArrayList<int[]>();
		
		int segmentStartIndex = -1;
		int segmentLastCruiseIndex = -1;
		int segmentSampleCount = 0;
		double segmentCruiseAltitude = 0;
		boolean withinCruiseSegment = false;
		
		sampleLoop:
		for(int i=0; i<table.baroAlt.length; i++) {
			if(!table.vaSample[i])
				continue;
			if(i <= segmentLastCruiseIndex)		// (gap was shifted)
				continue;
			if(withinCruiseSegment) {
				if(Math.abs(table.baroAlt[i]-segmentCruiseAltitude) <= altitudeDeviationMargin) {
					segmentLastCruiseIndex = i;
					segmentSampleCount++;
					continue;
				} else {
					// look ahead 60 sec.; when at least 30% is within margin, close gap
					double timeWithinMargin = 0;
					int previousValueIndex = -1;
					for(int j=i+1; j<table.baroAlt.length; j++) {
						if(!table.vaSample[j])
							continue;
						if(previousValueIndex == -1) {
							previousValueIndex = j;
							continue;
						}
						if(Math.abs(table.baroAlt[previousValueIndex]-segmentCruiseAltitude) <= altitudeDeviationMargin)
							if(Math.abs(table.baroAlt[j]-segmentCruiseAltitude) <= altitudeDeviationMargin)
								timeWithinMargin += table.time[j]-table.time[previousValueIndex];
						if(timeWithinMargin >= (double)60*0.3) {
							segmentLastCruiseIndex = j;
							continue sampleLoop;
						}
						if(table.time[j]-table.time[i] > 60)
							break;
						previousValueIndex = j;
					}
					// gap greater than 60 sec. / 30%:
					if(segmentSampleCount >= minSegmentSampleCount)
						cruiseIndices.add(new int[] {segmentStartIndex, segmentLastCruiseIndex});
					withinCruiseSegment = false;
					segmentSampleCount = 0;
				}
			}
			segmentCruiseAltitude = calcClosestCruisingLevel(table.baroAlt[i]);
			if(Math.abs(table.baroAlt[i]-segmentCruiseAltitude) <= altitudeDeviationMargin) {
				segmentStartIndex = i;
				segmentLastCruiseIndex = i;
				segmentSampleCount = 1;
				withinCruiseSegment = true;
			}
		}
		
		return cruiseIndices;
	}
	
	private double calcClosestCruisingLevel(double altitudeInFeet) {
		double closestCruisingLevel = 1000 * Math.floor((altitudeInFeet+500)/1000);
		return closestCruisingLevel;
	}
	
	private void convertBaroAltToFeet(TableVertical table) {
		for(int i=0; i<table.baroAlt.length; i++)
			table.baroAlt[i] *= CosMath.METER_TO_FEET;
	}
	
	private int filterMovingMedianBaroAlt(TableVertical table, int halfWindowSize) {
		double[] windowValues = new double[2*halfWindowSize+1];
		int windowSize = 0;
		int halfCount;
		double[] channelFiltered = new double[table.baroAlt.length];
		double sortTmpValue;
		
		for(int i=0; i<table.baroAlt.length; i++) {
			// fill windowValues ...
			windowValues[0] = table.baroAlt[i];
			windowSize = 1;
			halfCount = 0;
			for(int j=-1; halfCount<halfWindowSize; j--) {
				if(i+j < 0)
					break;
				if(table.vaSample[i+j]) {
					windowValues[windowSize] = table.baroAlt[i+j];
					windowSize++;
					halfCount++;
				}
			}
			halfCount = 0;
			for(int j=1; halfCount<halfWindowSize; j++) {
				if(i+j >= table.baroAlt.length)
					break;
				if(table.vaSample[i+j]) {
					windowValues[windowSize] = table.baroAlt[i+j];
					windowSize++;
					halfCount++;
				}
			}
			// ... fill windowValues
			// sort windowValues ...
			for(int j=0; j<windowSize; j++)
				for(int k=j+1; k<windowSize; k++)
					if(windowValues[k] < windowValues[j]) {
						sortTmpValue = windowValues[j];
						windowValues[j] = windowValues[k];
						windowValues[k] = sortTmpValue;
					}
			// ... sort windowValues
			// set median value:
			if(windowSize%2 == 0)
				channelFiltered[i] = (windowValues[windowSize/2]+windowValues[windowSize/2-1])/2;
			else
				channelFiltered[i] = windowValues[(int)Math.round((double)windowSize/2-0.5)];
		}
		
		for(int i=0; i<table.baroAlt.length; i++)
			table.baroAlt[i] = channelFiltered[i];
		
		return 0;
	}
	
	private int filterRedundantSamples(TableVertical table, double[] channel) {
		if(channel == null)
			return -1;
		
		for(int i=1; i<channel.length; i++)
			if(channel[i] == channel[i-1])
				table.vaSample[i] = false;
		
		return 0;
	}
}
