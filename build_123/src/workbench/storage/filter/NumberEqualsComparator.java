/*
 * NumberEqualsComparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.storage.filter;

import workbench.util.NumberUtil;
import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class NumberEqualsComparator
	implements ColumnComparator
{

	@Override
	public boolean supportsIgnoreCase()
	{
		return false;
	}

	@Override
	public String getValueExpression(Object value)
	{
		return (value == null ? "" : value.toString());
	}

	@Override
	public String getOperator()
	{
		return "=";
	}

	@Override
	public String getUserDisplay()
	{
		return getOperator();
	}

	@Override
	public boolean needsValue()
	{
		return true;
	}

	@Override
	public boolean comparesEquality()
	{
		return true;
	}

	@Override
	public boolean evaluate(Object reference, Object value, boolean ignoreCase)
	{
		if (reference == null || value == null)
		{
			return false;
		}
		try
		{
			if ((reference instanceof Number) && (value instanceof Number))
			{
				return NumberUtil.valuesAreEqual((Number)reference, (Number)value);
			}
			return reference.equals(value);
		}
		catch (Exception e)
		{
			return false;
		}
	}

	@Override
	public boolean supportsType(Class valueClass)
	{
		return Number.class.isAssignableFrom(valueClass);
	}

	@Override
	public boolean equals(Object other)
	{
		return (other.getClass().equals(this.getClass()));
	}

	@Override
	public boolean validateInput(Object value)
	{
		return value == null ? false : StringUtil.isNumber(value.toString());
	}
}
