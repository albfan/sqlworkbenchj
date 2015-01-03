/*
 * DataConverter.java
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
package workbench.storage;

/**
 *
 * An interface for the RowData class to "convert" data that is read from the database
 * on the fly.
 * <br/>
 * Any new implementation should be created through {@link RowDataReader#getConverterInstance(workbench.db.WbConnection)   }
 * to ensure that the RowData class actually uses the converter.
 * <br/><br/>
 * An implementation of this interface should be done as a singleton, because a reference to the
 * converter is passed to every RowData instance that is created in the factory. Therefor a Singleton is
 * is recommended to avoid too many instances of the implementing class.
 *
 * @author Thomas Kellerer
 * @see RowDataReader#setConverter(workbench.storage.DataConverter)
 */
public interface DataConverter
{
	boolean convertsType(int jdbcType, String dbmsType);
	Object convertValue(int jdbcType, String dbmsType, Object originalValue);
	Class getConvertedClass(int jdbcType, String dbmsType);
}
