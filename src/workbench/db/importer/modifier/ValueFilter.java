/*
 * ValueFilter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
