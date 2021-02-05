package de.tu_bs.iff.adsb.dataparser.main;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Formatter;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import de.tu_bs.iff.adsb.dataparser.cosmath.CosMath;
import de.tu_bs.iff.adsb.dataparser.lib.TrajectoryHorizontal;
import de.tu_bs.iff.adsb.dataparser.lib.TrajectoryMerged;
import de.tu_bs.iff.adsb.dataparser.lib.TrajectoryStateVectorsData4;
import de.tu_bs.iff.adsb.dataparser.lib.TrajectoryVertical;

/**
 * Main class of ADS-B data parser program. 
 * This class will not be used for library-use.
 */
public class ADSbDataParser {
	private static final String PROGRAM_VERSION = "1-18";
	private static final String PROGRAM_AUTHORS = "LLS, CBZ";

	static TrajectoryStateVectorsData4 trajectory;
	static TrajectoryVertical trajectoryVertical;
	static TrajectoryHorizontal trajectoryHorizontal;
	static TrajectoryMerged trajectoryMerged;
	
	/**
	 * Main function can be used to show program-info and to test parsing data from a file. 
	 * If this function is called without arguments, the program-info will be printed. 
	 * Argument '-dialog': Open file dialog for parsing a file. 
	 * Argument '-file IN_FILE_DIR OUT_FILE_DIR': Parse the file IN_FILE_DIR and save parsed trajectory to OUT_FILE_DIR. 
	 * @param args Arguments for main-function call. 
	 */
	public static void main(String[] args) {
		if(args.length == 0) {
			System.out.println(String.format("ADSbDataParser\nVersion: %s", PROGRAM_VERSION));
			System.out.println(String.format("by %s", PROGRAM_AUTHORS));
			System.out.println("Institute of Flight Guidance, TU Braunschweig");
			System.out.println("\nArgument Options:");
			System.out.println("-dialog: Open file dialog for parsing a file");
			System.out.println("-file IN_FILE_DIR OUT_FILE_DIR: Parse the file IN_FILE_DIR and save parsed trajectory to OUT_FILE_DIR");
		} else {
			String inFileDir;
			String outFileDir;
			switch(args[0]) {
			case "-dialog":
				JFileChooser fileChooser = new JFileChooser();
				if(fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
					return;
				inFileDir = fileChooser.getSelectedFile().getAbsolutePath();
				parseTrajectory(inFileDir);
				
				JFileChooser fileChooserOutput = new JFileChooser();
				fileChooserOutput.setFileFilter(new FileNameExtensionFilter("Traffic Scenario  (*.atttfc)", "atttfc"));
				if(fileChooserOutput.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
					String absoluteSaveFilePath = fileChooserOutput.getSelectedFile().getAbsolutePath();
					if(!absoluteSaveFilePath.endsWith(".atttfc"))
						outFileDir = absoluteSaveFilePath.concat(".atttfc");
					else
						outFileDir = absoluteSaveFilePath;
					if(saveParsedTrajectoryToFile(trajectoryMerged, outFileDir))
						System.out.println("Parsed trajectory saved to file.");
				}

				break;
			case "-file":
				if(args.length <= 2) {
					System.err.println(String.format("Please specify directory of in- and output file to parse. \nArgument: -ffile IN_FILE_DIR OUT_FILE_DIR"));
				} else {
					inFileDir = args[1];
					outFileDir = args[2];
					parseTrajectory(inFileDir);
					if(saveParsedTrajectoryToFile(trajectoryMerged, outFileDir))
						System.out.println("Parsed trajectory saved to file.");
				}
				break;
			default:
				System.err.println(String.format("Unknown argument"));
				System.out.println("\nArgument Options:");
				System.out.println("-dialog: Open file dialog for parsing a file");
				System.out.println("-file IN_FILE_DIR OUT_FILE_DIR: Parse the file IN_FILE_DIR and save parsed trajectory to OUT_FILE_DIR");
				break;
			}
		}
	}
	
	private static void parseTrajectory(String fileDir) {
		trajectory = new TrajectoryStateVectorsData4();
		int errorCode = trajectory.readInInterfaceDataFile(fileDir);
		if(errorCode < 0) {
			System.err.println("Error while reading data file.");
			return;
		}

		trajectoryVertical = new TrajectoryVertical();
		trajectoryHorizontal = new TrajectoryHorizontal();
		trajectoryMerged = new TrajectoryMerged();
		
		if(trajectoryVertical.setRawTrajectoryFromStateVectorsData4(trajectory) != 0) {
			System.err.println("Error while setting vertical trajectory.");
			return;
		}
		if(trajectoryVertical.parseTrajectory() != 0) {
			System.err.println("Error while parsing vertical trajectory.");
			return;
		}

		if(trajectoryHorizontal.setRawTrajectoryFromStateVectorsData4(trajectory) != 0) {
			System.err.println("Error while setting horizontal trajectory.");
			return;
		}
		if(trajectoryHorizontal.parseTrajectory() != 0) {
			System.err.println("Error while parsing horizontal trajectory.");
			return;
		}

		trajectoryMerged.setTrajectories(trajectoryVertical, trajectoryHorizontal);
		if(trajectoryMerged.mergeTrajectories() != 0) {
			System.err.println("Error while merging trajectory-planes.");
			return;
		}
		
		System.out.println("Trajectory parsing finished.");
	}
	
	private static boolean saveParsedTrajectoryToFile(TrajectoryMerged trajectoryMerged, String fileDir) {
		if(trajectoryMerged == null)
			return false;

		try {
			double[] time = trajectoryMerged.getTime();
			double[] lat = trajectoryMerged.getLat();
			double[] lon = trajectoryMerged.getLon();
			double[] baroAlt = trajectoryMerged.getBaroAlt();
			
			if(time.length == 0) {
				System.out.println("Parsed trajectory contains zero samples, output-file will not be created/ saved.");
				return false;
			}
			
			Writer fileWriter = new FileWriter(fileDir);
			fileWriter.write("");		// clear file
			Formatter formatter;
			
			fileWriter.append("#callsign,icao24\n");
			fileWriter.append(String.format("%s,%s\n", trajectoryMerged.callsign, trajectoryMerged.icao24));
			fileWriter.append("#time,lat,lon,baroAlt\n");
			
			for(int i=0; i<time.length; i++) {
				formatter = new Formatter(new StringBuilder(), Locale.US);
				fileWriter.append(formatter.format("%.2f,%.6f,%.6f,%.2f\n", time[i], lat[i], lon[i], baroAlt[i]*CosMath.FEET_TO_METER).toString());
				formatter.close();
			}
			
			fileWriter.close();
		} catch (IOException e) {
			System.err.println("Error while writing file ...");
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

}
