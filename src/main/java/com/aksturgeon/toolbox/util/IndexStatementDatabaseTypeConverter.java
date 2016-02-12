package com.aksturgeon.toolbox.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class IndexStatementDatabaseTypeConverter {
  // conversionType = sqlToDb2 or db2ToSql
  private static String conversionType = "sqlToDb2";
  private static String fileName = "C:\\aksToolbox\\clc-cmc-indexes-sqlserver.sql";
  private static String mapTableName = "fieldMap";

  public static void main(String[] args) throws ClassNotFoundException, SQLException {
    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    Connection conn = DriverManager
        .getConnection("jdbc:sqlserver://localhost;user=sa;password=Genelc01;database=gias352_field_map");
    Statement dbConnStatement = conn.createStatement();

    try {
      BufferedReader br = new BufferedReader(new FileReader(fileName));
      String sqlIdxStatement;
      while ((sqlIdxStatement = br.readLine()) != null) {
        if (conversionType.equals("sqlToDb2")) {
          generateDb2IndexFromSqlIndex(dbConnStatement, sqlIdxStatement);
        } else {
          generateSqlIndexFromDb2Index(dbConnStatement, sqlIdxStatement);
        }
      }
      br.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void generateSqlIndexFromDb2Index(Statement dbConnStatement, String db2IdxStatement)
      throws SQLException {
    // db2IdxStatement = "CREATE UNIQUE INDEX CMPARTYH01 ON CMPARTYHST
    // (CLIENTNUM, TRANSDATE DESC);";
    String db2Table = db2IdxStatement.substring(db2IdxStatement.indexOf("ON") + 3, db2IdxStatement.indexOf("(") - 1);
    String db2Keys = db2IdxStatement.substring(db2IdxStatement.indexOf("(") + 1, db2IdxStatement.indexOf(")"));
    String columns[] = db2Keys.split(",");

    boolean isUnique = db2IdxStatement.indexOf("UNIQUE") > 0;
    boolean isDescending = false;
    String idxAppendage = "";
    String db2Index = "";
    String sqlTable = "";
    String sqlColumn = "";
    String sqlIdxStatement = "";
    String sql = "SELECT DISTINCT SQLTable FROM " + mapTableName + " WHERE DB2Table = '" + db2Table + "'";
    ResultSet rsTable = dbConnStatement.executeQuery(sql);
    ResultSet rsColumn = null;

    if (rsTable.next()) {
      sqlTable = rsTable.getString("SQLTable");
      db2Index = db2IdxStatement.substring(db2IdxStatement.indexOf("INDEX") + 6, db2IdxStatement.indexOf("ON") - 1);
      if (isUnique) {
        // Create unique constraint
        sqlIdxStatement = "ALTER TABLE " + sqlTable + " ADD CONSTRAINT UQ_" + sqlTable + " UNIQUE (";
      } else {
        // Create index (multiple indexes may exist for a table; find the
        // "appendage" that differentiates them)
        idxAppendage = getDb2IndexAppendage(db2Table, db2Index);
        sqlIdxStatement = "CREATE INDEX IDX_" + sqlTable + idxAppendage + " ON " + sqlTable + " (";
      }

      for (String column : columns) {
        if (column.toUpperCase().endsWith(" DESC")) {
          isDescending = true;
          column = column.substring(0, column.length() - 5);
        }
        if (column.trim().equals("time")) {
          column = "timeFld";
        }
        sql = "SELECT SQLTable, SQLColumn FROM " + mapTableName + " WHERE DB2Table = '" + db2Table
            + "' AND DB2Column = '" + column.trim() + "'";
        rsColumn = dbConnStatement.executeQuery(sql);
        if (rsColumn.next()) {
          sqlColumn = rsColumn.getString("SQLColumn");
        } else {
          sqlColumn = "ERROR:" + column.trim();
        }
        if (isDescending) {
          sqlIdxStatement += sqlColumn + " DESC, ";
        } else {
          sqlIdxStatement += sqlColumn + ", ";
        }
        isDescending = false;
      }
      sqlIdxStatement = sqlIdxStatement.substring(0, sqlIdxStatement.length() - 2) + ");";
      System.out.println(db2Table + "." + db2Index + ": " + sqlIdxStatement);
    }
  }

  // THIS METHOD NEEDS TO BE REWRITTEN TO USE THE LEGACY I5 TABLE NAMES (-PF)
  // FOR THE INDEX NAMES
  private static void generateDb2IndexFromSqlIndex(Statement dbConnStatement, String sqlIdxStatement)
      throws SQLException {
    // String sqlIdxStatement =
    // "CREATE INDEX IDX_LedgerEntryPrevMth2A ON LedgerEntryPrevMth
    // (companyCode, currencyCode, generalLedgerAcctNum, referenceNumber1,
    // glTransactionDate);";
    String sqlTable = sqlIdxStatement.substring(12, sqlIdxStatement.indexOf(" ADD CONSTRAINT"));
    String idxAppendage = "";
    String idxPrefix = "";
    try {
      if (sqlIdxStatement.indexOf("IDX_") >= 0) {
        idxAppendage = sqlIdxStatement.substring(sqlIdxStatement.indexOf("IDX_") + 4 + sqlTable.length(),
            sqlIdxStatement.indexOf("ON") - 1);
        idxPrefix = "IDX_";
      } else {
        idxAppendage = sqlIdxStatement.substring(sqlIdxStatement.indexOf("UDX_") + 4 + sqlTable.length(),
            sqlIdxStatement.indexOf("ON") - 1);
        idxPrefix = "UDX_";
      }
    } catch (Exception e) {
      // e.printStackTrace();
      System.out.println("ERROR ON " + sqlIdxStatement);
    }

    String sqlKeys = sqlIdxStatement.substring(sqlIdxStatement.indexOf("(") + 1, sqlIdxStatement.indexOf(")"));
    String columns[] = sqlKeys.split(",");

    boolean isDescending = false;
    String db2Table = "";
    String db2Column = "";
    String db2IdxStatement = "";
    String sql = "SELECT DISTINCT DB2Table FROM " + mapTableName + " WHERE SQLTable = '" + sqlTable + "'";
    ResultSet rsTable = dbConnStatement.executeQuery(sql);
    ResultSet rsColumn = null;
    if (rsTable.next()) {
      db2Table = rsTable.getString("DB2Table");
      db2IdxStatement = sqlTable + ": CREATE INDEX " + idxPrefix + db2Table + idxAppendage + " ON " + db2Table + " (";
      for (String column : columns) {
        if (column.endsWith(" desc")) {
          isDescending = true;
          column = column.substring(0, column.length() - 5);
        }
        if (column.trim().equals("time")) {
          column = "timeFld";
        }
        sql = "SELECT DB2Table, DB2Column FROM " + mapTableName + " WHERE SQLTable = '" + sqlTable
            + "' AND SQLColumn = '" + column.trim() + "'";
        rsColumn = dbConnStatement.executeQuery(sql);
        if (rsColumn.next()) {
          db2Column = rsColumn.getString("DB2Column");
        } else {
          db2Column = "ERROR:" + column.trim();
        }
        if (isDescending) {
          db2IdxStatement += db2Column + " DESC, ";
        } else {
          db2IdxStatement += db2Column + ", ";
        }
        isDescending = false;
      }
      db2IdxStatement = db2IdxStatement.substring(0, db2IdxStatement.length() - 2) + ");";
    }
    // System.out.println(sqlIdxStatement);
    System.out.println(db2IdxStatement);
    // System.out.println("");
  }

  private static String getDb2IndexAppendage(String db2Table, String db2Index) {
    int diffAt = indexOfDifference(db2Table.substring(2), db2Index);
    if (db2Table.startsWith("PF")) {
      diffAt = indexOfDifference(db2Table.substring(2), db2Index);
    }else {
      diffAt = indexOfDifference(db2Table, db2Index);
    }
    
    if (diffAt == -1) {
        return "";
    }
    return db2Index.substring(diffAt);
  }

  public static int indexOfDifference(String str1, String str2) {
    if (str1 == str2) {
      return -1;
    }
    if (str1 == null || str2 == null) {
      return 0;
    }
    int i;
    for (i = 0; i < str1.length() && i < str2.length(); ++i) {
      if (str1.charAt(i) != str2.charAt(i)) {
        break;
      }
    }
    if (i < str2.length() || i < str1.length()) {
      return i;
    }
    return -1;
  }
}
