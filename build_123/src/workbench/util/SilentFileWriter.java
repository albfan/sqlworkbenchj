/*
 * SilentFileWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import java.io.FileWriter;

/**
 *
 * @author Thomas Kellerer
 */
public class SilentFileWriter
{
  private FileWriter writer;

  public SilentFileWriter(String filename)
  {
    try
    {
      writer = new FileWriter(filename);
    }
    catch (Throwable th)
    {
      th.printStackTrace();
    }
  }

  public void append(String text)
  {
    try
    {
      writer.append(text);
    }
    catch (Throwable th)
    {
      th.printStackTrace();
    }
  }

  public void close()
  {
    try
    {
      writer.close();
    }
    catch (Throwable th)
    {
      th.printStackTrace();
    }
  }

}
