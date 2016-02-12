package com.aksturgeon.toolbox.util;

import java.io.File;

import com.aksturgeon.toolbox.Dashboard;

public class ToolboxUtil {
  public boolean isNumeric(String input) {
    try {
      Integer.parseInt(input);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static String[] getKeyFieldNames(String[] keyNames, int selectedDbPlatform) {
    // TODO Auto-generated method stub
    return null;
  }

  public static String getTableNameFromHbn(String daoClassName, int selectedDbPlatform, String daoFilePath) {
    String mapSubDir = "";
    if (selectedDbPlatform == Dashboard.DB_TYPE_DB2I) {
      mapSubDir = "db2i";
    } else {
      mapSubDir = "sqlserver";
    }
    String defFilePath = daoFilePath.substring(0, daoFilePath.lastIndexOf("\\") - 3).concat(
        "orm\\hbn\\" + mapSubDir + "\\");
    File mappingFile = FileUtil.getFileFromDirectory(defFilePath, daoClassName + "Base.def.xml");
    
    //currently returning file name to avoid error, need to oepn file and get the 
    return mappingFile.getName();
  }
}
