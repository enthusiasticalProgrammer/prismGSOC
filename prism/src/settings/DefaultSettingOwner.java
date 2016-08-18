//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package settings;

import java.util.*;

/**
 * DefaultSettingOwners do not have a class and therefore can not be multiple
 */
public class DefaultSettingOwner extends Observable implements SettingOwner
{
	private String name;
	private int id;
	private SettingDisplay display;
	private ArrayList<Setting> settings;

	/** Creates a new instance of DefaultSettingOwner */
	public DefaultSettingOwner(String name, int id)
	{
		this.name = name;
		this.id = id;
		display = null;
		settings = new ArrayList<>();
	}

	@Override
	public int compareTo(SettingOwner po)
	{
			if (getSettingOwnerID() < po.getSettingOwnerID())
				return -1;
			else if (getSettingOwnerID() > po.getSettingOwnerID())
				return 1;
			else
				return 0;
	}

	@Override
	public SettingDisplay getDisplay()
	{
		return display;
	}

	@Override
	public int getNumSettings()
	{
		return settings.size();
	}

	@Override
	public Setting getSetting(int index)
	{
		return settings.get(index);
	}

	@Override
	public String getSettingOwnerClassName()
	{
		return "";
	}

	@Override
	public int getSettingOwnerID()
	{
		return id;
	}

	@Override
	public String getSettingOwnerName()
	{
		return name;
	}

	@Override
	public void notifySettingChanged(Setting setting)
	{
		setChanged();
		notifyObservers(setting);
	}

	@Override
	public void setDisplay(SettingDisplay display)
	{
		this.display = display;
	}

	public void addSetting(Setting s)
	{
		settings.add(s);
	}

	public Setting getFromKey(String key)
	{
		for (int i = 0; i < getNumSettings(); i++) {
			if (getSetting(i).equals(key))
				return getSetting(i);
		}
		return null;
	}
}
