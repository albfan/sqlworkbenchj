/*
 * RepositoryReporter.java
 *
 * Created on 12. August 2004, 23:09
 */

package workbench.db.report;

import java.sql.SQLException;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.storage.DataStore;

/**
 * Generate an report from a selection of database tables
 * @author  workbench@kellerer.org
 * 
 */
public class SchemaReporter
{
	private WbConnection dbConn;
	private TableIdentifier[] tables;
	private String xmlNamespace;
	
	public SchemaReporter(WbConnection conn)
	{
		this.dbConn = conn;
	}
	
	public void setTableList(TableIdentifier[] tableList)
	{
		this.tables = tableList;
	}
	
	public String getHtmlReport()
	{
		return "";
	}
	
	public String getXmlReport()
	{
		return "";
	}
	
	public String getXmlForTable(TableIdentifier tbl)
		throws SQLException
	{
		ReportTable table = new ReportTable(tbl, this.dbConn, this.xmlNamespace);
		return table.getXml();
	}
	
	public void setNamespace(String name) { this.xmlNamespace = name; }
	public String getNamespace() { return this.xmlNamespace; }
}
