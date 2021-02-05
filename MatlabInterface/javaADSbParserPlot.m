function javaADSbParserPlot(parsedTrajectory, interpolationTimeStep)
% Function to plot parsed (merged) trajectory from ADSbDataParser with option to plot reliability channel
% parsedTrajectory (struct): parsed trajectory from ADSbDataParser
% interpolationTimeStep (double): [OPTIONAL VALUE] If set, parsed trajectory will be interpolated with this value as max. time-step
% In most cases (with default settings - see below), regarding the plot-visualization, the interpolation only effects the horizontal path. 


% Indication what to plot: 
	PlotMergedHori = true;
	plotMergedVert = true;
	plotMergedReliability = true;
	plotMergedVertAndMergedRelaibility = true;

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

	latMerged = parsedTrajectory.lat;
	lonMerged = parsedTrajectory.lon;

	timeMerged = parsedTrajectory.time;
	baroAltMerged = parsedTrajectory.baroAlt;

	flightPhasesMerged = parsedTrajectory.flightPhases;
	reliabilityMerged = parsedTrajectory.reliability;

	reliabilityTimeMerged = parsedTrajectory.reliabilityTime;

	reliabilityMetric = parsedTrajectory.metrics.reliability;
	completenessMetric = parsedTrajectory.metrics.completeness;
	plausibilityMetric = parsedTrajectory.metrics.plausibility;

% PLOT MERGED TRAJECTORY HORIZONTAL
	if(PlotMergedHori)
		if(~isempty(lonMerged))
			figure;
			if(interpolationOn)
				% with interpolation:
				plot(interpolatedStates.lon, interpolatedStates.lat, 'k-');
			else
				% without interpolation:
				plot(lonMerged, latMerged, 'k-');
			end
			hold on;
			plot(lonMerged, latMerged, 'k.');
			plot(lonMerged(1), latMerged(1), 'ro');
			plot(lonMerged(end), latMerged(end), 'b+');
			xlabel('lon / deg');
			ylabel('lat / deg');
			title(sprintf("parsed geographic position - %s (ICAO24: %s)", callsign, icao24));
			axis equal;
		end
	end

% PLOT MERGED TRAJECTORY VERTICAL
	if(plotMergedVert)
		if(~isempty(timeMerged))
			figure;
			if(interpolationOn)
				% with interpolation:
				plot(interpolatedStates.time-timeMerged(1), interpolatedStates.baroAlt, 'k-');
			else
				% without interpolation:
				plot(timeMerged-timeMerged(1), baroAltMerged, 'k-');
			end
			hold on;
			plot(timeMerged-timeMerged(1), baroAltMerged, 'k.');

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
						objectHandle = patch([phaseStartTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseStartTime-timeMerged(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'b', 'DisplayName', 'cruise', 'FaceAlpha', 0.2);
						set(objectHandle, 'EdgeColor', 'none');
					end
					if(flightPhasesMerged(i,2) == 1)
						objectHandle = patch([phaseStartTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseStartTime-timeMerged(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'g', 'DisplayName', 'level', 'FaceAlpha', 0.2);
						set(objectHandle, 'EdgeColor', 'none');
					end
					if(flightPhasesMerged(i,2) == 2)
						objectHandle = patch([phaseStartTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseStartTime-timeMerged(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'y', 'DisplayName', 'climb', 'FaceAlpha', 0.2);
						set(objectHandle, 'EdgeColor', 'none');
					end
					if(flightPhasesMerged(i,2) == 3)
						objectHandle = patch([phaseStartTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseStartTime-timeMerged(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'm', 'DisplayName', 'descent', 'FaceAlpha', 0.2);
						set(objectHandle, 'EdgeColor', 'none');
					end
					if(flightPhasesMerged(i,2) == -1)
						objectHandle = patch([phaseStartTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseStartTime-timeMerged(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'k', 'DisplayName', 'undefined', 'FaceAlpha', 0.2);
						set(objectHandle, 'EdgeColor', 'none');
					end
				end
			end

			xlabel('time / sec');
			ylabel('barometric altitude / ft');
			title(sprintf("parsed altitude - %s (ICAO24: %s)", callsign, icao24));
		end
	end

% PLOT RELIABILITY
	if(plotMergedReliability)
		if(~isempty(reliabilityTimeMerged))
			figure;
			if(interpolationOn)
				% with interpolation:
				plot(interpolatedStates.time-reliabilityTimeMerged(1), interpolatedStates.reliability, 'm-');
			else
				% without interpolation:
				plot(reliabilityTimeMerged-reliabilityTimeMerged(1), reliabilityMerged, 'm-');
			end
			xlabel('time / sec');
			ylabel('reliability / 1');
			title(sprintf("reliability (horizontal and vertical) - %s (ICAO24: %s)", callsign, icao24));
		end
	end

% PLOT MERGED TRAJECTORY VERTICAL AND RELIABILITY
	if(plotMergedVertAndMergedRelaibility)
		if(~isempty(timeMerged))
			figure;
			ax1 = subplot(2,1,1);
			if(interpolationOn)
				% with interpolation:
				plot(interpolatedStates.time-timeMerged(1), interpolatedStates.baroAlt, 'k-');
			else
				% without interpolation:
				plot(timeMerged-timeMerged(1), baroAltMerged, 'k-');
			end
			hold on;
			plot(timeMerged-timeMerged(1), baroAltMerged, 'k.');

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
						objectHandle = patch([phaseStartTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseStartTime-timeMerged(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'b', 'DisplayName', 'cruise', 'FaceAlpha', 0.2);
						set(objectHandle, 'EdgeColor', 'none');
					end
					if(flightPhasesMerged(i,2) == 1)
						objectHandle = patch([phaseStartTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseStartTime-timeMerged(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'g', 'DisplayName', 'level', 'FaceAlpha', 0.2);
						set(objectHandle, 'EdgeColor', 'none');
					end
					if(flightPhasesMerged(i,2) == 2)
						objectHandle = patch([phaseStartTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseStartTime-timeMerged(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'y', 'DisplayName', 'climb', 'FaceAlpha', 0.2);
						set(objectHandle, 'EdgeColor', 'none');
					end
					if(flightPhasesMerged(i,2) == 3)
						objectHandle = patch([phaseStartTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseStartTime-timeMerged(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'm', 'DisplayName', 'descent', 'FaceAlpha', 0.2);
						set(objectHandle, 'EdgeColor', 'none');
					end
					if(flightPhasesMerged(i,2) == -1)
						objectHandle = patch([phaseStartTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseEndTime-timeMerged(1) phaseStartTime-timeMerged(1)], [max(ylim) max(ylim) min(ylim) min(ylim)], 'k', 'DisplayName', 'undefined', 'FaceAlpha', 0.2);
						set(objectHandle, 'EdgeColor', 'none');
					end
				end
			end

			xlabel('time / sec');
			ylabel('barometric altitude / ft');
			title(sprintf("parsed altitude - %s (ICAO24: %s)", callsign, icao24));

			if(~isempty(reliabilityTimeMerged))
				ax2 = subplot(2,1,2);
				if(interpolationOn)
					% with interpolation:
					plot(interpolatedStates.time-reliabilityTimeMerged(1), interpolatedStates.reliability, 'm-');
				else
					% without interpolation:
					plot(reliabilityTimeMerged-reliabilityTimeMerged(1), reliabilityMerged, 'm-');
				end
				xlabel('time / sec');
				ylabel('reliability / 1');
				title(sprintf("reliability (horizontal and vertical) - %s (ICAO24: %s)", callsign, icao24));
				linkaxes([ax1, ax2], 'x');
			end
		end
	end

end
