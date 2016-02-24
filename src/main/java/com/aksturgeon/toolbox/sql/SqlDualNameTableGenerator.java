package com.aksturgeon.toolbox.sql;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.aksturgeon.toolbox.Dashboard;

/**
 * Parses xml mapping files that were generated by the OrmAssembler and
 * generates DDL scripts for db2 and sql server based on the map configurations.
 * 
 * @author Ken Sturgeon
 */
public class SqlDualNameTableGenerator {
  private String ddl = "";
  private String table = "";
  private String shortTable = "";
  private String longTable = "";
  private String constraints = "";
  private String udx = "";
  private String column = "";
  private String shortColumn = "";
  private String longColumn = "";
  private String longColumnType = "";
  private String sqlType = "";
  private String keys = "";
  private String javaType = "";

  /**
   * Entry point
   * @param props
   * @param subSystem
   * @param dbPlatform
   */
  public void generateSql(Properties props, int subSystem, String dbPlatform) {
    switch (subSystem) {
      case Dashboard.CLC_SUBSYSTEM:
        System.out.println("Generating script for clc subsystem...");
        createScriptFile(Dashboard.CLC_SUBSYSTEM, dbPlatform, props);
        System.out.println("Script for clc subsystem complete.");
        break;
      case Dashboard.GLC_SUBSYSTEM:
        System.out.println("Generating script for glc subsystem...");
        createScriptFile(Dashboard.GLC_SUBSYSTEM, dbPlatform, props);
        System.out.println("Script for glc subsystem complete.");
        break;
      case Dashboard.ILC_SUBSYSTEM:
        System.out.println("Generating script for ilc subsystem...");
        createScriptFile(Dashboard.ILC_SUBSYSTEM, dbPlatform, props);
        System.out.println("Script for ilc subsystem complete.");
        break;
    }
  }

  private void createScriptFile(int subSystem, String dbPlatform, Properties props) {
    String outputFolder = props.getProperty("dashboard.root.output.folder");
    String outputFileName = outputFolder;

    switch (subSystem) {
      case Dashboard.CLC_SUBSYSTEM:
        outputFileName += "/clc-" + dbPlatform + "-ddl.sql";
        generateSubSystemDDL(outputFolder + "/clc", dbPlatform);
        break;
      case Dashboard.GLC_SUBSYSTEM:
        outputFileName += "/glc-" + dbPlatform + "-ddl.sql";
        generateSubSystemDDL(outputFolder + "/glc", dbPlatform);
        break;
      case Dashboard.ILC_SUBSYSTEM:
        outputFileName += "/ilc-" + dbPlatform + "-ddl.sql";
        generateSubSystemDDL(outputFolder + "/ilc", dbPlatform);
        break;
    }
    createFile(outputFileName, ddl);
  }

