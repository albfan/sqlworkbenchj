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
import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import workbench.db.ConnectionProfile;

/**
 *
 * @author  workbench@kellerer.org
 */
class ProfileListModel 
	extends AbstractListModel
{

	private ArrayList profiles;
	
	/** Creates a new instance of ProfileListModel */
	public ProfileListModel(Map aProfileList)
	{
		this.profiles = new ArrayList();
		
		if (aProfileList != null)
		{
			this.profiles.addAll(aProfileList.values());
			Collections.sort(this.profiles, ConnectionProfile.getNameComparator());
		}
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
	
	public void profileChanged(ConnectionProfile aProfile)
	{
		int index = this.profiles.indexOf(aProfile);
		if (index >= 0)
		{
			this.fireContentsChanged(this, index, index);
		}
	}

	public void addProfile(ConnectionProfile aProfile)
	{
		this.profiles.add(this.profiles.size(), aProfile);
		this.fireIntervalAdded(this, this.profiles.size() - 1,  this.profiles.size() - 1);
	}

	public void deleteProfile(int index)
	{
		this.profiles.remove(index);
		this.fireIntervalRemoved(this, index, index);
	}

	public void putProfile(int index, ConnectionProfile aProfile)
	{
		this.profiles.set(index, aProfile);
	}

	public Collection getValues()
	{
		return this.profiles;
	}
	
	
}

