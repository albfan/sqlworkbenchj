/*
 * WbVersionReader.java
 *
 * Created on January 5, 2004, 6:59 PM
 */

package workbench.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import workbench.log.LogMgr;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbVersionReader
{

	private String currentDevBuildNumber;
	private String currentDevBuildDate;
	
	private String currentStableBuildNumber;
	private String currentStableBuildDate;
	
	public WbVersionReader()
		throws Exception
	{
		long start, end;
		start = System.currentTimeMillis();
		this.readDevBuildInfo();
		this.readStableBuildInfo();
		end = System.currentTimeMillis();
		LogMgr.logDebug("WbVersionReader.<init>", "Retrieving version information took " + (end - start) + "ms");
	}

	private void readDevBuildInfo()
		throws Exception
	{
		try
		{
			URL conn = new URL("http://www.kellerer.org/workbench/Workbench.jar");
			LogMgr.logDebug("WbVersionReader.readDevBuildInfo", "Retrieving development version information...");
			InputStream httpStream = conn.openStream();
			JarInputStream devBuildArchive = new JarInputStream(httpStream);

			Manifest mani = devBuildArchive.getManifest();
			Attributes attrs = mani.getMainAttributes();
			String value = attrs.getValue("WbBuild-Number");
			if (value != null) this.currentDevBuildNumber = value.trim();
			
			value = attrs.getValue("WbBuild-Date");
			if (value != null) this.currentDevBuildDate = value.trim();

			devBuildArchive.close();
			httpStream.close();
		}
		catch (Exception e)
		{
			LogMgr.logWarning("WbVersionReader.readDevBuildInfo()", "Could not read version information for development build");
			throw e;
		}
		finally
		{
			LogMgr.logDebug("WbVersionReader.readDevBuildInfo", "Retrieving development version information done.");
		}
	}
	
	private void readStableBuildInfo()
		throws Exception
	{
		String historyLine = null;
		try
		{
			LogMgr.logDebug("WbVersionReader.readDevBuildInfo", "Retrieving release version information...");
			URL conn = new URL("http://www.kellerer.org/workbench/workbench.zip");

			InputStream zipStream = conn.openStream();
			ZipInputStream zipInput = new ZipInputStream(zipStream);

			ZipEntry jarEntry = zipInput.getNextEntry();

			while (jarEntry != null)
			{
				String name = jarEntry.getName();
				if ("workbench.jar".equalsIgnoreCase(name))
				{
					JarInputStream jar = new JarInputStream(zipInput);
					Manifest mani = jar.getManifest();
					Attributes attrs = mani.getMainAttributes();
					String value = attrs.getValue("WbBuild-Number");
					if (value != null) this.currentStableBuildNumber = value.trim();
					
					value = attrs.getValue("WbBuild-Date");
					if (value != null) this.currentStableBuildDate = value.trim();
					break;
				}
				jarEntry = zipInput.getNextEntry();
			}
			zipInput.close();
			zipStream.close();
		}
		catch (Exception e)
		{
			LogMgr.logWarning("WbVersionReader.readStableBuildInfo()", "Could not read version information for release build");
			throw e;
		}
		finally
		{
			LogMgr.logDebug("WbVersionReader.readDevBuildInfo", "Retrieving release version information done.");
		}
		
		if (historyLine != null)
		{
			int spacePos = historyLine.indexOf(' ');
			int commaPos = historyLine.indexOf(',');
			if (spacePos > -1)
			{
				this.currentStableBuildNumber = historyLine.substring(spacePos, commaPos);
				this.currentStableBuildDate = historyLine.substring(commaPos + 2);
			}
		}
	}
	public String getDevBuildNumber() { return this.currentDevBuildNumber; }
	public String getDevBuildDate() { return this.currentDevBuildDate; }

	public String getStableBuildNumber() { return this.currentStableBuildNumber; }
	public String getStableBuildDate() { return this.currentStableBuildDate; }
	
	public static void main(String[] args)
	{
		try
		{
			WbVersionReader version = new WbVersionReader();
			System.out.println("dev build = " + version.getDevBuildDate());
			System.out.println("stable = " + version.getStableBuildNumber());
			System.out.println("stable date = " + version.getStableBuildDate());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
		}
	}
}
