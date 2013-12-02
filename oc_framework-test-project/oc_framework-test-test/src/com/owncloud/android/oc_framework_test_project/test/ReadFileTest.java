/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.oc_framework_test_project.test;

import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework_test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;

/**
 * Class to test Read File Operation
 * @author masensio
 *
 */

public class ReadFileTest extends 	ActivityInstrumentationTestCase2<TestActivity> {
	
	/* File data to read. This file must exist on the account */
    private final String mRemoteFolderPath = "/fileToRead.txt";
    
    
    private TestActivity mActivity;
    
    public ReadFileTest() {
        super(TestActivity.class);
    }
    
    @Override
      protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        mActivity = getActivity();
    }

    /**
     * Test Read File
     */
    public void testReadFile() {

            RemoteOperationResult result = mActivity.readFile(mRemoteFolderPath);
            assertTrue(result.getData().size() ==  1);
            assertTrue(result.isSuccess());
    }
    

}
