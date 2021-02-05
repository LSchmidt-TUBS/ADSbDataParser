package de.tu_bs.iff.adsb.dataparser.lib;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * Class containing a raw trajectory from TheOpenSkyNetwork in format 'state_vectors_data4'
 */
public class TrajectoryStateVectorsData4 {
	public String callsign = null;
	public String icao24 = null;
	
	public TableStateVectorsData4 table = new TableStateVectorsData4();		// contains the table with data values
	
	public TrajectoryStateVectorsData4() {
	}

	/**
	 * Function to read-in trajectory data from text file from Impala interface to The OpenSkyNetwork with format StateVectorsData4. 
	 * Example Impala request command: "SELECT * FROM state_vectors_data4 WHERE time>=1494201600 AND time<=1494288000 AND hour>=1494201600 AND hour<=1494288000 AND callsign='DLH6CR  ';"
	 * @param fileDir Directory to the text file
	 * @return Error code; <0: fatal error; 0: successful; >0: soft error (data available, but maybe not complete)
	 */
	public int readInInterfaceDataFile(String fileDir) {
		int errorCode = 0;
			// errorCode return success or error of readIn function
			// 0: success
			// fatal errors (no data available)
			// -1: file not found
			// -2: IOException
			// -3: lineBuffer too small
			// -4: time vector not consistent
			// -20xx: time-sort error -xx
			// soft errors (data available, but maybe not complete)
			// 1: icao24 vector not consistent
			// 2: callsign vector not consistent
			// 10xx: one or more sample-values (xx) not parseable (corresponding vaValue[index] set to false)
			// 20xx: time-sort error xx
		
		try {
			FileReader fileReader = new FileReader(fileDir);
			
			final int INPUT_DATA_SIZE = 2048;				// size of char buffer for to read in lines from the input file
			char[] inputData = new char[INPUT_DATA_SIZE];	// char buffer for to read in lines from the input file
			
			// obtain number of Samples within the file
			int numberOfSamples = 0;
			int lineEnd;				// position of the end of the file-line read into the char buffer (inputData)
			do {
				lineEnd = -1;				// -1: no new line available; >=0: new line
				int c;						// temporary variable to read in single characters
				while((c = fileReader.read()) != -1) {
					lineEnd++;
					inputData[lineEnd] = (char)c;
					if((char)c == '\n')		// end of line ...
						break;				// ... line readin finished
					if(lineEnd >= INPUT_DATA_SIZE-1) {
						errorCode = -3;
						break;
					}
				}
				if((lineEnd == -1) || (errorCode < 0))		// if lineEnd == -1: end of file reached
					break;
				if(inputData[0] == '|')			// potential line with sample starts with '|'
					if(inputData[2] != 't')		// header line starts with "time" at char[2] --> skip this line
						numberOfSamples++;
			} while(true);
			fileReader.close();
			if(errorCode < 0)
				return errorCode;
			
			// allocate trajectory variables with the number of samples
			table.allocateArrayMemory(numberOfSamples);
			
			// set callsign and icao24 to null (in case a new trajectory is read in)
			callsign = null;
			icao24 = null;

	
			// readIn Samples from the file (similar readin-algorith like above)
			fileReader = new FileReader(fileDir);
			int currentSampleIndex = 0;
			String lineString;
			do {
				lineEnd = -1;				// -1: no new line available; >=0: new line
				int c;						// temporary variable to read in single characters
				while((c = fileReader.read()) != -1) {
					lineEnd++;
					inputData[lineEnd] = (char)c;
					if((char)c == '\n')
						break;
					if(lineEnd >= INPUT_DATA_SIZE-1) {
						errorCode = -3;
						break;
					}
				}
				if((lineEnd == -1) || (errorCode < 0))		// if lineEnd == -1: end of file reached
					break;
				if(inputData[0] == '|')				// potential line with sample starts with '|'
					if(inputData[2] != 't') {		// header line starts with "time" at char[2] --> skip this line
						lineString = new String(inputData, 0, lineEnd);
						lineString = lineString.replace(" ", "");
						lineString = lineString.replace('|', ';');		// String.split(String) will not work with '|' and "|" has false meaning, so '|' needs to be replaced by another unique expression (';' chosen)
						String[] lineElements = lineString.split(";");

						try {
							table.time[currentSampleIndex] = Integer.parseInt(lineElements[1]);
						} catch(NumberFormatException e) {
							errorCode = -4;			// number of samples already set within the allocated memory (if a sample is not valid, whole trajectory data will be lost at this point --> fatal error)
						}
						
						if(icao24 == null)
							icao24 = lineElements[2];
						else
							if(!lineElements[2].equals(icao24))
								errorCode = 1;
						
						if(!parseDoubleSample(lineElements[3], table.lat, table.vaLat, currentSampleIndex))
							errorCode = 1000+3;
						if(!parseDoubleSample(lineElements[4], table.lon, table.vaLon, currentSampleIndex))
							errorCode = 1000+4;
						if(!parseDoubleSample(lineElements[5], table.velocity, table.vaVelocity, currentSampleIndex))
							errorCode = 1000+5;
						if(!parseDoubleSample(lineElements[6], table.heading, table.vaHeading, currentSampleIndex))
							errorCode = 1000+6;
						if(!parseDoubleSample(lineElements[7], table.vertRate, table.vaVertRate, currentSampleIndex))
							errorCode = 1000+7;

						if(callsign == null)
							callsign = lineElements[8];
						else
							if(!lineElements[8].equals(callsign))
								errorCode = 2;

						parseBooleanSample(lineElements[9], table.onGround, table.vaOnGround, currentSampleIndex);
						parseBooleanSample(lineElements[10], table.alert, table.vaAlert, currentSampleIndex);
						parseBooleanSample(lineElements[11], table.spi, table.vaSpi, currentSampleIndex);

						if(!parseIntSample(lineElements[12], table.squawk, table.vaSquawk, currentSampleIndex))
							errorCode = 1000+12;
						
						if(!parseDoubleSample(lineElements[13], table.baroAlt, table.vaBaroAlt, currentSampleIndex))
							errorCode = 1000+13;
						if(!parseDoubleSample(lineElements[14], table.geoAltitude, table.vaGeoAltitude, currentSampleIndex))
							errorCode = 1000+14;
						if(!parseDoubleSample(lineElements[15], table.lastPosUpdate, table.vaLastPosUpdate, currentSampleIndex))
							errorCode = 1000+15;
						if(!parseDoubleSample(lineElements[16], table.lastContact, table.vaLastContact, currentSampleIndex))
							errorCode = 1000+16;

						currentSampleIndex++;
					}
			} while(errorCode >= 0);
			
			fileReader.close();
		} catch(FileNotFoundException e) {
			errorCode = -1;
		} catch(IOException e) {
			errorCode = -2;
		}
		
		if(icao24 == null)			// no table structure with icao24 found
			errorCode = -3;
		if(callsign == null)		// no table structure with callsign found
			errorCode = -4;
		
		if(errorCode >= 0) {
			int timeSortErrorCode = sortSamplesByTime();
			if(timeSortErrorCode != 0) {
				if(timeSortErrorCode < 0)
					errorCode = -2000 + timeSortErrorCode;
				else
					errorCode = 2000 + timeSortErrorCode;
			}
		}
		
		if(errorCode < 0) {
			callsign = null;
			icao24 = null;
			
			// set trajectory table variables memory free:
			table.freeArrayMemory();
		}
		
		return errorCode;
	}

