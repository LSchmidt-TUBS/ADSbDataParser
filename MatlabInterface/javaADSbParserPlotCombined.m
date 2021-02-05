function javaADSbParserPlotCombined(parsedTrajectory, interpolationTimeStep)
% Function to plot raw and parsed (merged) trajectory from ADSbDataParser
% parsedTrajectory (struct): parsed trajectory from ADSbDataParser
% interpolationTimeStep (double): [OPTIONAL VALUE] If set, parsed trajectory will be interpolated with this value as max. time-step
% In most cases (with default settings - see below), regarding the plot-visualization, the interpolation only effects the horizontal path. 


% Indication whether to plot horizontal and/or vertical plane: 
	plotRawAndMergedHori = true;
	plotRawAndMergedVert = true;

% Indicate whether also to use timestamps of merged samples for interpolation:
	alsoUseMergedTimestampsForInterpolation = true;


	interpolationOn = false;
	if(nargin == 2)
		interpolationOn = true;
		interpolationTimestamps = (parsedTrajectory.time(1):interpolationTimeStep:parsedTrajectory.time(end))';
		if(alsoUseMergedTimestampsForInterpolation)
			interpolationTimestamps = [interpolationTimestamps; parsedTrajectory.time];
			interpolationTimestamps = unique(interpolationTimestamps);
			interpolationTimestamps = sortrows(interpolationTimestamps);
		end
		interpolatedStates = javaADSbParserInterpolate(parsedTrajectory, interpolationTimestamps);
	end

	callsign = parsedTrajectory.callsign;
	icao24 = parsedTrajectory.icao24;

	timeVertRaw = parsedTrajectory.raw.timeVert;
	baroAltRaw = parsedTrajectory.raw.baroAlt;

	timeHoriRaw = parsedTrajectory.raw.timeHori;
	latRaw = parsedTrajectory.raw.lat;
	lonRaw = parsedTrajectory.raw.lon;


	latMerged = parsedTrajectory.lat;
	lonMerged = parsedTrajectory.lon;

	timeMerged = parsedTrajectory.time;
	baroAltMerged = parsedTrajectory.baroAlt;

	flightPhasesMerged = parsedTrajectory.flightPhases;


	reliabilityMetric = parsedTrajectory.metrics.reliability;
	completenessMetric = parsedTrajectory.metrics.completeness;
	plausibilityMetric = parsedTrajectory.metrics.plausibility;


% PLOT TRAJECTORY HORIZONTAL
	if(plotRawAndMergedHori)
		if(~isempty(lonRaw))
			figure;
			rawHandle = plot(lonRaw, latRaw, 'c.-');
			hold on;
			plot(lonRaw(1), latRaw(1), 'ro');
			plot(lonRaw(end), latRaw(end), 'b+');

			mergedHandle = [];
			if(~isempty(lonMerged))
				if(interpolationOn)
					% with interpolation:
					mergedHandle = plot(interpolatedStates.lon, interpolatedStates.lat, 'k-');
				else
					% without interpolation:
					mergedHandle = plot(lonMerged, latMerged, 'k-');
				end
				hold on;
				plot(lonMerged, latMerged, 'k.');
				plot(lonMerged(1), latMerged(1), 'ro');
				plot(lonMerged(end), latMerged(end), 'b+');
			end

			xlabel('lon / deg');
			ylabel('lat / deg');
			if(~isempty(mergedHandle))
				legend([rawHandle mergedHandle], {'raw', 'parsed'});
			else
				legend([rawHandle], {'raw'});
			end
			title(sprintf("geographic position - %s (ICAO24: %s)", callsign, icao24));
			axis equal;
		end
	end

% PLOT TRAJECTORY VERTICAL
	if(plotRawAndMergedVert)
		if(~isempty(timeVertRaw))
			figure;
			rawHandle = plot(timeVertRaw-timeVertRaw(1), baroAltRaw, 'c.-');

			mergedHandle = [];
			if(~isempty(timeMerged))
				hold on;
				if(interpolationOn)
					% with interpolation:
					mergedHandle = plot(interpolatedStates.time-timeVertRaw(1), interpolatedStates.baroAlt, 'k-');
				else
					% without interpolation:
					mergedHandle = plot(timeMerged-timeVertRaw(1), baroAltMerged, 'k-');
				end
				plot(timeMerged-timeVertRaw(1), baroAltMerged, 'k.');

				% Plot flight phases
				if(~isempty(flightPhasesMerged))
					for i=1:1:length(flightPhasesMerged(:,1))
						phaseStartTime = flightPhasesMerged(i,1);
						if(i >= length(flightPhasesMerged(:,1)))
							phaseEndTime = timeMerged(length(timeMerged));
						else
							phaseEndTime = flightPhasesMerged(i+1,1);
						end
						if(flightPhasesMerged(i,2) == 0)
							objectHandle = patch([phaseStartTime-timeVertRaw(1) phaseEndTime-timeVertRaw(1) phaseEndTime-timeVertRaw(1) phaseStartTime-timeVertRaw(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'b', 'DisplayName', 'cruise', 'FaceAlpha', 0.2);
							set(objectHandle, 'EdgeColor', 'none');
						end
						if(flightPhasesMerged(i,2) == 1)
							objectHandle = patch([phaseStartTime-timeVertRaw(1) phaseEndTime-timeVertRaw(1) phaseEndTime-timeVertRaw(1) phaseStartTime-timeVertRaw(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'g', 'DisplayName', 'level', 'FaceAlpha', 0.2);
							set(objectHandle, 'EdgeColor', 'none');
						end
						if(flightPhasesMerged(i,2) == 2)
							objectHandle = patch([phaseStartTime-timeVertRaw(1) phaseEndTime-timeVertRaw(1) phaseEndTime-timeVertRaw(1) phaseStartTime-timeVertRaw(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'y', 'DisplayName', 'climb', 'FaceAlpha', 0.2);
							set(objectHandle, 'EdgeColor', 'none');
						end
						if(flightPhasesMerged(i,2) == 3)
							objectHandle = patch([phaseStartTime-timeVertRaw(1) phaseEndTime-timeVertRaw(1) phaseEndTime-timeVertRaw(1) phaseStartTime-timeVertRaw(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'm', 'DisplayName', 'descent', 'FaceAlpha', 0.2);
							set(objectHandle, 'EdgeColor', 'none');
						end
						if(flightPhasesMerged(i,2) == -1)
							objectHandle = patch([phaseStartTime-timeVertRaw(1) phaseEndTime-timeVertRaw(1) phaseEndTime-timeVertRaw(1) phaseStartTime-timeVertRaw(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'k', 'DisplayName', 'undefined', 'FaceAlpha', 0.2);
							set(objectHandle, 'EdgeColor', 'none');
						end
					end
				end
			end

			xlabel('time / sec');
			ylabel('barometric altitude / ft');
			if(~isempty(mergedHandle))
				legend([rawHandle mergedHandle], {'raw', 'parsed'});
			else
				legend([rawHandle], {'raw'});
			end
			title(sprintf("altitude - %s (ICAO24: %s)", callsign, icao24));
		end
	end

end
