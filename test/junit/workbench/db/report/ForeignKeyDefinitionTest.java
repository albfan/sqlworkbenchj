/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db.report;

import java.sql.DatabaseMetaData;

import workbench.db.DependencyNode;
import workbench.db.TableIdentifier;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ForeignKeyDefinitionTest
{

	public ForeignKeyDefinitionTest()
	{
	}


	@Test
	public void testIsDefinitionEqual()
	{
		TableIdentifier tbl = new TableIdentifier("ADDRESS");
		DependencyNode nodeOne = new DependencyNode(tbl);
		nodeOne.setDeferrableValue(DatabaseMetaData.importedKeyNotDeferrable);
		nodeOne.setUpdateActionValue(DatabaseMetaData.importedKeyNoAction);
		nodeOne.setDeleteActionValue(DatabaseMetaData.importedKeyNoAction);
		nodeOne.setEnabled(true);
		nodeOne.addColumnDefinition("PERSON_ID", "ID");

		DependencyNode nodeTwo = new DependencyNode(tbl);
		nodeTwo.setDeferrableValue(DatabaseMetaData.importedKeyNotDeferrable);
		nodeTwo.setUpdateActionValue(DatabaseMetaData.importedKeyNoAction);
		nodeTwo.setDeleteActionValue(DatabaseMetaData.importedKeyNoAction);
		nodeTwo.setEnabled(true);
		nodeTwo.addColumnDefinition("PERSON_ID", "ID");

		ReportTable ref1 = new ReportTable(new TableIdentifier("PERSON"));
		ForeignKeyDefinition one = new ForeignKeyDefinition(nodeOne);
		one.setForeignTable(ref1);

		ReportTable ref2 = new ReportTable(new TableIdentifier("PERSON"));
		ForeignKeyDefinition other = new ForeignKeyDefinition(nodeTwo);
		other.setForeignTable(ref2);

		boolean result = one.isDefinitionEqual(other);
		assertTrue(result);

		nodeTwo = new DependencyNode(tbl);
		nodeTwo.setDeferrableValue(DatabaseMetaData.importedKeyNotDeferrable);
		nodeTwo.setUpdateActionValue(DatabaseMetaData.importedKeyNoAction);
		nodeTwo.setDeleteActionValue(DatabaseMetaData.importedKeyNoAction);
		nodeTwo.setEnabled(true);
		nodeTwo.addColumnDefinition("PERSON_ID", "PER_ID");

		other = new ForeignKeyDefinition(nodeTwo);
		other.setForeignTable(ref2);
		assertFalse(one.isDefinitionEqual(other));

		nodeTwo = new DependencyNode(tbl);
		nodeTwo.setDeferrableValue(DatabaseMetaData.importedKeyNotDeferrable);
		nodeTwo.setUpdateActionValue(DatabaseMetaData.importedKeyNoAction);
		nodeTwo.setDeleteActionValue(DatabaseMetaData.importedKeyNoAction);
		nodeTwo.setEnabled(true);
		nodeTwo.addColumnDefinition("PERSON_ID", "id");

		other = new ForeignKeyDefinition(nodeTwo);
		other.setForeignTable(ref2);
		assertTrue(one.isDefinitionEqual(other));
	}


}
