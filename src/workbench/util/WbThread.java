/*
 * WbThread.java
 *
 * Created on 10. September 2004, 15:25
 */

package workbench.util;

/**
 *
 * @author tkellerer
 */
public class WbThread
	extends Thread
{

	/** Creates a new instance of WbThread */
	public WbThread(String name)
	{
		super();
		this.setName(name);
		this.setDaemon(true);
	}

	public WbThread(Runnable run, String name)
	{
		super(run);
		this.setName(name);
		this.setDaemon(true);
	}

}