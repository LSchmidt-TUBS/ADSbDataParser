package de.tu_bs.iff.adsb.dataparser.lib;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Locale;

import de.tu_bs.iff.adsb.dataparser.cosmath.CosMath;

public class AirportDatabase {
	private Airport[] airports = null;
	
	public AirportDatabase() {
	}
	
	public Airport getNearestAirport(double[] pos) {
		int nearestAirport = 0;
		double nearestAirportDistance = -1;
		
		double distance;
		for(int i=0; i<airports.length; i++) {
			distance = CosMath.earthDistEstimatedNM(pos, new double[] {airports[i].getLat()*CosMath.DEG_TO_RAD, airports[i].getLon()*CosMath.DEG_TO_RAD});
			if(i == 0) {
				nearestAirport = 0;
				nearestAirportDistance = distance;
				continue;
			}
			if(distance < nearestAirportDistance) {
				nearestAirport = i;
				nearestAirportDistance = distance;
			}
		}
		
		return airports[nearestAirport];
	}

	public Airport getNearestAirportWithElevationInformation(double[] pos) {
		int nearestAirport = -1;
		double nearestAirportDistance = -1;
		
		double distance;
		for(int i=0; i<airports.length; i++) {
			if(!airports[i].isElevationAvailable())
				continue;
			
			distance = CosMath.earthDistEstimatedNM(pos, new double[] {airports[i].getLat()*CosMath.DEG_TO_RAD, airports[i].getLon()*CosMath.DEG_TO_RAD});
			if(nearestAirportDistance == -1) {
				nearestAirport = i;
				nearestAirportDistance = distance;
				continue;
			}
			if(distance < nearestAirportDistance) {
				nearestAirport = i;
				nearestAirportDistance = distance;
			}
		}
		
		if(nearestAirport != -1)
			return airports[nearestAirport];
		else
			return null;
	}

	public int readInAirports(String dir) {
		ArrayList<Airport> airports = new ArrayList<Airport>();
		
		String programInfoMessage = "";
		
		int errorCode = 0;		// <0: fatal error; 0: successful; >0: soft error (errorCode: count of erroneous database-entries)

		try {
			Reader fileReader = new FileReader(dir);
			final int INPUT_DATA_SIZE = 2048;				// size of char buffer for to read in lines from the input file
			char[] inputData = new char[INPUT_DATA_SIZE];	// char buffer for to read in lines from the input file
			int lineEnd;				// position of the end of the file-line read into the char buffer (inputData)
			String lineString;
			
			int tableNumber = -1;

			// read in file lines ...
			do {
				// read in new line
				lineEnd = -1;				// -1: no new line available; >=0: new line
				int c;						// temporary variable to read in single characters
				while((c = fileReader.read()) != -1) {
					lineEnd++;
					inputData[lineEnd] = (char)c;
					if((char)c == '\n')		// end of line ...
						break;				// ... line readin finished
					if(lineEnd >= INPUT_DATA_SIZE-1) {
						System.err.println("Error: Read-in airports. Input buffer too small");
						fileReader.close();
						return (errorCode = -1);
					}
				}
				if(lineEnd == -1)		// if lineEnd == -1: end of file reached
					break;

				if(inputData[0] == '%') {
					if(!programInfoMessage.isEmpty())
						programInfoMessage = programInfoMessage.concat("\n");
					programInfoMessage = programInfoMessage.concat(new String(inputData, 1, lineEnd-1));
					continue;
				}

				if(inputData[0] == '#') {
					tableNumber++;
					continue;
				}

				lineString = new String(inputData, 0, lineEnd);
				String[] lineElements = lineString.split(",", -1);
				
				switch(tableNumber) {
/*				case 0:
					break;
				case 1:
					break;*/
				default:
					// airports data file doesn't have specific tables (just one table)
					try {
						double lat = Double.parseDouble(lineElements[5]);
						double lon = Double.parseDouble(lineElements[6]);
						boolean vaElevation;
						double elevation;
						if(!lineElements[7].isEmpty()) {
							vaElevation = true;
							elevation = Double.parseDouble(lineElements[7]);
						} else {
							vaElevation = false;
							elevation = -Double.MAX_VALUE;			// value not valid
						}
						boolean vaTimezoneUTCoffset;
						double timezoneUTCoffset;
						if((lineElements.length >= 9) && (!lineElements[8].isEmpty())) {
							vaTimezoneUTCoffset = true;
							timezoneUTCoffset = Double.parseDouble(lineElements[8]);
						} else {
							vaTimezoneUTCoffset = false;
							timezoneUTCoffset = -Double.MAX_VALUE;	// value not valid
						}
						String name = lineElements[0];
						String city = lineElements[1];
						String country = lineElements[2];
						String iataCode = lineElements[3];
						String icaoCode = lineElements[4];
						
						airports.add(new Airport(lat, lon, vaElevation, elevation, vaTimezoneUTCoffset, timezoneUTCoffset, name, city, country, iataCode, icaoCode));
					} catch(Exception e) {
						errorCode++;
					}
					break;
				}

			} while(true);
			// ... read in file lines
			fileReader.close();
		} catch (IOException e) {
			System.err.println("Error: Read-in airports not successful. ");
			e.printStackTrace();
			return (errorCode = -1);
		}
		
		if(!programInfoMessage.isEmpty()) {
		}

		this.airports = new Airport[airports.size()];
		for(int i=0; i<airports.size(); i++) {
			this.airports[i] = (Airport)airports.get(i);
		}
		return errorCode;
	}

