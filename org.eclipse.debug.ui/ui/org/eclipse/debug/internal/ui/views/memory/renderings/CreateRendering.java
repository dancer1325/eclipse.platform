/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory.renderings;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.views.memory.MemoryViewUtil;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.memory.AbstractMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingBindingsListener;
import org.eclipse.debug.ui.memory.IMemoryRenderingContainer;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * The rendering to allow users to create a rendering.
 * @since 3.1
 */
public class CreateRendering extends AbstractMemoryRendering implements IMemoryRenderingBindingsListener {

	private ListViewer fViewer;
	private Label fMemoryBlockLabel;
	private IMemoryRenderingContainer fContainer; 
	private Composite fCanvas;
	
	public CreateRendering(IMemoryRenderingContainer container)
	{
		super("org.eclipse.debug.internal.ui.views.createrendering"); //$NON-NLS-1$
		fContainer = container;
	}

	class MemoryRenderingLabelProvider implements ILabelProvider
	{
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if (element instanceof IMemoryRenderingType)
			{	
				String label = ((IMemoryRenderingType)element).getLabel();
				return label;
			}
            return element.toString();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void addListener(ILabelProviderListener listener) {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
		 */
		public void dispose() {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
		 */
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void removeListener(ILabelProviderListener listener) {
		}

	}
	
	class MemoryRenderingContentProvider implements IStructuredContentProvider
	{

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			IMemoryRenderingType[] renderings = DebugUITools.getMemoryRenderingManager().getRenderingTypes((IMemoryBlock)inputElement);
			return renderings;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
			
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.memory.IMemoryRendering#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public Control createControl(Composite parent) {
		fCanvas = new Composite(parent, SWT.NONE);
		GridLayout compositeLayout = new GridLayout();
		compositeLayout.numColumns = 2;
		compositeLayout.makeColumnsEqualWidth = false;
		fCanvas.setLayout(compositeLayout);
		
		GridData comositeSpec= new GridData();
		comositeSpec.grabExcessVerticalSpace= true;
		comositeSpec.grabExcessHorizontalSpace= true;
		comositeSpec.horizontalAlignment= GridData.FILL;
		comositeSpec.verticalAlignment= GridData.CENTER;
		fCanvas.setLayoutData(comositeSpec);
		
		fMemoryBlockLabel = new Label(fCanvas, SWT.BORDER);
		
		String memoryBlockLabel = " "; //$NON-NLS-1$
		memoryBlockLabel = getLabel();
		
		fMemoryBlockLabel.setText("  " + DebugUIMessages.getString("CreateRenderingTab.Memory_monitor") + memoryBlockLabel + "  "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		GridData textLayout = new GridData();
		textLayout.verticalAlignment=GridData.CENTER;
		textLayout.horizontalAlignment=GridData.BEGINNING;
		fMemoryBlockLabel.setLayoutData(textLayout);

		Label renderingLabel = new Label(fCanvas, SWT.NONE);
		renderingLabel.setText(DebugUIMessages.getString("CreateRenderingTab.Select_renderings_to_create")); //$NON-NLS-1$
		GridData renderingLayout = new GridData();
		renderingLayout.horizontalAlignment = GridData.BEGINNING;
		renderingLayout.verticalAlignment = GridData.CENTER;
		renderingLayout.horizontalSpan = 2;
		renderingLabel.setLayoutData(renderingLayout);
		
		fViewer = new ListViewer(fCanvas);
		fViewer.setContentProvider(new MemoryRenderingContentProvider());
		fViewer.setLabelProvider(new MemoryRenderingLabelProvider());
		fViewer.setInput(getMemoryBlock());
		
		if (fViewer.getElementAt(0) != null)
		{
			fViewer.getList().select(0);
		}
		
		GridData listLayout = new GridData(GridData.FILL_BOTH);
		listLayout.horizontalSpan = 1;
		fViewer.getControl().setLayoutData(listLayout);
		
		fViewer.addDoubleClickListener(new IDoubleClickListener (){

			public void doubleClick(DoubleClickEvent event) {
				addRenderings();
			}});
		
		Button addButton = new Button(fCanvas, SWT.NONE);
		addButton.setText(DebugUIMessages.getString("CreateRenderingTab.Add_renderings")); //$NON-NLS-1$
		GridData buttonLayout = new GridData();
		buttonLayout.horizontalAlignment = GridData.BEGINNING;
		buttonLayout.verticalAlignment = GridData.BEGINNING;
		addButton.setLayoutData(buttonLayout);
		
		addButton.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				addRenderings();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				addRenderings();
			}});
		
