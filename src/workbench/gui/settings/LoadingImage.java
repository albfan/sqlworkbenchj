/*
 * LoadingImage.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.gui.settings;

import java.awt.Image;
import javax.swing.ImageIcon;
import workbench.resource.ResourceMgr;

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
