/*
 * Created on January 18, 2003, 11:45 AM
 */
package workbench.log;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 *
 * @author  workbench@kellerer.org
 */
public class DuplicatingPrintStream
	extends PrintStream
{
	private PrintStream secondaryStream = null;
	public DuplicatingPrintStream(OutputStream primary, PrintStream secondary)
	{
		super(primary);
		this.secondaryStream = secondary;
	}
	
	public void print(String aMsg)
	{
		super.print(aMsg);
		if (this.secondaryStream != null)
		{
			this.secondaryStream.print(aMsg);
		}
	}
	
	public void println(String aMsg)
	{
		super.println(aMsg);
		if (this.secondaryStream != null)
		{
			this.secondaryStream.println(aMsg);
		}
	}
	
	public void println()
	{
		super.println();
		if (this.secondaryStream != null)
		{
			this.secondaryStream.println();
		}
	}
	
}
