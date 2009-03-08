/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */
package workbench.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import workbench.util.FileUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class ConsolePager
	implements Pager
{
	private Process pagerProcess;
	private PrintStream out;
	private InputStream realIn;

	public ConsolePager()
	{
	}

	public boolean canPrintLine(int lineNumber)
	{
		return true;
	}

	public void startOutput()
		throws IOException
	{
		ProcessBuilder pb = new ProcessBuilder("more.com");
		pb.redirectErrorStream(true);

		pagerProcess = pb.start();
		out = new PrintStream(pagerProcess.getOutputStream());
		realIn = System.in;
		System.setIn(pagerProcess.getInputStream());
	}

	public void println(String s)
	{
		out.println(s);
	}
	
	public void outputFinished()
	{
		out.flush();
		pagerProcess.destroy();
		FileUtil.closeQuitely(out);
		System.setIn(realIn);
		realIn = null;
	}

	public static void main(String[] args)
	{
		System.out.println("starting...");
		ConsolePager pager = new ConsolePager();
		try
		{
			pager.startOutput();
			for (int i=0; i < 100; i++)
			{
				pager.println("This is Line " + i);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
		}
		finally
		{
			pager.outputFinished();
		}
		System.out.println("finished...");
	}
}