	public int recompileCSVdatabase(String inputFileDir, String outputFileDir, int[] fieldIndices, int headerLinesCount, char separationChar, char quotationChar, String[] replace, String[] replacement, boolean latLonAreInRadian, boolean elevationIsInFeet) {
		int errorCode = 0;
		
		try {
			Reader fileReader = new FileReader(inputFileDir);
			final int INPUT_DATA_SIZE = 2048;				// size of char buffer for reading in lines from the input file
			char[] inputData = new char[INPUT_DATA_SIZE];	// char buffer for reading in lines from the input file
			int lineEnd;				// position of the end of the file-line read into the char buffer (inputData)
			String lineString;
			
			Writer fileWriter = new FileWriter(outputFileDir);
			fileWriter.write("#name,city,country,IATA code,ICAO code,lat [deg],lon [deg],elevation [ft]\n");		// write header line
			Formatter formatter;
			
			String name;
			String city;
			String country;
			String iataCode;
			String icaoCode;
			String latString;
			String lonString;
			String elevationString;
			double lat;
			double lon;
			double elevation;

			int lineNumber = 0;
			// recompile file lines ...
			do {
				// read in new line
				lineNumber++;
				lineEnd = -1;				// -1: no new line available; >=0: new line
				int c;						// temporary variable to read in single characters
				while((c = fileReader.read()) != -1) {
					lineEnd++;
					inputData[lineEnd] = (char)c;
					if((char)c == '\n')		// end of line ...
						break;				// ... line readin finished
					if(lineEnd >= INPUT_DATA_SIZE-1) {
						System.err.println("Error: Recompile airports csv-file. Input buffer too small");
						fileReader.close();
						fileWriter.close();
						return (errorCode = -1);
					}
				}
				if(lineEnd == -1)		// if lineEnd == -1: end of file reached
					break;

				if(inputData[0] == '%') {
					continue;
				}

				if(inputData[0] == '#') {
					continue;
				}
				
				// separationChar within quotation-marks (replace with '_') ...
				boolean withinQuotation = false;
				for(int i=0; i<lineEnd; i++)
					if(withinQuotation) {
						if(inputData[i] == separationChar) {
							inputData[i] = '_';
						}
						if(inputData[i] == quotationChar)
							withinQuotation = false;
					} else {
						if(inputData[i] == quotationChar)
							withinQuotation = true;
					}
				// ... separationChar within quotation-marks (replace with '_')

				lineString = new String(inputData, 0, lineEnd);
				
				String[] lineElements = lineString.split(String.format("%c", separationChar), -1);
				
				if(lineNumber <= headerLinesCount)
					continue;
				
				try {
					name = lineElements[fieldIndices[0]];
					city = lineElements[fieldIndices[1]];
					country = lineElements[fieldIndices[2]];
					iataCode = lineElements[fieldIndices[3]];
					icaoCode = lineElements[fieldIndices[4]];
					latString = lineElements[fieldIndices[5]];
					lonString = lineElements[fieldIndices[6]];
					elevationString = lineElements[fieldIndices[7]];
				} catch(Exception e) {
					errorCode++;
					continue;
				}
				
				for(int i=0; i<replace.length; i++) {
					name = name.replaceAll(replace[i], replacement[i]);
					city = city.replaceAll(replace[i], replacement[i]);
					country = country.replaceAll(replace[i], replacement[i]);
					iataCode = iataCode.replaceAll(replace[i], replacement[i]);
					icaoCode = icaoCode.replaceAll(replace[i], replacement[i]);
					latString = latString.replaceAll(replace[i], replacement[i]);
					lonString = lonString.replaceAll(replace[i], replacement[i]);
					elevationString = elevationString.replaceAll(replace[i], replacement[i]);
				}
				
				try {
					lat = Double.parseDouble(latString);
					lon = Double.parseDouble(lonString);
					if(!elevationString.isEmpty()) {
						elevation = Double.parseDouble(elevationString);
						if(!elevationIsInFeet)
							elevation *= CosMath.METER_TO_FEET;
						formatter = new Formatter(new StringBuilder(), Locale.US);
						elevationString = formatter.format("%.0f", elevation).toString();
						formatter.close();
					} else
						elevationString = "";
				} catch(Exception e) {
					errorCode++;
					continue;
				}
				
				if(latLonAreInRadian) {
					lat *= CosMath.RAD_TO_DEG;
					lon *= CosMath.RAD_TO_DEG;
				}
				
				formatter = new Formatter(new StringBuilder(), Locale.US);
				fileWriter.append(formatter.format("%s,%s,%s,%s,%s,%.6f,%.6f,%s\n", name, city, country, iataCode, icaoCode, lat, lon, elevationString).toString());
				formatter.close();

			} while(true);
			// ... recompile file lines
			fileReader.close();
			fileWriter.close();
		} catch(Exception e) {
			System.err.println("Error: Recompile airports csv-file not successful. ");
			e.printStackTrace();
			return (errorCode = -1);
		}
		
		return errorCode;
	}

}
