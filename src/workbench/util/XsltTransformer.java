package workbench.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Locale;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerException;

/**
 *  Xslt transformer using the JDK built-in XSLT stuff
 */
public class XsltTransformer
{
	private Transformer transformer;
	private String basedir;

	public XsltTransformer(File xslfile)
		throws IOException, TransformerException
	{
		if(!xslfile.exists())
		{
			throw new FileNotFoundException("File "+xslfile.getAbsolutePath()+" doesn't exist");
		}
		this.basedir = xslfile.getParent();
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
	}

	public static void transformFile(String inputFileName, String outputFilename, String xsltFile)
		throws TransformerException, IOException
	{
		File f = new File(xsltFile);
		XsltTransformer trans = new XsltTransformer(f);
		InputStream in = new BufferedInputStream(new FileInputStream(inputFileName));
		OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFilename));
		trans.transform(in, out);
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