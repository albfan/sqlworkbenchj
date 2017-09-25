/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands.console;

import java.util.List;
import java.util.Properties;

import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;

import workbench.sql.StatementRunnerResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbCreateProfileTest
  extends WbTestCase
{

  public WbCreateProfileTest()
  {
    super("WbCreateProfileTest");
  }

  @Before
  public void setUp()
  {
    ConnectionMgr.getInstance().clearProfiles();
  }

  @After
  public void tearDown()
  {
    ConnectionMgr.getInstance().clearProfiles();
  }

  @Test
  public void testExecute()
    throws Exception
  {
    WbCreateProfile create = new WbCreateProfile();

    String cmd =
      "wbcreateprofile " +
      "   -name=newprofile \n" +
      "   -profilegroup=postgres \n" +
      "   -url='jdbc:postgresql://localhost/wbtest'\n" +
      "   -username=thomas\n" +
      "   -password=welcome\n" +
      "   -connectionProperties='foo=bar' \n" +
      "   -driverClass=org.postgresql.Driver\n";

    StatementRunnerResult result = create.execute(cmd);
    assertTrue(result.isSuccess());

    List<ConnectionProfile> profiles = ConnectionMgr.getInstance().getProfiles();
    assertEquals(1, profiles.size());
    ConnectionProfile p = profiles.get(0);
    assertEquals("newprofile", p.getName());
    assertEquals("postgres", p.getGroup());
    assertEquals("thomas", p.getUsername());
    Properties props = p.getConnectionProperties();
    assertNotNull(props);
    assertEquals("bar", props.getProperty("foo"));
  }

}
