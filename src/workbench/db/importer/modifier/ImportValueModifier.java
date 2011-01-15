/*
 * ImportValueModifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer.modifier;

import workbench.db.ColumnIdentifier;

/**
 * An interface for modifiers that are applied during importing data.
 * 
 * @author Thomas Kellerer
 * @see workbench.interfaces.ImportFileParser#setValueModifier(workbench.db.importer.modifier.ImportValueModifier)
 */
public interface ImportValueModifier 
{
	int getSize();
	String modifyValue(ColumnIdentifier column, String value);
}
