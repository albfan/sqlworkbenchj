/*
 * PersistenceClassGenerator.java
 *
 * Created on 31. Oktober 2002, 18:47
 */

package workbench.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.storage.DataStore;

/**
 *
 * @author  thomas
 */
public class PersistenceClassGenerator
{
	public static final String DEFAULT_TABLE_PATTERN = "%tablename%Persistence";
	public static final String DEFAULT_VALUE_PATTERN = "%tablename%ValueObject";
	private String getterPattern = "get%columnname%";
	private String setterPattern = "set%columnname%";
	private String tablePattern = DEFAULT_TABLE_PATTERN;
	private String valuePattern = DEFAULT_VALUE_PATTERN;
	
	private DataStore tableDefinition;
	private TableIdentifier table;
	private WbConnection connection;
	private boolean fullyQualifiedNames;
	private String tablepackage;
	private String valuepackage;
	private String valueOutputDir;
	private String tableOutputDir;
	
	private ArrayList pkColumns;
	private ArrayList columns;
	private int[] coltypes;
	private int[] colsizes;
	private int[] coldigits;
	private boolean cleanupUnderscores;
	
	public PersistenceClassGenerator()
	{
	}
	
	public void setConnection(WbConnection aConnection)
	{
		this.connection = aConnection;
		if (this.connection != null && this.table != null)
		{
			this.readTableDefinition();
		}
	}
	
	public void setTable(TableIdentifier anId) 
	{ 
		this.table = anId; 
		if (this.connection != null && this.table != null)
		{
			this.readTableDefinition();
		}
	}

	public void setValueOutputDir(String aDir) { this.valueOutputDir = aDir; }
	public void setTableOutputDir(String aDir) { this.tableOutputDir = aDir; }

	public boolean getUseFullyQualifiedNames() { return this.fullyQualifiedNames; }
	public void setUseFullyQualifiedNames(boolean aFlag) { this.fullyQualifiedNames = aFlag; }
	
	public void setValuePackageName(String aName) 
  { 
    if (aName != null && aName.trim().length() == 0) 
    {
      this.valuepackage = null;
    }
    else
    {
      this.valuepackage = aName; 
    }
  }
	
	public void setTablePackageName(String aName) 
  { 
    if (aName != null && aName.trim().length() == 0) 
    {
      this.tablepackage = null;
    }
    else
    {
      this.tablepackage = aName; 
    }
  }

	public void setTablePattern(String aPattern)
	{
		if (aPattern == null || aPattern.trim().length() == 0)
		{
			this.tablePattern = DEFAULT_TABLE_PATTERN;
		}
		else
		{
			this.tablePattern = aPattern;
		}
	}
	
	public void setValuePattern(String aPattern)
	{
		this.valuePattern = aPattern;
	}
	
	private void readTableDefinition()
	{
		try
		{
			this.pkColumns = new ArrayList();
			this.columns = new ArrayList();
			DbMetadata meta = this.connection.getMetadata();
			DataStore def = meta.getTableDefinition(this.table.getCatalog(), this.table.getSchema(), this.table.getTable());
			int cols = def.getRowCount();
			this.coltypes = new int[cols];
			this.colsizes = new int[cols];
			this.coldigits = new int[cols];
			for (int i=0; i < cols; i++)
			{
				String column = def.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
				String pk = def.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_PK_FLAG);
				int type = def.getValueAsInt(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_TYPE_ID, 0);
				int size = def.getValueAsInt(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_SCALE, 0);
				int digits = def.getValueAsInt(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_PRECISION, 0);
				
				this.columns.add(column);
				if ("YES".equals(pk))
				{
					this.pkColumns.add(column);
				}
				this.coltypes[i] = type;
				this.colsizes[i] = size;
				this.coldigits[i] = digits;
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("PersistenceClassGenerator.readTableDefinition()", "Error reading table definition for " + this.table.getTable(), e);
		}
	}
	
	public String[] generateFiles(boolean copyBaseFiles)
		throws IOException
	{
		String base[] = null;
		String result[] = null;
		
		if (copyBaseFiles) 
		{
			base = this.copyBaseFiles();
		}
		
		if (base != null)
		{
			result = new String[4];
			result[2] = base[0];
			result[3] = base[1];
		}
		else
		{
			result = new String[2];
		}
		result[0] = this.writeTableClass();
		result[1] = this.writeValueObjectClass();
		return result;
	}
	
	private String writeTableClass()
		throws IOException
	{
		String source = this.generateTableClass();
		String filename = this.tableOutputDir + "/" + this.getTableClassName() + ".java";
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
		writer.println(source);
		writer.close();
		return filename;
	}

	private String writeValueObjectClass()
		throws IOException
	{
		String source = this.generateValueClass();
		String filename = this.valueOutputDir + "/" + this.getValueClassName() + ".java";
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
		writer.println(source);
		writer.close();
		return filename;
	}
	
