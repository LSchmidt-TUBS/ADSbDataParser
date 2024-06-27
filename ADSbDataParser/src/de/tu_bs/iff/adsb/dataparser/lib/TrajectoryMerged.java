package de.tu_bs.iff.adsb.dataparser.lib;

import java.util.ArrayList;
import java.util.Collections;

import de.tu_bs.iff.adsb.dataparser.cosmath.CosMath;

/**
 * Class containing a merged trajectory and functions for merging the vertical and horizontal path. 
 */
public class TrajectoryMerged {
	public String callsign = null;
	public String icao24 = null;
	
	private double[] reliability = null;
	private double[] reliabilityTime = null;
	
	private TrajectoryVertical trajectoryVertical = null;
	private TrajectoryHorizontal trajectoryHorizontal = null;
	
	private TableMerged table = new TableMerged();
	
	private ArrayList<VerticalFlightPhase> flightPhases = new ArrayList<VerticalFlightPhase>();
	
	public TrajectoryMerged() {
	}

	public String getCallsign() {
		return callsign;
	}
	public String getIcao24() {
		return icao24;
	}
	
	public double[] getTime() {
		return table.time;
	}
	public double[] getBaroAlt() {
		return table.baroAlt;
	}
	public double[] getLat() {
		return table.lat;
	}
	public double[] getLon() {
		return table.lon;
	}

	public double[][] getFlightPhases() {
		double[][] flightPhasesMatrix = new double[flightPhases.size()][2];
		for(int i=0; i<flightPhases.size(); i++) {
			flightPhasesMatrix[i][0] = table.time[flightPhases.get(i).startIndex];
			switch(flightPhases.get(i).phase) {
			case CRUISE:
				flightPhasesMatrix[i][1] = 0;
				break;
			case LEVEL:
				flightPhasesMatrix[i][1] = 1;
				break;
			case CLIMB:
				flightPhasesMatrix[i][1] = 2;
				break;
			case DESCENT:
				flightPhasesMatrix[i][1] = 3;
				break;
			default:
				flightPhasesMatrix[i][1] = -1;
				break;
			}
		}
		return flightPhasesMatrix;
	}
	
	public double[] getReliability() {
		return reliability;
	}
	public double[] getReliabilityTime() {
		return reliabilityTime;
	}
	
	// flight metrics ...
	public double getReliabilityMetric() {
		if(reliability == null)
			return 0;
		
		double reliabilitySum = 0;
		double sumCount = 0;
		for(int i=0; i<reliability.length; i++) {
			reliabilitySum += reliability[i];
			sumCount++;
		}
		return reliabilitySum/sumCount;
	}

	public double getCompletenessMetric(String airportDatabaseDir) {
		AirportDatabase airportDatabase = AirportDatabase.readInAirportDatabase(airportDatabaseDir);
		
		return getCompletenessMetric(airportDatabase);
	}

	public double getCompletenessMetric(AirportDatabase airportDatabase) {
		if(table.lat == null)
			return -1;
		if(table.lon == null)
			return -1;
		if(table.lat.length < 2)
			return -1;
		if(table.lon.length < 2)
			return -1;
		if(airportDatabase == null)
			return -1;
		
		Airport departure = airportDatabase.getNearestAirportWithElevationInformation(new double[] {table.lat[0]*CosMath.DEG_TO_RAD, table.lon[0]*CosMath.DEG_TO_RAD});
		Airport arrival = airportDatabase.getNearestAirportWithElevationInformation(new double[] {table.lat[table.lat.length-1]*CosMath.DEG_TO_RAD, table.lon[table.lon.length-1]*CosMath.DEG_TO_RAD});
		
		double departureDeltaHeight = table.baroAlt[0] - departure.getElevation();
		double arrivalDeltaHeight = table.baroAlt[table.baroAlt.length-1] - arrival.getElevation();
		
		double maxTrajectoryAltitude = 0;
		for(int i=0; i<table.baroAlt.length; i++)
			if(table.baroAlt[i] > maxTrajectoryAltitude)
				maxTrajectoryAltitude = table.baroAlt[i];
		
		double completenessMetric = 1;
		if(departureDeltaHeight > 0)
			completenessMetric -= 0.5*(departureDeltaHeight/maxTrajectoryAltitude);
		if(arrivalDeltaHeight > 0)
			completenessMetric -= 0.5*(arrivalDeltaHeight/maxTrajectoryAltitude);

		return completenessMetric;
	}
	
