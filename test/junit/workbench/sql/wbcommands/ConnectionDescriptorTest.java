/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.ssh.SshConfig;

import workbench.db.ConnectionProfile;

import workbench.util.WbFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ConnectionDescriptorTest
  extends WbTestCase
{

  public ConnectionDescriptorTest()
  {
    super("ConnectionDescriptorTest");
  }

  @Before
  public void setUp()
  {
  }

  @After
  public void tearDown()
  {
  }

  @Test
  public void testParseDefinition()
    throws Exception
  {
    ConnectionDescriptor def = new ConnectionDescriptor(null);
    ConnectionProfile profile = def.parseDefinition("username=\"thomas\", url=jdbc:postgresql://localhost/thomas, password='secret'", null);
    assertNotNull(profile);
    assertEquals("thomas", profile.getUsername());
    assertEquals("secret", profile.getLoginPassword());
    assertEquals("jdbc:postgresql://localhost/thomas", profile.getUrl());
    assertEquals("org.postgresql.Driver", profile.getDriverclass());

    profile = def.parseDefinition("username=Arthur, url=jdbc:somedb:someparameter, password=MyPassword, driverjar=xyz.jar, driver=com.foobar.Driver", null);
    assertNotNull(profile);
    assertEquals("Arthur", profile.getUsername());
    assertEquals("MyPassword", profile.getLoginPassword());
    assertEquals("jdbc:somedb:someparameter", profile.getUrl());
    assertEquals("com.foobar.Driver", profile.getDriverclass());
    String jarPath = def.getJarPath();
    assertNotNull(jarPath);
    WbFile f = new WbFile(jarPath);
    assertEquals("xyz.jar", f.getName());

    profile = def.parseDefinition("username=Arthur, url=jdbc:somedb:someparameter, password=MyPassword, driverjar=xyz.jar, driver=com.foobar.Driver, " +
      "sshHost=somehost, sshUser=ford, sshLocalPort=56789, sshPort=44, sshPassword=supersecret", null);
    assertNotNull(profile);
    SshConfig config = profile.getSshConfig();
    assertNotNull(config);
    assertEquals("ford", config.getUsername());
    assertEquals("somehost", config.getHostname());
    assertEquals(56789, config.getLocalPort());
    assertEquals(44, config.getSshPort());
    assertEquals("supersecret", config.getPassword());
    assertNull(config.getPrivateKeyFile());
    assertFalse(config.getRewriteURL());

    String baseDir = getTestUtil().getBaseDir();
    WbFile pk = new WbFile(baseDir, "private.ppk");
    TestUtil.writeFile(pk, "dummy content");

    profile = def.parseDefinition("username=Arthur, url=jdbc:somedb:someparameter, password=MyPassword, driverjar=xyz.jar, driver=com.foobar.Driver, " +
      "sshHost=somehost, sshUser=ford, sshLocalPort=56789, sshPort=44, sshPrivateKey='" + pk.getFullPath() + "', " +
      "sshPassword=supersecret, sshRewriteURL=true", null);
    assertNotNull(profile);
    config = profile.getSshConfig();
    assertNotNull(config);
    assertEquals(pk.getFullPath(), config.getPrivateKeyFile());
    assertTrue(config.getRewriteURL());
  }

}
