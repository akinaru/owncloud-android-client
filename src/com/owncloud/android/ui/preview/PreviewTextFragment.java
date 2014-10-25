package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.ui.dialog.RemoveFileDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

public class PreviewTextFragment extends FileFragment {
    private static final String EXTRA_FILE = "FILE";
    private static final String EXTRA_ACCOUNT = "ACCOUNT";
    private static final String TAG = PreviewTextFragment.class.getSimpleName();

    private Account mAccount;
    private ListView mTextPreviewList;

    /**
     * Creates an empty fragment for previews.
     * <p/>
     * MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically
     * (for instance, when the device is turned a aside).
     * <p/>
     * DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful
     * construction
     */
    public PreviewTextFragment() {
        super();
        mAccount = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log_OC.e(TAG, "onCreateView");


        View ret = inflater.inflate(R.layout.text_file_preview, container, false);

        mTextPreviewList = (ListView) ret.findViewById(R.id.text_preview_list);
        mTextPreviewList.setAdapter(new TextLineAdapter());

        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OCFile file = getFile();

        Bundle args = getArguments();

        if (file == null)
            file = args.getParcelable(FileDisplayActivity.EXTRA_FILE);

        if (mAccount == null)
            mAccount = args.getParcelable(FileDisplayActivity.EXTRA_ACCOUNT);


        if (savedInstanceState == null) {
            if (file == null) {
                throw new IllegalStateException("Instanced with a NULL OCFile");
            }
            if (mAccount == null) {
                throw new IllegalStateException("Instanced with a NULL ownCloud Account");
            }
        } else {
            file = savedInstanceState.getParcelable(EXTRA_FILE);
            mAccount = savedInstanceState.getParcelable(EXTRA_ACCOUNT);
        }
        setFile(file);
        setHasOptionsMenu(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PreviewImageFragment.EXTRA_FILE, getFile());
        outState.putParcelable(PreviewImageFragment.EXTRA_ACCOUNT, mAccount);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log_OC.e(TAG, "onStart");
    }

    private void loadAndShowTextPreview() {
        new TextLoadAsyncTask().execute(getFile().getStoragePath());
    }

    /**
     * Reads the file to preview and shows its contents. Too critical to be anonymous.
     */
    private class TextLoadAsyncTask extends AsyncTask<Object, StringWriter, Void> {
        private int TEXTVIEW_WIDTH;
        private float TEXTVIEW_SIZE;
        private final Queue<Character> accumulatedText = new LinkedList<Character>();
        private final String DIALOG_WAIT_TAG = "DIALOG_WAIT";
        private final Rect bounds = new Rect();
        private final Paint paint = new Paint();

        @SuppressLint("InflateParams")
        @Override
        protected void onPreExecute() {
            ((TextLineAdapter) mTextPreviewList.getAdapter()).clear();
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            TEXTVIEW_WIDTH = displaymetrics.widthPixels;
            TEXTVIEW_SIZE = ((TextView) ((LayoutInflater) getActivity().getApplicationContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(
                            R.layout.text_file_preview_list_item, null)).getTextSize();
            showLoadingDialog();
            paint.setTextSize(TEXTVIEW_SIZE);
        }

        @Override
        protected Void doInBackground(java.lang.Object... params) {
            if (params.length != 1)
                throw new IllegalArgumentException("The parameter to " + TextLoadAsyncTask.class.getName() + " must be the file location only");
            final String location = (String) params[0];

            FileInputStream inputStream = null;
            Scanner sc = null;
            try {
                inputStream = new FileInputStream(location);
                sc = new Scanner(inputStream);
                while (sc.hasNextLine()) {
                    StringWriter target = new StringWriter();
                    BufferedWriter bufferedWriter = new BufferedWriter(target);
                    if (sc.hasNextLine())
                        bufferedWriter.write(sc.nextLine());
                    bufferedWriter.close();
                    publishProgress(target);
                }
                IOException exc = sc.ioException();
                if (exc != null) throw exc;
            } catch (IOException e) {
                finish();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        finish();
                    }
                }
                if (sc != null) {
                    sc.close();
                }
            }
            //Add the remaining text, if any
            while (!accumulatedText.isEmpty()) {
                addLine();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(StringWriter... values) {
            super.onProgressUpdate(values);
            final char[] newTextAsCharArray = values[0].toString().toCharArray();
            for (char c : newTextAsCharArray) {
                accumulatedText.add(c);
            }
            addLine();
        }

        private synchronized void addLine() {
            StringBuilder textForThisLine = new StringBuilder();
            do {
                Character polled = accumulatedText.poll();
                textForThisLine.append(polled);
            }
            while (!isTooLarge(textForThisLine.toString()) && !accumulatedText.isEmpty());
            String line = textForThisLine.toString();
            ((TextLineAdapter) mTextPreviewList.getAdapter()).add(line.contentEquals("null") ? "" : line);
        }

        private boolean isTooLarge(String text) {
            paint.getTextBounds(text, 0, text.length(), bounds);
            int lineWidth = (int) Math.ceil(bounds.width());
            return lineWidth / TEXTVIEW_WIDTH > 1;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mTextPreviewList.setVisibility(View.VISIBLE);
            dismissLoadingDialog();
        }

        /**
         * Show loading dialog
         */
        public void showLoadingDialog() {
            // Construct dialog
            LoadingDialog loading = new LoadingDialog(getResources().getString(R.string.wait_a_moment));
            FragmentManager fm = getActivity().getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            loading.show(ft, DIALOG_WAIT_TAG);
        }

        /**
         * Dismiss loading dialog
         */
        public void dismissLoadingDialog() {
            Fragment frag = getActivity().getSupportFragmentManager().findFragmentByTag(DIALOG_WAIT_TAG);
            if (frag != null) {
                LoadingDialog loading = (LoadingDialog) frag;
                loading.dismiss();
            }
        }
    }

