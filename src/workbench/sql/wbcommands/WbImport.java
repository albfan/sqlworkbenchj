package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.List;

import workbench.db.WbConnection;
import workbench.db.importer.DataImporter;
import workbench.exception.ExceptionUtil;
import workbench.exception.WbException;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbImport extends SqlCommand
{
	public static final String VERB = "IMPORT";
	private DataImporter imp;

	private ArgumentParser cmdLine;

	public WbImport()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("type");
		cmdLine.addArgument("file");
		cmdLine.addArgument("table");
		cmdLine.addArgument("delimiter");
		cmdLine.addArgument("quotechar");
		cmdLine.addArgument("dateformat");
		cmdLine.addArgument("timestampformat");
		cmdLine.addArgument("decimal");
		cmdLine.addArgument("commitevery");
		cmdLine.addArgument("header");
		cmdLine.addArgument("encoding");
		cmdLine.addArgument("columns");
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException, WbException
	{
		imp = new DataImporter();

		StatementRunnerResult result = new StatementRunnerResult(aSql);
		aSql = SqlUtil.makeCleanSql(aSql, false, '"');
		int pos = aSql.indexOf(' ');
		if (pos > -1) 
			aSql = aSql.substring(pos);
		else 
			aSql = "";
		
		try
		{
			cmdLine.parse(aSql);
		}
		catch (Exception e)
		{
			result.addMessage(ResourceMgr.getString("ErrorImportWrongParameters"));
			result.setFailure();
			return result;
		}

		if (cmdLine.hasUnknownArguments())
		{
			List params = cmdLine.getUnknownArguments();
			StringBuffer msg = new StringBuffer(ResourceMgr.getString("ErrorUnknownParameter"));
			for (int i=0; i < params.size(); i++)
			{
				msg.append((String)params.get(i));
				if (i > 0) msg.append(',');
			}
			result.addMessage(msg.toString());
			result.addMessage(ResourceMgr.getString("ErrorImportWrongParameters"));
			result.setFailure();
			return result;
		}
		
		String type = null;
		String file = null;
		String cleancr = null;
		
		type = cmdLine.getValue("type");
		file = cmdLine.getValue("file");
		
		if (type == null || file == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorImportWrongParameters"));
			result.setFailure();
			return result;
		}

		String table = cmdLine.getValue("table");
		
		if ("text".equalsIgnoreCase(type) || "txt".equalsIgnoreCase(type))
		{
			if (table == null)
			{
				result.addMessage(ResourceMgr.getString("ErrorTextImportRequiresTableName"));
				result.setFailure();
				return result;
			}
			
			imp.setImportTypeText();
			imp.setTableName(table);
			
			String delimiter = cmdLine.getValue("delimiter");
			if (delimiter != null) imp.setTextDelimiter(delimiter);
			
			String quote = cmdLine.getValue("quotechar");
			if (quote != null) imp.setTextQuoteChar(quote);

			String format = cmdLine.getValue("dateformat");
			if (format != null) imp.setTextDateFormat(format);

			format = cmdLine.getValue("timestampformat");
			if (format != null) imp.setTextTimestampFormat(format);

			format = cmdLine.getValue("decimal");
			if (format != null) imp.setDecimalSymbol(format);

			String header = cmdLine.getValue("header");
			imp.setTextContainsHeaders(StringUtil.stringToBool(header));

			String encoding = cmdLine.getValue("encoding");
			if (encoding != null) imp.setEncoding(encoding);
			
			String columns = cmdLine.getValue("columns");
			if (columns != null)
			{
				List cols = StringUtil.stringToList(columns, ",");
				imp.setTextFileColumns(cols);
			}
			if (!"true".equals(header) && columns == null)
			{
				result.addMessage(ResourceMgr.getString("ErrorHeaderOrColumnDefRequired"));
				result.setFailure();
				return result;
			}
		}
		else if ("xml".equalsIgnoreCase(type))
		{
			imp.setImportTypeXml();

			if (table != null) imp.setTableName(table);
			
			String encoding = cmdLine.getValue("encoding");
			if (encoding != null) imp.setEncoding(encoding);
			
			int commit = StringUtil.getIntValue(cmdLine.getValue("commitevery"),-1);
			imp.setCommitEvery(commit);
		}
		else
		{
			result.addMessage(ResourceMgr.getString("ErrorImportWrongParameters"));
			result.setFailure();
			return result;
		}
		file = StringUtil.trimQuotes(file);
		this.imp.setInputFilename(file);
		this.imp.setConnection(aConnection);
		this.imp.setRowActionMonitor(this.rowMonitor);
		String msg = ResourceMgr.getString("MsgImportingFile");
		msg += " " + file;
		if (table != null)
		{
			msg += " " + ResourceMgr.getString("MsgImportTable");
			msg += ": " + table.toUpperCase();
		}
		result.addMessage(msg);
		
		try
		{
			imp.startImport();
			result.setSuccess();
		}
		catch (Exception e)
		{
			LogMgr.logError("WbImport.execute()", "Error when importing file (" + file +")", e);
			result.setFailure();
			result.addMessage(ExceptionUtil.getDisplay(e));
		}
		this.addWarnings(result);
		this.addErrors(result);
		long rows = imp.getAffectedRow();
		msg = rows + " " + ResourceMgr.getString("MsgImportNumRows");
		result.addMessage(msg);
		
		return result;
	}

	private void addWarnings(StatementRunnerResult result)
	{
		String[] err = imp.getWarnings();
		for (int i=0; i < err.length; i++)
		{
			result.addMessage(err[i]);
		}
	}
	
	private void addErrors(StatementRunnerResult result)
	{
		String[] warn = imp.getErrors();
		for (int i=0; i < warn.length; i++)
		{
			result.addMessage(warn[i]);
		}
		if (warn.length > 0)
		{
			// force an empty line if we had warnings
			result.addMessage("");
		}
	}
	
	public void done()
	{
		super.done();
		this.imp = null;
	}

	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (this.imp != null)
		{
			this.imp.cancelExecution();
		}
	}
}
