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

package com.owncloud.android.operations;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.FileStorageUtils;

import org.apache.http.HttpStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

//import android.support.v4.content.LocalBroadcastManager;


/**
 *  Remote operation performing the synchronization of the list of files contained 
 *  in a folder identified with its remote path.
 *  
 *  Fetches the list and properties of the files contained in the given folder, including their 
 *  properties, and updates the local database with them.
 *  
 *  Does NOT enter in the child folders to synchronize their contents also.
 * 
 *  @author David A. Velasco
 */
public class SynchronizeFolderOperation extends SyncOperation {

    private static final String TAG = SynchronizeFolderOperation.class.getSimpleName();

    /** Time stamp for the synchronization process in progress */
    private long mCurrentSyncTime;

    /** Remote folder to synchronize */
    private OCFile mLocalFolder;

    /** Account where the file to synchronize belongs */
    private Account mAccount;

    /** Android context; necessary to send requests to the download service */
    private Context mContext;

    /** Files and folders contained in the synchronized folder after a successful operation */
    private List<OCFile> mChildren;

    /** Counter of conflicts found between local and remote files */
    private int mConflictsFound;

    /** Counter of failed operations in synchronization of kept-in-sync files */
    private int mFailsInFavouritesFound;

    /**
     * Map of remote and local paths to files that where locally stored in a location
     * out of the ownCloud folder and couldn't be copied automatically into it
     **/
    private Map<String, String> mForgottenLocalFiles;

    /** 'True' means that the remote folder changed and should be fetched */
    private boolean mRemoteFolderChanged;


    /**
     * Creates a new instance of {@link SynchronizeFolderOperation}.
     *
     * @param   context                 Application context.
     * @param   remotePath              Path to synchronize.
     * @param   account                 ownCloud account where the folder is located.
     * @param   currentSyncTime         Time stamp for the synchronization process in progress.
     */
    public SynchronizeFolderOperation(Context context, String remotePath, Account account, long currentSyncTime){
        mLocalFolder = new OCFile(remotePath);
        mCurrentSyncTime = currentSyncTime;
        mAccount = account;
        mContext = context;
        mForgottenLocalFiles = new HashMap<String, String>();
        mRemoteFolderChanged = false;
    }


    public int getConflictsFound() {
        return mConflictsFound;
    }

    public int getFailsInFavouritesFound() {
        return mFailsInFavouritesFound;
    }

    public Map<String, String> getForgottenLocalFiles() {
        return mForgottenLocalFiles;
    }

    /**
     * Returns the list of files and folders contained in the synchronized folder,
     * if called after synchronization is complete.
     *
     * @return  List of files and folders contained in the synchronized folder.
     */
    public List<OCFile> getChildren() {
        return mChildren;
    }

