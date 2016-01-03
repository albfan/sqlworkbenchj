/*
 * TemplateHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.sqltemplates;

import java.io.IOException;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbObject;
import workbench.db.MetaDataSqlManager;
import workbench.db.QuoteHandler;
import workbench.db.WbConnection;

import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public abstract class TemplateHandler
{

	protected String getStringProperty(String property, String defaultValue)
	{
		String value= Settings.getInstance().getProperty(property, defaultValue);
		if (value != null && value.startsWith("@file:"))
		{
			value = readFile(value);
		}
		return value;
	}

	private String readFile(String propValue)
	{
		String fname = propValue.replace("@file:", "");
		WbFile f = new WbFile(fname);
		if (!f.isAbsolute())
		{
			f = new WbFile(Settings.getInstance().getConfigDir(), fname);
		}

		String sql = null;
		try
		{
			LogMgr.logDebug("TemplateHandler.readFile()", "Reading SQL template from: " + f.getAbsolutePath());
			sql = FileUtil.readFile(f, "UTF-8");
		}
		catch (IOException io)
		{
			LogMgr.logError("TemplateHandler.readFile", "Could not read file: " + fname, io);
			sql = null;
		}
		return sql;
	}

	/**
	 * Remove the placeholder completely from the template SQL.
	 *
	 * @param sql          the sql template
	 * @param placeholder  the placeholder
	 * @param withNL       controls replacing of newlines after the placeholder.
	 *                     if true, newlines after and whitespace before the placeholder are removed as well.
	 *
	 * @return the template with the placeholder removed
	 */
	public static String removePlaceholder(String sql, String placeholder, boolean withNL)
	{
		String s;
		if (withNL)
		{
			StringBuilder b = new StringBuilder(placeholder.length() + 10);
			b.append("[ \\t]*");
			b.append(StringUtil.quoteRegexMeta(placeholder));
			b.append("[\n|\r\n]?");
			s = b.toString();
		}
		else
		{
			s = StringUtil.quoteRegexMeta(placeholder);
		}
		return sql.replaceAll(s, StringUtil.EMPTY_STRING);
	}

  public static String replaceTablePlaceholder(String sql, DbObject table, WbConnection connection)
  {
    return replaceTablePlaceholder(sql, table, connection, false);
  }

  public static String replaceTablePlaceholder(String sql, DbObject table, WbConnection connection, boolean addWhitespace)
  {
    if (sql == null) return sql;
    if (table == null) return sql;
    QuoteHandler handler = connection == null ? QuoteHandler.STANDARD_HANDLER : connection.getMetadata();
    sql = replacePlaceholder(sql, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, handler.quoteObjectname(table.getObjectName()), addWhitespace);

    // do not call getObjectExpression() or getFullyQualifiedName() if not necessary.
    // this might trigger a SELECT to the database to get the current schema and/or catalog
    // to avoid unnecessary calls, this is only done if really needed

    if (sql.contains(MetaDataSqlManager.TABLE_EXPRESSION_PLACEHOLDER))
    {
      sql = replacePlaceholder(sql, MetaDataSqlManager.TABLE_EXPRESSION_PLACEHOLDER, table.getObjectExpression(connection), addWhitespace);
    }
    if (sql.contains(MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER))
    {
      sql = replacePlaceholder(sql, MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER, table.getFullyQualifiedName(connection), addWhitespace);
    }
    return sql;
  }

  public static String removeSchemaPlaceholder(String sql, char schemaSeparator)
  {
    return removeNamespacePlaceholder(sql, MetaDataSqlManager.SCHEMA_NAME_PLACEHOLDER, schemaSeparator);
  }

  public static String removeCatalogPlaceholder(String sql, char schemaSeparator)
  {
    return removeNamespacePlaceholder(sql, MetaDataSqlManager.CATALOG_NAME_PLACEHOLDER, schemaSeparator);
  }

	/**
	 * Remove the a schema or catalog placeholder completely from the template SQL.
	 *
	 * @param sql                the sql template
	 * @param placeholder        the placeholder
	 * @param namespaceSeparator if this character follows the placeholder, it is removed as well
	 *
	 * @return the template with the placeholder removed
   *
   * @see #removeSchemaPlaceholder(java.lang.String, java.lang.String)
   * @see #removeCatalogPlaceholder(java.lang.String, java.lang.String)
	 */
  public static String removeNamespacePlaceholder(String sql, String placeholder, char namespaceSeparator)
  {
    String clean = removePlaceholder(sql, placeholder, false);
    clean = removePlaceholder(sql, placeholder + namespaceSeparator, false);
    return clean;
  }

	/**
	 * Replace the placeholder in the given SQL template.
	 *
	 * If the template does not have whitespace before or after the placeholder a whitespace will be inserted.
	 *
	 * If replacement is null or an empty string, the placeholder will be removed.
	 *
	 * @param sql           the SQL template
	 * @param placeholder   the placeholder
	 * @param replacement   the replacement
   * @param addWhitespace if true a space will be added after the replacement
   *
	 * @return the template with the placeholder replaced
	 * @see #removePlaceholder(String, String, boolean)
	 */
	public static String replacePlaceholder(String sql, String placeholder, String replacement, boolean addWhitespace)
	{
		if (StringUtil.isEmptyString(replacement)) return removePlaceholder(sql, placeholder, false);
		int pos = sql.indexOf(placeholder);
		if (pos < 0) return sql;

		String realReplacement = replacement;

		if (pos > 1)
		{
			String opening = "([\"'`";
			char prev = sql.charAt(pos - 1);
			if (addWhitespace && !Character.isWhitespace(prev) && opening.indexOf(prev) == -1)
			{
				realReplacement = " " + realReplacement;
			}
		}

		if (pos + placeholder.length() < sql.length())
		{
			String closing = ")]\"'`";
			char next = sql.charAt(pos + placeholder.length());
			if (addWhitespace && !Character.isWhitespace(next) && closing.indexOf(next) == -1)
			{
				realReplacement = realReplacement + " ";
			}
		}
		return StringUtil.replace(sql, placeholder, realReplacement);
	}

}
