/*
 * ProfileListModel.java
 *
 * Created on 5. Juli 2002, 23:40
 */

package workbench.gui.profiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import workbench.db.ConnectionProfile;

/**
 *
 * @author  sql.workbench@freenet.de
 */
class ProfileListModel implements ListModel
{

	ArrayList profiles;
	/** Creates a new instance of ProfileListModel */
	public ProfileListModel(Map aProfileList)
	{
		this.profiles = new ArrayList(aProfileList.size());
		this.profiles.addAll(aProfileList.values());
		Collections.sort(this.profiles, ConnectionProfile.getNameComparator());
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
		return this.profiles.get(index);
	}

	/**
	 * Returns the length of the list.
	 * @return the length of the list
	 *
	 */
	public int getSize()
	{
		return this.profiles.size();
	}

	/** Removes a listener from the list that's notified each time a
	 * change to the data model occurs.
	 * @param l the <code>ListDataListener</code> to be removed
	 *
	 */
	public void removeListDataListener(ListDataListener l)
	{
	}

	public void addProfile(ConnectionProfile aProfile)
	{
		this.profiles.add(this.profiles.size(), aProfile);
	}

	public void deleteProfile(int index)
	{
		this.profiles.remove(index);
	}

	public void putProfile(int index, ConnectionProfile aProfile)
	{
		ConnectionProfile last = (ConnectionProfile)this.profiles.get(index);
		this.profiles.set(index, aProfile);
		ConnectionProfile newp = (ConnectionProfile)this.profiles.get(index);
		index = 1;
	}

	public Collection getValues()
	{
		return this.profiles;
	}
}

