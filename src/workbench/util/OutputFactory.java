/*
 * OutputFactory.java
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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 *
 * @author Thomas Kellerer
 */
public interface OutputFactory
{
  OutputStream createOutputStream(File output)
    throws IOException;

  Writer createWriter(File output, String encoding)
    throws IOException;

  Writer createWriter(String filename, String encoding)
    throws IOException;

  void done() throws IOException;
  boolean isArchive();
}
