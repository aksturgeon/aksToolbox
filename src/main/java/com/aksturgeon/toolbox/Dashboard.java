package com.aksturgeon.toolbox;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

import com.aksturgeon.toolbox.dbDocHelper.DbFieldMapper;
import com.aksturgeon.toolbox.ormAssembler.OrmAssembler;
import com.aksturgeon.toolbox.sql.SqlViewIndexGenerator;
import com.aksturgeon.toolbox.sql.SqlTableGenerator;

/**
 * Sets up constants and calls subordinate utility classes.
 * 
 * @author Ken Sturgeon
 */
public class Dashboard {
  public static final int CLC_SUBSYSTEM = 1;
  public static final int GLC_SUBSYSTEM = 2;
  public static final int ILC_SUBSYSTEM = 3;
  public static final int DB_TYPE_DB2I = 1;
  public static final int DB_TYPE_SQLSERVER = 2;

  private static void createSqlScripts(Scanner userInput, Properties props, int selectedSubsystem, String dbPlatform) {
    SqlTableGenerator sqlGenerator = new SqlTableGenerator();
    if (selectedSubsystem == 0 || selectedSubsystem == Dashboard.CLC_SUBSYSTEM) {
      String clcSourceFolder = props.getProperty("dashboard.root.clc.folder");
      System.out.println("\nThe root source folder for CLC components is " + clcSourceFolder);

      System.out.println("\nStarting SqlGenerator for CLC for " + dbPlatform);
      sqlGenerator.generateSql(props, CLC_SUBSYSTEM, dbPlatform);
      System.out.println("SqlGenerator completed for CLC for " + dbPlatform);
    }
    if (selectedSubsystem == 0 || selectedSubsystem == Dashboard.GLC_SUBSYSTEM) {
      String glcSourceFolder = props.getProperty("dashboard.root.glc.folder");
      System.out.println("\nThe root source folder for GLC components is " + glcSourceFolder);

      System.out.println("\nStarting SqlGenerator for GLC for " + dbPlatform);
      sqlGenerator.generateSql(props, GLC_SUBSYSTEM, dbPlatform);
      System.out.println("SqlGenerator completed for GLC for " + dbPlatform);
    }
    if (selectedSubsystem == 0 || selectedSubsystem == Dashboard.ILC_SUBSYSTEM) {
      String ilcSourceFolder = props.getProperty("dashboard.root.ilc.folder");
      System.out.println("\nThe root source folder for ILC components is " + ilcSourceFolder);

      System.out.println("\nStarting SqlGenerator for ILC for " + dbPlatform);
      sqlGenerator.generateSql(props, ILC_SUBSYSTEM, dbPlatform);
      System.out.println("SqlGenerator completed for ILC for " + dbPlatform);
    }
    System.out.println("**SQL SCRIPT GENERATION COMPLETE**");
  }

  private static String getActionSummaryMessage(int selectedFunction, int selectedSubsystem, int selectedDbPlatform) {
    String msg = "You have chosen to ";
    switch (selectedFunction) {
      case 1:
        msg += "reassemble the mapping files ";
        break;
      case 2:
        msg += "generate sql scripts ";
        break;
      case 3:
        msg += "create database documentation ";
        break;
    }
    switch (selectedSubsystem) {
      case 0:
        msg += "for All subsystems ";
        break;
      case 1:
        msg += "for the CLC subsystem ";
        break;
      case 2:
        msg += "for the GLC subsystem ";
        break;
      case 3:
        msg += "for the ILC subsystem ";
        break;
    }
    switch (selectedDbPlatform) {
      case 1:
        msg += "targeting the DB2 database platform.";
        break;
      case 2:
        msg += "targeting the SQL Server database platform.";
        break;
    }
    return msg;
  }

  private static String getOrmInputFileLocation(int selectedSubsystem) {
    switch (selectedSubsystem) {
      case 0:
        return "ALL";
      case CLC_SUBSYSTEM:
        return "CLC";
      case GLC_SUBSYSTEM:
        return "GLC";
      case ILC_SUBSYSTEM:
        return "ILC";
    }
    return "unknown";
  }