	/**
	 * Function to read-in trajectory data from String with data from Impala interface to The OpenSkyNetwork with format StateVectorsData4. 
	 * Example Impala request command: "SELECT * FROM state_vectors_data4 WHERE time>=1494201600 AND time<=1494288000 AND hour>=1494201600 AND hour<=1494288000 AND callsign='DLH6CR  ';"
	 * @param dataString String containing the data (raw Impala SSH interface format)
	 * @return Error code; <0: fatal error; 0: successful; >0: soft error (data available, but maybe not complete)
	 */
	public int readInInterfaceDataString(String dataString) {
		int errorCode = 0;
			// errorCode return success or error of readIn function
			// 0: success
			// fatal errors (no data available)
			// -1: file not found
			// -2: IOException
			// -3: lineBuffer too small
			// -4: time vector not consistent
			// -20xx: time-sort error -xx
			// soft errors (data available, but maybe not complete)
			// 1: icao24 vector not consistent
			// 2: callsign vector not consistent
			// 10xx: one or more sample-values (xx) not parseable (corresponding vaValue[index] set to false)
			// 20xx: time-sort error xx
		
		try {
			StringReader stringReader = new StringReader(dataString);
			
			final int INPUT_DATA_SIZE = 2048;				// size of char buffer for to read in lines from the input file
			char[] inputData = new char[INPUT_DATA_SIZE];	// char buffer for to read in lines from the input file
			
			// obtain number of Samples within the file
			int numberOfSamples = 0;
			int lineEnd;				// position of the end of the file-line read into the char buffer (inputData)
			do {
				lineEnd = -1;				// -1: no new line available; >=0: new line
				int c;						// temporary variable to read in single characters
				while((c = stringReader.read()) != -1) {
					lineEnd++;
					inputData[lineEnd] = (char)c;
					if((char)c == '\n')		// end of line ...
						break;				// ... line readin finished
					if(lineEnd >= INPUT_DATA_SIZE-1) {
						errorCode = -3;
						break;
					}
				}
				if((lineEnd == -1) || (errorCode < 0))		// if lineEnd == -1: end of file reached
					break;
				if(inputData[0] == '|')			// potential line with sample starts with '|'
					if(inputData[2] != 't')		// header line starts with "time" at char[2] --> skip this line
						numberOfSamples++;
			} while(true);
			stringReader.close();
			if(errorCode < 0)
				return errorCode;
			
			// allocate trajectory variables with the number of samples:
			table.allocateArrayMemory(numberOfSamples);
			
			// set callsign and icao24 to null (in case a new trajectory is read in)
			callsign = null;
			icao24 = null;

	
			// readIn Samples from the file (similar readin-algorith like above)
			stringReader = new StringReader(dataString);
			int currentSampleIndex = 0;
			String lineString;
			do {
				lineEnd = -1;				// -1: no new line available; >=0: new line
				int c;						// temporary variable to read in single characters
				while((c = stringReader.read()) != -1) {
					lineEnd++;
					inputData[lineEnd] = (char)c;
					if((char)c == '\n')
						break;
					if(lineEnd >= INPUT_DATA_SIZE-1) {
						errorCode = -3;
						break;
					}
				}
				if((lineEnd == -1) || (errorCode < 0))		// if lineEnd == -1: end of file reached
					break;
				if(inputData[0] == '|')				// potential line with sample starts with '|'
					if(inputData[2] != 't') {		// header line starts with "time" at char[2] --> skip this line
						lineString = new String(inputData, 0, lineEnd);
						lineString = lineString.replace(" ", "");
						lineString = lineString.replace('|', ';');		// String.split(String) will not work with '|' and "|" has false meaning, so '|' needs to be replaced by another unique expression (';' chosen)
						String[] lineElements = lineString.split(";");

						try {
							table.time[currentSampleIndex] = Integer.parseInt(lineElements[1]);
						} catch(NumberFormatException e) {
							errorCode = -4;			// number of samples already set within the allocated memory (if a sample is not valid, whole trajectory data will be lost at this point --> fatal error)
						}
						
						if(icao24 == null)
							icao24 = lineElements[2];
						else
							if(!lineElements[2].equals(icao24))
								errorCode = 1;
						
						if(!parseDoubleSample(lineElements[3], table.lat, table.vaLat, currentSampleIndex))
							errorCode = 1000+3;
						if(!parseDoubleSample(lineElements[4], table.lon, table.vaLon, currentSampleIndex))
							errorCode = 1000+4;
						if(!parseDoubleSample(lineElements[5], table.velocity, table.vaVelocity, currentSampleIndex))
							errorCode = 1000+5;
						if(!parseDoubleSample(lineElements[6], table.heading, table.vaHeading, currentSampleIndex))
							errorCode = 1000+6;
						if(!parseDoubleSample(lineElements[7], table.vertRate, table.vaVertRate, currentSampleIndex))
							errorCode = 1000+7;

						if(callsign == null)
							callsign = lineElements[8];
						else
							if(!lineElements[8].equals(callsign))
								errorCode = 2;

						parseBooleanSample(lineElements[9], table.onGround, table.vaOnGround, currentSampleIndex);
						parseBooleanSample(lineElements[10], table.alert, table.vaAlert, currentSampleIndex);
						parseBooleanSample(lineElements[11], table.spi, table.vaSpi, currentSampleIndex);

						if(!parseIntSample(lineElements[12], table.squawk, table.vaSquawk, currentSampleIndex))
							errorCode = 1000+12;
						
						if(!parseDoubleSample(lineElements[13], table.baroAlt, table.vaBaroAlt, currentSampleIndex))
							errorCode = 1000+13;
						if(!parseDoubleSample(lineElements[14], table.geoAltitude, table.vaGeoAltitude, currentSampleIndex))
							errorCode = 1000+14;
						if(!parseDoubleSample(lineElements[15], table.lastPosUpdate, table.vaLastPosUpdate, currentSampleIndex))
							errorCode = 1000+15;
						if(!parseDoubleSample(lineElements[16], table.lastContact, table.vaLastContact, currentSampleIndex))
							errorCode = 1000+16;

						currentSampleIndex++;
					}
			} while(errorCode >= 0);
			
			stringReader.close();
		} catch(FileNotFoundException e) {
			errorCode = -1;
		} catch(IOException e) {
			errorCode = -2;
		}
		
		if(errorCode >= 0) {
			int timeSortErrorCode = sortSamplesByTime();
			if(timeSortErrorCode != 0) {
				if(timeSortErrorCode < 0)
					errorCode = -2000 + timeSortErrorCode;
				else
					errorCode = 2000 + timeSortErrorCode;
			}
		}
		
		if(errorCode < 0) {
			callsign = null;
			icao24 = null;
			
			// set trajectory table variables memory free:
			table.freeArrayMemory();
		}
		
		return errorCode;
	}

