/* ownCloud Android client application
 *   Copyright (C) 2012-2015 ownCloud Inc.
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
package com.owncloud.android.authentication;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.owncloud.android.MainApp;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.accounts.AccountTypeUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;

import java.lang.ref.WeakReference;


/**
 * Async Task to verify the credentials of a user
 *
 * @author masensio on 09/02/2015.
 */
public class AuthenticatorAsyncTask  extends AsyncTask<String, Void, RemoteOperationResult> {

    private static String REMOTE_PATH = "/";
    private static boolean SUCCESS_IF_ABSENT = false;

    private Context mContext;
    private final WeakReference<OnAuthenticatorTaskListener> mListener;
    protected Activity mActivity;

    public AuthenticatorAsyncTask(Activity activity) {
        mContext = activity.getApplicationContext();
        mListener = new WeakReference<OnAuthenticatorTaskListener>((OnAuthenticatorTaskListener)activity);
        mActivity = activity;
    }

    @Override
    protected RemoteOperationResult doInBackground(String... params) {

        RemoteOperationResult result;
        if (params!= null && params.length==5) {
            String url = params[0];
            String username = params[1];
            String password = params[2];
            String authToken = params[3];
            String authTokenType = params[4];

            // Client
            String basic = AccountTypeUtils.getAuthTokenTypePass(MainApp.getAccountType());
            String oAuth = AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.getAccountType());
            String saml =  AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType());

            Uri uri = Uri.parse(url);
            OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(uri, mContext, false);
            OwnCloudCredentials credentials = null;
            if (authTokenType.equals(basic)) {
                credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                        username, password); // basic

            } else if (authTokenType.equals(oAuth)) {
                credentials = OwnCloudCredentialsFactory.newBearerCredentials(
                        authToken);  // bearer token

            } else if (authTokenType.equals(saml)) {
                credentials = OwnCloudCredentialsFactory.newSamlSsoCredentials(
                        authToken); // SAML SSO
            }

            client.setCredentials(credentials);

            // Operation
            ExistenceCheckRemoteOperation operation = new ExistenceCheckRemoteOperation(REMOTE_PATH,
                    mContext, SUCCESS_IF_ABSENT);
            result = operation.execute(client);

        } else {
            result = new RemoteOperationResult(RemoteOperationResult.ResultCode.UNKNOWN_ERROR);
        }

        return result;
    }

    @Override
    protected void onPostExecute(RemoteOperationResult result) {

        if (result!= null)
        {
            OnAuthenticatorTaskListener listener = mListener.get();
            if (listener!= null)
            {
                listener.onAuthenticatorTaskCallback(result);
            }
        }
    }
    /*
     * Interface to retrieve data from recognition task
     */
    public interface OnAuthenticatorTaskListener{

        void onAuthenticatorTaskCallback(RemoteOperationResult result);
    }
}
