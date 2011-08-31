package org.freeeed.main;

import java.io.File;
import java.text.DecimalFormat;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.configuration.Configuration;
import org.freeeed.services.Stats;
import org.freeeed.ui.FreeEedUI;

/**
 * Main application instance
 *
 * @author mark
 */
public class FreeEedMain {

    private static FreeEedMain instance = new FreeEedMain();
    private CommandLine commandLine;
    private Configuration processingParameters;

    public static FreeEedMain getInstance() {
        return instance;
    }

    public Configuration getProcessingParameters() {
        return processingParameters;
    }
    private Options options = formOptions();

    private Options formOptions() {
        Options buildOptions = new Options();
        for (CommandLineOption o : CommandLineOption.values()) {
            buildOptions.addOption(o.getName(), o.isHasArg(), o.getHelp());
        }
        return buildOptions;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        FreeEedLogging.init();
        instance.processOptions(args);
    }

    private FreeEedMain() {
    }

    /**
     * Process the command line arguments
     *
     * @param args command line arguments
     */
    private void processOptions(String[] args) {
        String customParameterFile = null;
        try {
            BasicParser parser = new BasicParser();
            commandLine = parser.parse(options, args);

            // one-time actions
            if (commandLine.hasOption(CommandLineOption.HELP.getName())
                    || commandLine.getOptions().length == 0) {
                HelpFormatter f = new HelpFormatter();
                f.printHelp("java -jar FreeEed.jar [options]\n\n"
                        + "where options include:", options);
            } else if (commandLine.hasOption(CommandLineOption.VERSION.getName())) {
                System.out.println(Version.getVersion());
            } else if (commandLine.hasOption(CommandLineOption.GUI.getName())) {
                openGUI();
            } else if (commandLine.hasOption(CommandLineOption.ENRON.getName())) {
                processEnronDataSet();
            } else {
                if (commandLine.hasOption(CommandLineOption.PARAM_FILE.getName())) {
                    // independent actions
                    customParameterFile = commandLine.getOptionValue(CommandLineOption.PARAM_FILE.getName());
                    setProcessingParameters(ParameterProcessing.collectProcessingParameters(customParameterFile));
                    ParameterProcessing.echoProcessingParameters(getProcessingParameters());
                }
                if (commandLine.hasOption(CommandLineOption.DRY.getName())) {
                    System.out.println("Dry run - exiting now.");
                } else {
                    if (FreeEedMain.getInstance().getProcessingParameters().getString("stage") != null) {
                        stagePackageInput();
                    }
                    String runWhere = FreeEedMain.getInstance().getProcessingParameters().getString(ParameterProcessing.PROCESS_WHERE);
                    if (runWhere != null) {
                        process(runWhere);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * Process staged files
     *
     * @param runWhere determines whether processing occurs on local, private, or EC2 Hadoop instance
     * @throws FreeEedException
     */
    public void process(String runWhere) {
        String projectName = processingParameters.getString(ParameterProcessing.PROJECT_NAME);
        Stats.getInstance().setJobStarted(projectName);
        try {
            new ActionProcessing(runWhere).process();
        } catch (Exception e) {
            // TODO - what to do in case of exception
            e.printStackTrace(System.out);
        }
    }

    /**
     * Process from GUI in a thread
     *
     * @param runWhere determines whether processing occurs on local, private, or EC2 Hadoop instance
     * @throws FreeEedException
     */
    public void runProcessing(String runWhere) throws FreeEedException {
        String projectName = processingParameters.getString(ParameterProcessing.PROJECT_NAME);
        Stats.getInstance().setJobStarted(projectName);
        new Thread(new ActionProcessing(runWhere)).start();
    }

    /**
     * Stage (package) the input files
     * 
     * @throws Exception
     */
    public void stagePackageInput() throws Exception {
        new ActionStaging().stagePackageInput();
    }

    /**
     * Stage from GUI in a thread
     * 
     * @throws Exception
     */
    public void runStagePackageInput() throws Exception {
        // TODO - think through the use of threads, locking, communication, cancel, etc.
        new Thread(new ActionStaging()).start();
    }

    private void openGUI() {
        FreeEedUI.main(null);
    }

    /**
     * @param processingParameters the processingParameters to set
     */
    public void setProcessingParameters(Configuration processingParameters) {
        this.processingParameters = processingParameters;
    }

    private void processEnronDataSet() {
        int ENRON_SET_SIZE = 154;
        String localDir = "freeeed_output/";
        String output = "output/";
        for (int i = 1; i <= ENRON_SET_SIZE; ++i) {
            LinuxUtil.runLinuxCommand("rm -r " + localDir);
            String dir = "/mnt/tmp/";
            DecimalFormat decimalFormat = new DecimalFormat("enron000");
            String projectName = decimalFormat.format(i);
            if (new File(dir + projectName + ".project").exists() == false) {
                continue;
            }
            String outputPath = dir + "results/"+ projectName + "/";
            if (new File(outputPath).exists()) {
                continue;
            }
            String[] argv = new String[2];
            argv[0] = "-param_file";
            argv[1] = dir + projectName + ".project";
            processOptions(argv);
            
            new File(outputPath).mkdirs();
            String command = "cp " + localDir + output 
                    + "native.zip " + outputPath + projectName + ".zip";
            LinuxUtil.runLinuxCommand(command);
            command = "cp " + localDir + output + "part-r-00000 " +
                    outputPath + projectName + ".csv";
            LinuxUtil.runLinuxCommand(command);
            command = "mv logs/stats.txt " + 
                    outputPath + projectName + ".txt";                      
            LinuxUtil.runLinuxCommand(command);
        }
    }
}
