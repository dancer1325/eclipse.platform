/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.console.IOConsole;

/**
 * Tests if an IOConsole's type matches the expected value
 * 
 * @since 3.1
 */
public class ConsoleTypePropertyTester extends PropertyTester {

    /* (non-Javadoc)
     * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
     */
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        IOConsole console = (IOConsole) receiver;
        String type = console.getType();
        return type != null ? type.equals(expectedValue) : false;
    }

}
