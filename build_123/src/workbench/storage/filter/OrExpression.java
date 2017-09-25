/*
 * OrExpression.java
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

import java.util.Map;

/**
 * @author Thomas Kellerer
 */
public class OrExpression
	extends ComplexExpression
{
	@Override
	public boolean evaluate(Map<String, Object> columnValues)
	{
		for (FilterExpression expr : filter)
		{
			if (expr.evaluate(columnValues)) return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof OrExpression)
		{
			return super.equals(other);
		}
		else
		{
			return false;
		}
	}

	@Override
	public String toString()
	{
		StringBuilder value = new StringBuilder();
		for (FilterExpression expr : filter)
		{
			if (value.length() > 0) value.append(" AND ");
			value.append(expr.toString());
		}
		return value.toString();
	}

}
