/*
 * TableDependencyTree.java
 *
 * Created on October 22, 2002, 1:44 PM
 */

package workbench.db;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.tree.DefaultMutableTreeNode;
import workbench.log.LogMgr;
import workbench.storage.DataStore;

/**
 *
 * @author  workbench@kellerer.org
 */
public class TableDependency
{
	private WbConnection connection;
	private String tablename;
	private String catalog;
	private String schema;
	private DependencyNode tableRoot;
	private DbMetadata wbMetadata;
	private DatabaseMetaData dbMetadata;
	
	public TableDependency()
	{
	}
	
	public void setConnection(WbConnection aConn)
		throws SQLException
	{
		this.connection = aConn;
		this.wbMetadata = this.connection.getMetadata();
		this.dbMetadata = this.connection.getSqlConnection().getMetaData();
	}
	
	public void setTableName(String aCatalog, String aSchema, String aTable)
	{
		this.tablename = aTable;
		this.catalog = aCatalog;
		this.schema = aSchema;
	}

	public void readDependencyTree()
	{
		if (this.tablename == null) return;
		if (this.connection == null) return;
		this.tableRoot = new DependencyNode(this.catalog, this.schema, this.tablename);
		this.readTree(this.tableRoot);
	}
	
	/**
	 *	Create the dependency tree.
	 *	If treeParent is passed as null, the TreeNode for a display in a JTree 
	 *	are not created.
	 */
	private int readTree(DependencyNode parent)
	{
		String parentcatalog = parent.getCatalog();
		String parentschema = parent.getSchema();
		String parenttable = parent.getTable();
		//System.out.println("reading fk for " + table);
		try
		{
			ResultSet rs = this.dbMetadata.getExportedKeys(parentcatalog, parentschema, parenttable);
			
			// cache the contents of the result set as we
			// need to call this method recursively and we don't know
			// if the JDBC driver supports multiple open result sets!
			DataStore ds = new DataStore(rs, true);
			rs.close();
			
			DependencyNode child = null;
			String currentfk = null;
			String currenttable = null;
			DefaultMutableTreeNode treeNode = null;
			String catalog = null;
			String schema = null;
			String table = null;
			String fkname = null;
			
			int count = ds.getRowCount();
			for (int i=0; i<count; i++)
			{
				catalog = ds.getValueAsString(i, 4);
				schema = ds.getValueAsString(i, 5);
				table = ds.getValueAsString(i, 6);
        fkname = ds.getValueAsString(i, 11);
				
				if (child == null || !child.isDefinitionFor(catalog, schema, table, fkname))
				{
					child = new DependencyNode(catalog, schema, table);
					child.setParentTable(parentcatalog, parentschema, parenttable, fkname);
					parent.addChild(child);
				}
				String tablecolumn = ds.getValueAsString(i, 7); // the column in "table" referencing the other table
				String parentcolumn = ds.getValueAsString(i, 3); // the column in the parent table 
				child.addColumnDefinition(tablecolumn, parentcolumn);
  
				int children = this.readTree(child);
			}
      return count;
		}
		catch (Exception e)
		{
			LogMgr.logError("TableDependencyTree.readTree()", "Error when reading FK definition", e);
		}
    return 0;
	}

  public DependencyNode getRootNode() { return this.tableRoot; }
  
}
