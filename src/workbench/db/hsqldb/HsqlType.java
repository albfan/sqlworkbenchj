/*
 * HsqlType.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.hsqldb;

import java.util.Collections;
import java.util.List;

import workbench.db.BaseObjectType;
import workbench.db.ColumnIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlType
	extends BaseObjectType
{
	private String dataType;

	public HsqlType(String schema, String typeName)
	{
		super(schema, typeName);
	}

	public void setDataTypeName(String typeName)
	{
		dataType = typeName;
	}

	public String getDataTypeName()
	{
		return dataType;
	}

	@Override
	public List<ColumnIdentifier> getAttributes()
	{
		return Collections.emptyList();
	}

	@Override
	public void setAttributes(List<ColumnIdentifier> attr)
	{
	}
}
