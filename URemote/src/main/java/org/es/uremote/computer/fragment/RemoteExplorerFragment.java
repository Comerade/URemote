package org.es.uremote.computer.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.es.uremote.ComputerActivity;
import org.es.uremote.R;
import org.es.uremote.common.AbstractExplorerFragment;
import org.es.uremote.device.ServerSetting;
import org.es.uremote.exchange.Message.Request;
import org.es.uremote.exchange.Message.Request.Code;
import org.es.uremote.exchange.Message.Request.Type;
import org.es.uremote.exchange.Message.Response;
import org.es.uremote.exchange.RequestSender;
import org.es.uremote.network.AsyncMessageMgr;
import org.es.uremote.utils.TaskCallbacks;
import org.es.uremote.utils.ToastSender;

import static org.es.uremote.exchange.Message.Response.ReturnCode.RC_ERROR;

/**
 * Remote file explorer fragment.
 * This fragment allow you to browse the content of a remote device.
 *
 * @author Cyril Leroux
 *         Created on 21/04/12.
 */
public class RemoteExplorerFragment extends AbstractExplorerFragment implements RequestSender {

    private TaskCallbacks mCallbacks;
    private ToastSender mToastSender;

    private TextView mTvEmpty;
    private ImageButton mBtnRefresh;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (TaskCallbacks) activity;
        mToastSender = (ToastSender) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    /**
     * Set the callback to null so we don't accidentally leak the
     * Activity instance.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        mToastSender = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);

        mTvEmpty = (TextView) view.findViewById(R.id.tvEmpty);
        mBtnRefresh = (ImageButton) view.findViewById(R.id.btnRefresh);
        mBtnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO refresh
            }
        });

        return view;
    }

    @Override
    protected void onDirectoryClick(String dirPath) {
        navigateTo(dirPath);
    }

    @Override
    protected void onFileClick(String filename) {

        try {
            sendRequest(Request.newBuilder()
                    .setSecurityToken(getSecurityToken())
                    .setType(Type.EXPLORER)
                    .setCode(Code.OPEN_SERVER_SIDE)
                    .setStringExtra(filename)
                    .build());

        } catch (Exception e) {
            mToastSender.sendToast(R.string.build_request_error);
        }
    }

    /**
     * Ask the server to list the content of the passed directory.
     * Updates the view once the data have been received from the server.
     *
     * @param dirPath The path of the directory to display.
     */
    @Override
    protected void navigateTo(String dirPath) {
        if (dirPath != null) {
            sendRequest(Request.newBuilder()
                    .setSecurityToken(getSecurityToken())
                    .setType(Type.EXPLORER)
                    .setCode(Code.QUERY_CHILDREN)
                    .setStringExtra(dirPath)
                    .build());
        } else {
            mToastSender.sendToast(R.string.msg_no_path_defined);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // *********************** Message Sender *********************** //
    ////////////////////////////////////////////////////////////////////

    /**
     * Initializes the message handler then send the request.
     *
     * @param request The request to send.
     */
    @Override
    public void sendRequest(Request request) {
        if (ExplorerMessageMgr.availablePermits() > 0) {
            new ExplorerMessageMgr().execute(request);
        } else {
            mToastSender.sendToast(R.string.msg_no_more_permit);
        }
    }

    @Override
    public ServerSetting getDevice() {
        return ((ComputerActivity) mCallbacks).getDevice();
    }

    public String getSecurityToken() {
        final ServerSetting settings = getDevice();
        if (settings != null) {
            return settings.getSecurityToken();
        }
        return null;
    }

    /**
     * Class that handle asynchronous requests sent to a remote server.
     *
     * @author Cyril Leroux
     */
    private class ExplorerMessageMgr extends AsyncMessageMgr {

        public ExplorerMessageMgr() {
            super(getDevice(), mCallbacks);
        }

        @Override
        protected void onPostExecute(Response response) {
            super.onPostExecute(response);

            if (response == null) {
                return;
            }

            // Display the message if any
            if (!response.getMessage().isEmpty()) {
                mToastSender.sendToast(response.getMessage());
            }

            // Update view in case of empty list (error or empty directory)
            if (RC_ERROR.equals(response.getReturnCode())) {
                mTvEmpty.setText(R.string.request_error);
                mBtnRefresh.setVisibility(View.VISIBLE);
                return;

            }
            mTvEmpty.setText(R.string.filemanager_empty_directory);
            mBtnRefresh.setVisibility(View.INVISIBLE);
            updateView(response.getFile());
        }
    }
}