	/**
	 * Parses double from String (lineElement) and saves result within Array (value[valueIndex]) and indicates availability of value (va[valueIndex])
	 * @param lineElement String of line-element to parse
	 * @param value Value-array to store value
	 * @param va Va-array indicating if a value is available within the value-array
	 * @param valueIndex Index within arrays value and va to store result
	 * @return Status; true: success; false: error (String not "NULL" and parsing of value not successful)
	 */
	private boolean parseDoubleSample(String lineElement, double[] value, boolean[] va, int valueIndex) {
		if(lineElement.equals("NULL"))
			va[valueIndex] = false;
		else {
			va[valueIndex] = true;
			try {
				value[valueIndex] = Double.parseDouble(lineElement);
			} catch(NumberFormatException e) {
				va[valueIndex] = false;
				return false;
			}
		}
		return true;
	}
	/**
	 * Parses integer from String (lineElement) and saves result within Array (value[valueIndex]) and indicates availability of value (va[valueIndex])
	 * @param lineElement String of line-element to parse
	 * @param value Value-array to store value
	 * @param va Va-array indicating if a value is available within the value-array
	 * @param valueIndex Index within arrays value and va to store result
	 * @return Status; true: success; false: error (String not "NULL" and parsing of value not successful)
	 */
	private boolean parseIntSample(String lineElement, int[] value, boolean[] va, int valueIndex) {
		if(lineElement.equals("NULL"))
			va[valueIndex] = false;
		else {
			va[valueIndex] = true;
			try {
				value[valueIndex] = Integer.parseInt(lineElement);
			} catch(NumberFormatException e) {
				va[valueIndex] = false;
				return false;
			}
		}
		return true;
	}
	/**
	 * Parses boolean from String (lineElement) and saves result within Array (value[valueIndex]) and indicates availability of value (va[valueIndex])
	 * @param lineElement String of line-element to parse
	 * @param value Value-array to store value
	 * @param va Va-array indicating if a value is available within the value-array
	 * @param valueIndex Index within arrays value and va to store result
	 */
	private void parseBooleanSample(String lineElement, boolean[] value, boolean[] va, int valueIndex) {
		switch(lineElement) {
		case "true":
			va[valueIndex] = true;
			value[valueIndex] = true;
			break;
		case "false":
			va[valueIndex] = true;
			value[valueIndex] = false;
			break;
		default:
			va[valueIndex] = false;
			break;
		}
	}
	
