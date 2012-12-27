/*
 * PrintPreviewPanel.java
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
package workbench.gui.print;

import java.awt.*;
import javax.swing.JPanel;

/**
 *
 * @author Thomas Kellerer
 */
public class PrintPreviewPanel
	extends JPanel
{
  private static final int BORDER_SIZE = 5;

	private Image preview;
	private Image scaledImage;
	private double scale = 1.0;
	private Dimension currentSize = new Dimension();

	public PrintPreviewPanel()
	{
		super();
	}

	public void setScale(double newScale)
	{
		this.scale = newScale;
		invalidate();
		doLayout();
	}

	public void setPreviewImage(Image img)
	{
		this.preview = img;
		this.currentSize.height = 0;
		this.currentSize.width = 0;
		invalidate();
		doLayout();
	}

	@Override
	public Dimension getPreferredSize()
	{
		int w = (int) (preview.getWidth(null) * scale);
		int h = (int) (preview.getHeight(null) * scale);

		if ((currentSize.width != w) || (currentSize.height != h))
		{
			currentSize.width = w;
			currentSize.height = h;
			scaledImage = preview.getScaledInstance(w, h, Image.SCALE_SMOOTH);
			setPreferredSize(new Dimension(w + 2 * BORDER_SIZE, h + 2 * BORDER_SIZE));
		}
		return super.getPreferredSize();
	}

	@Override
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

	@Override
	public Dimension getMaximumSize()
	{
		return getPreferredSize();
	}

	@Override
	public void paintComponent(Graphics g)
	{
		Graphics2D g2d = (Graphics2D)g;
		g.setColor(Color.LIGHT_GRAY);
		Stroke oldStroke = g2d.getStroke();
		g2d.setStroke(new BasicStroke((float)BORDER_SIZE));
		g.drawRect(0, 0, currentSize.width, currentSize.height);
		g.setColor(Color.WHITE);
		g2d.setStroke(oldStroke);
		int imgw = currentSize.width - BORDER_SIZE;
		int imgh = currentSize.height - BORDER_SIZE;
		g.fillRect(BORDER_SIZE, BORDER_SIZE, imgw, imgh);
		if (scaledImage != null)
		{
			g.drawImage(scaledImage, BORDER_SIZE, BORDER_SIZE, imgw, imgh, null);
		}
	}
}
