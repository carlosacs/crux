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
package org.cruxframework.crux.core.rebind;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cruxframework.crux.core.rebind.provider.ScreenContextProvider;
import org.cruxframework.crux.core.rebind.screen.Screen;
import org.cruxframework.crux.core.rebind.screen.ScreenConfigException;
import org.cruxframework.crux.core.rebind.screen.ScreenFactory;
import org.cruxframework.crux.core.rebind.screen.View;
import org.cruxframework.crux.core.server.CruxBridge;
import org.cruxframework.crux.core.server.development.ViewTesterScreen;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;

/**
 * 
 * Base class for all generators that create a smart stub for a base interface
 * @author Thiago da Rosa de Bustamante
 *
 */
public abstract class AbstractInterfaceWrapperProxyCreator extends AbstractProxyCreator
{
	private static final String PROXY_SUFFIX = "_Impl";
	protected JClassType baseIntf;
	private ScreenFactory screenFactory;

	public AbstractInterfaceWrapperProxyCreator(TreeLogger logger, GeneratorContext context, JClassType baseIntf, boolean cacheable)
    {
	    super(logger, context, cacheable);
		this.baseIntf = baseIntf;
		ScreenContextProvider screenProvider = new ScreenContextProvider(context);
		this.screenFactory = new ScreenFactory(screenProvider);
    }

	/**
	 * @return the full qualified name of the proxy object.
	 */
	@Override
	public String getProxyQualifiedName()
	{
		return baseIntf.getPackage().getName() + "." + getProxySimpleName();
	}
	
	/**
	 * @return the simple name of the proxy object.
	 */
	@Override
	public String getProxySimpleName()
	{
		JClassType enclosingType = baseIntf.getEnclosingType();
		String enclosingTypeName = (enclosingType==null?"":enclosingType.getSimpleSourceName()+"_");
		return enclosingTypeName+baseIntf.getSimpleSourceName() + PROXY_SUFFIX;
	}

	/**
	 * 
	 * @return
	 */
	protected String getUserAgent()
	{
		try
		{
			SelectionProperty userAgent = context.getPropertyOracle().getSelectionProperty(logger, "user.agent");
			return userAgent==null?null:userAgent.getCurrentValue();
		}
		catch (BadPropertyValueException e)
		{
			logger.log(TreeLogger.ERROR, "Can not read user.agent property.",e);
			throw new CruxGeneratorException();
		}
	}
	
	/**
	 * 
	 * @return
	 */
	protected String getDeviceFeatures()
	{
		try
		{
			SelectionProperty device = context.getPropertyOracle().getSelectionProperty(logger, "device.features");
			return device==null?null:device.getCurrentValue();
		}
		catch (BadPropertyValueException e)
		{
			throw new CruxGeneratorException("Can not read device.features property.", e);
		}
	}
	
	protected String getModule()
	{
		try
		{
			if (ViewTesterScreen.isTestViewScreen())
			{
				return ViewTesterScreen.getModuleForViewTesting();
			}
			else
			{
				String screenID = CruxBridge.getInstance().getLastPageRequested();
				Screen requestedScreen = screenFactory.getScreen(screenID, getDeviceFeatures());
				if(requestedScreen != null)
				{
					return requestedScreen.getModule();
				}
			}
			return null;
		}
		catch (ScreenConfigException e)
		{
			logger.log(TreeLogger.ERROR, "Error Generating registered element. Can not retrieve current module.",e);
			throw new CruxGeneratorException();
        }
	}
	
