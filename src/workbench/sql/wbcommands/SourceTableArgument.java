/*
 * SourceTableArgument.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class SourceTableArgument
{
	private List<TableIdentifier> tables = new ArrayList<TableIdentifier>();
	private boolean wildcardsPresent = false;
	
  public SourceTableArgument(String argument, WbConnection dbConn)
		throws SQLException
  {
		if (StringUtil.isEmptyString(argument)) return;
		if (dbConn == null) return;
		
		List<String> args = StringUtil.getObjectNames(argument);
		int argCount = args.size();

		if (argCount <= 0) return;
		
		String t = args.get(0);
		
		// If only one table argument is present, we'll have to
		// to check for wildcards e.g. -sourcetable=theschema.*
		if (argCount == 1 && (t.indexOf('*') > -1 || t.indexOf('%') > -1))
		{
			this.wildcardsPresent = true;
			TableIdentifier tbl = new TableIdentifier(t);
			if (tbl.getSchema() == null)
			{
				tbl.setSchema(dbConn.getMetadata().getSchemaToUse());
			}
			tbl.adjustCase(dbConn);
			List<TableIdentifier> l = dbConn.getMetadata().getTableList(tbl.getTableName(), tbl.getSchema());
			this.tables.addAll(l);
		}
		else
		{
			for (int i=0; i < argCount; i++)
			{
				tables.add(new TableIdentifier(args.get(i)));
			}
		}
	}

	public List<TableIdentifier> getTables()
	{
		return Collections.unmodifiableList(this.tables);
	}

	public boolean wasWildCardArgument() 
	{
		return this.wildcardsPresent;
	}
}
