/*
 * DependencyNodeTest.java
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

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class DependencyNodeTest
{

	@Test
	public void testGetLevel()
	{
		TableIdentifier root = new TableIdentifier("root");
		DependencyNode rootNode = new DependencyNode(root);
		assertEquals("Wrong level for root", 0, rootNode.getLevel());

		TableIdentifier child1 = new TableIdentifier("child1");
		DependencyNode cnode = rootNode.addChild(child1, "test");
		assertEquals("Wrong level for first child", 1, cnode.getLevel());

		TableIdentifier child2 = new TableIdentifier("child2");
		DependencyNode cnode2 = rootNode.addChild(child2, "test_2");
		assertEquals("Wrong level for second child", 1, cnode2.getLevel());

		TableIdentifier child3 = new TableIdentifier("child3");
		DependencyNode cnode3 = cnode2.addChild(child3, "test_3");
		assertEquals("Wrong level for third child", 2, cnode3.getLevel());

		DependencyNode f = rootNode.findNode(cnode3);
		assertNotNull(f);
		assertEquals(f, cnode3);
	}

}
