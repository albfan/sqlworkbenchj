/*
 * DefaultDataTypeResolver.java
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
package workbench.db;
import workbench.util.SqlUtil;
/**
 * @author Thomas Kellerer
 */
public class DefaultDataTypeResolver
	implements DataTypeResolver
{

	@Override
	public String getColumnClassName(int type, String dbmsType)
	{
		return null;
	}

	/**
	 * Returns the correct display for the given data type.
	 *
	 * @see workbench.util.SqlUtil#getSqlTypeDisplay(java.lang.String, int, int, int)
	 */
	@Override
	public String getSqlTypeDisplay(String dbmsName, int sqlType, int size, int digits)
	{
		return SqlUtil.getSqlTypeDisplay(dbmsName, sqlType, size, digits);
	}

	/**
	 * Default implementation, does not change the datatype
	 * @param type the java.sql.Types as returned from the driver
	 * @param dbmsType the DBMS data type as returned from the driver
	 * @return the passed type
	 */
	@Override
	public int fixColumnType(int type, String dbmsType)
	{
		// Nothing to do
		return type;
	}

}
