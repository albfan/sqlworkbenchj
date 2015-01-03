/*
 * ValueFilter.java
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
package workbench.db.importer.modifier;

import java.util.LinkedList;
import java.util.List;
import workbench.db.ColumnIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class ValueFilter
	implements ImportValueModifier
{
	private List<ImportValueModifier> modifiers = new LinkedList<ImportValueModifier>();

	@Override
	public int getSize()
	{
		int size = 0;
		for (ImportValueModifier modifier : modifiers)
		{
			size += modifier.getSize();
		}
		return size;
	}

	@Override
	public String modifyValue(ColumnIdentifier column, String value)
	{
		for (ImportValueModifier modifier : modifiers)
		{
			value = modifier.modifyValue(column, value);
		}
		return value;
	}

	/**
	 * Adds a column modifier.
	 * The modifiers are called in the order how they are added.
	 *
	 * @param modifier
	 */
	public void addColumnModifier(ImportValueModifier modifier)
	{
		this.modifiers.add(modifier);
	}

}
