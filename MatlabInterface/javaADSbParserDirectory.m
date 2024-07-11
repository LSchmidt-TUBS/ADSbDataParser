function parsedTrajectories = javaADSbParserDirectory(directoryDir)
% Function with interface to java ADSbDataParser to parse all ADS-B trajectories with initial format state-vectors-data4, which are contained within a certain directory, in parallel mode
% directoryDir (String): directory to folder containing files of trajectories with format state-vectors-data4 to be parsed (all files within this directory will be parsed)
% return value: parsedTrajectories (struct[]): struct-vector containing the raw trajectories (version used by the parser; NOT initial state-vectors-data4 trajectory) and parsed trajectories
% CAUTION: In case of an fatal error during parsing a trajectory, the corresponding trajectory will be skipped in the return vector (there will not be an empty field to indicate the error)
% e.g.: parsedTrajectories = javaADSbParserDirectory('./impalaFiles/');


	directoryContent = dir(directoryDir);

	fileDirs = [];
	for i=1:1:length(directoryContent)
		if(directoryContent(i).isdir)
			continue;
		end
		if(~endsWith(string(directoryContent(i).name), ".log"))
			if(~endsWith(string(directoryContent(i).name), ".txt"))
				continue;
			end
		end
		fileDirs = [fileDirs; sprintf("%s\\%s", directoryContent(i).folder, directoryContent(i).name)];
	end

	parsedTrajectories = javaADSbParserParallelMultiple(fileDirs);

end
