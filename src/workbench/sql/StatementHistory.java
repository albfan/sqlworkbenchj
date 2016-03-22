/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
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

package workbench.sql;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

import workbench.interfaces.SqlHistoryProvider;
import workbench.log.LogMgr;

import workbench.sql.wbcommands.WbHistory;

import workbench.util.CharacterRange;
import workbench.util.CollectionUtil;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.FixedSizeList;
import workbench.util.SqlParsingUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class StatementHistory
  extends FixedSizeList<String>
  implements SqlHistoryProvider
{

  public StatementHistory(int max)
  {
    super(max);
    setAllowDuplicates(true);
    doAppend(true);
  }

  @Override
  public synchronized boolean add(String statement)
  {
    if (StringUtil.isEmptyString(statement)) return false;

    String last = entries.size() > 0 ? entries.getLast() : "";
    if (last != null && last.equals(statement)) return false;

    String verb = SqlParsingUtil.getInstance(null).getSqlVerb(statement);
    if (verb.equalsIgnoreCase(WbHistory.VERB) || verb.equalsIgnoreCase(WbHistory.SHORT_VERB)) return false;

    return super.add(statement);
  }

  @Override
  public List<String> getHistoryEntries()
  {
    return Collections.unmodifiableList(this.getEntries());
  }

  @Override
  public String getHistoryEntry(int index)
  {
    return this.get(index);
  }

  public void readFrom(File f)
  {
    if (f == null || !f.exists()) return;

    entries.clear();
    BufferedReader reader = null;
    try
    {
      reader = EncodingUtil.createBufferedReader(f, "UTF-8");
      String line = reader.readLine();
      while (line != null)
      {
        line = StringUtil.decodeUnicode(line);
        this.append(line);
        line = reader.readLine();
      }
      LogMgr.logInfo("StatementHistory.readFrom()", "Loaded statement history from " + f.getAbsolutePath());
    }
    catch (IOException io)
    {
      LogMgr.logError("StatementHistory.readFrom()", "Could not save history", io);
    }
    finally
    {
      FileUtil.closeQuietely(reader);
    }
  }

  public void saveTo(File f)
  {
    if (CollectionUtil.isEmpty(entries)) return;

    Writer writer = null;
    try
    {
      writer = EncodingUtil.createWriter(f, "UTF-8", false);
      for (String sql : entries)
      {
        String line = StringUtil.escapeText(sql, CharacterRange.RANGE_CONTROL);
        writer.write(line);
        writer.write('\n');
      }
      LogMgr.logInfo("StatementHistory.saveTo()", "Saved statement history to " + f.getAbsolutePath());
    }
    catch (IOException io)
    {
      LogMgr.logError("StatementHistory.saveTo()", "Could not save history", io);
    }
    finally
    {
      FileUtil.closeQuietely(writer);
    }
  }
}
