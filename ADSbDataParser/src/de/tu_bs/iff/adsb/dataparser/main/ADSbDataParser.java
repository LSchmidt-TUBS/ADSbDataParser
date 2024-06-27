package de.tu_bs.iff.adsb.dataparser.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Locale;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import de.tu_bs.iff.adsb.dataparser.lib.AirportDatabase;
import de.tu_bs.iff.adsb.dataparser.lib.TrajectoryHorizontal;
import de.tu_bs.iff.adsb.dataparser.lib.TrajectoryMerged;
import de.tu_bs.iff.adsb.dataparser.lib.TrajectoryStateVectorsData4;
import de.tu_bs.iff.adsb.dataparser.lib.TrajectoryVertical;
import de.tu_bs.iff.adsb.dataparser.lib.VerticalFlightPhase;
import de.tu_bs.iff.adsb.dataparser.parallel.ParallelParser;

/**
 * Main class of ADS-B data parser program. 
 * This class will not be used for library-use. 
 */
public class ADSbDataParser {
	private static final String PROGRAM_VERSION = "2-23";
	private static final String PROGRAM_AUTHORS = "LLS, CBZ";

	static TrajectoryStateVectorsData4 trajectory;
	static TrajectoryVertical trajectoryVertical;
	static TrajectoryHorizontal trajectoryHorizontal;
	static TrajectoryMerged trajectoryMerged;
	
	/**
	 * Main function can be used to open parser-GUI or to execute parser within command line. 
	 * If this function is called without arguments, the parser-GUI will be opened. 
	 * @param args Arguments for main-function call. 
	 */
	public static void main(String[] args) {
		CommandLineParameters commandLineParameters = parseCommandLine(args);
		if(commandLineParameters == null)
			return;
		
		if(commandLineParameters.gui)
			openGUI(commandLineParameters);
		else
			parse(commandLineParameters, null);
	}
	
	private enum MODE {
		FILE, FOLDER
	}
	static class CommandLineParameters {
		MODE mode = MODE.FILE;
		boolean modeSpecified = false;
		boolean gui = false;
		String inputDir = null;
		String outputDir = null;
		String airportDatabaseFileDir = null;
		boolean filterRedundantSamples = true;;
		boolean forceOverwriteOutputFile = false;
		int threadCount = (int)Math.round((double)Runtime.getRuntime().availableProcessors() / 2);
		boolean threadCountSpecified = false;
		int stepCount = 2*Runtime.getRuntime().availableProcessors();
		boolean stepCountSpecified = false;
	}
	private static CommandLineParameters parseCommandLine(String[] args) {
		CommandLineParameters commandLineParameters = new CommandLineParameters();
		if(args.length == 0)
			commandLineParameters.gui = true;
		
		for(int i=0; i<args.length; i++) {
			String argument = args[i];
			String value = null;
			
			if(argument.indexOf('=') != -1) {
				value = argument.substring(argument.indexOf('=')+1);
				argument = argument.substring(0, argument.indexOf('='));
			}
			
			switch(argument) {
			case "--help":
			case "-h":
				if(args.length != 1)
					System.err.println("ERROR: Arguments '-h' and '--help' can only be passed exclusively. ");
				else
					printCommandLineHelp();
				return null;
			case "--mode":
			case "-m":
				try {
					commandLineParameters.mode = MODE.valueOf(value);
					commandLineParameters.modeSpecified = true;
				} catch(IllegalArgumentException e) {
					System.err.println(String.format("ERROR: Unknown mode value: '%s'", value));
					return null;
				}
				break;
			case "--gui":
			case "-g":
				commandLineParameters.gui = true;
				break;
			case "--input":
			case "-i":
				commandLineParameters.inputDir = value;
				break;
			case "--output":
			case "-o":
				commandLineParameters.outputDir = value;
				break;
			case "--airport-database":
			case "-a":
				commandLineParameters.airportDatabaseFileDir = value;
				break;
			case "--redundancy-filtration":
			case "-r":
				switch(value) {
				case "ON":
					commandLineParameters.filterRedundantSamples = true;
					break;
				case "OFF":
					commandLineParameters.filterRedundantSamples = false;
					break;
				default:
					System.err.println(String.format("ERROR: Unknown setting for rundancy filtration: '%s' (allowed values are ON and OFF)", value));
					return null;
				}
				break;
			case "--force-overwrite":
			case "-f":
				commandLineParameters.forceOverwriteOutputFile = true;
				break;
			case "--thread-count":
			case "-t":
				try {
					commandLineParameters.threadCount = Integer.parseInt(value);
					commandLineParameters.threadCountSpecified = true;
				} catch(NumberFormatException e) {
					System.err.println(String.format("ERROR: Unknown format for number of threads: '%s'", value));
					return null;
				}
				break;
			case "--step-count":
			case "-s":
				try {
					commandLineParameters.stepCount = Integer.parseInt(value);
					commandLineParameters.stepCountSpecified = true;
				} catch(NumberFormatException e) {
					System.err.println(String.format("ERROR: Unknown format for number of steps: '%s'", value));
					return null;
				}
				break;
			case "--recompile-airport-database":
				if(args.length != 1)
					System.err.println("ERROR: Argument '--recompile-airport-database' can only be passed exclusively. ");
				else
					openGUItoRecompileAirportDatabase(JFrame.EXIT_ON_CLOSE);
				return null;
			default:
				System.err.println(String.format("ERROR: Unknown argument: '%s'", argument));
				return null;
			}
		}
		
		return commandLineParameters;
	}
	
	private static void printCommandLineHelp() {
		System.out.println(String.format("ADSbDataParser\nVersion: %s", PROGRAM_VERSION));
		System.out.println(String.format("by %s", PROGRAM_AUTHORS));
		System.out.println("Institute of Flight Guidance, TU Braunschweig\n");

		System.out.println("----------");
		System.out.println("Command line syntax: \n");
		System.out.println("java -jar ADSbDataParser.jar [-m=MODE] [-g] [-i=DIR] [-o=DIR] [-a=DIR] [-r=ON/OFF] [-f] [-t=COUNT] [-s=COUNT]");
		System.out.println("java -jar ADSbDataParser.jar --recompile-airport-database");
		System.out.println("java -jar ADSbDataParser.jar -h\n");
		
		System.out.println("-m, --mode=MODE                     sets the parser-mode (default: FILE)");
		System.out.println("                                    MODE: {FILE, FOLDER}");
		System.out.println("                                      FILE: Parses a single flight within a text-file (trino/CLI- or impala/PuTTY-log-file)");
		System.out.println("                                      FOLDER: Parses all text-files (*.txt and *.log) within the specified input-folder");
		System.out.println("-g, --gui                           opens graphical user interface");
		System.out.println("-i, --input=DIR                     dir to input file/folder");
		System.out.println("-o, --output=DIR                    dir to output file");
		System.out.println("-a, --airport-database=DIR          dir to airport database (optional)");
		System.out.println("-r, --redundancy-filtration=ON/OFF  turns redundancy filtration on/off (default: ON)");
		System.out.println("-f, --force-overwrite               forces file-overwrite of existing output file (default: off, not applicable with arguments -g or --gui)");
		System.out.println("-t, --thread-count=COUNT            number of threads (only for FOLDER-mode, default: [number of processors available to the Java virtual machine] / 2)");
		System.out.println("                                    COUNT: {1 ... n}");
		System.out.println("-s, --step-count=COUNT              number of trajectories per (save-)step (only for FOLDER-mode, default: 2 * [number of processors available to the Java virtual machine])");
		System.out.println("                                    COUNT: {1 ... n}");
		System.out.println("--recompile-airport-database        opens a GUI to recompile a given airport database (not applicable with any other command line argument)");
		System.out.println("-h, --help                          prints this command line help (not applicable with any other command line argument)");
	}
	
