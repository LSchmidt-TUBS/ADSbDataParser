function javaADSbParserRecompileAirportDatabase(inputFileDir, outputFileDir)
% Function to recompile (csv) airport database. Parameters adapted for airport database of OurAirports: https://ourairports.com/
% Please find source airport database here: https://ourairports.com/data/airports.csv
% inputFileDir (string): file-directory to source airport database
% outputFileDir (string): file-directory to save recompiled airport database
% e.g.: javaADSbParserRecompileAirportDatabase("./airports.csv", "./airportsRecompiled.csv");


% Java directory to ADSbDataParser: 
	javaaddpath("./../ADSbDataParser/bin");


	nameFieldIndex = 3;
	cityFieldIndex = 10;
	countryFieldIndex = 8;
	iataCodeFieldIndex = 13;
	icaoCodeFieldIndex = 1;
	latFieldIndex = 4;
	lonFieldIndex = 5;
	elevationFieldIndex = 6;
	fieldIndices = [nameFieldIndex; cityFieldIndex; countryFieldIndex; iataCodeFieldIndex; icaoCodeFieldIndex; latFieldIndex; lonFieldIndex; elevationFieldIndex];

	java_AirportDatabase = javaObject("de.tu_bs.iff.adsb.dataparser.lib.AirportDatabase");					% variable containing the java object of class AirportDatabase
	errorCode = javaMethod("recompileCSVdatabase", java_AirportDatabase, string(inputFileDir), string(outputFileDir), fieldIndices, 1, ',', '"', [""""], [""], false, true);

	if(errorCode == 0)
		disp('Recompiling of airport database successful');
	else
		if(errorCode > 0)
			disp(sprintf("Airport database recompiled with %d entry-errors (these entries are skipped)", errorCode));
		else
			disp('Error: Recompiling of airport database not successful');
		end
	end
	
end
