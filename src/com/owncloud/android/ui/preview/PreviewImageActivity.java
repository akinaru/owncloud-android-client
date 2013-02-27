/* ownCloud Android client application
 *   Copyright (C) 2012-2013  ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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
package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.ui.activity.FileDetailActivity;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.FileFragment;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.R;

/**
 *  Used as an utility to preview image files contained in an ownCloud account.
 *  
 *  @author David A. Velasco
 */
public class PreviewImageActivity extends SherlockFragmentActivity implements FileFragment.ContainerActivity, ViewPager.OnPageChangeListener {
    
    public static final int DIALOG_SHORT_WAIT = 0;

    public static final String TAG = PreviewImageActivity.class.getSimpleName();
    
    public static final String KEY_WAITING_TO_PREVIEW = "WAITING_TO_PREVIEW";
    private static final String KEY_WAITING_FOR_BINDER = "WAITING_FOR_BINDER";
    
    private OCFile mFile;
    private OCFile mParentFolder;  
    private Account mAccount;
    private DataStorageManager mStorageManager;
    
    private ViewPager mViewPager; 
    private PreviewImagePagerAdapter mPreviewImagePagerAdapter;    
    
    private FileDownloaderBinder mDownloaderBinder = null;
    private ServiceConnection mDownloadConnection, mUploadConnection = null;
    private FileUploaderBinder mUploaderBinder = null;

    private boolean mRequestWaitingForBinder;
    
    private DownloadFinishReceiver mDownloadFinishReceiver;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFile = getIntent().getParcelableExtra(FileDetailFragment.EXTRA_FILE);
        mAccount = getIntent().getParcelableExtra(FileDetailFragment.EXTRA_ACCOUNT);
        if (mFile == null) {
            throw new IllegalStateException("Instanced with a NULL OCFile");
        }
        if (mAccount == null) {
            throw new IllegalStateException("Instanced with a NULL ownCloud Account");
        }
        if (!mFile.isImage()) {
            throw new IllegalArgumentException("Non-image file passed as argument");
        }
        
        setContentView(R.layout.preview_image_activity);
    
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(mFile.getFileName());
        
        mStorageManager = new FileDataStorageManager(mAccount, getContentResolver());
        mParentFolder = mStorageManager.getFileById(mFile.getParentId());
        if (mParentFolder == null) {
            // should not be necessary
            mParentFolder = mStorageManager.getFileByPath(OCFile.PATH_SEPARATOR);
        }

        if (savedInstanceState != null) {
            mRequestWaitingForBinder = savedInstanceState.getBoolean(KEY_WAITING_FOR_BINDER);
        } else {
            mRequestWaitingForBinder = false;
        }
        
