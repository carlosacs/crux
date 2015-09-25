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
package org.cruxframework.crux.core.client.dataprovider.pager;

import org.cruxframework.crux.core.client.dataprovider.AbstractHasPagedDataProvider;
import org.cruxframework.crux.core.client.dataprovider.DataChangedEvent;
import org.cruxframework.crux.core.client.dataprovider.DataChangedHandler;
import org.cruxframework.crux.core.client.dataprovider.DataFilterEvent;
import org.cruxframework.crux.core.client.dataprovider.DataFilterHandler;
import org.cruxframework.crux.core.client.dataprovider.DataLoadStoppedEvent;
import org.cruxframework.crux.core.client.dataprovider.DataLoadStoppedHandler;
import org.cruxframework.crux.core.client.dataprovider.DataProvider;
import org.cruxframework.crux.core.client.dataprovider.DataSortedEvent;
import org.cruxframework.crux.core.client.dataprovider.DataSortedHandler;
import org.cruxframework.crux.core.client.dataprovider.FilterableProvider;
import org.cruxframework.crux.core.client.dataprovider.PageLoadedEvent;
import org.cruxframework.crux.core.client.dataprovider.PageLoadedHandler;
import org.cruxframework.crux.core.client.dataprovider.TransactionEndEvent;
import org.cruxframework.crux.core.client.dataprovider.TransactionEndHandler;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Panel;

/**
 * Base implementation for Pageable widgets
 * @author Thiago da Rosa de Bustamante
 */
public abstract class AbstractPageable<T, P extends IsWidget> extends AbstractHasPagedDataProvider<T> implements Pageable<T>
{
	protected boolean allowRefreshAfterDataChange = true;
	protected HandlerRegistration dataChangedHandler;
	protected HandlerRegistration dataFilterHandler;
	protected HandlerRegistration dataSortedHandler;
	protected HasPageable<T> hasPageable;
	protected HandlerRegistration loadStoppedHandler;
	protected HandlerRegistration pageLoadedHandler;
	protected DataProvider.DataReader<T> reader = getDataReader();
	protected HandlerRegistration transactionEndHandler;
	private   P pagePanel;
	
	public void add(T object)
	{
		if (getDataProvider() != null)
		{
			getDataProvider().add(object);
		}
	}

	public void commit()
	{
		if (getDataProvider() != null)
		{
			getDataProvider().commit();
		}
	}

	public int indexOf(T object)
	{
		if (getDataProvider() != null)
		{
			return getDataProvider().indexOf(object);
		}
		return -1;
	}

	public boolean isDirty()
	{
		return getDataProvider() != null && getDataProvider().isDirty();
	}

	public void loadData()
	{
		if (getDataProvider() != null)
		{
			getDataProvider().load();
		}
	}

	public void refresh()
	{
		refresh(true);
	}
	
	public void remove(int index)
	{
		if (getDataProvider() != null)
		{
			getDataProvider().remove(index);
		}
	}
	
	public void reset()
	{
		reset(false);
	}

	public void reset(boolean reloadData)
	{
		if(getDataProvider() != null)
		{
			getDataProvider().reset();
		}

		if (reloadData)
		{
			loadData();
		}
	}
	
	public void rollback()
	{
		if (getDataProvider() != null)
		{
			getDataProvider().rollback();
		}
	}
	
	public void set(int index, T object)
	{
		if (getDataProvider() != null)
		{
			getDataProvider().set(index, object);
		}
	}

	@Override
	public void setHeight(String height) 
	{
		if(hasPageable != null && hasPageable.supportsInfiniteScroll())
		{
			hasPageable.asWidget().setHeight(height);
		} 
		else
		{
			super.setHeight(height);
		}
	}

	@Override
    public void setPager(HasPageable<T> pager)
    {
		this.hasPageable = pager;
		if (pager != null)
 		{
			if (getDataProvider() != null)
			{
				pager.setDataProvider(getDataProvider(), false);
			}
			pager.initializeContentPanel(getContentPanel());
			if (pager.supportsInfiniteScroll())
			{
				initializeAndUpdatePagePanel(true);
			}
			else if (getPagePanel() != null)
			{
				pager.updatePagePanel(getPagePanel(), true);
			}			
 		}
    }
	
	@Override
	protected void addDataProviderHandler()
	{
		pageLoadedHandler = getDataProvider().addPageLoadedHandler(new PageLoadedHandler() 
		{
			@Override
			public void onPageLoaded(final PageLoadedEvent event)
			{
				boolean renewPagePanel = hasPageable == null || !hasPageable.supportsInfiniteScroll();
				if (renewPagePanel)
				{
					final IsWidget pagePanel = (hasPageable != null)?doInitializePagePanel():null;
					
					if (getPagePanel() == null)
					{
						IsWidget panel = doInitializePagePanel();
						getContentPanel().add(panel);
					};
					
					render((hasPageable == null), new RenderCallback()
					{
						@Override
						public void onRendered()
						{
							if (hasPageable != null)
							{
								hasPageable.updatePagePanel(pagePanel, event.getCurrentPage() > event.getPreviousPage());
							}
						}
					});
				}
				else
				{
					render(false, null);
				}
			}
		});
		loadStoppedHandler = getDataProvider().addLoadStoppedHandler(new DataLoadStoppedHandler()
		{
			@Override
			public void onLoadStopped(DataLoadStoppedEvent event)
			{
				render(true, null);
			}
		});
		
		transactionEndHandler = getDataProvider().addTransactionEndHandler(new TransactionEndHandler()
		{
			@Override
			public void onTransactionEnd(TransactionEndEvent event)
			{
				onTransactionCompleted(event.isCommited());
			}
		});
				
		dataChangedHandler = getDataProvider().addDataChangedHandler(new DataChangedHandler()
		{
			@Override
			public void onDataChanged(DataChangedEvent event)
			{
				final int pageStartRecordOnTransactionEnd = getDataProvider().getCurrentPageStartRecord();
				if(allowRefreshAfterDataChange)
				{
					refreshPage(pageStartRecordOnTransactionEnd);
				}
			}
		});

		dataSortedHandler = getDataProvider().addDataSortedHandler(new DataSortedHandler()
		{
			@Override
			public void onSorted(DataSortedEvent event)
			{
				if (!event.isPageChanged())
				{
					final int pageStartRecordOnTransactionEnd = getDataProvider().getCurrentPageStartRecord();
					if(allowRefreshAfterDataChange)
					{
						refreshPage(pageStartRecordOnTransactionEnd);
					}
				}
			}
		});
				
		if (getDataProvider() instanceof FilterableProvider<?>)
		{
			@SuppressWarnings("unchecked")
			FilterableProvider<T> filterable = (FilterableProvider<T>) this.getDataProvider();
			dataFilterHandler = filterable.addDataFilterHandler(new DataFilterHandler<T>()
			{
				@Override
				public void onFiltered(DataFilterEvent<T> event)
				{
					render(true, null);
				}
			});
		}
	}
	
