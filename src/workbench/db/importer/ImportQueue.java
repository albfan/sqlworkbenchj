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

package workbench.db.importer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author support@sql-workbench.net
 */
public class ImportQueue
	extends ArrayBlockingQueue<ImportRow>
{
	private boolean stopped;

	public ImportQueue(int capacity, boolean fair)
	{
		super(capacity, fair);
	}

	public boolean isStopped()
	{
		return stopped;
	}
	
	public synchronized void stop()
	{
		clear();
		stopped = true;
	}

	@Override
	public boolean add(ImportRow e)
	{
		if (stopped) return false;
		return super.add(e);
	}

	@Override
	public boolean offer(ImportRow e)
	{
		if (stopped) return false;
		return super.offer(e);
	}

	@Override
	public boolean offer(ImportRow e, long timeout, TimeUnit unit)
		throws InterruptedException
	{
		if (stopped) return false;
		return super.offer(e, timeout, unit);
	}

	@Override
	public ImportRow peek()
	{
		if (stopped) return null;
		return super.peek();
	}

	@Override
	public ImportRow poll()
	{
		if (stopped) return null;
		return super.poll();
	}

	@Override
	public ImportRow poll(long timeout, TimeUnit unit)
		throws InterruptedException
	{
		if (stopped) return null;
		return super.poll(timeout, unit);
	}

	@Override
	public void put(ImportRow e)
		throws InterruptedException
	{
		if (!stopped) super.put(e);
	}

	@Override
	public int size()
	{
		if (stopped) return 0;
		return super.size();
	}

	@Override
	public ImportRow take()
		throws InterruptedException
	{
		if (stopped) return null;
		return super.take();
	}

}
