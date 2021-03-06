/*
 * Copyright 2011 cruxframework.org.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.cruxframework.crux.core.rebind.screen;

 

/**
 * Represents a Crux Screen at the application's server side. Used for GWT Generators.
 * 
 * @author Thiago Bustamante
 */
public class Screen 
{
	protected String id;
	protected String module;
	protected View rootView;
	private long lastModified;
	
	public Screen(String id, String module, View rootView) 
	{
		this.id = id;
		this.module = module;
		this.rootView = rootView;
	}

	/**
	 * Return screen identifier
	 * @return
	 */
	public String getId() 
	{
		return id;
	}
	
	public long getLastModified()
	{
		return lastModified;
	}

	/**
	 * @return
	 */
	public String getModule()
	{
		return module;
	}

	/**
	 * 
	 * @return
	 */
	public View getRootView()
    {
    	return rootView;
    }

	void setLastModified(long lastModified)
    {
		this.lastModified = lastModified;
    }
}
