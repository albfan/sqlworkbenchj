/*
 * RowDataProducer.java
 *
 * Created on December 20, 2003, 10:20 AM
 */

package workbench.db.importer;

import java.util.List;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface RowDataProducer
{
	void setReceiver(RowDataReceiver receiver);
	void start() throws Exception;
	void cancel();
	String getMessages();
}
