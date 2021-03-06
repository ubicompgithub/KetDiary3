package com.ubicomp.ketdiary;


import java.util.Calendar;

import com.ubicomp.ketdiary.data.db.DatabaseControl;
import com.ubicomp.ketdiary.data.upload.DataUploader;
import com.ubicomp.ketdiary.system.PreferenceControl;
import com.ubicomp.ketdiary.system.check.DefaultCheck;
import com.ubicomp.ketdiary2.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class NoteThinkingService extends Service {
	/**
	 * Start the uploading service
	 * 
	 * @param context
	 *            Application context
	 */
	public static void startUploadService(Context context) {
		Intent intent = new Intent(context, NoteThinkingService.class);
		context.startService(intent);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		DatabaseControl db = new DatabaseControl();

		/*Calendar cal = Calendar.getInstance();

		if (db.detectionIsDone())
			return Service.START_REDELIVER_INTENT;

		int cur_hour = cal.get(Calendar.HOUR_OF_DAY);
		if (cur_hour < 8)
			return Service.START_REDELIVER_INTENT;
		
		
		if (PreferenceControl.getInTest())         //If user in the test, disable notification.
			return Service.START_REDELIVER_INTENT;*/
		

		Intent mIntent = new Intent(this, MainActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, mIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		Notification notification;

		String title = getResources().getString(R.string.app_name);
		String msgText = getResources().getString(R.string.notification_msg_2);

		// Generate notification for SDK_INT >= 11
		if (Build.VERSION.SDK_INT >= 11) {

			Notification.Builder notificationBuilder = new Notification.Builder(
					getBaseContext());

			notificationBuilder.setContentTitle(title);
			notificationBuilder.setContentText(msgText);
			notificationBuilder.setSmallIcon(R.drawable.k_noti_logo);
			notificationBuilder.setContentIntent(pIntent);

			if (Build.VERSION.SDK_INT < 16)
				// Generate notification for SDK_INT < 16
				notification = notificationBuilder.getNotification();
			else
				// Generate notification for SDK_INT >= 16
				notification = notificationBuilder.build();

			notification.defaults = Notification.DEFAULT_ALL;
			notification.flags |= Notification.FLAG_AUTO_CANCEL;

			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(0, notification);
		} else {
			// Generate notification for SDK_INT < 11
			notification = new Notification();
			notification.defaults = Notification.DEFAULT_ALL;
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			notification.setLatestEventInfo(getBaseContext(), title, msgText,
					pIntent);
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(0, notification);
		}
		stopSelf();

		return Service.START_REDELIVER_INTENT;
	}
}

