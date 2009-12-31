/*
 * SynonymDDLHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.log.LogMgr;
import workbench.util.StringUtil;

/**
 * A class to re-create the SQL for a Synonym
 * 
 * @author Thomas Kellerer
 */
public class SynonymDDLHandler
{
	public SynonymDDLHandler()
	{
	}

	/**
	 * Return the SQL statement to recreate the given synonym.
	 *
	 * @return the SQL to create the synonym.
	 * @see SynonymReader#getSynonymSource(workbench.db.WbConnection, java.lang.String, java.lang.String)
	 */
	public String getSynonymSource(WbConnection dbConnection, TableIdentifier synonym, boolean includeTable)
	{
		SynonymReader reader = dbConnection.getMetadata().getSynonymReader();
		if (reader == null) return StringUtil.EMPTY_STRING;
		StringBuilder result = new StringBuilder(100);
		TableIdentifier tbl = synonym.createCopy();
		tbl.adjustCase(dbConnection);
		try
		{
			String source = reader.getSynonymSource(dbConnection, tbl.getSchema(), tbl.getTableName());
			result.append(source);
		}
		catch (Exception e)
		{
			result.setLength(0);
		}

		if (StringUtil.isNonBlank(synonym.getComment()))
		{
			CommentSqlManager mgr = new CommentSqlManager(dbConnection.getMetadata().getDbId());
			String sql = mgr.getCommentSqlTemplate(synonym.getType());
			if (StringUtil.isNonBlank(sql))
			{
				sql = sql.replace(CommentSqlManager.COMMENT_OBJECT_NAME_PLACEHOLDER, synonym.getRawTableName());
				sql = sql.replace(CommentSqlManager.COMMENT_PLACEHOLDER, synonym.getComment().replace("'", "''"));
				result.append('\n');
				result.append(sql);
				result.append(";\n");
			}
		}

		if (includeTable)
		{
			try
			{
				TableIdentifier syn = dbConnection.getMetadata().getSynonymTable(tbl.getSchema(), tbl.getTableName());
				if (syn != null)
				{
					TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(dbConnection);

					String tableSql = builder.getTableSource(syn, false, true);
					if (StringUtil.isNonBlank(tableSql))
					{
						result.append("\n\n");
						result.append("-------------- ");
						result.append(syn.getTableExpression(dbConnection));
						result.append(" --------------");
						result.append("\n\n");
						result.append(tableSql);
					}
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("SynonymDDLHandler.getSynonymSource()", "Error when retrieving source for synonym table", e);
			}
		}

		return result.toString();
	}

}
