/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.Splitter;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;

public class CompareContentViewerSwitchingPane extends
		CompareViewerSwitchingPane {

	private CompareEditorInput fCompareEditorInput;

	private ViewerDescriptor fSelectedViewerDescriptor;

	private ToolBar toolBar;

	public CompareContentViewerSwitchingPane(Splitter parent, int style,
			CompareEditorInput cei) {
		super(parent, style);
		fCompareEditorInput = cei;
	}

	private CompareConfiguration getCompareConfiguration() {
		return fCompareEditorInput.getCompareConfiguration();
	}

	protected Viewer getViewer(Viewer oldViewer, Object input) {
		if (fSelectedViewerDescriptor != null) {
			ViewerDescriptor[] array = CompareUIPlugin.getDefault().findContentViewerDescriptor(
					oldViewer, input, getCompareConfiguration());
			List list = array != null ? Arrays.asList(array)
					: Collections.EMPTY_LIST;
			if (list.contains(fSelectedViewerDescriptor)) {
				// use selected viewer only when appropriate for the new input
				fCompareEditorInput
						.setViewerDescriptor(fSelectedViewerDescriptor);
				Viewer viewer = fCompareEditorInput.findContentViewer(
						oldViewer, (ICompareInput) input, this);
				return viewer;
			}
			// fallback to default otherwise
			fSelectedViewerDescriptor = null;
		}
		if (input instanceof ICompareInput) {
			fCompareEditorInput.setViewerDescriptor(null);
			Viewer viewer = fCompareEditorInput.findContentViewer(oldViewer,
					(ICompareInput) input, this);
			fCompareEditorInput.setViewerDescriptor(fSelectedViewerDescriptor);
			return viewer;
		}
		return null;
	}

	protected Control createTopLeft(Composite p) {
		final Composite composite = new Composite(p, SWT.NONE) {
			public Point computeSize(int wHint, int hHint, boolean changed) {
				return super.computeSize(wHint, Math.max(24, hHint), changed);
			}
		};

		RowLayout layout = new RowLayout();
		layout.marginTop = 0;
		composite.setLayout(layout);

		CLabel cl = new CLabel(composite, SWT.NONE);
		cl.setText(null);

		toolBar = new ToolBar(composite, SWT.FLAT);
		toolBar.setVisible(false); // hide by default
		final ToolItem toolItem = new ToolItem(toolBar, SWT.PUSH, 0);
		toolItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(
				/* IWorkbenchGraphicConstants */"IMG_LCL_VIEW_MENU")); //$NON-NLS-1$
		toolItem
				.setToolTipText(CompareMessages.CompareContentViewerSwitchingPane_switchButtonTooltip);
		toolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showMenu();
			}
		});
		toolBar.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				showMenu();
			}
		});
		return composite;
	}
	
	protected boolean inputChanged(Object input) {
		return getInput() != input
				|| fCompareEditorInput.getViewerDescriptor() != fSelectedViewerDescriptor;
	}

	public void setInput(Object input) {
		super.setInput(input);
		ViewerDescriptor[] vd = CompareUIPlugin.getDefault()
				.findContentViewerDescriptor(getViewer(), getInput(),
						getCompareConfiguration());
		toolBar.setVisible(vd != null && vd.length > 1);
	}
	
	private void showMenu() {
		ViewerDescriptor[] vd = CompareUIPlugin.getDefault()
				.findContentViewerDescriptor(getViewer(), getInput(),
						getCompareConfiguration());

		// 1. create
		final Menu menu = new Menu(getShell(), SWT.POP_UP);

		// add default
		String label = CompareMessages.CompareContentViewerSwitchingPane_defaultViewer;
		MenuItem defaultItem = new MenuItem(menu, SWT.RADIO);
		defaultItem.setText(label);
		defaultItem.addSelectionListener(createSelectionListener(null));
		defaultItem.setSelection(fSelectedViewerDescriptor == null);

		new MenuItem(menu, SWT.SEPARATOR);
		
		// add others
		for (int i = 0; i < vd.length; i++) {
			final ViewerDescriptor vdi = vd[i];
			label = vdi.getLabel();
			if (label == null || label.equals("")) { //$NON-NLS-1$
				String l = CompareUIPlugin.getDefault().findContentTypeNameOrType((ICompareInput) getInput(), vdi, getCompareConfiguration());
				if (l == null)
					// couldn't figure out the label, skip the viewer
					continue;
				label = NLS.bind(CompareMessages.CompareContentViewerSwitchingPane_discoveredLabel, new Object[] {l});
			}
			MenuItem item = new MenuItem(menu, SWT.RADIO);
			item.setText(label);
			item.addSelectionListener(createSelectionListener(vdi));
			item.setSelection(vdi == fSelectedViewerDescriptor);
		}
		
		// 2. show
		Rectangle bounds = toolBar.getItem(0).getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = toolBar.toDisplay(topLeft);
		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);
		
		// 3. dispose on close
		menu.addMenuListener(new MenuAdapter() {
			public void menuHidden(MenuEvent e) {
				e.display.asyncExec(new Runnable() {
					public void run() {
						menu.dispose();
					}
				});
			}
		});
	}

	private SelectionListener createSelectionListener(final ViewerDescriptor vd) {
		return new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				MenuItem mi = (MenuItem) e.widget;
				if (mi.getSelection()) {
					Viewer oldViewer = getViewer();
					fSelectedViewerDescriptor = vd;
					CompareContentViewerSwitchingPane.this.setInput(oldViewer
							.getInput());
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing to do
			}
		};
	}

	public void setText(String label) {
		Composite c = (Composite) getTopLeft();
		Control[] children = c.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof CLabel) {
				CLabel cl = (CLabel) children[i];
				if (cl != null && !cl.isDisposed()) {
					cl.setText(label);
					c.layout();
				}
				return;
			}
		}
	}

	public void setImage(Image image) {
		Composite c = (Composite) getTopLeft();
		Control[] children = c.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof CLabel) {
				CLabel cl = (CLabel) children[i];
				if (cl != null && !cl.isDisposed())
					cl.setImage(image);
				return;
			}
		}
	}
	
	public void addMouseListener(MouseListener listener) {
		Composite c = (Composite) getTopLeft();
		Control[] children = c.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof CLabel) {
				CLabel cl = (CLabel) children[i];
				cl.addMouseListener(listener);
			}
		}
	}
}
