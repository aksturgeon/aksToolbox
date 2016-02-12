package com.aksturgeon.toolbox.sql;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.aksturgeon.toolbox.Dashboard;
import com.ibm.genelco.kernel.data.hibernate.DataAccessObjectHbn;
import com.ibm.genelco.kernel.data.hibernate.HibernateRequestBuilder;

/**
 * Iterates through the *View[0-9]HbnBase.java files and extracts the list of
 * access path keys from the configureRequesteBuilder method. Generates a create
 * index statement for each access path identified.
 * 
 * @author Ken Sturgeon
 * 
 */
public class SqlViewIndexGenerator {
  private final List<File> fileList = new ArrayList<File>();

  /**
   * Entry point
   */
  public void generateAccessPathIndexes(int subsystem, Properties props, int selectedDbPlatform) {
    String platform = "";
    String sourceFolder = "";
    String outputFolder = props.getProperty("dashboard.root.output.folder");

    if (selectedDbPlatform == Dashboard.DB_TYPE_SQLSERVER) {
      platform = "sqlserver";
    } else {
      platform = "db2i";
    }

    if (subsystem == Dashboard.CLC_SUBSYSTEM) {
      sourceFolder = props.getProperty("dashboard.root.clc.folder");
      outputFolder += "/clc";
    }
    if (subsystem == Dashboard.GLC_SUBSYSTEM) {
      sourceFolder = props.getProperty("dashboard.root.glc.folder")
          + "/grp_dao/src/com/ibm/genelco/gias/group/data/dao";
      outputFolder += "/glc";
    }
    if (subsystem == Dashboard.ILC_SUBSYSTEM) {
      sourceFolder = props.getProperty("dashboard.root.ilc.folder")
          + "/ind_dao/src/com/ibm/genelco/gias/individual/data/dao";
      outputFolder += "/ilc";
    }
    File sourceDir = new File(sourceFolder);
    populateViewList(sourceDir);
    generateSQLIndexes(selectedDbPlatform, outputFolder);
  }

