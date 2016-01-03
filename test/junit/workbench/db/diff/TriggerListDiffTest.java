/*
 * TriggerListDiffTest.java
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
package workbench.db.diff;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;

import workbench.util.CollectionUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TriggerListDiffTest
	extends WbTestCase
{

	public TriggerListDiffTest()
	{
		super("TriggerListDiffTest");
	}

	@Test
	public void testWriteXml()
	{
		TriggerDefinition one = new TriggerDefinition(null, null, "trg_one");
		one.setRelatedTable(new TableIdentifier("PERSON"));
		one.setTriggerEvent("UPDATE");
		one.setTriggerType("BEFORE");
		one.setSource("CREATE TRIGGER trg_one BEFORE UPDATE ON person FOR EACH ROW EXECUTE PROCEDURE trg_func();");

		TriggerDefinition two = new TriggerDefinition(null, null, "trg_two");
		two.setRelatedTable(new TableIdentifier("ADDRESS"));
		two.setTriggerEvent("UPDATE");
		two.setTriggerType("BEFORE");
		two.setSource("CREATE TRIGGER trg_one BEFORE UPDATE ON address FOR EACH ROW EXECUTE PROCEDURE trg_func2();");

		TriggerDefinition three = new TriggerDefinition(null, null, "trg_to_add");
		three.setRelatedTable(new TableIdentifier("OTHER_TABLE"));
		three.setTriggerEvent("UPDATE");
		three.setTriggerType("AFTER");
		three.setSource("CREATE TRIGGER trg_to_add AFTER UPDATE ON other_table FOR EACH ROW EXECUTE PROCEDURE something();");

		TriggerDefinition oneC = new TriggerDefinition(null, null, "trg_one");
		oneC.setRelatedTable(new TableIdentifier("PERSON"));
		oneC.setTriggerEvent("UPDATE");
		oneC.setTriggerType("BEFORE");
		oneC.setSource("CREATE TRIGGER trg_one BEFORE UPDATE ON person FOR EACH ROW EXECUTE PROCEDURE trg_func();");

		TriggerDefinition twoC = new TriggerDefinition(null, null, "trg_two");
		twoC.setRelatedTable(new TableIdentifier("ADDRESS"));
		twoC.setTriggerEvent("UPDATE");
		twoC.setTriggerType("AFTER");
		twoC.setSource("CREATE TRIGGER trg_one AFTER UPDATE ON address FOR EACH ROW EXECUTE PROCEDURE trg_func2();");

		TriggerDefinition threeC = new TriggerDefinition(null, null, "trg_three");
		threeC.setRelatedTable(new TableIdentifier("SOME_TABLE"));
		threeC.setTriggerEvent("UPDATE OR DELETE");
		threeC.setTriggerType("AFTER");
		threeC.setSource("CREATE TRIGGER trg_three AFTER UPDATE OR DELETE ON some_table FOR EACH ROW EXECUTE PROCEDURE trg_func3();");

		List<TriggerDefinition> reference = CollectionUtil.arrayList(one, two, three);
		List<TriggerDefinition> toCompare = CollectionUtil.arrayList(oneC, twoC, threeC);

		TriggerListDiff diff = new TriggerListDiff(reference, toCompare);
		assertTrue(diff.hasChanges());
		StringBuilder result = new StringBuilder(500);
		result.append("<triggers>\n"); // make the result valid XML

		StringBuilder indent = new StringBuilder("  ");
		diff.writeXml(indent, result);
		result.append("</triggers>\n"); // make the result valid XML

//		System.out.println(result.toString());
		String xml = result.toString();
		String count = TestUtil.getXPathValue(xml, "count(/triggers/drop-trigger[@name='trg_three'])");
		assertEquals("1", count);

		count = TestUtil.getXPathValue(xml, "count(/triggers/update-trigger/trigger-def[trigger-name='trg_two'])");
		assertEquals("1", count);

		count = TestUtil.getXPathValue(xml, "count(/triggers/create-trigger/trigger-def[trigger-name='trg_to_add'])");
		assertEquals("1", count);
	}

}
