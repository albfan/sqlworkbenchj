/*
 * CharacterRange.java
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
package workbench.util;

import workbench.resource.ResourceMgr;

/**
 * @author  Thomas Kellerer
 */
public class CharacterRange
{
  /**
   * Includes all characters
   */
  public static final CharacterRange RANGE_NONE = new CharacterRange(0);

  /**
   * Excludes all control characters (&lt; 32)
   */
  public static final CharacterRange RANGE_CONTROL = new CharacterRange(1);

  /**
   * Excludes everything &lt; 32 and &gt; 126
   */
  public static final CharacterRange RANGE_7BIT = new CharacterRange(2);

  /**
   * Excludes everything &lt; 32 and &gt; 255
   */
  public static final CharacterRange RANGE_8BIT = new CharacterRange(3);

  /**
   * Excludes everything &lt; 32 and &gt; 255
   * and everything &gt; 126 and &lt; 161
   */
  public static final CharacterRange RANGE_8BIT_EXTENDED = new CharacterRange(4);

  private int typeIndex = 0;

  private CharacterRange(int index)
  {
    typeIndex = index;
  }

  public static CharacterRange[] getRanges()
  {
    return new CharacterRange[] { RANGE_NONE, RANGE_CONTROL, RANGE_7BIT, RANGE_8BIT, RANGE_8BIT_EXTENDED };
  }

  public String getName()
  {
    switch (typeIndex)
    {
      case 1:
        return "control";
      case 2:
        return "7bit";
      case 3:
        return "8bit";
      case 4:
        return "extended";
      default:
        return "none";
    }
  }

  public static CharacterRange getRangeByName(String name)
  {
    for (CharacterRange range : getRanges())
    {
      if (range.getName().equalsIgnoreCase(name)) return range;
    }
    return RANGE_NONE;
  }

  public static CharacterRange getRangeById(int index)
  {
    switch (index)
    {
      case 0:
        return RANGE_NONE;
      case 1:
        return RANGE_CONTROL;
      case 2:
        return RANGE_7BIT;
      case 3:
        return RANGE_8BIT;
      case 4:
        return RANGE_8BIT_EXTENDED;
      default:
        return null;
    }
  }

  public int getId()
  {
    return typeIndex;
  }

  public boolean isOutsideRange(char c)
  {
    switch (typeIndex)
    {
      case 0:
        return false;
      case 1:
        return (c < 32);
      case 2:
        return (c < 32 || c > 126);
      case 3:
        return (c < 32 || c > 255);
      case 4:
        return (c < 32 || (c > 126 && c < 161) || c > 255);
      default:
        return false;
    }
  }

  public String getFilterDescription()
  {
    switch (typeIndex)
    {
      case 0:
        return ResourceMgr.getString("LblNone");
      case 1:
        return ResourceMgr.getString("LblExportRangeCtrl");
      case 2:
        return ResourceMgr.getString("LblExportRange7Bit");
      case 3:
        return ResourceMgr.getString("LblExportRange8Bit");
      case 4:
        return ResourceMgr.getString("LblExportRange8BitExtended");
      default:
        return "";
    }
  }

  @Override
  public String toString()
  {
    return getFilterDescription();
  }
}
