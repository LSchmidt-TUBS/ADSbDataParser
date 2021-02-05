function parsedTrajectories = javaADSbParserParallelMultiple(fileDirs)
% Function with interface to java ADSbDataParser to parse multiple ADS-B trajectories with initial format state-vectors-data4 in parallel mode
% In case of huge numbers of trajectories to parse, this function is to be prefered to the direct function-call of javaADSbParserParallel. 
% fileDirs (String[]): file directories to input text files containing state-vectors-data4 trajectories
% return value: parsedTrajectories (struct[]): struct-vector containing the raw trajectories (version used by the parser; NOT initial state-vectors-data4 trajectory) and parsed trajectories
% CAUTION: In case of an fatal error during parsing a trajectory, the corresponding trajectory will be skipped in the return vector (there will not be an empty field to indicate the error)
% e.g.: parsedTrajectories = javaADSbParserParallelMultiple(["./impalaFiles/Flight_A.log"; "./impalaFiles/Flight_B.log"; "./impalaFiles/Flight_C.log"]);


% Number of trajectories passed to the data parser at once for parsing. In case of issues (e.g. performance) this value may be adapted/ reduced: 
	STEP_TRAJECTORY_COUNT = 20;

	parsedTrajectories = [];
	for i=1:STEP_TRAJECTORY_COUNT:length(fileDirs)
		fileDirsEndIndex = i+STEP_TRAJECTORY_COUNT-1;
		if(fileDirsEndIndex > length(fileDirs))
			fileDirsEndIndex = length(fileDirs);
		end
		currentFileDirs = fileDirs(i:fileDirsEndIndex);
		currentParsedTrajectories = javaADSbParserParallel(currentFileDirs);

		parsedTrajectories = [parsedTrajectories; currentParsedTrajectories];

	%	clc;
		fprintf("progress: %.4f %%\r", fileDirsEndIndex/length(fileDirs)*100);
		drawnow;
	end

end
