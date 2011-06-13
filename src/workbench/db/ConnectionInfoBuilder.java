/*
 * ConnectionInfo
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 * A class to generate a summary display of a WbConnection.
 *
 * The information can be presented as HTML or plain text.
 * @author Thomas Kellerer
 */
public class ConnectionInfoBuilder
{

	public ConnectionInfoBuilder()
	{
	}

	public String getHtmlDisplay(WbConnection conn)
	{
		if (conn == null) return "";
		if (conn.isClosed()) return "";
		return getDisplay(conn, true, 0);
	}

	public String getPlainTextDisplay(WbConnection conn, int indent)
	{
		if (conn == null) return "";
		if (conn.isClosed()) return "";
		return getDisplay(conn, false, indent);
	}

	private String getDisplay(WbConnection conn, boolean useHtml, int indent)
	{
		try
		{
			StringBuilder content = new StringBuilder(500);
			if (useHtml) content.append("<html>");

			String space = StringUtil.padRight("", indent);

			String lineStart = useHtml ? "<div style=\"white-space:nowrap;\">" : space;
			String lineEnd = useHtml ? "</div>\n" : "\n";
			String boldStart = useHtml ? "<b>" : "";
			String boldEnd = useHtml ? "</b> " : " ";
			String newLine = useHtml ? "<br>\n" : "\n";

			DatabaseMetaData meta = conn.getSqlConnection().getMetaData();
			DbMetadata wbmeta = conn.getMetadata();

			content.append(lineStart + boldStart + ResourceMgr.getString("LblDbProductName") + ":" + boldEnd + wbmeta.getProductName() + lineEnd);
			content.append(lineStart + boldStart + ResourceMgr.getString("LblDbProductVersion") + ":" + boldEnd + conn.getDatabaseVersion() + lineEnd);
			content.append(lineStart + boldStart + ResourceMgr.getString("LblDbProductInfo") + ":" + boldEnd + conn.getDatabaseProductVersion() + lineEnd);
			content.append(lineStart + boldStart + ResourceMgr.getString("LblDriverInfoName") + ":" + boldEnd + meta.getDriverName() + lineEnd);
			content.append(lineStart + boldStart + ResourceMgr.getString("LblDriverInfoClass") + ":" + boldEnd + conn.getProfile().getDriverclass() + lineEnd);
			content.append(lineStart + boldStart + ResourceMgr.getString("LblDriverInfoVersion") + ":" + boldEnd + conn.getDriverVersion() + lineEnd);
			content.append(lineStart + boldStart + ResourceMgr.getString("LblDbURL") + ":" + boldEnd + conn.getUrl() + lineEnd);
			content.append(space + boldStart + "Isolation Level:" + boldEnd + " " + conn.getIsolationLevel() + newLine);
			content.append(space + boldStart + ResourceMgr.getString("LblUsername") + ":" + boldEnd + conn.getCurrentUser() + newLine);
			String term = wbmeta.getSchemaTerm();
			String s = StringUtil.capitalize(term);
			if (!"schema".equalsIgnoreCase(term))
			{
				s += " (" + ResourceMgr.getString("LblSchema") + ")";
			}
			content.append(space + boldStart + s + ":" + boldEnd + nvl(conn.getCurrentSchema()) + newLine);

			term = wbmeta.getCatalogTerm();
			s = StringUtil.capitalize(term);
			if (!"catalog".equalsIgnoreCase(term))
			{
				s += " (" +  ResourceMgr.getString("LblCatalog") + ")";
			}
			content.append(space + boldStart + s + ":" + boldEnd + nvl(conn.getCurrentCatalog()) + newLine);
			content.append(space + boldStart + "Workbench DBID:" + boldEnd + wbmeta.getDbId());
			if (useHtml) content.append("</html>");
			return content.toString();
		}
		catch (SQLException e)
		{
			return e.getMessage();
		}
	}

	private String nvl(String value)
	{
		if (value == null) return "";
		return value;
	}
}
