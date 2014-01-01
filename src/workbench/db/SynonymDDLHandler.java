/*
 * SynonymDDLHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	public String getSynonymSource(WbConnection dbConnection, TableIdentifier synonym, boolean includeTable, boolean includeDrop)
	{
		SynonymReader reader = dbConnection.getMetadata().getSynonymReader();
		if (reader == null) return StringUtil.EMPTY_STRING;
		StringBuilder result = new StringBuilder(100);

		if (includeDrop)
		{
			GenericObjectDropper dropper = new GenericObjectDropper();
			dropper.setConnection(dbConnection);
			result.append(dropper.getDropForObject(synonym));
			result.append(";\n");
		}
		TableIdentifier tbl = synonym.createCopy();
		tbl.adjustCase(dbConnection);
		try
		{
			String source = reader.getSynonymSource(dbConnection, tbl.getCatalog(), tbl.getSchema(), tbl.getTableName());
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
				TableIdentifier syn = dbConnection.getMetadata().getSynonymTable(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName());
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