  private void generateSQLIndexes(int selectedDbPlatform, String outputFolder) {
    String clsPath = "";
    String daoClassName = "";
    String keyOrder = "";
    String lastDaoClassName = "";
    String appendage = "";
    boolean firstIndex = true;
    boolean firstKey = true;
    boolean[] keyDirection = null;
    boolean isUnique = false;

    for (File file : fileList) {
      int begin = file.getAbsolutePath().indexOf("com\\");
      clsPath = file.getAbsolutePath().substring(begin, file.getAbsolutePath().length() - 5);
      clsPath = clsPath.replace("\\", ".");
      // System.out.println("clsPath = " + clsPath);
      Class<?> daoClass = null;
      try {
        daoClass = Class.forName(clsPath);
      } catch (ClassNotFoundException e) {
        System.out.println(e.getMessage());
      }

      // get the key fields for the View
      DataAccessObjectHbn daoInstance = null;
      try {
        daoInstance = (DataAccessObjectHbn) daoClass.newInstance();
      } catch (InstantiationException e) {
        System.out.println(e.getMessage());
      } catch (IllegalAccessException e) {
        System.out.println(e.getMessage());
      } catch (NullPointerException e) {
        System.out.println(e.getMessage());
        continue;
      }

      String[] keyNames = daoInstance.getKeyNames();
      firstIndex = true;

      // iterate the keys
      try {
        daoClassName = daoClass.getName().substring(daoClass.getName().lastIndexOf(".") + 1,
            daoClass.getName().indexOf("View"));
        appendage = daoClass.getName().substring(daoClass.getName().lastIndexOf("View") + 4,
            daoClass.getName().indexOf("HbnBase"));
        lastDaoClassName = daoClassName;
      } catch (Exception e) {
        e.printStackTrace();
      }

      // getDeclaredField works on the specific class - it ignores parent
      // classes
      // so have to walk up the hierarchy to the class where it is defined.
      Class<?> daoSuperClass = daoClass;
      while (true) {
        daoSuperClass = daoSuperClass.getSuperclass();
        if (daoSuperClass == null) {
          break;
        } else if (daoSuperClass.getName().equals("com.ibm.genelco.kernel.data.hibernate.DataAccessObjectHbn")) {
          break;
        }
      }
      // get the sort order for the key fields
      if (daoSuperClass != null) {
        Method m = null;
        try {
          m = daoSuperClass.getDeclaredMethod("getOrder", new Class<?>[] {});
        } catch (SecurityException e) {
          e.printStackTrace();
        } catch (NoSuchMethodException e) {
          e.printStackTrace();
        }
        // tell Java to skip the security/scope check
        m.setAccessible(true);
        try {
          keyOrder = (String) m.invoke(daoInstance, new Object[] {});
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }

        // get the request builder method
        try {
          m = daoSuperClass.getDeclaredMethod("getRequestBuilder", new Class<?>[] {});
        } catch (SecurityException e) {
          e.printStackTrace();
        } catch (NoSuchMethodException e) {
          e.printStackTrace();
        }
        // tell Java to skip the security/scope check
        m.setAccessible(true);
        // get the hibernate request builder
        HibernateRequestBuilder hibernateRequestBuilder = null;
        try {
          hibernateRequestBuilder = (HibernateRequestBuilder) m.invoke(daoInstance, new Object[] {});
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
        // get the key field directions
        Field field = null;
        try {
          field = hibernateRequestBuilder.getClass().getDeclaredField("keyDirection");
        } catch (SecurityException e) {
          e.printStackTrace();
        } catch (NoSuchFieldException e) {
          e.printStackTrace();
        }
        // tell Java to skip security/scope check
        field.setAccessible(true);
        Object value = null;
        try {
          value = field.get(hibernateRequestBuilder);
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
        keyDirection = (boolean[]) value;

        // see if the keys are unique
        try {
          field = hibernateRequestBuilder.getClass().getDeclaredField("uniquelyKeyed");
        } catch (SecurityException e) {
          e.printStackTrace();
        } catch (NoSuchFieldException e) {
          e.printStackTrace();
        }
        // tell Java to skip security/scope check
        field.setAccessible(true);
        value = null;
        try {
          value = field.get(hibernateRequestBuilder);
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
        isUnique = (Boolean) value;
        if (appendage.trim().length() > 0) {
          isUnique = false;
        }
      }

      String tableName = "";
      String[] keyFieldNames = null;
      File mappingFile = new File(outputFolder + "/" + daoClassName + "Base.hbn.xml");

      // TODO: START HERE... REACH INTO THE MAPPING FILE AND GRAB THE TABLE AND
      // KEY COLUMN NAMES BASED ON THE DB TYPE SELECTED

      String indexStmt = "";
      // if (appendage.length() == 0) {
      // indexStmt = "CREATE UNIQUE INDEX UDX_" + daoClassName;
      // } else {
      if (!isUnique) {
        indexStmt = "CREATE INDEX IDX_" + daoClassName;
      } else {
        if (selectedDbPlatform == Dashboard.DB_TYPE_DB2I) {
          indexStmt = "CREATE UNIQUE INDEX UDX_" + daoClassName;
        } else {
          indexStmt = "ALTER TABLE " + daoClassName + " ADD CONSTRAINT UQ_" + daoClassName;
        }
      }

      if (appendage.length() > 0) {
        indexStmt += appendage;
      }
      if ((selectedDbPlatform == Dashboard.DB_TYPE_DB2I) || (!isUnique && selectedDbPlatform == Dashboard.DB_TYPE_SQLSERVER)) {
        indexStmt += " ON " + daoClassName + " (";
      } else {
        indexStmt += " UNIQUE (";
      }
      int keyCount = keyNames.length;
      firstKey = true;
      for (int i = 0; i < keyCount; i++) {
        if (firstKey) {
          firstKey = false;
        } else {
          indexStmt += ", ";
        }
        indexStmt += keyNames[i];
        if (keyDirection[i] == false) {
          indexStmt += " desc";
        }
      }
      // Make the assumption that the views that are named the same as the
      // entity (without an appendage on the name) contain the natural key
      // access paths. Do nothing with these as they are covered by the unique
      // constraints.
      // if (appendage.length() > 0) {
      System.out.println(indexStmt + ");");
      // }
      // if (!isUnique) {
      // System.out.println(daoClassName + appendage);
      // }
    }
  }

  private void populateViewList(File sourceDir) {
    File[] files = sourceDir.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        // System.out.println("directory:" + file.getCanonicalPath());
        populateViewList(file);
      } else {
        if (file.getName().contains("View") && file.getName().contains("HbnBase") && file.getName().endsWith(".java")) {
          // System.out.println("     file:" + file.getCanonicalPath());
          // getRequestBuilderKeys(file);
          fileList.add(file);
        }
      }
    }
  }
}
