package com.aksturgeon.toolbox.dbDocHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.aksturgeon.toolbox.Dashboard;
import com.ibm.genelco.kernel.data.container.Entity;

class DbDictionaryMap {
  String db2iColumn;
  String db2iTable;
  String wrapperReturnType;
  String entityName;
  String propertyName;
  String sqlColumn;
  String sqlTable;
  String sqlType;
  String db2iType;
  String hbnType;
}

/**
 * Iterates through the reassembled mapping files and... 1. Extracts the entity
 * class name. 2. Instantiates the entity class. 3. Iterates the fields in the
 * mapping file and... a. Calls the entity wrapper method to get dictionary
 * class. b. Prepares an insert statement into a table used to generate the
 * database documentation.
 * 
 * @author Ken Sturgeon
 */
public class DbFieldMapper {
  List<DbDictionaryMap> ddMapList = new ArrayList<DbDictionaryMap>();

  /**
   * Entry point
   * 
   * @param subsystem
   * @param props
   */
  public void generateDatabaseMap(int subsystem, Properties props) {
    File db2iMapDirectory = null;
    File sqlMapDirectory = null;
    File outputFile = null;
    String rootOutputFolder = props.getProperty("dashboard.root.output.folder");
    if (subsystem == Dashboard.CLC_SUBSYSTEM) {
      db2iMapDirectory = new File(rootOutputFolder + "/clc/db2i");
      sqlMapDirectory = new File(rootOutputFolder + "/clc/sqlserver");
      outputFile = new File(rootOutputFolder + "/field-map-clc.sql");
    }
    if (subsystem == Dashboard.GLC_SUBSYSTEM) {
      db2iMapDirectory = new File(rootOutputFolder + "/glc/db2i");
      sqlMapDirectory = new File(rootOutputFolder + "/glc/sqlserver");
      outputFile = new File(rootOutputFolder + "/field-map-glc.sql");
    }
    if (subsystem == Dashboard.ILC_SUBSYSTEM) {
      db2iMapDirectory = new File(rootOutputFolder + "/ilc/db2i");
      sqlMapDirectory = new File(rootOutputFolder + "/ilc/sqlserver");
      outputFile = new File(rootOutputFolder + "/field-map-ilc.sql");
    }

    // List<DbDictionaryMap> ddMapList = new ArrayList<DbDictionaryMap>();
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder;
    try {
      docBuilder = docBuilderFactory.newDocumentBuilder();
      File[] sqlXmlFileList = sqlMapDirectory.listFiles();
      System.out.println("directory: " + sqlMapDirectory.getName());
      System.out.println("file count: " + sqlXmlFileList.length);
      for (File sqlXmlFile : sqlXmlFileList) {
        // System.out.println("file: " + sqlXmlFile.getName());
        // System.out.println(sqlXmlFile.getName());
        // open the db2 map
        File db2iXmlFile = new File(db2iMapDirectory + "/" + sqlXmlFile.getName());
        Document sqlDoc = docBuilder.parse(sqlXmlFile);
        Document db2iDoc = docBuilder.parse(db2iXmlFile);
        sqlDoc.getDocumentElement().normalize();
        db2iDoc.getDocumentElement().normalize();

        NodeList packageNodeList = sqlDoc.getElementsByTagName("hibernate-mapping");
        NodeList sqlNodeList = sqlDoc.getElementsByTagName("class");
        NodeList db2iNodeList = db2iDoc.getElementsByTagName("class");
        Node packageNode = packageNodeList.item(0);
        Node sqlClassNode = sqlNodeList.item(0);
        Node db2iClassNode = db2iNodeList.item(0);

        Element packageElement = (Element) packageNode;
        Element sqlClassElement = (Element) sqlClassNode;
        Element db2iClassElement = (Element) db2iClassNode;

        String packageName = packageElement.getAttribute("package");
        String entityName = sqlClassElement.getAttribute("name");
        String sqlTableName = sqlClassElement.getAttribute("table");
        String db2iTableName = db2iClassElement.getAttribute("table");

        // Iterate the nodes in the sql doc and find the corresponding node in
        // the db2i doc
        String sqlPropertyName = "";
        String sqlColumnName = "";
        String db2iColumnName = "";
        String hbnType = "";
        String sqlType = "";
        String db2iType = "";

        // printHeader(packageName, entityName, sqlTableName, db2iTableName);

        DbDictionaryMap ddRecIdMap = new DbDictionaryMap();
        ddRecIdMap.entityName = packageName + "." + entityName;
        ddRecIdMap.propertyName = "uniqueID";
        ddRecIdMap.hbnType = "long";
        ddRecIdMap.sqlTable = sqlTableName;
        ddRecIdMap.sqlColumn = "RECID";
        ddRecIdMap.sqlType = "BIGINT";
        ddRecIdMap.db2iTable = db2iTableName;
        ddRecIdMap.db2iColumn = "RECID";
        ddRecIdMap.db2iType = "BIGINT";
        ddRecIdMap.wrapperReturnType = "Unique identifier";
        ddMapList.add(ddRecIdMap);

        DbDictionaryMap ddVersionIdMap = new DbDictionaryMap();
        ddVersionIdMap.entityName = packageName + "." + entityName;
        ddVersionIdMap.propertyName = "version";
        ddVersionIdMap.hbnType = "long";
        ddVersionIdMap.sqlTable = sqlTableName;
        ddVersionIdMap.sqlColumn = "VERSIONID";
        ddVersionIdMap.sqlType = "BIGINT";
        ddVersionIdMap.db2iTable = db2iTableName;
        ddVersionIdMap.db2iColumn = "VERSIONID";
        ddVersionIdMap.db2iType = "BIGINT";
        ddVersionIdMap.wrapperReturnType = "Tracks nuumber of record changes";
        ddMapList.add(ddVersionIdMap);

        try {
          Entity entityInstance = getEntityInstance(packageName + "." + entityName);

          NodeList sqlPropertyNodeList = sqlDoc.getElementsByTagName("property");
          NodeList db2iPropertyNodeList = db2iDoc.getElementsByTagName("property");
          for (int i = 0; i < sqlPropertyNodeList.getLength(); i++) {

            // get property name
            Node sqlPropertyNode = sqlPropertyNodeList.item(i);
            if (sqlPropertyNode.getNodeType() == Node.ELEMENT_NODE) {
              Element sqlPropertyElement = (Element) sqlPropertyNode;
              hbnType = sqlPropertyElement.getAttribute("type");
              sqlPropertyName = sqlPropertyElement.getAttribute("name");
              // get the matching db2i element
              Element db2iPropertyElement = getDb2iPropertyElement(db2iPropertyNodeList, sqlPropertyName);

              // get column name
              NodeList sqlColumnNodeList = sqlPropertyElement.getChildNodes();
              NodeList db2iColumnNodeList = db2iPropertyElement.getChildNodes();
              for (int x = 0; x < sqlColumnNodeList.getLength(); x++) {
                Node sqlColumnNode = sqlColumnNodeList.item(x);
                Node db2iColumnNode = db2iColumnNodeList.item(x);
                if (sqlColumnNode.getNodeType() == Node.ELEMENT_NODE) {
                  // bypass "type" element nodes
                  if (sqlColumnNode.getNodeName().equals("column")) {
                    Element sqlColumnElement = (Element) sqlColumnNode;
                    sqlColumnName = sqlColumnElement.getAttribute("name");
                    sqlType = sqlColumnElement.getAttribute("sql-type");

                    Element db2iColumnElement = (Element) db2iColumnNode;
                    db2iColumnName = db2iColumnElement.getAttribute("name");
                    db2iType = db2iColumnElement.getAttribute("sql-type");

                    // for the CMC maps the hbnType must be populated from the
                    // type node instead of the type attribute
                    if (hbnType.trim().length() == 0) {
                      NodeList hbnTypeNodeList = sqlPropertyNode.getChildNodes();
                      for (int y = 0; y < hbnTypeNodeList.getLength(); y++) {
                        Node hbnTypeNode = hbnTypeNodeList.item(y);
                        if (hbnTypeNode.getNodeName().equals("type")) {
                          Element hbnTypeElement = (Element) hbnTypeNode;
                          hbnType = hbnTypeElement.getAttribute("name");
                        }
                      }
                    }

                    DbDictionaryMap ddMap = new DbDictionaryMap();
                    ddMap.entityName = packageName + "." + entityName;
                    ddMap.propertyName = sqlPropertyName;
                    ddMap.hbnType = hbnType;
                    ddMap.sqlTable = sqlTableName;
                    ddMap.sqlColumn = sqlColumnName;
                    ddMap.sqlType = sqlType;
                    ddMap.db2iTable = db2iTableName;
                    ddMap.db2iColumn = db2iColumnName;
                    ddMap.db2iType = db2iType;
                    ddMap.wrapperReturnType = getWrapperReturnType(entityInstance, sqlPropertyName);
                    ddMapList.add(ddMap);

                    // printDetail(ddMap.propertyName, ddMap.hbnType,
                    // ddMap.wrapperReturnType, ddMap.sqlTable,
                    // ddMap.sqlColumn, ddMap.sqlType, ddMap.db2iTable,
                    // ddMap.db2iColumn, ddMap.db2iType);
                  }
                }
              }
            }
          }
        } catch (Exception e) {
          System.out.println("**getEntityInstance EXCEPTION: " + packageName + "." + entityName + " does not exist");
        }
        // System.out.println("entityName: " + packageName + "." + entityName);
        //generateSqlInserts(ddMapList, outputFile);
        // generateCsvMap(ddMapList);
        generateCsvMapWithDictionary(ddMapList);
        ddMapList.clear();
      }
      // } catch (ParserConfigurationException e) {
      // e.printStackTrace();
      // System.out.println("Unable do create DocumentBuilder.");
      // } catch (SAXException e) {
      // e.printStackTrace();
      // System.out.println("XML parsing error.");
      // } catch (IOException e) {
      // e.printStackTrace();
      // System.out.println("Error accessing file.");
    } catch (Exception e) {
      System.out.println("**generateDatabaseMap EXCEPTION: " + e.getMessage());
    }
  }

