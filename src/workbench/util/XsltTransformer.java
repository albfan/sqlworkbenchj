/*
 * XsltTransformer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import workbench.resource.Settings;

/**
 *  Xslt transformer using the JDK built-in XSLT support
 *
 * @author support@sql-workbench.net
 */
public class XsltTransformer
{
	private File xsltBasedir;

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
	
	public void transform(String inputFileName, String outputFileName, String xslFileName)
		throws IOException, TransformerException
	{
		File inputFile = new File(inputFileName);
		File outputFile = new File(outputFileName);
		File xslFile = findStylesheet(xslFileName);
		transform(inputFile, outputFile, xslFile);
	}

	public void transform(File inputFile, File outputFile, File xslfile)
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
		try
		{
			xlsInput = new FileInputStream(xslfile);

			Source sxslt = new StreamSource(xlsInput);
			sxslt.setSystemId(xslfile.getName());
			TransformerFactory factory = TransformerFactory.newInstance();
			transformer = factory.newTransformer(sxslt);

			in = new BufferedInputStream(new FileInputStream(inputFile),32*1024);
			out = new BufferedOutputStream(new FileOutputStream(outputFile), 32*1024);
			Source xmlSource = new StreamSource(in);
			StreamResult result = new StreamResult(out);
			transformer.transform(xmlSource, result);
		}
		finally
		{
			if (transformer != null) transformer.reset();
			FileUtil.closeQuitely(xlsInput);
			FileUtil.closeQuitely(in);
			FileUtil.closeQuitely(out);
		}
	}

	/**
	 * Searches for a stylesheet. <br/>
	 * If the filename is an absolute filename, no searching takes place.
	 * <br/>
	 * If the filename does not include a
	 * directory then the xslt sub-directory of the installation directory
	 * is checked first. <br/>
	 * Then the supplied base directory is checked, if nothing is found
	 * the confid directory is checked and finally the installation directory.
	 */
	public File findStylesheet(String file)
	{
		File f = new File(file);
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
			if (args.length != 3)
			{
				System.out.println("Call with: XsltTransformer inputfile outputfile stylesheet");
			}
			else
			{
				XsltTransformer transformer = new XsltTransformer();
				transformer.transform(args[0], args[1], args[2]);
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
