/*
 * SourceStatementsHelp.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.WbManager;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class SourceStatementsHelp
{

	public static final String VIEW_ERROR_START = "Support for displaying view source is currently not configured for:";
	public static final String PROC_ERROR_START = "Support for displaying procedure source is currently not configured for:";

	public String explainMissingViewSourceSql(String product)
	{
		String jarDir = WbManager.getInstance().getJarPath();
		WbFile xmlfile = new WbFile(jarDir, "ViewSourceStatements.xml");

		String explain = VIEW_ERROR_START + " " + product +
			"\n\nTo enable this, create the file:\n" + xmlfile.getFullPath() + "\n" +
			"using the supplied sample below, filling out the necessary SELECT statement where indicated, \n" +
			"to retrieve the source from the DBMS:\n\n" +
			"--- Example ViewSourceStatements.xml starts here ---\n" +
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>  \n" +
			"<java version=\"1.5\" class=\"java.beans.XMLDecoder\">  \n" +
			" \n" +
			" <object class=\"java.util.HashMap\">  \n" +
			"  <void method=\"put\">  \n" +
			"   <string>" + product + "</string>  \n" +
			"   <object class=\"workbench.db.GetMetaDataSql\">  \n" +
			"    <void property=\"baseSql\">  \n" +
			"     <string>The SELECT statement to retrieve the source for a view</string>  \n" +
			"    </void>  \n" +
			"    <void property=\"objectNameField\">  \n" +
			"     <string>The column name from the above SELECT that identifies the view name</string>  \n" +
			"    </void>  \n" +
			"    <void property=\"schemaField\">  \n" +
			"     <string>The column from the above SELECT that identifies the view schema (if necessary)</string>  \n" +
			"    </void>  \n" +
			"    <void property=\"orderBy\">  \n" +
			"     <string>Define an order by clause for the select statement in case the source is returned in more than one row.</string>  \n" +
			"    </void>  \n" +
			"   </object>  \n" +
			"  </void>  \n" +
			"   \n" +
			" </object>  \n" +
			"</java>\n";
		return explain;
	}

	public String explainMissingProcSourceSql(String product)
	{
		String jarDir = WbManager.getInstance().getJarPath();
		WbFile xmlfile = new WbFile(jarDir, "ProcSourceStatements.xml");

		String explain = PROC_ERROR_START + " " + product +
			"\n\nTo enable this, create the file\n" + xmlfile.getFullPath() + "\n" +
			"using the supplied sample below, filling out the necessary SELECT statement where indicated, \n" +
			"to retrieve the source from the DBMS:\n\n" +
			"--- Example ProcSourceStatements.xml starts here ---\n" +
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>  \n" +
			"<java version=\"1.5\" class=\"java.beans.XMLDecoder\">  \n" +
			" <object class=\"java.util.HashMap\">  \n" +
			"  <void method=\"put\">  \n" +
			"   <string>" + product + "</string>  \n" +
			"   <object class=\"workbench.db.GetMetaDataSql\">  \n" +
			"    <void property=\"baseSql\">  \n" +
			"     <string>The SELECT statement to retrieve the source for a Procedure/Function</string>  \n" +
			"    </void>  \n" +
			"    <void property=\"objectNameField\">  \n" +
			"     <string>The column name from the select that identifies the procedure name</string>  \n" +
			"    </void>  \n" +
			"    <void property=\"schemaField\">  \n" +
			"     <string>The column name from the select that identifies the procedure schema (if necessary)</string>  \n" +
			"    </void>  \n" +
			"    <void property=\"orderBy\">  \n" +
			"     <string>Define an order by clause for the above SELECT statement in case the source is returned in more than one row.</string>  \n" +
			"    </void>  \n" +
			"   </object>  \n" +
			"  </void>  \n" +
			"   \n" +
			" </object>  \n" +
			"</java>\n";
		return explain;
	}

}