  private static String getSubsystemText(Properties props, int selectedSubsystem) {
    switch (selectedSubsystem) {
      case 0:
        return props.getProperty("dashboard.root.clc.folder"); // ROOT_SOURCE_FOLDER
                                                               // + "/" +
                                                               // userName +
                                                               // "_Gias35_Common_DV/Gias_Common";
      case CLC_SUBSYSTEM:
        return "CLC";
      case GLC_SUBSYSTEM:
        return "GLC";
      case ILC_SUBSYSTEM:
        return "ILC";
    }
    return "unknown";
  }

  public static void main(String[] args) {
    Properties props = new Properties();
    InputStream inputProps = null;

    try {
      inputProps = new FileInputStream("toolboxConfig.properties");
      props.load(inputProps);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    Scanner userInput = new Scanner(System.in);

    System.out.println("\nEnter Ctrl + C at any time to cancel.");
    System.out.println("What function do you want to perform?\n"
        + "Enter 1 to reassemble the mappping files\n" + "Enter 2 to generate sql scripts\n"
        + "Enter 3 to create database field map\n"
        + "Enter 4 to generate sql server database indexes for access pahts defined in views");
    int selectedFunction = userInput.nextInt();

    System.out.println("What subsystem are you targeting?\n" + "Enter 0 for All subsystems\n" + "Enter "
        + CLC_SUBSYSTEM + " for CLC\n" + "Enter " + GLC_SUBSYSTEM + " for GLC\n" + "Enter " + ILC_SUBSYSTEM
        + " for ILC\n");
    int selectedSubsystem = userInput.nextInt();

    if (selectedFunction == 3) {
      DbFieldMapper dbFieldMapper = new DbFieldMapper();
      dbFieldMapper.generateDatabaseMap(selectedSubsystem, props);
      System.exit(0);
    }

    // if (selectedFunction == 4) {
    // SqlViewIndexGenerator sqlIndexGenerator = new SqlViewIndexGenerator();
    // sqlIndexGenerator.generateAccessPathIndexes(selectedSubsystem, props);
    // System.exit(0);
    // }

    System.out.println("\nWhich database type are you targeting?\n" + "Enter " + DB_TYPE_DB2I + " for db2i\n"
        + "Enter " + DB_TYPE_SQLSERVER + " for sqlserver.");
    int selectedDbPlatform = userInput.nextInt();

    String dbPlatform = "";
    if (selectedDbPlatform == DB_TYPE_DB2I) {
      dbPlatform = "db2i";
    }
    if (selectedDbPlatform == DB_TYPE_SQLSERVER) {
      dbPlatform = "sqlserver";
    }

    System.out.println(getActionSummaryMessage(selectedFunction, selectedSubsystem, selectedDbPlatform));
    switch (selectedFunction) {
      case 1:
        reassembleMappingFiles(userInput, props, selectedSubsystem, dbPlatform);
        break;
      case 2:
        createSqlScripts(userInput, props, selectedSubsystem, dbPlatform);
        break;
      // case 3:
      // createDatabaseDocumentation(userInput, userName, selectedSubsystem,
      // dbPlatform);
      // DictionaryDescriptor dictionaryDescriptor = new DictionaryDescriptor();
      // dictionaryDescriptor.generateSQLInserts(userName);
      // break;
      case 4:
        SqlViewIndexGenerator sqlIndexGenerator = new SqlViewIndexGenerator();
        sqlIndexGenerator.generateAccessPathIndexes(selectedSubsystem, props, selectedDbPlatform);
        break;
    }
  }

  private static void createSqlScript(Scanner userInput, Properties props, int selectedSubsystem, String dbPlatform) {
    // System.out.println(getOrmInputFileLocation(selectedSubsystem));
    SqlTableGenerator sqlGenerator = new SqlTableGenerator();
    if (selectedSubsystem == 0 || selectedSubsystem == Dashboard.CLC_SUBSYSTEM) {
      // String clcSourceFolder = ROOT_SOURCE_FOLDER + "/" + userName +
      // "_Gias35_Common_DV/Gias_Common";
      String clcSourceFolder = props.getProperty("dashboard.root.clc.folder");
      System.out.println("\nThe root source folder for CLC components is " + clcSourceFolder);

      System.out.println("\nStarting SqlGenerator for CLC for " + dbPlatform);
      sqlGenerator.generateSql(props, CLC_SUBSYSTEM, dbPlatform);
      System.out.println("SqlGenerator completed for CLC for " + dbPlatform);
    }
    if (selectedSubsystem == 0 || selectedSubsystem == Dashboard.GLC_SUBSYSTEM) {
      // String glcSourceFolder = ROOT_SOURCE_FOLDER + "/" + userName +
      // "_Gias35_GroupLic_DV/Gias_GroupLic";
      String glcSourceFolder = props.getProperty("dashboard.root.glc.folder");
      System.out.println("\nThe root source folder for GLC components is " + glcSourceFolder);

      System.out.println("\nStarting SqlGenerator for GLC for " + dbPlatform);
      sqlGenerator.generateSql(props, GLC_SUBSYSTEM, dbPlatform);
      System.out.println("SqlGenerator completed for GLC for " + dbPlatform);
    }
    if (selectedSubsystem == 0 || selectedSubsystem == Dashboard.ILC_SUBSYSTEM) {
      // String ilcSourceFolder = ROOT_SOURCE_FOLDER + "/" + userName +
      // "_Gias35_IndvLic_DV/Gias_IndvLic";
      String ilcSourceFolder = props.getProperty("dashboard.root.ilc.folder");
      System.out.println("\nThe root source folder for ILC components is " + ilcSourceFolder);

      System.out.println("\nStarting SqlGenerator for ILC for " + dbPlatform);
      sqlGenerator.generateSql(props, ILC_SUBSYSTEM, dbPlatform);
      System.out.println("SqlGenerator completed for ILC for " + dbPlatform);
    }
    System.out.println("**SQL SCRIPT GENERATION COMPLETE**");
  }

  private static void reassembleMappingFiles(Scanner userInput, Properties props, int selectedSubsystem,
      String dbPlatform) {
    OrmAssembler ormAssembler = new OrmAssembler();
    if (selectedSubsystem == 0 || selectedSubsystem == Dashboard.CLC_SUBSYSTEM) {
      // String clcSourceFolder = ROOT_SOURCE_FOLDER + "/" + userName +
      // "_Gias35_Common_DV/Gias_Common";
      String clcSourceFolder = props.getProperty("dashboard.root.clc.folder");
      System.out.println("\nThe root source folder for CLC components is " + clcSourceFolder);

      System.out.println("\nStarting OrmAssembler for CLC for " + dbPlatform);
      ormAssembler.assembleMaps(clcSourceFolder, CLC_SUBSYSTEM, dbPlatform, props);
      System.out.println("OrmAssembler completed for CLC for " + dbPlatform);
    }
    if (selectedSubsystem == 0 || selectedSubsystem == Dashboard.GLC_SUBSYSTEM) {
      // String glcSourceFolder = ROOT_SOURCE_FOLDER + "/" + userName +
      // "_Gias35_GroupLic_DV/Gias_GroupLic";
      String glcSourceFolder = props.getProperty("dashboard.root.glc.folder");
      System.out.println("\nThe root source folder for GLC components is " + glcSourceFolder);

      System.out.println("\nStarting OrmAssembler for GLC for " + dbPlatform);
      ormAssembler.assembleMaps(glcSourceFolder, GLC_SUBSYSTEM, dbPlatform, props);
      System.out.println("OrmAssembler completed for GLC for " + dbPlatform);
    }
    if (selectedSubsystem == 0 || selectedSubsystem == Dashboard.ILC_SUBSYSTEM) {
      // String ilcSourceFolder = ROOT_SOURCE_FOLDER + "/" + userName +
      // "_Gias35_IndvLic_DV/Gias_IndvLic";
      String ilcSourceFolder = props.getProperty("dashboard.root.ilc.folder");
      System.out.println("\nThe root source folder for ILC components is " + ilcSourceFolder);

      System.out.println("\nStarting OrmAssembler for ILC for " + dbPlatform);
      ormAssembler.assembleMaps(ilcSourceFolder, ILC_SUBSYSTEM, dbPlatform, props);
      System.out.println("OrmAssembler completed for ILC for " + dbPlatform);
    }
    System.out.println("**MAPPING FILE REASSEMBLY COMPLETE**");
  }
}
