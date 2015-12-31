/*
 * OracleObjectType.java
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
package workbench.db.oracle;

import workbench.db.BaseObjectType;
import workbench.db.DbObject;
import workbench.db.WbConnection;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleObjectType
	extends BaseObjectType
{
	private int numMethods;
	private int numAttributes;

	public OracleObjectType(String owner, String objectName)
	{
		super(owner, objectName);
	}

	@Override
	public String getDropStatement(WbConnection con, boolean cascade)
	{
		return null;
	}

	public void setNumberOfMethods(int count)
	{
		this.numMethods = count;
	}

	public int getNumberOfMethods()
	{
		return numMethods;
	}

	public void setNumberOfAttributes(int count)
	{
		this.numAttributes = count;
	}

	@Override
	public int getNumberOfAttributes()
	{
		if (CollectionUtil.isEmpty(getAttributes()))
		{
			return numAttributes;
		}
		return getAttributes().size();
	}

	@Override
	public boolean isEqualTo(DbObject other)
	{
		boolean isEqual = super.isEqualTo(other);
		if (isEqual && (other instanceof OracleObjectType))
		{
			OracleObjectType otherType = (OracleObjectType)other;
			isEqual = this.getNumberOfMethods() == otherType.getNumberOfMethods();
		}
		return isEqual;
	}

}