        createViewPager();

    }

    private void createViewPager() {
        mPreviewImagePagerAdapter = new PreviewImagePagerAdapter(getSupportFragmentManager(), mParentFolder, mAccount, mStorageManager);
        mViewPager = (ViewPager) findViewById(R.id.fragmentPager);
        int position = mPreviewImagePagerAdapter.getFilePosition(mFile);
        position = (position >= 0) ? position : 0;
        mViewPager.setAdapter(mPreviewImagePagerAdapter); 
        mViewPager.setOnPageChangeListener(this);
        Log.e(TAG, "Setting initial position " + position);
        mViewPager.setCurrentItem(position);
        if (position == 0 && !mFile.isDown()) {
            // this is necessary because mViewPager.setCurrentItem(0) just after setting the adapter does not result in a call to #onPageSelected(0) 
            mRequestWaitingForBinder = true;
        }
    }
    
    
    @Override
    public void onStart() {
        super.onStart();
        Log.e(TAG, "PREVIEW ACTIVITY ON START");
        mDownloadConnection = new PreviewImageServiceConnection();
        bindService(new Intent(this, FileDownloader.class), mDownloadConnection, Context.BIND_AUTO_CREATE);
        mUploadConnection = new PreviewImageServiceConnection();
        bindService(new Intent(this, FileUploader.class), mUploadConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_WAITING_FOR_BINDER, mRequestWaitingForBinder);    
    }


    /** Defines callbacks for service binding, passed to bindService() */
    private class PreviewImageServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
                
            if (component.equals(new ComponentName(PreviewImageActivity.this, FileDownloader.class))) {
                Log.e(TAG, "PREVIEW_IMAGE Download service connected");
                mDownloaderBinder = (FileDownloaderBinder) service;
                if (mRequestWaitingForBinder) {
                    mRequestWaitingForBinder = false;
                    Log.e(TAG, "Simulating reselection of current page after connection of download binder");
                    onPageSelected(mViewPager.getCurrentItem());
                }
                    
            } else if (component.equals(new ComponentName(PreviewImageActivity.this, FileUploader.class))) {
                Log.d(TAG, "Upload service connected");
                mUploaderBinder = (FileUploaderBinder) service;
            } else {
                return;
            }
            
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(PreviewImageActivity.this, FileDownloader.class))) {
                Log.d(TAG, "Download service suddenly disconnected");
                mDownloaderBinder = null;
            } else if (component.equals(new ComponentName(PreviewImageActivity.this, FileUploader.class))) {
                Log.d(TAG, "Upload service suddenly disconnected");
                mUploaderBinder = null;
            }
        }
    };    
    
    
    @Override
    public void onStop() {
        super.onStop();
        if (mDownloadConnection != null) {
            unbindService(mDownloadConnection);
            mDownloadConnection = null;
        }
        if (mUploadConnection != null) {
            unbindService(mUploadConnection);
            mUploadConnection = null;
        }
    }
    
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean returnValue = false;
        
        switch(item.getItemId()){
        case android.R.id.home:
            backToDisplayActivity();
            returnValue = true;
            break;
        default:
        	returnValue = super.onOptionsItemSelected(item);
        }
        
        return returnValue;
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "PREVIEW ACTIVITY ONRESUME");
        mDownloadFinishReceiver = new DownloadFinishReceiver();
        IntentFilter filter = new IntentFilter(FileDownloader.DOWNLOAD_FINISH_MESSAGE);
        registerReceiver(mDownloadFinishReceiver, filter);
    }

    
    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.e(TAG, "PREVIEW ACTIVITY ONPOSTRESUME");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mDownloadFinishReceiver);
        mDownloadFinishReceiver = null;
    }
    

    private void backToDisplayActivity() {
        /*
        Intent intent = new Intent(this, FileDisplayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(FileDetailFragment.EXTRA_FILE, mFile);
        intent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, mAccount);
        startActivity(intent);
        */
        finish();
    }
    
    
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_SHORT_WAIT: {
            ProgressDialog working_dialog = new ProgressDialog(this);
            working_dialog.setMessage(getResources().getString(
                    R.string.wait_a_moment));
            working_dialog.setIndeterminate(true);
            working_dialog.setCancelable(false);
            dialog = working_dialog;
            break;
        }
        default:
            dialog = null;
        }
        return dialog;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onFileStateChanged() {
        // nothing to do here!
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public FileDownloaderBinder getFileDownloaderBinder() {
        return mDownloaderBinder;
    }


    @Override
    public FileUploaderBinder getFileUploaderBinder() {
        return mUploaderBinder;
    }


    @Override
    public void showFragmentWithDetails(OCFile file) {
        Intent showDetailsIntent = new Intent(this, FileDetailActivity.class);
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, file);
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, AccountUtils.getCurrentOwnCloudAccount(this));
        showDetailsIntent.putExtra(FileDetailActivity.EXTRA_MODE, FileDetailActivity.MODE_DETAILS);
        startActivity(showDetailsIntent);
    }

    
    private void requestForDownload(OCFile file) {
        Log.e(TAG, "REQUEST FOR DOWNLOAD : " + file.getFileName());
        if (mDownloaderBinder == null) {
            Log.e(TAG, "requestForDownload called without binder to download service");
            
        } else if (!mDownloaderBinder.isDownloading(mAccount, file)) {
            Intent i = new Intent(this, FileDownloader.class);
            i.putExtra(FileDownloader.EXTRA_ACCOUNT, mAccount);
            i.putExtra(FileDownloader.EXTRA_FILE, file);
            startService(i);
        }
    }

    @Override
    public void notifySuccessfulDownload(OCFile file, Intent intent, boolean success) {
        /*
        if (success) {
            if (mWaitingToPreview != null && mWaitingToPreview.equals(file)) {
                mWaitingToPreview = null;
                int position = mViewPager.getCurrentItem();
                mPreviewImagePagerAdapter.updateFile(position, file);
                Log.e(TAG, "BEFORE NOTIFY DATA SET CHANGED");
                mPreviewImagePagerAdapter.notifyDataSetChanged();
                Log.e(TAG, "AFTER NOTIFY DATA SET CHANGED");
            }
        }
        */
    }

    
    /**
     * This method will be invoked when a new page becomes selected. Animation is not necessarily complete.
     * 
     *  @param  Position        Position index of the new selected page
     */
    @Override
    public void onPageSelected(int position) {
        Log.e(TAG, "onPageSelected " + position);
        if (mDownloaderBinder == null) {
            mRequestWaitingForBinder = true;
            
        } else {
            OCFile currentFile = mPreviewImagePagerAdapter.getFileAt(position); 
            getSupportActionBar().setTitle(currentFile.getFileName());
            if (!currentFile.isDown()) {
                requestForDownload(currentFile);
                //updateCurrentDownloadFragment(true);        
            }
        }
    }
    
    /**
     * Called when the scroll state changes. Useful for discovering when the user begins dragging, 
     * when the pager is automatically settling to the current page, or when it is fully stopped/idle.
     * 
     * @param   State       The new scroll state (SCROLL_STATE_IDLE, _DRAGGING, _SETTLING
     */
    @Override
    public void onPageScrollStateChanged(int state) {
    }

    /**
     * This method will be invoked when the current page is scrolled, either as part of a programmatically 
     * initiated smooth scroll or a user initiated touch scroll.
     * 
     * @param   position                Position index of the first page currently being displayed. 
     *                                  Page position+1 will be visible if positionOffset is nonzero.
     *                                  
     * @param   positionOffset          Value from [0, 1) indicating the offset from the page at position.
     * @param   positionOffsetPixels    Value in pixels indicating the offset from position. 
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }
    

    private void updateCurrentDownloadFragment(boolean transferring) {
        FileFragment fragment = mPreviewImagePagerAdapter.getFragmentAt(mViewPager.getCurrentItem());
        if (fragment instanceof FileDownloadFragment) {
            ((FileDownloadFragment) fragment).updateView(transferring); 
            //mViewPager.invalidate();
        }
    }
    
    
    /**
     * Class waiting for broadcast events from the {@link FielDownloader} service.
     * 
     * Updates the UI when a download is started or finished, provided that it is relevant for the
     * folder displayed in the gallery.
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String accountName = intent.getStringExtra(FileDownloader.ACCOUNT_NAME);
            String downloadedRemotePath = intent.getStringExtra(FileDownloader.EXTRA_REMOTE_PATH);
            if (mAccount.name.equals(accountName) && 
                    downloadedRemotePath != null) {

                OCFile file = mStorageManager.getFileByPath(downloadedRemotePath);
                int position = mPreviewImagePagerAdapter.getFilePosition(file);
                boolean downloadWasFine = intent.getBooleanExtra(FileDownloader.EXTRA_DOWNLOAD_RESULT, false);
                boolean isCurrent =  (mViewPager.getCurrentItem() == position);
                
                if (position >= 0) {
                    /// ITS MY BUSSINESS
                    Log.e(TAG, "downloaded file FOUND in adapter");
                    if (downloadWasFine) {
                        mPreviewImagePagerAdapter.updateFile(position, file);
                        //Log.e(TAG, "BEFORE NOTIFY DATA SET CHANGED");
                        mPreviewImagePagerAdapter.notifyDataSetChanged();
                        //Log.e(TAG, "AFTER NOTIFY DATA SET CHANGED");
                        
                    } else if (isCurrent) {
                        updateCurrentDownloadFragment(false);
                    }
                    
                } else {
                    Log.e(TAG, "DOWNLOADED FILE NOT FOUND IN ADAPTER ");
                }
                
            }
            removeStickyBroadcast(intent);
        }

    }
    
    
}
