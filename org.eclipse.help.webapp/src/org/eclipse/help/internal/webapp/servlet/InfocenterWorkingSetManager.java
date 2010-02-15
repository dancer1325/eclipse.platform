/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.internal.webapp.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.criteria.CriterionResource;
import org.eclipse.help.internal.util.URLCoder;
import org.eclipse.help.internal.webapp.HelpWebappPlugin;
import org.eclipse.help.internal.workingset.AdaptableHelpResource;
import org.eclipse.help.internal.workingset.AdaptableToc;
import org.eclipse.help.internal.workingset.AdaptableTocsArray;
import org.eclipse.help.internal.workingset.AdaptableTopic;
import org.eclipse.help.internal.workingset.IHelpWorkingSetManager;
import org.eclipse.help.internal.workingset.WorkingSet;
import org.eclipse.help.internal.workingset.WorkingSetComparator;

/**
 * The Infocenter working set manager stores help working sets. Working sets are
 * persisted in client cookies whenever one is added or removed.
 * 
 * @since 3.0
 */
public class InfocenterWorkingSetManager implements IHelpWorkingSetManager {
	private static final String COOKIE_WSET_CONTENTS = "wset_contents"; //$NON-NLS-1$
	private static final String COOKIE_WSET_CRITERIA = "wset_criteria"; //$NON-NLS-1$
	private static final int MAX_COOKIES = 15;
	private HttpServletRequest request;
	private HttpServletResponse response;

	// Current working set , empty string means all documents
	private String currentWorkingSet = ""; //$NON-NLS-1$
	private SortedSet workingSets = new TreeSet(new WorkingSetComparator());
	private String locale;
	private AdaptableTocsArray root;
	
	private static final String UNCATEGORIZED = "Uncategorized"; //$NON-NLS-1$
	private Map allCriteriaValues;

	/**
	 * Constructor
	 * 
	 * @param locale
	 */
	public InfocenterWorkingSetManager(HttpServletRequest request,
			HttpServletResponse response, String locale) {
		this.request = request;
		this.response = response;
		this.locale = locale;
		restoreState();
	}

	public AdaptableTocsArray getRoot() {
		if (root == null)
			root = new AdaptableTocsArray(HelpPlugin.getTocManager().getTocs(
					locale));
		return root;
	}

	/**
	 * Adds a new working set and saves it
	 */
	public void addWorkingSet(WorkingSet workingSet) throws IOException {
		if (workingSet == null || workingSets.contains(workingSet))
			return;
		workingSets.add(workingSet);
		saveState();
	}

	/**
	 * Creates a new working set
	 */
	public WorkingSet createWorkingSet(String name,
			AdaptableHelpResource[] elements) {
		return new WorkingSet(name, elements);
	}

	public WorkingSet createWorkingSet(String name, AdaptableHelpResource[] elements, CriterionResource[] criteria) {
		return new WorkingSet(name, elements, criteria);
	}
	
	/**
	 * Returns a working set by name
	 *  
	 */
	public WorkingSet getWorkingSet(String name) {
		if (name == null || workingSets == null)
			return null;

		Iterator iter = workingSets.iterator();
		while (iter.hasNext()) {
			WorkingSet workingSet = (WorkingSet) iter.next();
			if (name.equals(workingSet.getName()))
				return workingSet;
		}
		return null;
	}

	/**
	 * Implements IWorkingSetManager.
	 * 
	 * @see org.eclipse.help.internal.workingset.IHelpWorkingSetManager#getWorkingSets()
	 */
	public WorkingSet[] getWorkingSets() {
		return (WorkingSet[]) workingSets.toArray(new WorkingSet[workingSets
				.size()]);
	}

	/**
	 * Removes specified working set
	 */
	public void removeWorkingSet(WorkingSet workingSet) {
		workingSets.remove(workingSet);
		try {
			saveState();
		} catch (IOException ioe) {
		}
	}

	private void restoreState() {		
		restoreContents();
		restoreCriteria();
	}
	
	private void restoreContents(){
		String data = CookieUtil.restoreString(COOKIE_WSET_CONTENTS, request);
		if (data == null) {
			return;
		}

		String[] values = data.split("\\|", -1); //$NON-NLS-1$
		if (values.length < 1) {
			return;
		}
		
		currentWorkingSet = URLCoder.decode(values[0]);
		i : for (int i = 1; i < values.length; i++) {
			String[] nameAndHrefs = values[i].split("&", -1); //$NON-NLS-1$

			String name = URLCoder.decode(nameAndHrefs[0]);

			AdaptableHelpResource[] elements = new AdaptableHelpResource[nameAndHrefs.length - 1];
			// for each href (working set resource)
			for (int e = 0; e < nameAndHrefs.length - 1; e++) {
				int h = e + 1;
				elements[e] = getAdaptableToc(URLCoder.decode(nameAndHrefs[h]));
				if (elements[e] == null) {
					elements[e] = getAdaptableTopic(URLCoder.decode(nameAndHrefs[h]));
				}
				if (elements[e] == null) {
					// working set cannot be restored
					continue i;
				}
			}
			WorkingSet ws = createWorkingSet(name, elements, null);
			workingSets.add(ws);
		}
	}
	
