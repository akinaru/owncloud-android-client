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

package com.owncloud.android.authentication;

import java.io.ByteArrayInputStream;
import java.lang.ref.WeakReference;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.utils.Log_OC;


/**
 * Custom {@link WebViewClient} client aimed to catch the end of a single-sign-on process 
 * running in the {@link WebView} that is attached to.
 * 
 * Assumes that the single-sign-on is kept thanks to a cookie set at the end of the
 * authentication process.
 *   
 * @author David A. Velasco
 */
public class SsoWebViewClient extends WebViewClient {
        
    private static final String TAG = SsoWebViewClient.class.getSimpleName();
    
    public interface SsoWebViewClientListener {
        public void onSsoFinished(String sessionCookie);
    }
    
    private Context mContext;
    private Handler mListenerHandler;
    private WeakReference<SsoWebViewClientListener> mListenerRef;
    private String mTargetUrl;
    private String mLastReloadedUrlAtError;
    
    public SsoWebViewClient (Context context, Handler listenerHandler, SsoWebViewClientListener listener) {
        mContext = context;
        mListenerHandler = listenerHandler;
        mListenerRef = new WeakReference<SsoWebViewClient.SsoWebViewClientListener>(listener);
        mTargetUrl = "fake://url.to.be.set";
        mLastReloadedUrlAtError = null;
    }
    
    public String getTargetUrl() {
        return mTargetUrl;
    }
    
    public void setTargetUrl(String targetUrl) {
        mTargetUrl = targetUrl;
    }

    @Override
    public void onPageStarted (WebView view, String url, Bitmap favicon) {
        Log_OC.d(TAG, "onPageStarted : " + url);
        super.onPageStarted(view, url, favicon);
    }
    
    @Override
    public void onFormResubmission (WebView view, Message dontResend, Message resend) {
        Log_OC.d(TAG, "onFormResubMission ");

        // necessary to grant reload of last page when device orientation is changed after sending a form
        resend.sendToTarget();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return false;
    }
    