  private void createFile(String sqlOutputFileName, String inputDDL) {
    try {
      System.out.println("Writing " + sqlOutputFileName);
      File sqlFile = new File(sqlOutputFileName);
      if (sqlFile.exists()) {
        sqlFile.delete();
      }
      if (!sqlFile.exists()) {
        sqlFile.createNewFile();
      }
      StringReader stringReader = new StringReader(inputDDL);
      BufferedReader bufferedReader = new BufferedReader(stringReader);
      FileWriter fileWriter = new FileWriter(sqlFile.getCanonicalFile());
      BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
      for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
        bufferedWriter.write(line);
        bufferedWriter.newLine();
      }
      bufferedReader.close();
      bufferedWriter.close();
      System.out.println(sqlOutputFileName + " complete.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void generateSubSystemDDL(String outputFolder, String dbPlatform) {
    File directory = new File(outputFolder + "/" + dbPlatform);
    File[] fList = directory.listFiles();
    for (File xmlFile : fList) {
      createSqlUsingOrm(xmlFile, dbPlatform);
    }
  }

  private void createSqlUsingOrm(File xmlFile, String dbPlatform) {
    NodeList sqlClassNodeList = null;
    Node sqlClassNode = null;
    Element sqlClassElement = null;
    // sqlConstraints = sqlKeys = db2Constraints = db2Keys = "";
    constraints = keys = "";

    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    
    // For db2i we need to open the sqlserver xml file to get the long column names
    File sqlServerXmlFile = null;
    Document docSqlMap = null;
    if(dbPlatform.equals("db2i")) {
      sqlServerXmlFile = new File(xmlFile.getPath().toString().replace("db2i","sqlserver"));
    }
    
    try {
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(xmlFile);
      doc.getDocumentElement().normalize();
      if(dbPlatform.equals("db2i")) {
        docSqlMap = docBuilder.parse(sqlServerXmlFile);
        docSqlMap.getDocumentElement().normalize();
      }

      // Get the table name
      NodeList classNodeList = doc.getElementsByTagName("class");
      Node classNode = classNodeList.item(0);
      Element classElement = (Element) classNode;
      if(dbPlatform.equals("db2i")) {
        sqlClassNodeList = docSqlMap.getElementsByTagName("class");
        sqlClassNode = sqlClassNodeList.item(0);
        sqlClassElement = (Element) sqlClassNode;
      }
      

      beginCreateTableStatement(dbPlatform, classElement, sqlClassElement);
      setUniqueKeys(doc, docSqlMap);
      generateConstraints(dbPlatform);
      if (dbPlatform.equals("db2i")) {
        generateUniqueIndex();
      }

      // Get columns
      NodeList propertyNodeList = doc.getElementsByTagName("property");
      for (int i = 0; i < propertyNodeList.getLength(); i++) {
        Node propertyNode = propertyNodeList.item(i);
        if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
          Element propertyElement = (Element) propertyNode;
          longColumn = propertyElement.getAttribute("name");
          longColumnType = propertyElement.getAttribute("type");
          if (propertyNode.hasChildNodes()) {
            NodeList columnNodeList = propertyNode.getChildNodes();
            for (int x = 0; x < columnNodeList.getLength(); x++) {
              Node columnNode = columnNodeList.item(x);
              if (columnNode.getNodeType() == Node.ELEMENT_NODE) {
                Element columnElement = (Element) columnNode;
                column = columnElement.getAttribute("name");
                shortColumn = columnElement.getAttribute("name");
                if (dbPlatform.equals("db2i") && propertyElement.getAttribute("type").toString().equals("compositeDate")) {
                  longColumn = getLongCompositeDateName(propertyElement.getAttribute("name"), docSqlMap, x);
                }
                sqlType = columnElement.getAttribute("sql-type").toUpperCase();
                if (sqlType.equals("INTEGER")) {
                  sqlType = "INT";
                }
                if (sqlType.length() == 0) {
                  javaType = propertyElement.getAttribute("type");
                  if (javaType.equals("integer")) {
                    sqlType = "INT";
                  }
                  if (javaType.equals("long")) {
                    sqlType = "BIGINT";
                  }
                  if (javaType.equals("fixedDate")) {
                    sqlType = "NUMERIC(8)";
                  }
                  if (javaType.equals("date") || javaType.equals("calendar")) {
                    if (dbPlatform.equals("sqlserver")) {
                      sqlType = "DATETIME";
                    }
                    if (dbPlatform.equals("db2i")) {
                      sqlType = "TIMESTAMP";
                    }
                  }
                }
                if (keys.contains(column)) {
                  sqlType += " NOT NULL";
                }
                if (dbPlatform.equals("db2i")) {
                  ddl += "  " + longColumn.toUpperCase() + " FOR COLUMN " + shortColumn + " " + sqlType + ",\n";
                } else {
                  ddl += "  " + column + " " + sqlType + ",\n";
                }
              }
            }
          }
        }
      }
      ddl += constraints;
      if (dbPlatform.equals("db2i")) {
        if (udx.length() > 0) {
          ddl += udx;
        }
        ddl += "RENAME TABLE " + longTable + " TO SYSTEM NAME " + shortTable + ";";
      }
      ddl += "\n\n";
      if (dbPlatform.equals("db2i")) {
        System.out.println(longTable + " FOR SYSTEM NAME " + shortTable + " script generated.");
      } else {
        System.out.println(table + " script generated.");
      }
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
      System.out.println("Unable do create DocumentBuilder.");
    } catch (SAXException e) {
      e.printStackTrace();
      System.out.println("XML parsing error.");
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Error accessing file.");
    }
  }