	protected abstract void clear();
	
	protected abstract void clearRange(int startRecord);

	protected abstract Panel getContentPanel();

	protected abstract DataProvider.DataReader<T> getDataReader();

	/**
	* Retrieve the panel that will contain all the page data.
	* @return
	*/
	protected final P getPagePanel()
	{
		return pagePanel;
	}

	protected void initializeAndUpdatePagePanel(boolean forward)
	{
		IsWidget pagePanel = doInitializePagePanel();
		if (hasPageable != null)
	    {
	     	hasPageable.updatePagePanel(pagePanel, forward);
		}
	}

	/**
	* Creates the panel that will contain all the page data.
	* @return
	*/
	protected abstract P initializePagePanel();
	
	/**
	 * @return a Page Panel instance. 
	 */
	private IsWidget doInitializePagePanel()
	{
		pagePanel = initializePagePanel();
		return pagePanel;
	}
	
	/**
	 * @return true if is allowed to refresh the page after the data 
	 * provider changes some of their values. This generally happens after
	 * a commit.
	 */
	protected boolean isAllowRefreshAfterDataChange() 
	{
		return allowRefreshAfterDataChange;
	}

	@Override
	protected void onDataProviderSet()
	{
		if (getPagePanel() == null)
		{
			IsWidget panel = doInitializePagePanel();
			getContentPanel().add(panel);
		}
		getDataProvider().first();
		render(true, null);		
	}
		
	protected void onTransactionCompleted(boolean commited)
    {
		final int pageStartRecordOnTransactionEnd = getDataProvider().getCurrentPageStartRecord();
		Scheduler.get().scheduleDeferred(new ScheduledCommand()
		{
			@Override
			public void execute()
			{
				refreshPage(pageStartRecordOnTransactionEnd);
			}
		});
    }
	
	protected void refresh(boolean goToFirstPage)
	{
		if (goToFirstPage)
		{
			getDataProvider().first();
		}
		else
		{
			getDataProvider().firstOnPage();
		}
		render(goToFirstPage, null);		
	}
	
	protected void refreshPage(int startRecord)
    {
		boolean refreshAll = hasPageable == null || !hasPageable.supportsInfiniteScroll();
	    if (refreshAll)
	    {
	    	clear();
	    }
	    else
	    {
	    	clearRange(startRecord);
	    }
	    refresh(false);
    }
		
	@Override
	protected void removeDataProviderHandler()
	{
		if (transactionEndHandler != null)
		{
			transactionEndHandler.removeHandler();
			transactionEndHandler = null;
		}
		if (loadStoppedHandler != null)
		{
			loadStoppedHandler.removeHandler();
			loadStoppedHandler = null;
		}
		if (dataChangedHandler != null)
		{
			dataChangedHandler.removeHandler();
			dataChangedHandler = null;
		}
		if (dataFilterHandler != null)
		{
			dataFilterHandler.removeHandler();
			dataFilterHandler = null;
		}
		if (pageLoadedHandler != null)
		{
			pageLoadedHandler.removeHandler();
			pageLoadedHandler = null;
		}
		if (dataSortedHandler != null)
		{
			dataSortedHandler.removeHandler();
			dataSortedHandler = null;
		}
	}
		
	protected void render(boolean refresh, RenderCallback callback)
    {
		if (refresh)
		{
			clear();
		}
		int rowCount = getRowsToBeRendered();

		for (int i=0; i<rowCount; i++)
		{
			getDataProvider().read(reader);
			if (getDataProvider().hasNext())
			{
				getDataProvider().next();
			}
			else
			{
				break;
			}
		}
		if (callback != null)
		{
			callback.onRendered();
		}
    }
	
	/**
	 * @param allowRefreshAfterDataChange indicate if is allowed to 
	 * refresh the page after the data provider changes some of their values. 
	 * This generally happens after a commit. 
	 */
	protected void setAllowRefreshAfterDataChange(boolean allowRefreshAfterDataChange) 
	{
		this.allowRefreshAfterDataChange = allowRefreshAfterDataChange;
	} 
	
	private int getRowsToBeRendered()
	{
		if(isDataLoaded())
		{
			if(getDataProvider().getCurrentPage() == 0)
			{
				getDataProvider().nextPage();
			}

			return getDataProvider().getCurrentPageSize();
		}

		return 0;
	}

	protected static interface RenderCallback
	{
		void onRendered();
	}
}