    /**
     * Performs the synchronization.
     *
     * {@inheritDoc}
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        mFailsInFavouritesFound = 0;
        mConflictsFound = 0;
        mForgottenLocalFiles.clear();

        result = checkForChanges(client);

        if (result.isSuccess()) {
            if (mRemoteFolderChanged) {
                result = fetchAndSyncRemoteFolder(client);
            } else {
                mChildren = getStorageManager().getFolderContent(mLocalFolder);
            }
        }

        return result;

    }

    private RemoteOperationResult checkForChanges(OwnCloudClient client) {
        mRemoteFolderChanged = true;
        RemoteOperationResult result = null;
        String remotePath = null;

        remotePath = mLocalFolder.getRemotePath();
        Log_OC.d(TAG, "Checking changes in " + mAccount.name + remotePath);

        // remote request
        ReadRemoteFileOperation operation = new ReadRemoteFileOperation(remotePath);
        result = operation.execute(client);
        if (result.isSuccess()){
            OCFile remoteFolder = FileStorageUtils.fillOCFile((RemoteFile) result.getData().get(0));

            // check if remote and local folder are different
            mRemoteFolderChanged =
                        !(remoteFolder.getEtag().equalsIgnoreCase(mLocalFolder.getEtag()));

            result = new RemoteOperationResult(ResultCode.OK);

            Log_OC.i(TAG, "Checked " + mAccount.name + remotePath + " : " +
                    (mRemoteFolderChanged ? "changed" : "not changed"));

        } else {
            // check failed
            if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
                removeLocalFolder();
            }
            if (result.isException()) {
                Log_OC.e(TAG, "Checked " + mAccount.name + remotePath  + " : " +
                        result.getLogMessage(), result.getException());
            } else {
                Log_OC.e(TAG, "Checked " + mAccount.name + remotePath + " : " +
                        result.getLogMessage());
            }
        }

        return result;
    }


    private RemoteOperationResult fetchAndSyncRemoteFolder(OwnCloudClient client) {
        String remotePath = mLocalFolder.getRemotePath();
        ReadRemoteFolderOperation operation = new ReadRemoteFolderOperation(remotePath);
        RemoteOperationResult result = operation.execute(client);
        Log_OC.d(TAG, "Synchronizing " + mAccount.name + remotePath);

        if (result.isSuccess()) {
            synchronizeData(result.getData(), client);
            if (mConflictsFound > 0  || mFailsInFavouritesFound > 0) {
                result = new RemoteOperationResult(ResultCode.SYNC_CONFLICT);
                    // should be a different result code, but will do the job
            }
        } else {
            if (result.getCode() == ResultCode.FILE_NOT_FOUND)
                removeLocalFolder();
        }

        return result;
    }


    private void removeLocalFolder() {
        FileDataStorageManager storageManager = getStorageManager();
        if (storageManager.fileExists(mLocalFolder.getFileId())) {
            String currentSavePath = FileStorageUtils.getSavePath(mAccount.name);
            storageManager.removeFolder(
                    mLocalFolder,
                    true,
                    (   mLocalFolder.isDown() &&
                            mLocalFolder.getStoragePath().startsWith(currentSavePath)
                    )
            );
        }
    }


    /**
     *  Synchronizes the data retrieved from the server about the contents of the target folder
     *  with the current data in the local database.
     *
     *  Grants that mChildren is updated with fresh data after execution.
     *
     *  @param folderAndFiles   Remote folder and children files in Folder
     *
     *  @param client           Client instance to the remote server where the data were
     *                          retrieved.
     *  @return                 'True' when any change was made in the local data, 'false' otherwise
     */
    private void synchronizeData(ArrayList<Object> folderAndFiles, OwnCloudClient client) {
        FileDataStorageManager storageManager = getStorageManager();
        
        // get 'fresh data' from the database
        mLocalFolder = storageManager.getFileByPath(mLocalFolder.getRemotePath());

        // parse data from remote folder
        OCFile remoteFolder = fillOCFile((RemoteFile)folderAndFiles.get(0));
        remoteFolder.setParentId(mLocalFolder.getParentId());
        remoteFolder.setFileId(mLocalFolder.getFileId());

        Log_OC.d(TAG, "Remote folder " + mLocalFolder.getRemotePath()
                + " changed - starting update of local data ");

        List<OCFile> updatedFiles = new Vector<OCFile>(folderAndFiles.size() - 1);
        List<SynchronizeFileOperation> filesToSyncContents = new Vector<SynchronizeFileOperation>();

        // get current data about local contents of the folder to synchronize
        List<OCFile> localFiles = storageManager.getFolderContent(mLocalFolder);
        Map<String, OCFile> localFilesMap = new HashMap<String, OCFile>(localFiles.size());
        for (OCFile file : localFiles) {
            localFilesMap.put(file.getRemotePath(), file);
        }

        // loop to update every child
        OCFile remoteFile = null, localFile = null;
        for (int i=1; i<folderAndFiles.size(); i++) {
            /// new OCFile instance with the data from the server
            remoteFile = fillOCFile((RemoteFile)folderAndFiles.get(i));
            remoteFile.setParentId(mLocalFolder.getFileId());

            /// retrieve local data for the read file
            //  localFile = mStorageManager.getFileByPath(remoteFile.getRemotePath());
            localFile = localFilesMap.remove(remoteFile.getRemotePath());

            /// add to the remoteFile (the new one) data about LOCAL STATE (not existing in server)
            remoteFile.setLastSyncDateForProperties(mCurrentSyncTime);
            if (localFile != null) {
                // some properties of local state are kept unmodified
                remoteFile.setFileId(localFile.getFileId());
                remoteFile.setKeepInSync(localFile.keepInSync());
                remoteFile.setLastSyncDateForData(localFile.getLastSyncDateForData());
                remoteFile.setModificationTimestampAtLastSyncForData(
                        localFile.getModificationTimestampAtLastSyncForData()
                );
                remoteFile.setStoragePath(localFile.getStoragePath());
                // eTag will not be updated unless contents are synchronized
                //  (Synchronize[File|Folder]Operation with remoteFile as parameter)
                remoteFile.setEtag(localFile.getEtag());
                if (remoteFile.isFolder()) {
                    remoteFile.setFileLength(localFile.getFileLength());
                        // TODO move operations about size of folders to FileContentProvider
                } else if (mRemoteFolderChanged && remoteFile.isImage() &&
                        remoteFile.getModificationTimestamp() != localFile.getModificationTimestamp()) {
                    remoteFile.setNeedsUpdateThumbnail(true);
                    Log.d(TAG, "Image " + remoteFile.getFileName() + " updated on the server");
                }
                remoteFile.setPublicLink(localFile.getPublicLink());
                remoteFile.setShareByLink(localFile.isShareByLink());
            } else {
                // remote eTag will not be updated unless contents are synchronized
                //  (Synchronize[File|Folder]Operation with remoteFile as parameter)
                remoteFile.setEtag("");
            }

            /// check and fix, if needed, local storage path
            checkAndFixForeignStoragePath(remoteFile);      // policy - local files are COPIED
                                                            // into the ownCloud local folder;
            searchForLocalFileInDefaultPath(remoteFile);    // legacy

            /// prepare content synchronization for kept-in-sync files
            if (remoteFile.keepInSync()) {
                SynchronizeFileOperation operation = new SynchronizeFileOperation(  localFile,
                                                                                    remoteFile,
                                                                                    mAccount,
                                                                                    true,
                                                                                    mContext
                                                                                    );

                filesToSyncContents.add(operation);
            }

            if (!remoteFile.isFolder()) {
                // Start file download
                requestForDownloadFile(remoteFile);
            } else {
                // Run new SyncFolderOperation for download children files recursively from a folder
                SynchronizeFolderOperation synchFolderOp =  new SynchronizeFolderOperation( mContext,
                        remoteFile.getRemotePath(),
                        mAccount,
                        mCurrentSyncTime);

                synchFolderOp.execute(mAccount, mContext, null, null);
            }

            updatedFiles.add(remoteFile);
        }

        // save updated contents in local database
        storageManager.saveFolder(remoteFolder, updatedFiles, localFilesMap.values());

        // request for the synchronization of file contents AFTER saving current remote properties
        startContentSynchronizations(filesToSyncContents, client);

        mChildren = updatedFiles;
    }

