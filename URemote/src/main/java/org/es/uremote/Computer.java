package org.es.uremote;

import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.es.uremote.computer.FragAdmin;
import org.es.uremote.computer.FragDashboard;
import org.es.uremote.computer.FragKeyboard;
import org.es.uremote.computer.KeyboardListener;
import org.es.uremote.computer.RemoteExplorerFragment;
import org.es.uremote.computer.ServerListActivity;
import org.es.uremote.device.ServerSetting;
import org.es.uremote.exchange.Message.Request;
import org.es.uremote.exchange.Message.Request.Code;
import org.es.uremote.exchange.Message.Request.Type;
import org.es.uremote.exchange.Message.Response;
import org.es.uremote.exchange.MessageUtils;
import org.es.uremote.network.AsyncMessageMgr;
import org.es.uremote.utils.Constants;
import org.es.uremote.utils.TaskCallbacks;
import org.es.utils.Log;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.ACTION_EDIT;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.view.KeyEvent.KEYCODE_A;
import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_SHORT;
import static org.es.uremote.exchange.Message.Request.Code.DOWN;
import static org.es.uremote.exchange.Message.Request.Code.UP;
import static org.es.uremote.exchange.Message.Request.Type.SIMPLE;
import static org.es.uremote.exchange.Message.Request.Type.VOLUME;
import static org.es.uremote.exchange.Message.Response.ReturnCode.RC_ERROR;
import static org.es.uremote.utils.Constants.STATE_CONNECTING;
import static org.es.uremote.utils.Constants.STATE_KO;
import static org.es.uremote.utils.Constants.STATE_OK;
import static org.es.uremote.utils.IntentKeys.EXTRA_SERVER_DATA;

/**
 * @author Cyril Leroux
 *         Created on 10/05/12.
 */
public class Computer extends FragmentActivity implements OnPageChangeListener, TaskCallbacks, ToastSender {

    private static final String TAG = "Computer Activity";
    private static final String SELECTED_TAB_INDEX = "SELECTED_TAB_INDEX";
    private static final int PAGES_COUNT = 4;
    private static final int EXPLORER_PAGE_ID = 2;

    private FragAdmin mFragAdmin;
    private FragDashboard mFragDashboard;
    private RemoteExplorerFragment mExplorerFragment;
    private FragKeyboard mFragKeyboard;

    private int mCurrentPage = -1;
    private TextView mTvServerState;
    private ProgressBar mPbConnection;

    private static Toast mToast = null;

    private ServerSetting mSelectedServer = null;