	public String generateTableClass()
	{
		String className = this.getTableClassName();
		String valueClass = this.getValueClassName();
		StringBuffer result = new StringBuffer(6000);
		if (this.tablepackage != null)
		{
			result.append("package ");
			result.append(this.tablepackage);
			result.append(";");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append(StringUtil.LINE_TERMINATOR);
		}
		result.append("public class ");
		result.append(className);
		result.append(" extends BaseTablePersistence");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append('{');
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\tpublic ");result.append(className);result.append("()");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t{");
		result.append(StringUtil.LINE_TERMINATOR);
		
		result.append("\t\tthis.setTablename(\"");
		if (this.getUseFullyQualifiedNames())
		{
			result.append(this.table.getTableExpression());
		}
		else
		{
			result.append(this.table.getTable());
		}
		result.append("\");");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t\tthis.valueObjectClass=\"");
		if (this.valuepackage != null)
		{
			result.append(this.valuepackage);
			result.append('.');
		}
		result.append(valueClass);
		result.append("\";");
		result.append(StringUtil.LINE_TERMINATOR);
		
		for (int i=0; i < this.pkColumns.size(); i++)
		{
			result.append("\t\tthis.addPkColumn(\"");
			result.append( (String)this.pkColumns.get(i));
			result.append("\");");
			result.append(StringUtil.LINE_TERMINATOR);
		}
		
		for (int i=0; i < this.columns.size(); i++)
		{
			result.append("\t\tthis.addColumn(\"");
			result.append( (String)this.columns.get(i));
			result.append("\");");
			result.append(StringUtil.LINE_TERMINATOR);
		}
		
		result.append("\t}");
		result.append(StringUtil.LINE_TERMINATOR);

		result.append("\tpublic " + valueClass + " get" + valueClass + "()");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t{");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t\treturn (" + valueClass + ")this.getValueObject();");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t}");
		result.append(StringUtil.LINE_TERMINATOR);
		
		String table = StringUtil.capitalize(this.table.getTable().toLowerCase());
		
		result.append("\tpublic " + valueClass + " get" + table + "PkValueObject()");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t{");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t\treturn (" + valueClass + ")this.getPkValueObject();");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t}");
		result.append(StringUtil.LINE_TERMINATOR);
		
		
		result.append('}');
		result.append(StringUtil.LINE_TERMINATOR);
		return result.toString();
	}
	
	public String generateValueClass()
	{
		String className = this.getValueClassName();
		StringBuffer result = new StringBuffer(4000);
		if (this.valuepackage != null)
		{
			result.append("package ");
			result.append(this.valuepackage);
			result.append(";");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append(StringUtil.LINE_TERMINATOR);
		}
		result.append("public class ");
		result.append(className);
		result.append(" extends BaseValueObject");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append('{');
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\tpublic ");result.append(className);result.append("()");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t{");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t}");
		result.append(StringUtil.LINE_TERMINATOR);
		for (int i=0; i < this.columns.size(); i++)
		{
			String col = (String)this.columns.get(i);
			result.append(this.generateGetter(col, i));
			result.append(this.generateSetter(col, i));
		}
		result.append(StringUtil.LINE_TERMINATOR);
		result.append('}');
		result.append(StringUtil.LINE_TERMINATOR);
		return result.toString();
	}

	private String generateSetter(String aCol, int index)
	{
		StringBuffer result = new StringBuffer(500);
		String clz = SqlUtil.getJavaClass(this.coltypes[index], this.colsizes[index], this.coldigits[index]);
		String fname = "set" + this.convertColNameToMethod(aCol);
		result.append("\tpublic void ");
		result.append(fname);
		result.append("Value");
		result.append("(");
		result.append(clz);
		result.append(" value");
		result.append(")");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t{");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t\tthis.setColumnValue(\"");
		result.append(aCol);
		result.append("\",value);");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t}");
		result.append(StringUtil.LINE_TERMINATOR);
		String prim = SqlUtil.getJavaPrimitive(clz);
		if (prim != null)
		{
			result.append("\tpublic void " + fname);
			result.append("(" + prim + " value)");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append("\t{");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append("\t\t");result.append(clz);
			result.append(" objValue = new ");
			result.append(clz);result.append("(value);");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append("\t\tthis.setColumnValue(\"");
			result.append(aCol);
			result.append("\",objValue);");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append("\t}");
			result.append(StringUtil.LINE_TERMINATOR);
		}
		return result.toString();
	}
	
	private String generateGetter(String aCol, int colindex)
	{
		StringBuffer result = new StringBuffer(500);
		String clz = SqlUtil.getJavaClass(this.coltypes[colindex], this.colsizes[colindex], this.coldigits[colindex]);
		if (clz == null) clz = "Object";
		String fname = "get" + this.convertColNameToMethod(aCol);
		result.append("\tpublic ");
		result.append(clz);
		result.append(' ');
		result.append(fname);
		result.append("Value()");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t{");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t\t");
		result.append(clz);
		result.append(" value = ");
		result.append('(');result.append(clz);
		result.append(")this.getColumnValue(\"");
		result.append(aCol);
		result.append("\");");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t\treturn value;");
		result.append(StringUtil.LINE_TERMINATOR);
		result.append("\t}");
		result.append(StringUtil.LINE_TERMINATOR);
		
		String prim = SqlUtil.getJavaPrimitive(clz);
		if (prim != null)
		{
			result.append("\tpublic " + prim + " " + fname + "()");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append("\t{");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append("\t\treturn " + fname + "Value()." + prim + "Value();");
			result.append(StringUtil.LINE_TERMINATOR);
			result.append("\t}");
			result.append(StringUtil.LINE_TERMINATOR);
		}
		
		return result.toString();
	}
	
	private String convertColNameToMethod(String aString)
	{
		if (aString == null) return null;
		int pos = aString.indexOf('_');
		if (!this.cleanupUnderscores || pos < 0)
		{
			return StringUtil.capitalize(aString.toLowerCase());
		}
		StringBuffer result = new StringBuffer(aString.length());
		int len = aString.length();
		result.append(Character.toUpperCase(aString.charAt(0)));
		for (int i=1; i < len; i++)
		{
			char c = aString.charAt(i);
			if (c == '_') 
			{
				if (i < len - 1) 
				{
					i++;
					c = Character.toUpperCase(aString.charAt(i));
				}
			}
			else
			{
				c = Character.toLowerCase(aString.charAt(i));
			}
			result.append(c);
		}
		return result.toString();
	}
	private String getValueClassName()
	{
		String table = StringUtil.capitalize(this.table.getTable().toLowerCase());
		return StringUtil.replace(this.valuePattern, "%tablename%", table);
	}
	
	private String getTableClassName()
	{
		String table = StringUtil.capitalize(this.table.getTable().toLowerCase());
		return  StringUtil.replace(this.tablePattern, "%tablename%", table);
	}

	private static WbConnection getConnection()
		throws SQLException, ClassNotFoundException
	{
		Connection con;
		Class.forName("org.hsqldb.jdbcDriver");
		//Class.forName("com.inet.tds.TdsDriver");
		//Class.forName("oracle.jdbc.OracleDriver");
		//Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
		//con = DriverManager.getConnection("jdbc:inetdae:demsqlvisa02:1433?database=visa_cpl_test", "visa", "savivisa");
		//con = DriverManager.getConnection("jdbc:inetdae:reosqlpro08:1433?database=visa", "visa", "savivisa");
		//con = DriverManager.getConnection("jdbc:oracle:thin:@DEMRDB34:1521:SBL1", "sadmin", "sadmin");
		//con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:oradb", "auto", "auto");
		//con = DriverManager.getConnection("jdbc:oracle:oci8:@oradb", "auto", "auto");
		//con = DriverManager.getConnection("jdbc:odbc:Patsy");
		//con = DriverManager.getConnection("jdbc:hsqldb:d:\\daten\\db\\hsql\\test", "sa", null);
		con = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost", "sa", null);

		return new WbConnection(con);
	}
	
	public String[] copyBaseFiles()
		throws IOException
	{
		ClassLoader loader = this.getClass().getClassLoader();
		String[] result = new String[2];
		InputStream in = null;
		String outputfile = null;
		if (this.valueOutputDir != null)
		{
			in = loader.getResourceAsStream("workbench/util/BaseValueObject.java");
			outputfile = this.valueOutputDir + "/BaseValueObject.java";
			result[0] = outputfile;
			this.copyFile(in, outputfile, this.valuepackage);
		}
	
		if (this.tableOutputDir != null)
		{
			in = loader.getResourceAsStream("workbench/util/BaseTablePersistence.java");
			outputfile = this.tableOutputDir + "/BaseTablePersistence.java";
			result[1] = outputfile;
			this.copyFile(in, outputfile, this.tablepackage);
		}
		return result;
	}
	
	private void copyFile(InputStream in, String outputfile, String targetPackage)
		throws IOException
	{
		if (in == null)
		{
			System.out.println("could not open base file!");
			return;
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputfile)));
		String line = reader.readLine();
		while (line != null)
		{
			if (line.indexOf("package") > -1)
			{
				if (targetPackage == null || targetPackage.trim().length() == 0) line = "";
				else line = "package " + targetPackage + ";";
			}
			writer.println(line);
			line = reader.readLine();
		}
		reader.close();
		writer.close();
	}
	
	public static void main(String args[])
	{
	}
	
	/** Getter for property cleanupUnderscores.
	 * @return Value of property cleanupUnderscores.
	 *
	 */
	public boolean isCleanupUnderscores()
	{
		return cleanupUnderscores;
	}
	
	/** Setter for property cleanupUnderscores.
	 * @param cleanupUnderscores New value of property cleanupUnderscores.
	 *
	 */
	public void setCleanupUnderscores(boolean cleanupUnderscores)
	{
		this.cleanupUnderscores = cleanupUnderscores;
	}
	
}

