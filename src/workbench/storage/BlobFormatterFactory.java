/*
 * BlobFormatterFactory.java
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
package workbench.storage;

import workbench.db.DbMetadata;
import workbench.db.DbSettings;

import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class BlobFormatterFactory
{

  public static BlobLiteralFormatter createAnsiFormatter()
  {
    // ANSI Syntax is X'FFF0'...
    DefaultBlobFormatter f = new DefaultBlobFormatter();
    f.setPrefix("X'");
    f.setSuffix("'");
    return f;
  }

  public static BlobLiteralFormatter createInstance(BlobLiteralType type)
  {
    if (type == BlobLiteralType.pgDecode || type == BlobLiteralType.pgEscape || type == BlobLiteralType.pgHex)
    {
      return new PostgresBlobFormatter(type);
    }
    DefaultBlobFormatter f = new DefaultBlobFormatter();
    f.setLiteralType(type);
    return f;
  }

  public static BlobLiteralFormatter createInstance(DbMetadata meta)
  {
		// Check for a user-defined formatter definition
    // for the current DBMS
    DbSettings s = meta.getDbSettings();
    String prefix = s.getBlobLiteralPrefix();
    String suffix = s.getBlobLiteralSuffix();

    if (StringUtil.isNonBlank(prefix) && StringUtil.isNonBlank(suffix))
    {
      DefaultBlobFormatter f = new DefaultBlobFormatter();
      String type = s.getBlobLiteralType();

      BlobLiteralType literalType = null;
      try
      {
        literalType = BlobLiteralType.valueOf(type);
      }
      catch (Throwable e)
      {
        literalType = BlobLiteralType.hex;
      }

      BlobLiteralType.valueOf(type);
      f.setUseUpperCase(s.getBlobLiteralUpperCase());
      f.setLiteralType(literalType);
      f.setPrefix(prefix);
      f.setSuffix(suffix);
      return f;
    }

    // No user-defined formatter definition found, use the built-in settings
    if (meta.isPostgres())
    {
      return new PostgresBlobFormatter();
    }
    else if (meta.isOracle())
    {
			// this might only work with Oracle 10g...
      // and will probably fail on BLOBs > 4KB
      DefaultBlobFormatter f = new DefaultBlobFormatter();
      f.setUseUpperCase(true);
      f.setPrefix("to_blob(utl_raw.cast_to_raw('");
      f.setSuffix("'))");
      return f;
    }
    else if (meta.isSqlServer())
    {
      DefaultBlobFormatter f = new DefaultBlobFormatter();
      f.setPrefix("0x");
      return f;
    }

    // Use the ANSI format for all others (e.g.: DB2, H2, HSQLDB
    return createAnsiFormatter();
  }

}
