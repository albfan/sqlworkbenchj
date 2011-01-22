/*
 * SourceStatementsHelp.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
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
			"     <string>THE SELECT TO RETRIEVE THE SOURCE FOR A VIEW</string>  \n" +
			"    </void>  \n" +
			"    <void property=\"objectNameField\">  \n" +
			"     <string>THE COLUMN FROM THE SELECT THAT IDENTIFIES THE VIEW NAME</string>  \n" +
			"    </void>  \n" +
			"    <void property=\"schemaField\">  \n" +
			"     <string>THE COLUMN FROM THE SELECT THAT IDENTIFIES THE VIEW SCHEMA (if necessary)</string>  \n" +
			"    </void>  \n" +
			"    <void property=\"orderBy\">  \n" +
			"     <string>DEFINE AN ORDER BY CLAUSE FOR THE SELECT STATEMENT</string>  \n" +
			"    </void>  \n" +
			"   </object>  \n" +
			"  </void>  \n" +
			"   \n" +
			" </object>  \n" +
			"</java>";
		return explain;
	}

	public String explainMissingProcSourceSql(String product)
	{
		String jarDir = WbManager.getInstance().getJarPath();
		WbFile xmlfile = new WbFile(jarDir, "ViewSourceStatements.xml");

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