	public double getPlausibilityMetric() {
		if(table.lat == null)
			return 0;
		if(table.lon == null)
			return 0;
		if(table.lat.length < 2)
			return 0;
		if(table.lon.length < 2)
			return 0;
		
		double plausibilityMetric = 1.0;
		
		// check baroAlt for plausibility ...
		int maxTrajectoryAltitudeIndex = 0;
		for(int i=0; i<table.baroAlt.length; i++)
			if(table.baroAlt[i] > table.baroAlt[maxTrajectoryAltitudeIndex])
				maxTrajectoryAltitudeIndex = i;
		
		int leftTODborder = maxTrajectoryAltitudeIndex;
		int rightTODborder = maxTrajectoryAltitudeIndex;
		
		double latestMaxBaroAlt = table.baroAlt[0];
		double altitudeJumpSum = 0;
		double currentAltitudeJumpMax = 0;
		for(int i=1; i<leftTODborder; i++) {
			if(table.baroAlt[i] >= latestMaxBaroAlt) {
				altitudeJumpSum += currentAltitudeJumpMax;
				currentAltitudeJumpMax = 0;
				latestMaxBaroAlt = table.baroAlt[i];
				continue;
			}
			if(latestMaxBaroAlt-table.baroAlt[i] > currentAltitudeJumpMax)
				currentAltitudeJumpMax = latestMaxBaroAlt-table.baroAlt[i];
		}
		plausibilityMetric -= altitudeJumpSum/table.baroAlt[maxTrajectoryAltitudeIndex];
		
		double latestMinBaroAlt = table.baroAlt[rightTODborder];
		altitudeJumpSum = 0;
		currentAltitudeJumpMax = 0;
		for(int i=rightTODborder; i<table.baroAlt.length-1; i++) {
			if(table.baroAlt[i] <= latestMinBaroAlt) {
				altitudeJumpSum += currentAltitudeJumpMax;
				currentAltitudeJumpMax = 0;
				latestMinBaroAlt = table.baroAlt[i];
				continue;
			}
			if(table.baroAlt[i]-latestMinBaroAlt > currentAltitudeJumpMax)
				currentAltitudeJumpMax = table.baroAlt[i]-latestMinBaroAlt;
		}
		plausibilityMetric -= altitudeJumpSum/table.baroAlt[maxTrajectoryAltitudeIndex];
		
		if(plausibilityMetric < 0)
			return 0;
		// ... check baroAlt for plausibility


		// check horizontal track for plausibility ...
		double totalTrackDistance = 0;
		for(int i=1; i<table.lat.length; i++)
			totalTrackDistance += CosMath.earthDistEstimatedNM(new double[] {table.lat[i-1]*CosMath.DEG_TO_RAD, table.lon[i-1]*CosMath.DEG_TO_RAD}, new double[] {table.lat[i]*CosMath.DEG_TO_RAD, table.lon[i]*CosMath.DEG_TO_RAD});
		
		double[] posA = new double[2];
		double[] posB = new double[2];
		double[] posC = new double[2];
		double trackAngleChangePositionTwo;
		for(int i=1; i<table.lat.length-1; i++) {
			posA[0] = table.lat[i-1]*CosMath.DEG_TO_RAD;
			posA[1] = table.lon[i-1]*CosMath.DEG_TO_RAD;
			posB[0] = table.lat[i]*CosMath.DEG_TO_RAD;
			posB[1] = table.lon[i]*CosMath.DEG_TO_RAD;
			posC[0] = table.lat[i+1]*CosMath.DEG_TO_RAD;
			posC[1] = table.lon[i+1]*CosMath.DEG_TO_RAD;
			trackAngleChangePositionTwo = CosMath.RAD_TO_DEG*(CosMath.calcOrthodromeTrackAngle(posA, posB, 1)-CosMath.calcOrthodromeTrackAngle(posB, posC, 0));
			while(trackAngleChangePositionTwo > 180)
				trackAngleChangePositionTwo -= 360;
			while(trackAngleChangePositionTwo <= -180)
				trackAngleChangePositionTwo += 360;
			
			if(Math.abs(trackAngleChangePositionTwo) > 30) {
				// Extreme track-angle change detected, track-angle change: trackAngleChangePositionTwo [deg]
				double segmentDistance = CosMath.earthDistEstimatedNM(posA, posB);
				segmentDistance += CosMath.earthDistEstimatedNM(posB, posC);
				double factor = segmentDistance/20;		// avoid wider influence of small segments
				if(factor > 1)
					factor = 1;
				plausibilityMetric -= factor*Math.abs(trackAngleChangePositionTwo)/(90*3);		// maximum: 3 abrupt waypoint-turns with 90 deg track-angle-change
			}
		}
		
		double destinationArrivalDistance = CosMath.earthDistEstimatedNM(new double[] {table.lat[0], table.lon[0]}, new double[] {table.lat[table.lat.length-1], table.lon[table.lon.length-1]});
		if(totalTrackDistance/destinationArrivalDistance > 1.5)
			plausibilityMetric -= totalTrackDistance/destinationArrivalDistance-1.5;
		// ... check horizontal track for plausibility

		if(plausibilityMetric < 0)
			plausibilityMetric = 0;

		return plausibilityMetric;
	}
	// ... flight metrics