    private KeyboardView mKeyboardView = null;
    private KeyboardView mExtendedKeyboardView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_computer);

        mKeyboardView = (KeyboardView) findViewById(R.id.keyboardView);
        mExtendedKeyboardView = (KeyboardView) findViewById(R.id.keyboardViewExtended);

        // Create custom keyboard
        final KeyboardListener keyboardListener = new KeyboardListener();
        keyboardListener.setHapticFeedbackView(mKeyboardView);

        final Keyboard keyboard = new Keyboard(getApplicationContext(), R.xml.pc_keyboard_qwerty);
        mKeyboardView.setKeyboard(keyboard);
        mKeyboardView.setPreviewEnabled(false);
        mKeyboardView.setOnKeyboardActionListener(keyboardListener);

        final Keyboard extendedKeyboard = new Keyboard(getApplicationContext(), R.xml.pc_keyboard_extended);
        mExtendedKeyboardView.setKeyboard(extendedKeyboard);
        mExtendedKeyboardView.setPreviewEnabled(false);
        mExtendedKeyboardView.setOnKeyboardActionListener(keyboardListener);

        // Server info default value.
        ((TextView) findViewById(R.id.tvServerInfos)).setText(R.string.no_server_configured);

        final ServerSetting server = getIntent().getParcelableExtra(EXTRA_SERVER_DATA);
        if (server == null) {
            finish();
        }
        initServer(server);

        // ActionBar configuration
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        // Enable Home as Up
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Fragment to use in each tab
        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (mFragAdmin == null) {
            mFragAdmin = new FragAdmin();
        }
        if (mFragDashboard == null) {
            mFragDashboard = new FragDashboard();
        }
        if (mExplorerFragment == null) {
            mExplorerFragment = new RemoteExplorerFragment();
        }
        if (mFragKeyboard == null) {
            mFragKeyboard = new FragKeyboard();
        }

        List<Fragment> fragments = new ArrayList<>(PAGES_COUNT);
        fragments.add(mFragAdmin);
        fragments.add(mFragDashboard);
        fragments.add(mExplorerFragment);
        fragments.add(mFragKeyboard);

        ViewPager viewPager = (ViewPager) findViewById(R.id.vpMain);
        ComputerPagerAdapter pagerAdapter = new ComputerPagerAdapter(super.getSupportFragmentManager(), fragments);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOnPageChangeListener(this);
        viewPager.setCurrentItem(mCurrentPage);

        mTvServerState = (TextView) findViewById(R.id.tvServerState);
        mPbConnection = (ProgressBar) findViewById(R.id.pbConnection);

        if (savedInstanceState != null) {
            final int newTabIndex = savedInstanceState.getInt(SELECTED_TAB_INDEX, 1);
            if (newTabIndex != actionBar.getSelectedNavigationIndex()) {
                actionBar.setSelectedNavigationItem(newTabIndex);
            }
        }
    }

    /**
     * Initialize the server.
     * <p/>
     * <ul>
     * <li>Send ping message to the server.</li>
     * <li>Update server info TextView.</li>
     * </ul>
     *
     * @param selectedServer Server use to initialize MUST NOT be null.
     */
    protected void initServer(final ServerSetting selectedServer) {
        mSelectedServer = selectedServer;
        sendAsyncRequest(SIMPLE, Code.PING);
        ((TextView) findViewById(R.id.tvServerInfos)).setText(getServerString(mSelectedServer));

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        int tabIndex = getActionBar().getSelectedNavigationIndex();
        outState.putInt(SELECTED_TAB_INDEX, tabIndex);
        super.onSaveInstanceState(outState);
    }

    /**
     * Handle volume physical buttons.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KEYCODE_VOLUME_UP) {
            sendAsyncRequest(VOLUME, UP);
            return true;

        } else if (keyCode == KEYCODE_VOLUME_DOWN) {
            sendAsyncRequest(VOLUME, DOWN);
            return true;

        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mCurrentPage == EXPLORER_PAGE_ID && mExplorerFragment.canNavigateUp()) {
                return true;
            }
        }
        Log.warning(TAG, "#onKeyDown - Key " + KeyEvent.keyCodeToString(keyCode) + " not handle for page " + mCurrentPage);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KEYCODE_A) {

            Toast.makeText(getApplicationContext(), "A key up", LENGTH_SHORT).show();
            //            sendAsyncRequest(ExchangeMessagesUtils.buildRequest(mSelectedServer.getSecurityToken(),
            //                    KEYBOARD, Code.DEFINE, Code.NONE, "Q"));
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.server, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                Intent intent = new Intent(getApplicationContext(), Home.class);
                intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;

            case R.id.server_keyboard:

                // Toggle custom keyboard visibility
                if (mKeyboardView.getVisibility() == GONE) {
                    showCustomKeyboard();
                } else {
                    hideCustomKeyboard();
                }
                return true;

            case R.id.server_settings:
                startActivity(new Intent(getApplicationContext(), AppSettings.class));
                return true;

            case R.id.server_list:
                startActivity(new Intent(getApplicationContext(), ServerListActivity.class).setAction(ACTION_EDIT));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Show the custom keyboard.
     */
    private void showCustomKeyboard() {

        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(findViewById(R.id.vpMain).getWindowToken(), 0);

        mKeyboardView.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(mKeyboardView, "translationY", 100f, 0f)
                .setDuration(100)
                .start();
        mKeyboardView.setEnabled(true);

        mExtendedKeyboardView.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(mExtendedKeyboardView, "translationY", -100f, 0f)
                .setDuration(100)
                .start();
        mExtendedKeyboardView.setEnabled(true);
    }

    /**
     * Hide the custom keyboard.
     */
    private void hideCustomKeyboard() {

        mKeyboardView.setVisibility(View.GONE);
        mKeyboardView.setEnabled(false);

        mExtendedKeyboardView.setVisibility(View.GONE);
        mExtendedKeyboardView.setEnabled(false);
    }

    public void sendToast(final String message) {
        if (mToast == null) {
            mToast = Toast.makeText(getApplicationContext(), "", LENGTH_SHORT);
        }
        mToast.setText(message);
        mToast.show();
    }

    public ServerSetting getServer() {
        return mSelectedServer;
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageSelected(int position) {
        // TODO this call does not work as it should.
        mCurrentPage = position;
        if (position == 1) {
            getActionBar().setIcon(R.drawable.ic_filemanager);
        } else if (position == 2) {
            getActionBar().setIcon(R.drawable.ic_keyboard);
        } else {
            getActionBar().setIcon(R.drawable.ic_launcher);
        }
    }

    @Override
    public void onPreExecute() {
        updateConnectionState(STATE_CONNECTING);
    }

    @Override
    public void onProgressUpdate(int percent) {
    }

    @Override
    public void onCancelled() {
    }

    @Override
    public void onPostExecute(Response response) {
        if (RC_ERROR.equals(response.getReturnCode())) {
            updateConnectionState(STATE_KO);
        } else {
            updateConnectionState(STATE_OK);
        }
    }

    private class ComputerPagerAdapter extends FragmentPagerAdapter {

        private final List<Fragment> mFragments;

        /**
         * @param fm        The fragment manager
         * @param fragments The fragments list.
         */
        public ComputerPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            mFragments = fragments;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }
    }

    /**
     * Initializes the message handler then send the request.
     *
     * @param requestType The request type.
     * @param requestCode The request code.
     */
    public void sendAsyncRequest(Type requestType, Code requestCode) {

        final ServerSetting serverSetting = getServer();

        if (serverSetting == null) {
            Toast.makeText(getApplicationContext(), R.string.no_server_configured, LENGTH_SHORT).show();
            return;
        }

        Request request = MessageUtils.buildRequest(serverSetting.getSecurityToken(), requestType, requestCode);

        if (request == null) {
            Toast.makeText(getApplicationContext(), R.string.msg_null_request, LENGTH_SHORT).show();
            return;
        }

        if (AsyncMessageMgr.availablePermits() > 0) {
            new AsyncMessageMgr(serverSetting).execute(request);
        } else {
            Toast.makeText(getApplicationContext(), R.string.msg_no_more_permit, LENGTH_SHORT).show();
        }
    }

    /**
     * Update the connection state of the UI
     *
     * @param state The state of the connection :
     *              <ul>
     *              <li>{@link Constants#STATE_OK}</li>
     *              <li>{@link Constants#STATE_KO}</li>
     *              <li>{@link Constants#STATE_CONNECTING}</li>
     *              </ul>
     */
    public void updateConnectionState(int state) {
        int drawableResId;
        int messageResId;
        int visibility;

        switch (state) {
            case STATE_OK:
                drawableResId = android.R.drawable.presence_online;
                messageResId = R.string.msg_command_succeeded;
                visibility = INVISIBLE;
                break;

            case STATE_CONNECTING:
                drawableResId = android.R.drawable.presence_away;
                messageResId = R.string.msg_command_running;
                visibility = VISIBLE;
                break;

            default: // KO
                drawableResId = android.R.drawable.presence_offline;
                messageResId = R.string.msg_command_failed;
                visibility = INVISIBLE;
                break;
        }

        final Drawable imgLeft = getResources().getDrawable(drawableResId);
        imgLeft.setBounds(0, 0, 24, 24);
        mTvServerState.setCompoundDrawables(imgLeft, null, null, null);
        mTvServerState.setText(messageResId);
        mPbConnection.setVisibility(visibility);
    }

    private String getServerString(ServerSetting serverSetting) {
        if (serverSetting == null) {
            return getString(R.string.no_server_configured);
        }

        if (serverSetting.isLocal(getApplicationContext())) {
            return serverSetting.getFullLocal();
        }
        return serverSetting.getFullRemote();
    }
}
