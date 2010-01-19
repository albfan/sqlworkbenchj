/*
 * XsltTransformer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import workbench.resource.Settings;

/**
 *  Xslt transformer using the JDK built-in XSLT support
 *
 * @author Thomas Kellerer
 */
public class XsltTransformer
	implements URIResolver
{
	private Exception resolveError;
	private File xsltBasedir;
	private String sysOut;
	private String sysErr;
	private boolean saveSystemOut;
	private File xsltUsed;
	

	/**
	 * The directory where the initially defined XSLT is stored.
	 * Will be set by transform() in order to be able to
	 * resolve includes or imports from the same directory.
	 */
	private File sourceDir;

	public XsltTransformer()
	{
	}

	/**
	 * Define a directory to be searched for when looking for an xstl file
	 * @param dir
	 */
	public void setXsltBaseDir(File dir)
	{
		this.xsltBasedir = dir;
	}

	public void setSAveSystemOutMessages(boolean flag)
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
		xsltUsed = xslFile;
		transform(inputFile, outputFile, xslFile, parameters);
	}

	public void transform(File inputFile, File outputFile, File xslfile, Map<String, String> parameters)
		throws IOException, TransformerException
	{
		if (!xslfile.exists())
		{
			throw new FileNotFoundException("File " + xslfile.getAbsolutePath() + " doesn't exist");
		}

		InputStream in = null;
		OutputStream out = null;
		InputStream xlsInput = null;
		Transformer transformer = null;

		ByteArrayOutputStream systemOut = null;
		ByteArrayOutputStream systemErr = null;
		PrintStream oldOut = System.out;
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
				System.setErr(new PrintStream(systemErr));
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
					transformer.setParameter(entry.getKey(), entry.getValue());
				}
			}
			in = new BufferedInputStream(new FileInputStream(inputFile),32*1024);
			out = new BufferedOutputStream(new FileOutputStream(outputFile), 32*1024);
			Source xmlSource = new StreamSource(in);
			StreamResult result = new StreamResult(out);
			transformer.transform(xmlSource, result);
		}
		finally
		{
			FileUtil.closeQuitely(xlsInput);
			FileUtil.closeQuitely(in);
			FileUtil.closeQuitely(out);
			if (saveSystemOut)
			{
				System.setOut(oldOut);
				System.setErr(oldErr);
				sysOut = (systemOut != null ? systemOut.toString() : null);
				sysErr = (systemErr != null ? systemErr.toString() : null);
			}
		}
	}

	public String getSystemErr()
	{
		return sysErr;
	}
	
	public String getSystemOut()
	{
		return sysOut;
	}
	
	public Exception getNestedError()
	{
		return resolveError;
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
			resolveError = e;
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

	public static void main(String[] args)
	{
		try
		{
			if (args.length < 3)
			{
				System.out.println("Call with: XsltTransformer inputfile outputfile stylesheet [param=value ...]");
			}
			else
			{
				XsltTransformer transformer = new XsltTransformer();

				Map<String, String> parameters = new HashMap<String, String>();
				if (args.length > 3)
				{
					for (int i=3; i < args.length; i++)
					{
						String[] pardef = args[i].split("=");
						if (pardef.length == 2)
						{
							parameters.put(pardef[0], pardef[1]);
						}
						else
						{
							System.out.println("Ignoring incorrect parameter definition: " + args[i]);
						}
					}
				}
				transformer.transform(args[0], args[1], args[2], parameters);
				System.out.println(args[0] + " has been successfully transformed into " + args[1]);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

}
