/*
 * SourceStatementsHelp.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

/**
 *
 * @author support@sql-workbench.net
 */
public class SourceStatementsHelp
{
	
	public SourceStatementsHelp()
	{
	}
	
	public String explainMissingViewSourceSql(String product)
	{
		String explain = "Currently no SQL query is configured\n" +
			" to retrieve the source of a view for your DBMS.\n" + 
			"To enabled this, create a file called ViewSourceStatements.xml\n" +
			"in the same directory where Workbench.jar is located,\nwith the following content:\n\n" +
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>  \n" + 
			"<java version=\"1.4.0_01\" class=\"java.beans.XMLDecoder\">  \n" + 
			" \n" + 
			" <object class=\"java.util.HashMap\">  \n" + 
			"  <void method=\"put\">  \n" + 
			"   <string>" + product + "</string>  \n" + 
			"   <object class=\"workbench.db.GetMetaDataSql\">  \n" + 
			"    <void property=\"baseSql\">  \n" + 
			"     <string>THE SELECT TO RETRIEVE THE SOURCE FOR A VIEW</string>  \n" + 
			"    </void>  \n" + 
			"    <void property=\"objectNameField\">  \n" + 
			"     <string>THE COLUMN FROM THE SELECT THAT IDENTIFIES THE VIEW NAME</string>  \n" + 
			"    </void>  \n" + 
			"    <void property=\"schemaField\">  \n" + 
			"     <string>THE COLUMN FROM THE SELECT THAT IDENTIFIES THE VIEW SCHEMA (if necessary)</string>  \n" + 
			"    </void>  \n" + 
			"    <void property=\"orderBy\">  \n" + 
			"     <string>DEFINE AN ORDER BY CLAUSE FOR THE SELECT STATEMENT (if necessary)</string>  \n" + 
			"    </void>  \n" + 
			"   </object>  \n" + 
			"  </void>  \n" + 
			"   \n" + 
			" </object>  \n" + 
			"</java> ";		
		return explain;
	}

	public String explainMissingProcSourceSql(String product)
	{
		String explain = "Currently no SQL query is configured to retrieve the source\n" +
			"of a stored procedure for your DBMS.\n" + 
			"To enabled this, create a file called ProcSourceStatements.xml\n" +
			"in the same directory where Workbench.jar is located,\nwith the following content:\n\n" +
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>  \n" + 
			"<java version=\"1.4.0_01\" class=\"java.beans.XMLDecoder\">  \n" + 
			" \n" + 
			" <object class=\"java.util.HashMap\">  \n" + 
			"  <void method=\"put\">  \n" + 
			"   <string>" + product + "</string>  \n" + 
			"   <object class=\"workbench.db.GetMetaDataSql\">  \n" + 
			"    <void property=\"baseSql\">  \n" + 
			"     <string>THE SELECT TO RETRIEVE THE SOURCE FOR A PROCEDURE/FUNCTION</string>  \n" + 
			"    </void>  \n" + 
			"    <void property=\"objectNameField\">  \n" + 
			"     <string>THE COLUMN FROM THE SELECT THAT IDENTIFIES THE PROCEDURE NAME</string>  \n" + 
			"    </void>  \n" + 
			"    <void property=\"schemaField\">  \n" + 
			"     <string>THE COLUMN FROM THE SELECT THAT IDENTIFIES THE PROCEDURE SCHEMA (if necessary)</string>  \n" + 
			"    </void>  \n" + 
			"    <void property=\"orderBy\">  \n" + 
			"     <string>DEFINE AN ORDER BY CLAUSE FOR THE SELECT STATEMENT (if necessary)</string>  \n" + 
			"    </void>  \n" + 
			"   </object>  \n" + 
			"  </void>  \n" + 
			"   \n" + 
			" </object>  \n" + 
			"</java> ";		
		return explain;
	}
	
}
