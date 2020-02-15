/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.nophonespam;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.Method;

import at.bitfire.nophonespam.model.DbHelper;
import at.bitfire.nophonespam.model.Number;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "NoPhoneSpam";

    private static final int NOTIFY_REJECTED = 0;
    private static boolean AlreadyOnCall = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction()) &&
                intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            /* swy: we can receive two notifications; the first one doesn't
                    have EXTRA_INCOMING_NUMBER, so just skip it */
            if (incomingNumber == null)
                return;

            Log.i(TAG, "Received call: " + incomingNumber);

            Settings settings = new Settings(context);
            if (TextUtils.isEmpty(incomingNumber)) {
                // private number (no caller ID)
                if (settings.blockHiddenNumbers())
                    rejectCall(context, null);

            } else {
                DbHelper dbHelper = new DbHelper(context);
                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    Cursor c = db.query(Number._TABLE, null, "? LIKE " + Number.NUMBER, new String[] { incomingNumber }, null, null, null);
                    boolean inList = c.moveToNext();
                    if (inList && !settings.whitelist()) {
                        ContentValues values = new ContentValues();
                        DatabaseUtils.cursorRowToContentValues(c, values);
                        Number number = Number.fromValues(values);

                        rejectCall(context, number);

                        values.clear();
                        values.put(Number.LAST_CALL, System.currentTimeMillis());
                        values.put(Number.TIMES_CALLED, number.timesCalled + 1);
                        db.update(Number._TABLE, values, Number.NUMBER + "=?", new String[]{number.number});

                        BlacklistObserver.notifyUpdated();

                    } else if (!inList && settings.whitelist()) {
                        Number number = new Number();
                        number.number = incomingNumber;
                        number.name = context.getResources().getString(R.string.receiver_notify_unknown_caller);

                        rejectCall(context, number);
                        BlacklistObserver.notifyUpdated();
                    }
                    c.close();
                } finally {
                    dbHelper.close();
                }
            }
        }

        /* swy: keep track of any calls that may already be happening while someone else tries to call us;
                we don't want to interrupt actual, running calls by mistake */
             if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) AlreadyOnCall = true;
        else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE))    AlreadyOnCall = false;
    }

    @SuppressLint("MissingPermission")
    protected void rejectCall(@NonNull Context context, Number number) {
        if (!AlreadyOnCall) {
            boolean failed = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);

                try {
                    telecomManager.endCall();
                    Log.d(TAG, "Invoked 'endCall' on TelecomManager");
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't end call with TelecomManager", e);
                    failed = true;
                }
            } else {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                try {
                    Method m = tm.getClass().getDeclaredMethod("getITelephony");
                    m.setAccessible(true);

                    ITelephony telephony = (ITelephony) m.invoke(tm);

                    telephony.endCall();
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't end call with TelephonyManager", e);
                    failed = true;
                }
            }
            if (failed) {
                Toast.makeText(context, context.getString(R.string.call_blocking_unsupported), Toast.LENGTH_LONG).show();
            }
        }

        Settings settings = new Settings(context);
        if (settings.showNotifications()) {


            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager notificationManager =  (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel channel = new NotificationChannel(
                        "default", context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription(context.getString(R.string.receiver_notify_call_rejected));
                notificationManager.createNotificationChannel(channel);
            }

            Notification notify = new NotificationCompat.Builder(context, "M_CH_ID")
                    .setSmallIcon(R.drawable.ic_launcher_small)
                    .setContentTitle(context.getString(R.string.receiver_notify_call_rejected))
                    .setContentText(number != null ? (number.name != null ? number.name : number.number) : context.getString(R.string.receiver_notify_private_number))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setShowWhen(true)
                    .setAutoCancel(true)
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, BlacklistActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                    .addPerson("tel:" + number)
                    .setGroup("rejected")
                    .setChannelId("default")
                    .setGroupSummary(true) /* swy: fix notifications not appearing on kitkat: https://stackoverflow.com/a/37070917/674685 */
                    .build();

            String tag = number != null ? number.number : "private";
            NotificationManagerCompat.from(context).notify(tag, NOTIFY_REJECTED, notify);
        }

    }

}
