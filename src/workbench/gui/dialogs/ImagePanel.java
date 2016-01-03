/*
 * ImagePanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.dialogs;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.UIManager;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.FileUtil;


/**
 * A panel that display a BLOB as an image.
 *
 * @author Thomas Kellerer
 */
public class ImagePanel
	extends JPanel
{
	private Image displayImage;

	public ImagePanel()
	{
		super();
		setBorder(BorderFactory.createEtchedBorder());
	}

	public void setImage(File imageData)
		throws IOException, SQLException
	{
		InputStream in = new BufferedInputStream(new FileInputStream(imageData));
		this.readImageData(in);
	}

	public void setImage(Blob imageData)
		throws IOException, SQLException
	{
		byte[] data = imageData.getBytes(1, (int)imageData.length());
		setImage(data);
	}

	public void setImage(byte[] imageData)
		throws IOException
	{
		if (imageData == null) return;
		if (imageData.length < 4) return;
		InputStream in = new ByteArrayInputStream(imageData);
		this.readImageData(in);
	}

	public boolean hasImage()
	{
		return this.displayImage != null;
	}

	private void readImageData(InputStream in)
		throws IOException
	{
		if (displayImage != null)
		{
			displayImage.flush();
		}

		try
		{
			displayImage = ImageIO.read(in);
		}
		catch (Exception e)
		{
			displayImage = null;
			LogMgr.logError("ImagePanel.readImageData", "Error reading image", e);
		}
		finally
		{
			FileUtil.closeQuietely(in);
		}
	}

	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		int panelWidth = getWidth();
		int panelHeight = getHeight();
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		if (displayImage != null)
		{
			int imageWidth = displayImage.getWidth(this);
			int imageHeight = displayImage.getHeight(this);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			if (imageWidth > panelWidth || imageHeight > panelHeight)
			{
				g2.drawImage(displayImage, 0, 0, panelWidth, panelHeight, null);
			}
			else
			{
				int x = (panelWidth - imageWidth) / 2;
				int y = (panelHeight - imageHeight) / 2;
				g2.drawImage(displayImage, x, y, imageWidth, imageHeight, null);
			}
		}
		else
		{
			String txt = ResourceMgr.getString("ErrImgNotSupp");
			Font std = UIManager.getDefaults().getFont("Label.font");
			Font f = std.deriveFont((float)12);
			g.setFont(f);
			FontMetrics fm = g.getFontMetrics();
			int width = fm.stringWidth(txt);
			int height = fm.getHeight();
			int x = (panelWidth - width) / 2;
			int y = (panelHeight - height) / 2;

			Toolkit tk = Toolkit.getDefaultToolkit();
			Map renderingHints = (Map) tk.getDesktopProperty("awt.font.desktophints");
			g2.addRenderingHints(renderingHints);

			g2.drawString(txt, x, y);
		}
	}
}
