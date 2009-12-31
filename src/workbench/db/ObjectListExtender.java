/*
 * ObjectListExtender.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.List;
import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public interface ObjectListExtender
{
	/**
	 * Adds the objects managed by this extender to the existing result
	 * The DataStore's structure is the one created by DbMetadata.getObjects()
	 *
	 * @param con the database connection to be used
	 * @param result the DataStore containing the objects already returned by the driver
	 * @param requestedTypes the object types as passed to DbMetadata.getObjects()
	 * 
	 * @see DbMetadata#getObjects(java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	void extendObjectList(WbConnection con, DataStore result, String aCatalog, String aSchema, String objects, String[] requestedTypes);

	/**
	 * Return a list of object types that this extender handles
	 */
	List<String> supportedTypes();
		
	/**
	 * Checks if the extender handles the given type
	 * @param type
	 * @return true if the type is handled by the extender
	 * @see #handlesType(java.lang.String[])
	 */
	boolean handlesType(String type);

	/**
	 * Checks if at least one of the types is handled by this extender
	 *
	 * @param types
	 * @see #handlesType(java.lang.String) 
	 */
	boolean handlesType(String[] types);

	/**
	 * Retrieve a definition of the object so that it can be displayed
	 * in the DbExplorer's TableListPanel
	 *
	 * @param con the database connection to be used
	 * @param object the object to be retrieved
	 * @return a DataStore containing detailed information about the object
	 * @see DbMetadata#getObjectDetails(workbench.db.TableIdentifier)
	 */
	DataStore getObjectDetails(WbConnection con, DbObject object);

	/**
	 * Retrieves the "native" DbObject that is identified by
	 * the object name and type passed.
	 *
	 * @param con
	 * @param name
	 * @return a concrete DbObject
	 */
	DbObject getObjectDefinition(WbConnection con, DbObject name);

	String getObjectSource(WbConnection con, DbObject object);
}
