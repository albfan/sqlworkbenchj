/*
 * BooleanEqualsComparator.java
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

import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class BooleanEqualsComparator
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
		return "equals";
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

		Boolean refValue = null;
		Boolean compare = null;

		if (reference instanceof Boolean)
		{
			refValue = (Boolean)reference;
		}

		if (value instanceof Boolean)
		{
			compare = (Boolean)value;
		}

		if (reference instanceof String)
		{
			refValue = StringUtil.stringToBool((String)reference);
		}

		if (value instanceof String)
		{
			compare = StringUtil.stringToBool((String)value);
		}
		if (refValue == null || compare == null) return false;

		return refValue.booleanValue() == compare.booleanValue();
	}

	@Override
	public boolean supportsType(Class valueClass)
	{
		return Boolean.class.isAssignableFrom(valueClass);
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
