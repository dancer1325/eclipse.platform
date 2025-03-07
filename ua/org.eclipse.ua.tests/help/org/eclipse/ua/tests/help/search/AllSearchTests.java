/*******************************************************************************
 *  Copyright (c) 2006, 2025 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ua.tests.help.search;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/*
 * Tests help functionality (automated).
 */
@Suite
@SelectClasses({ //
		ExtraDirTest.class, //
		BasicTest.class, //
		WildcardTest.class, //
		LocaleTest.class, //
		AnalyzerTest.class, //
		SearchCheatsheet.class, //
		SearchIntro.class, //
		EncodedCharacterSearch.class, //
		MetaKeywords.class, //
		SearchParticipantTest.class, //
		SearchParticipantXMLTest.class, //
		SearchProcessorTest.class, //
		SearchRanking.class, //
		WorkingSetManagerTest.class, //
		InfocenterWorkingSetManagerTest.class, //
		PrebuiltIndexCompatibility.class, //
		LockTest.class, //
})
public class AllSearchTests {
}
