/*
 * SpreadsheetReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.importer;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author Thomas Kellerer
 */
public interface SpreadsheetReader
{
	List<String> getHeaderColumns();
	void setActiveWorksheet(int index);
	List<Object> getRowValues(int row);
	void setNullString(String nullString);

	/**
	 * Return the total row count in the spreadsheet including a possible header row.
	 *
	 * @return the row count
	 */
	int getRowCount();
	void done();
	void load()
		throws IOException;

	List<String> getSheets();
}
