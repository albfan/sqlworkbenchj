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
package workbench.sql.formatter;

import java.io.File;
import java.io.IOException;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;


/**
 *
 * @author Thomas Kellerer
 */
public class ExternalFormatter
  implements SqlFormatter
{
  public static final String DEFAULT_DBID = "default";
  public static final String INPUT_FILE = "${wbin}";
  public static final String OUTPUT_FILE = "${wbout}";

  private String program;
  private String cmdLine;
  private String inputEncoding;
  private String outputEncoding;
  private boolean supportsMultipleStatements;
  private boolean enabled = true;

  public ExternalFormatter()
  {
  }

  public void setEnabled(boolean flag)
  {
    enabled = flag;
  }

  public boolean isUsable()
  {
    return (enabled && programExists());
  }

  public boolean programExists()
  {
    if (StringUtil.isBlank(program)) return false;
    WbFile f = new WbFile(program);
    return f.exists();
  }

  public boolean isEnabled()
  {
    return enabled;
  }

  public String getProgram()
  {
    return program;
  }

  public void setProgram(String executable)
  {
    program = executable;
  }

  public String getCommandLine()
  {
    return cmdLine;
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
    File errFile = File.createTempFile("wbf$_err", ".txt");

    boolean useSystemOut = true;
    boolean useSystemIn = true;
    String args = "";

    if (cmdLine != null && cmdLine.contains(INPUT_FILE))
    {
      args = cmdLine.replace(INPUT_FILE, "\"" + infile.getAbsolutePath() + "\"");
      useSystemIn = false;
    }

    if (args.contains(OUTPUT_FILE))
    {
      // just to be sure that the tool doesn't fail because the file is already there
      FileUtil.deleteSilently(outfile);
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

      WbFile exe = new WbFile(program);

      ProcessBuilder pb = new ProcessBuilder("\"" + exe.getAbsolutePath() + "\"", args);
      pb.directory(exe.getAbsoluteFile().getParentFile());

      if (useSystemIn)
      {
        pb.redirectInput(infile);
      }

      if (useSystemOut)
      {
        pb.redirectOutput(outfile);
      }

      pb.redirectError(errFile);

			LogMgr.logInfo("ExternalFormatter.runFormatter()", "Running external formatter: " + pb.command());

			Process task = pb.start();
			task.waitFor();

			int exitValue = task.exitValue();
      LogMgr.logDebug("ExternalFormatter.runFormatter()", "Return value was: " + exitValue);

      String formatted = FileUtil.readFile(outfile, outputEncoding);

      String error = getErrorOutput(errFile);
      if (StringUtil.isNonEmpty(error))
      {
        LogMgr.logWarning("ExternalFormatter.runFormatter()", "Error message from formatter: " + error);
      }

      return StringUtil.trim(formatted);
    }
    catch (Exception ex)
    {
      String error = getErrorOutput(errFile);
      if (StringUtil.isNonEmpty(error))
      {
        LogMgr.logError("ExternalFormatter.runFormatter()", "Error message from formatter: " + error, null);
        LogMgr.logDebug("ExternalFormatter.runFormatter()", "Error cause", ex);
      }
      else
      {
        LogMgr.logError("ExternalFormatter.runFormatter()", "Error running formatter", ex);
      }
    }
    finally
    {
      FileUtil.deleteSilently(infile);
      FileUtil.deleteSilently(outfile);
      FileUtil.deleteSilently(errFile);
    }

    // something went wrong, return the original SQL statement
    return sql;
  }

  private String getErrorOutput(File errFile)
  {
    String error = null;
    try
    {
      if (errFile.exists())
      {
        error = FileUtil.readFile(errFile, System.getProperty("file.encoding"));
      }
    }
    catch (Throwable th)
    {
      // ignore
    }
    return error;
  }

  @Override
  public String toString()
  {
    if (StringUtil.isEmptyString(program)) return "<empty>";
    WbFile f = new WbFile(program);
    return f.getAbsolutePath();
  }

  public static void saveDefinition(ExternalFormatter formatter, String dbId)
  {
    if (formatter == null) return;

    Settings.getInstance().setProperty("workbench.formatter." + dbId + ".program", formatter.program == null ? "" : formatter.program);
    Settings.getInstance().setProperty("workbench.formatter." + dbId + ".enabled", formatter.isEnabled());
    Settings.getInstance().setProperty("workbench.formatter." + dbId + ".cmdline", formatter.cmdLine);
    Settings.getInstance().setProperty("workbench.formatter." + dbId + ".inputencoding", formatter.inputEncoding);
    Settings.getInstance().setProperty("workbench.formatter." + dbId + ".outputencoding", formatter.outputEncoding);
    Settings.getInstance().setProperty("workbench.formatter." + dbId + ".supports.scripts", formatter.supportsMultipleStatements);
  }

  public static ExternalFormatter getDefinition(String dbId)
  {
    if (StringUtil.isEmptyString(dbId))
    {
      dbId = DEFAULT_DBID;
    }

    String prg = Settings.getInstance().getProperty("workbench.formatter." + dbId + ".program", null);
    boolean enabled = Settings.getInstance().getBoolProperty("workbench.formatter." + dbId + ".enabled", false);

    String cmdLine = Settings.getInstance().getProperty("workbench.formatter." + dbId + ".cmdline", null);
    String inputEncoding = Settings.getInstance().getProperty("workbench.formatter." + dbId + ".inputencoding", null);
    String outputEncoding = Settings.getInstance().getProperty("workbench.formatter." + dbId + ".outputencoding", null);
    boolean supportsScripts = Settings.getInstance().getBoolProperty("workbench.formatter." + dbId + ".supports.scripts", false);

    ExternalFormatter f = new ExternalFormatter();
    f.setCommandLine(cmdLine);
    f.setProgram(prg);
    f.setInputEncoding(inputEncoding);
    f.setOutputEncoding(outputEncoding);
    f.setSupportsMultipleStatements(supportsScripts);
    f.setEnabled(enabled);
    return f;
  }
}
