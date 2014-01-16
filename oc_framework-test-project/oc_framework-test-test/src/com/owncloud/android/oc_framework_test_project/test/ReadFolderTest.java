/* ownCloud webDAV Library for Android is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.oc_framework_test_project.test;

import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework_test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;

/**
 * Class to test Read Folder Operation
 * @author masensio
 *
 */

public class ReadFolderTest extends	ActivityInstrumentationTestCase2<TestActivity> {
	

	/* Folder data to read. This folder must exist on the account */
	private final String mRemoteFolderPath = "/folderToRead";
	
	
	private TestActivity mActivity;
	
	public ReadFolderTest() {
	    super(TestActivity.class);
	}
	
	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mActivity = getActivity();
	}

	/**
	 * Test Read Folder
	 */
	public void testReadFolder() {

		RemoteOperationResult result = mActivity.readFile(mRemoteFolderPath);
		assertTrue(result.getData().size() > 1);
		assertTrue(result.getData().size() == 4);
		assertTrue(result.isSuccess());
	}
	
}
