/*
 * WindowTitleBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.gui;

import java.io.File;

import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;

import workbench.db.ConnectionProfile;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class WindowTitleBuilder
{
	private static final int NAME_AT_END = 1;
	private static final int NAME_AT_START = 2;

	private boolean showProfileGroup = GuiSettings.getShowProfileGroupInWindowTitle();
	private boolean showURL = GuiSettings.getShowURLinWindowTitle();
	private boolean includeUser = GuiSettings.getIncludeUserInTitleURL();
	private int productNamePosition = NAME_AT_START;
	private boolean showWorkspace = GuiSettings.getShowWorkspaceInWindowTitle();
	private boolean showNotConnected = true;

	public WindowTitleBuilder()
	{
		setShowProductNameAtEnd(GuiSettings.getShowProductNameAtEnd());
	}

	public void setShowProfileGroup(boolean flag)
	{
		this.showProfileGroup = flag;
	}

	public void setShowURL(boolean flag)
	{
		this.showURL = flag;
	}

	public void setIncludeUser(boolean flag)
	{
		this.includeUser = flag;
	}

	public void setShowProductNameAtEnd(boolean flag)
	{
		if (flag)
		{
			productNamePosition = NAME_AT_END;
		}
		else
		{
			productNamePosition = NAME_AT_START;
		}
	}

	public void setShowWorkspace(boolean flag)
	{
		this.showWorkspace = flag;
	}

	public void setShowNotConnected(boolean flag)
	{
		this.showNotConnected = flag;
	}

	public String getWindowTitle(ConnectionProfile profile)
	{
		return getWindowTitle(profile, null, null);
	}

	public String getWindowTitle(ConnectionProfile profile, String workspaceFile, String editorFile)
	{
		final StringBuilder title = new StringBuilder(50);

		String enclose = GuiSettings.getTitleGroupBracket();
		String sep = GuiSettings.getTitleGroupSeparator();

		if (productNamePosition == NAME_AT_START)
		{
			title.append(ResourceMgr.TXT_PRODUCT_NAME);
			title.append(' ');
		}

		if (profile != null)
		{
			boolean showUser = includeUser || profile.getPromptForUsername();

			if (showURL)
			{
				String url = makeCleanUrl(profile.getUrl());
				if (showUser)
				{
					title.append(profile.getLoginUser());
					if (url.charAt(0) != '@')
					{
						title.append('@');
					}
				}
				title.append(url);
			}
			else
			{
				if (profile.getPromptForUsername())
				{
					// always display the username if prompted
					title.append(profile.getLoginUser());
					title.append("- ");
				}
				if (showProfileGroup)
				{
					char open = getOpeningBracket(enclose);
					char close = getClosingBracket(enclose);

					if (open != 0 && close != 0)
					{
						title.append(open);
					}
					title.append(profile.getGroup());
					if (open != 0 && close != 0)
					{
						title.append(close);
					}
					if (sep != null) title.append(sep);
				}
				title.append(profile.getName());
			}
		}
		else if (showNotConnected)
		{
			if (title.length() > 0) title.append("- ");
			title.append(ResourceMgr.getString("TxtNotConnected"));
		}

		if (workspaceFile != null && showWorkspace)
		{
			File f = new File(workspaceFile);
			String baseName = f.getName();
			title.append(" - ");
			title.append(baseName);
			title.append(" ");
		}

		int showFilename = GuiSettings.getShowFilenameInWindowTitle();
		if (editorFile != null && showFilename != GuiSettings.SHOW_NO_FILENAME)
		{
			title.append(" - ");
			if (showFilename == GuiSettings.SHOW_FULL_PATH)
			{
				title.append(editorFile);
			}
			else
			{
				File f = new File(editorFile);
				title.append(f.getName());
			}
		}

		if (productNamePosition == NAME_AT_END)
		{
			if (title.length() > 0) title.append(" - ");
			title.append(ResourceMgr.TXT_PRODUCT_NAME);
		}

		return title.toString();
	}

	private char getOpeningBracket(String settingsValue)
	{
		if (StringUtil.isEmptyString(settingsValue)) return 0;
		return settingsValue.charAt(0);
	}

	private char getClosingBracket(String settingsValue)
	{
		if (StringUtil.isEmptyString(settingsValue)) return 0;
		char open = getOpeningBracket(settingsValue);
		if (open == '{') return '}';
		if (open == '[') return ']';
		if (open == '(') return ')';
		if (open == '<') return '>';
		return 0;
	}

	public String makeCleanUrl(String url)
	{
		if (StringUtil.isEmptyString(url)) return url;

		int numColon = 2;
		if (url.startsWith("jdbc:oracle:") || url.startsWith("jdbc:jtds:"))
		{
			numColon = 3;
		}
		int pos = StringUtil.findOccurance(url, ':', numColon);
		if (pos > 0)
		{
			return url.substring(pos + 1);
		}
		return url;
	}

}
