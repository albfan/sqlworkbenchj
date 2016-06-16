/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.profiles;

import java.awt.Window;
import java.util.Collections;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import workbench.resource.ResourceMgr;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.FeedbackWindow;
import workbench.gui.components.ValidatingDialog;

import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class ConnectionGuiHelper
{

  public static boolean doPrompt(Window parent, ConnectionProfile profile)
  {
    if (profile == null) return true;

    if (profile.getPromptForUsername())
    {
      return promptUsername(parent, profile);
    }

    if (profile.needsPasswordPrompt())
    {
      return promptPassword(parent, profile);
    }
    return true;
  }

  public static boolean promptUsername(Window parent, ConnectionProfile profile)
  {
    if (profile == null) return false;

    LoginPrompt prompt = new LoginPrompt(profile.getSettingsKey());
    boolean ok = ValidatingDialog.showConfirmDialog(parent, prompt, ResourceMgr.getString("TxtEnterLogin"));
    if (!ok) return false;
    profile.setPassword(prompt.getPassword());
    profile.setTemporaryUsername(prompt.getUserName());
    return true;
  }

  public static boolean promptPassword(Window parent, ConnectionProfile profile)
  {
    if (profile == null) return false;

    String pwd = WbSwingUtilities.getUserInputHidden(parent, ResourceMgr.getString("MsgInputPwdWindowTitle"), "");
    if (StringUtil.isEmptyString(pwd)) return false;
    profile.setPassword(pwd);
    return true;
  }

  public static void testConnection(final JComponent caller, final ConnectionProfile profile)
  {
    final Window window = SwingUtilities.getWindowAncestor(caller);

    if (!doPrompt(window, profile)) return;

    final FeedbackWindow connectingInfo = new FeedbackWindow((JDialog)window, ResourceMgr.getString("MsgConnecting"));

    WbThread testThread = new WbThread("ConnectionTest")
    {
      @Override
      public void run()
      {
        try
        {
          WbConnection conn = ConnectionMgr.getInstance().getConnection(profile, "$Connection-Test");

          connectingInfo.setVisible(false);
          ConnectionMgr.getInstance().abortAll(Collections.singletonList(conn));

          WbSwingUtilities.showMessage(window, ResourceMgr.getFormattedString("MsgBatchConnectOk", profile.getUrl()));
        }
        catch (Exception ex)
        {
          String error = ExceptionUtil.getDisplay(ex);
          connectingInfo.setVisible(false);
          WbSwingUtilities.showFriendlyErrorMessage(window, ResourceMgr.getString("ErrConnectFailed"), error);
        }
        finally
        {
          connectingInfo.dispose();
        }
      }
    };
    testThread.start();

    WbSwingUtilities.center(connectingInfo, window);
    connectingInfo.setVisible(true);
  }

}
