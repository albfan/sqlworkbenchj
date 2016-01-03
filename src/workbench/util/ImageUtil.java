/*
 * ImageUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import workbench.WbManager;
import workbench.log.LogMgr;
import workbench.resource.Settings;


/**
 *
 * @author Thomas Kellerer
 */
public class ImageUtil
{
	public enum GifType
	{
		Standard,
		Animated,
		None;
	}

	public static boolean isPng(File image)
	{
		if (image == null) return false;
		if (!image.exists()) return false;

		boolean valid = false;
		InputStream in = null;

		try
		{
			in = new FileInputStream(image);
			byte[] header = new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10};
			byte[] buffer = new byte[header.length];

			in.read(buffer, 0, header.length);
			valid = Arrays.equals(header, buffer);
		}
		catch (Throwable th)
		{
			valid = false;
		}
		finally
		{
			FileUtil.closeQuietely(in);
		}
		return valid;
	}

	public static boolean isGifIcon(File file)
	{
		return getGifType(file) == GifType.Standard;
	}

	/**
	 * Returns the type of an GIF image.
	 *
	 * Taken from: https://forums.oracle.com/thread/1270979
	 *
	 * @param file  the file to check
	 * @return the type of the GIF image
	 */
	public static GifType getGifType(File file)
	{
		byte[] version = new byte[3];
		InputStream in = null;
		GifType result = GifType.None;

		try
		{
			in = new BufferedInputStream(new FileInputStream(file));

			// Read the GIF header and check if the file is a GIF file
			in.read(version, 0, 3);

			byte[] gif = new byte[] { 0x47, 0x49, 0x46 }; // "GIF"

			if (Arrays.equals(version, gif))
			{
				result = GifType.Standard;

				int numberOfExtension = 0;
				boolean extensionIntro = false;

				int value = in.read();

				// Read until the EOF or if extensions are > 2
				// We try to detect the Graphic Control Extension, beginning with 0x21 0xF9
				while (value != -1 && numberOfExtension < 2)
				{
					if (value == 0x21)
					{
						extensionIntro = true;
					}
					else if (value == 0xF9 && extensionIntro)
					{
						numberOfExtension++;
						extensionIntro = false;
					}
					else
					{
						extensionIntro = false;
					}
					value = in.read();
				}

				if (numberOfExtension > 1)
				{
					result = GifType.Animated;
				}
			}
		}
		catch (Throwable th)
		{
			result = GifType.None;
		}
		finally
		{
			FileUtil.closeQuietely(in);
		}
		return result;
	}

	/**
	 * Extracts the filenames from the given (path-like) list of files and checks
	 * if they reference valid (usable) icon files.
	 *
	 * @see #isPng(java.io.File)
	 * @see #isGifIcon(java.io.File)
	 *
	 * @return a list of valid icon files, never null
	 *
	 */
	public static List<File> getIcons(String iconList)
	{
		List<File> iconFiles = new ArrayList<>(2);
		if (StringUtil.isBlank(iconList)) return iconFiles;

		try
		{
			List<String> fileNames = StringUtil.stringToList(iconList, System.getProperty("path.separator"));
			File jarDir = WbManager.getInstance().getJarFile().getParentFile();
			File confDir = Settings.getInstance().getConfigDir();

			for (String fname : fileNames)
			{
				if (StringUtil.isNonEmpty(fname))
				{
					File f = new File(fname);
					if (!f.isAbsolute())
					{
						f = new File(jarDir, fname);
						if (!f.exists())
						{
							f = new File(confDir, fname);
						}
					}
					if (ImageUtil.isPng(f) || ImageUtil.isGifIcon(f))
					{
						iconFiles.add(f);
					}
					else
					{
						LogMgr.logWarning("ImageUtil.getIcons()", "Ignoring invalid icon file: " + f.getAbsolutePath());
					}
				}
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("ImageUtil.getIcons()", "Could not retrieve list of icon files", e);
			iconFiles.clear();
		}
		return iconFiles;
	}

}
