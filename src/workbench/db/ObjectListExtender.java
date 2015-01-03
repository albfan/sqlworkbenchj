/*
 * ObjectListExtender.java
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
	 * Adds the objects managed by this extender to the existing result.
	 *
	 * The DataStore's structure is the one created by DbMetadata.getObjects()
	 *
	 * @param con the database connection to be used
	 * @param result the DataStore containing the objects already returned by the driver
	 * @param requestedTypes the object types as passed to DbMetadata.getObjects()
	 * @return true if the list was changed
	 * @see DbMetadata#getObjects(java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 *
	 */
	boolean extendObjectList(WbConnection con, DataStore result, String aCatalog, String aSchema, String objects, String[] requestedTypes);

	/**
	 * Return a list of object types that this extender handles
	 */
	List<String> supportedTypes();

	/**
	 * Returns if the type is a type derived from some other type.
	 *
	 * This is currently only used in the DbExplorer to hide some types from the "Type dropdown"
	 *
	 * @return true if this type is derived from an existing one.
	 * @see DbMetadata#getObjectTypes() 
	 */
	boolean isDerivedType();

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
