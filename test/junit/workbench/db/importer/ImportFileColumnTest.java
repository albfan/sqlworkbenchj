/*
 * ImportFileColumnTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.util.List;
import org.junit.Test;
import workbench.db.ColumnIdentifier;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ImportFileColumnTest
{

	@Test
	public void testEquals()
	{
		List<ImportFileColumn> columns = ImportFileColumn.createList();
		columns.add(new ImportFileColumn(new ColumnIdentifier("firstname")));
		columns.add(new ImportFileColumn(new ColumnIdentifier("lastname")));
		columns.add(new ImportFileColumn(new ColumnIdentifier("person_id")));

		int index = columns.indexOf("firstname");
		assertEquals(0, index);

		index = columns.indexOf("person_id");
		assertEquals(2, index);

		index = columns.indexOf(new ColumnIdentifier("person_id"));
		assertEquals(2, index);

	}

}
