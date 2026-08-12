package io.github.freewebmovement.igniter.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.core.app.NotificationCompat;

import java.util.Set;

import io.github.freewebmovement.igniter.IgniterApplication;
import io.github.freewebmovement.igniter.R;
import io.github.freewebmovement.igniter.connection.TestConnection;
import io.github.freewebmovement.igniter.persistence.data.ExemptAppDataManager;
import io.github.freewebmovement.igniter.persistence.data.ExemptAppDataSource;
import io.github.freewebmovement.igniter.persistence.NetWorkConfig;
import io.github.freewebmovement.igniter.proxy.aidl.ITrojanService;
import io.github.freewebmovement.igniter.proxy.aidl.ITrojanServiceCallback;
import io.github.freewebmovement.igniter.activities.MainActivity;

public class ProxyService extends VpnService implements TestConnection.OnResultListener {
    private static final String TAG = "ProxyService";

    public static final int STATE_NONE = -1;
    public static final int STARTING = 0;
    public static final int STARTED = 1;
    public static final int STOPPING = 2;
    public static final int STOPPED = 3;
    public static final int PROXY_SERVICE_STATUS_NOTIFY_MSG_ID = 114514;
    public IgniterApplication app;

    @IntDef({STATE_NONE, STARTING, STARTED, STOPPING, STOPPED})
    public @interface ProxyState {
    }