  private void generateCsvMap(List<DbDictionaryMap> ddMapList) {
    String entityName = "";
    String packageName = "";
    // System.out.println("Package,Entity,SQL Table,SQL Column,DB2i Table,DB2i
    // Column,Dictionary Class");
    for (DbDictionaryMap ddMap : ddMapList) {
      entityName = ddMap.entityName.substring(ddMap.entityName.lastIndexOf(".") + 1);
      packageName = ddMap.entityName.substring(0, ddMap.entityName.lastIndexOf("."));
      // System.out.println(packageName + ";" + entityName + ";" + ddMap.hbnType
      // + ";" + ddMap.sqlTable + ";"
      // + ddMap.sqlColumn + ";" + ddMap.sqlType + ";" + ddMap.db2iTable + ";" +
      // ddMap.db2iColumn + ";"
      // + ddMap.db2iType);
      System.out.println(ddMap.sqlTable);
    }
  }

  private void generateCsvMapWithDictionary(List<DbDictionaryMap> ddMapList) {
    String entityName = "";
    String packageName = "";
    String wrapperReturnType = "";
    // System.out.println("Package,Entity,SQL Table,SQL Column,DB2i Table,DB2i
    // Column,Dictionary Class");
    for (DbDictionaryMap ddMap : ddMapList) {
      entityName = ddMap.entityName.substring(ddMap.entityName.lastIndexOf(".") + 1);
      packageName = ddMap.entityName.substring(0, ddMap.entityName.lastIndexOf("."));
      wrapperReturnType = ddMap.wrapperReturnType;
      if (wrapperReturnType.contains(".")) {
        wrapperReturnType = ddMap.wrapperReturnType.substring(wrapperReturnType.lastIndexOf(".") + 1);
      }
      System.out.println(packageName + ";" + entityName + ";" + ddMap.hbnType + ";" + ddMap.sqlTable + ";"
          + ddMap.sqlColumn + ";" + ddMap.sqlType + ";" + ddMap.db2iTable + ";" + ddMap.db2iColumn + ";"
          + ddMap.db2iType + ";" + wrapperReturnType);
    }
  }

