/*
 * HtmlRowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import workbench.storage.RowData;

import workbench.util.HtmlUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Convert RowData into HTML.
 *
 * @author  Thomas Kellerer
 * @see HtmlExportWriter
 */
public class HtmlRowDataConverter
	extends RowDataConverter
{
	private String pageTitle;
	private boolean createFullPage = true;
	private boolean escapeHtml = false;
	private String heading;
	private String trailer;

	@Override
	public StringBuilder getEnd(long totalRows)
	{
		StringBuilder html = new StringBuilder("</table>\n");
		if (StringUtil.isNonBlank(trailer))
		{
			html.append(trailer);
			html.append('\n');
		}

		if (createFullPage)
		{
			html.append("</body>\n</html>\n");
		}
		return html;
	}

	public void setHeading(String head)
	{
		heading = head;
	}

	public void setTrailer(String html)
	{
		trailer = html;
	}

	public void setCreateFullPage(boolean flag)
	{
		this.createFullPage = flag;
	}

	@Override
	public void setPageTitle(String title)
	{
		this.pageTitle = title;
	}

	public void setEscapeHtml(boolean flag)
	{
		this.escapeHtml = flag;
	}

	@Override
	public StringBuilder convertRowData(RowData row, long rowIndex)
	{
		int count = this.metaData.getColumnCount();
		StringBuilder result = new StringBuilder(count * 30);
		result.append("  <tr>\n      ");
		for (int c=0; c < count; c ++)
		{
			if (!includeColumnInExport(c)) continue;

			String value = this.getValueAsFormattedString(row, c);
			if (createFullPage)
			{
				int type = this.metaData.getColumnType(c);
				if (SqlUtil.isDateType(type))
				{
					result.append("<td class=\"date-cell\">");
				}
				else if (SqlUtil.isNumberType(type) || SqlUtil.isDateType(type))
				{
					result.append("<td class=\"number-cell\">");
				}
				else
				{
					result.append("<td class=\"text-cell\">");
				}
			}
			else
			{
				result.append("<td>");
			}

			if (value == null)
			{
				String nullDisplay = getNullDisplay();
				if (nullDisplay != null)
				{
					result.append(nullDisplay);
				}
				else
				{
					result.append("&nbsp;");
				}
			}
			else
			{
				if (this.escapeHtml)
				{
					value = HtmlUtil.escapeHTML(value);
				}
				result.append(value);
			}
			result.append("</td>");
		}
		result.append("\n  </tr>\n");
		return result;
	}

	@Override
	public StringBuilder getStart()
	{
		StringBuilder result = new StringBuilder(250);

		if (createFullPage)
		{
			result.append("<html>\n");

			if (pageTitle != null && pageTitle.length() > 0)
			{
				result.append("<head>\n<title>");
				result.append(pageTitle);
				result.append("</title>\n");
			}
			result.append("<style type=\"text/css\">\n");
			result.append("<!--\n");
			result.append("  table { border-spacing:0; border-collapse:collapse}\n");
			result.append("  td, th { font-weight:normal;padding:2; border-style:solid;border-width:1px; vertical-align:top;}\n");
			result.append("  .number-cell { text-align:right; white-space:nowrap; } \n");
			result.append("  .text-cell { text-align:left; } \n");
			result.append("  .date-cell { text-align:left; white-space:nowrap;} \n");
			result.append("-->\n</style>\n");

			result.append("</head>\n<body>\n");
		}

		if (StringUtil.isNonBlank(heading))
		{
			result.append(heading);
			result.append('\n');
		}

		result.append("<table>\n");

		// table header with column names
		result.append("  <tr>\n");
		for (int c=0; c < this.metaData.getColumnCount(); c ++)
		{
			if (!includeColumnInExport(c)) continue;
			result.append("      <th>");
			result.append("<b>");
			result.append(this.metaData.getColumnDisplayName(c));
			result.append("</b>");
			String comment = metaData.getColumn(c).getComment();
			if (includeColumnComments && StringUtil.isNonBlank(comment))
			{
				result.append("<br/>(");
				result.append(comment);
				result.append(")");
			}
			result.append("      </th>\n");
		}
		result.append("\n  </tr>\n");

		return result;
	}

}
