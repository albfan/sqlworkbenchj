package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.WbManager;
import workbench.db.ColumnIdentifier;
import workbench.db.DataCopier;
import workbench.db.TableIdentifier;

import workbench.db.WbConnection;
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
 * A command to copy data from one DBMS to another. This is the commandline
 * version of the DataPumper.
 * @author  workbench@kellerer.org
 */
public class WbCopy 
	extends SqlCommand
{
	public static final String VERB = "COPY";

	private ArgumentParser cmdLine;
	private DataCopier copier;

	public WbCopy()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("sourcetable");
		cmdLine.addArgument("sourcequery");
		cmdLine.addArgument("targettable");
		cmdLine.addArgument("sourceprofile");
		cmdLine.addArgument("targetprofile");
		cmdLine.addArgument("columnmapping");
		cmdLine.addArgument("columns");
		cmdLine.addArgument("sourcewhere");
		cmdLine.addArgument("columns");
		cmdLine.addArgument("commitevery");
		cmdLine.addArgument("continue");
		cmdLine.addArgument("deletetarget");
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException, WbException
	{
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
			result.addMessage(ResourceMgr.getString("ErrorCopyWrongParameters"));
			result.setFailure();
			return result;
		}
		
		
		String sourceProfile = cmdLine.getValue("sourceprofile");
		String targetProfile = cmdLine.getValue("targetprofile");
		
		if (targetProfile == null && targetProfile == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorCopyWrongParameters"));
			result.setFailure();
			return result;
		}

		int commit = StringUtil.getIntValue(cmdLine.getValue("commitevery"),-1);
		
		String sourcetable = cmdLine.getValue("sourcetable");
		String sourcequery = cmdLine.getValue("sourcequery");
		if (sourcetable == null && sourcequery == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorCopyNoSourceSpecified"));
			result.addMessage("");
			result.addMessage(ResourceMgr.getString("ErrorCopyWrongParameters"));
			result.setFailure();
			return result;
		}
		
		String targettable = cmdLine.getValue("targettable");
		if (targettable == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorCopyWrongParameters"));
			result.setFailure();
			return result;
		}

		WbConnection targetCon = null;
		WbConnection sourceCon = null;
		if (targetProfile == null)
		{
			targetCon = aConnection;
		}	
		else
		{
			try
			{
				targetCon = WbManager.getInstance().getConnectionMgr().getConnection(targetProfile, "Wb-Copy-Target");
			}
			catch (Exception e)
			{
				result.addMessage(ResourceMgr.getString("ErrorCopyCouldNotConnectTarget"));
				result.setFailure();
				return result;
			}
		}

		if (sourceProfile == null)
		{
			sourceCon = aConnection;
		}
		else
		{
			try
			{
				sourceCon = WbManager.getInstance().getConnectionMgr().getConnection(sourceProfile, "Wb-Copy-Source");
			}
			catch (Exception e)
			{
				result.addMessage(ResourceMgr.getString("ErrorCopyCouldNotConnectSource"));
				result.setFailure();
				return result;
			}
		}
		boolean delete = "true".equalsIgnoreCase(cmdLine.getValue("deletetarget"));
		boolean cont = "true".equalsIgnoreCase(cmdLine.getValue("continue"));

		this.copier = new DataCopier();
		copier.setRowActionMonitor(this.rowMonitor);
		copier.setDeleteTarget(delete);
		copier.setContinueOnError(cont);
		copier.setCommitEvery(commit);

		TableIdentifier targetId = new TableIdentifier(targettable);
		
		if (sourcetable != null)
		{
			TableIdentifier srcTable = new TableIdentifier(sourcetable);
			String where = cmdLine.getValue("sourcewhere");
			boolean createTable = "true".equals(cmdLine.getValue("createtable"));
			if (createTable)
			{
				ColumnIdentifier[] cols = this.parseColumns();
				copier.setDefinitionForNewTable(sourceCon, targetCon, srcTable, cols, where);
			}
			else
			{
				Map mapping = this.parseMapping();
				copier.setDefinition(sourceCon, targetCon, srcTable, targetId, mapping, where);
			}
		}
		else 
		{
			ColumnIdentifier[] cols = this.parseColumns();
			copier.setDefinition(sourceCon, targetCon, sourcequery, targetId, cols);
		}
		
		try
		{
			copier.start();
			result.setSuccess();
		}
		catch (Exception e)
		{
			LogMgr.logError("WbCopy.execute()", "Error when copying data", e);
			result.setFailure();
			result.addMessage(ExceptionUtil.getDisplay(e));
		}
		
		try
		{
			if (sourceCon.getId().startsWith("Wb-Copy"))
			{
				sourceCon.disconnect();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbCopy.execute()", "Errro when disconnecting source connection",e);
			result.addMessage(ExceptionUtil.getDisplay(e));
		}

		try
		{
			if (targetCon.getId().startsWith("Wb-Copy"))
			{
				targetCon.disconnect();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbCopy.execute()", "Errro when disconnecting target connection",e);
			result.addMessage(ExceptionUtil.getDisplay(e));
		}
		this.addWarnings(result);
		this.addErrors(result);
		long rows = copier.getAffectedRow();
		String msg = rows + " " + ResourceMgr.getString("MsgCopyNumRows");
		result.addMessage(msg);
		
		return result;
	}

	private ColumnIdentifier[] parseColumns()
	{
		String cols = cmdLine.getValue("columns");
		List l = StringUtil.stringToList(cols, ",");
		int count = l.size();
		ColumnIdentifier[] result = new ColumnIdentifier[count];
		for (int i=0; i < count; i++)
		{
			String c = (String)l.get(i);
			result[i] = new ColumnIdentifier(c);
		}
		return result;
	}
	
	private Map parseMapping()
	{
		String cols = cmdLine.getValue("columnmapping");
		List l = StringUtil.stringToList(cols, ",");
		int count = l.size();
		HashMap mapping = new HashMap(count);
		for (int i=0; i < count; i++)
		{
			String s = (String)l.get(i);
			int pos = s.indexOf("/");
			String scol = s.substring(0, pos).trim();
			String tcol = s.substring(pos + 1).trim();
			//ColumnIdentifier sid = new ColumnIdentifier(scol);
			//ColumnIdentifier tid = new ColumnIdentifier(tcol);
			mapping.put(scol, tcol);
		}
		return mapping;
	}
	
	
	private void addWarnings(StatementRunnerResult result)
	{
		String[] err = copier.getWarnings();
		for (int i=0; i < err.length; i++)
		{
			result.addMessage(err[i]);
		}
	}
	
	private void addErrors(StatementRunnerResult result)
	{
		String[] warn = copier.getErrors();
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
		this.copier = null;;
	}

	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (this.copier != null)
		{
			this.copier.cancel();
		}
	}

}