  private String getLongCompositeDateName(String compositeDateName, Document docSqlMap, int childNodeNumber) {
    String returnColumnName = "";
    NodeList propertyNodeList = docSqlMap.getElementsByTagName("property");
    for (int i = 0; i < propertyNodeList.getLength(); i++) {
      Node propertyNode = propertyNodeList.item(i);
      if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
        Element propertyElement = (Element) propertyNode;
        if (propertyElement.getAttribute("name").equals(compositeDateName)) {
          NodeList columnNodeList = propertyNode.getChildNodes();
          Node columnNode = columnNodeList.item(childNodeNumber);
          Element columnElement = (Element) columnNode;
          returnColumnName = columnElement.getAttribute("name");
          break;
        }
      }
    }
    return returnColumnName;
  }
  
  private void beginCreateTableStatement(String dbPlatform, Element classElement, Element sqlClassElement) {
    if (dbPlatform.equals("sqlserver")) {
      table = classElement.getAttribute("table"); //.substring(0, classElement.getAttribute("name").length() - 4);
      ddl += "CREATE TABLE " + table + " (\n" + "  RECID BIGINT NOT NULL IDENTITY,\n"
          + "  VERSIONID BIGINT NOT NULL,\n";
    }
    if (dbPlatform.equals("db2i")) {
      table = sqlClassElement.getAttribute("table");
      shortTable = classElement.getAttribute("table");
      longTable = table.toUpperCase();
      ddl += "CREATE TABLE " + longTable + " (\n" + "  RECID BIGINT GENERATED ALWAYS AS IDENTITY,\n"
          + "  VERSIONID BIGINT NOT NULL,\n";
    }
  }

  private void generateConstraints(String dbPlatform) {
    if (dbPlatform.equals("sqlserver")) {
      constraints = "  CONSTRAINT PK_" + table + " PRIMARY KEY(RECID)";
      if (keys.length() > 0) {
        keys = keys.substring(0, keys.length() - 2);
        constraints += ",\n  CONSTRAINT UQ_" + table + " UNIQUE(" + keys + ")";
      }
      constraints += "\n);\nGO\n";
    }
    if (dbPlatform.equals("db2i")) {
      constraints = "  CONSTRAINT PK_" + longTable + " PRIMARY KEY(RECID)";
      constraints += "\n);\n";
    }
  }

  private void generateUniqueIndex() {
    if (keys.length() > 0) {
      keys = keys.substring(0, keys.length() - 2);
      udx = "CREATE UNIQUE INDEX UDX_" + longTable + " FOR SYSTEM NAME " + shortTable
          + " ON " + longTable + "(" + keys + ");\n";
    }
  }

  private void setUniqueKeys(Document doc, Document docSqlMap) {
    NodeList keyNodeList = null;
    if (docSqlMap != null) {
      keyNodeList = docSqlMap.getElementsByTagName("natural-id");
    } else {
      keyNodeList = doc.getElementsByTagName("natural-id");
    }
    //NodeList keyNodeList = doc.getElementsByTagName("natural-id");
    if (keyNodeList != null && keyNodeList.getLength() > 0) {
      for (int i = 0; i < keyNodeList.getLength(); i++) {
        Node propertyKeyNode = keyNodeList.item(i);
        if (propertyKeyNode.getNodeType() == Node.ELEMENT_NODE) {
          if (propertyKeyNode.hasChildNodes()) {
            NodeList propertyChildNodeList = propertyKeyNode.getChildNodes();
            for (int x = 0; x < propertyChildNodeList.getLength(); x++) {
              Node propertyChildNode = propertyChildNodeList.item(x);
              if (propertyChildNode.getNodeType() == Node.ELEMENT_NODE) {
                Element propertyElement = (Element) propertyChildNode;
                NodeList columnNodeList = propertyElement.getChildNodes();
                for (int y = 0; y < columnNodeList.getLength(); y++) {
                  Node columnNode = columnNodeList.item(y);
                  if (columnNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element columnElement = (Element) columnNode;
                    keys += columnElement.getAttribute("name") + ", ";
                    if (docSqlMap != null) {
                      keys = keys.toUpperCase();
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

}
