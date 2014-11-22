/*
 * WindowTitleBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

	public String getWindowTitle(ConnectionProfile profile, String workspaceFile, String editorFile)
	{
		final StringBuilder title = new StringBuilder(50);

		boolean showProductNameAtEnd = GuiSettings.getShowProductNameAtEnd();
		boolean showWorkspace = GuiSettings.getShowWorkspaceInWindowTitle();

		String enclose = GuiSettings.getTitleGroupBracket();
		String sep = GuiSettings.getTitleGroupSeparator();

		if (!showProductNameAtEnd)
		{
			title.append(ResourceMgr.TXT_PRODUCT_NAME);
			title.append(" - ");
		}

		if (profile == null)
		{
			title.append(ResourceMgr.getString("TxtNotConnected"));
		}
		else
		{
			boolean showProfileGroup = GuiSettings.getShowProfileGroupInWindowTitle();
			boolean showURL = GuiSettings.getShowURLinWindowTitle();
			boolean includeUser = GuiSettings.getIncludeUserInTitleURL() || profile.getPromptForUsername();

			if (showURL)
			{
				String url = makeCleanUrl(profile.getUrl());
				if (includeUser)
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
					title.append(" - ");
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

		if (workspaceFile != null && showWorkspace)
		{
			File f = new File(workspaceFile);
			String baseName = f.getName();
			title.append(" - ");
			title.append(baseName);
			title.append(" ");
		}

		int showTitle = GuiSettings.getShowFilenameInWindowTitle();
		if (editorFile != null && showTitle != GuiSettings.SHOW_NO_FILENAME)
		{
			title.append(" - ");
			if (showTitle == GuiSettings.SHOW_FULL_PATH)
			{
				title.append(editorFile);
			}
			else
			{
				File f = new File(editorFile);
				title.append(f.getName());
			}
		}

		if (showProductNameAtEnd)
		{
			title.append(" - ");
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