    @Override
    public void onReceivedError (WebView view, int errorCode, String description, String failingUrl) {
        Log_OC.e(TAG, "onReceivedError : " + failingUrl + ", code " + errorCode + ", description: " + description);
        if (!failingUrl.equals(mLastReloadedUrlAtError)) {
            view.reload();
            mLastReloadedUrlAtError = failingUrl;
        } else {
            mLastReloadedUrlAtError = null;
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }
    
    @Override
    public void onPageFinished (WebView view, String url) {
        Log_OC.d(TAG, "onPageFinished : " + url);
        mLastReloadedUrlAtError = null;
        if (url.startsWith(mTargetUrl)) {
            view.setVisibility(View.GONE);
            CookieManager cookieManager = CookieManager.getInstance();
            final String cookies = cookieManager.getCookie(url);
            Log_OC.d(TAG, "Cookies: " + cookies);
            if (mListenerHandler != null && mListenerRef != null) {
                // this is good idea because onPageFinished is not running in the UI thread
                mListenerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        SsoWebViewClientListener listener = mListenerRef.get();
                        if (listener != null) {
                        	// Send Cookies to the listener
                            listener.onSsoFinished(cookies);
                        }
                    }
                });
            }
        } 
    }
    
    
    @Override
    public void doUpdateVisitedHistory (WebView view, String url, boolean isReload) {
        Log_OC.d(TAG, "doUpdateVisitedHistory : " + url);
    }
    
    @Override
    public void onReceivedSslError (final WebView view, final SslErrorHandler handler, SslError error) {
        Log_OC.d(TAG, "onReceivedSslError : " + error);
        // Test 1
        X509Certificate x509Certificate = getX509CertificateFromError(error);
        boolean isKnownServer = false;
        
        if (x509Certificate != null) {
            Log_OC.d(TAG, "------>>>>> x509Certificate " + x509Certificate.toString());
            
            try {
                isKnownServer = NetworkUtils.isCertInKnownServersStore((Certificate) x509Certificate, mContext);
            } catch (Exception e) {
                Log_OC.e(TAG, "Exception: " + e.getMessage());
            }
        }
        
         if (isKnownServer) {
             handler.proceed();
         } else {
             ((AuthenticatorActivity)mContext).showUntrustedCertDialog(x509Certificate, error, handler);
         }
    }
    
    /**
     * Obtain the X509Certificate from SslError
     * @param   error     SslError
     * @return  X509Certificate from error
     */
    public X509Certificate getX509CertificateFromError (SslError error) {
        Bundle bundle = SslCertificate.saveState(error.getCertificate());
        X509Certificate x509Certificate;
        byte[] bytes = bundle.getByteArray("x509-certificate");
        if (bytes == null) {
            x509Certificate = null;
        } else {
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(bytes));
                x509Certificate = (X509Certificate) cert;
            } catch (CertificateException e) {
                x509Certificate = null;
            }
        }        
        return x509Certificate;
    }
    
    @Override
    public void onReceivedHttpAuthRequest (WebView view, HttpAuthHandler handler, String host, String realm) {
        Log_OC.d(TAG, "onReceivedHttpAuthRequest : " + host);
//        Toast.makeText(mContext, "onReceivedHttpAuthRequest : " + host, Toast.LENGTH_LONG).show();

        createAuthenticationDialog(view, handler);

    }

    @Override
    public WebResourceResponse shouldInterceptRequest (WebView view, String url) {
        Log_OC.d(TAG, "shouldInterceptRequest : " + url);
        return null;
    }
    
    @Override
    public void onLoadResource (WebView view, String url) {
        Log_OC.d(TAG, "onLoadResource : " + url);   
    }
    
    @Override
    public void onReceivedLoginRequest (WebView view, String realm, String account, String args) {
        Log_OC.d(TAG, "onReceivedLoginRequest : " + realm + ", " + account + ", " + args);
    }
    
    @Override
    public void onScaleChanged (WebView view, float oldScale, float newScale) {
        Log_OC.d(TAG, "onScaleChanged : " + oldScale + " -> " + newScale);
        super.onScaleChanged(view, oldScale, newScale);
    }

    @Override
    public void onUnhandledKeyEvent (WebView view, KeyEvent event) {
        Log_OC.d(TAG, "onUnhandledKeyEvent : " + event);
    }
    
    @Override
    public boolean shouldOverrideKeyEvent (WebView view, KeyEvent event) {
        Log_OC.d(TAG, "shouldOverrideKeyEvent : " + event);
        return false;
    }

    /**
     * Create dialog for request authentication to the user
     * @param webView
     * @param handler
     */
    private void createAuthenticationDialog(WebView webView, HttpAuthHandler handler) {
        final WebView mWebView = webView;
        final HttpAuthHandler mHandler = handler;

        // Create field for username
        final EditText usernameET = new EditText(mContext);
        usernameET.setHint(mContext.getText(R.string.auth_username));

        // Create field for password
        final EditText passwordET = new EditText(mContext);
        passwordET.setHint(mContext.getText(R.string.auth_password));
        passwordET.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // Prepare LinearLayout for dialog
        LinearLayout ll = new LinearLayout(mContext);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(usernameET);
        ll.addView(passwordET);

        Builder authDialog = new AlertDialog
                .Builder(mContext)
                .setTitle(mContext.getText(R.string.saml_authentication_required_text))
                .setView(ll)
                .setCancelable(false)
                .setPositiveButton(mContext.getText(R.string.common_ok),
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        String username = usernameET.getText().toString().trim();
                        String password = passwordET.getText().toString().trim();

                        // Proceed with the authentication
                        mHandler.proceed(username, password);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(mContext.getText(R.string.common_cancel),
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        mWebView.stopLoading();
                    }
                });

        if (mWebView!=null) {
            authDialog.show();
        }

    }
}
