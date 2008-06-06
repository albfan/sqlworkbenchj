/*
 * RowDataComparer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.compare;

import workbench.resource.Settings;
import workbench.storage.DmlStatement;
import workbench.storage.RowData;
import workbench.storage.StatementFactory;

/**
 * Compare two {@link workbench.storage.RowData} objects to check for equality.
 * Used to generate the approriate SQL scripts when comparing the data from
 * two tables.
 * 
 * @author support@sql-workbench.net
 */
public class RowDataComparer 
{
	private RowData migrationData;
	private boolean targetWasNull;
	
	/**
	 * Compares two database rows.
	 * If the targetRow is null, it is assumed that it needs to be created.
	 * 
	 * @param referenceRow
	 * @param targetRow
	 */
	public RowDataComparer(RowData referenceRow, RowData targetRow)
	{
		int cols = referenceRow.getColumnCount();
		if (targetRow == null)
		{
			targetWasNull = true;
			migrationData = referenceRow.createCopy();
			migrationData.resetStatus();
		}
		else
		{
			targetWasNull = false;
			migrationData = targetRow.createCopy();
			migrationData.resetStatus();
			
			int tcols = migrationData.getColumnCount();
			if (cols != tcols) throw new IllegalArgumentException("Column counts must match!");

			for (int i=0; i < cols; i++)
			{
				// if the value passed to the target row is 
				// identical to the existing value, this will
				// not change the state of the RowData
				migrationData.setValue(i, referenceRow.getValue(i));
			}
		}
	}
	
	public DmlStatement getMigrationSql(StatementFactory factory)
	{
		String le = Settings.getInstance().getInternalEditorLineEnding();

		DmlStatement result = null;
		if (targetWasNull)
		{
			result = factory.createInsertStatement(migrationData, true, le);
		}
		else
		{
			result = factory.createUpdateStatement(migrationData, false, le);
		}
		return result;
	}
	
}