	/**
	 * Sorts the samples-table within table by time-channel
	 * @return Error code; <0: fatal error; 0: successful; >0: soft error (data available, but maybe not complete)
	 */
	private int sortSamplesByTime() {
		int errorCode = 0;
		// errorCode return success or error of time-sorting function
		// 0: success
		// fatal errors (no data available)
		// -1: input and output vector length not equal
		// soft errors (data available, but maybe not complete)
		// 1: double time values
		
		TableStateVectorsData4 tableUnsorted = table;			// local variable to temporary store unsorted trajectory-table
		
		table = new TableStateVectorsData4();					// global table will be reset, to later copy values in time-sorted order
		table.allocateArrayMemory(tableUnsorted.time.length);	// same table-size than before (no new samples and no samples will be skipped)

		ArrayList<Integer> timeUnsortedArray = new ArrayList<Integer>();		// ArrayList of time-vector from trajectory-table (entrys will be picked and removed from this ArrayList in sorted order)
		ArrayList<Integer> indexesUnsortedArray = new ArrayList<Integer>();		// ArrayList of index/line-number from trajectory-table (entrys will be picked and removed from this ArrayList simultaneously to timeUnsortedArray)
		
		for(int i=0; i<tableUnsorted.time.length; i++) {
			timeUnsortedArray.add(tableUnsorted.time[i]);
			indexesUnsortedArray.add(i);
		}
		
		ArrayList<Integer> indexesSorted = new ArrayList<Integer>();			// ArrayList with the list of time-sorted indexes (used to copy the trajectory-table-lines in time-sorted order afterwards)
		
		// copy indexes from indexesUnsortedArray to indexesSorted in time-sorted order ...
		int nextSmallestTimeIndex;
		for(; timeUnsortedArray.size()>0;) {
			nextSmallestTimeIndex = 0;
			for(int currentIndex=1; currentIndex<timeUnsortedArray.size(); currentIndex++) {
				if((int)timeUnsortedArray.get(currentIndex) <= (int)timeUnsortedArray.get(nextSmallestTimeIndex)) {
					if((int)timeUnsortedArray.get(currentIndex) == (int)timeUnsortedArray.get(nextSmallestTimeIndex)) {
						// (double timestamp within the trajetory-table found)
						errorCode = 1;
						continue;
					}
					nextSmallestTimeIndex = currentIndex;
				}
			}
			indexesSorted.add((int)indexesUnsortedArray.get(nextSmallestTimeIndex));
			timeUnsortedArray.remove(nextSmallestTimeIndex);
			indexesUnsortedArray.remove(nextSmallestTimeIndex);
		}
		// ... finished
		
		// copy table-lines from tableUnsorted to table in time-sorted order: 
		for(int i=0; i<indexesSorted.size(); i++) {
			nextSmallestTimeIndex = (int)indexesSorted.get(i);
			table.copyTableLine(tableUnsorted, nextSmallestTimeIndex, i);
		}
		
		if(table.time.length != tableUnsorted.time.length)
			errorCode = -1;
		
		tableUnsorted.freeArrayMemory();
		tableUnsorted = null;
		System.gc();
		
		return errorCode;
	}
	