		addButton.setFocus();
		
		DebugUITools.getMemoryRenderingManager().addListener(this);
		
		return fCanvas;		
	}
	
	private void addRenderings()
	{									
		ISelection selection = fViewer.getSelection();
		Object[] renderings = null;
		
		if (selection instanceof IStructuredSelection)
		{
			IStructuredSelection strucSelection = (IStructuredSelection)selection;
			
			renderings = strucSelection.toArray();
		}
		
		if (renderings == null)
		{
			Status stat = new Status(
					IStatus.ERROR,DebugUIPlugin.getUniqueIdentifier(),
					DebugException.INTERNAL_ERROR, DebugUIMessages.getString("CreateRenderingTab.0"), null); //$NON-NLS-1$
			DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), DebugUIMessages.getString("CreateRenderingTab.1"), DebugUIMessages.getString("CreateRenderingTab.2"), stat); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
										
		// ask for debug target and memory block retrieval
		IDebugTarget debugTarget = ((IDebugElement)getMemoryBlock()).getDebugTarget();
		IMemoryBlockRetrieval standardMemRetrieval = (IMemoryBlockRetrieval)((IDebugElement)getMemoryBlock()).getAdapter(IMemoryBlockRetrieval.class);
		
		if (standardMemRetrieval == null)
		{	
			standardMemRetrieval = debugTarget;
		}
		
		if (standardMemRetrieval == null)
			return;
		
		// add memory renderings to Memory Rendering Manager
		for (int i=0; i<renderings.length; i++)
		{	
			if (renderings[i] instanceof IMemoryRenderingType)
			{
				try {
					IMemoryRendering rendering = ((IMemoryRenderingType)renderings[i]).createRendering();
					if (rendering != null)
					{
						rendering.init(fContainer, getMemoryBlock());
						fContainer.addMemoryRendering(rendering);
					}
				} catch (CoreException e) {
					
					MemoryViewUtil.openError(DebugUIMessages.getString("CreateRendering.0"), DebugUIMessages.getString("CreateRendering.1"), e);  //$NON-NLS-1$ //$NON-NLS-2$
				}
			}					
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.memory.IMemoryRendering#dispose()
	 */
	public void dispose() {
		fViewer = null;
		fCanvas = null;
		fContainer = null;
		fMemoryBlockLabel = null;
		DebugUITools.getMemoryRenderingManager().removeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.memory.AbstractMemoryRendering#getControl()
	 */
	public Control getControl() {
		return fCanvas;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.memory.AbstractMemoryRendering#getLabel()
	 */
	public String getLabel() {
		String memoryBlockLabel = " "; //$NON-NLS-1$
		if (getMemoryBlock() instanceof IMemoryBlockExtension)
		{
			if (((IMemoryBlockExtension)getMemoryBlock()).getExpression() != null)
			{
				String prefix = ""; //$NON-NLS-1$
				if (((IMemoryBlockExtension)getMemoryBlock()).getExpression().startsWith("&")) //$NON-NLS-1$
				{
					prefix = "&"; //$NON-NLS-1$
					memoryBlockLabel += prefix;
				}
				memoryBlockLabel += ((IMemoryBlockExtension)getMemoryBlock()).getExpression();
			}
			
			try {
				if (((IMemoryBlockExtension)getMemoryBlock()).getBigBaseAddress() != null)
				{
					memoryBlockLabel += " <0x" + ((IMemoryBlockExtension)getMemoryBlock()).getBigBaseAddress().toString(16) + ">"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			} catch (DebugException e) {
				// return whatever we have for label
				return memoryBlockLabel;
			}
		}
		else
		{
			long address = getMemoryBlock().getStartAddress();
			memoryBlockLabel = Long.toHexString(address);
		}
		return memoryBlockLabel;
	}

	public void memoryRenderingBindingsChanged() {
		if (fViewer != null)
			fViewer.refresh();
	}

}