	public void setTrajectories(TrajectoryVertical trajectoryVertical, TrajectoryHorizontal trajectoryHorizontal) {
		this.trajectoryVertical = trajectoryVertical;
		this.trajectoryHorizontal = trajectoryHorizontal;
	}
	
	public int mergeTrajectories() {
		if(trajectoryVertical == null)
			return -1;
		if(trajectoryHorizontal == null)
			return -1;
		
		callsign = trajectoryVertical.callsign;
		icao24 = trajectoryVertical.icao24;
		if(!callsign.equals(trajectoryHorizontal.callsign)) {
			callsign = "error";
			System.err.println(String.format("Callsign error: vertical callsign: %s, horizontal callsign: %s", trajectoryVertical.callsign, trajectoryHorizontal.callsign));
		}
		if(!icao24.equals(trajectoryHorizontal.icao24)) {
			icao24 = "error";
			System.err.println(String.format("ICAO24 error: vertical ICAO24: %s, horizontal ICAO24: %s", trajectoryVertical.icao24, trajectoryHorizontal.icao24));
		}
		
		double startTimestamp;
		double endTimestamp;
		
		startTimestamp = determineMergedStartTimestamp(trajectoryVertical, trajectoryHorizontal);
		endTimestamp = determineMergedEndTimestamp(trajectoryVertical, trajectoryHorizontal);
		if((startTimestamp == -1) || (endTimestamp == -1)) {
			return -1;
		}
		
		ArrayList<Double> totalTimestamps = new ArrayList<Double>();
		totalTimestamps.add(startTimestamp);
		totalTimestamps.add(endTimestamp);
		// add required vertical timestamps
		for(int i=0; i<trajectoryVertical.table.vaSample.length; i++)
			if(trajectoryVertical.table.vaSample[i] && !trajectoryVertical.table.vrSample[i]) {
				if(trajectoryVertical.table.time[i] <= startTimestamp)
					continue;
				if(trajectoryVertical.table.time[i] >= endTimestamp)
					break;
				if(!totalTimestamps.contains(trajectoryVertical.table.time[i]))
					totalTimestamps.add(trajectoryVertical.table.time[i]);
			}
		// add required horizontal timestamps
		for(int i=0; i<trajectoryHorizontal.table.vaSample.length; i++)
			if(trajectoryHorizontal.table.vaSample[i] && !trajectoryHorizontal.table.vrSample[i]) {
				if(trajectoryHorizontal.table.time[i] <= startTimestamp)
					continue;
				if(trajectoryHorizontal.table.time[i] >= endTimestamp)
					break;
				if(!totalTimestamps.contains(trajectoryHorizontal.table.time[i]))
					totalTimestamps.add(trajectoryHorizontal.table.time[i]);
			}
		Collections.sort(totalTimestamps);
		
		table.allocateArrayMemory(totalTimestamps.size());
		for(int i=0; i<totalTimestamps.size(); i++)
			table.time[i] = totalTimestamps.get(i);
		
		fillAndInterpolateChannels();
		
		fillAndAdaptFlightPhases();
		
		fillAndCombineReliability();
		
		return 0;
	}
	private void fillAndCombineReliability() {
		double timeStep = 5;

		double firstTimestamp = table.time[0];
		double lastTimestamp = table.time[table.time.length-1];
		int size;
		if((lastTimestamp-firstTimestamp)/timeStep > Math.round((lastTimestamp-firstTimestamp)/timeStep))
			size = (int)Math.round((lastTimestamp-firstTimestamp)/timeStep)+2;
		else
			size = (int)Math.round((lastTimestamp-firstTimestamp)/timeStep)+1;
		
		reliability = new double[size];
		reliabilityTime = new double[size];
		for(int i=0; i<size; i++)
			reliabilityTime[i] = firstTimestamp+i*timeStep;
		reliabilityTime[size-1] = lastTimestamp;
		
		double reliabilityVertical;
		double reliabilityHorizontal;
		for(int i=0; i<size; i++) {
			reliabilityVertical = trajectoryVertical.interpolateReliability(reliabilityTime[i]);
			reliabilityHorizontal = trajectoryHorizontal.interpolateReliability(reliabilityTime[i]);
			
			if(reliabilityVertical < reliabilityHorizontal)
				reliability[i] = reliabilityVertical;
			else
				reliability[i] = reliabilityHorizontal;
		}
	}
	private void fillAndAdaptFlightPhases() {
		double startTimestamp = table.time[0];
		double endTimestamp = table.time[table.time.length-1];
		
		// clone verticalFlightPhases ...
		for(int i=0; i<trajectoryVertical.verticalFlightPhases.size(); i++)
			flightPhases.add(trajectoryVertical.verticalFlightPhases.get(i).clone());
		// ... clone verticalFlightPhases
		
		// translate time indices of VerticalFlightPhase to merged time indices
		int newIndex;
		int i = 0;
		while(i<flightPhases.size()) {
			if(trajectoryVertical.table.time[flightPhases.get(i).startIndex] >= endTimestamp) {
				flightPhases.remove(i);
				continue;
			}
			if(trajectoryVertical.table.time[flightPhases.get(i).startIndex] < startTimestamp) {
				double phaseEndTimestamp;
				if(i+1 < flightPhases.size())
					phaseEndTimestamp = trajectoryVertical.table.time[flightPhases.get(i+1).startIndex];
				else
					phaseEndTimestamp = endTimestamp;
				if(phaseEndTimestamp <= startTimestamp)
					flightPhases.remove(i);
				else {
					flightPhases.get(i).startIndex = 0;
					i++;
				}
				continue;
			}
			
			newIndex = determineTimestampIndex(trajectoryVertical.table.time[flightPhases.get(i).startIndex]);
			if(newIndex == -1) {
				// VerticalFlightPhases-timestamps should be within TrajectoryMerged.table.time at this step in any case
				// Error-handling: Fatal error regarding flight-phases
				System.err.println(String.format("Fatal Error - %s: VerticalFlightPhase-timestamp not within time-channel of merged Trajectory. \nVerticalFlightPhases will not be available. ", callsign));
				flightPhases.clear();
				return;
			}
			flightPhases.get(i).startIndex = newIndex;
			
			i++;
		}
	}
	private int determineTimestampIndex(double timestamp) {
		for(int i=0; i<table.time.length; i++)
			if(table.time[i] == timestamp)
				return i;
		return -1;
	}
	private void fillAndInterpolateChannels() {
		double timestamp;
		double[] pos;
		for(int i=0; i<table.time.length; i++) {
			timestamp = table.time[i];
			table.baroAlt[i] = trajectoryVertical.interpolateBaroAlt(timestamp);
			pos = trajectoryHorizontal.interpolatePos(timestamp);
			if(pos == null)
				pos = new double[] {Double.MAX_VALUE, Double.MAX_VALUE};
			table.lat[i] = pos[0];
			table.lon[i] = pos[1];
		}
	}
	private double determineMergedStartTimestamp(TrajectoryVertical trajectoryVertical, TrajectoryHorizontal trajectoryHorizontal) {
		double startTimestamp;
		
		// get trajectoryVertical startTime ...
		int index = -1;
		for(int i=0; i<trajectoryVertical.table.vaSample.length; i++)
			if(trajectoryVertical.table.vaSample[i] && !trajectoryVertical.table.vrSample[i]) {
				index = i;
				break;
			}
		if(index == -1)
			return -1;
		startTimestamp = trajectoryVertical.table.time[index];
		// ... get trajectoryVertical startTime

		// get trajectoryHorizontal startTime ...
		index = -1;
		for(int i=0; i<trajectoryHorizontal.table.vaSample.length; i++)
			if(trajectoryHorizontal.table.vaSample[i] && !trajectoryHorizontal.table.vrSample[i]) {
				index = i;
				break;
			}
		if(index == -1)
			return -1;
		if(trajectoryHorizontal.table.time[index] > startTimestamp)
			startTimestamp = trajectoryHorizontal.table.time[index];
		// ... get trajectoryHorizontal startTime
		
		return startTimestamp;
	}
	private double determineMergedEndTimestamp(TrajectoryVertical trajectoryVertical, TrajectoryHorizontal trajectoryHorizontal) {
		double endTimestamp;
		
		// get trajectoryVertical endTime ...
		int index = -1;
		for(int i=trajectoryVertical.table.vaSample.length-1; i>=0; i--)
			if(trajectoryVertical.table.vaSample[i] && !trajectoryVertical.table.vrSample[i]) {
				index = i;
				break;
			}
		if(index == -1)
			return -1;
		endTimestamp = trajectoryVertical.table.time[index];
		// ... get trajectoryVertical endTime

		// get trajectoryHorizontal endTime ...
		index = -1;
		for(int i=trajectoryHorizontal.table.vaSample.length-1; i>=0; i--)
			if(trajectoryHorizontal.table.vaSample[i] && !trajectoryHorizontal.table.vrSample[i]) {
				index = i;
				break;
			}
		if(index == -1)
			return -1;
		if(trajectoryHorizontal.table.time[index] < endTimestamp)
			endTimestamp = trajectoryHorizontal.table.time[index];
		// ... get trajectoryHorizontal endTime
		
		return endTimestamp;
	}

}
