package edu.utah.bmi.nlp.easycie;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AddNewPipeline {
    protected String configDirResource = "/demo_configurations/";
    protected String configIdStr = "";
    protected String configDirPath = "";
    protected String projectConfigTemplate = "demo_config";
    protected String dbConfigTemplate = "demo_sqlite_config";
    protected String outputProjectConfig = "";
    protected String outputDbConfig = "";

    protected String projectName = "demo";
    protected String annotator = "v0";
    protected File appDir = new File("./");
    protected File configDir;


    public AddNewPipeline(String[] args) {
        switch (args.length) {
            case 7:
                annotator = args[6];
            case 6:
                dbConfigTemplate = args[5];
            case 5:
                projectConfigTemplate = args[4];
            case 4:
                configDirResource = args[3];
            case 3:
                appDir = new File(args[2]);
            case 2:
                projectName = args[1];
                if (configDirPath.length() == 0)
                    configDirPath = "conf/" + projectName + configIdStr;
                outputProjectConfig = projectName + "_config";
                outputDbConfig = "dbconfig/" + projectName + "_" + args[0] + "_sqlite_config";
            case 1:
//              use to differentiate different batch, while sharing same rule files.
                configIdStr = args[0];
                if (configIdStr.charAt(0) == '-')
                    configIdStr = "";
                else
                    configIdStr = "_" + configIdStr;
                configDir=new File(appDir, configDirPath);
                break;
        }

    }

    public void gen() {
        if (!configDir.exists()) {
            try {
                FileUtils.forceMkdir(configDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File localDemoConfig = new File("conf", configDirResource);
        if (localDemoConfig.exists()) {
            String configTxt = getLocalResourceText(configDirResource + projectConfigTemplate);
            String dbText = getLocalResourceText(configDirResource + dbConfigTemplate);
            writeConfigFiles(configTxt, dbText, configDir);
            configDirResource = localDemoConfig.getAbsolutePath();
            copyLocalRuleConfigs();
        } else {
            String configTxt = getResourceText(configDirResource + projectConfigTemplate + ".xml");
            String dbText = getResourceText(configDirResource + "dbconfig/" + dbConfigTemplate + ".xml");
            writeConfigFiles(configTxt, dbText, configDir);
            copyDemoRuleConfigs();
        }

    }

    private void writeConfigFiles(String configTxt, String dbText, File configDir) {
        configTxt = configTxt.replaceAll("\\{batchId\\}", configIdStr)
                .replaceAll("\\{configDir\\}", configDirPath)
                .replaceAll("\\{dbPrefix\\}", outputDbConfig)
                .replaceAll("\\{annotator\\}", annotator);
        File dbFile = new File(appDir, "data/" + projectName + "_" + configIdStr + ".sqlite");
        if (!dbFile.getParentFile().exists())
            dbFile.getParentFile().mkdirs();
        dbText = dbText.replaceAll("\\{sqlite_path\\}", dbFile.getPath());
        try {
            FileUtils.writeStringToFile(new File(configDir, outputProjectConfig + ".xml"), configTxt, StandardCharsets.UTF_8);
            FileUtils.writeStringToFile(new File(configDir, outputDbConfig + ".xml"), dbText, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyLocalRuleConfigs() {
        File dir = new File(configDirResource);
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                String fileName = f.getName().toLowerCase();
                if (fileName.endsWith(".tsv") || fileName.endsWith(".csv") || fileName.endsWith(".txt") || fileName.endsWith(".xlsx")) {
                    try {
                        IOUtils.copy(new FileInputStream(f), new FileOutputStream(new File(configDir, f.getName())));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    protected void copyDemoRuleConfigs() {
        for (String resource : listFilesInJarDir(configDirResource)) {
            String fileName = resource.toLowerCase();
            if (fileName.endsWith(".tsv") || fileName.endsWith(".csv") || fileName.endsWith(".txt") || fileName.endsWith(".xlsx")) {
                try {
                    IOUtils.copy(getResourceInputStream(configDirResource + resource), new FileOutputStream(new File(configDir, resource)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    protected InputStream getResourceInputStream(String path) {
        System.out.println("get resource: " + path);
        InputStream ins = getClass().getResourceAsStream(path);
        if (ins == null) {
            System.err.println(path);
        }
        return ins;
    }

    protected ArrayList<String> getResourceLines(String path) {
        ArrayList<String> lines = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getResourceInputStream(path)));
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    protected ArrayList<String> listFilesInJarDir(String path) {
        ArrayList<String> files = new ArrayList<>();
        if (path.startsWith("/"))
            path = path.substring(1);
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        CodeSource src = getClass().getProtectionDomain().getCodeSource();
        try {
            if (src != null) {
                URL jar = src.getLocation();
                if (new File(jar.getPath()).exists() && !jar.getPath().endsWith(".jar")) {
                    configDirResource = new File(jar.getPath(), configDirResource).getAbsolutePath();
                    System.out.println(configDirResource);
                    copyLocalRuleConfigs();
                    return new ArrayList<>();
                }
                ZipInputStream zip = null;
                zip = new ZipInputStream(jar.openStream());
                while (true) {
                    ZipEntry e = zip.getNextEntry();
                    if (e == null)
                        break;
                    String name = e.getName();
                    System.out.println(name + "\t" + (name.indexOf(path) >= 0));
                    if (name.indexOf(path) >= 0) {
                        files.add(name.substring(path.length()).replaceAll("/", ""));
                    }
                }
            } else {
                System.out.println("no rule file found in jar.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }

    protected String getResourceText(String path) {
        StringBuilder content = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getResourceInputStream(path)));
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
                content.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }

    protected String getLocalResourceText(String path) {
        String content = "";
        if (new File(path).exists()) {
            try {
                content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return content;
    }

    public static void main(String[] args) {
        System.out.println(args.length);
        System.out.println(new AddNewPipeline(args));
    }
}
