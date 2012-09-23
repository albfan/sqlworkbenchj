/*
 * TableChangeValidator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import workbench.db.DbMetadata;
import workbench.db.DbObjectChanger;
import workbench.db.WbConnection;
import workbench.gui.components.DataStoreTableModel;
import workbench.storage.InputValidator;

/**
 *
 * @author Thomas Kellerer
 */
public class TableChangeValidator
	implements InputValidator
{
	private DbObjectChanger changer;

	public TableChangeValidator()
	{
	}

	@Override
	public boolean isValid(Object newValue, int row, int col, DataStoreTableModel source)
	{
		if (changer == null) return false;

		String type = (String)source.getValueAt(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
		if (col == DbMetadata.COLUMN_IDX_TABLE_LIST_NAME)
		{
			return changer.getRenameObjectSql(type) != null;
		}
		else if (col == DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS)
		{
			return changer.getCommentSql(type) != null;
		}
		else if (col == DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA)
		{
			return changer.getChangeSchemaSql(type) != null;
		}
		else if (col == DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG)
		{
			return changer.getChangeCatalogSql(type) != null;
		}
		return false;
	}

	public void setConnection(WbConnection con)
	{
		if (con != null)
		{
			changer = new DbObjectChanger(con);
		}
		else
		{
			changer = null;
		}
	}
}
