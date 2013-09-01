package org.es.uremote.computer;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import org.es.uremote.R;
import org.es.uremote.common.LocalExplorerFragment;
import org.es.uremote.utils.IntentKeys;

/**
 * Created by Cyril on 31/08/13.
 */
public class LoadServerActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load from caller activity
		final String path = getIntent().getStringExtra(IntentKeys.DIRECTORY_PATH);

		// Send to fragment
		Bundle fragmentArg = new Bundle();
		fragmentArg.putString(IntentKeys.DIRECTORY_PATH, path);

		if (savedInstanceState == null) {
			LocalExplorerFragment fragment = new LocalExplorerFragment();
			fragment.setArguments(fragmentArg);
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(android.R.id.content, fragment).commit();
		}

		setContentView(R.layout.activity_explorer_load);

		/////////////////////////////////
		/*
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but all we need
			//  to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		/////////////////////////////////
		Environment.getExternalStorageDirectory();
		String path = "/sdcard";
		*/
	}
}
