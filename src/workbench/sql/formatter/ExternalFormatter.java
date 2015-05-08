/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.formatter;

import java.io.File;
import java.io.IOException;

import workbench.log.LogMgr;

import workbench.util.FileUtil;
import workbench.util.StringUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class ExternalFormatter
  implements SqlFormatter
{
  public static final String INPUT_FILE = "${wb_input}";
  public static final String OUTPUT_FILE = "${wb_output}";

  private String program;
  private String cmdLine;
  private String inputEncoding;
  private String outputEncoding;
  private boolean supportsMultipleStatements;

  public ExternalFormatter()
  {
  }

  public void setProgram(String executable)
  {
    program = executable;
  }

  public void setCommandLine(String command)
  {
    cmdLine = command;
  }

  public void setInputEncoding(String encoding)
  {
    inputEncoding = encoding;
  }

  public void setOutputEncoding(String encoding)
  {
    outputEncoding = encoding;
  }

  @Override
  public boolean supportsMultipleStatements()
  {
    return supportsMultipleStatements;
  }

  public void setSupportsMultipleStatements(boolean supportsMultipleStatements)
  {
    this.supportsMultipleStatements = supportsMultipleStatements;
  }

  @Override
  public String getFormattedSql(String sql)
  {
    try
    {
      return runFormatter(sql);
    }
    catch (IOException ex)
    {
      LogMgr.logError("ExternalFormatter.format()", "Could not format SQL statement", ex);
      return sql;
    }
  }

  private String runFormatter(String sql)
    throws IOException
  {
    File infile = File.createTempFile("wbf$_in", ".sql");
    File outfile = File.createTempFile("wbf$_out", ".sql");

    boolean useSystemOut = true;
    boolean useSystemIn = true;
    String args = "";

    if (cmdLine.contains(INPUT_FILE))
    {
      args = cmdLine.replace(INPUT_FILE, "\"" + infile.getAbsolutePath() + "\"");
      useSystemIn = false;
    }

    if (args.contains(OUTPUT_FILE))
    {
      // just to be sure that the tool doesn't fail because the file is already there
      outfile.delete();

      args = args.replace(OUTPUT_FILE, "\"" + outfile.getAbsolutePath() + "\"");
      useSystemOut = false;
    }

    if (inputEncoding == null)
    {
      inputEncoding =  System.getProperty("file.encoding");
      LogMgr.logInfo("ExternalFormatter.runFormatter()", "Using encoding for SQL formatter input file: " + inputEncoding);
    }

    if (outputEncoding == null)
    {
      outputEncoding = inputEncoding;
      LogMgr.logInfo("ExternalFormatter.runFormatter()", "Using encoding for SQL formatter output file: " + outputEncoding);
    }

    try
    {
      FileUtil.writeString(infile, sql, inputEncoding, false);

      ProcessBuilder pb = new ProcessBuilder(program, args);

      if (useSystemIn)
      {
        pb.redirectInput(infile);
      }

      if (useSystemOut)
      {
        pb.redirectOutput(outfile);
      }

			LogMgr.logInfo("ExternalFormatter.runFormatter()", "Running external formatter: " + pb.command());

			Process task = pb.start();

			task.waitFor();

			int exitValue = task.exitValue();
      LogMgr.logDebug("ExternalFormatter.runFormatter()", "Return value was: " + exitValue);

      String formatted = FileUtil.readFile(outfile, outputEncoding);
      return StringUtil.trim(formatted);
    }
    catch (Exception ex)
    {
      LogMgr.logError("ExternalFormatter.runFormatter()", "Error running formatter", ex);
    }
    finally
    {
      infile.delete();
      outfile.delete();
    }
    return sql;
  }

}
