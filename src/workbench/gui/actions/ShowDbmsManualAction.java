/*
 * ShowDbmsManualAction.java
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
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;

import workbench.util.BrowserLauncher;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;
import workbench.util.VersionNumber;

/**
 * @author Thomas Kellerer
 */
public class ShowDbmsManualAction
	extends WbAction
{
	private String onlineManualUrl;

	public ShowDbmsManualAction()
	{
		super();
		initMenuDefinition("MnuTxtDbmsHelp");
		removeIcon();
		setEnabled(false);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		if (StringUtil.isNonBlank(onlineManualUrl))
		{
			try
			{
				BrowserLauncher.openURL(onlineManualUrl);
			}
			catch (Exception ex)
			{
				WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(ex));
			}
		}
	}

	public void setDbms(String dbid, VersionNumber version)
	{
		setDbms(dbid, version.getMajorVersion(), version.getMinorVersion());
	}
	
	public void setDbms(String dbid, int majorVersion, int minorVersion)
	{
		if (StringUtil.isNonBlank(dbid))
		{
			String url = null;
			if (majorVersion > 0 && minorVersion > 0)
			{
				url = Settings.getInstance().getProperty("workbench.db." + dbid + "." + Integer.toString(majorVersion) + "." + Integer.toString(minorVersion) + ".manual", null);
			}

			if (url == null && majorVersion > 0)
			{
				url = Settings.getInstance().getProperty("workbench.db." + dbid + "." + Integer.toString(majorVersion) + ".manual", null);
			}
			if (url == null)
			{
				url = Settings.getInstance().getProperty("workbench.db." + dbid + ".manual", null);
			}
			if (url != null)
			{
				onlineManualUrl = MessageFormat.format(url, majorVersion, minorVersion);
			}
			else
			{
				onlineManualUrl = null;
			}
		}
		else
		{
			onlineManualUrl = null;
		}
		setEnabled(StringUtil.isNonBlank(onlineManualUrl));
		if (onlineManualUrl != null)
		{
			setTooltip("<html>" + ResourceMgr.getDescription("MnuTxtDbmsHelp") + "<br>(" + onlineManualUrl + ")</html>");
		}
		else
		{
			setTooltip(null);
		}
	}

}
