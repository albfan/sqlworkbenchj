/*
 * ProfileListModel.java
 *
 * Created on 5. Juli 2002, 23:40
 */

package workbench.gui.profiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;

/**
 *
 * @author  sql.workbench@freenet.de
 */
class DriverListModel implements ListModel
{

	ArrayList drivers;
	/** Creates a new instance of ProfileListModel */
	public DriverListModel(List aDriverList)
	{
		this.drivers = new ArrayList(aDriverList.size());
		this.drivers.addAll(0, aDriverList);
		Collections.sort(this.drivers, DbDriver.getNameComparator());
	}

	/** Adds a listener to the list that's notified each time a change
	 * to the data model occurs.
	 * @param l the <code>ListDataListener</code> to be added
	 *
	 */
	public void addListDataListener(ListDataListener l)
	{
	}

	/** Returns the value at the specified index.
	 * @param index the requested index
	 * @return the value at <code>index</code>
	 *
	 */
	public Object getElementAt(int index)
	{
		return this.getDriver(index).getName();
	}

	public DbDriver getDriver(int index)
	{
		DbDriver drv = (DbDriver)this.drivers.get(index);
		return drv;
	}

	/**
	 * Returns the length of the list.
	 * @return the length of the list
	 *
	 */
	public int getSize()
	{
		return this.drivers.size();
	}

	/** Removes a listener from the list that's notified each time a
	 * change to the data model occurs.
	 * @param l the <code>ListDataListener</code> to be removed
	 *
	 */
	public void removeListDataListener(ListDataListener l)
	{
	}

	public void addDriver(DbDriver aDriver)
	{
		this.drivers.add(this.drivers.size(), aDriver);
	}

	public void deleteDriver(int index)
	{
		this.drivers.remove(index);
	}

	public void putDriver(int index, DbDriver aDriver)
	{
		DbDriver last = (DbDriver)this.drivers.get(index);
		this.drivers.set(index, aDriver);
		DbDriver newp = (DbDriver)this.drivers.get(index);
		index = 1;
	}

	public List getValues()
	{
		return this.drivers;
	}
}

