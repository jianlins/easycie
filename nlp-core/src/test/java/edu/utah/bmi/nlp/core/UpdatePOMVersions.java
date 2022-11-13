package edu.utah.bmi.nlp.core;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Up to maven version 3.8.4 using ${revision} in version configuration won't replace the actual version in pom files while deploying, even though
 * the compiled jar files' versions will be replaced.
 * <p>
 * So this class is used to replace the versions in pom files, so that when set the dependencies on these modules will not throw error of
 * version ${revision} not found.
 *
 * @author Jianlin Shi
 */
public class UpdatePOMVersions {
    Pattern p = Pattern.compile("(?<=<version>)[^<]+(?=</version>[\\s\\n\\r]+</parent>)");
    Pattern vp = Pattern.compile("(?<=<revision>)[^<]+(?=</revision>)");
    Pattern rp = Pattern.compile("(?<=<version>)[^<]+(?=</version>)");
    Pattern pp = Pattern.compile("(?<=<version>)[^<]+(?=</version>[\\s\\n\\r]+<packaging>pom)");

    @Test
    public void update() {
        System.out.println(new File("../").getAbsolutePath());
        Collection<File> poms = FileUtils.listFiles(new File("../"), new String[]{"xml"}, true);
        String version = readVersion();


        File[] subDir = new File("../").listFiles((FileFilter) FileFilterUtils.directoryFileFilter());
        for (File d : subDir) {
            File pom = new File(d, "pom.xml");
            if (pom.exists()) {
                System.out.println(pom);
                updatePOM(pom, version);
            }
        }
        Collection<File> readmes = FileUtils.listFiles(new File("../"), new String[]{"md","MD"}, true);
        for (File readme :readmes){
            updateREADME(readme, version);
        }

    }

    public String readVersion() {
        File parentpom = new File("../pom.xml");
        if (parentpom.exists()) {
            try {
                String content = FileUtils.readFileToString(parentpom, StandardCharsets.UTF_8);
                Matcher m = vp.matcher(content);
                if (m.find()) {
                    String version = m.group(0);
                    System.out.println("Find revision: " + version + "\n\tfrom parent pom file: " + parentpom.getCanonicalPath());
                    updateParent(parentpom, version);
                    return version;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                System.out.println("Parent pom hasn't been found at: " + parentpom.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";

    }

    public void updateParent(File parentpom, String version) {
        updateVersionInFile(parentpom, version, "", pp);
    }

    public void updatePOM(File pom, String version) {
        updateVersionInFile(pom, version, "<parent>", p);
    }

    public void updateREADME(File readme, String version) {
        updateVersionInFile(readme, version, "<version>", rp);
    }

    /**
     * Update the a file's content with up-to-date version
     *
     * @param file  java io File object
     * @param version dependency version
     */
    public void updateVersionInFile(File file, String version, String filter, Pattern p) {
        String content = "";
        try {
            content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (filter.length()==0 || content.contains(filter)) {

            Matcher m = p.matcher(content);
            if (m.find()) {
                try {
                    System.out.println("Update file: "+file.getCanonicalPath()+" "+m.start() + "\t" + m.end());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("from "+m.group()+" to "+version);

                int end = m.end();
                int start = m.start();
                String updatedContent = content.substring(0, start) + version + content.substring(end);
                try {
                    FileUtils.write(file, updatedContent, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("not found");
            }
        }


    }
}
