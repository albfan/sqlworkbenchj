/*
 * LoadingImage.java
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
package workbench.gui.settings;

import java.awt.Image;

import javax.swing.ImageIcon;

import workbench.resource.IconMgr;

/**
 * A container to store the name and the real image for the busy icon dropdown.
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
		image = IconMgr.getInstance().getLoadingImage(name);
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
		Image img = getImage();
		if (img != null)
		{
			img.flush();
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
