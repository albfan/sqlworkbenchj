/*
 * ProfileKeyTest.java
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
package workbench.gui.profiles;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class ProfileKeyTest
{

	@Test
	public void testCreate()
	{
		ProfileKey key = new ProfileKey(" { Group } / ProfileName ");
		assertEquals("Wrong group detected", "Group", key.getGroup());
		assertEquals("Wrong name detected", "ProfileName", key.getName());

		key = new ProfileKey("{Group}/ProfileName ");
		assertEquals("Wrong group detected", "Group", key.getGroup());
		assertEquals("Wrong name detected", "ProfileName", key.getName());

		// Allow group definition without a slash
		key = new ProfileKey("{Group}ProfileName ");
		assertEquals("Wrong group detected", "Group", key.getGroup());
		assertEquals("Wrong name detected", "ProfileName", key.getName());
	}

	@Test
	public void testCompare()
	{
		ProfileKey key1 = new ProfileKey("Profile1");
		ProfileKey key2 = new ProfileKey("Profile1", "Default Group");
		assertEquals(key1, key2);

		key1 = new ProfileKey("Profile1");
		key2 = new ProfileKey("Profile1");
		assertEquals(key1, key2);

		key1 = new ProfileKey("{DefaultGroup}/Profile1");
		key2 = new ProfileKey("Profile1", "Other Group");
		assertNotSame(key1, key2);

		key1 = new ProfileKey("Profile1", "Default Group");
		key2 = new ProfileKey("Profile2", "Default Group");
		assertNotSame(key1, key2);

		key1 = new ProfileKey("Profile1", "Default Group");
		key2 = new ProfileKey("{ Default Group} / Profile1");
		assertEquals(key1, key2);


		key1 = new ProfileKey("TAD310DAT@c63d64b", null);
		key2 = new ProfileKey("TAD310DAT@c63d64b", "BG");
		assertEquals(key1, key2);
	}

}
