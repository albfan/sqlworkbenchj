package workbench.sql.wbcommands;

import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.db.DataSpooler;
import workbench.db.WbConnection;
import workbench.db.importer.DataImporter;
import workbench.exception.ExceptionUtil;
import workbench.exception.WbException;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.commands.SelectCommand;
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
		cmdLine.addArgument("createtable");
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
			result.addMessage(ResourceMgr.getString("ErrorImportTextNotImplemented"));
			result.setFailure();
			return result;
			
//			imp.setImportTypeText();
//			String delimiter = cmdLine.getValue("delimiter");
//			if (delimiter != null) imp.setTextDelimiter(delimiter);
//			
//			String quote = cmdLine.getValue("quotechar");
//			if (quote != null) imp.setTextQuoteChar(quote);
//
//			String format = cmdLine.getValue("dateformat");
//			if (format != null) imp.setTextDateFormat(format);
//
//			format = cmdLine.getValue("timestampformat");
//			if (format != null) imp.setTextTimestampFormat(format);
//
//			format = cmdLine.getValue("decimal");
//			if (format != null) imp.setDecimalSymbol(format);
//
//			String header = cmdLine.getValue("header");
//			imp.setTextContainsHeaders(StringUtil.stringToBool(header));
		}
		else if ("xml".equalsIgnoreCase(type))
		{
//			String format = cmdLine.getValue("dateformat");
//			if (format != null) imp.setTextDateFormat(format);

//			format = cmdLine.getValue("timestampformat");
//			if (format != null) imp.setTextTimestampFormat(format);
//
//			format = cmdLine.getValue("decimal");
//			if (format != null) imp.setDecimalSymbol(format);

			imp.setImportTypeXml();
			if (table != null) imp.setTableName(table);
			
			String commit = cmdLine.getValue("commitevery");
			if (commit != null)
			{
			}
		}
		else
		{
			result.addMessage(ResourceMgr.getString("ErrorImportWrongParameters"));
			result.setFailure();
			return result;
		}
		file = StringUtil.trimQuotes(file);
		this.imp.setInputFilename(file);
		this.imp.setConnection(aConnection.getSqlConnection());
		String msg = ResourceMgr.getString("MsgImportInit");
		msg = StringUtil.replace(msg, "%type%", type.toUpperCase());
		msg = StringUtil.replace(msg, "%file%", file);
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
		
		this.addErrors(result);
		long rows = imp.getAffectedRow();
		msg = rows + " " + ResourceMgr.getString("MsgImportNumRows");
		result.addMessage(msg);
		
		return result;
	}

	private void addErrors(StatementRunnerResult result)
	{
		String[] err = imp.getErrors();
		for (int i=0; i < err.length; i++)
		{
			result.addMessage(err[i]);
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
