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
import java.lang.ClassNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 *
 * @author  thomas
 */
public class PersistenceClassGenerator
{
	private String getterPattern = "get%columnname%";
	private String setterPattern = "set%columnname%";
	private String classnamePattern = "%tablename%Persistence";
	private String valueobjectPattern = "%tablename%ValueObject";
	
	private DataStore tableDefinition;
	private TableIdentifier table;
	private WbConnection connection;
	private boolean fullyQualifiedNames;
	private String packagename;
	private String outputDir;
	
	private ArrayList pkColumns;
	private ArrayList columns;
	private int[] coltypes;
	
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

	public void setOutputDir(String aDir) { this.outputDir = aDir; }
	public String getOutputDir() { return this.outputDir; }
	
	public boolean getUseFullyQualifiedNames() { return this.fullyQualifiedNames; }
	public void setUseFullyQualifiedNames(boolean aFlag) { this.fullyQualifiedNames = aFlag; }
	
	public void setPackageName(String aName) { this.packagename = aName; }
	public String getPackageName() { return this.packagename; }

	private void copyBaseClasses(String anOutputdir)
	{
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
			for (int i=0; i < cols; i++)
			{
				String column = (String)def.getValue(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
				String pk = (String)def.getValue(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_PK_FLAG);
				int type = ((Integer)def.getValue(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_TYPE_ID)).intValue();
				this.columns.add(column);
				if ("YES".equals(pk))
				{
					this.pkColumns.add(column);
				}
				this.coltypes[i] = type;
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("PersistenceClassGenerator.readTableDefinition()", "Error reading table definition for " + this.table.getTable(), e);
		}
	}
	
	public void generateFiles()
		throws IOException
	{
		this.copyBaseFiles();
		this.writeTableClass();
		this.writeValueObjectClass();
	}
	
	private void writeTableClass()
		throws IOException
	{
		String source = this.generateTableClass();
		String filename = this.outputDir + "/" + this.getTableClassName() + ".java";
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
		writer.println(source);
		writer.close();
	}

	private void writeValueObjectClass()
		throws IOException
	{
		String source = this.generateValueClass();
		String filename = this.outputDir + "/" + this.getValueClassName() + ".java";
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
		writer.println(source);
		writer.close();
	}
	
	public String generateTableClass()
	{
		String className = this.getTableClassName();
		String valueClass = this.getValueClassName();
		StringBuffer result = new StringBuffer(6000);
		if (this.packagename != null)
		{
			result.append("package ");
			result.append(this.packagename);
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
		if (this.packagename != null)
		{
			result.append(this.packagename);
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
		String className = StringUtil.capitalize(this.table.getTable()) + "ValueObject";
		StringBuffer result = new StringBuffer(4000);
		if (this.packagename != null)
		{
			result.append("package ");
			result.append(this.packagename);
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
			result.append(this.generateGetter(col, this.coltypes[i]));
			result.append(this.generateSetter(col, this.coltypes[i]));
		}
		result.append(StringUtil.LINE_TERMINATOR);
		result.append('}');
		result.append(StringUtil.LINE_TERMINATOR);
		return result.toString();
	}

	private String generateSetter(String aCol, int type)
	{
		StringBuffer result = new StringBuffer(500);
		String clz = SqlUtil.getJavaClass(type);
		String fname = "set" + StringUtil.capitalize(aCol.toLowerCase());
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
		String prim = SqlUtil.getJavaPrimitive(type);
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
	
	private String generateGetter(String aCol, int type)
	{
		StringBuffer result = new StringBuffer(500);
		String clz = SqlUtil.getJavaClass(type);
		if (clz == null) clz = "Object";
		String fname = "get" + StringUtil.capitalize(aCol.toLowerCase());
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
		
		String prim = SqlUtil.getJavaPrimitive(type);
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
	
	private String getValueClassName()
	{
		return StringUtil.capitalize(this.table.getTable()) + "ValueObject";
	}
	
	private String getTableClassName()
	{
		return StringUtil.capitalize(this.table.getTable()) + "Persistence";
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
	
	private void copyBaseFiles()
		throws IOException
	{
		if (this.packagename.equals("workbench.util")) return;
		ClassLoader loader = this.getClass().getClassLoader();
		InputStream in = loader.getResourceAsStream("workbench/util/BaseValueObject.java");
		String outputfile = this.getOutputDir() + "/BaseValueObject.java";
		this.copyFile(in, outputfile);
		in = loader.getResourceAsStream("workbench/util/BaseTablePersistence.java");
		outputfile = this.getOutputDir() + "/BaseTablePersistence.java";
		this.copyFile(in, outputfile);
	}
	
	private void copyFile(InputStream in, String outputfile)
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
				if (this.packagename == null) line = "";
				else line = "package " + this.packagename + ";";
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
}

