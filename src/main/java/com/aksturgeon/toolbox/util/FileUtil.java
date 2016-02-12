package com.aksturgeon.toolbox.util;

import java.io.File;

public class FileUtil {
  private static String fileNameToSearch;
  private static String filePath;
  
  
  public static File getFileFromDirectory(String folder, String fileName) {
    fileNameToSearch = fileName;
    searchDirectoryForFile(new File(folder));
    if(filePath != null) {
      return new File(filePath);
    }
    return null;
  }
  
  private static void searchDirectoryForFile(File folder) {
    File[] files = folder.listFiles();
    if(files != null) {
      for (File file : files) {
        if(file.isDirectory()) {
          searchDirectoryForFile(file);
        } else {
          if(file.getName().equals(fileNameToSearch)) {
            filePath = file.getPath();
            break;
          }
        }
      }
    }
  }
}
