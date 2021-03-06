package com.cyrillrx.uremote.common.device;

/**
 * @author Cyril Leroux
 *         Created on 03/11/13.
 */
public class ConnectedDevice {

    protected String mId;
    protected String mName;
    /** If the connection with the remote server is not established within this timeout, it is dismiss. */
    protected int    mConnectionTimeout;
    protected int    mReadTimeout;
    protected String mSecurityToken;

    @Override
    public String toString() {
        return mName;
    }

    /** @return The device id. */
    public String getId() { return mId; }

    /** @return The device name. */
    public String getName() {
        return mName;
    }

    /** @return Timeout connection in milliseconds. */
    public int getConnectionTimeout() {
        return mConnectionTimeout;
    }

    /** @return Read timeout in milliseconds. */
    public int getReadTimeout() {
        return mReadTimeout;
    }

    /** @return The security token that will be use to authenticate the user. */
    public String getSecurityToken() {
        return mSecurityToken;
    }
}
