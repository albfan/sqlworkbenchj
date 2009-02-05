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

import java.sql.SQLException;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.WbThread;

/**
 *
 * @author support@sql-workbench.net
 */
public class WorkerPool
{
	private ImportWorker[] workers;
	private Thread[] threadPool;


	public WorkerPool(WbConnection base, int numWorkers)
		throws SQLException
	{
		if (numWorkers < 1) numWorkers = 1;
		workers = new ImportWorker[numWorkers];
		for (int i=0; i < workers.length; i++)
		{
			workers[i] = new ImportWorker(base, i);
		}
	}

	public boolean isActive()
	{
		return (threadPool != null);
	}


	public void commit()
		throws SQLException
	{
		for (ImportWorker worker : workers)
		{
			worker.commit();
		}
	}

	public void rollback()
		throws SQLException
	{
		for (ImportWorker worker : workers)
		{
			worker.rollback();
		}
	}

	public void done()
		throws SQLException
	{
		for (ImportWorker worker : workers)
		{
			worker.done();
		}
	}

	public void init(MultiThreadedDataImporter imp)
	{
		for (ImportWorker worker : workers)
		{
			worker.setController(imp);
		}
	}
	
	public void setInsertSql(String sql)
		throws SQLException
	{
		for (ImportWorker worker : workers)
		{
			worker.setInsertSql(sql);
		}
	}

	public void setUpdateSql(String sql)
		throws SQLException
	{
		for (ImportWorker worker : workers)
		{
			worker.setUpdateSql(sql);
		}
	}
	
	public void start()
		throws SQLException
	{
		threadPool = new Thread[workers.length];
		LogMgr.logDebug("WorkerPool.start()", "Starting " + workers.length + " threads");
		for (int i=0; i < workers.length; i++)
		{
			threadPool[i] = new WbThread(workers[i], "ImportThread " + i);
			threadPool[i].start();
		}
	}

	public synchronized void dispose()
	{
		for (ImportWorker worker : workers)
		{
			worker.stop();
		}

		if (threadPool != null)
		{
			for (Thread thread : threadPool)
			{
				thread.interrupt();
			}
		}
		
		for (ImportWorker worker : workers)
		{
			worker.dispose();
		}
	}

	public void cancel()
	{
		for (ImportWorker worker : workers)
		{
			worker.cancel();
		}
	}

	public void finishTable()
		throws SQLException
	{
		for (ImportWorker worker : workers)
		{
			worker.flush();
		}

	}
	public long getUpdatedRows()
	{
		long rows = 0;
		for (ImportWorker worker : workers)
		{
			rows += worker.getUpdatedRows();
		}
		return rows;
	}

	public long getInsertedRows()
	{
		long rows = 0;
		for (ImportWorker worker : workers)
		{
			long wr = worker.getInsertedRows();
//			System.out.println("Worker " + worker.toString() + " had " + wr + " inserted rows");
			rows += wr;
		}
		return rows;
	}

	public long getTotalRows()
	{
		long rows = 0;
		for (ImportWorker worker : workers)
		{
			rows += worker.getTotalRows();
		}
		return rows;
	}

	public boolean hasErrors()
	{
		for (ImportWorker worker : workers)
		{
			if (worker.hasErrors()) return true;
		}
		return false;
	}

}
