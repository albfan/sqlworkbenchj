/*
 * ImagePanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.FileUtil;


/**
 * @author support@sql-workbench.net
 */
public class ImagePanel
	extends JPanel
{
	private Image displayImage;
	private JLabel label = new JLabel();

	public ImagePanel()
	{
		super();
		this.setLayout(new BorderLayout());
		this.add(label, BorderLayout.CENTER);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setBorder(new EtchedBorder());
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

	public boolean hasImage() { return this.displayImage != null; }

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
			FileUtil.closeQuitely(in);
		}

		if (displayImage == null)
		{
			label.setText(ResourceMgr.getString("ErrImgNotSupp"));
		}
		else
		{
			label.setIcon(new ImageIcon(this.displayImage));
		}
	}

}
