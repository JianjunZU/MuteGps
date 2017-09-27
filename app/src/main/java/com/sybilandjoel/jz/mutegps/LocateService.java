package com.sybilandjoel.jz.mutegps;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.media.AudioManager.STREAM_MUSIC;

/**
 * Created by JZ on 2017/7/16.
 */

public class LocateService extends Service {
    MyDatabaseHelper dbHelper;
    private int min = 10;//后期用sharedpreference
    private int MUTE_FLAG = 0;

    private AMapLocationClient locationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != locationClient) {
            locationClient.onDestroy();
            locationClient = null;
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        dbHelper = new MyDatabaseHelper(this, "locs.db3", 1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                ///
                locationClient = new AMapLocationClient(getApplicationContext());
//初始化AMapLocationClientOption对象
                mLocationOption = new AMapLocationClientOption();
                mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
                mLocationOption.setOnceLocationLatest(true);
                locationClient.startLocation();

                AMapLocation location = locationClient.getLastKnownLocation();
                LatLng LocTmp = new LatLng(location.getLatitude(), location.getLongitude());
                SimpleDateFormat   TimeFormat   =   new SimpleDateFormat("yyyy年MM月dd日   HH:mm:ss");
                Date CurrentDate =  new Date(System.currentTimeMillis());
                String   time   =   TimeFormat.format(CurrentDate);
                try {
                    File file = new File("/mnt/sdcard/MuteGps/data/locations.txt");
                    FileOutputStream fos=new FileOutputStream(file,true);
                    PrintStream ps=new PrintStream(fos);
                    ps.println(time+" "+LocTmp);
                    ps.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("output", "error:"+e);
                }

                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Notification notify = new Notification.Builder(LocateService.this).setContentTitle("" + LocTmp)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.drawable.gps_point)
                        .build();
//                nm.notify((int)System.currentTimeMillis(), notify);
                nm.notify(0x123, notify);

                checkMute(LocTmp);
                locationClient.stopLocation();
            }
        }).start();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int SpanTime = min * 60 * 1000; // 这是10分钟的毫秒数
        long triggerAtTime = SystemClock.elapsedRealtime() + SpanTime;
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }

    private void checkMute(LatLng pos) {
        MUTE_FLAG = 0;
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery("select * from locs", null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext() && MUTE_FLAG == 0) {
                    LatLng loc = new LatLng(Double.parseDouble(cursor.getString(cursor.getColumnIndex("latitude"))),
                            Double.parseDouble(cursor.getString(cursor.getColumnIndex("longitude"))));
                    int radius = Integer.parseInt(cursor.getString(cursor.getColumnIndex("radius")));
                    String remark = cursor.getString(cursor.getColumnIndex("remark"));
                    float distance = AMapUtils.calculateLineDistance(loc, pos);
                 //   Log.e("distance", pos + "," + loc + "," + distance + "," + radius);
                    if (distance < radius) MUTE_FLAG = 1;

                }
                //
                AudioManager audio = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
                if (MUTE_FLAG == 1 && audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {

                    audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 1);
                } else if (MUTE_FLAG == 0 && audio.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                    audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, audio.getStreamMaxVolume(STREAM_MUSIC) / 2, 1);
                }
            }
        }
    }

}

