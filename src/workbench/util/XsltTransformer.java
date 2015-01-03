/*
 * XsltTransformer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *  Xslt transformer using the JDK built-in XSLT support
 *
 * @author Thomas Kellerer
 */
public class XsltTransformer
	implements URIResolver
{
	private Exception nestedError;
	private File xsltBasedir;
	private String sysOut;
	private String sysErr;
	private boolean saveSystemOut;
	private File xsltUsed;

	private List<String> ignoredMessages = new ArrayList<>();

	/**
	 * The directory where the initially defined XSLT is stored.
	 * Will be set by transform() in order to be able to
	 * resolve includes or imports from the same directory.
	 */
	private File sourceDir;

	public XsltTransformer()
	{
		ignoredMessages.add("'http://javax.xml.XMLConstants/property/accessExternalDTD'");
		ignoredMessages.add("'http://javax.xml.XMLConstants/feature/secure-processing'");
		ignoredMessages.add("'http://www.oracle.com/xml/jaxp/properties/entityExpansionLimit'");
	}

	/**
	 * Define a directory to be searched for when looking for an xstl file
	 * @param dir
	 */
	public void setXsltBaseDir(File dir)
	{
		this.xsltBasedir = dir;
	}

	public void setSaveSystemOutMessages(boolean flag)
	{
		saveSystemOut = flag;
	}

	public void transform(String inputFileName, String outputFileName, String xslFileName)
		throws IOException, TransformerException
	{
		transform(inputFileName, outputFileName, xslFileName, null);
	}

	public File getXsltUsed()
	{
		return xsltUsed;
	}

	public void transform(String inputFileName, String outputFileName, String xslFileName, Map<String, String> parameters)
		throws IOException, TransformerException
	{
		File inputFile = new File(inputFileName);
		File outputFile = new File(outputFileName);
		File xslFile = findStylesheet(xslFileName);
		transform(inputFile, outputFile, xslFile, parameters);
	}

	public void transform(File inputFile, File outputFile, File xslfile, Map<String, String> parameters)
		throws IOException, TransformerException
	{
		if (!xslfile.exists())
		{
			throw new FileNotFoundException("File " + xslfile.getAbsolutePath() + " doesn't exist");
		}

		xsltUsed = xslfile;

		InputStream in = null;
		OutputStream out = null;
		InputStream xlsInput = null;
		Transformer transformer = null;

		ByteArrayOutputStream systemOut = null;
		ByteArrayOutputStream systemErr = null;
		final PrintStream oldOut = System.out;
		PrintStream oldErr = System.err;
		try
		{
			xlsInput = new FileInputStream(xslfile);
			sourceDir = xslfile.getParentFile();

			if (saveSystemOut)
			{
				systemOut = new ByteArrayOutputStream();
				System.setOut(new PrintStream(systemOut));
				systemErr = new ByteArrayOutputStream();
				System.setErr(new PrintStream(systemErr)
				{
					@Override
					public void println(String x)
					{
						// if the Xerces parser is in the class path it emits some useless warnings
						// that are filtered out here
						if (!ignoreXsltMessage(x))
						{
							super.println(x);
						}
					}
				});
			}

			Source sxslt = new StreamSource(xlsInput);
			sxslt.setSystemId(xslfile.getName());
			TransformerFactory factory = TransformerFactory.newInstance();
			factory.setURIResolver(this);

			transformer = factory.newTransformer(sxslt);
			transformer.setURIResolver(this);

			if (parameters != null)
			{
				for (Map.Entry<String, String> entry : parameters.entrySet())
				{
					String value = StringUtil.decodeUnicode(entry.getValue());
					transformer.setParameter(entry.getKey(), value);
				}
			}
			File outDir = outputFile.getParentFile();
			transformer.setParameter("wb-basedir", outDir.getAbsolutePath());

			File scriptDir = inputFile.getParentFile();
			transformer.setParameter("wb-scriptdir", scriptDir.getAbsolutePath());

			in = new BufferedInputStream(new FileInputStream(inputFile),32*1024);
			out = new BufferedOutputStream(new FileOutputStream(outputFile), 32*1024);
			Source xmlSource = new StreamSource(in);
			StreamResult result = new StreamResult(out);
			transformer.transform(xmlSource, result);
		}
		finally
		{
			FileUtil.closeQuietely(xlsInput);
			FileUtil.closeQuietely(in);
			FileUtil.closeQuietely(out);
			if (saveSystemOut)
			{
				System.setOut(oldOut);
				System.setErr(oldErr);
				sysOut = (systemOut != null ? systemOut.toString() : null);
				sysErr = (systemErr != null ? systemErr.toString() : null);
			}
		}
	}

	private boolean ignoreXsltMessage(String message)
	{
		for (String msg : ignoredMessages)
		{
			if (message.contains(msg)) return true;
		}
		return false;
	}

	public Exception getNestedError()
	{
		return nestedError;
	}

	public String getAllOutputs()
	{
		return getAllOutputs(null);
	}

	private String getSystemErr()
	{
		if (StringUtil.isBlank(sysErr)) return "";
		int index = sysErr.indexOf("Compiler warnings:");
		if (index > 0)
		{
			return sysErr.substring(0, index - 1).trim();
		}
		return sysErr.trim();
	}

	public String getAllOutputs(Exception e)
	{
		StringBuilder result = new StringBuilder();
		result.append(getSystemErr());

		if (StringUtil.isNonBlank(sysOut))
		{
			if (result.length() > 0) result.append('\n');
			result.append(sysOut.trim());
		}
		if (nestedError != null)
		{
			result.append('\n');
			if (e != null)
			{
				result.append(e.getMessage());
				result.append(": ");
			}
			result.append(ExceptionUtil.getDisplay(nestedError));
		}
		if (result.length() == 0 && e != null)
		{
			result.append(ResourceMgr.getFormattedString("ErrXsltProcessing", xsltUsed.getAbsolutePath(), ExceptionUtil.getDisplay(e)));
		}
		return result.toString();
	}

	@Override
	public Source resolve(String href, String base)
		throws TransformerException
	{
		File referenced = new File(base);

		try
		{
			if (referenced.exists())
			{
				return new StreamSource(new FileInputStream(referenced));
			}
			File toUse = new File(sourceDir, href);
			if (toUse.exists())
			{
				return new StreamSource(new FileInputStream(toUse));
			}
			toUse = findStylesheet(href);
			return new StreamSource(new FileInputStream(toUse));
		}
		catch (FileNotFoundException e)
		{
			nestedError = e;
			throw new TransformerException(e);
		}
	}

	/**
	 * Searches for a stylesheet. <br/>
	 * If the filename is an absolute filename, no searching takes place.
	 * <br/>
	 * If the filename does not include a directory then the xslt sub-directory
	 * of the installation directory is checked first. <br/>
	 * Then the supplied base directory is checked, if nothing is found
	 * the confid directory is checked and finally the installation directory.
	 * <br/>
	 * If the supplied filename does not have the .xslt extension, it is added
	 * before searching for the file.
	 */
	public File findStylesheet(String file)
	{
		WbFile f = new WbFile(file);
		if (StringUtil.isEmptyString(f.getExtension()))
		{
			f = new WbFile(file + ".xslt");
		}

		if (f.isAbsolute()) return f;
		if (f.getParentFile() == null)
		{
			// This is the default directory layout in the distribution archive
			File xsltdir = Settings.getInstance().getDefaultXsltDirectory();

			File totest = new File(xsltdir, file);
			if (totest.exists()) return totest;
		}
		if (this.xsltBasedir != null)
		{
			File totest = new File(xsltBasedir, file);
			if (totest.exists()) return totest;
		}
		File configdir = Settings.getInstance().getConfigDir();
		File totest = new File(configdir, file);
		if (totest.exists()) return totest;
		return new File(file);
	}

}
