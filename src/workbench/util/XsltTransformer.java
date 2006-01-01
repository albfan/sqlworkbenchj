/*
 * XsltTransformer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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

/**
 *  Xslt transformer using the JDK built-in XSLT stuff
 */
public class XsltTransformer
{
	private Transformer transformer;

	public XsltTransformer(File xslfile)
		throws IOException, TransformerException
	{
		if(!xslfile.exists())
		{
			throw new FileNotFoundException("File "+xslfile.getAbsolutePath()+" doesn't exist");
		}
		Source sxslt = new StreamSource(new FileInputStream(xslfile));
		sxslt.setSystemId(xslfile.getName());
		TransformerFactory factory = TransformerFactory.newInstance();
		this.transformer = factory.newTransformer(sxslt);
	}

	public void transform(InputStream inXml, OutputStream out)
		throws TransformerException
	{
		Source xmlSource = new StreamSource(inXml);
		StreamResult res = new StreamResult(out);
		transformer.transform(xmlSource, res);
		try { inXml.close(); } catch (Throwable ignore) {} 
		try { out.close(); } catch (Throwable ignore) {} 
	}

	public static void transformFile(String inputFileName, String outputFilename, String xsltFile)
		throws TransformerException, IOException
	{
		File f = new File(xsltFile);
		XsltTransformer trans = new XsltTransformer(f);
		InputStream in = new BufferedInputStream(new FileInputStream(inputFileName),32*1024);
		OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFilename), 32*1024);
		trans.transform(in, out);
		try { in.close(); } catch (Throwable ignore) {} 
		try { out.close(); } catch (Throwable ignore) {} 
	}

	public static void main(String args[])
	{
		try
		{
			if (args.length != 3)
			{
				System.out.println("Call with: XsltTransformer inputfile outputfile stylesheet");
			}
			else
			{
				transformFile(args[0], args[1], args[2]);
				System.out.println(args[0] + " has been successfully transformed into " + args[1]);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