	public double[][] getDoubleTableMatrix() {
		double[][] tableMatrix = new double[table.time.length][14];
		
		for(int i=0; i<table.time.length; i++) {
			tableMatrix[i][0] = (double)(table.time[i]);

			if(table.vaLat[i])
				tableMatrix[i][1] = (double)(table.lat[i]);
			else
				tableMatrix[i][1] = Double.NaN;

			if(table.vaLon[i])
				tableMatrix[i][2] = (double)(table.lon[i]);
			else
				tableMatrix[i][2] = Double.NaN;

			if(table.vaVelocity[i])
				tableMatrix[i][3] = (double)(table.velocity[i]);
			else
				tableMatrix[i][3] = Double.NaN;

			if(table.vaHeading[i])
				tableMatrix[i][4] = (double)(table.heading[i]);
			else
				tableMatrix[i][4] = Double.NaN;

			if(table.vaVertRate[i])
				tableMatrix[i][5] = (double)(table.vertRate[i]);
			else
				tableMatrix[i][5] = Double.NaN;

			if(table.vaOnGround[i])
				if(table.onGround[i])
					tableMatrix[i][6] = (double)(1);
				else
					tableMatrix[i][6] = (double)(0);
			else
				tableMatrix[i][6] = Double.NaN;

			if(table.vaAlert[i])
				if(table.alert[i])
					tableMatrix[i][7] = (double)(1);
				else
					tableMatrix[i][7] = (double)(0);
			else
				tableMatrix[i][7] = Double.NaN;

			if(table.vaSpi[i])
				if(table.spi[i])
					tableMatrix[i][8] = (double)(1);
				else
					tableMatrix[i][8] = (double)(0);
			else
				tableMatrix[i][8] = Double.NaN;

			if(table.vaSquawk[i])
				tableMatrix[i][9] = (double)(table.squawk[i]);
			else
				tableMatrix[i][9] = Double.NaN;

			if(table.vaBaroAlt[i])
				tableMatrix[i][10] = (double)(table.baroAlt[i]);
			else
				tableMatrix[i][10] = Double.NaN;

			if(table.vaGeoAltitude[i])
				tableMatrix[i][11] = (double)(table.geoAltitude[i]);
			else
				tableMatrix[i][11] = Double.NaN;

			if(table.vaLastPosUpdate[i])
				tableMatrix[i][12] = (double)(table.lastPosUpdate[i]);
			else
				tableMatrix[i][12] = Double.NaN;

			if(table.vaLastContact[i])
				tableMatrix[i][13] = (double)(table.lastContact[i]);
			else
				tableMatrix[i][13] = Double.NaN;
		}
		
		return tableMatrix;
	}

}
