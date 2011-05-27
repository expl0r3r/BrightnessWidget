package priv.twoerner.brightnesswidget;

import java.util.List;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;

public class BrightnessWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "BrightnessWidgetProvider";
    private static final int MESSAGE_STOP_SERVICE = 1;
    private static final String UPDATE_SERVICE_ACTION = "action";
    private final static String ACTION_BRIGHTNESS_EXTRA = "brightness";
    public final static String ACTION_CHANGE_TOUCH_BRIGHTNESS = "priv.twoerner.touchbrightness.ACTION_CHANGE_BRIGHTNESS";

    @Override
    public void onReceive(Context context, Intent intent) {
	super.onReceive(context, intent);

	Log.d(TAG, "onReceive");
	Log.d(TAG, intent.getAction());

	if (intent != null && intent.getAction() != null) {
	    String intentAction = intent.getAction();

	    if (intentAction.equals(ViewConfig.ACTION_1) || intentAction.equals(ViewConfig.ACTION_2)
		    || intentAction.equals(ViewConfig.ACTION_3) || intentAction.equals(ViewConfig.ACTION_4)
		    || intentAction.equals(ViewConfig.ACTION_5)) {
		Intent serviceIntent = new Intent(context, UpdateService.class);
		serviceIntent.putExtra(UPDATE_SERVICE_ACTION, intent.getAction());
		Log.d(TAG, "Starting service");
		context.startService(serviceIntent);
	    }

	}
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	Log.d(TAG, "onUpdate");

	final int N = appWidgetIds.length;

	// Perform this loop procedure for each App Widget that belongs to this provider
	for (int i = 0; i < N; i++) {
	    int appWidgetId = appWidgetIds[i];

	    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.brightness_widget);

	    views = ViewConfig.configView(views, context);
	    // Tell the AppWidgetManager to perform an update on the current App Widget
	    appWidgetManager.updateAppWidget(appWidgetId, views);
	}
    }

    public static class UpdateService extends Service {

	private static final String TAG = "UpdateService";

	@Override
	public int onStartCommand(final Intent intent, int flags, int startId) {

	    final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
		    if (msg != null) {
			if (msg.what == MESSAGE_STOP_SERVICE) {
			    Log.d(TAG, "Stopping service");
			    stopSelf();
			}
		    }
		}
	    };

	    mHandler.post(new Runnable() {

		@Override
		public void run() {
		    try {
			int sysBackLightValue = 255;
			float factor = 1.0f;
			String factorStr = "100";
			String touchButtonBrightness = null;
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(UpdateService.this);

			if (intent == null || intent.getStringExtra(UPDATE_SERVICE_ACTION) == null || prefs == null) {
			    return;
			}

			String action = intent.getStringExtra(UPDATE_SERVICE_ACTION);

			if (action.equals(ViewConfig.ACTION_1)) {
			    factorStr = prefs.getString("button1", "20");
			    touchButtonBrightness = prefs.getString("button1_touchbrightness", "10");
			} else if (action.equals(ViewConfig.ACTION_2)) {
			    factorStr = prefs.getString("button2", "40");
			    touchButtonBrightness = prefs.getString("button2_touchbrightness", "10");
			} else if (action.equals(ViewConfig.ACTION_3)) {
			    factorStr = prefs.getString("button3", "60");
			    touchButtonBrightness = prefs.getString("button3_touchbrightness", "10");
			} else if (action.equals(ViewConfig.ACTION_4)) {
			    factorStr = prefs.getString("button4", "80");
			    touchButtonBrightness = prefs.getString("button4_touchbrightness", "10");
			} else if (action.equals(ViewConfig.ACTION_5)) {
			    factorStr = prefs.getString("button5", "100");
			    touchButtonBrightness = prefs.getString("button5_touchbrightness", "10");
			}

			factor = Float.parseFloat(factorStr) / 100f;
			sysBackLightValue = (int) (factor * 255);
			Log.d(TAG, Integer.toString(sysBackLightValue));
			Settings.System.putInt(UpdateService.this.getContentResolver(),
				Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
			Settings.System.putInt(UpdateService.this.getContentResolver(),
				Settings.System.SCREEN_BRIGHTNESS, sysBackLightValue);

			if (prefs.getBoolean("control_touch_brightness", false)) {
			    Intent touchBrightnessIntent = new Intent();
			    touchBrightnessIntent.setAction(ACTION_CHANGE_TOUCH_BRIGHTNESS);
			    touchBrightnessIntent.putExtra(ACTION_BRIGHTNESS_EXTRA,
				    Integer.parseInt(touchButtonBrightness));

			    PackageManager packageManager = getPackageManager();
			    List<ResolveInfo> list = packageManager.queryBroadcastReceivers(touchBrightnessIntent, 0);
			    if (list != null && list.size() > 0) {
				Log.d(TAG, "Sending [" + touchButtonBrightness + "] to touch brightness");
				sendBroadcast(touchBrightnessIntent);
			    }
			}

			Intent updateIntent = new Intent();
			updateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			updateIntent.putExtra("factor", factor);
			updateIntent.setClass(UpdateService.this, UpdateBrightnessActivity.class);
			UpdateService.this.startActivity(updateIntent);
		    } finally {
			mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_STOP_SERVICE));
		    }
		}
	    });
	    return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
	    return null;
	}

    }

}