	static class TrajectoryMetrics {
		double reliabilityMetric = -1;
		double completenessMetric = -1;
		double plausibilityMetric = -1;
	}
	private static TrajectoryMetrics getTrajectoryMetrics(TrajectoryMerged trajectoryMerged, AirportDatabase airportDatabase) {
		TrajectoryMetrics trajectoryMetrics = new TrajectoryMetrics();
		
		trajectoryMetrics.reliabilityMetric = trajectoryMerged.getReliabilityMetric();
		trajectoryMetrics.completenessMetric = trajectoryMerged.getCompletenessMetric(airportDatabase);
		trajectoryMetrics.plausibilityMetric = trajectoryMerged.getPlausibilityMetric();
		
		return trajectoryMetrics;
	}
	private static boolean appendParsedTrajectoryToFile(TrajectoryMerged trajectoryMerged, TrajectoryMetrics trajectoryMetrics, boolean redundancyFiltered, FileWriter fileWriter, GUI gui) {
		if(trajectoryMerged == null)
			return false;

		try {
			double[] time = trajectoryMerged.getTime();
			double[] lat = trajectoryMerged.getLat();
			double[] lon = trajectoryMerged.getLon();
			double[] baroAlt = trajectoryMerged.getBaroAlt();
			
			if(time.length == 0) {
				System.out.println("Parsed trajectory contains zero samples. ");
				return false;
			}
			
			Formatter formatter;
			
			fileWriter.append("#callsign,icao24\n");
			fileWriter.append(String.format("%s,%s\n", trajectoryMerged.callsign, trajectoryMerged.icao24));
			fileWriter.append("#time [s],lat [deg],lon [deg],baroAlt [ft]\n");
			
			for(int i=0; i<time.length; i++) {
				formatter = new Formatter(new StringBuilder(), Locale.US);
				fileWriter.append(formatter.format("%.2f,%.6f,%.6f,%.2f\n", time[i], lat[i], lon[i], baroAlt[i]).toString());
				formatter.close();
			}
			
			// write parsingParameters ...
			fileWriter.append("#parsingRedundancyFiltered\n");
			if(redundancyFiltered)
				fileWriter.append("true\n");
			else
				fileWriter.append("false\n");
			
			fileWriter.append("#parsingMetrics:reliability,completeness,plausibility\n");
			formatter = new Formatter(new StringBuilder(), Locale.US);
			fileWriter.append(formatter.format("%.4f,%.4f,%.4f\n", trajectoryMetrics.reliabilityMetric, trajectoryMetrics.completenessMetric, trajectoryMetrics.plausibilityMetric).toString());
			formatter.close();

			double[][] flightPhases = trajectoryMerged.getFlightPhases();
			fileWriter.append("#parsingFlightPhases:startTime [s],flightPhase\n");
			for(int j=0; j<flightPhases.length; j++) {
				String flightPhaseString = VerticalFlightPhase.Phase.UNDEFINED.name();
				if(flightPhases[j][1] != -1)
					flightPhaseString = (VerticalFlightPhase.Phase.values()[(int)(flightPhases[j][1]) + 1]).name();
				formatter = new Formatter(new StringBuilder(), Locale.US);
				fileWriter.append(formatter.format("%.2f,%s\n", flightPhases[j][0], flightPhaseString).toString());
				formatter.close();
			}

			fileWriter.append("#parsingReliabilityChannel:time [s],reliability\n");
			double[] reliabilityTime = trajectoryMerged.getReliabilityTime();
			double[] reliability = trajectoryMerged.getReliability();
			for(int j=0; j<reliabilityTime.length; j++) {
				formatter = new Formatter(new StringBuilder(), Locale.US);
				fileWriter.append(formatter.format("%.2f,%.4f\n", reliabilityTime[j], reliability[j]).toString());
				formatter.close();
			}
			// ... write parsingParameters
		} catch (IOException e) {
			showMessage("Error while writing file.", JOptionPane.ERROR_MESSAGE, gui);
			e.printStackTrace();
			return false;
		}
		
		return true;
	}


	private static void openGUI(CommandLineParameters commandLineParameters) {
		new GUI(commandLineParameters);
	}
	
	static class GUI {
		JFrame mainFrame;
		
		JLabel threadCountLabel;
		JTextField threadCountTextField;
		JLabel trajectoryCountPerRoundLabel;
		JTextField trajectoryCountPerRoundTextField;
		
		JLabel parseFromLabel = new JLabel("parse from ");
		JRadioButton fileRadioButton;
		JRadioButton folderRadioButton;
		
		JLabel inputDirLabel;
		JTextField inputDirTextField;
		
		JLabel outputDirLabel;
		JTextField outputDirTextField;
		
		JLabel airportDatabaseDirLabel;
		JTextField airportDatabaseDirTextField;
		
		JLabel filterRedundantSamplesLabel;
		JCheckBox filterRedundantSamplesCheckBox;
		
		JButton parseStartStopButton;
		
		CommandLineParameters commandLineParameters;
		
		JLabel statusMessageLabel = new JLabel(" ");
		JProgressBar progressBar = new JProgressBar(0, 100);
		
		public boolean stopParsing = false;
		
