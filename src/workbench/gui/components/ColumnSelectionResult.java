/*
 * ColumnSelectionResult
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.components;

import java.util.List;
import workbench.db.ColumnIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnSelectionResult
{
	public boolean includeHeaders;
	public boolean selectedOnly;
	public List<ColumnIdentifier> columns;
}
