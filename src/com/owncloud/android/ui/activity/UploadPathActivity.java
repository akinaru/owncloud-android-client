/* ownCloud Android client application
 *   Copyright (C) 2012-2014 ownCloud Inc.
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

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.content.Intent;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;

public class UploadPathActivity extends FolderPickerActivity implements FileFragment.ContainerActivity,
        OnClickListener, OnEnforceableRefreshListener {

    public static final String KEY_INSTANT_UPLOAD_PATH = "INSTANT_UPLOAD_PATH";

    public static final int RESULT_OK_SET_UPLOAD_PATH = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String instantUploadPath = getIntent().getStringExtra(KEY_INSTANT_UPLOAD_PATH);

        OCFile folder = new OCFile(instantUploadPath);

        setFile(folder);
    }

    /**
     * Called when the ownCloud {@link Account} associated to the Activity was
     * just updated.
     */
    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
        if (getAccount() != null) {

            updateFileFromDB();

            OCFile folder = getFile();
            if (folder == null || !folder.isFolder()) {
                // fall back to root folder
                setFile(getStorageManager().getFileByPath(OCFile.ROOT_PATH));
                folder = getFile();
            }

            onBrowsedDownTo(folder);

            if (!stateWasRecovered) {
                OCFileListFragment listOfFolders = getListOfFilesFragment();
                listOfFolders.listDirectory(folder);

                startSyncFolderOperation(folder, false);
            }

            updateNavigationElementsInActionBar();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mCancelBtn) {
            finish();
        } else if (v == mChooseBtn) {
            Intent data = new Intent();
            data.putExtra(EXTRA_CURRENT_FOLDER, getCurrentFolder());
            setResult(RESULT_OK_SET_UPLOAD_PATH, data);
            finish();
        }
    }
}
