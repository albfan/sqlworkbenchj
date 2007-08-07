/*
 * OracleControlFileWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.ResultInfo;
import workbench.util.CharacterRange;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * @author support@sql-workbench.net
 */
public class OracleControlFileWriter
  implements FormatFileWriter
{

  public OracleControlFileWriter()
  {
  }

  public void writeFormatFile(DataExporter exporter, RowDataConverter converter)
  {
    if (!exporter.getWriteOracleControlFile())
    {
      return;
    }
    ResultInfo resultInfo = converter.getResultInfo();
    WbFile baseFile = new WbFile(exporter.getFullOutputFilename());
    String dir = baseFile.getParent();
    String baseName = baseFile.getFileName();
    File ctl = new File(dir, baseName + ".ctl");
    PrintWriter out = null;
    try
    {
      out = new PrintWriter(new BufferedWriter(new FileWriter(ctl)));
      if (exporter.getExportHeaders())
      {
        out.println("-- Skip the header row of the input file");
        out.println("OPTIONS (skip=1)");
      }

      if (!exporter.getEncoding().startsWith("UTF"))
      {
        out.println("-- The specified character set is an ISO name and will most probably not work");
        out.println("-- as Oracle uses its own names for character sets (e.g. WE8ISO8859P1 for ISO-8859-1)");
      }
      out.print("LOAD DATA CHARACTERSET '");
      if (exporter.getEncoding().equalsIgnoreCase("UTF-8"))
      {
        // Oracle only understand UTF8 not UTF-8
        out.println("UTF8'");
      }
      else
      {
        out.println(exporter.getEncoding().toUpperCase() + "'");
      }
      File f = new File(exporter.getFullOutputFilename());
      out.println("INFILE '" + f.getName() + "'");
      out.println("-- to replace the data in the table use TRUNCATE instead of APPEND");
      out.println("APPEND");
      out.print("INTO TABLE ");
      out.println(resultInfo.getUpdateTable().getTableName());
      out.print("FIELDS TERMINATED BY '");
      out.print(StringUtil.escapeUnicode(exporter.getTextDelimiter(), CharacterRange.RANGE_CONTROL));
      out.println("' TRAILING NULLCOLS");
      out.println("(");
      int count = resultInfo.getColumnCount();
      int max = 0;
      // calculate max. column name length for proper formatting
      for (int i = 0; i < count; i++)
      {
        int l = resultInfo.getColumnName(i).length();
        if (l > max)
        {
          max = l;
        }
      }
      max++;

      String format = exporter.getTimestampFormat();
      if (format == null)
      {
        format = Settings.getInstance().getDefaultTimestampFormat();
        //if (format == null) format = exporter.getDateFormat();
      }
      String oraFormat = convertJavaDateFormatToOracle(format);
      List<String> blobColumns = new LinkedList<String>();
      boolean clobAsFile = exporter.getWriteClobAsFile();
      for (int i = 0; i < count; i++)
      {
        String col = resultInfo.getColumnName(i);
        int type = resultInfo.getColumnType(i);
        out.print("  ");
        if (SqlUtil.isBlobType(type) || (clobAsFile && SqlUtil.isClobType(type)))
        {
          blobColumns.add(col);
          out.print("lob_file_" + blobColumns.size() + " FILLER");
        }
        else
        {
          out.print(col);
        }

        if (SqlUtil.isDateType(type))
        {
          for (int k = col.length(); k < max; k++)
          {
            out.print(" ");
          }
          out.print("DATE");
          if (format != null)
          {
            out.print(" \"");
            out.print(oraFormat);
            out.print("\"");
          }
        }
//				else if (SqlUtil.isCharacterType(type))
//				{
//					for (int k=col.length(); k < max; k++) out.print(" ");
//					out.print("\"trim(:" + col + ")\"");
//				}
        if (i < count - 1 || blobColumns.size() > 0)
        {
          out.print(",");
        }
        out.println();
      }
      if (blobColumns.size() > 0)
      {
        Iterator itr = blobColumns.iterator();
        int i = 1;
        while (itr.hasNext())
        {
          String col = (String) itr.next();
          out.print("  ");
          out.print(col);
          out.print(" LOBFILE(lob_file_" + i + ") TERMINATED BY EOF");
          if (itr.hasNext())
          {
            out.print(",");
          }
          out.println();
        }
      }
      out.print(")");
    }
    catch (IOException io)
    {
      LogMgr.logError("OracleControlFileWriter.writeFormatFile()", "Error opening outputfile", io);
    }
    finally
    {
      try
      {
        out.close();
      }
      catch (Throwable th)
      {
      }
    }
  }

  private String convertJavaDateFormatToOracle(String format)
  {
    String result = format.replaceAll("HH", "HH24");
    result = result.replaceAll("hh", "HH12");
    result = result.replaceAll("mm", "MI");
    result = result.replaceAll("yy", "YY");
    result = result.replaceAll("dd", "DD");
    result = result.replaceAll("ss", "SS");
    return result;
  }
}
