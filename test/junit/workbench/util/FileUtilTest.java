/*
 * FileUtilTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import workbench.TestUtil;
import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class FileUtilTest
	extends WbTestCase
{
	private TestUtil testUtil;

	public FileUtilTest()
	{
		super("FileUtilTest");
		testUtil = getTestUtil();
	}

	@Test
	public void testGetLines()
	{
		try
		{
			File f = new File(testUtil.getBaseDir(), "somedata.txt");
			String encoding = "ISO-8859-1";
			Writer w = EncodingUtil.createWriter(new FileOutputStream(f), encoding);
			for (int i=0; i < 100; i++)
			{
				w.write("line_" + i + "\n");
			}
			w.close();

			BufferedReader in = new BufferedReader(new FileReader(f));

			Collection<String> lines = FileUtil.getLines(in);
			assertEquals(100, lines.size());
			for (int i=0; i < 100; i++)
			{
				assertTrue(lines.contains("line_" + i));
			}
		}
		catch (Throwable th)
		{
			th.printStackTrace();
			fail(th.getMessage());
		}
	}

	@Test
	public void testReadLines()
	{
		try
		{
			File f = new File(testUtil.getBaseDir(), "linetest.txt");
			String encoding = "ISO-8859-1";
			Writer w = EncodingUtil.createWriter(new FileOutputStream(f), encoding);
			w.write("Line 1\n");
			w.write("Line 2\n");
			w.close();

			BufferedReader in = EncodingUtil.createBufferedReader(f, encoding);
			StringBuilder content = new StringBuilder();
			int lines = FileUtil.readLines(in, content, 5, "\n");
			in.close();
			assertEquals("Not enough lines", 2, lines);
			assertEquals("Content not read properly", "Line 1\nLine 2\n", content.toString());

			StringBuilder fileContent = new StringBuilder();
			for (int i = 0; i < 15; i++)
			{
				fileContent.append("Line " + i + "\n");
			}
			w = EncodingUtil.createWriter(new FileOutputStream(f), encoding);
			w.write(fileContent.toString());
			w.close();

			content = new StringBuilder();
			in = EncodingUtil.createBufferedReader(f, encoding);
			lines = FileUtil.readLines(in, content, 10, "\n");
			assertEquals("Wrong line count: ", 10, lines);
			lines = FileUtil.readLines(in, content, 10, "\n");
			in.close();
			assertEquals("Wrong line count: ", 5, lines);
			assertEquals("Wrong content retrieved", fileContent.toString(), content.toString());

			fileContent = new StringBuilder();
			for (int i = 0; i < 237; i++)
			{
				fileContent.append("Line " + i + "\n");
			}
			w = EncodingUtil.createWriter(new FileOutputStream(f), encoding);
			w.write(fileContent.toString());
			w.close();

			content = new StringBuilder(1000);
			in = EncodingUtil.createBufferedReader(f, encoding);
			lines = FileUtil.readLines(in, content, 10, "\n");
			while (lines == 10)
			{
				lines = FileUtil.readLines(in, content, 10, "\n");
			}
			in.close();
			assertEquals("Wrong content retrieved", fileContent.toString(), content.toString());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetLineEnding()
		throws Exception
	{
		try
		{
			File f = new File(testUtil.getBaseDir(), "ending.txt");

			String encoding = "ISO-8859-1";
			Writer w = EncodingUtil.createWriter(new FileOutputStream(f), encoding);
			w.write("Line 1\n");
			w.write("Line 2");
			w.close();

			Reader r = EncodingUtil.createReader(f, encoding);
			String ending = FileUtil.getLineEnding(r);
			assertEquals("Unix line ending not detected", "\n", ending);
			r.close();

			w = EncodingUtil.createWriter(new FileOutputStream(f), encoding);
			w.write("Line 1");
			w.write("\r\n");
			w.write("Line 2");
			w.close();

			r = EncodingUtil.createReader(f, encoding);
			ending = FileUtil.getLineEnding(r);
			assertEquals("DOS line ending not detected", "\r\n", ending);
			r.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEstimateRecords()
		throws Exception
	{
		try
		{
			File f = new File(testUtil.getBaseDir(), "short_file.txt");
			PrintWriter pw = new PrintWriter(new FileWriter(f));
			pw.println("Line 1");
			pw.close();
			long r = FileUtil.estimateRecords(f, 10);
			// as the file is only one line, we expect 1 as the result
			assertEquals("Wrong lines estimated", 1, r);

			// Test empty file
			pw = new PrintWriter(new FileWriter(f));
			pw.close();
			r = FileUtil.estimateRecords(f, 10);
			assertEquals("Wrong lines estimated", 0, r);

			// Test normal file
			pw = new PrintWriter(new FileWriter(f));
			for (int i = 0; i < 100; i++)
			{
				pw.println("Line data");
			}
			pw.close();
			r = FileUtil.estimateRecords(f, 10);
			assertEquals("Wrong lines estimated", 100, r);
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testCopy()
	{
		try
		{
			File sf = new File(testUtil.getBaseDir(), "sourcefile.dat");
			OutputStream out = new FileOutputStream(sf);
			for (int i = 0; i < 32768; i++)
			{
				out.write(i);
			}
			out.close();
			InputStream in = new FileInputStream(sf);

			File tf = new File("copy.dat");
			OutputStream target = new FileOutputStream("copy.dat");
			FileUtil.copy(in, target);

			assertEquals("Wrong file size", tf.length(), sf.length());

			sf.delete();
			tf.delete();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testReadCharacters()
		throws Exception
	{
		try
		{
			File f = new File(testUtil.getBaseDir(), "chartest.txt");
			PrintWriter pw = new PrintWriter(new FileWriter(f));
			String content = "Don't panic, count to ten... then panic!";
			pw.print(content);
			pw.close();

			FileReader in = new FileReader(f);
			String result = FileUtil.readCharacters(in);
			assertEquals("Wrong content retrieved", content, result);
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testReadBytes()
		throws Exception
	{
		try
		{
			File sf = new File(testUtil.getBaseDir(), "sourcefile.dat");
			OutputStream out = new FileOutputStream(sf);
			for (int i = 0; i < 32768; i++)
			{
				out.write(i);
			}
			out.close();
			InputStream in = new FileInputStream(sf);

			byte[] b = FileUtil.readBytes(in);
			assertEquals("Wrong file size", (int) sf.length(), b.length);

			assertEquals("Wrong content read from file", 1, b[1]);
			assertEquals("Wrong content read from file", 2, b[2]);
			assertEquals("Wrong content read from file", 3, b[3]);
			sf.delete();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetCharacterLength()
	{
		try
		{
			File f = new File(testUtil.getBaseDir(), "mydata.txt");
			String encoding = "ISO-8859-1";
			Writer out = new OutputStreamWriter(new FileOutputStream(f),encoding);
			String content = "This is a test";
			out.write(content);
			out.close();

			assertEquals(content.length(), FileUtil.getCharacterLength(f, encoding));

			encoding = "UTF-8";
			content = "This is a test for the UTF-8 length \u00c3\u00b6\u00c3\u00a4\u00c3\u00bc\u00c3\u2013\u00c3\u201e\u00c3\u0153. "+
				"Let's see how it works";
			out = new OutputStreamWriter(new FileOutputStream(f),encoding);
			out.write(content);
			out.close();

			assertEquals(content.length(), FileUtil.getCharacterLength(f, encoding));

			f.delete();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}

	}
}
