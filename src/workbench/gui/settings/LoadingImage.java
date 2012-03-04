/*
 * LoadingImage.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.settings;

import java.awt.Image;
import javax.swing.ImageIcon;
import workbench.resource.ResourceMgr;

/**
 * A container to store the name and the real image for the busy icon droddown.
 * 
 * @author Thomas Kellerer
 */
class LoadingImage
{
	private String imageName;
	private ImageIcon image;

	LoadingImage()
	{

	}

	LoadingImage(String name)
	{
		imageName = name;
		image = ResourceMgr.getPicture(name);
	}

	public void setName(String name)
	{
		imageName = name;
	}

	public String getName()
	{
		return imageName;
	}

	public ImageIcon getImageIcon()
	{
		return image;
	}

	public Image getImage()
	{
		if (image == null) return null;
		return image.getImage();
	}

	public void dispose()
	{
		if (image != null)
		{
			image.getImage().flush();
		}
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof String)
		{
			return this.imageName.equals((String)other);
		}
		if (other instanceof LoadingImage)
		{
			return this.imageName.equals(((LoadingImage)other).imageName);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 3;
		hash = 31 * hash + (this.imageName != null ? this.imageName.hashCode() : 0);
		return hash;
	}
}
