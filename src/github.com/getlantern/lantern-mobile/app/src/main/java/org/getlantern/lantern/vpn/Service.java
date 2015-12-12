package org.getlantern.lantern.vpn;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import android.content.Context;
import android.content.SharedPreferences;

import org.getlantern.lantern.config.LanternConfig;
import org.getlantern.lantern.model.UI;
import org.getlantern.lantern.sdk.Utils;

import java.util.Map;

public class Service extends VpnBuilder implements Handler.Callback {
    private static final String TAG = "VpnService";
    private String mSessionName = "LanternVpn";

    private Handler mHandler;

    public static UI LanternUI;

    private LanternVpn lantern;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }

        String action = intent.getAction();

        if (lantern == null) {
            lantern = new LanternVpn(this);
        }

        // STOP button was pressed
        // shut down Lantern and close the VPN connection
        if (action.equals(LanternConfig.DISABLE_VPN)) {

            stop();
            lantern = null;

            if (mHandler != null) {
                mHandler.postDelayed(new Runnable () {
                    public void run () { 
                        stopSelf();
                    }
                }, 1000);
            }
        } else if (action.equals(LanternConfig.ENABLE_VPN)) {
            start();
        } else if (action.equals(LanternConfig.RESTART_VPN)) {
            try { 
                if (lantern != null) {
                    super.restart(lantern.getSettings());
                }
            } catch (Exception e) {
                Log.e(TAG, "Could not restart Lantern: " + e.getMessage());
            }
        }

        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        return START_STICKY;
    }

    private synchronized void start() {
        Log.d(TAG, "Loading Lantern library");
        final Service service = this;
        Thread mThread = new Thread() {
            public void run() {
                try {
                    lantern.start();
                    Thread.sleep(2000);
                    service.configure(lantern.getSettings());
                } catch (Exception uhe) {
                    Log.e(TAG, "Error starting Lantern with given host: " + uhe);
                }
            }
        };
        mThread.start();
    }

    public void stop() {
        try {
            Log.d(TAG, "Stopping Lantern...");
            if (lantern != null) {
                lantern.stop(); 
            }
            Utils.clearPreferences(this);

            Log.d(TAG, "Closing VPN interface..");
            super.close();
        } catch (Exception e) {
            Log.e(TAG, "Could not stop Lantern: " + e);
        }
    }

    public void setVersionNum(String latestVersion) {
        LanternUI.setVersionNum(latestVersion);
    }

    @Override
    public void onDestroy() {
        try {
            stop();
        } catch (Exception e) {

        }
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}