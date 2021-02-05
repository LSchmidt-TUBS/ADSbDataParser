package de.tu_bs.iff.adsb.dataparser.lib;

public class Airport {
	private double lat;
	private double lon;
	private boolean vaElevation;
	private double elevation;
	private boolean vaTimezoneUTCoffset;
	private double timezoneUTCoffset;
	private String name;
	private String city;
	private String country;
	private String iataCode;
	private String icaoCode;
	
	public Airport(double lat, double lon, boolean vaElevation, double elevation, boolean vaTimezoneUTCoffset, double timezoneUTCoffset, String name, String city, String country, String iataCode, String icaoCode) {
		this.lat = lat;
		this.lon = lon;
		this.vaElevation = vaElevation;
		this.elevation = elevation;
		this.vaTimezoneUTCoffset = vaTimezoneUTCoffset;
		this.timezoneUTCoffset = timezoneUTCoffset;
		this.name = name;
		this.city = city;
		this.country = country;
		this.iataCode = iataCode;
		this.icaoCode = icaoCode;
	}
	
	public double getLat() {
		return lat;
	}
	
	public double getLon() {
		return lon;
	}
	
	public String getIcaoCode() {
		return icaoCode;
	}

	public String getName() {
		return name;
	}
	
	public String getCity() {
		return city;
	}

	public boolean isElevationAvailable() {
		return vaElevation;
	}
	
	public double getElevation() {
		return elevation;
	}
	
	public boolean isTimezoneUTCoffsetAvailable() {
		return vaTimezoneUTCoffset;
	}
	
	public double getTimezoneUTCoffset() {
		return timezoneUTCoffset;
	}
	
	public String getCountry() {
		return country;
	}
	
	public String getIataCode() {
		return iataCode;
	}
}
