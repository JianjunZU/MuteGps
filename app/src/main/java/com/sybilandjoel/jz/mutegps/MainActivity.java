package com.sybilandjoel.jz.mutegps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.CHANGE_CONFIGURATION;
import static android.Manifest.permission.CHANGE_WIFI_STATE;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WAKE_LOCK;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_SETTINGS;

public class MainActivity extends AppCompatActivity {
    MyDatabaseHelper dbHelper;
private Button add;
    private Button mute;
    private String[] PERMISSIONS={INTERNET,WRITE_EXTERNAL_STORAGE,ACCESS_COARSE_LOCATION,ACCESS_NETWORK_STATE,
            ACCESS_FINE_LOCATION,READ_PHONE_STATE,CHANGE_WIFI_STATE,ACCESS_WIFI_STATE,CHANGE_CONFIGURATION
    ,WAKE_LOCK,WRITE_SETTINGS,MOUNT_UNMOUNT_FILESYSTEMS,WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setContentView(R.layout.activity_main);
        add=(Button)findViewById(R.id.add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,Map_Settings.class);
                startActivity(intent);
            }
        });
        mute=(Button) findViewById(R.id.mute);
        mute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,LocateService.class);
                startService(intent);
            }
        });
    }
    protected void init(){
        int permission = ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permission!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,PERMISSIONS,1);
        }
        dbHelper = new MyDatabaseHelper(this,"locs.db3",1);
        if (dbHelper!=null){
            dbHelper.close();
        }
        File file = new File("/mnt/sdcard/MuteGps/data/");
        if (!file.exists()) {
            try {
                //按照指定的路径创建文件夹
                file.mkdirs();
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        File dir = new File("/mnt/sdcard/MuteGps/data/locations.txt");
        if (!dir.exists()) {
            try {
                //在指定的文件夹中创建文件
                dir.createNewFile();
            } catch (Exception e) {
            }
        }
    }
}
