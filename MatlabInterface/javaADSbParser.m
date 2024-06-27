function parsedTrajectory = javaADSbParser(fileDir)
% Function with interface to java ADSbDataParser to parse ADS-B trajectories with initial format state-vectors-data4
% fileDir (String): file directory to input text file containing state-vectors-data4 trajectory
% return value: parsedTrajectory (struct): struct containing the raw trajectory (version used by the parser; NOT initial state-vectors-data4 trajectory) and parsed trajectory; in case of an fatal error the return value is empty ([])
% e.g.: parsedTrajectory = javaADSbParser("./impalaFiles/Flight_A.log");


% Java directory to ADSbDataParser: 
	javaaddpath("./../ADSbDataParser/bin");

% Directory to the airport database for completeness metric ("NONE" if no database available): 
	AIRPORT_DATABASE_FILE_DIR = "NONE";
	%AIRPORT_DATABASE_FILE_DIR = "./airportDatabase.attapt";
	
% Setting for filtering of redundant samples (true: ON / false: OFF)
	FILTER_REDUNDANT_SAMPLES = true;


	java_TrajectoryStateVectorsData4 = javaObject("de.tu_bs.iff.adsb.dataparser.lib.TrajectoryStateVectorsData4");		% variable containing the java object of class TrajectoryStateVectorsData4
	errorCode = javaMethod("readInInterfaceDataFile", java_TrajectoryStateVectorsData4, fileDir);		% read in data from text file
	if(errorCode < 0)
		disp('Error while read-in trajectory data from file');
		parsedTrajectory = [];
		return;
	end

% VERTICAL PROFILE ...
	java_TrajectoryVertical = javaObject("de.tu_bs.iff.adsb.dataparser.lib.TrajectoryVertical");		% variable containing the java object of class TrajectoryVertical
	errorCode = javaMethod("setRawTrajectoryFromStateVectorsData4", java_TrajectoryVertical, java_TrajectoryStateVectorsData4);
	if(errorCode < 0)
		disp('Error while setting raw vertical trajectory');
		parsedTrajectory = [];
		return;
	end
	errorCode = javaMethod("parseTrajectory", java_TrajectoryVertical, FILTER_REDUNDANT_SAMPLES);			% parse vertical trajectory
	if(errorCode < 0)
		disp('Error while parsing vertical trajectory');
		parsedTrajectory = [];
		return;
	end

	% get raw trajectory from data-parser (java interface): 
	timeVertRaw = javaMethod("getTimeRaw", java_TrajectoryVertical);
	baroAltRaw = javaMethod("getBaroAltRaw", java_TrajectoryVertical);

	samplingTimeVert = javaMethod("getSamplingTime", java_TrajectoryVertical);
% ... VERTICAL PROFILE

% HORIZONTAL PROFILE ...
	java_TrajectoryHorizontal = javaObject("de.tu_bs.iff.adsb.dataparser.lib.TrajectoryHorizontal");		% variable containing the java object of class TrajectoryHorizontal
	errorCode = javaMethod("setRawTrajectoryFromStateVectorsData4", java_TrajectoryHorizontal, java_TrajectoryStateVectorsData4);
	if(errorCode < 0)
		disp('Error while setting raw horizontal trajectory');
		parsedTrajectory = [];
		return;
	end
	errorCode = javaMethod("parseTrajectory", java_TrajectoryHorizontal, FILTER_REDUNDANT_SAMPLES);			% parse horizontal trajectory
	if(errorCode < 0)
		disp('Error while parsing horizontal trajectory');
		parsedTrajectory = [];
		return;
	end

	% get raw trajectory from data-parser (java interface): 
	timeHoriRaw = javaMethod("getTimeRaw", java_TrajectoryHorizontal);
	latRaw = javaMethod("getLatRaw", java_TrajectoryHorizontal);
	lonRaw = javaMethod("getLonRaw", java_TrajectoryHorizontal);

	samplingTimeHori = javaMethod("getSamplingTime", java_TrajectoryHorizontal);
% ... HORIZONTAL PROFILE

% MERGED TRAJECTORY ...
	java_TrajectoryMerged = javaObject("de.tu_bs.iff.adsb.dataparser.lib.TrajectoryMerged");			% variable containing the java object of class TrajectoryMerged
	javaMethod("setTrajectories", java_TrajectoryMerged, java_TrajectoryVertical, java_TrajectoryHorizontal);
	errorCode = javaMethod("mergeTrajectories", java_TrajectoryMerged);
	if(errorCode < 0)
		disp('Error while merging horizontal and vertical path');
		parsedTrajectory = [];
		return;
	end

	% get merged trajectory from data-parser (java interface): 
	callsign = javaMethod("getCallsign", java_TrajectoryMerged);
	icao24 = javaMethod("getIcao24", java_TrajectoryMerged);

	timeMerged = javaMethod("getTime", java_TrajectoryMerged);
	latMerged = javaMethod("getLat", java_TrajectoryMerged);
	lonMerged = javaMethod("getLon", java_TrajectoryMerged);
	baroAltMerged = javaMethod("getBaroAlt", java_TrajectoryMerged);

	flightPhasesMerged = javaMethod("getFlightPhases", java_TrajectoryMerged);
	reliabilityTimeMerged = javaMethod("getReliabilityTime", java_TrajectoryMerged);
	reliabilityMerged = javaMethod("getReliability", java_TrajectoryMerged);

	reliabilityMetric = javaMethod("getReliabilityMetric", java_TrajectoryMerged);

	if(AIRPORT_DATABASE_FILE_DIR ~= "NONE")
		completenessMetric = javaMethod("getCompletenessMetric", java_TrajectoryMerged, AIRPORT_DATABASE_FILE_DIR);
	else
		disp('No AIRPORT_DATABASE_FILE_DIR set. Will continue without airport database. ');
		completenessMetric = -1;
	end

	plausibilityMetric = javaMethod("getPlausibilityMetric", java_TrajectoryMerged);
% ... MERGED TRAJECTORY

	rawTrajectory = struct('timeVert', timeVertRaw, 'baroAlt', baroAltRaw, 'timeHori', timeHoriRaw, 'lat', latRaw, 'lon', lonRaw);
	samplingTime = struct('samplingTimeVert', samplingTimeVert, 'samplingTimeHori', samplingTimeHori);
	parsedTrajectory = struct('callsign', callsign, 'icao24', icao24, 'time', timeMerged, 'lat', latMerged, 'lon', lonMerged, 'baroAlt', baroAltMerged, 'redundancyFiltered', FILTER_REDUNDANT_SAMPLES, 'flightPhases', flightPhasesMerged, 'samplingTime', samplingTime, 'reliabilityTime', reliabilityTimeMerged, 'reliability', reliabilityMerged, 'metrics', struct('reliability', reliabilityMetric, 'completeness', completenessMetric, 'plausibility', plausibilityMetric), 'raw', rawTrajectory);
end
