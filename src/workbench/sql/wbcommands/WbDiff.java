/*
 * WbDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.List;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.db.diff.SchemaDiff;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
import workbench.util.StrWriter;
import workbench.util.StringUtil;

/**
 * @author  info@sql-workbench.net
 */
public class WbDiff
	extends SqlCommand
{
	public static final String VERB = "WBDIFF";
	public static final String PARAM_SOURCEPROFILE = "sourceprofile";
	public static final String PARAM_TARGETPROFILE = "targetprofile";
	public static final String PARAM_FILENAME = "file";
	public static final String PARAM_ENCODING = "encoding";
	
	public static final String PARAM_SOURCETABLES = "sourcetables";
	public static final String PARAM_TARGETTABLES = "targettables";
	
	private ArgumentParser cmdLine;
	private SchemaDiff diff;
	
	public WbDiff()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(PARAM_SOURCEPROFILE);
		cmdLine.addArgument(PARAM_TARGETPROFILE);
		cmdLine.addArgument(PARAM_FILENAME);
		cmdLine.addArgument(PARAM_ENCODING);
		cmdLine.addArgument(PARAM_SOURCETABLES);
		cmdLine.addArgument(PARAM_TARGETTABLES);
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(WbConnection aConnection, String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		if (sql.equalsIgnoreCase(VERB))
		{
			result.addMessage(ResourceMgr.getString("ErrorDiffWrongParameters"));
			result.setFailure();
			return result;
		}
		int pos = sql.toUpperCase().indexOf(VERB);
		if (pos > -1) sql = sql.substring(pos + VERB.length() + 1);
		
		try
		{
			cmdLine.parse(sql);
		}
		catch (Exception e)
		{
			result.addMessage(ResourceMgr.getString("ErrorDiffWrongParameters"));
			result.setFailure();
			return result;
		}

		if (cmdLine.hasUnknownArguments())
		{
			List params = cmdLine.getUnknownArguments();
			StringBuffer msg = new StringBuffer(ResourceMgr.getString("ErrorUnknownParameter") + " ");
			for (int i=0; i < params.size(); i++)
			{
				if (i > 0) msg.append(',');
				msg.append((String)params.get(i));
			}
			result.addMessage(msg.toString());
			result.addMessage("");
			result.addMessage(ResourceMgr.getString("ErrorDiffWrongParameters"));
			result.setFailure();
			return result;
		}
		
		String filename = cmdLine.getValue(PARAM_FILENAME);

		String sourceProfile = cmdLine.getValue(PARAM_SOURCEPROFILE);
		String targetProfile = cmdLine.getValue(PARAM_TARGETPROFILE);

		WbConnection targetCon = null;
		WbConnection sourceCon = null;
		
		this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
		
		if (targetProfile == null || aConnection.getProfile().getName().equals(targetProfile))
		{
			targetCon = aConnection;
		}
		else
		{
			try
			{
				this.rowMonitor.setCurrentObject(ResourceMgr.getString("MsgDiffConnectingTarget"),-1,-1);
				targetCon = ConnectionMgr.getInstance().getConnection(targetProfile, "Wb-Diff-Target");
			}
			catch (Exception e)
			{
				result.addMessage(ResourceMgr.getString("ErrorDiffCouldNotConnectTarget"));
				result.setFailure();
				return result;
			}
		}

		if (sourceProfile == null || aConnection.getProfile().getName().equals(sourceProfile))
		{
			sourceCon = aConnection;
		}
		else
		{
			try
			{
				this.rowMonitor.setCurrentObject(ResourceMgr.getString("MsgDiffConnectingSource"),-1,-1);
				sourceCon = ConnectionMgr.getInstance().getConnection(sourceProfile, "Wb-Diff-Source");
			}
			catch (Exception e)
			{
				result.addMessage(ResourceMgr.getString("ErrorDiffCouldNotConnectSource"));
				result.setFailure();
				// disconnect the target connection only if it was created by this command
				if (targetCon.getId().startsWith("Wb-Diff"))
				{
					try { targetCon.disconnect(); } catch (Throwable th) {}
				}
				return result;
			}
		}
		
		this.diff = new SchemaDiff(sourceCon, targetCon);
		diff.setMonitor(this.rowMonitor);
		
		String refTables = cmdLine.getValue(PARAM_SOURCETABLES);
		String tarTables = cmdLine.getValue(PARAM_TARGETTABLES);
		if (refTables == null)
		{
			diff.compareAll();
		}
		else if (tarTables == null)
		{
			List rl = StringUtil.stringToList(refTables, ",", true, true);
			diff.setTables(rl);
		}
		else
		{
			List rl = StringUtil.stringToList(refTables, ",", true, true);
			List tl = StringUtil.stringToList(tarTables, ",", true, true);
			if (rl.size() != tl.size())
			{
				result.addMessage(ResourceMgr.getString("ErrorDiffTableListNoMatch"));
				result.setFailure();
				return result;
			}
			diff.setTables(rl, tl);
		}
		
		Writer out = null;
		boolean outputToConsole = false;
		try
		{
			if (filename == null || filename.trim().length() == 0)
			{
				out = new StrWriter(5000);
				outputToConsole = true;
			}
			else
			{
				String encoding = cmdLine.getValue(PARAM_ENCODING);
				if (encoding == null) 
				{
					encoding =  diff.getEncoding();
				}
				else
				{
					diff.setEncoding(encoding);
				}
				out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), encoding));
			}
			if (!diff.isCancelled()) diff.writeXml(out);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { out.close(); } catch (Throwable th) {}
		}
		if (diff.isCancelled())
		{
			result.addMessage(ResourceMgr.getString("MsgDiffCancelled"));
		}
		else
		{
			if (outputToConsole)
			{
				result.addMessage(out.toString());
			}
		}
		return result;
	}
	
	public void cancel()
	{
		if (this.diff != null) this.diff.cancel();
	}
}
