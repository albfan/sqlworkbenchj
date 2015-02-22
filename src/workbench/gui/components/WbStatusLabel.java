/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.components;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import workbench.interfaces.StatusBar;
import workbench.resource.IconMgr;

import workbench.gui.WbSwingUtilities;

import workbench.storage.RowActionMonitor;

/**
 *
 * @author Thomas Kellerer
 */
public class WbStatusLabel
  extends JLabel
  implements StatusBar
{
	private static final Border DEFAULT_BORDER = new CompoundBorder(new EmptyBorder(2, 0, 0, 0), new CompoundBorder(BorderFactory.createEtchedBorder(), new EmptyBorder(1, 1, 1, 0)));

  public WbStatusLabel(String text)
  {
    super(text);
    setBorder(DEFAULT_BORDER);
    initSize();
  }

  public WbStatusLabel()
  {
    super();
    setBorder(DEFAULT_BORDER);
    initSize();
  }

  @Override
  public void setFont(Font f)
  {
    super.setFont(f);
    initSize();
  }

  private void initSize()
  {
		Font f = getFont();
		FontMetrics fm = null;
		if (f != null) fm = getFontMetrics(f);
    int height = 0;
    int width = 0;
    int borderHeight = 6;
    if (fm != null)
    {
      height = (int)(fm.getHeight() * 1.2) + borderHeight;
      width = fm.charWidth('W');
      height = Math.max(22, height);
      width = Math.max(80, width * 10);
    }
    else
    {
      int size = (int)(IconMgr.getInstance().getSizeForLabel() * 1.2) + borderHeight;
      width = size;
      height = size;
    }
		Dimension d = new Dimension(width, height);
		setMinimumSize(d);
		setPreferredSize(d);
  }

  public RowActionMonitor getMonitor()
  {
    return new GenericRowMonitor(this);
  }

  @Override
  public void setStatusMessage(String message, int duration)
  {
    setStatusMessage(message);
  }

  @Override
  public void doRepaint()
  {
    repaint();
  }

  @Override
  public void setStatusMessage(final String message)
  {
    WbSwingUtilities.invoke(new Runnable()
    {
      @Override
      public void run()
      {
        setText(message);
      }

    });
  }

  @Override
  public void clearStatusMessage()
  {
    WbSwingUtilities.invoke(new Runnable()
    {
      @Override
      public void run()
      {
        setText("");
      }

    });
  }

  @Override
  public String getText()
  {
    return super.getText();
  }

}
