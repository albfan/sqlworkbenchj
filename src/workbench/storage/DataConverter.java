/*
 * DataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

/**
 *
 * An interface for the RowData class to "convert" data that is read from the database
 * on the fly.
 * Any new implementation should be created in {@link RowDataFactory#createConverter(workbench.db.WbConnection) }
 * to ensure that the RowData class actually uses the converter.
 *
 * An implementation of this interface should be done as a singleton, because a reference to the
 * converter is passed to every RowData instance that is created in the factory. Therefor a Singleton is
 * is recommended to avoid too many instances of the implementing class.
 *
 * @author support@sql-workbench.net
 */
public interface DataConverter
{
	boolean convertsType(int jdbcType, String dbmsType);
	Object convertValue(int jdbcType, String dbmsType, Object originalValue);
}
