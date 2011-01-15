/*
 * ObjectListExtender.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public interface ObjectListEnhancer
{
	/**
	 * Updates the objects in the passed result DataStore with additional information (e.g. table remarks)
	 *
	 * @param con the database connection to be used
	 * @param result the DataStore containing the objects already returned by the driver
	 * @param catalogPattern the catalog pattern
	 * @param schemaPattern  the schema pattern
	 * @param objectNamePattern the object name pattern
	 * @param requestedTypes the object types as passed to DbMetadata.getObjects()
	 * 
	 */
	void updateObjectList(WbConnection con, DataStore result, String catalogPattern, String schemaPattern, String objectNamePattern, String[] requestedTypes);

}
