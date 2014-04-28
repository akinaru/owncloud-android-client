/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
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
package com.owncloud.android.ui.adapter;

import android.accounts.Account;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
//import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;


import java.util.Vector;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.ui.activity.TransferServiceGetter;
import com.owncloud.android.utils.DisplayUtils;


/**
 * This Adapter populates a ListView with all files and folders in an ownCloud
 * instance.
 * 
 * @author Bartek Przybylski
 * 
 */
public class FileListListAdapter extends SimpleCursorAdapter implements ListAdapter {

    private Context mContext;
    private static OCFile mFile = null;
    private Vector<OCFile> mFiles = null;
    private static FileDataStorageManager mStorageManager;
    private Account mAccount;
    private TransferServiceGetter mTransferServiceGetter;
    private static Cursor mCursor = null;
    
    private static String[] cursorFrom = { ProviderMeta.ProviderTableMeta.FILE_NAME,
        ProviderMeta.ProviderTableMeta.FILE_MODIFIED, 
        ProviderMeta.ProviderTableMeta.FILE_CONTENT_LENGTH
        }; 
    private static int[] cursorTo  = { R.id.Filename, 
        R.id.last_mod, 
        R.id.file_size
    };

    public FileListListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        // TODO Auto-generated constructor stub
    }
    
    public FileListListAdapter(Context context, TransferServiceGetter transferServiceGetter, OCFile file) {
        super(context,
                R.layout.list_item,
                mCursor,
                cursorFrom,
                cursorTo, 
                0);
        mContext = context;
        mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        mTransferServiceGetter = transferServiceGetter;
        mFile = file;
        mCursor = file == null ? null: mStorageManager.getContent(file.getParentId());
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        return mFiles != null ? mFiles.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        if (mFiles == null || mFiles.size() <= position)
            return null;
        return mFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (mFiles == null || mFiles.size() <= position)
            return 0;
        return mFiles.get(position).getFileId();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflator = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflator.inflate(R.layout.list_item, null);
        }
    
        if (mFiles != null && mFiles.size() > position) {
            OCFile file = mFiles.get(position);
            TextView fileName = (TextView) view.findViewById(R.id.Filename);
            String name = file.getFileName();

            fileName.setText(name);
            ImageView fileIcon = (ImageView) view.findViewById(R.id.imageView1);
            fileIcon.setImageResource(DisplayUtils.getResourceId(file.getMimetype()));
            ImageView localStateView = (ImageView) view.findViewById(R.id.imageView2);
            FileDownloaderBinder downloaderBinder = mTransferServiceGetter.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mTransferServiceGetter.getFileUploaderBinder();
            if (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, file)) {
                localStateView.setImageResource(R.drawable.downloading_file_indicator);
                localStateView.setVisibility(View.VISIBLE);
            } else if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, file)) {
                localStateView.setImageResource(R.drawable.uploading_file_indicator);
                localStateView.setVisibility(View.VISIBLE);
            } else if (file.isDown()) {
                localStateView.setImageResource(R.drawable.local_file_indicator);
                localStateView.setVisibility(View.VISIBLE);
            } else {
                localStateView.setVisibility(View.INVISIBLE);
            }
            
            TextView fileSizeV = (TextView) view.findViewById(R.id.file_size);
            TextView lastModV = (TextView) view.findViewById(R.id.last_mod);
            ImageView checkBoxV = (ImageView) view.findViewById(R.id.custom_checkbox);
            
            if (!file.isFolder()) {
                fileSizeV.setVisibility(View.VISIBLE);
                fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));
                lastModV.setVisibility(View.VISIBLE);
                lastModV.setText(DisplayUtils.unixTimeToHumanReadable(file.getModificationTimestamp()));
                // this if-else is needed even thoe fav icon is visible by default
                // because android reuses views in listview
                if (!file.keepInSync()) {
                    view.findViewById(R.id.imageView3).setVisibility(View.GONE);
                } else {
                    view.findViewById(R.id.imageView3).setVisibility(View.VISIBLE);
                }
                
                ListView parentList = (ListView)parent;
                if (parentList.getChoiceMode() == ListView.CHOICE_MODE_NONE) { 
                    checkBoxV.setVisibility(View.GONE);
                } else {
                    if (parentList.isItemChecked(position)) {
                        checkBoxV.setImageResource(android.R.drawable.checkbox_on_background);
                    } else {
                        checkBoxV.setImageResource(android.R.drawable.checkbox_off_background);
                    }
                    checkBoxV.setVisibility(View.VISIBLE);
                }
                
            } 
            else {
                
                fileSizeV.setVisibility(View.INVISIBLE);
                //fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));
                lastModV.setVisibility(View.VISIBLE);
                lastModV.setText(DisplayUtils.unixTimeToHumanReadable(file.getModificationTimestamp()));
                checkBoxV.setVisibility(View.GONE);
                view.findViewById(R.id.imageView3).setVisibility(View.GONE);
            }
            
            ImageView shareIconV = (ImageView) view.findViewById(R.id.shareIcon);
            if (file.isShareByLink()) {
                shareIconV.setVisibility(View.VISIBLE);
            } else {
                shareIconV.setVisibility(View.INVISIBLE);
            }
        }

        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return (mFiles == null || mFiles.isEmpty());
    }

    /**
     * Change the adapted directory for a new one
     * @param directory                 New file to adapt. Can be NULL, meaning "no content to adapt".
     * @param updatedStorageManager     Optional updated storage manager; used to replace mStorageManager if is different (and not NULL)
     */
    public void swapDirectory(OCFile directory, FileDataStorageManager updatedStorageManager) {
        mFile = directory;
        if (updatedStorageManager != null && updatedStorageManager != mStorageManager) {
            mStorageManager = updatedStorageManager;
            mAccount = AccountUtils.getCurrentOwnCloudAccount(mContext);
        }
        if (mStorageManager != null) {
            mFiles = mStorageManager.getFolderContent(mFile);
            mCursor = mStorageManager.getContent(mFile.getParentId());
        } else {
            mFiles = null;
            mCursor = null;
        }
        notifyDataSetChanged();
    }
    
}