    /**
     * Performs a list of synchronization operations, determining if a download or upload is needed
     * or if exists conflict due to changes both in local and remote contents of the each file.
     *
     * If download or upload is needed, request the operation to the corresponding service and goes
     * on.
     *
     * @param filesToSyncContents       Synchronization operations to execute.
     * @param client                    Interface to the remote ownCloud server.
     */
    private void startContentSynchronizations(
            List<SynchronizeFileOperation> filesToSyncContents, OwnCloudClient client
        ) {
        RemoteOperationResult contentsResult = null;
        for (SynchronizeFileOperation op: filesToSyncContents) {
            contentsResult = op.execute(getStorageManager(), mContext);   // async
            if (!contentsResult.isSuccess()) {
                if (contentsResult.getCode() == ResultCode.SYNC_CONFLICT) {
                    mConflictsFound++;
                } else {
                    mFailsInFavouritesFound++;
                    if (contentsResult.getException() != null) {
                        Log_OC.e(TAG, "Error while synchronizing favourites : "
                                +  contentsResult.getLogMessage(), contentsResult.getException());
                    } else {
                        Log_OC.e(TAG, "Error while synchronizing favourites : "
                                + contentsResult.getLogMessage());
                    }
                }
            }   // won't let these fails break the synchronization process
        }
    }


    public boolean isMultiStatus(int status) {
        return (status == HttpStatus.SC_MULTI_STATUS);
    }