		public GUI(CommandLineParameters commandLineParameters) {
			this.commandLineParameters = commandLineParameters;
			mainFrame = new JFrame("ADSbDataParser");
			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			JPanel mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
			
			threadCountLabel = new JLabel(" number of threads");
			threadCountTextField = new JTextField(String.format("%d", commandLineParameters.threadCount));
			trajectoryCountPerRoundLabel = new JLabel(" number of trajectories per calculation-round");
			trajectoryCountPerRoundTextField = new JTextField(String.format("%d", commandLineParameters.stepCount));

			fileRadioButton = new JRadioButton("file");
			folderRadioButton = new JRadioButton("folder");
			if(commandLineParameters.mode == MODE.FILE) {
				fileRadioButton.setSelected(true);
				
				threadCountLabel.setVisible(false);
				threadCountTextField.setVisible(false);
				trajectoryCountPerRoundLabel.setVisible(false);
				trajectoryCountPerRoundTextField.setVisible(false);
			} else
				folderRadioButton.setSelected(true);
			ButtonGroup fileFolderButtonGroup = new ButtonGroup();
			fileFolderButtonGroup.add(fileRadioButton);
			fileFolderButtonGroup.add(folderRadioButton);
			
			JPanel upperPanel = new JPanel();
			GridBagLayout upperPanelGridBagLayout = new GridBagLayout();
			upperPanel.setLayout(upperPanelGridBagLayout);
			
			JLabel programInfoLabel = new JLabel("  [?]  ");
			programInfoLabel.setFont(programInfoLabel.getFont().deriveFont(Font.PLAIN));
			programInfoLabel.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {
					String programInfoMessage = String.format("ADSbDataParser\n"
							+ "----------\n"
							+ "Version: %s\n"
							+ "by %s\n"
							+ "Institute of Flight Guidance, TU Braunschweig", PROGRAM_VERSION, PROGRAM_AUTHORS);
					JOptionPane.showMessageDialog(null, programInfoMessage, "ADSbDataParser", JOptionPane.INFORMATION_MESSAGE);
				}

				@Override
				public void mousePressed(MouseEvent e) {}

				@Override
				public void mouseReleased(MouseEvent e) {}

				@Override
				public void mouseEntered(MouseEvent e) {
					((JLabel)e.getSource()).setFont(((JLabel)e.getSource()).getFont().deriveFont(Font.BOLD));
				}

				@Override
				public void mouseExited(MouseEvent e) {
					((JLabel)e.getSource()).setFont(((JLabel)e.getSource()).getFont().deriveFont(Font.PLAIN));
				}
			});

			GridBagConstraints gridBagConstraintsProgramInfoLabel = new GridBagConstraints();
			gridBagConstraintsProgramInfoLabel.fill = GridBagConstraints.NONE;
			gridBagConstraintsProgramInfoLabel.gridx = 0;
			gridBagConstraintsProgramInfoLabel.gridy = 0;
			gridBagConstraintsProgramInfoLabel.gridwidth = 1;
			gridBagConstraintsProgramInfoLabel.gridheight = 1;
			gridBagConstraintsProgramInfoLabel.weightx = 0;
			gridBagConstraintsProgramInfoLabel.weighty = 0;
			upperPanelGridBagLayout.setConstraints(programInfoLabel, gridBagConstraintsProgramInfoLabel);
			upperPanel.add(programInfoLabel);
			
			JPanel selectionFileFolderPanel = new JPanel();
			selectionFileFolderPanel.setLayout(new BoxLayout(selectionFileFolderPanel, BoxLayout.X_AXIS));
			
			selectionFileFolderPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
			selectionFileFolderPanel.add(parseFromLabel);
			selectionFileFolderPanel.add(fileRadioButton);
			selectionFileFolderPanel.add(folderRadioButton);
			
			GridBagConstraints gridBagConstraintsSelectionFileFolderPanel = new GridBagConstraints();
			gridBagConstraintsSelectionFileFolderPanel.fill = GridBagConstraints.NONE;
			gridBagConstraintsSelectionFileFolderPanel.gridx = 1;
			gridBagConstraintsSelectionFileFolderPanel.gridy = 0;
			gridBagConstraintsSelectionFileFolderPanel.gridwidth = 1;
			gridBagConstraintsSelectionFileFolderPanel.gridheight = 1;
			gridBagConstraintsSelectionFileFolderPanel.weightx = 1;
			gridBagConstraintsSelectionFileFolderPanel.weighty = 0;
			upperPanelGridBagLayout.setConstraints(selectionFileFolderPanel, gridBagConstraintsSelectionFileFolderPanel);
			upperPanel.add(selectionFileFolderPanel);

			inputDirLabel = new JLabel(" raw file dir");
			inputDirTextField = new JTextField("");
			if(commandLineParameters.inputDir != null)
				inputDirTextField.setText(commandLineParameters.inputDir);
			
