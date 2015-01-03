/*
 * TableSourceOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import workbench.db.objectcache.DbObjectCacheFactory;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectSourceOptions
	implements Serializable
{
	private static final long serialVersionUID = DbObjectCacheFactory.CACHE_VERSION_UID;

	private final String KEY_TYPE_MODIFIER = "typMod";
	private final String KEY_INLINE_OPTION = "inlineSql";
	private final String KEY_TABLE_OPTION = "tbleOpt";
	private final String KEY_ADDITIONAL_SQL = "addSql";

	private final Map<String, String> options = new HashMap<>();
	private final Map<String, String> configSettings = new HashMap<>();

	private boolean initialized;

	/**
	 * Return an expression that should be used in the CREATE TABLE statement to specify an optional table type.
	 * e.g. <tt>UNLOGGED</tt> TABLE for Postgres, or <tt>GLOBAL TEMPORARY</tt> for Oracle
	 */
	public String getTypeModifier()
	{
		return options.get(KEY_TYPE_MODIFIER);
	}

	public void setTypeModifier(String modifier)
	{
		this.options.put(KEY_TYPE_MODIFIER, modifier);
	}

	/**
	 * Return the SQL fragment that should be added after all column and constraint definitions, but before the closing bracket.
	 */
	public String getInlineOption()
	{
		return options.get(KEY_INLINE_OPTION);
	}

	/**
	 * Define the SQL fragment that should be added after all column and constraint definitions, but before the closing bracket.
	 */
	public void setInlineOption(String option)
	{
		options.put(KEY_INLINE_OPTION, option);
	}

	/**
	 * Return the SQL fragment that should be added after the closing ) of the CREATE TABLE statement.
	 *
	 * @see #setTableOption(java.lang.String)
	 */
	public String getTableOption()
	{
		return options.get(KEY_TABLE_OPTION);
	}

	/**
	 * Define the SQL fragment that should be added after the closing ) of the CREATE TABLE statement.
	 *
	 * @see #getTableOption()
	 */
	public void setTableOption(String option)
	{
		options.put(KEY_TABLE_OPTION, option);
	}

	/**
	 * Return a complete SQL statement that should be executed after the CREATE TABLE statement.
	 */
	public String getAdditionalSql()
	{
		return options.get(KEY_ADDITIONAL_SQL);
	}

	/**
	 * Define a complete SQL statement that should be executed after the CREATE TABLE statement.
	 * @see #getAdditionalSql()
	 */
	public void setAdditionalSql(String sql)
	{
		options.put(KEY_ADDITIONAL_SQL, sql);
	}

	/**
	 * Add a configuration setting that is used for the XML schema report.
	 * @param key    the DBMS specific option keyword
	 * @param value  the DBMS specific value
	 *
	 * @see #getConfigSettings()
	 */
	public void addConfigSetting(String key, String value)
	{
		this.configSettings.put(key, value);
	}

	/**
	 * Return a DBMS specific key/value mapping for table options.
	 * @return the settings defined through {@link #addConfigSetting(java.lang.String, java.lang.String)}
	 * @see #addConfigSetting(java.lang.String, java.lang.String)
	 */
	public Map<String, String> getConfigSettings()
	{
		return Collections.unmodifiableMap(configSettings);
	}

	public void setInitialized()
	{
		this.initialized = true;
	}

	public boolean isInitialized()
	{
		return this.initialized;
	}

	public ObjectSourceOptions createCopy()
	{
		ObjectSourceOptions copy = new ObjectSourceOptions();
		copy.options.putAll(this.options);
		copy.configSettings.putAll(this.configSettings);
		copy.initialized = this.initialized;
		return copy;
	}

}
