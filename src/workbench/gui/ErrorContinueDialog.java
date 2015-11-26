/*
 * DialogInvoker.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import workbench.resource.ErrorPromptType;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;

import workbench.gui.components.WbOptionPane;

import workbench.sql.ErrorDescriptor;

import static workbench.gui.WbSwingUtilities.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ErrorContinueDialog
{
  private int dialogChoice;

  private ErrorDescriptor errorDetails;
  private String question;

  public ErrorContinueDialog(ErrorDescriptor error, String continueQuestion)
  {
    this.errorDetails = error;
    this.question = continueQuestion;
  }

  public int askContinue(final Component caller)
  {
    WbSwingUtilities.invoke(new Runnable()
    {
      @Override
      public void run()
      {
        dialogChoice = showDialog(caller);
      }
    });

    return dialogChoice;
  }

  private int showDialog(final Component caller)
  {
    String[] options = new String[]
    {
      ResourceMgr.getString("LblIgnoreThis"), ResourceMgr.getString("LblIgnoreAllErr"), ResourceMgr.getPlainString("LblStopScript")
    };

    final Object message = getMessage();

    JOptionPane ignorePane = new WbOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options);

    JDialog dialog = ignorePane.createDialog(WbSwingUtilities.getWindowAncestor(caller), ResourceMgr.TXT_PRODUCT_NAME);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    int rvalue = JOptionPane.CANCEL_OPTION;

    try
    {
      dialog.setResizable(true);
      dialog.pack();
      dialog.setVisible(true);
      Object result = ignorePane.getValue();
      if (result == null)
      {
        rvalue = JOptionPane.CANCEL_OPTION;
      }
      else if (result.equals(options[0]))
      {
        rvalue = WbSwingUtilities.IGNORE_ONE;
      }
      else if (result.equals(options[1]))
      {
        rvalue = WbSwingUtilities.IGNORE_ALL;
      }
      else if (result.equals(options[2]))
      {
        rvalue = JOptionPane.CANCEL_OPTION;
      }
      else
      {
        rvalue = JOptionPane.CANCEL_OPTION;
      }
    }
    finally
    {
      dialog.dispose();
    }
    return rvalue;
  }

  private Object getMessage()
  {
    if (GuiSettings.getErrorPromptType() == ErrorPromptType.PromptWithErroressage && errorDetails != null && errorDetails.getErrorMessage() != null)
    {
      return createMessagePanel();
    }
    else
    {
      return question;
    }
  }

  private JComponent createMessagePanel()
  {
    JPanel messagePanel = getMultilineLabel(question);

    String errorMessage = errorDetails != null ? errorDetails.getErrorMessage() : "";

    Font messageFont = UIManager.getFont("OptionPane.messageFont");
    FontMetrics fm = messagePanel.getFontMetrics(messageFont);
    int vgap = fm.getHeight();

    messagePanel.setBorder(new EmptyBorder(0, 0, vgap/4, 0));

    JPanel panel = new JPanel(new BorderLayout(0, 0));
    JComponent errorPanel = createErrorMessagePanel(errorMessage, PROP_ERROR_MSG_WRAP, GuiSettings.allowWordWrapForErrorMessage());

    panel.add(messagePanel, BorderLayout.PAGE_START);
    panel.add(errorPanel, BorderLayout.CENTER);

    return panel;
  }


}