	private void restoreCriteria(){
		
		String data = CookieUtil.restoreString(COOKIE_WSET_CRITERIA, request);
		if (data == null) {
			return;
		}
		String[] values = data.split("\\|", -1); //$NON-NLS-1$
		if (values.length < 1) {
			return;
		}
		//scope1$platform#AIX,WINDOWS,$version#1.0,2.0,
		for (int i = 1; i < values.length; ++i) {
			String[] nameAndCriteria = values[i].split("\\$", -1); //$NON-NLS-1$
			if(nameAndCriteria.length < 2){
				continue;
			}
			String name = URLCoder.decode(nameAndCriteria[0]);
		    List criteriaResource = new ArrayList();
			for (int j = 1; j < nameAndCriteria.length; ++j) {
				String criterion = nameAndCriteria[j];
				String[] keyAndValue = criterion.split("#", -1); //$NON-NLS-1$
				if(keyAndValue.length != 2)
					continue;
				String key = URLCoder.decode(keyAndValue[0]);
			    String value = URLCoder.decode(keyAndValue[1]);
				String[] criterionValues = value.split(",", -1); //$NON-NLS-1$
				if(criterionValues.length < 1)
					continue;

				List criterionValuesList = Arrays.asList(criterionValues);
				CriterionResource criterionResource = new CriterionResource(key, criterionValuesList);
				criteriaResource.add(criterionResource);

			}
			
			WorkingSet workingset = getWorkingSet(name);
			if(workingset != null){
				CriterionResource[] criteria = new CriterionResource[criteriaResource.size()];
				criteriaResource.toArray(criteria);
				workingset.setCriteria(criteria);
			}
		}
	}

	/***************************************************************************
	 * Persists all working sets. Should only be called by the webapp working
	 * set dialog. Saves the working sets in the persistence store (cookie)
	 * format: curentWorkingSetName|name1&href11&href12|name2&href22
	 */
	private void saveState() throws IOException {
		saveContents();
		saveCriteria();
	}
	
	private void saveContents() throws IOException {
		
		StringBuffer data = new StringBuffer();
		data.append(URLCoder.encode(currentWorkingSet /* , "UTF8" */
		));

		for (Iterator i = workingSets.iterator(); i.hasNext();) {
			data.append('|');
			WorkingSet ws = (WorkingSet) i.next();
			data.append(URLCoder.encode(ws.getName() /* , "UTF8" */
			));

			AdaptableHelpResource[] resources = ws.getElements();
			for (int j = 0; j < resources.length; j++) {
				data.append('&');

				IAdaptable parent = resources[j].getParent();
				if (parent == getRoot()) {
					// saving toc
					data.append(URLCoder.encode(resources[j].getHref()
					/* , "UTF8" */
					));
				} else {
					// saving topic as tochref_topic#_
					AdaptableToc toc = (AdaptableToc) parent;
					AdaptableHelpResource[] siblings = (toc).getChildren();
					for (int t = 0; t < siblings.length; t++) {
						if (siblings[t] == resources[j]) {
							data.append(URLCoder.encode(toc.getHref()
							/* , "UTF8" */
							));
							data.append('_');
							data.append(t);
							data.append('_');
							break;
						}
					}
				}
			}
		}
		
		saveToCookie(COOKIE_WSET_CONTENTS, data.toString());
	}
	
	private void saveCriteria() throws IOException {
		
		StringBuffer data = new StringBuffer();
		data.append(URLCoder.encode(currentWorkingSet));
		//|scope1$platform#AIX,WINDOWS,$version#1.0,2.0,
		for (Iterator i = workingSets.iterator(); i.hasNext();) {
			data.append('|');
			WorkingSet ws = (WorkingSet) i.next();
			data.append(URLCoder.encode(ws.getName()));

			CriterionResource[] criteria = ws.getCriteria();
			for (int j = 0; j < criteria.length; ++ j){
				CriterionResource criterion = criteria[j];
				String criterionName = criterion.getCriterionName();
				List criterionValues = criterion.getCriterionValues();
				if(null != criterionValues && !criterionValues.isEmpty()){
					data.append('$');
					data.append(URLCoder.encode(criterionName));
					data.append('#');
					for (Iterator iter = criterionValues.iterator(); iter.hasNext();) {
						String value = (String) iter.next();
						data.append(URLCoder.encode(value));
						data.append(',');
					}
				}
			}	
		}
		
		saveToCookie(COOKIE_WSET_CRITERIA, data.toString());
	}
	
