package de.tu_bs.iff.adsb.dataparser.lib;

import java.util.ArrayList;

import de.tu_bs.iff.adsb.dataparser.cosmath.CosMath;

/**
 * Class containing a horizontal trajectory and functions for parsing the horizontal path. 
 * Considered channels of TableHorizontal: lat, lon, lastPosUpdate
 * Only samples of channels of TableHorizontal, which hold valid values (vaValue[] == true) for all channels mentioned above will be considered. 
 */
public class TrajectoryHorizontal {
	public String callsign = null;
	public String icao24 = null;

	public double[] reliability;
	public double[] reliabilityTime;

	public TableHorizontal table = new TableHorizontal();		// table for parsed trajectory
	
	public TableHorizontal tableRaw = new TableHorizontal();	// table for initial trajectory
	
	public TrajectoryHorizontal() {
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
	public double[] getLastPosUpdate() {
		return getChannelVaVrSamples(table.lastPosUpdate);
	}
	public double[] getLat() {
		return getChannelVaVrSamples(table.lat);
	}
	public double[] getLon() {
		return getChannelVaVrSamples(table.lon);
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
	public double[] getLatRaw() {
		return tableRaw.lat;
	}
	public double[] getLonRaw() {
		return tableRaw.lon;
	}

	/**
	 * Sets TrajectoryHorizontals raw-trajectory (tableRaw) from StateVectorsData4-trajectory
	 * @param sourceTrajectory Source trajectory
	 * @return ErrorCode; -1: error; 0: successful
	 */
	public int setRawTrajectoryFromStateVectorsData4(TrajectoryStateVectorsData4 sourceTrajectory) {
		if(sourceTrajectory.table.time == null)
			return -1;
		
		// obtain number of samples, which contain (va = true): lat, lon, lastPosUpdate: 
		int numberOfApplicableSamples = 0;
		for(int i=0; i<sourceTrajectory.table.time.length; i++)
			if(sourceTrajectory.table.vaLat[i])
				if(sourceTrajectory.table.vaLon[i])
					if(sourceTrajectory.table.vaLastPosUpdate[i])
						numberOfApplicableSamples++;
		
		// allocate trajectory variables with the number of samples
		tableRaw.allocateArrayMemory(numberOfApplicableSamples);
		
		// set callsign and icao24
		callsign = new String(sourceTrajectory.callsign);
		icao24 = new String(sourceTrajectory.icao24);

		// get applicable Samples from sourceTrajectory
		int currentTableRawIndex = 0;
		for(int i=0; i<sourceTrajectory.table.time.length; i++)
			if(sourceTrajectory.table.vaLat[i])
				if(sourceTrajectory.table.vaLon[i])
					if(sourceTrajectory.table.vaLastPosUpdate[i]) {
						tableRaw.time[currentTableRawIndex] = (double)sourceTrajectory.table.time[i];		// (tableRaw.time is double[], whereas sourceTrajectory.table.time is int[])
						tableRaw.lat[currentTableRawIndex] = sourceTrajectory.table.lat[i];
						tableRaw.lon[currentTableRawIndex] = sourceTrajectory.table.lon[i];
						tableRaw.lastPosUpdate[currentTableRawIndex] = sourceTrajectory.table.lastPosUpdate[i];
						tableRaw.vaSample[currentTableRawIndex] = true;
						tableRaw.vrSample[currentTableRawIndex] = false;
						currentTableRawIndex++;
					}
		return 0;
	}
	
	/**
	 * Parses the raw horizontal trajectory in tableRaw and stores the result in table. 
	 * Data of tableRaw will not be changed. 
	 * @return Error code; -1: error; 0: successful
	 */
	public int parseTrajectory(boolean filterRedundantOrthodromeInterpolationSamples) {
		if(tableRaw.time == null)
			return -1;
		
		table.allocateArrayMemory(tableRaw.time.length);
		// copy trajectory-data from tableRaw to table; only content of table will be changed during parsing afterwards: 
		for(int i=0; i<table.time.length; i++) {
			table.time[i] = tableRaw.time[i];
			table.lat[i] = tableRaw.lat[i];
			table.lon[i] = tableRaw.lon[i];
			table.lastPosUpdate[i] = tableRaw.lastPosUpdate[i];
			table.vaSample[i] = tableRaw.vaSample[i];
			table.vrSample[i] = tableRaw.vrSample[i];
		}
		
		// Filter Samples with consecutive lastPosUpdate value
		filterRedundantSamples(table, table.lastPosUpdate);
		
		// Filter single Samples with sharp track-angle-change
		filterTrackAngleChange(table, /*maxTrackAngleChange*/(double)30, /*maxSampleRemovalCount*/(int)8);

		// Filter Samples by unrealistic GroundSpeed
		filterUnrealisticGroundSpeed(table, /*maxGroundSpeed*/(double)750);

		// Determine reliability
		determineReliability(table);

		// Filter redundant (orthodrome-interpolation) Samples in horizontal plane (if requested)
		if(filterRedundantOrthodromeInterpolationSamples)
			filterRedundantOrthodromeInterpolationSamples(table, /*ctdThreshold*/(double)0.01, /*ltdThreshold*/(double)0.1);

		return 0;
	}
	
	
	public double[] interpolatePos(double timestamp) {
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
			return null;
		if(timestamp == table.time[rightIndex])		// when timestamp lies on a sample (including first and last sample)
			return new double[] {table.lat[rightIndex], table.lon[rightIndex]};
		if(leftIndex == -1)							// when timestamp lies left-off the channel-window
			return null;
		
		double orthodromeRelativePosition = (timestamp-table.time[leftIndex])/(table.time[rightIndex]-table.time[leftIndex]);
		double[] posA = new double[] {CosMath.DEG_TO_RAD*table.lat[leftIndex], CosMath.DEG_TO_RAD*table.lon[leftIndex]};
		double[] posB = new double[] {CosMath.DEG_TO_RAD*table.lat[rightIndex], CosMath.DEG_TO_RAD*table.lon[rightIndex]};
		double[] interpolatedPos = CosMath.orthodromeInterpolate(posA, posB, orthodromeRelativePosition);
		interpolatedPos[0] *= CosMath.RAD_TO_DEG;
		interpolatedPos[1] *= CosMath.RAD_TO_DEG;
		
		return interpolatedPos;
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


	private void determineReliability(TableHorizontal table) {
		double timeStep = 5;
		double timeWindow = 2*60;
		double requiredDensity = 0.25;		// half density of vertical profile
		double[] samplesWindowCount;

		ArrayList<Integer> validIndices = new ArrayList<Integer>();
		for(int i=0; i<table.vaSample.length; i++)
			if(table.vaSample[i])
				validIndices.add(i);
		
		if(validIndices.size() == 0)
			return;
		
		double firstTimestamp = table.time[validIndices.get(0)];
		double lastTimestamp = table.time[validIndices.get(validIndices.size()-1)];
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

	private int filterRedundantOrthodromeInterpolationSamples(TableHorizontal table, double ctdThreshold, double ltdThreshold) {
		ArrayList<Integer> orthodromeAnchors = new ArrayList<Integer>();
		
		int firstAvailableSampleIndex = -1;
		int lastAvailableSampleIndex = -1;
		for(int i=0; i<table.vaSample.length; i++)
			if(table.vaSample[i]) {
				firstAvailableSampleIndex = i;
				break;
			}
		for(int i=table.vaSample.length-1; i>=0; i--)
			if(table.vaSample[i]) {
				lastAvailableSampleIndex = i;
				break;
			}
		
		if((firstAvailableSampleIndex == -1) || (lastAvailableSampleIndex == -1))
			return -1;
		
		orthodromeAnchors.add(firstAvailableSampleIndex);
		orthodromeAnchors.add(lastAvailableSampleIndex);
		
		int lastValidAnkerIndex = 0;
		double[] orthodromePosA = new double[2];
		double[] orthodromePosB = new double[2];
		double orthodromePosAtime;
		double orthodromePosBtime;
		while(lastValidAnkerIndex != orthodromeAnchors.size()-1) {
			orthodromePosA[0] = CosMath.DEG_TO_RAD*table.lat[(int)(orthodromeAnchors.get(lastValidAnkerIndex))];
			orthodromePosA[1] = CosMath.DEG_TO_RAD*table.lon[(int)(orthodromeAnchors.get(lastValidAnkerIndex))];
			orthodromePosAtime = table.time[(int)(orthodromeAnchors.get(lastValidAnkerIndex))];
			orthodromePosB[0] = CosMath.DEG_TO_RAD*table.lat[(int)(orthodromeAnchors.get(lastValidAnkerIndex+1))];
			orthodromePosB[1] = CosMath.DEG_TO_RAD*table.lon[(int)(orthodromeAnchors.get(lastValidAnkerIndex+1))];
			orthodromePosBtime = table.time[(int)(orthodromeAnchors.get(lastValidAnkerIndex+1))];
			
			double[] trajPos = new double[2];
			double trajPosTime;
			double deviationDistance;
			double maxDeviationDistance = 0;
			int maxDeviationDistanceIndex = -1;
			for(int i=orthodromeAnchors.get(lastValidAnkerIndex)+1; i<orthodromeAnchors.get(lastValidAnkerIndex+1); i++) {
				if(!table.vaSample[i])
					continue;
				trajPos[0] = CosMath.DEG_TO_RAD*table.lat[i];
				trajPos[1] = CosMath.DEG_TO_RAD*table.lon[i];
				trajPosTime = table.time[i];

				double orthodromeRelativePosition = (trajPosTime-orthodromePosAtime)/(orthodromePosBtime-orthodromePosAtime);
				double[] interpolationPos = CosMath.orthodromeInterpolate(orthodromePosA, orthodromePosB, orthodromeRelativePosition);
				double[] orthodromeNearestPos = CosMath.orthodromeNearestPosition(trajPos, orthodromePosA, orthodromePosB);
				
				double ctd = CosMath.KM_TO_NM*CosMath.earthDist(orthodromeNearestPos, trajPos);
				double ltd = CosMath.KM_TO_NM*CosMath.earthDist(orthodromeNearestPos, interpolationPos);
				deviationDistance = CosMath.earthDist(interpolationPos, trajPos);

				if((ctd > ctdThreshold) || (ltd > ltdThreshold))
					if(deviationDistance > maxDeviationDistance) {
						maxDeviationDistance = deviationDistance;
						maxDeviationDistanceIndex = i;
					}
			}
			
			if(maxDeviationDistanceIndex != -1)
				orthodromeAnchors.add(lastValidAnkerIndex+1, maxDeviationDistanceIndex);
			else
				lastValidAnkerIndex++;
		}
		
		// remove all samples (redundancy), except orthodrome-anchors
		for(int i=0; i<table.vaSample.length; i++)
			if(!orthodromeAnchors.contains(i))
				table.vrSample[i] = true;
		
		return 0;
	}

	private double calcWaypointTrackAngleChange(double[] posA, double[] posB, double[] posC) {
		double trackAngleChange = (CosMath.calcOrthodromeTrackAngle(posA, posB, 1)-CosMath.calcOrthodromeTrackAngle(posB, posC, 0));
		while(trackAngleChange > Math.PI)
			trackAngleChange -= 2*Math.PI;
		while(trackAngleChange <= -Math.PI)
			trackAngleChange += 2*Math.PI;
		return trackAngleChange;
	}
	private void filterTrackAngleChange(TableHorizontal table, double maxTrackAngleChange, int maxSampleRemovalCount) {
		ArrayList<Integer> validIndices = new ArrayList<Integer>();
		double[] posA = new double[2];
		double[] posB = new double[2];
		double[] posC = new double[2];
		
		for(int i=0; i<table.vaSample.length; i++)
			if(table.vaSample[i])
				validIndices.add(i);
		
		int i = 2;
		mainSampleLoop:
		while(i<validIndices.size()-2-1) {
			i++;
			
			posA[0] = table.lat[validIndices.get(i-1)]*CosMath.DEG_TO_RAD;
			posA[1] = table.lon[validIndices.get(i-1)]*CosMath.DEG_TO_RAD;
			posB[0] = table.lat[validIndices.get(i)]*CosMath.DEG_TO_RAD;
			posB[1] = table.lon[validIndices.get(i)]*CosMath.DEG_TO_RAD;
			posC[0] = table.lat[validIndices.get(i+1)]*CosMath.DEG_TO_RAD;
			posC[1] = table.lon[validIndices.get(i+1)]*CosMath.DEG_TO_RAD;
			double trackAngleChangePositionTwo = CosMath.RAD_TO_DEG*calcWaypointTrackAngleChange(posA, posB, posC);
			
			if(Math.abs(trackAngleChangePositionTwo) > maxTrackAngleChange) {
				int samplesToRemove = 1;
				while(samplesToRemove <= maxSampleRemovalCount) {
					// trying to remove future values starting at position 2 ...
					// check if necessary additional elements in future are available:
						if(isSolvingByRemovingSamples(i, i+samplesToRemove-1, table, validIndices, maxTrackAngleChange)) {
							for(int j=0; j<samplesToRemove; j++)
								table.vaSample[validIndices.get(i+j)] = false;
							for(int j=0; j<samplesToRemove; j++)
								validIndices.remove(i);
							continue mainSampleLoop;
						}
					// ... trying to remove future values starting at position 2
					// trying to remove future values starting at position 3 ...
					// check if necessary additional elements in future are available:
						if(isSolvingByRemovingSamples(i+1, i/*+1*/+samplesToRemove/*-1*/, table, validIndices, maxTrackAngleChange)) {
							for(int j=0; j<samplesToRemove; j++)
								table.vaSample[validIndices.get(i+1+j)] = false;
							for(int j=0; j<samplesToRemove; j++)
								validIndices.remove(i+1);
							continue mainSampleLoop;
						}
					// ... trying to remove future values starting at position 3
					// trying to remove past values starting at position 2 ...
					// check if necessary additional elements in past are available:
						if(isSolvingByRemovingSamples(i-(samplesToRemove-1), i, table, validIndices, maxTrackAngleChange)) {
							for(int j=0; j>-samplesToRemove; j--)
								table.vaSample[validIndices.get(i+j)] = false;
							for(int j=0; j>-samplesToRemove; j--)
								validIndices.remove(i-(samplesToRemove-1));
							i -= samplesToRemove;
							continue mainSampleLoop;
						}
					// ... trying to remove future values starting at position 2
					samplesToRemove++;
				}
			}
		}
	}
	private boolean isSolvingByRemovingSamples(int startIndex, int endIndex, TableHorizontal table, ArrayList<Integer> validIndices, double maxTrackAngleChange) {
		double[] posA = new double[2];
		double[] posB = new double[2];
		double[] posC = new double[2];
		double[] posD = new double[2];
		
		if(startIndex-2 <= 0)
			return false;
		if(endIndex+2 >= validIndices.size())
			return false;

		posA[0] = table.lat[validIndices.get(startIndex-2)]*CosMath.DEG_TO_RAD;
		posA[1] = table.lon[validIndices.get(startIndex-2)]*CosMath.DEG_TO_RAD;
		posB[0] = table.lat[validIndices.get(startIndex-1)]*CosMath.DEG_TO_RAD;
		posB[1] = table.lon[validIndices.get(startIndex-1)]*CosMath.DEG_TO_RAD;
		posC[0] = table.lat[validIndices.get(endIndex+1)]*CosMath.DEG_TO_RAD;
		posC[1] = table.lon[validIndices.get(endIndex+1)]*CosMath.DEG_TO_RAD;
		posD[0] = table.lat[validIndices.get(endIndex+2)]*CosMath.DEG_TO_RAD;
		posD[1] = table.lon[validIndices.get(endIndex+2)]*CosMath.DEG_TO_RAD;

		double trackAngleChangeLeftBound = CosMath.RAD_TO_DEG*calcWaypointTrackAngleChange(posA, posB, posC);
		double trackAngleChangeRightBound = CosMath.RAD_TO_DEG*calcWaypointTrackAngleChange(posB, posC, posD);
		
		if(Math.abs(trackAngleChangeLeftBound) < maxTrackAngleChange)
			if(Math.abs(trackAngleChangeRightBound) < maxTrackAngleChange)
				return true;
		return false;
	}
	
	private int filterUnrealisticGroundSpeed(TableHorizontal table, double maxGroundSpeed) {
		ArrayList<Integer> validIndices = new ArrayList<Integer>();
		
		for(int i=0; i<table.vaSample.length; i++)
			if(table.vaSample[i])
				validIndices.add(i);

		double groundSpeed;
		double[] posA = new double[2];
		double timeA;
		double[] posB = new double[2];
		double timeB;
		int i = 0;
		mainSampleLoop:
		while(i<validIndices.size()-1) {
			posA[0] = CosMath.DEG_TO_RAD*table.lat[validIndices.get(i)];
			posA[1] = CosMath.DEG_TO_RAD*table.lon[validIndices.get(i)];
			timeA = table.time[validIndices.get(i)];
			posB[0] = CosMath.DEG_TO_RAD*table.lat[validIndices.get(i+1)];
			posB[1] = CosMath.DEG_TO_RAD*table.lon[validIndices.get(i+1)];
			timeB = table.time[validIndices.get(i+1)];
			groundSpeed = CosMath.KM_TO_NM*CosMath.earthDist(posA, posB)/(timeB-timeA)*60*60;
			if(groundSpeed <= maxGroundSpeed) {
				i++;
				continue;
			}
			
			double[] posC = new double[2];
			double timeC;
			for(int removeCount=1; (i+removeCount+1<validIndices.size()) || (i-removeCount>=0); removeCount++) {
				if(i+removeCount+1<validIndices.size()) {
					// try to remove following positions to solve unrealistic groundSpeed
					posC[0] = CosMath.DEG_TO_RAD*table.lat[validIndices.get(i+removeCount+1)];
					posC[1] = CosMath.DEG_TO_RAD*table.lon[validIndices.get(i+removeCount+1)];
					timeC = table.time[validIndices.get(i+removeCount+1)];;
					groundSpeed = CosMath.KM_TO_NM*CosMath.earthDist(posA, posC)/(timeC-timeA)*60*60;
					if(groundSpeed <= maxGroundSpeed) {
						for(int j=0; j<removeCount; j++)
							table.vaSample[validIndices.get(i+j+1)] = false;
						for(int j=0; j<removeCount; j++)
							validIndices.remove(i+1);
						continue mainSampleLoop;
					}
				}
				if(i-removeCount>=0) {
					// try to remove past positions to solve unrealistic groundSpeed
					posC[0] = CosMath.DEG_TO_RAD*table.lat[validIndices.get(i-removeCount)];
					posC[1] = CosMath.DEG_TO_RAD*table.lon[validIndices.get(i-removeCount)];
					timeC = table.time[validIndices.get(i-removeCount)];;
					groundSpeed = CosMath.KM_TO_NM*CosMath.earthDist(posA, posC)/(timeA-timeC)*60*60;
					if(groundSpeed <= maxGroundSpeed) {
						for(int j=0; j<removeCount; j++)
							table.vaSample[validIndices.get(i-j)] = false;
						for(int j=0; j<removeCount; j++)
							validIndices.remove(i-j);
						i -= removeCount-1;
						continue mainSampleLoop;
					}
				}
			}
			i++;
		}
		
		return 0;
	}
	
	private int filterRedundantSamples(TableHorizontal table, double[] channel) {
		if(channel == null)
			return -1;
		
		for(int i=1; i<channel.length; i++)
			if(channel[i] == channel[i-1])
				table.vaSample[i] = false;
		
		return 0;
	}
}
