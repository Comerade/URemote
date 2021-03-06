package com.cyrillrx.uremote.common.device;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.cyrillrx.logger.Logger;

/**
 * Parcelable class that holds network device data (ip, mac address, etc.).
 *
 * @author Cyril Leroux
 *         Created on 19/05/13.
 */
public class NetworkDevice extends ConnectedDevice implements Parcelable {

    private static final String TAG      = NetworkDevice.class.getSimpleName();
    public static final  String FILENAME = "serverConfig.xml";

    /** CREATOR is a required attribute to create an instance of a class that implements Parcelable */
    public static final Parcelable.Creator<NetworkDevice> CREATOR = new Parcelable.Creator<NetworkDevice>() {
        @Override
        public NetworkDevice createFromParcel(Parcel src) {
            return new NetworkDevice(src);
        }

        @Override
        public NetworkDevice[] newArray(int size) {
            return new NetworkDevice[size];
        }
    };

    private String         mLocalHost;
    private int            mLocalPort;
    private String         mBroadcast;
    private String         mRemoteHost;
    private int            mRemotePort;
    private String         mMacAddress;
    private ConnectionType mConnectionType;

    /**
     * Constructor with parameters
     *
     * @param name
     * @param localHost
     * @param localPort
     * @param broadcastIp
     * @param remoteHost
     * @param remotePort
     * @param macAddress
     * @param connectionTimeout
     * @param readTimeout
     * @param securityToken
     * @param connectionType
     */
    private NetworkDevice(
            final String name, final String localHost, final int localPort,
            final String broadcastIp, final String remoteHost, final int remotePort,
            final String macAddress,
            final int connectionTimeout, final int readTimeout,
            final String securityToken,
            final ConnectionType connectionType) {

        mName = name;
        mLocalHost = localHost;
        mLocalPort = localPort;
        mBroadcast = broadcastIp;
        mRemoteHost = remoteHost;
        mRemotePort = remotePort;
        mMacAddress = macAddress;
        mConnectionTimeout = connectionTimeout;
        mReadTimeout = readTimeout;
        mSecurityToken = securityToken;
        mConnectionType = connectionType;
    }

    /** @param src  */
    public NetworkDevice(final Parcel src) {
        mName = src.readString();
        mLocalHost = src.readString();
        mLocalPort = src.readInt();
        mBroadcast = src.readString();
        mRemoteHost = src.readString();
        mRemotePort = src.readInt();
        mMacAddress = src.readString();
        mConnectionTimeout = src.readInt();
        mReadTimeout = src.readInt();
        mSecurityToken = src.readString();
        mConnectionType = ConnectionType.valueOf(src.readString());
    }

    /**
     * Update the server with the object passed.
     *
     * @param server The server updated data.
     */
    public void update(final NetworkDevice server) {
        mName = server.getName();
        mLocalHost = server.getLocalHost();
        mLocalPort = server.getLocalPort();
        mBroadcast = server.getBroadcast();
        mRemoteHost = server.getRemoteHost();
        mRemotePort = server.getRemotePort();
        mMacAddress = server.getMacAddress();
        mConnectionTimeout = server.getConnectionTimeout();
        mReadTimeout = server.getReadTimeout();
        mSecurityToken = server.getSecurityToken();
        mConnectionType = server.getConnectionType();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeString(mName);
        destination.writeString(mLocalHost);
        destination.writeInt(mLocalPort);
        destination.writeString(mBroadcast);
        destination.writeString(mRemoteHost);
        destination.writeInt(mRemotePort);
        destination.writeString(mMacAddress);
        destination.writeInt(mConnectionTimeout);
        destination.writeInt(mReadTimeout);
        destination.writeString(mSecurityToken);
        destination.writeString(mConnectionType.toString());
    }

    /**
     * @param context
     * @return True is the server is in the same network than the device.
     */
    public boolean isLocal(Context context) {
        // TODO define when local and remote => User defined
        final WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiMgr.isWifiEnabled();
    }

    @Override
    public String toString() {
        return (mConnectionType == ConnectionType.LOCAL) ? toStringLocal() : toStringRemote();
    }

    /** @return Concatenation of host and port of the local server. */
    public String toStringLocal() { return mLocalHost + ":" + mLocalPort; }

    /** @return Concatenation of host and port of the remote server. */
    public String toStringRemote() { return mRemoteHost + ":" + mRemotePort; }

    /** @return The ip address of the local server. */
    public String getLocalHost() { return mLocalHost; }

    public void setLocalHost(String localHost) { mLocalHost = localHost; }

    /** @return The port of the local server. */
    public int getLocalPort() { return mLocalPort; }

    /** @return The broadcast address. */
    public String getBroadcast() { return mBroadcast; }

    /** @return The ip address of the remote server. */
    public String getRemoteHost() { return mRemoteHost; }

    /** @return The port of the remote server. */
    public int getRemotePort() { return mRemotePort; }

    /** @return The mac address of the server. */
    public String getMacAddress() { return mMacAddress; }

    /** @return The type of connection (remote or local). */
    public ConnectionType getConnectionType() { return mConnectionType; }

    /**
     * The type of connection.
     */
    public enum ConnectionType {
        /** The server IS in the same network than the device. It must be accessed locally. */
        LOCAL,
        /** The server IS NOT in the same network than the device. It must be accessed remotely. */
        REMOTE
    }