	/**
	 * 
	 * @param logger
	 * @return
	 * @throws CruxGeneratorException
	 */
	protected List<Screen> getScreens() throws CruxGeneratorException
	{
		try
        {
	        List<Screen> screens = new ArrayList<Screen>();

	        String module = getModule();
	        
	        if(module != null)
	        {
	        	Set<String> screenIDs = screenFactory.getScreens(module);
	        	
	        	if (screenIDs == null)
	        	{
	        		throw new ScreenConfigException("Can not find the module ["+module+"].");
	        	}
	        	for (String screenID : screenIDs)
	        	{
	        		Screen screen = screenFactory.getScreen(screenID, getDeviceFeatures());
	        		if(screen != null)
	        		{
	        			screens.add(screen);
	        		}
	        	}
	        }
	        
	        return screens;
        }
        catch (ScreenConfigException e)
        {
			logger.log(TreeLogger.ERROR, "Error Generating registered element. Can not retrieve module's list of screens.",e);
			throw new CruxGeneratorException();
        }
	}

	/**
	 * 
	 * @return
	 */
	protected List<View> getViews()
	{
		List<View> views = new ArrayList<View>();
		if (ViewTesterScreen.isTestViewScreen())
		{
			try
			{
				String moduleId = getModule();
				List<String> viewList = screenFactory.getViewFactory().getViews("*", moduleId);
				for (String viewName : viewList)
				{
					View innerView = screenFactory.getViewFactory().getView(viewName, getDeviceFeatures());
					if (innerView != null)
					{
						views.add(innerView);
					}
				}
			}
			catch (ScreenConfigException e)
			{
				logger.log(TreeLogger.ERROR, "Error Generating registered element. Can not retrieve list of views.",e);
				throw new CruxGeneratorException();
			}
			
		}
		else
		{
			List<Screen> screens = getScreens();
			HashSet<String> added = new HashSet<String>();
			for (Screen screen : screens)
			{
				findViews(screen, views, added);
			}
		}
		return views;
	}
		
	/**
	 * @return a sourceWriter for the proxy class
	 */
	@Override
	protected SourcePrinter getSourcePrinter()
	{
		JPackage pkg = baseIntf.getPackage();
		String packageName = pkg == null ? "" : pkg.getName();
		PrintWriter printWriter = context.tryCreate(logger, packageName, getProxySimpleName());

		if (printWriter == null)
		{
			return null;
		}

		ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(packageName, getProxySimpleName());

		String[] imports = getImports();
		for (String imp : imports)
		{
			composerFactory.addImport(imp);
		}

		composerFactory.addImplementedInterface(baseIntf.getQualifiedSourceName());

		return new SourcePrinter(composerFactory.createSourceWriter(context, printWriter), logger);
	}
	
	/**
	 * @return
	 */
	protected boolean findCacheableImplementationAndMarkForReuseIfAvailable()
	{
		return findCacheableImplementationAndMarkForReuseIfAvailable(baseIntf);
	}
	
	/**
	 * 
	 * @param screen
	 * @param views
	 * @param added
	 */
	private void findViews(Screen screen, List<View> views, Set<String> added) 
	{
		View rootView = screen.getRootView();
		if (!added.contains(rootView.getId()))
		{
			added.add(rootView.getId());
			views.add(rootView);
			findViews(rootView, views, added, screen.getModule());
		}
	}
	
	/**
	 * 
	 * @param view
	 * @param views
	 * @param added
	 */
	private void findViews(View view, List<View> views, Set<String> added, String moduleId) 
	{
		try
		{
			Iterator<String> iterator = view.iterateViews();
			while (iterator.hasNext())
			{
				String viewLocator = iterator.next();
				if (!added.contains(viewLocator))
				{
					added.add(viewLocator);
					
					List<String> viewList = screenFactory.getViewFactory().getViews(viewLocator, moduleId);
					for (String viewName : viewList)
                    {
						View innerView = screenFactory.getViewFactory().getView(viewName, getDeviceFeatures());
						if (innerView != null)
						{
							views.add(innerView);
							findViews(innerView, views, added, moduleId);
						}
                    }
				}
			}
		}
		catch (ScreenConfigException e)
		{
			logger.log(TreeLogger.ERROR, "Error Generating registered element. Can not retrieve screen's list of views.",e);
			throw new CruxGeneratorException();
		}
	}
	
	/**
	 * @return the list of imports required by proxy
	 */
	protected abstract String[] getImports();
}
