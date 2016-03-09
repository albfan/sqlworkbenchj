/*
 * ClipboardFile.java
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
package workbench.util;

import java.io.File;
import java.io.IOException;

/**
 * @author Thomas Kellerer
 */
public class ClipboardFile
  extends File
{
  private String buffer;

  public ClipboardFile(String contents)
  {
    super("Clipboard");
    buffer = contents;
  }

  public String getContents()
  {
    return this.buffer;
  }

  @Override
  public boolean canRead()
  {
    return true;
  }

  @Override
  public boolean canWrite()
  {
    return false;
  }

  @Override
  public boolean delete()
  {
    return false;
  }

  @Override
  public String getAbsolutePath()
  {
    return "Clipboard";
  }

  @Override
  public File getAbsoluteFile()
  {
    return this;
  }

  @Override
  public String getName()
  {
    return "Clipboard";
  }

  @Override
  public boolean isDirectory()
  {
    return false;
  }

  @Override
  public boolean isHidden()
  {
    return false;
  }

  @Override
  public boolean exists()
  {
    return true;
  }

  @Override
  public String getParent()
  {
    return null;
  }

  @Override
  public File getParentFile()
  {
    return null;
  }

  @Override
  public String getPath()
  {
    return getAbsolutePath();
  }

  @Override
  public String getCanonicalPath()
    throws IOException
  {
    return getAbsolutePath();
  }

  @Override
  public File getCanonicalFile()
    throws IOException
  {
    return this;
  }

  @Override
  public boolean createNewFile()
    throws IOException
  {
    return false;
  }

  @Override
  public int compareTo(File pathname)
  {
    return -1;
  }

  @Override
  public boolean equals(Object obj)
  {
    return false;
  }

  @Override
  public long length()
  {
    if (this.buffer == null)
    {
      return 0;
    }
    return this.buffer.length();
  }

  @Override
  public int hashCode()
  {
    if (buffer == null)
    {
      return 0;
    }
    return buffer.hashCode();
  }
}
