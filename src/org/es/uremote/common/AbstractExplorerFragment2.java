package org.es.uremote.common;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import org.es.uremote.R;
import org.es.uremote.components.ExplorerAdapter;
import org.es.uremote.utils.IntentKeys;
import org.es.utils.FileUtils;
import org.es.utils.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.es.uremote.exchange.ExchangeMessages.DirContent.File.FileType.DIRECTORY;

/**
 * File explorer fragment.<br />
 * This fragment allow you to browse a list of files.
 *
 * @author Cyril Leroux
 * Created on 03/09/13.
 */
public abstract class AbstractExplorerFragment2 extends ListFragment {

	private static final String TAG = "AbstractExplorerFragment";

	private static final String PREVIOUS_DIRECTORY_PATH	= "..";
	private static final String KEY_DIRECTORY_CONTENT	= "DIRECTORY_CONTENT";

	private TextView mTvPath;
	protected List<File> mFiles		= null;
	protected String mCurrentPath	= null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View view = inflater.inflate(R.layout.server_frag_explorer, container, false);
		mTvPath = (TextView) view.findViewById(R.id.tvPath);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		List<File> files = null;
		String path = null;

		// Restoring current directory content
		if (savedInstanceState != null) {
			path = savedInstanceState.getString(KEY_DIRECTORY_CONTENT);
		}

		// Get the directory content or update the one that already exist.
		if (files == null) {
			path = getActivity().getIntent().getStringExtra(IntentKeys.DIRECTORY_PATH);
			navigateTo(path);
		} else {
			updateView(path, files);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (mCurrentPath != null) {
			outState.putString(KEY_DIRECTORY_CONTENT, mCurrentPath);
		}
		if (mFiles != null) {
			// Parcelable Files
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		final File file = mFiles.get(position);
		final String filename = file.getName();
		final String fullPath = file.getAbsolutePath();

		if (file.isDirectory()) {

			if (PREVIOUS_DIRECTORY_PATH.equals(filename)) {
				navigateUp();

			} else {
				onDirectoryClick(fullPath);
			}

		} else {
			onFileClick(fullPath);
		}
	}

	/**
	 * Updates the view with the content passed directory.
	 *
	 * @param files The object that represents the directory content.
	 */
	protected void updateView(final String dirPath, final List<File> files) {

		mFiles = files;

		if (files.size() == 0) {
			Log.warning(TAG, "#updateView - No file in the directory.");
			return;
		}


		if (getListAdapter() == null) {
			final ExplorerAdapter2 adapter = new ExplorerAdapter2(getActivity().getApplicationContext(), files);
			setListAdapter(adapter);
		} else {
			((ExplorerAdapter2) getListAdapter()).clear();
			((ExplorerAdapter2) getListAdapter()).addAll(files);
		}

		((ExplorerAdapter2) getListAdapter()).notifyDataSetChanged();
		getListView().invalidate();
		getListView().invalidateViews();

		mTvPath.setText(dirPath);
	}

	/**
	 * Lists the content of the passed directory.<br />
	 * Updates the view once the data have been received.
	 *
	 * @param dirPath The path of the directory to display.
	 */
	protected abstract void navigateTo(String dirPath);

	/**
	 * Navigates up if possible.<br />
	 * This method is supposed to be called from the parent Activity (most likely through the ActionBar).<br />
	 * Updates the view once the data have been received from the server.
	 */
	public void navigateUp() {

		if (canNavigateUp()) {
			doNavigateUp();
		}
	}

	/**
	 * Call by the activity that holds the fragment if the back button is override.
	 * If the function returns true, the back button is override to go up.
	 * Else, it behaves normally.
	 *
	 * @return True if we can navigate up from the current directory. False otherwise.
	 */
	public boolean canNavigateUp() {
		return mCurrentPath != null &&
				mCurrentPath.contains(File.separator);
	}

	/**
	 * Call navigateTo on the parent directory.
	 */
	protected void doNavigateUp() {
		final String parentPath = FileUtils.truncatePath(mCurrentPath);
		navigateTo(parentPath);
	}

	/**
	 * Callback triggered when the user clicks on a directory.
	 *
	 * @param dirPath The path of the clicked directory.
	 */
	protected abstract void onDirectoryClick(String dirPath);

	/**
	 * Callback triggered when the user clicks on a file.
	 *
	 * @param filename The path of the clicked file.
	 */
	protected abstract void onFileClick(String filename);
}
