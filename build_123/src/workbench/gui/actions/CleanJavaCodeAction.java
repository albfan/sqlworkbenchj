/*
 * CleanJavaCodeAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.KeyStroke;

import workbench.interfaces.TextContainer;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 * Action to convert Java code into a SQL statement
 *
 * @author Thomas Kellerer
 */
public class CleanJavaCodeAction
  extends WbAction
{
  protected TextContainer client;

  public CleanJavaCodeAction(TextContainer aClient)
  {
    super();
    this.client = aClient;
    this.initMenuDefinition("MnuTxtCleanJavaCode", KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_MASK));
    this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    boolean selected = true;
    String code = this.client.getSelectedText();
    if (code == null || code.length() == 0)
    {
      code = this.client.getText();
      selected = false;
    }
    final String sql = cleanJavaString(code);
    if (sql != null && sql.length() > 0)
    {
      final boolean sel = selected;
      EventQueue.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          if (sel)
            client.setSelectedText(sql);
          else
            client.setText(sql);
        }
      });
    }
  }

  public String cleanJavaString(String aString)
  {
    if (StringUtil.isEmptyString(aString)) return "";
    // a regex to find escaped newlines in the literal
    Pattern newline = Pattern.compile("\\\\n|\\\\r");
    String[] lines = StringUtil.PATTERN_CRLF.split(aString);
    StringBuilder result = new StringBuilder(aString.length());
    int count = lines.length;
    for (int i = 0; i < count; i++)
    {
      String l = lines[i];
      if (l == null) continue;
      if (l.trim().startsWith("//"))
      {
        l = l.replaceFirst("//", "--");
      }
      else
      {
        l = l.trim();
        //if (l.startsWith("\"")) start = 1;
        int start = l.indexOf("\"");
        int end = l.lastIndexOf("\"");
        if (end == start) start = 1;
        if (end == 0) end = l.length() - 1;
        if (start > -1) start++;
        if (start > -1 && end > -1)
        {
          l = l.substring(start, end);
        }
      }
      Matcher m = newline.matcher(l);
      l = m.replaceAll("");
      l = StringUtil.replace(l, "\\\"", "\"");
      result.append(l);
      if (i < count - 1) result.append('\n');
    }
    return result.toString();
  }

}