    /**
     * Creates and populates a new {@link com.owncloud.android.datamodel.OCFile} object with the data read from the server.
     *
     * @param remote    remote file read from the server (remote file or folder).
     * @return          New OCFile instance representing the remote resource described by we.
     */
    private OCFile fillOCFile(RemoteFile remote) {
        OCFile file = new OCFile(remote.getRemotePath());
        file.setCreationTimestamp(remote.getCreationTimestamp());
        file.setFileLength(remote.getLength());
        file.setMimetype(remote.getMimeType());
        file.setModificationTimestamp(remote.getModifiedTimestamp());
        file.setEtag(remote.getEtag());
        file.setPermissions(remote.getPermissions());
        file.setRemoteId(remote.getRemoteId());
        return file;
    }


    /**
     * Checks the storage path of the OCFile received as parameter.
     * If it's out of the local ownCloud folder, tries to copy the file inside it.
     *
     * If the copy fails, the link to the local file is nullified. The account of forgotten
     * files is kept in {@link #mForgottenLocalFiles}
     *)
     * @param file      File to check and fix.
     */
    private void checkAndFixForeignStoragePath(OCFile file) {
        String storagePath = file.getStoragePath();
        String expectedPath = FileStorageUtils.getDefaultSavePathFor(mAccount.name, file);
        if (storagePath != null && !storagePath.equals(expectedPath)) {
            /// fix storagePaths out of the local ownCloud folder
            File originalFile = new File(storagePath);
            if (FileStorageUtils.getUsableSpace(mAccount.name) < originalFile.length()) {
                mForgottenLocalFiles.put(file.getRemotePath(), storagePath);
                file.setStoragePath(null);

            } else {
                InputStream in = null;
                OutputStream out = null;
                try {
                    File expectedFile = new File(expectedPath);
                    File expectedParent = expectedFile.getParentFile();
                    expectedParent.mkdirs();
                    if (!expectedParent.isDirectory()) {
                        throw new IOException(
                                "Unexpected error: parent directory could not be created"
                        );
                    }
                    expectedFile.createNewFile();
                    if (!expectedFile.isFile()) {
                        throw new IOException("Unexpected error: target file could not be created");
                    }
                    in = new FileInputStream(originalFile);
                    out = new FileOutputStream(expectedFile);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0){
                        out.write(buf, 0, len);
                    }
                    file.setStoragePath(expectedPath);

                } catch (Exception e) {
                    Log_OC.e(TAG, "Exception while copying foreign file " + expectedPath, e);
                    mForgottenLocalFiles.put(file.getRemotePath(), storagePath);
                    file.setStoragePath(null);

                } finally {
                    try {
                        if (in != null) in.close();
                    } catch (Exception e) {
                        Log_OC.d(TAG, "Weird exception while closing input stream for "
                                + storagePath + " (ignoring)", e);
                    }
                    try {
                        if (out != null) out.close();
                    } catch (Exception e) {
                        Log_OC.d(TAG, "Weird exception while closing output stream for "
                                + expectedPath + " (ignoring)", e);
                    }
                }
            }
        }
    }


    /**
     * Scans the default location for saving local copies of files searching for
     * a 'lost' file with the same full name as the {@link com.owncloud.android.datamodel.OCFile} received as
     * parameter.
     *  
     * @param file      File to associate a possible 'lost' local file.
     */
    private void searchForLocalFileInDefaultPath(OCFile file) {
        if (file.getStoragePath() == null && !file.isFolder()) {
            File f = new File(FileStorageUtils.getDefaultSavePathFor(mAccount.name, file));
            if (f.exists()) {
                file.setStoragePath(f.getAbsolutePath());
                file.setLastSyncDateForData(f.lastModified());
            }
        }
    }

    /**
     * Requests for a download to the FileDownloader service
     *
     * @param file     OCFile object representing the file to download
     */
    private void requestForDownloadFile(OCFile file) {
        Intent i = new Intent(mContext, FileDownloader.class);
        i.putExtra(FileDownloader.EXTRA_ACCOUNT, mAccount);
        i.putExtra(FileDownloader.EXTRA_FILE, file);
        mContext.startService(i);
    }

    public boolean getRemoteFolderChanged() {
        return mRemoteFolderChanged;
    }

}