	private void saveToCookie(String name, String data) throws IOException{
		
		try {
			CookieUtil.saveString(name, data, MAX_COOKIES, request, response);
		} catch (IOException ioe) {
			if (HelpWebappPlugin.DEBUG_WORKINGSETS) {
				String msg = "InfocenterWorkingSetManager.saveState(): Too much data to save: " + data; //$NON-NLS-1$
				System.out.println(msg);
			}
			throw ioe;
		}
	}
	

	/**
	 * *
	 * 
	 * @param changedWorkingSet
	 *            the working set that has changed
	 */
	public void workingSetChanged(WorkingSet changedWorkingSet)
			throws IOException {
		saveState();
	}

	public AdaptableToc getAdaptableToc(String href) {
		return getRoot().getAdaptableToc(href);
	}

	public AdaptableTopic getAdaptableTopic(String id) {

		if (id == null || id.length() == 0)
			return null;

		// toc id's are hrefs: /pluginId/path/to/toc.xml
		// topic id's are based on parent toc id and index of topic:
		// /pluginId/path/to/toc.xml_index_
		int len = id.length();
		if (id.charAt(len - 1) == '_') {
			// This is a first level topic
			String indexStr = id.substring(id.lastIndexOf('_', len - 2) + 1,
					len - 1);
			int index = 0;
			try {
				index = Integer.parseInt(indexStr);
			} catch (Exception e) {
			}

			String tocStr = id.substring(0, id.lastIndexOf('_', len - 2));
			AdaptableToc toc = getAdaptableToc(tocStr);
			if (toc == null)
				return null;
			IAdaptable[] topics = toc.getChildren();
			if (index < 0 || index >= topics.length)
				return null;
			return (AdaptableTopic) topics[index];
		}

		return null;
	}

	public String getCurrentWorkingSet() {
		return currentWorkingSet;
	}

	public void setCurrentWorkingSet(String workingSet) {
		currentWorkingSet = workingSet;
		try {
			saveState();
		} catch (IOException ioe) {
		}
	}

	public boolean isCriteriaScopeEnabled(){
		if(null == allCriteriaValues){
			allCriteriaValues = HelpPlugin.getCriteriaManager().getAllCriteriaValues(locale);
		}
		if(HelpPlugin.getCriteriaManager().isCriteriaEnabled() && !allCriteriaValues.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}
	
	public String[] getCriterionIds() {
		if(null == allCriteriaValues){
			allCriteriaValues = HelpPlugin.getCriteriaManager().getAllCriteriaValues(locale);
		}
		List criterionIds = new ArrayList();
		if(null != allCriteriaValues){
			for(Iterator iter = allCriteriaValues.keySet().iterator(); iter.hasNext();){
				String criterion = (String) iter.next();
				if(null == criterion || 0 == criterion.length() || 0 == getCriterionValueIds(criterion).length)
					continue;
				criterionIds.add(criterion);
			}
			Collections.sort(criterionIds);
		}
		String[] ids = new String[criterionIds.size()];                                        		
		criterionIds.toArray(ids);
		return ids;
	}
	

	public String[] getCriterionValueIds(String criterionName) {
		if(null == allCriteriaValues){
			allCriteriaValues = HelpPlugin.getCriteriaManager().getAllCriteriaValues(locale);
		}
		List valueIds = new ArrayList();
		if(null != criterionName && null != allCriteriaValues) {
			Set criterionValues = (Set)allCriteriaValues.get(criterionName);
			if(null != criterionValues && !criterionValues.isEmpty()) {
				valueIds.addAll(criterionValues);
				Collections.sort(valueIds);
				valueIds.add(UNCATEGORIZED);
			}
		}
		String[] valueIdsArray = new String[valueIds.size()];                                        		
		valueIds.toArray(valueIdsArray);
		return valueIdsArray;
	}
	

	public String getCriterionDisplayName(String criterionId) {
		return HelpPlugin.getCriteriaManager().getCriterionDisplayName(criterionId, locale);
	}

	public String getCriterionValueDisplayName(String criterionId, String criterionValueId) {
		return HelpPlugin.getCriteriaManager().getCriterionValueDisplayName(criterionId, criterionValueId, locale);
	}

}