  private void generateSqlInserts(List<DbDictionaryMap> ddMapList, File outputFile) {
    // System.out.println("Writing insert statements to " +
    // outputFile.getAbsolutePath());
    // try {
    // FileWriter fw = new FileWriter(outputFile.getAbsolutePath());
    // BufferedWriter bw = new BufferedWriter(fw);

    for (DbDictionaryMap ddMap : ddMapList) {
      String entityName = ddMap.entityName.substring(ddMap.entityName.lastIndexOf(".") + 1);
      String wrapperReturnType = "";
      if (ddMap.wrapperReturnType != null && ddMap.wrapperReturnType.length() > 0) {
        wrapperReturnType = ddMap.wrapperReturnType.substring(ddMap.wrapperReturnType.lastIndexOf(".") + 1);
      }

      String insertStmt = "INSERT INTO FieldDictionaryMap"
          + " (entity,property,wrapperReturnType,sqlTable,db2iTable,sqlColumn,db2iColumn)" + " VALUES ('" + entityName
          + "','" + ddMap.propertyName + "','" + wrapperReturnType + "','" + ddMap.sqlTable + "','" + ddMap.db2iTable
          + "','" + ddMap.sqlColumn + "','" + ddMap.db2iColumn + "," + ddMap.sqlTable + "');";
      System.out.println(insertStmt);
      // bw.write(insertStmt);
      // bw.append(insertStmt);
    }
    // bw.close();
    // System.out.println("Completed writing insert statements to " +
    // outputFile.getAbsolutePath());
    // } catch (IOException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
  }

