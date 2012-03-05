/*
 * ImagePanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.settings;

import java.awt.*;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import workbench.gui.WbSwingUtilities;

/**
 *
 * @author Thomas Kellerer
 */
public class ImagePanel
	extends JPanel
	implements ListCellRenderer
{
	private Image image;
	private Dimension dim = new Dimension(32,18);

	public ImagePanel()
	{
		setOpaque(true);
	}

  @Override
  public void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    if (image != null)
    {
      g.drawImage(image, 0, 0, this);
    }
  }

	@Override
	public Dimension getPreferredSize()
	{
		return dim;
	}

	@Override
	public Dimension getMinimumSize()
	{
		return dim;
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		if (value instanceof LoadingImage)
		{
			image = ((LoadingImage)value).getImage();
		}
		else
		{
			image = null;
		}

		if (isSelected)
		{
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		}
		else
		{
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		WbSwingUtilities.repaintLater(this);
		WbSwingUtilities.repaintLater(list);

		return this;
	}

}
