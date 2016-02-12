package com.aksturgeon.toolbox.ormAssembler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.aksturgeon.toolbox.Dashboard;

/**
 * Iterates through the identified source folders to create a single mapping
 * file for each of the .hbn.xml files that exist by replacing the &BaseDef;
 * placeholder with the contents of the corresponding .def.xml file.
 * 
 * @author Ken Sturgeon
 */
public class OrmAssembler {

  private final String REGEX_ENTITY = "(\\[<!.*classpath:.*/)(.*\\.def\\.xml)(.*\\]>)";
  private final String REGEX_XML_VERSION = "(<\\?xml.*\\?>)";

  private List<String> hbnFileList = new ArrayList<String>();

  /**
   * Entry point
   */
  public void assembleMaps(String sourceFolder, int subSystem, String dbPlatform, Properties props) {
    stageMappingFiles(sourceFolder, subSystem, dbPlatform, props);
  }

  private void copyMergedMaps(int subSystem, String dbPlatform, String sourceFolder, Properties props) {
    hbnFileList.clear();
    fillHbnFileList(dbPlatform, sourceFolder);
    for (String hbnFileName : hbnFileList) {
      createMergedFile(hbnFileName, subSystem, dbPlatform, props);
    }
  }

  private void fillHbnFileList(String dbPlatform, String sourceFolder) {
    File directory = new File(sourceFolder);
    File[] fList = directory.listFiles();
    for (File file : fList) {
      if (file.isFile()) {
        if (file.getPath().contains("\\orm\\")) {
          // System.out.println(file.getPath());
        }
        if (file.getPath().contains(dbPlatform) && file.getName().endsWith(".hbn.xml")) {
          hbnFileList.add(file.getPath());
        }
      } else if (file.isDirectory()) {
        fillHbnFileList(dbPlatform, file.getAbsolutePath());
      }
    }
  }

  private void createMergedFile(String hbnSourceFileName, int subSystem, String dbPlatform, Properties props) {
    String outputFolder = props.getProperty("dashboard.root.output.folder");
    switch (subSystem) {
      case Dashboard.CLC_SUBSYSTEM:
        outputFolder = outputFolder + "/clc/" + dbPlatform;
        break;
      case Dashboard.GLC_SUBSYSTEM:
        outputFolder = outputFolder + "/glc/" + "/" + dbPlatform;
        break;
      case Dashboard.ILC_SUBSYSTEM:
        outputFolder = outputFolder + "/ilc/" + "/" + dbPlatform;
        break;
    }
    generateMergedFile(hbnSourceFileName, outputFolder);
  }

  private void createOutputFolder(String outputFolder, String dbPlatform, Properties props) {
    File outputBaseDir = new File(props.getProperty("dashboard.root.output.folder"));
    // if (outputBaseDir.exists()) {
    // System.out.println("Dropping folder " + App.ROOT_OUTPUT_FOLDER);
    // outputBaseDir.delete();
    // }
    if (!outputBaseDir.exists()) {
      System.out.println("Creating new root folder " + props.getProperty("dashboard.root.output.folder"));
      outputBaseDir.mkdir();
    }
    File outputSubDir = new File(outputFolder);
    if (!outputSubDir.exists()) {
      System.out.println("Creating new folder " + outputFolder);
      new File(outputFolder).mkdir();
    }
    File platformSubDir = new File(outputFolder + "/" + dbPlatform);
    if (!platformSubDir.exists()) {
      System.out.println("Creating new folder " + outputFolder + "/" + dbPlatform);
      new File(outputFolder + "/" + dbPlatform).mkdir();
    }
  }

  private void generateMergedFile(String hbnSourceFileName, String outputFolder) {
    File sourceHbn = new File(hbnSourceFileName);
    Pattern regexPattern = Pattern.compile(REGEX_ENTITY);
    try {
      Matcher match = regexPattern.matcher(FileUtils.readFileToString(sourceHbn));
      if (match.find()) {
        String matchedText = match.group(0);
        // System.out.println(matchedText);
        // There are some cases where this patter will not give the proper
        // results because the def file may be in another package but the regex
        // used cannot be refined any further because the format differs in CLC.
        // The errors report a missing def file because it is looking in the
        // folder that the hbn is in but the def is actually in a different
        // package.
        String defFileName = matchedText.replaceAll("(?m)(\\[<!.*classpath:.*/)(.*\\.def\\.xml)(.*\\])(>)", "$2");
        String defFilePath = sourceHbn.getPath().replace(sourceHbn.getName(), defFileName);
        File sourceDef = new File(defFilePath);
        // System.out.println(defFilePath);
        String destFileContents = FileUtils.readFileToString(sourceHbn).replace("&BaseDef;",
            FileUtils.readFileToString(sourceDef));

        regexPattern = Pattern.compile(REGEX_XML_VERSION);
        match = regexPattern.matcher(destFileContents);
        while (match.find()) {
          destFileContents = destFileContents.replaceAll("(?m)(<\\?xml.*\\?>)", "");
        }

        File destFile = new File(outputFolder + "/" + sourceHbn.getName().replace(".hbn.xml", ".xml"));
        if (!destFile.exists()) {
          destFile.createNewFile();
        }
        FileWriter fw = new FileWriter(destFile.getAbsolutePath());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(destFileContents);
        bw.close();
        System.out.println("Created: " + outputFolder + "/" + sourceHbn.getName().replace(".hbn.xml", ".xml"));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void stageMappingFiles(String sourceFolder, int subSystemSelected, String dbPlatform, Properties props) {
    String outputFolder = props.getProperty("dashboard.root.output.folder");
    switch (subSystemSelected) {
      case Dashboard.CLC_SUBSYSTEM:
        outputFolder += "/clc";
        break;
      case Dashboard.GLC_SUBSYSTEM:
        outputFolder += "/glc";
        break;
      case Dashboard.ILC_SUBSYSTEM:
        outputFolder += "/ilc";
        break;
    }
    createOutputFolder(outputFolder, dbPlatform, props);
    copyMergedMaps(subSystemSelected, dbPlatform, sourceFolder, props);
  }
}
