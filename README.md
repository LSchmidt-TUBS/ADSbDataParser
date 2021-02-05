# *ADSb***DataParser**

The ADS-B data parser is based on the work of L.L. Schmidt and C. Bloemer-Zurborg conducted at the Institute of Flight Guidance, TU Braunschweig. It can be used to filter and preprocess ADS-B data and is especially adapted to historic traffic data with format *state_vectors_data4* of [The OpenSky Network](https://opensky-network.org/). 

Notices: 

* The data parser can do its best, when fed with the whole raw trajectory available. Even though if only parts of the trajectory are of interest. 
* The returned parsed trajectory needs to be interpolated to regain the aircraft state at any timestamp (see redundancy-filtration). 


## Quick start guide

1. Download (and extract - if zipped) this repository. 
2. Start MATLAB and navigate to directory *'./MatlabInterface'* in the downloaded repository-folder
3. **This step is optional** (for completeness metric required): Download, recompile and set airport database

i. Download airport database from [OurAirports](https://ourairports.com/) here: https://ourairports.com/data/airports.csv and save the file to *'./MatblabInterface/airports.csv'* in the downloaded repository-folder

ii. Recompile airport database with following command in MATLAB: 
<pre>
javaADSbParserRecompileAirportDatabase("./airports.csv", "./airportsRecompiled.csv");
</pre>

iii. Set airport database directory *("./airportsRecompiled.csv")* in files *javaADSbParser.m* and *javaADSbParserParallel.m* in variable *AIRPORT_DATABASE_FILE_DIR* (you may just use the presets in the comments - see lines 13 and 15 in the function files)

4. Gather ADS-B data from the Historical Database of [The OpenSky Network](https://opensky-network.org)

i. Connect to the Impala Shell of [The OpenSky Network](https://opensky-network.org) (e.g. with PuTTY ([PuTTY - Wikipedia](https://en.wikipedia.org/wiki/PuTTY))) and log the communication to the file *'./MatlabInterface/impalaFiles/flight_DLH9U.log'*

ii. Request flight *'DLH9U'* from the Historical Database via the Impala Shell with the following command (Shell): 
<pre>
SELECT * FROM state_vectors_data4 WHERE callsign='DLH9U   ' AND time>=1494235200 AND time<=1494246600 AND hour>=1494234000 AND hour<=1494248400;
</pre>

iii. End connection (Shell): 
<pre>
exit;
</pre>

5. Parse the flight in Matlab with: 
<pre>
parsedTrajectory = javaADSbParser("./impalaFiles/flight_DLH9U.log");
</pre>

6. Plot the parsed trajectory in Matlab: 
<pre>
javaADSbParserPlot(parsedTrajectory);

javaADSbParserPlotCombined(parsedTrajectory);
</pre>

  or (with interpolated aircraft states of parsed trajectory, max. time-step: 10 sec.): 
<pre>
javaADSbParserPlot(parsedTrajectory, 10);

javaADSbParserPlotCombined(parsedTrajectory, 10);
</pre>


## Redundancy-filtration, interpolation and reliability-channel

The data parser is filtering redundant samples in horizontal and vertical plane. That is to say, samples which can be interpolated on an orthodrome (great circle) in the horizontal plane and with linear interpolation in the vertical plane, within certain margins, are removed from the parsed trajectory. In combination with the filtering of faulty data, this led to a reduction of the number of samples to 7.5 &#37; in a dataset comprising 5467 trajectories. Nevertheless is the underlaying raw-data (version used by the parser, not initial state-vectors-data4 trajectory) stored in the return-struct (parsedTrajectory.raw). For to use the afformentioned memory reduction it is recommended to clear or remove this field (parsedTrajectory.raw) from the result-struct. 
For to re-obtain the aircraft states, the position (horizontal plane) needs to be interpolated on an orthodrome (great circle) and the baro altitude (vertical plane) needs to be interpolated linearly. This can be done with following function-call in MATLAB: 
<pre>
interpolatedStates = javaADSbParserInterpolate(parsedTrajectory, timestamps);
</pre>

The removal of redundant samples also removes the information, at which timestamps underlying raw-samples (supporting samples) were used to support the data history. Therefore the struct (parsedTrajectory.samplingTime) contains all used valid (fault-filterd, but not redundant-filtered) sample-timestamps of horizontal and vertical planes. You may also clear/ remove this field for memory reduction, if not required. 
Furthermore the return struct contains a reliability-channel ([parsedTrajectory.reliabilityTime, parsedTrajectory.reliability]). This channel shall indicate the reliability of the final (re-interpolated) trajectory, ranges from 0 to 1 and has a time-step of 5 sec. For example: In trajectory phases without receiver coverage (no supporting samples), the reliability-channel shall take 0 as value. For to assess the reliability of the (re-interpolated) trajectory, this channel is recommended to be used. The reliability-channel is an input for the reliability-metric. 


## How to use

The data parser is developed using Java technology and m-files are provided to use the parser within MATLAB. 


### Input data

Requests of historic flights of table *state_vectors_data4* from [The OpenSky Network](https://opensky-network.org) via the Impala Shell serve as input to the data parser. That is to say the parser directly loads the log-files of Impala Shell connection, whereby each flight needs to be stored in a separate log-file. For gathering these log-files you may use PuTTY ([PuTTY - Wikipedia](https://en.wikipedia.org/wiki/PuTTY)). 
For to request a historic flight you may use the following dummy Impala Shell request: 

	SELECT * FROM state_vectors_data4 WHERE callsign='CALLSIGN' AND time>=0 AND time<=0 AND hour>=0 AND hour<=0;
  
Please replace CALLSIGN with the desired callsign, whereby the String always needs be be sized to eight characters by adding spaces. For example: 

	Correct: callsign='FLIGHT  '
	Wrong: callsign='FLIGHT'

Also the time- and hour-specification needs to be adapted. For further reading please refer to [A Quick Guide To OpekSky's Impala Shell](https://opensky-network.org/data/impala). 


### How to set up an airport database (for completeness metric)

The determination of the completeness metric needs an airport database. In case there is no database available, the ADS-B data parser will work as well, but the completeness metric will be assigned as *-1*. 
The airport database needs to be in csv-format with the following table-structure: 

	#name,city,country,IATA code,ICAO code,lat [deg],lon[deg],elevation [ft]

In the current version of the parser only fields of lat, lon and elevation are required. The other fields may be left blank. Character &#35; specifies the table-structure definition line, whereas &#37; can be used to set line-comments in the database file. 

It is suggested to use the airport database of [OurAirports](https://ourairports.com), which can be found here: https://ourairports.com/data/airports.csv

For to automatically generate the required format for the ADS-B data parser, you may use the m-function *javaADSbParserRecompileAirportDatabase* provided within this package: 

	javaADSbParserRecompileAirportDatabase("./airports.csv", "./airportsRecompiled.csv");

The directory of the recompiled aiport database needs to be stored in the variable *AIRPORT_DATABASE_FILE_DIR* of m-files *javaADSbParser.m* and *javaADSbParserParallel.m*. 


### How to use the MATLAB interface

Various MATLAB-functions for parsing single or multiple trajectories are provided, whereby all functions need the log-files of Impala requests specified above. Please note, that each flight needs to be stored in a separate file and the format of requested data needs to be *state_vectors_data4*. 

The following function-call can be used to parse a single trajectory stored in the file-directory-string *"./fileDir/flight.log"*. The result will be returned as a struct: 

	parsedTrajectory = javaADSbParser("./fileDir/flight.log");

This function-call may be used to parse multiple trajectories stored in the directories of passed string-vector in parallel mode. The result will be returned as a struct-vector. It is not recommended to use this function for more than 20 trajectories at once: 

	parsedTrajectories = javaADSbParserParallel(["./fileDir/flight_A.log"; "./fileDir/flight_B.log"; ...]);

This function-call can be used to parse multiple trajectories (even more than 20 trajectories) stored in the directories of passed string-vector in parallel mode. The result will be returned as a struct-vector: 

	parsedTrajectories = javaADSbParserParallelMultiple(["./fileDir/flight_A.log"; "./fileDir/flight_B.log"; ...]);

This function-call parses all trajectory-files contained in the folder-directory *'./fileDir/folderWithFilesToParse/'*. There shouldn't be any other files in the directory than trajectory-log-files, exept other directories, as the data parser will try to parse all files found in the specified directory: 

	parsedTrajectories = javaADSbParserDirectory('./fileDir/folderWithFilesToParse/');

The following function-calls plot the raw and parsed trajectory of struct parsedTrajectory. You may refer to the source code of these functions to learn more about the structure of the returned structs of parsed trajectories: 

	javaADSbParserPlot(parsedTrajectory);
  
	javaADSbParserPlotCombined(parsedTrajectory);

To interpolate the aircraft states of parsed trajectory before plotting, you may use the following function-calls (max. interpolation time-step: interpolationTimeStep [sec.]). In most cases (with default settings), regarding the plot-visualization, the interpolation only effects the horizontal path. 

	javaADSbParserPlot(parsedTrajectory, interpolationTimeStep);
  
	javaADSbParserPlotCombined(parsedTrajectory, interpolationTimeStep);

Interpolation of aircraft states (see removal of redundant samples): 

	interpolatedStates = javaADSbParserInterpolate(parsedTrajectory, timestamps)


## Related content/ references

[Institute of Flight Guidance - TU Braunschweig](https://www.tu-braunschweig.de/en/iff)

[The OpenSky Network](https://opensky-network.org/)

[OurAirports](https://ourairports.com/)

[AirTrafficTool developed at IFF](https://www.tu-braunschweig.de/en/iff/research/projects/airtraffictool)

