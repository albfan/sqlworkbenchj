/*
 * RowDataReaderFactory.java
 *
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
package workbench.storage;

import workbench.log.LogMgr;

import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.db.oracle.OracleUtils;

/**
 *
 * @author Thomas Kellerer
 */
public class RowDataReaderFactory
{
  public static RowDataReader createReader(ResultInfo info, WbConnection conn)
  {
    DbMetadata meta = conn == null ? null : conn.getMetadata();
    if (conn != null && meta != null && meta.isOracle() && OracleUtils.fixTimestampTZ())
    {
      try
      {
        return new OracleRowDataReader(info, conn);
      }
      catch (Exception cnf)
      {
        if (LogMgr.isDebugEnabled())
        {
          LogMgr.logDebug("RowDataReaderFactory.createReader()", "Could not instantiate OracleRowDataReader", cnf);
        }
        else
        {
          LogMgr.logWarning("RowDataReaderFactory.createReader()", "Could not instantiate OracleRowDataReader. Probably the Oracle specific classes are not available");
        }
        // disable the usage of the OracleRowDataReader for now, to avoid unnecessary further attempts
        System.setProperty(OracleUtils.PROP_FIX_TIMESTAMPTZ, "false");
      }
    }
    
    return new RowDataReader(info, conn);
  }
}