    private static final String TUN2SOCKS5_SERVER_HOST = "127.0.0.1";
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private @ProxyState
    volatile int state = STATE_NONE;
    private volatile ParcelFileDescriptor pfd;
    private ExemptAppDataSource mExemptAppDataSource;
    /**
     * Receives stop event.ate
     */
    private final BroadcastReceiver mStopBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String stopAction = getString(R.string.stop_service);
            final String action = intent.getAction();
            if (stopAction.equals(action)) {
                stop();
            }
        }
    };
    /**
     * Callback list for remote processes or services.
     */
    private final RemoteCallbackList<ITrojanServiceCallback> mCallbackList = new RemoteCallbackList<>();
    /**
     * Binder implementation of {@link ITrojanService}, which provides access of connection state,
     * connection test and callback registration.
     */
    private final IBinder mBinder = new ITrojanService.Stub() {
        @Override
        public int getState() {
            Log.i(TAG, "IBinder getState# : " + state);
            return state;
        }

        @Override
        public void testConnection(String testUrl) {
            if (state != STARTED) {
                onResult(TUN2SOCKS5_SERVER_HOST, false, 0L, "ProxyService not yet connected.");
                return;
            }
            long port = app.trojanConfig.getLocalPort();
            new TestConnection(TUN2SOCKS5_SERVER_HOST, port, ProxyService.this).execute(testUrl);
        }

        @Override
        public void showDevelopInfoInLogcat() {
//            Log.showDevelopInfoInLogcat();
        }

        @Override
        public void registerCallback(ITrojanServiceCallback callback) {
            Log.i(TAG, "IBinder registerCallback#");
            mCallbackList.register(callback);
        }

        @Override
        public void unregisterCallback(ITrojanServiceCallback callback) {
            Log.i(TAG, "IBinder unregisterCallback#");
            mCallbackList.unregister(callback);
        }
    };

    private void setState(int state) {
        Log.i(TAG, "setState: " + state);
        this.state = state;
        notifyStateChange();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.stop_service));
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // only for gingerbread and newer versions
            registerReceiver(mStopBroadcastReceiver, filter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(mStopBroadcastReceiver, filter);
        }
        app = IgniterApplication.getApplication();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        mCallbackList.kill();
        setState(STOPPED);
        unregisterReceiver(mStopBroadcastReceiver);
        pfd = null;
    }

    /**
     * Broadcast the state change event by invoking callbacks from other processes or services.
     */
    private void notifyStateChange() {
        int state = this.state;
        for (int i = mCallbackList.beginBroadcast() - 1; i >= 0; i--) {
            try {
                // the second String parameter is currently useless. Might be the url of the profile.
                mCallbackList.getBroadcastItem(i).onStateChanged(state, "state changed");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mCallbackList.finishBroadcast();
    }

    @Override
    public void onRevoke() {
        // Calls to this method may not happen on the main thread
        // of the process.
        stop();
    }

    @Override
    public void onResult(String testUrl, boolean connected, long delay, String error) {
        // broadcast test result by invoking callbacks from other processes or services.
        for (int i = mCallbackList.beginBroadcast() - 1; i >= 0; i--) {
            try {
                mCallbackList.getBroadcastItem(i).onTestResult(testUrl, connected, delay, error);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mCallbackList.finishBroadcast();
    }

    @Override
    public IBinder onBind(Intent intent) {
        final String bindServiceAction = getString(R.string.bind_service);
        if (bindServiceAction.equals(intent.getAction())) {
            return mBinder;
        }
        return super.onBind(intent);
    }

    private Set<String> getExemptAppPackageNames() {
        if (mExemptAppDataSource == null) {
            mExemptAppDataSource = new ExemptAppDataManager(app);
        }
        // ensures that new exempted app list can be applied on proxy after modification.
        return mExemptAppDataSource.loadExemptAppPackageNameSet();
    }

    /**
     * Start foreground notification to avoid ANR and crash, as Android requires that Service which
     * is started by calling {@link Context#startForegroundService(Intent)} must
     * invoke {@link android.app.Service#startForeground(int, Notification)} within 5 seconds.
     */
    private void startForegroundNotification(String channelId) {
        Intent openMainActivityIntent = new Intent(this, MainActivity.class);
        openMainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingOpenMainActivityIntent = PendingIntent.getActivity(this,
                0, openMainActivityIntent,
                PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_starting_service))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingOpenMainActivityIntent)
                .setAutoCancel(false)
                .setOngoing(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setShowWhen(true);
        }
        builder.setWhen(0L);

        // it's required to create a notification channel before startForeground on SDK >= Android O
        createNotificationChannel(channelId);
        Log.i(TAG, "Start foreground notification");
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // only for gingerbread and newer versions
            startForeground(PROXY_SERVICE_STATUS_NOTIFY_MSG_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
        } else {
            startForeground(PROXY_SERVICE_STATUS_NOTIFY_MSG_ID, builder.build());
        }
    }

    public VpnService.Builder getVPNBuilder() {
        // The VPN interface (addresses, routes, DNS, MTU) is fully configured in
        // NetWorkConfig.establish() to keep a single source of truth.
        return new VpnService.Builder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        if (state == STARTED || state == STARTING) {
            return START_NOT_STICKY;
        }
        // In order to keep the service long-lived, starting the service by Context.startForegroundService()
        // might be the easiest way. According to the official indication, a service which is started
        // by Context.startForegroundService() must call Service.startForeground() within 5 seconds.
        // Otherwise the process will be shutdown and user will get an ANR notification.
        startForegroundNotification(getString(R.string.notification_channel_id));
        setState(STARTING);

        Set<String> packageNames = getExemptAppPackageNames();
        packageNames.add(getPackageName());

        // VPN setup performs blocking I/O (port probing, native startup) and must
        // not run on the main thread.
        new Thread(() -> startVpn(packageNames), "igniter-vpn-start").start();
        return START_STICKY;
    }

    private void startVpn(Set<String> packageNames) {
        final String statusStr;
        try {
            pfd = NetWorkConfig.establish(
                    app,
                    getVPNBuilder(),
                    getString(R.string.app_name),
                    packageNames
            );
            Log.i("VPN", "pfd established");
            if (pfd == null) {
                throw new IllegalStateException(getString(R.string.error_establish_vpn));
            }
            statusStr = NetWorkConfig.startService(app, pfd.detachFd());
        } catch (SecurityException e) {
            Log.e(TAG, "VPN permission revoked", e);
            failStart(getString(R.string.error_vpn_permission_revoked));
            return;
        } catch (Exception e) {
            Log.e(TAG, "failed to start proxy", e);
            String message = e.getMessage();
            failStart(message == null || message.isEmpty() ? e.toString() : message);
            return;
        }
        mHandler.post(() -> {
            setState(STARTED);
            Intent openMainActivityIntent = new Intent(this, MainActivity.class);
            openMainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingOpenMainActivityIntent = PendingIntent.getActivity(this, 0, openMainActivityIntent, PendingIntent.FLAG_IMMUTABLE);
            final String channelId = getString(R.string.notification_channel_id);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(getString(R.string.service_is_running))
                    .setContentText(statusStr)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(statusStr))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    // Set the intent that will fire when the user taps the notification
                    .setContentIntent(pendingOpenMainActivityIntent)
                    .setAutoCancel(false)
                    .setOngoing(true);
            startForeground(PROXY_SERVICE_STATUS_NOTIFY_MSG_ID, builder.build());
        });
    }

    /**
     * Stops the VPN after a startup failure, showing the reason briefly so the
     * user can tell why the connection did not come up.
     */
    private void failStart(String message) {
        Log.e(TAG, "failStart: " + message);
        try {
            Notification notification = new NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setContentTitle(getString(R.string.service_failed))
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build();
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.notify(PROXY_SERVICE_STATUS_NOTIFY_MSG_ID, notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setState(STOPPED);
        NetWorkConfig.stop(app);
        pfd = null;
        // Give the error notification a moment to be seen before the process is recycled.
        new Handler(Looper.getMainLooper()).postDelayed(this::stop, 1500);
    }

    private void shutdown() {
        Log.i(TAG, "shutdown");
        setState(STOPPING);

        NetWorkConfig.stop(app);
        stopSelf();

        setState(STOPPED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH);
        } else {
            stopForeground(true);
        }
        destroyNotificationChannel(getString(R.string.notification_channel_id));
    }

    private void createNotificationChannel(String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(channelId,
                    getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void destroyNotificationChannel(String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.deleteNotificationChannel(channelId);
        }
    }

    public void stop() {
        shutdown();
        // this is essential for goMobile aar
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
