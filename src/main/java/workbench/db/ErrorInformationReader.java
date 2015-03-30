/*
 * ErrorInformationReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import workbench.db.oracle.OracleErrorInformationReader;

import workbench.sql.ErrorDescriptor;

/**
 * An interface for reading extended error information from the database.
 *
 * This should be implemented for any database that can return additional
 * error information after running DDL statements.
 *
 * Currently only implemented for Oracle to retrieve detailed error messages
 * after a CREATE PROCEDURE or similar statement.
 *
 * @author Thomas Kellerer
 * @see OracleErrorInformationReader
 */
public interface ErrorInformationReader
{
	ErrorDescriptor getErrorInfo(String originalSql, String schema, String object, String type, boolean formatMessage);
}
