package workbench.sql.wbcommands;

import java.sql.SQLException;

import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.XsltTransformer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbXslt
	extends SqlCommand
{
	private ArgumentParser cmdLine;
	public static final String ARG_STYLESHEET = "stylesheet";
	public static final String ARG_OUTPUT = "xsltoutput";
	public static final String ARG_INPUT = "inputfile";

	public WbXslt()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_STYLESHEET);
		cmdLine.addArgument(ARG_OUTPUT);
		cmdLine.addArgument(ARG_INPUT);
	}

	public String getVerb()
	{
		return "XSLT";
	}

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult("XSLT");
		if (!aSql.trim().toUpperCase().startsWith(this.getVerb()))
		{
			result.addMessage("Wrong command! " + this.getVerb() + " expected.");
			result.setFailure();
			return result;
		}

		String parm = aSql.trim().substring(this.getVerb().length());

		cmdLine.parse(parm);

		String inputFile = cmdLine.getValue(ARG_INPUT);
		String outputFile = cmdLine.getValue(ARG_OUTPUT);
		String xsltFile = cmdLine.getValue(ARG_STYLESHEET);

		if (!cmdLine.hasArguments())
		{
			result.addMessage(ResourceMgr.getString("ErrorXsltWrongParameter"));
			result.setFailure();
			return result;
		}

		if (inputFile == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorXsltMissingInputFile"));
			result.setFailure();
			return result;
		}
		if (outputFile == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorXsltMissingOutputFile"));
			result.setFailure();
			return result;
		}
		if (xsltFile == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorXsltMissingStylesheet"));
			result.setFailure();
			return result;
		}

		try
		{
			XsltTransformer.transformFile(inputFile, outputFile, xsltFile);
			result.addMessage(ResourceMgr.getString("MsgXsltSuccessful"));
			result.setSuccess();
		}
		catch (Exception e)
		{
			LogMgr.logError("WbXslt.execute()", "Error when transforming '" + inputFile + "' to '" + outputFile + "' using " + xsltFile, e);
			result.addMessage(e.getMessage());
		}
		return result;
	}

}