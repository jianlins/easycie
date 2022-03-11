package edu.utah.bmi.nlp.runner;

import edu.utah.bmi.nlp.uima.MyAnnotationViewerMain;

import java.io.File;

/**
 * Created by Jianlin Shi on 9/25/16.
 */
public class Viewer {
    public static void main(String[] args) {
        if (args.length == 0) {
            runDefaultSetting();
        } else if (args[0].equals("help")) {
            printInstructions();
        } else {
            MyAnnotationViewerMain.main(args);
        }

    }

    private static void runDefaultSetting() {
        File xmiDir = new File("data/output/xmi");
        if (!xmiDir.exists()) {
            xmiDir = new File("data/xmi");
            if (!xmiDir.exists()) {
                xmiDir = new File("");
            }
        }
        File descriptorFile = new File("desc/type/customized.xml");
        if (!descriptorFile.exists()) {
            descriptorFile = new File("desc/type/All_Types.xml");
        }
        MyAnnotationViewerMain.main(new String[]{xmiDir.getAbsolutePath(), descriptorFile.getAbsolutePath()});

    }

    public static void printInstructions() {
        System.out.println("This program can take 0~2 parameters. It will open an annotation viewer for xmi files.\n" +
                "\t1. The directory that holds xmi files.\n" +
                "\t2. The descriptor file path.");
    }
}
