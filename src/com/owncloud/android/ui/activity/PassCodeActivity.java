/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2011 Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
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

import java.util.Arrays;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.DisplayUtils;

public class PassCodeActivity extends SherlockFragmentActivity {


    private static final String TAG = PassCodeActivity.class.getSimpleName();

    public final static String ACTION_ENABLE = PassCodeActivity.class.getCanonicalName() + ".ENABLE";
    public final static String ACTION_DISABLE = PassCodeActivity.class.getCanonicalName() + ".DISABLE";
    public final static String ACTION_REQUEST = PassCodeActivity.class.getCanonicalName()  + ".REQUEST";

    private Button mBCancel;
    private TextView mPassCodeHdr;
    private TextView mPassCodeHdrExplanation;
    private EditText mText0;
    private EditText mText1;
    private EditText mText2;
    private EditText mText3;
    
    private String [] mPassCodeDigits = {"","","",""};
    private boolean mConfirmingPassCode = false;

    private boolean mBChange = true; // to control that only one blocks jump


    /**
     * Initializes the activity.
     *
     * An intent with a valid ACTION is expected; if none is found, an {@link IllegalArgumentException} will be thrown.
     *
     * @param savedInstanceState    Previously saved state - irrelevant in this case
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passcodelock);
        
        mBCancel = (Button) findViewById(R.id.cancel);
        mPassCodeHdr = (TextView) findViewById(R.id.header);
        mPassCodeHdrExplanation = (TextView) findViewById(R.id.explanation);
        mText0 = (EditText) findViewById(R.id.txt0);
        mText0.requestFocus();
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mText1 = (EditText) findViewById(R.id.txt1);
        mText2 = (EditText) findViewById(R.id.txt2);
        mText3 = (EditText) findViewById(R.id.txt3);

        if (ACTION_REQUEST.equals(getIntent().getAction())) {
            /// this is a pass code request; the user has to input the right value
            mPassCodeHdr.setText(R.string.pass_code_enter_pass_code);
            mPassCodeHdrExplanation.setVisibility(View.INVISIBLE);
            setCancelButtonEnabled(false);      // no option to cancel

        } else if (ACTION_ENABLE.equals(getIntent().getAction())) {
            /// pass code preference has just been activated in Preferences; will receive and confirm pass code value
            mPassCodeHdr.setText(R.string.pass_code_configure_your_pass_code);
            //mPassCodeHdr.setText(R.string.pass_code_enter_pass_code); // TODO choose a header, check iOS
            mPassCodeHdrExplanation.setVisibility(View.VISIBLE);
            setCancelButtonEnabled(true);

        } else if (ACTION_DISABLE.equals(getIntent().getAction())) {
            /// pass code preference has just been disabled in Preferences;
            // will confirm user knows pass code, then remove it
            mPassCodeHdr.setText(R.string.pass_code_remove_your_pass_code);
            mPassCodeHdrExplanation.setVisibility(View.INVISIBLE);
            setCancelButtonEnabled(true);

        } else {
            throw new IllegalArgumentException("A valid ACTION is needed in the Intent passed to " + TAG);
        }

        setTextListeners();
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setIcon(DisplayUtils.getSeasonalIconId());
    }
    

    protected void setCancelButtonEnabled(boolean enabled){
        if(enabled){
            mBCancel.setVisibility(View.VISIBLE);
            mBCancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    revertActionAndExit();
                }
            });
        } else {
            mBCancel.setVisibility(View.GONE);
            mBCancel.setVisibility(View.INVISIBLE);
            mBCancel.setOnClickListener(null);
        }
    
    }
    
    
    /*
     *  
     */
    protected void setTextListeners(){
    
        /*------------------------------------------------
         *  FIRST BOX
         -------------------------------------------------*/
        
        mText0.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    if (!mConfirmingPassCode) {
                        mPassCodeDigits[0] = mText0.getText().toString();
                    }
                    mText1.requestFocus();
                } else {
                    Log_OC.w(TAG, "Input in text box 0 resulted in empty string");
                }
            }
        });
        
        

        /*------------------------------------------------
         *  SECOND BOX 
         -------------------------------------------------*/
        mText1.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    if (!mConfirmingPassCode) {
                        mPassCodeDigits[1] = mText1.getText().toString();
                    }
                    mText2.requestFocus();
                } else {
                    Log_OC.w(TAG, "Input in text box 1 resulted in empty string");
                }
            }
        });
 
        mText1.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && mBChange) {  // TODO WIP: event should be used to control what's exactly happening with DEL, not any custom field...
                    mText0.setText("");
                    mText0.requestFocus();
                    if (!mConfirmingPassCode)
                        mPassCodeDigits[0] = "";    // TODO WIP: what is this for??
                    mBChange = false;

                } else if (!mBChange) {
                    mBChange = true;
                }
                return false;
            }
        });        
 
        mText1.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mText1.setCursorVisible(true);      // TODO WIP this could be made static, or just nothing, since default is true...
                if (mText0.getText().toString().equals("")) {    // TODO WIP is this really needed? when?
                    mText1.setSelected(false);
                    mText1.setCursorVisible(false); // TODO WIP really this is a problem?
                    mText0.requestFocus();  // TODO WIP how many focus requests do we need?
                    mText0.setSelected(true);   // TODO WIP what is this for?
                    mText0.setSelection(0);     // TODO WIP what is THIS for?
                }

            }
        });
        
        
        /*------------------------------------------------
         *  THIRD BOX
         -------------------------------------------------*/
        /// TODO WIP yeah, let's repeat all the code again...
        mText2.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    if (!mConfirmingPassCode) {
                        mPassCodeDigits[2] = mText2.getText().toString();
                    }
                    mText3.requestFocus();
                } else {
                    Log_OC.w(TAG, "Input in text box 2 resulted in empty string");
                }
            }
        });
        
        mText2.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && mBChange) {
                    mText1.requestFocus();
                    if (!mConfirmingPassCode)
                        mPassCodeDigits[1] = "";
                    mText1.setText("");
                    mBChange = false;

                } else if (!mBChange) {
                    mBChange = true;

                }
                return false;
            }
        });

        mText2.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                /// TODO WIP: hasFocus is there for some reason; for instance, doing NOTHING if this is not my business, instead of considering all the possible cases in every edit text
                mText2.setCursorVisible(true);
                if (mText0.getText().toString().equals("")) {
                    mText2.setSelected(false);
                    mText2.setCursorVisible(false);
                    mText0.requestFocus();
                    mText0.setSelected(true);
                    mText0.setSelection(0);
                } else if (mText1.getText().toString().equals("")) {
                    mText2.setSelected(false);
                    mText2.setCursorVisible(false);
                    mText1.requestFocus();
                    mText1.setSelected(true);
                    mText1.setSelection(0);
                }

            }
        });


        /*------------------------------------------------
         *  FOURTH BOX
         -------------------------------------------------*/
        mText3.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {

                    if (!mConfirmingPassCode) {
                        mPassCodeDigits[3] = mText3.getText().toString();
                    }
                    mText0.requestFocus();

                    processFullPassCode();

                } else {
                    Log_OC.w(TAG, "Input in text box 3 resulted in empty string");
                }
            }
        });


        mText3.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && mBChange) {
                    mText2.requestFocus();
                    if (!mConfirmingPassCode)
                        mPassCodeDigits[2] = "";
                    mText2.setText("");
                    mBChange = false;

                } else if (!mBChange) {
                    mBChange = true;
                }
                return false;
            }
        });

        mText3.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mText3.setCursorVisible(true);

                if (mText0.getText().toString().equals("")) {
                    mText3.setSelected(false);
                    mText3.setCursorVisible(false);
                    mText0.requestFocus();
                    mText0.setSelected(true);
                    mText0.setSelection(0);
                } else if (mText1.getText().toString().equals("")) {
                    mText3.setSelected(false);
                    mText3.setCursorVisible(false);
                    mText1.requestFocus();
                    mText1.setSelected(true);
                    mText1.setSelection(0);
                } else if (mText2.getText().toString().equals("")) {
                    mText3.setSelected(false);
                    mText3.setCursorVisible(false);
                    mText2.requestFocus();
                    mText2.setSelected(true);
                    mText2.setSelection(0);
                }

            }
        });
        
        
        
    } // end setTextListener


    /**
     * Processes the pass code entered by the user just after the last digit was in.
     *
     * Takes into account the action requested to the activity, the currently saved pass code and the previously
     * typed pass code, if any.
     */
    private void processFullPassCode() {
        if (ACTION_REQUEST.equals(getIntent().getAction())) {
            if (checkPassCode()) {
                /// pass code accepted in request, user is allowed to access the app
                finish();

            }  else {
                showErrorAndRestart(R.string.common_error, R.string.pass_code_enter_pass_code, View.INVISIBLE);
                    /// TODO better error message
            }

        } else if (ACTION_DISABLE.equals(getIntent().getAction())) {
            if (checkPassCode()) {
                /// pass code accepted when disabling, pass code is removed
                SharedPreferences.Editor appPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext()).edit();
                appPrefs.putBoolean("set_pincode", false);  // TODO remove; this should be unnecessary, was done before entering in the activity
                appPrefs.commit();

                Toast.makeText(PassCodeActivity.this, R.string.pass_code_removed, Toast.LENGTH_LONG).show();
                finish();

            } else {
                showErrorAndRestart(R.string.common_error, R.string.pass_code_enter_pass_code, View.INVISIBLE);
                    /// TODO better error message
            }

        } else if (ACTION_ENABLE.equals(getIntent().getAction())) {
            /// enabling pass code
            if (!mConfirmingPassCode) {
                requestPassCodeConfirmation();

            } else if (confirmPassCode()) {
                /// confirmed: user typed the same pass code twice
                savePassCodeAndExit();

            } else {
                showErrorAndRestart(
                        R.string.pass_code_mismatch, R.string.pass_code_configure_your_pass_code, View.VISIBLE
                );
            }
        }
    }

    
    private void showErrorAndRestart(int errorMessage, int headerMessage, int explanationVisibility) {
        Arrays.fill(mPassCodeDigits, null);
        CharSequence errorSeq = getString(errorMessage);
        Toast.makeText(this, errorSeq, Toast.LENGTH_LONG).show();
        mPassCodeHdr.setText(headerMessage);                // TODO check if really needed
        mPassCodeHdrExplanation.setVisibility(explanationVisibility);  // TODO check if really needed
        clearBoxes();
    }


    /**
     * Ask to the user for retyping the pass code just entered before saving it as the current pass code.
     */
    protected void requestPassCodeConfirmation(){
        clearBoxes();
        mPassCodeHdr.setText(R.string.pass_code_reenter_your_pass_code);
        mPassCodeHdrExplanation.setVisibility(View.INVISIBLE);
        mConfirmingPassCode = true;
    }

    /**
     * Compares pass code entered by the user with the value currently saved in the app.
     *
     * @return     'True' if entered pass code equals to the saved one.
     */
    protected boolean checkPassCode(){
        SharedPreferences appPrefs = PreferenceManager
            .getDefaultSharedPreferences(getApplicationContext());

        String savedPassCodeDigits[] = new String[4];
        savedPassCodeDigits[0] = appPrefs.getString("PrefPinCode1", null);
        savedPassCodeDigits[1] = appPrefs.getString("PrefPinCode2", null);
        savedPassCodeDigits[2] = appPrefs.getString("PrefPinCode3", null);
        savedPassCodeDigits[3] = appPrefs.getString("PrefPinCode4", null);

        return (
            mPassCodeDigits[0].equals(savedPassCodeDigits[0]) &&
            mPassCodeDigits[1].equals(savedPassCodeDigits[0]) &&
            mPassCodeDigits[2].equals(savedPassCodeDigits[0]) &&
            mPassCodeDigits[3].equals(savedPassCodeDigits[0])
        );
    }

    /**
     * Compares pass code retyped by the user with the value entered just before.
     *
     * @return     'True' if retyped pass code equals to the entered before.
     */
    protected boolean confirmPassCode(){
        mConfirmingPassCode = false;

        String retypedPassCodeDigits[] = new String[4];
        retypedPassCodeDigits[0] = mText0.getText().toString();
        retypedPassCodeDigits[1] = mText1.getText().toString();
        retypedPassCodeDigits[2] = mText2.getText().toString();
        retypedPassCodeDigits[3] = mText3.getText().toString();

        return (
            mPassCodeDigits[0].equals(retypedPassCodeDigits[0]) &&
            mPassCodeDigits[1].equals(retypedPassCodeDigits[0]) &&
            mPassCodeDigits[2].equals(retypedPassCodeDigits[0]) &&
            mPassCodeDigits[3].equals(retypedPassCodeDigits[0])
        );
    }

    /**
     * Sets the input fields to empty strings and puts the focus on the first one.
     */
    protected void clearBoxes(){
        mText0.setText("");
        mText1.setText("");
        mText2.setText("");
        mText3.setText("");
        mText0.requestFocus();
    }

    /**
     * Overrides click on the BACK arrow to correctly cancel ACTION_ENABLE or ACTION_DISABLE, while preventing
     * than ACTION_REQUEST may be worked around.
     *
     * @param keyCode       Key code of the key that triggered the down event.
     * @param event         Event triggered.
     * @return              'True' when the key event was processed by this method.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount()== 0){
            if (ACTION_ENABLE.equals(getIntent().getAction()) || ACTION_DISABLE.equals(getIntent().getAction())) {
                revertActionAndExit();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Saves the pass code input by the user as the current pass code.
     */
    protected void savePassCodeAndExit() {
        SharedPreferences.Editor appPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()).edit();
        
        appPrefs.putString("PrefPinCode1", mPassCodeDigits[0]);
        appPrefs.putString("PrefPinCode2", mPassCodeDigits[1]);
        appPrefs.putString("PrefPinCode3", mPassCodeDigits[2]);
        appPrefs.putString("PrefPinCode4", mPassCodeDigits[3]);
        appPrefs.putBoolean("set_pincode", true);    /// TODO remove; unnecessary, Preferences did it before entering here
        appPrefs.commit();

        Toast.makeText(this, R.string.pass_code_stored, Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Cancellation of ACTION_ENABLE or ACTION_DISABLE; reverts the enable or disable action done by
     * {@link Preferences}, then finishes.
     */
    protected void revertActionAndExit() {
        SharedPreferences.Editor appPrefsE = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()).edit();

        SharedPreferences appPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        boolean state = appPrefs.getBoolean("set_pincode", false);
        appPrefsE.putBoolean("set_pincode", !state);
        // TODO WIP: this is reverting the value of the preference because it was changed BEFORE entering
        // TODO         in this activity; was the PreferenceCheckBox in the caller who did it
        appPrefsE.commit();
        finish();
    }

}