    /** @return An instance of ServerSetting.Builder. */
    public static Builder newBuilder() { return new Builder(); }

    /**
     * Class that holds server connection data.
     *
     * @author Cyril Leroux
     *         Created on 03/06/13.
     */
    // TODO Move a part of the builder in ConnectedDevice class
    public static class Builder {

        private static String         DEFAULT_NAME               = "";
        private static String         DEFAULT_LOCAL_HOST         = "";
        private static int            DEFAULT_PORT               = 0;
        private static String         DEFAULT_BROADCAST          = "";
        private static String         DEFAULT_REMOTE_HOST        = "";
        private static String         DEFAULT_MAC_ADDRESS        = "";
        private static ConnectionType DEFAULT_CONNECTION         = ConnectionType.LOCAL;
        private static int            DEFAULT_CONNECTION_TIMEOUT = 500;
        private static int            DEFAULT_READ_TIMEOUT       = 500;
        private static String         DEFAULT_SECURITY_TOKEN     = "";

        private String         mName              = DEFAULT_NAME;
        private String         mLocalHost         = DEFAULT_LOCAL_HOST;
        private int            mLocalPort         = DEFAULT_PORT;
        private String         mBroadcast         = DEFAULT_BROADCAST;
        private String         mRemoteHost        = DEFAULT_REMOTE_HOST;
        private int            mRemotePort        = DEFAULT_PORT;
        private String         mMacAddress        = DEFAULT_MAC_ADDRESS;
        private ConnectionType mConnectionType    = DEFAULT_CONNECTION;
        /** If the connection with the remote server is not established within this timeout, it is dismissed. */
        private int            mConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        private int            mReadTimeout       = DEFAULT_READ_TIMEOUT;
        private String         mSecurityToken     = DEFAULT_SECURITY_TOKEN;

        private Builder() { }

        /** Reset builder */
        public void clear() {
            mName = DEFAULT_NAME;
            mLocalHost = DEFAULT_LOCAL_HOST;
            mLocalPort = DEFAULT_PORT;
            mBroadcast = DEFAULT_BROADCAST;
            mRemoteHost = DEFAULT_REMOTE_HOST;
            mRemotePort = DEFAULT_PORT;
            mMacAddress = DEFAULT_MAC_ADDRESS;
            mConnectionType = DEFAULT_CONNECTION;
            mConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
            mReadTimeout = DEFAULT_READ_TIMEOUT;
            mSecurityToken = DEFAULT_SECURITY_TOKEN;
        }

        /**
         * @return A fully loaded {@link NetworkDevice} object.
         * @throws Exception if the builder has not all the data to build the object.
         */
        public NetworkDevice build() throws Exception {

            boolean error = false;
            boolean warning = false;

            StringBuilder sb = new StringBuilder();

            if (TextUtils.isEmpty(mName)) {
                error = true;
                sb.append("- Name is null or empty.\n");
            }

            if (TextUtils.isEmpty(mLocalHost)) {
                error = true;
                sb.append("- Localhost is empty.\n");
            }

            if (mLocalPort == 0) {
                error = true;
                sb.append("- Local port is 0.\n");
            }

            if (TextUtils.isEmpty(mBroadcast)) {
                warning = true;
                sb.append("- Broadcast is empty.\n");
            }

            if (TextUtils.isEmpty(mRemoteHost)) {
                warning = true;
                sb.append("- Remote host is empty.\n");
            }

            if (mRemotePort == 0) {
                warning = true;
                sb.append("- Remote port is 0.\n");
            }

            if (TextUtils.isEmpty(mMacAddress)) {
                warning = true;
                sb.append("- Mac address is empty.\n");
            }

            if (error) {
                throw new Exception("Server creation has failed:\n" + sb.toString());
            }

            if (warning) {
                Logger.warning(TAG, "Server creation succeeded with warnings:\n" + sb.toString());
            } else {
                Logger.warning(TAG, "Server creation succeeded.");
            }

            return new NetworkDevice(mName, mLocalHost, mLocalPort, mBroadcast, mRemoteHost, mRemotePort, mMacAddress, mConnectionTimeout, mReadTimeout, mSecurityToken, mConnectionType);
        }

        public Builder setConnectionType(ConnectionType type) {
            mConnectionType = type;
            return this;
        }

        public Builder setName(final String name) {
            mName = name;
            return this;
        }

        public Builder setLocalHost(final String ipAddress) {
            mLocalHost = ipAddress;
            return this;
        }

        public Builder setLocalPort(final int port) {
            mLocalPort = port;
            return this;
        }

        public Builder setBroadcast(final String broadcastAddress) {
            mBroadcast = broadcastAddress;
            return this;
        }

        public Builder setRemoteHost(final String ipAddress) {
            mRemoteHost = ipAddress;
            return this;
        }

        public Builder setRemotePort(final int port) {
            mRemotePort = port;
            return this;
        }

        public Builder setMacAddress(final String macAddress) {
            mMacAddress = macAddress;
            return this;
        }

        /**
         * If the connection with the remote server is not established
         * within this timeout, it is dismissed.
         */
        public Builder setConnectionTimeout(final int timeout) {
            mConnectionTimeout = timeout;
            return this;
        }

        public Builder setReadTimeout(final int timeout) {
            mReadTimeout = timeout;
            return this;
        }

        public Builder setSecurityToken(final String securityToken) {
            mSecurityToken = securityToken;
            return this;
        }
    }
}