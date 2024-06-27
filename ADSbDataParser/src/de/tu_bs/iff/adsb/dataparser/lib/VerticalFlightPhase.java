package de.tu_bs.iff.adsb.dataparser.lib;

public class VerticalFlightPhase {
	public enum Phase {
		UNDEFINED, CRUISE, LEVEL, CLIMB, DESCENT
	}
	
	public Phase phase;
	public int startIndex;
	
	public VerticalFlightPhase(Phase phase, int startIndex) {
		this.phase = phase;
		this.startIndex = startIndex;
	}
	
	public VerticalFlightPhase clone() {
		return new VerticalFlightPhase(phase, startIndex);
	}

}
