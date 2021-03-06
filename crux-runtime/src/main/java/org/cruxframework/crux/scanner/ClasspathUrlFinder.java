/*
 * Copyright 2014 cruxframework.org.
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
package org.cruxframework.crux.scanner;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Various functions to locate URLs to scan
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClasspathUrlFinder
{

	/**
	 * Find the classpath URLs for a specific classpath resource.  The classpath URL is extracted
	 * from loader.getResources() using the baseResource.
	 *
	 * @param baseResource
	 * @return
	 */
	public static URL[] findResourceBases(String baseResource, ClassLoader loader)
	{
		ArrayList<URL> list = new ArrayList<URL>();
		try
		{
			Enumeration<URL> urls = loader.getResources(baseResource);
			while (urls.hasMoreElements())
			{
				URL url = urls.nextElement();
				list.add(findResourceBase(url, baseResource));
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		return list.toArray(new URL[list.size()]);
	}

	/**
	 * Find the classpath URLs for a specific classpath resource.  The classpath URL is extracted
	 * from loader.getResources() using the baseResource.
	 *
	 * @param baseResource
	 * @return
	 */
	public static URL[] findResourceBases(String baseResource)
	{
		return findResourceBases(baseResource, Thread.currentThread().getContextClassLoader());
	}

	private static URL findResourceBase(URL url, String baseResource)
	{
		String urlString = url.toString();
		int idx = urlString.lastIndexOf(baseResource);
		urlString = urlString.substring(0, idx);
		URL deployUrl = null;
		try
		{
			deployUrl = new URL(urlString);
		}
		catch (MalformedURLException e)
		{
			throw new RuntimeException(e);
		}
		return deployUrl;
	}

	/**
	 * Find the classpath URL for a specific classpath resource.  The classpath URL is extracted
	 * from Thread.currentThread().getContextClassLoader().getResource() using the baseResource.
	 *
	 * @param baseResource
	 * @return
	 */
	public static URL findResourceBase(String baseResource)
	{
		return findResourceBase(baseResource, Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Find the classpath URL for a specific classpath resource.  The classpath URL is extracted
	 * from loader.getResource() using the baseResource.
	 *
	 * @param baseResource
	 * @param loader
	 * @return
	 */
	public static URL findResourceBase(String baseResource, ClassLoader loader)
	{
		URL url = loader.getResource(baseResource);
		return findResourceBase(url, baseResource);
	}

	/**
	 * Find the classpath for the particular class
	 *
	 * @param clazz
	 * @return
	 */
	public static URL findClassBase(Class<?> clazz)
	{
		String resource = clazz.getName().replace('.', '/') + ".class";
		return findResourceBase(resource, clazz.getClassLoader());
	}

	/**
	 * Uses the java.class.path system property to obtain a list of URLs that represent the CLASSPATH
	 *
	 * @return
	 */
	public static URL[] findClassPaths()
	{
		List<URL> list = new ArrayList<URL>();
		String classpath = System.getProperty("java.class.path");
		StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);

		while (tokenizer.hasMoreTokens())
		{
			String path = tokenizer.nextToken();
			File fp = new File(path);
			if (!fp.exists()) throw new RuntimeException("File in java.class.path does not exist: " + fp);
			try
			{
				list.add(fp.toURI().toURL());
			}
			catch (MalformedURLException e)
			{
				throw new RuntimeException(e);
			}
		}
		return list.toArray(new URL[list.size()]);
	}

	/**
	 * Search on every classpath entry the informed config file and loads all of them into the given property file
	 * @param propertyFile the {@link Properties} File to be loaded
	 * @param configFileName the name of the .properties file
	 * @throws Exception
	 */
	public static void loadFromConfigFiles(Properties propertyFile, String configFileName) throws Exception
	{
		Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(configFileName);
		
		while (resources.hasMoreElements())
		{
			URL url = resources.nextElement();
			URLStreamManager streamManager = new URLStreamManager(url);
			propertyFile.load(streamManager.open());
			streamManager.close();
		}
	}
}

