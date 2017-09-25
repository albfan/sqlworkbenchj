/*
 * ModifierArgumentsTest.java
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

import workbench.db.ColumnIdentifier;
import workbench.db.importer.modifier.ColumnValueSubstring;
import workbench.db.importer.modifier.ImportValueModifier;
import workbench.db.importer.modifier.RegexModifier;
import workbench.db.importer.modifier.SubstringModifier;
import workbench.util.ArgumentParser;
import static org.junit.Assert.*;
import org.junit.Test;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class ModifierArgumentsTest
  extends WbTestCase
{
  public ModifierArgumentsTest()
  {
    super("ModifierArgumentsTest");
  }

  @Test
  public void testRegex()
  {
    try
    {
      ArgumentParser cmdLine = new ArgumentParser();
      ModifierArguments.addArguments(cmdLine);

      // -colSubstring should overwrite whatever was specified with -maxLength
      cmdLine.parse("-colReplacement=firstname=bla:blub");
      ModifierArguments args = new ModifierArguments(cmdLine);

      ImportValueModifier mod = args.getModifier();
      assertNotNull(mod);
      assertEquals(1, mod.getSize());

      RegexModifier regex = args.getRegexModifier();
      assertNotNull(regex);

      String modified = regex.modifyValue(new ColumnIdentifier("firstname"), "blast");
      assertEquals("blubst", modified);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  public void testSubstring()
  {
    ArgumentParser cmdLine = new ArgumentParser();
    ModifierArguments.addArguments(cmdLine);

    // -colSubstring should overwrite whatever was specified with -maxLength
    cmdLine.parse("-maxLength=firstname=2 -colSubstring=firstname=5:10,lastname = 1:3");
    ModifierArguments args = new ModifierArguments(cmdLine);

    ImportValueModifier mod = args.getModifier();
    assertNotNull(mod);
    assertEquals(2, mod.getSize());

    SubstringModifier substring = args.getSubstringModifier();
    assertNotNull(substring);

    ColumnIdentifier fname = new ColumnIdentifier("firstname");
    ColumnValueSubstring s1 = substring.getSubstring(fname);
    assertNotNull(s1);
    assertEquals(5, s1.getStart());
    assertEquals(10, s1.getEnd());

    ColumnIdentifier lname = new ColumnIdentifier("lastname");
    ColumnValueSubstring s2 = substring.getSubstring(lname);
    assertNotNull(s2);
    assertEquals(1, s2.getStart());
    assertEquals(3, s2.getEnd());
  }

  @Test
  public void testMaxLength()
  {
    ArgumentParser cmdLine = new ArgumentParser();
    ModifierArguments.addArguments(cmdLine);

    cmdLine.parse("-gag=test -maxlength=firstname=100, lastname = 10");
    ModifierArguments args = new ModifierArguments(cmdLine);

    ImportValueModifier mod = args.getModifier();
    assertNotNull(mod);
    assertEquals(2, mod.getSize());

    SubstringModifier substring = args.getSubstringModifier();
    assertNotNull(substring);

    ColumnIdentifier fname = new ColumnIdentifier("firstname");
    ColumnValueSubstring s1 = substring.getSubstring(fname);
    assertNotNull(s1);
    assertEquals(0, s1.getStart());
    assertEquals(100, s1.getEnd());

    ColumnIdentifier lname = new ColumnIdentifier("lastname");
    ColumnValueSubstring s2 = substring.getSubstring(lname);
    assertNotNull(s2);
    assertEquals(0, s2.getStart());
    assertEquals(10, s2.getEnd());

    String modified = s2.getSubstring("12345678901234567890");
    assertEquals("1234567890", modified);
  }
}
