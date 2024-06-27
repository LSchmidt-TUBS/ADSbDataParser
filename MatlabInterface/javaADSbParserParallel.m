function parsedTrajectories = javaADSbParserParallel(fileDirs)
% Function with interface to java ADSbDataParser to parse multiple ADS-B trajectories with initial format state-vectors-data4 in parallel mode
% In case of huge numbers of trajectories to parse, the function javaADSbParserParallelMultiple is to be prefered to the direct function-call of javaADSbParserParallel. 
% fileDirs (String[]): file directories to input text files containing state-vectors-data4 trajectories
% return value: parsedTrajectories (struct[]): struct-vector containing the raw trajectories (version used by the parser; NOT initial state-vectors-data4 trajectory) and parsed trajectories
% CAUTION: In case of an fatal error during parsing a trajectory, the corresponding trajectory will be skipped in the return vector (there will not be an empty field to indicate the error)
% e.g.: parsedTrajectories = javaADSbParserParallel(["./impalaFiles/Flight_A.log"; "./impalaFiles/Flight_B.log"; "./impalaFiles/Flight_C.log"]);


% Java directory to ADSbDataParser: 
	javaaddpath("./../ADSbDataParser/bin");

% Directory to the airport database for completeness metric ("NONE" if no database available): 
	AIRPORT_DATABASE_FILE_DIR = "NONE";
	%AIRPORT_DATABASE_FILE_DIR = "./airportDatabase.attapt";

% Number of threads to be used (may be adapted): 
	%THREAD_COUNT = 8;
	THREAD_COUNT = feature('numcores')+1;

% Setting for filtering of redundant samples (true: ON / false: OFF)
	FILTER_REDUNDANT_SAMPLES = true;


	tic;

	java_ParallelParser = javaObject("de.tu_bs.iff.adsb.dataparser.parallel.ParallelParser");

	if(AIRPORT_DATABASE_FILE_DIR ~= "NONE")
		javaMethod("setAirportDatabaseDir", java_ParallelParser, AIRPORT_DATABASE_FILE_DIR);
	else
		disp('No AIRPORT_DATABASE_FILE_DIR set. Will continue without airport database. ');
	end
	javaMethod("setDirs", java_ParallelParser, fileDirs);
	javaMethod("parseAll", java_ParallelParser, THREAD_COUNT, FILTER_REDUNDANT_SAMPLES);

	parsedTrajectories = [];
	for i=1:1:length(fileDirs)
		errorCode = javaMethod("getErrorCode", java_ParallelParser, i-1);

		if(errorCode < 0)
			continue;
		end

		java_TrajectoryVertical = javaMethod("getTrajectoryVertical", java_ParallelParser, i-1);
		java_TrajectoryHorizontal = javaMethod("getTrajectoryHorizontal", java_ParallelParser, i-1);
		java_TrajectoryMerged = javaMethod("getTrajectoryMerged", java_ParallelParser, i-1);

		timeVertRaw = javaMethod("getTimeRaw", java_TrajectoryVertical);
		baroAltRaw = javaMethod("getBaroAltRaw", java_TrajectoryVertical);
		samplingTimeVert = javaMethod("getSamplingTime", java_TrajectoryVertical);
		timeHoriRaw = javaMethod("getTimeRaw", java_TrajectoryHorizontal);
		latRaw = javaMethod("getLatRaw", java_TrajectoryHorizontal);
		lonRaw = javaMethod("getLonRaw", java_TrajectoryHorizontal);
		samplingTimeHori = javaMethod("getSamplingTime", java_TrajectoryHorizontal);

		callsign = javaMethod("getCallsign", java_TrajectoryMerged);
		icao24 = javaMethod("getIcao24", java_TrajectoryMerged);
		timeMerged = javaMethod("getTime", java_TrajectoryMerged);
		latMerged = javaMethod("getLat", java_TrajectoryMerged);
		lonMerged = javaMethod("getLon", java_TrajectoryMerged);
		baroAltMerged = javaMethod("getBaroAlt", java_TrajectoryMerged);
		flightPhasesMerged = javaMethod("getFlightPhases", java_TrajectoryMerged);
		reliabilityTimeMerged = javaMethod("getReliabilityTime", java_TrajectoryMerged);
		reliabilityMerged = javaMethod("getReliability", java_TrajectoryMerged);

		reliabilityMetric = javaMethod("getReliabilityMetric", java_ParallelParser, i-1);
		completenessMetric = javaMethod("getCompletenessMetric", java_ParallelParser, i-1);
		plausibilityMetric = javaMethod("getPlausibilityMetric", java_ParallelParser, i-1);

		rawTrajectory = struct('timeVert', timeVertRaw, 'baroAlt', baroAltRaw, 'timeHori', timeHoriRaw, 'lat', latRaw, 'lon', lonRaw);
		samplingTime = struct('samplingTimeVert', samplingTimeVert, 'samplingTimeHori', samplingTimeHori);
		parsedTrajectory = struct('callsign', callsign, 'icao24', icao24, 'time', timeMerged, 'lat', latMerged, 'lon', lonMerged, 'baroAlt', baroAltMerged, 'redundancyFiltered', FILTER_REDUNDANT_SAMPLES, 'flightPhases', flightPhasesMerged, 'samplingTime', samplingTime, 'reliabilityTime', reliabilityTimeMerged, 'reliability', reliabilityMerged, 'metrics', struct('reliability', reliabilityMetric, 'completeness', completenessMetric, 'plausibility', plausibilityMetric), 'raw', rawTrajectory);

		parsedTrajectories = [parsedTrajectories; parsedTrajectory];
	end
	drawnow;

	toc;

end
