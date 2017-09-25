/*
 * BlobMode.java
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
package workbench.db.exporter;

import java.util.List;

import workbench.util.CollectionUtil;

/**
 * Define codes for the different ways how BLOBs can be handled by the export classes.
 *
 * @author Thomas Kellerer
 */
public enum BlobMode
{
  /**
   * Use a DBMS specific literals for BLOBs in SQL statements.
   * @see workbench.storage.BlobFormatterFactory#createInstance(workbench.db.DbMetadata meta)
   * @see workbench.db.exporter.DataExporter#setBlobMode(BlobMode)
   */
  DbmsLiteral,

  /**
   * Use ANSI literals for BLOBs in SQL statements.
   * @see workbench.storage.BlobFormatterFactory#createAnsiFormatter()
   * @see workbench.db.exporter.DataExporter#setBlobMode(BlobMode)
   */
  AnsiLiteral,

  /**
   * Generate WB Specific {$blobfile=...} statements
   * @see workbench.db.exporter.DataExporter#setBlobMode(BlobMode)
   */
  SaveToFile,

  /**
   * Encode the blob using a Base64 encoding (e.g. for Postgres COPY format)
   */
  Base64,

  /**
   * Encode the blob using Postgres' decode() function
   * @see workbench.storage.PostgresBlobFormatter
   */
  pgDecode,

  /**
   * Encode the blob using Postgres' Octal escaping
   * @see workbench.storage.PostgresBlobFormatter
   */
  pgEscape,

  pgHex,

  None;

  /**
   * Convert a user-supplied mode keyword to the matching BlobMode
   * Valid input strings are:
   * <ul>
   * <li><tt>none</tt> - maps to {@link #None}</li>
   * <li><tt>ansi</tt> - maps to {@link #AnsiLiteral}</li>
   * <li><tt>dbms</tt> - maps to {@link #DbmsLiteral}</li>
   * <li><tt>file</tt> - maps to {@link #SaveToFile}</li>
   * <li><tt>base64</tt> - maps  to {@link #Base64}</li>
   * </ul>
   * @param type the type as entered by the user
   * @return null if the type was invalid, the corresponding BlobMode otherwise
   */
  public static BlobMode getMode(String type)
  {
    if (type == null) return BlobMode.None;
    if ("none".equalsIgnoreCase(type.trim())) return BlobMode.None;
    if ("ansi".equalsIgnoreCase(type.trim())) return BlobMode.AnsiLiteral;
    if ("dbms".equalsIgnoreCase(type.trim())) return BlobMode.DbmsLiteral;
    if ("file".equalsIgnoreCase(type.trim())) return BlobMode.SaveToFile;
    if ("base64".equalsIgnoreCase(type.trim())) return BlobMode.Base64;
    if ("pgescape".equalsIgnoreCase(type.trim())) return BlobMode.pgEscape;
    if ("pghex".equalsIgnoreCase(type.trim())) return BlobMode.pgHex;
    if ("pgdecode".equalsIgnoreCase(type.trim())) return BlobMode.pgDecode;
    try
    {
      return BlobMode.valueOf(type);
    }
    catch (Throwable e)
    {
      return null;
    }
  }

  public String getTypeString()
  {
    switch (this)
    {
      case None:
        return "";
      case AnsiLiteral:
        return "ansi";
      case DbmsLiteral:
        return "dbms";
      case SaveToFile:
        return "file";
      case Base64:
        return "base64";
      case pgDecode:
        return "pgdecode";
      case pgEscape:
        return "pgescape";
      case pgHex:
        return "pghex";
      default:
        return "";
    }

  }
  public static List<String> getTypes()
  {
    return CollectionUtil.arrayList("file", "ansi", "dbms", "base64", "pgescape", "pgdecode", "pghex");
  }

}
