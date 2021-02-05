function interpolatedStates = javaADSbParserInterpolate(parsedTrajectory, timestamps)
% Function with interface to java ADSbDataParser to interpolate aircraft states (orthodrome interpolation in horizontal plane)
% parsedTrajectory (struct): parsed trajectory from ADSbDataParser
% timestamps (double[]): Vector containing the timestamps to interpolate aircraft states
% return value: interpolatedStates (struct): struct containing interpolated aircraft states (values become NaN if timestamps can't be interpolated)


% Java directory to ADSbDataParser: 
	javaaddpath("./../ADSbDataParser/bin");

	java_CosMath = javaObject("de.tu_bs.iff.adsb.dataparser.cosmath.CosMath");

	if(length(timestamps(1,:)) ~= 1)
		timestamps = timestamps';
	end
	if(length(timestamps(1,:)) ~= 1)
		error('ERROR: Input (timestamps) is required to be a Vector');
	end

	% sort timestamps ...
	indexVector = (1:1:length(timestamps))';		% index vector as reference for re-arranging the results according to request
	timestamps = [timestamps, indexVector];
	timestamps = sortrows(timestamps, 1);
	% ... sort timestamps

	leftIndex = 1;
	statesVector = [];
	partInitValues = [];
	partInitValuesLeftIndex = -1;
	for i=1:1:length(timestamps(:,1))
		timestamp = timestamps(i,1);
		lat = NaN;
		lon = NaN;
		baroAlt = NaN;
		reliability = NaN;

		if((timestamp < parsedTrajectory.time(1)) || (leftIndex == -1))
			statesVector = [statesVector; timestamp, lat, lon, baroAlt, reliability];
			continue;
		end
		while(~( (parsedTrajectory.time(leftIndex)<=timestamp) && (parsedTrajectory.time(leftIndex+1)>=timestamp) ))
			leftIndex = leftIndex+1;
			if(leftIndex+1 > length(parsedTrajectory.time))
				leftIndex = -1;
				break;
			end
		end
		if(leftIndex == -1)
			statesVector = [statesVector; timestamp, lat, lon, baroAlt, reliability];
			continue;
		end

		if(partInitValuesLeftIndex ~= leftIndex)
			% orthodromeInterpolatePartInit only needs to be called once
			% per orthodrome position pair (posA, posB)
			posA = [parsedTrajectory.lat(leftIndex)/180*pi(); parsedTrajectory.lon(leftIndex)/180*pi()];
			posB = [parsedTrajectory.lat(leftIndex+1)/180*pi(); parsedTrajectory.lon(leftIndex+1)/180*pi()];
			partInitValues = javaMethod("orthodromeInterpolatePartInit", java_CosMath, posA, posB);
			partInitValuesLeftIndex = leftIndex;
		end
		relativePosition = (timestamp-parsedTrajectory.time(leftIndex)) / (parsedTrajectory.time(leftIndex+1)-parsedTrajectory.time(leftIndex));
		pos = javaMethod("orthodromeInterpolatePartCalc", java_CosMath, partInitValues, relativePosition);
		lat = pos(1)/pi()*180;
		lon = pos(2)/pi()*180;

		baroAlt = interp1([parsedTrajectory.time(leftIndex); parsedTrajectory.time(leftIndex+1)], [parsedTrajectory.baroAlt(leftIndex); parsedTrajectory.baroAlt(leftIndex+1)], timestamp);
		
		if(~isempty(parsedTrajectory.reliabilityTime))
			reliability = interp1(parsedTrajectory.reliabilityTime, parsedTrajectory.reliability, timestamp);
		end

		statesVector = [statesVector; timestamp, lat, lon, baroAlt, reliability];
	end

	% re-arranging the results according to request ...
	statesVector = [timestamps(:,2), statesVector];
	statesVector = sortrows(statesVector, 1);
	% ... re-arranging the results according to request
	
	interpolatedStates = struct('time', statesVector(:,2), 'lat', statesVector(:,3), 'lon', statesVector(:,4), 'baroAlt', statesVector(:,5), 'reliability', statesVector(:,6));

end
