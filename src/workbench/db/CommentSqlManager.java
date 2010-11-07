/*
 * CommentSqlManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class CommentSqlManager
{
	/**
	 * The placeholder for the fully qualified object (table, view, ...) name
	 */
	public static final String COMMENT_FQ_OBJECT_NAME_PLACEHOLDER = "%fq_object_name%";
	public static final String COMMENT_OBJECT_NAME_PLACEHOLDER = "%object_name%";
	public static final String COMMENT_CATALOG_PLACEHOLDER = "%catalog%";
	public static final String COMMENT_SCHEMA_PLACEHOLDER = "%schema%";
	public static final String COMMENT_COLUMN_PLACEHOLDER = MetaDataSqlManager.COLUMN_NAME_PLACEHOLDER;
	public static final String COMMENT_PLACEHOLDER = "%comment%";

	private String dbid;

	public CommentSqlManager(String id)
	{
		this.dbid = id;
	}

	public String getCommentSqlTemplate(String objectType)
	{
		if (StringUtil.isBlank(objectType)) return null;

		objectType = objectType.toLowerCase().replace(" ", "_");

		String defaultValue = Settings.getInstance().getProperty("workbench.db.sql.comment." + objectType, null);
		String key = "workbench.db." + dbid + ".sql.comment." + objectType;
		String sql = Settings.getInstance().getProperty(key, defaultValue);
		if (StringUtil.isEmptyString(sql))
		{
			// If the DB specific property is present, but empty, this means
			// the database does not support this type of comments.
			// if I did not test for presence of the key, the default would
			// always be returned, and thus it would not be possible to "delete"
			// the default by overwriting it with an empty key
			if (Settings.getInstance().isPropertyDefined(key))
			{
				return null;
			}
			else
			{
				sql = defaultValue;
			}
		}
		sql = Settings.getInstance().replaceProperties(sql);
		return SqlUtil.trimSemicolon(sql);
	}
}