			inputDirTextField.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent me) {
					if(!((JTextField)me.getSource()).isEnabled())
						return;
					
					JFileChooser fileChooser = new JFileChooser();
					if((new File(inputDirTextField.getText())).exists())
						if(folderRadioButton.isSelected())
							fileChooser.setSelectedFile(new File(inputDirTextField.getText().concat("\\*")));
						else
							fileChooser.setSelectedFile(new File(inputDirTextField.getText()));
					if(fileRadioButton.isSelected())
						fileChooser.setFileFilter(new FileNameExtensionFilter("Raw Flight-Data (*.txt, *.log)", new String[] {"txt", "log"}));
					if(folderRadioButton.isSelected())
						fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
						inputDirTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
				}

				@Override
				public void mouseEntered(MouseEvent arg0) {}

				@Override
				public void mouseExited(MouseEvent arg0) {}

				@Override
				public void mousePressed(MouseEvent arg0) {}

				@Override
				public void mouseReleased(MouseEvent arg0) {}
			});

			fileRadioButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if(fileRadioButton.isSelected()) {
						inputDirLabel.setText(" raw file dir");
						inputDirTextField.setText("");

						threadCountLabel.setVisible(false);
						threadCountTextField.setVisible(false);
						trajectoryCountPerRoundLabel.setVisible(false);
						trajectoryCountPerRoundTextField.setVisible(false);
					}
				}
			});

			folderRadioButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if(folderRadioButton.isSelected()) {
						inputDirLabel.setText(" raw folder dir");
						inputDirTextField.setText("");

						threadCountLabel.setVisible(true);
						threadCountTextField.setVisible(true);
						trajectoryCountPerRoundLabel.setVisible(true);
						trajectoryCountPerRoundTextField.setVisible(true);
					}
				}
			});

			outputDirLabel = new JLabel(" output file dir");
			outputDirTextField = new JTextField("");
			if(commandLineParameters.outputDir != null)
				outputDirTextField.setText(commandLineParameters.outputDir);

			outputDirTextField.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent me) {
					if(!((JTextField)me.getSource()).isEnabled())
						return;
					
					JFileChooser fileChooser = new JFileChooser();
					if(outputDirTextField.getText().contains(".atttfc"))
						fileChooser.setSelectedFile(new File(outputDirTextField.getText()));
					fileChooser.setFileFilter(new FileNameExtensionFilter("Traffic Scenario (*.atttfc)", "atttfc"));
					if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
						String absoluteSaveFilePath = fileChooser.getSelectedFile().getAbsolutePath();
						if(!absoluteSaveFilePath.endsWith(".atttfc"))
							absoluteSaveFilePath = absoluteSaveFilePath.concat(".atttfc");
						outputDirTextField.setText(absoluteSaveFilePath);
					}
				}

				@Override
				public void mouseEntered(MouseEvent arg0) {}

				@Override
				public void mouseExited(MouseEvent arg0) {}

				@Override
				public void mousePressed(MouseEvent arg0) {}

				@Override
				public void mouseReleased(MouseEvent arg0) {}
			});

			airportDatabaseDirLabel = new JLabel(" airport database file dir (optional) ");
			airportDatabaseDirTextField = new JTextField("");
			if(commandLineParameters.airportDatabaseFileDir != null)
				airportDatabaseDirTextField.setText(commandLineParameters.airportDatabaseFileDir);

			airportDatabaseDirTextField.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent me) {
					if(!((JTextField)me.getSource()).isEnabled())
						return;
					
					JFileChooser fileChooser = new JFileChooser();
					if((new File(airportDatabaseDirTextField.getText())).exists())
						fileChooser.setSelectedFile(new File(airportDatabaseDirTextField.getText()));
					fileChooser.setFileFilter(new FileNameExtensionFilter("Airport Database (*.attapt)", "attapt"));
					if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
						airportDatabaseDirTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
				}

				@Override
				public void mouseEntered(MouseEvent arg0) {}

				@Override
				public void mouseExited(MouseEvent arg0) {}

				@Override
				public void mousePressed(MouseEvent arg0) {}

				@Override
				public void mouseReleased(MouseEvent arg0) {}
			});
			
			filterRedundantSamplesLabel = new JLabel(" redundancy filtration (state interpolation required)");
			filterRedundantSamplesCheckBox = new JCheckBox();
			filterRedundantSamplesCheckBox.setSelected(commandLineParameters.filterRedundantSamples);

			JPanel inOutputPanel = new JPanel();
			inOutputPanel.setLayout(new GridLayout(6, 2, 4, 4));
			
			JPanel airportDatabaseDirLabelPanel = new JPanel();
			airportDatabaseDirLabelPanel.setLayout(new BoxLayout(airportDatabaseDirLabelPanel, BoxLayout.X_AXIS));
			JLabel openRecompileAirportDatabaseGuiLabel = new JLabel("[open recompiler]");
			openRecompileAirportDatabaseGuiLabel.setForeground(Color.BLUE);
			openRecompileAirportDatabaseGuiLabel.setFont(openRecompileAirportDatabaseGuiLabel.getFont().deriveFont(Font.PLAIN));
			airportDatabaseDirLabelPanel.add(airportDatabaseDirLabel);
			airportDatabaseDirLabelPanel.add(openRecompileAirportDatabaseGuiLabel);
			airportDatabaseDirLabelPanel.add(new JLabel("    "));
			
			openRecompileAirportDatabaseGuiLabel.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {
					openGUItoRecompileAirportDatabase(JFrame.DISPOSE_ON_CLOSE);
				}

				@Override
				public void mousePressed(MouseEvent e) {}

				@Override
				public void mouseReleased(MouseEvent e) {}

				@Override
				public void mouseEntered(MouseEvent e) {
					((JLabel)e.getSource()).setFont(((JLabel)e.getSource()).getFont().deriveFont(Font.BOLD));
				}

				@Override
				public void mouseExited(MouseEvent e) {
					((JLabel)e.getSource()).setFont(((JLabel)e.getSource()).getFont().deriveFont(Font.PLAIN));
				}
			});
			
			inOutputPanel.add(inputDirLabel);
			inOutputPanel.add(inputDirTextField);
			inOutputPanel.add(outputDirLabel);
			inOutputPanel.add(outputDirTextField);
			inOutputPanel.add(airportDatabaseDirLabelPanel);
			inOutputPanel.add(airportDatabaseDirTextField);
			inOutputPanel.add(filterRedundantSamplesLabel);
			inOutputPanel.add(filterRedundantSamplesCheckBox);
			
			inOutputPanel.add(threadCountLabel);
			inOutputPanel.add(threadCountTextField);
			inOutputPanel.add(trajectoryCountPerRoundLabel);
			inOutputPanel.add(trajectoryCountPerRoundTextField);

			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			buttonPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
			
			parseStartStopButton = new JButton("parse");
			
			buttonPanel.add(parseStartStopButton);
			
			JPanel statusMessagePanel = new JPanel();
			statusMessagePanel.setLayout(new BoxLayout(statusMessagePanel, BoxLayout.X_AXIS));
			statusMessagePanel.add(statusMessageLabel);
			
			mainPanel.add(upperPanel);
			mainPanel.add(inOutputPanel);
			mainPanel.add(statusMessagePanel);
			mainPanel.add(progressBar);
			mainPanel.add(buttonPanel);
			
			mainFrame.add(mainPanel);
			
			int screenWidth = (int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth());
			int screenHeight = (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight());
			
			mainFrame.pack();
			// Prevent mainFrame from expanding too large (due to content of TextFields): 
			if(mainFrame.getWidth() > (int)Math.round(0.8*screenWidth))
				mainFrame.setSize((int)Math.round(0.8*screenWidth), mainFrame.getHeight());
			
			mainFrame.setLocation((screenWidth-mainFrame.getWidth())/2, (screenHeight-mainFrame.getHeight())/2);
			
			mainFrame.setVisible(true);
			
			parseStartStopButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ae) {
					if(((JButton)ae.getSource()).getText().equals("stop")) {
						String[] stopParsingRequestOptions = {"stop parsing", "continue parsing"};
						int selection = JOptionPane.showOptionDialog(null, "Are you sure you want to stop the parsing process? \nA resumption is not possible. ", "ADSbDataParser", JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, stopParsingRequestOptions, stopParsingRequestOptions[1]);
						if(!(selection == 0))
							return;
						
						stopParsing = true;
						((JButton)ae.getSource()).setText("stopping ...");
						((JButton)ae.getSource()).setEnabled(false);
						return;
					}
					// Should not be reachable (button not enabled), but left for completeness: 
					if(((JButton)ae.getSource()).getText().equals("stopping ..."))
						return;
					
					boolean parseFileTrueFolderFalse = fileRadioButton.isSelected();
					String inputDir = inputDirTextField.getText();
					String outputDir = outputDirTextField.getText();
					String airportDatabaseDir = null;
					if(!airportDatabaseDirTextField.getText().isBlank())
						airportDatabaseDir = airportDatabaseDirTextField.getText();
					int threadCount = -1;
					int trajectoryCountPerRound = -1;

					if(parseFileTrueFolderFalse)
						GUI.this.commandLineParameters.mode = MODE.FILE;
					else
						GUI.this.commandLineParameters.mode = MODE.FOLDER;
					
					// Check if TrafficScenario-file to be written already exists ...
					GUI.this.commandLineParameters.forceOverwriteOutputFile = false;	// Parameter -f shall have no effect on GUI
					if((new File(outputDir)).exists()) {
						if(!openRequestWindowOverwriteFile(outputDir))
							return;
						else
							GUI.this.commandLineParameters.forceOverwriteOutputFile = true;
					}
					// ... Check if TrafficScenario-file to be written already exists

					if(inputDir.isBlank()) {
						if(parseFileTrueFolderFalse)
							JOptionPane.showMessageDialog(null, "Please specify input file. ", "ADSbDataParser", JOptionPane.INFORMATION_MESSAGE);
						else
							JOptionPane.showMessageDialog(null, "Please specify input folder. ", "ADSbDataParser", JOptionPane.INFORMATION_MESSAGE);
						return;
					}
					GUI.this.commandLineParameters.inputDir = inputDir;
					if(outputDir.isBlank()) {
						JOptionPane.showMessageDialog(null, "Please specify TrafficScenario output file. ", "ADSbDataParser", JOptionPane.INFORMATION_MESSAGE);
						return;
					}
					GUI.this.commandLineParameters.outputDir = outputDir;
					if((airportDatabaseDir != null) && !(new File(airportDatabaseDir)).exists()) {
						JOptionPane.showMessageDialog(null, "Specified airport database file does not exist. ", "ADSbDataParser", JOptionPane.WARNING_MESSAGE);
						return;
					}
					GUI.this.commandLineParameters.airportDatabaseFileDir = airportDatabaseDir;
					
					GUI.this.commandLineParameters.filterRedundantSamples = filterRedundantSamplesCheckBox.isSelected();
					
					if(!parseFileTrueFolderFalse) {
						try {
							threadCount = Integer.parseInt(threadCountTextField.getText());
						} catch(Exception e) {
							JOptionPane.showMessageDialog(null, "Unknown number-format for thread count. ", "ADSbDataParser", JOptionPane.WARNING_MESSAGE);
							return;
						}
						try {
							trajectoryCountPerRound = Integer.parseInt(trajectoryCountPerRoundTextField.getText());
						} catch(Exception e) {
							JOptionPane.showMessageDialog(null, "Unknown number-format for step count. ", "ADSbDataParser", JOptionPane.WARNING_MESSAGE);
							return;
						}
						
						if(threadCount <= 0) {
							JOptionPane.showMessageDialog(null, "Thread count must be greater than zero. ", "ADSbDataParser", JOptionPane.WARNING_MESSAGE);
							return;
						}
						if(trajectoryCountPerRound <= 0) {
							JOptionPane.showMessageDialog(null, "Step count must be greater than zero. ", "ADSbDataParser", JOptionPane.WARNING_MESSAGE);
							return;
						}
						
						GUI.this.commandLineParameters.threadCount = threadCount;
						GUI.this.commandLineParameters.stepCount = trajectoryCountPerRound;
					}
					
					setGUIparserRunning(GUI.this.commandLineParameters);
					parse(GUI.this.commandLineParameters, GUI.this);
				}
			});
		}
		
		public void setProgress(int parsedTrajectoriesCount, int totalTrajectoriesCount) {
			progressBar.setMinimum(0);
			progressBar.setMaximum(totalTrajectoriesCount);
			progressBar.setValue(parsedTrajectoriesCount);
			mainFrame.validate();
			mainFrame.repaint();
		}
		
		public void setStatusMessage(String message) {
			statusMessageLabel.setText(message);
			mainFrame.validate();
			mainFrame.repaint();
		}
		
		private void setGUIparserRunning(CommandLineParameters commandLineParameters) {
			parseFromLabel.setEnabled(false);
			fileRadioButton.setEnabled(false);
			folderRadioButton.setEnabled(false);
			inputDirLabel.setEnabled(false);
			inputDirTextField.setEnabled(false);
			outputDirLabel.setEnabled(false);
			outputDirTextField.setEnabled(false);
			airportDatabaseDirLabel.setEnabled(false);
			airportDatabaseDirTextField.setEnabled(false);
			filterRedundantSamplesLabel.setEnabled(false);
			filterRedundantSamplesCheckBox.setEnabled(false);
			threadCountLabel.setEnabled(false);
			threadCountTextField.setEnabled(false);
			trajectoryCountPerRoundLabel.setEnabled(false);
			trajectoryCountPerRoundTextField.setEnabled(false);
			
			if(commandLineParameters.mode == MODE.FOLDER)
				parseStartStopButton.setText("stop");
			else
				parseStartStopButton.setEnabled(false);

			mainFrame.validate();
			mainFrame.repaint();
		}
		
		public void parsingFinished() {
			statusMessageLabel.setText(" ");
			progressBar.setMinimum(0);
			progressBar.setMaximum(100);
			progressBar.setValue(0);

			parseFromLabel.setEnabled(true);
			fileRadioButton.setEnabled(true);
			folderRadioButton.setEnabled(true);
			inputDirLabel.setEnabled(true);
			inputDirTextField.setEnabled(true);
			outputDirLabel.setEnabled(true);
			outputDirTextField.setEnabled(true);
			airportDatabaseDirLabel.setEnabled(true);
			airportDatabaseDirTextField.setEnabled(true);
			filterRedundantSamplesLabel.setEnabled(true);
			filterRedundantSamplesCheckBox.setEnabled(true);
			threadCountLabel.setEnabled(true);
			threadCountTextField.setEnabled(true);
			trajectoryCountPerRoundLabel.setEnabled(true);
			trajectoryCountPerRoundTextField.setEnabled(true);

			stopParsing = false;
			parseStartStopButton.setText("parse");
			parseStartStopButton.setEnabled(true);

			mainFrame.validate();
			mainFrame.repaint();
		}

		private static boolean openRequestWindowOverwriteFile(String fileDir) {
			String requestMessage = String.format("The file '%s' already exists. \n"
					+ "Do you want to overwrite it? ", fileDir);
			int selection = JOptionPane.showOptionDialog(null, requestMessage, "ADSbDataParser", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
			if(selection == JOptionPane.YES_OPTION)
				return true;
			return false;
		}
	}

	private static void showMessage(String message, int messageType, GUI gui) {
		if(gui != null)
			JOptionPane.showMessageDialog(null, message, "ADSbDataParser", messageType);
		else
			System.out.println(message);
	}
	private static int lastProgressParsedTrajectoriesCount = -1;
	private static void showProgress(int parsedTrajectoriesCount, int totalTrajectoriesCount, GUI gui) {
		if(parsedTrajectoriesCount == lastProgressParsedTrajectoriesCount)
			return;
		lastProgressParsedTrajectoriesCount = parsedTrajectoriesCount;
		
		String statusMessage = String.format("parsing files (%d / %d) ...", parsedTrajectoriesCount, totalTrajectoriesCount);
		
		if(gui != null) {
			gui.setProgress(parsedTrajectoriesCount, totalTrajectoriesCount);
			gui.setStatusMessage(statusMessage);
		} else
			System.out.println(statusMessage);
	}
	
	private static void parse(CommandLineParameters commandLineParameters, GUI gui) {
		if(commandLineParameters.inputDir == null) {
			System.err.println("ERROR: Input dir not specified. ");
			return;
		}
		if(commandLineParameters.outputDir == null) {
			System.err.println("ERROR: Output dir not specified. ");
			return;
		}
		if((commandLineParameters.airportDatabaseFileDir != null) && !(new File(commandLineParameters.airportDatabaseFileDir)).exists()) {
			System.err.println("ERROR: Specified airport database file does not exist. ");
			return;
		}
		if(!commandLineParameters.forceOverwriteOutputFile && (new File(commandLineParameters.outputDir)).exists()) {
			System.err.println(String.format("ERROR: Output file '%s' already exists. \n"
					+ "Use argument '-f' to force overwrite. ", commandLineParameters.outputDir));
			return;
		}
		if((commandLineParameters.mode == MODE.FOLDER) && (commandLineParameters.threadCount <= 0)) {
			System.err.println("ERROR: Thread count cannot be less than or equal to 0. ");
			return;
		}
		if((commandLineParameters.mode == MODE.FOLDER) && (commandLineParameters.stepCount <= 0)) {
			System.err.println("ERROR: Step count cannot be less than or equal to 0. ");
			return;
		}
		
		switch(commandLineParameters.mode) {
		case FILE:
			parseTrajectoryFile(commandLineParameters, gui);
			break;
		case FOLDER:
			parseTrajectoriesFolder(commandLineParameters, gui);
			break;
		}
	}

	private static void parseTrajectoryFile(CommandLineParameters commandLineParameters, GUI gui) {
		String inputFileDir = commandLineParameters.inputDir;
		String outputFileDir = commandLineParameters.outputDir;
		AirportDatabase airportDatabase = null;
		if(commandLineParameters.airportDatabaseFileDir != null) {
			airportDatabase = AirportDatabase.readInAirportDatabase(commandLineParameters.airportDatabaseFileDir);
			if(airportDatabase == null) {
				showMessage("Error while reading airport database file. ", JOptionPane.ERROR_MESSAGE, gui);
				return;
			}
		} else
			showMessage("No airport database file dir set. Will continue without airport database. ", JOptionPane.INFORMATION_MESSAGE, gui);
		boolean filterRedundantSamples = commandLineParameters.filterRedundantSamples;
		
		TrajectoryStateVectorsData4 trajectory = new TrajectoryStateVectorsData4();
		int errorCode = trajectory.readInInterfaceDataFile(inputFileDir);
		if(errorCode < 0) {
			showMessage("Error while reading data file. ", JOptionPane.ERROR_MESSAGE, gui);
			return;
		}

		TrajectoryVertical trajectoryVertical = new TrajectoryVertical();
		TrajectoryHorizontal trajectoryHorizontal = new TrajectoryHorizontal();
		TrajectoryMerged trajectoryMerged = new TrajectoryMerged();
		
		if(trajectoryVertical.setRawTrajectoryFromStateVectorsData4(trajectory) != 0) {
			showMessage("Error while setting vertical trajectory. ", JOptionPane.ERROR_MESSAGE, gui);
			return;
		}
		if(trajectoryVertical.parseTrajectory(filterRedundantSamples) != 0) {
			showMessage("Error while parsing vertical trajectory. ", JOptionPane.ERROR_MESSAGE, gui);
			return;
		}

		if(trajectoryHorizontal.setRawTrajectoryFromStateVectorsData4(trajectory) != 0) {
			showMessage("Error while setting horizontal trajectory. ", JOptionPane.ERROR_MESSAGE, gui);
			return;
		}
		if(trajectoryHorizontal.parseTrajectory(filterRedundantSamples) != 0) {
			showMessage("Error while parsing horizontal trajectory. ", JOptionPane.ERROR_MESSAGE, gui);
			return;
		}

		trajectoryMerged.setTrajectories(trajectoryVertical, trajectoryHorizontal);
		if(trajectoryMerged.mergeTrajectories() != 0) {
			showMessage("Error while merging trajectory-planes. ", JOptionPane.ERROR_MESSAGE, gui);
			return;
		}
		
		// Write parsed trajectory to file ...
		try {
			if(trajectoryMerged.getTime().length == 0) {
				showMessage("Parsed trajectory contains zero samples, output-file will not be created/ saved. ", JOptionPane.INFORMATION_MESSAGE, gui);
				return;
			}
			
			FileWriter fileWriter = new FileWriter(outputFileDir);
			fileWriter.write("");		// clear file
			TrajectoryMetrics trajectoryMetrics = getTrajectoryMetrics(trajectoryMerged, airportDatabase);
			appendParsedTrajectoryToFile(trajectoryMerged, trajectoryMetrics, filterRedundantSamples, fileWriter, gui);
			fileWriter.close();
		} catch (IOException e) {
			showMessage("Error while writing file. ", JOptionPane.ERROR_MESSAGE, gui);
			e.printStackTrace();
			return;
		}
		// ... Write parsed trajectory to file
		
		showMessage("Parsing trajectory finished. ", JOptionPane.INFORMATION_MESSAGE, gui);
		if(gui != null)
			gui.parsingFinished();
	}
	
	static String[] rawFileDirs;
	static int folderParsingFinalRoundProgress;
	static boolean folderParsingfinished;
	private static void parseTrajectoriesFolder(CommandLineParameters commandLineParameters, final GUI gui) {
		final String inputFolderDir = commandLineParameters.inputDir;
		final String outputFileDir = commandLineParameters.outputDir;
		final int nThreads = commandLineParameters.threadCount;
		final int trajectoryCountPerRound = commandLineParameters.stepCount;
		AirportDatabase airportDatabaseLoad = null;
		if(commandLineParameters.airportDatabaseFileDir != null) {
			airportDatabaseLoad = AirportDatabase.readInAirportDatabase(commandLineParameters.airportDatabaseFileDir);
			if(airportDatabaseLoad == null) {
				showMessage("Error while reading airport database file. ", JOptionPane.ERROR_MESSAGE, gui);
				return;
			}
		} else
			showMessage("No airport database file dir set. Will continue without airport database. ", JOptionPane.INFORMATION_MESSAGE, gui);
		final AirportDatabase airportDatabase = airportDatabaseLoad;
		final boolean filterRedundantSamples = commandLineParameters.filterRedundantSamples;

		Thread folderParsingThread = new Thread() {
			@Override
			public void run() {
				final ParallelParser parallelParser = new ParallelParser();
				
				File[] rawFiles = (new File(inputFolderDir)).listFiles();
				ArrayList<String> rawFileDirsList = new ArrayList<String>();
				
				for(int i=0; i<rawFiles.length; i++)
					if(rawFiles[i].isFile())
						if(rawFiles[i].getAbsolutePath().endsWith(".txt") || rawFiles[i].getAbsolutePath().endsWith(".log"))
							rawFileDirsList.add(rawFiles[i].getAbsolutePath());
				
				rawFileDirs = new String[rawFileDirsList.size()];
				for(int i=0; i<rawFileDirsList.size(); i++)
					rawFileDirs[i] = rawFileDirsList.get(i);
				
				int numberOfParsedTrajectories = 0;
				folderParsingFinalRoundProgress = 0;
				String[] currentRawFileDirs;
				
				folderParsingfinished = false;
				Thread progressUpdateThread = new Thread() {
					@Override
					public void run() {
						while(true) {
							if(folderParsingfinished)
								break;
							if((gui != null) && gui.stopParsing) {
								parallelParser.stopParsing();
								break;
							}
							if(!parallelParser.isFinished()) {
								int parsedTrajectoriesCount = folderParsingFinalRoundProgress + parallelParser.getNumberOfParsedTrajectories();
								showProgress(parsedTrajectoriesCount, rawFileDirs.length, gui);
							}
							try {
								Thread.sleep(100);
							} catch(Exception e) {}
						}
					}
				};
				progressUpdateThread.start();
				
				try {
					FileWriter fileWriter = new FileWriter(outputFileDir);
					fileWriter.write("");		// clear file
					
					while(numberOfParsedTrajectories < rawFileDirs.length) {
						if(rawFileDirs.length-numberOfParsedTrajectories >= trajectoryCountPerRound)
							currentRawFileDirs = new String[trajectoryCountPerRound];
						else
							currentRawFileDirs = new String[rawFileDirs.length-numberOfParsedTrajectories];
						
						for(int i=0; i<currentRawFileDirs.length; i++) {
							currentRawFileDirs[i] = rawFileDirs[numberOfParsedTrajectories];
							numberOfParsedTrajectories++;
						}
						
						parallelParser.setDirs(currentRawFileDirs);
						parallelParser.setAirportDatabase(airportDatabase);
						parallelParser.parseAll(nThreads, filterRedundantSamples);

						if((gui != null) && gui.stopParsing)
							break;

						// Save parsed trajectories to file ...
						for(int i=0; i<currentRawFileDirs.length; i++) {
							if(parallelParser.getErrorCode(i) < 0)
								continue;
							
							TrajectoryMerged trajectoryMerged = parallelParser.getTrajectoryMerged(i);
							// Trajectory metrics already calculated in parallel mode: 
							TrajectoryMetrics trajectoryMetrics = new TrajectoryMetrics();
							trajectoryMetrics.reliabilityMetric = parallelParser.getReliabilityMetric(i);
							trajectoryMetrics.completenessMetric = parallelParser.getCompletenessMetric(i);
							trajectoryMetrics.plausibilityMetric = parallelParser.getPlausibilityMetric(i);
							
							if(trajectoryMerged.getTime().length > 0)
								appendParsedTrajectoryToFile(trajectoryMerged, trajectoryMetrics, filterRedundantSamples, fileWriter, gui);
						}
						// ... Save parsed trajectories to file

						if((gui != null) && gui.stopParsing)
							break;

						folderParsingFinalRoundProgress = numberOfParsedTrajectories;
						showProgress(numberOfParsedTrajectories, rawFileDirs.length, gui);
					}
					
					fileWriter.close();
					
					folderParsingfinished = true;
				} catch (IOException e) {
					showMessage("Error while writing file. ", JOptionPane.ERROR_MESSAGE, gui);
					e.printStackTrace();
					return;
				}
				
				if((gui == null) || !gui.stopParsing)
					showMessage("Parsing trajectories finished. ", JOptionPane.INFORMATION_MESSAGE, gui);
				else
					showMessage(String.format("Parsing trajectories stopped. \n"
							+ "Generated TrafficScenario file may be incomplete. \n"
							+ "(%s)", outputFileDir), JOptionPane.WARNING_MESSAGE, gui);
				
				if(gui != null)
					gui.parsingFinished();
			}
		};
		folderParsingThread.start();
	}
	
	private static void openGUItoRecompileAirportDatabase(int defaultCloseOperation) {
		class GUIrecompileAirportDatabase {
			JFrame mainFrame;
			
			JTextField inputFileDirTextField;
			JTextField outputFileDirTextField;
			JTextField fieldIndicesTextField;
			JTextField headerLinesCountTextField;
			JTextField separationCharTextField;
			JTextField quotationCharTextField;
			JTextField replaceTextField;
			JTextField replacementTextField;
			JTextField latLonAreInRadianTextField;
			JTextField elevationIsInFeetTextField;
			
			public GUIrecompileAirportDatabase(int defaultCloseOperation) {
				mainFrame = new JFrame("ADSbDataParser");
				mainFrame.setDefaultCloseOperation(defaultCloseOperation);
		
				JPanel mainPanel = new JPanel();
				mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

				JLabel pageTitleLabel = new JLabel("Recompile airport database");
				pageTitleLabel.setAlignmentX(JButton.CENTER_ALIGNMENT);
				mainPanel.add(pageTitleLabel);
				
				JPanel inOutputPanel = new JPanel();
				inOutputPanel.setLayout(new GridLayout(10, 2, 4, 4));
				
				inputFileDirTextField = new JTextField();
				outputFileDirTextField = new JTextField();
				fieldIndicesTextField = new JTextField("{3, 10, 8, 13, 1, 4, 5, 6}");
				headerLinesCountTextField = new JTextField("1");
				separationCharTextField = new JTextField(",");
				quotationCharTextField = new JTextField("\"");
				replaceTextField = new JTextField("{\"\\\"\"}");
				replacementTextField = new JTextField("{\"\"}");
				latLonAreInRadianTextField = new JTextField("false");
				elevationIsInFeetTextField = new JTextField("true");

				inOutputPanel.add(new JLabel(" input file dir"));
				inOutputPanel.add(inputFileDirTextField);
				inOutputPanel.add(new JLabel(" output file dir"));
				inOutputPanel.add(outputFileDirTextField);
				inOutputPanel.add(new JLabel(" field indices"));
				inOutputPanel.add(fieldIndicesTextField);
				inOutputPanel.add(new JLabel(" number of header lines"));
				inOutputPanel.add(headerLinesCountTextField);
				inOutputPanel.add(new JLabel(" separation char"));
				inOutputPanel.add(separationCharTextField);
				inOutputPanel.add(new JLabel(" quotation char"));
				inOutputPanel.add(quotationCharTextField);
				inOutputPanel.add(new JLabel(" replace"));
				inOutputPanel.add(replaceTextField);
				inOutputPanel.add(new JLabel(" replacement"));
				inOutputPanel.add(replacementTextField);
				inOutputPanel.add(new JLabel(" initial lat/lon are in radian (else: degree)"));
				inOutputPanel.add(latLonAreInRadianTextField);
				inOutputPanel.add(new JLabel(" initial elevation is in feet (else: meter)"));
				inOutputPanel.add(elevationIsInFeetTextField);
				
				inputFileDirTextField.addMouseListener(new MouseListener() {
					@Override
					public void mouseClicked(MouseEvent me) {
						JFileChooser fileChooser = new JFileChooser();
						if((new File(inputFileDirTextField.getText())).exists())
								fileChooser.setSelectedFile(new File(inputFileDirTextField.getText()));
						fileChooser.setFileFilter(new FileNameExtensionFilter("Airport Database (*.csv)", "csv"));
						if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
							inputFileDirTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
					}

					@Override
					public void mouseEntered(MouseEvent arg0) {}

					@Override
					public void mouseExited(MouseEvent arg0) {}

					@Override
					public void mousePressed(MouseEvent arg0) {}

					@Override
					public void mouseReleased(MouseEvent arg0) {}
				});
				
				outputFileDirTextField.addMouseListener(new MouseListener() {
					@Override
					public void mouseClicked(MouseEvent me) {
						JFileChooser fileChooser = new JFileChooser();
						if(outputFileDirTextField.getText().contains(".attapt"))
							fileChooser.setSelectedFile(new File(outputFileDirTextField.getText()));
						fileChooser.setFileFilter(new FileNameExtensionFilter("Airport Database (recompiled) (*.attapt)", "attapt"));
						if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
							String absoluteSaveFilePath = fileChooser.getSelectedFile().getAbsolutePath();
							if(!absoluteSaveFilePath.endsWith(".attapt"))
								absoluteSaveFilePath = absoluteSaveFilePath.concat(".attapt");
							outputFileDirTextField.setText(absoluteSaveFilePath);
						}
					}

					@Override
					public void mouseEntered(MouseEvent arg0) {}

					@Override
					public void mouseExited(MouseEvent arg0) {}

					@Override
					public void mousePressed(MouseEvent arg0) {}

					@Override
					public void mouseReleased(MouseEvent arg0) {}
				});
				
				mainPanel.add(inOutputPanel);

				JButton recompileButton = new JButton("recompile airport database");
				recompileButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
				mainPanel.add(recompileButton);

				mainFrame.add(mainPanel);
				mainFrame.pack();

				int screenWidth = (int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth());
				int screenHeight = (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight());
				mainFrame.setLocation((screenWidth-mainFrame.getWidth())/2, (screenHeight-mainFrame.getHeight())/2);

				mainFrame.setVisible(true);
				
				recompileButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						String inputFileDir;
						String outputFileDir;
						int[] fieldIndices = new int[8];
						int headerLinesCount;
						char separationChar;
						char quotationChar;
						String[] replace;
						String[] replacement;
						boolean latLonAreInRadian;
						boolean elevationIsInFeet;
						
						inputFileDir = inputFileDirTextField.getText();
						outputFileDir = outputFileDirTextField.getText();
						
						if(inputFileDir.isBlank()) {
							JOptionPane.showMessageDialog(null, "Please specify input file dir. ", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
							return;
						}
						if(!(new File(inputFileDir)).exists()) {
							JOptionPane.showMessageDialog(null, "Error: Specified input file does not exist. ", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						if(outputFileDir.isBlank()) {
							JOptionPane.showMessageDialog(null, "Please specify output file dir. ", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
							return;
						}
						if((new File(outputFileDir)).exists()) {
							JOptionPane.showMessageDialog(null, "Error: Output file already exists. ", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						String[] fieldIndicesStrings = fieldIndicesTextField.getText().replaceAll("\\{", "").replaceAll("\\}", "").replaceAll(" ", "").split(",");
						if(fieldIndicesStrings.length != 8) {
							JOptionPane.showMessageDialog(null, "Field indices array needs to be of size 8. ", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
							return;
						}
						try {
							fieldIndices[0] = Integer.parseInt(fieldIndicesStrings[0]);
							fieldIndices[1] = Integer.parseInt(fieldIndicesStrings[1]);
							fieldIndices[2] = Integer.parseInt(fieldIndicesStrings[2]);
							fieldIndices[3] = Integer.parseInt(fieldIndicesStrings[3]);
							fieldIndices[4] = Integer.parseInt(fieldIndicesStrings[4]);
							fieldIndices[5] = Integer.parseInt(fieldIndicesStrings[5]);
							fieldIndices[6] = Integer.parseInt(fieldIndicesStrings[6]);
							fieldIndices[7] = Integer.parseInt(fieldIndicesStrings[7]);
						} catch(NumberFormatException ex) {
							JOptionPane.showMessageDialog(null, "Error in format of field indices array. ", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
							return;
						}

						try {
							headerLinesCount = Integer.parseInt(headerLinesCountTextField.getText());
						} catch(NumberFormatException ex) {
							JOptionPane.showMessageDialog(null, "Error in format of number of header lines. ", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						if(separationCharTextField.getText().length() != 1) {
							JOptionPane.showMessageDialog(null, "Please enter one separation char. ", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
							return;
						} else
							separationChar = separationCharTextField.getText().charAt(0);

						if(quotationCharTextField.getText().length() != 1) {
							JOptionPane.showMessageDialog(null, "Please enter one quotation char. ", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
							return;
						} else
							quotationChar = quotationCharTextField.getText().charAt(0);
						
						replace = parseStringArrayInput(replaceTextField.getText());
						if(replace == null) {
							JOptionPane.showMessageDialog(null, "Error in format of replace-array. ", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
							return;
						}

						replacement = parseStringArrayInput(replacementTextField.getText());
						if(replacement == null) {
							JOptionPane.showMessageDialog(null, "Error in format of replacement-array. ", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						if(replace.length != replacement.length) {
							JOptionPane.showMessageDialog(null, "Error: Lengths of replace-array and replacement-array need to be the same. ", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						switch(latLonAreInRadianTextField.getText()) {
						case "true":
							latLonAreInRadian = true;
							break;
						case "false":
							latLonAreInRadian = false;
							break;
						default:
							JOptionPane.showMessageDialog(null, "Error: Unknown format for 'initial lat/lon are in radian'. ", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
							return;
						}

						switch(elevationIsInFeetTextField.getText()) {
						case "true":
							elevationIsInFeet = true;
							break;
						case "false":
							elevationIsInFeet = false;
							break;
						default:
							JOptionPane.showMessageDialog(null, "Error: Unknown format for 'initial elevation is in feet'. ", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
							return;
						}

						int errorCode = AirportDatabase.recompileCSVdatabase(inputFileDir, outputFileDir, fieldIndices, headerLinesCount, separationChar, quotationChar, replace, replacement, latLonAreInRadian, elevationIsInFeet);
						
						if(errorCode == 0)
							JOptionPane.showMessageDialog(null, "Recompiling of airport database successful. ", "ADSbDataParser", JOptionPane.INFORMATION_MESSAGE);
						else {
							if(errorCode > 0)
								JOptionPane.showMessageDialog(null, String.format("Airport database recompiled with %d entry-errors (these entries are skipped)", errorCode), "ADSbDataParser", JOptionPane.WARNING_MESSAGE);
							else
								JOptionPane.showMessageDialog(null, "Error: Recompiling of airport database not successful", "ADSbDataParser", JOptionPane.ERROR_MESSAGE);
						}
					}
				});
			}
			
			private String[] parseStringArrayInput(String inputString) {
				String[] stringArray;
				
				if(inputString.length() < 2)
					return null;
				if(inputString.charAt(0) != '{')
					return null;
				if(inputString.charAt(inputString.length()-1) != '}')
					return null;
				
				inputString = inputString.substring(1, inputString.length()-1);
				
				ArrayList<String> elements = new ArrayList<String>();
				String currentElement = "";
				boolean withinString = false;
				boolean wasBackSlash = false;
				for(int i=0; i<inputString.length(); i++) {
					char c = inputString.charAt(i);
					
					switch(c) {
					case '"':
						if(withinString) {
							if(wasBackSlash)
								currentElement = currentElement.concat("\"");
							else {
								elements.add(currentElement);
								currentElement = "";
								withinString = false;
							}
						} else {
							withinString = true;
						}
						break;
					case '\\':
						if(wasBackSlash)
							currentElement = currentElement.concat("\\");
						break;
					default:
						if(wasBackSlash)
							return null;
						if(withinString)
							currentElement = String.format("%s%c", currentElement, c);
						else
							if((c != ',') && (c != ' '))
								return null;
						break;
					}
					
					if((c == '\\') && !wasBackSlash)
						wasBackSlash = true;
					else
						wasBackSlash = false;
				}
				
				stringArray = new String[elements.size()];
				for(int i=0; i<elements.size(); i++)
					stringArray[i] = elements.get(i);
				
				return stringArray;
			}
		}
		
		new GUIrecompileAirportDatabase(defaultCloseOperation);
	}

}
