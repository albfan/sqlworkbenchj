package workbench.db;

import java.lang.IllegalArgumentException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DependencyNode
{
	private String tablecatalog;
	private String tableschema;
	private String table;
	
	private String parenttable;
	private String parentcatalog;
	private String parentschema;
	
  private String fkName;
  
	private HashMap columns;

	private ArrayList childTables;
	
	public DependencyNode(String aCatalog, String aSchema, String aTable)
	{
		if (aTable == null) throw new IllegalArgumentException("Table name may not be null");
		if (aTable.trim().length() == 0) throw new IllegalArgumentException("Table name may not be empty");
		this.table = aTable;
		this.tablecatalog = adjustCatalogSchemaName(aCatalog);
		this.tableschema = adjustCatalogSchemaName(aSchema);
		this.parenttable = null;
	}

	public void addColumnDefinition(String aColumn, String aParentColumn)
	{
		if (this.columns == null) this.columns = new HashMap(10);
		this.columns.put(aColumn, aParentColumn);
	}
	
	public void setParentTable(String aCatalog, String aSchema, String aTable, String aName)
	{
		if (aTable == null) throw new IllegalArgumentException("Parent table may not be null");
		if (aTable.trim().length() == 0) throw new IllegalArgumentException("Parent table may not be empty");
		
		this.parenttable = aTable;
		this.parentschema = adjustCatalogSchemaName(aSchema);
		this.parentcatalog = adjustCatalogSchemaName(aCatalog);
    this.fkName = aName;
	}
	
	public String toString() 
	{ 
		if (this.fkName == null)
		{
			return this.table;
		}
		else
		{
			return this.table + " (" + this.fkName + ")"; 
		}
	}
  public String getFkName() { return this.fkName; }
	public String getParentTable() { return this.parenttable; }
	public String getParentCatalog() { return this.parentcatalog; }
	public String getParentSchema() { return this.parentschema; }
	
	public String getTable() { return this.table; }
	public String getSchema() { return this.tableschema; }
	public String getCatalog() { return this.tablecatalog; }

	public Map getColumns() 
	{ 
		if (this.columns == null)
		{
			return Collections.EMPTY_MAP;
		}
		else
		{
			return this.columns; 
		}
	}

	public boolean isDefinitionFor(String aCatalog, String aSchema, String aTable, String aFkname)
	{
		if (this.tablecatalog == null && aCatalog != null) return false;
		if (this.tablecatalog != null && aCatalog == null) return false;
		
		if (this.tableschema == null && aSchema != null) return false;
		if (this.tableschema != null && aSchema == null) return false;

		if (aTable == null) return false;
		if (this.tablecatalog == null && aCatalog == null &&
		    this.tableschema == null && aSchema == null &&
				this.table.equals(aTable)) 
			return true;
		
		return (aTable.equals(this.table) && 
		        aCatalog.equals(this.tablecatalog) &&
						aSchema.equals(this.tableschema));
	}
	
	public boolean isRoot() { return this.parenttable == null; }
	
	public List getChildren()
	{
		if (this.childTables == null)
		{
			return Collections.EMPTY_LIST;
		}
		else
		{
			return this.childTables;
		}
	}
	
	public void addChild(DependencyNode aTable)
	{
		if (this.childTables == null) this.childTables = new ArrayList();
		this.childTables.add(aTable);
	}

	private String adjustCatalogSchemaName(String aName)
	{
		if (aName == null) return null;
		if (aName.length() == 0) return null;
		if ("*".equals(aName)) return null;
		if ("%".equals(aName)) return null;
		return aName;
	}
}
