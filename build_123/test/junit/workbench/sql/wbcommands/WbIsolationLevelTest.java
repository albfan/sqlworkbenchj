/*
 * WbIsolationLevelTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.sql.wbcommands;

import java.sql.Connection;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbIsolationLevelTest
{

  public WbIsolationLevelTest()
  {
  }

  @Test
  public void testLevelMap()
    throws Exception
  {
    WbIsolationLevel cmd = new WbIsolationLevel();

    int level = cmd.stringToLevel(" read  committed ");
    assertEquals(Connection.TRANSACTION_READ_COMMITTED, level);

    level = cmd.stringToLevel(" Serializable");
    assertEquals(Connection.TRANSACTION_SERIALIZABLE, level);

    level = cmd.stringToLevel(" repeatable READ");
    assertEquals(Connection.TRANSACTION_REPEATABLE_READ, level);

    level = cmd.stringToLevel(" repeatable_READ");
    assertEquals(Connection.TRANSACTION_REPEATABLE_READ, level);

    level = cmd.stringToLevel(" read \nUNcommitted ");
    assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, level);

    level = cmd.stringToLevel(" not known");
    assertEquals(-1, level);

  }


}