    private class TextLineAdapter extends BaseAdapter {
        private static final int LIST_ITEM_LAYOUT = R.layout.text_file_preview_list_item;
        private final List<String> items = new ArrayList<String>();

        private void add(String line) {
            items.add(line);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        private void clear() {
            items.clear();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public String getItem(int position) {
            if (position >= items.size())
                throw new IllegalArgumentException();
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView =
                        ((LayoutInflater) getActivity().getApplicationContext()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                                .inflate(
                                        LIST_ITEM_LAYOUT, null);
                viewHolder = new ViewHolder();
                viewHolder.setLineView((TextView) convertView.findViewById(R.id.text_preview));
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.getLineView().setText(items.get(position));

            return convertView;
        }
    }

    private static class ViewHolder {
        private TextView lineView;

        private ViewHolder() {
        }

        public TextView getLineView() {
            return lineView;
        }

        public void setLineView(TextView lineView) {
            this.lineView = lineView;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.file_actions_menu, menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mContainerActivity.getStorageManager() != null) {
            FileMenuFilter mf = new FileMenuFilter(
                    getFile(),
                    mContainerActivity.getStorageManager().getAccount(),
                    mContainerActivity,
                    getSherlockActivity()
            );
            mf.filter(menu);
        }

        // additional restriction for this fragment
        MenuItem item = menu.findItem(R.id.action_rename_file);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_move);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // this one doesn't make sense since the file has to be down in order to be previewed
        item = menu.findItem(R.id.action_download_file);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        item = menu.findItem(R.id.action_settings);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        item = menu.findItem(R.id.action_logger);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        item = menu.findItem(R.id.action_sync_file);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        item = menu.findItem(R.id.action_sync_account);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share_file: {
                mContainerActivity.getFileOperationsHelper().shareFileWithLink(getFile());
                return true;
            }
            case R.id.action_unshare_file: {
                mContainerActivity.getFileOperationsHelper().unshareFileWithLink(getFile());
                return true;
            }
            case R.id.action_open_file_with: {
                openFile();
                return true;
            }
            case R.id.action_remove_file: {
                RemoveFileDialogFragment dialog = RemoveFileDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_see_details: {
                seeDetails();
                return true;
            }
            case R.id.action_send_file: {
                sendFile();
                return true;
            }
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }

            default:
                return false;
        }
    }

    /**
     * Update the file of the fragment with file value
     *
     * @param file The new file to set
     */
    public void updateFile(OCFile file) {
        setFile(file);
    }

    private void sendFile() {
        mContainerActivity.getFileOperationsHelper().sendDownloadedFile(getFile());
    }

    private void seeDetails() {
        mContainerActivity.showDetails(getFile());
    }

    @Override
    public void onPause() {
        Log_OC.e(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log_OC.e(TAG, "onResume");

        loadAndShowTextPreview();
    }

    @Override
    public void onDestroy() {
        Log_OC.e(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log_OC.e(TAG, "onStop");
    }

    /**
     * Opens the previewed file with an external application.
     */
    private void openFile() {
        mContainerActivity.getFileOperationsHelper().openFile(getFile());
        finish();
    }

    /**
     * Helper method to test if an {@link OCFile} can be passed to a {@link PreviewTextFragment} to be previewed.
     *
     * @param file File to test if can be previewed.
     * @return 'True' if the file can be handled by the fragment.
     */
    public static boolean canBePreviewed(OCFile file) {
        return (file != null && file.isDown() && file.isText());
    }

    /**
     * Finishes the preview
     */
    private void finish() {
        getSherlockActivity().onBackPressed();
    }
}