  private Element getDb2iPropertyElement(NodeList db2iPropertyNodeList, String sqlPropertyName) {
    Element db2iPropertyElement = null;
    for (int i = 0; i < db2iPropertyNodeList.getLength(); i++) {
      Element element = (Element) db2iPropertyNodeList.item(i);
      if (element.getAttribute("name").equals(sqlPropertyName)) {
        db2iPropertyElement = element;
      }
    }
    return db2iPropertyElement;
  }

  private String getWrapperReturnType(Entity entityInstance, String property) {
    String wrapperReturnType = "";
    String wrapperMethod = wrapperMethod = "wrap" + property.substring(0, 1).toUpperCase() + property.substring(1);

    if (entityInstance.getClass().toString().endsWith("PendBnftStatusChangeBase")) {
      if (property.equals("applyDate")) {
        wrapperMethod = "wrapApplicationDate";
      }
    }

    try {
      wrapperReturnType = entityInstance.getClass().getMethod(wrapperMethod).getReturnType().toString();
    } catch (Exception e) {
      //System.out.println("**getwrapperReturnType EXCEPTION: " + e.getMessage());
      return "unknown";
    }
    return wrapperReturnType;
  }

  private Entity getEntityInstance(String entityPath) {
    Class<?> entityClass = null;
    Entity entityInstance = null;
    try {
      entityClass = Class.forName(entityPath);
      // System.out.println("entityPath: " + entityPath);
      entityInstance = (Entity) entityClass.newInstance();
    } catch (ClassNotFoundException e) {
      System.out.println(e.getMessage());
    } catch (InstantiationException e) {
      System.out.println(e.getMessage());
    } catch (IllegalAccessException e) {
      System.out.println(e.getMessage());
    } catch (NoClassDefFoundError e) {
      System.out.println(e.getMessage());
    }
    return entityInstance;
  }

  private void printDetail(String propertyName, String hbnType, String wrapperReturnType, String sqlTable,
      String sqlColumn, String sqlType, String db2iTable, String db2iColumn, String db2iType) {
    System.out.println("property: " + propertyName);
    System.out.println("hbnType: " + hbnType);
    System.out.println("wrapperReturnType: " + wrapperReturnType);
    System.out.println("sqlColumn: " + sqlTable + "." + sqlColumn);
    System.out.println("db2iColumn: " + db2iTable + "." + db2iColumn);
    System.out.println("sqlType: " + db2iType);
    System.out.println("db2iType: " + db2iType);
    System.out.println();
  }

  private void printHeader(String packageName, String entityName, String sqlTableName, String db2iTableName) {
    System.out.println("------------------------------------------");
    System.out.println("Entity: " + packageName + "." + entityName);
    System.out.println("Table: " + sqlTableName + " / " + db2iTableName);
    System.out.println();
  }
}
