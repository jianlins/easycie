package edu.utah.bmi.nlp.test;

/**
 * Created by Jianlin Shi on 8/4/17.
 */
import org.apache.commons.cli.*;
import org.junit.jupiter.api.Test;

public class CommandlineTest {


    public static void main(String[] args) throws Exception {

        Options options = new Options();

        Option input = new Option("i", "input", true, "input file path");
        input.setRequired(true);
        options.addOption(input);

        input = new Option("o", "output", true, "output file");
        input.setRequired(true);
        options.addOption(input);

        input = new Option("x", "xoxo", false, "output file");
        input.setRequired(false);
        options.addOption(input);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
            return;
        }

        String inputFilePath = cmd.getOptionValue("de");
        String outputFilePath = cmd.getOptionValue("output");

        System.out.println(inputFilePath);
        System.out.println(outputFilePath);
        System.out.println(cmd.hasOption("x"));

    }

    @Test
    public void test2(){
        String[] args=new String[]{"-r","conf/db.xml"};
        Options options = new Options();
        addOption(options,"r","rconfig","short","1","1");
        addOption(options,"e","export","export file","0","1");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
            return;
        }
        System.out.println(getCmdValue(cmd,"r","null"));

    }

    protected void addOption(Options options, String shortArg,String longArg, String description, String required, String takeValue) {
        Option opt = new Option(shortArg, longArg, takeValue.equals("1"),description);
        opt.setRequired(required.equals("1"));
        options.addOption(opt);
    }

    protected String getCmdValue(CommandLine cmd, String key, String defaultValue) {
        String value = cmd.getOptionValue(key);
        if (value == null)
            value = defaultValue;
        return value;
    }

}