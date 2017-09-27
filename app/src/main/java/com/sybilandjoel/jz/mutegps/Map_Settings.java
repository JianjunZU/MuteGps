package com.sybilandjoel.jz.mutegps;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;

import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.Projection;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.Circle;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;

import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.maps2d.model.Tile;


/**
 * Created by JZ on 2017/7/13.
 */

public class Map_Settings extends AppCompatActivity implements
        AMap.OnMapLongClickListener, LocationSource, AMapLocationListener, AMap.OnMarkerClickListener {
    private MapView mapView;
    private AMap aMap;
    private TextView loc;
    private SeekBar radius;
    private Button add;
    private MarkerOptions addMarker;
    private Marker markerNew;
    private AMapLocationClientOption mLocationOption;
    private AMapLocationClient mlocationClient;
    private CircleOptions addCircle;
    private Circle circleNew;
    private LatLng LocTmp;
    private int RadTmp = 0;
    private LocationSource.OnLocationChangedListener mListener;
    private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);
    private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);
    private int firstrun_flag = 0;
    MyDatabaseHelper dbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapsettings);
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        init();
        initLocDatabase();
    }

    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }

        add = (Button) findViewById(R.id.add);
        addMarker = new MarkerOptions();
        loc = (TextView) findViewById(R.id.loc);
        radius = (SeekBar) findViewById(R.id.radius);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText RM = new EditText(Map_Settings.this);
                RM.setInputType(InputType.TYPE_CLASS_TEXT);
                RM.setHint("请输入地点信息");



                new AlertDialog.Builder(Map_Settings.this)
                        .setTitle("填写备注").setMessage("该地点为：")
                        .setView(RM).
                        setPositiveButton("Add", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String input = RM.getText().toString();
                                if (input.equals("")) {
                                    Toast.makeText(Map_Settings.this, "输入不可为空", Toast.LENGTH_LONG).show();
                                } else {
                                    insertLoc(dbHelper.getReadableDatabase(), "" + LocTmp.latitude, "" + LocTmp.longitude, "" + RadTmp, input);

                                    refresh();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create().show();


            }
        });
        radius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //aMap.clear();
                if (markerNew != null) markerNew.remove();
                if (circleNew != null) circleNew.remove();
                addMarker.position(LocTmp);
                markerNew = aMap.addMarker(addMarker);
                addCircle = new CircleOptions()
                        .center(LocTmp)
                        .fillColor(FILL_COLOR)
                        .radius(progress)
                        .strokeWidth(2)
                        .strokeColor(STROKE_COLOR);
                circleNew = aMap.addCircle(addCircle);
                RadTmp = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }

    public void initLocDatabase() {
        dbHelper = new MyDatabaseHelper(this, "locs.db3", 1);
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery("select * from locs", null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    LatLng locExistTmp = new LatLng(Double.parseDouble(cursor.getString(cursor.getColumnIndex("latitude"))),
                            Double.parseDouble(cursor.getString(cursor.getColumnIndex("longitude"))));
                    int radiusExistTmp = Integer.parseInt(cursor.getString(cursor.getColumnIndex("radius")));
                    String remark = cursor.getString(cursor.getColumnIndex("remark"));
                    int id = cursor.getInt(cursor.getColumnIndex("_id"));
                    MarkerOptions markerExistTmp = new MarkerOptions().icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)).
                            position(locExistTmp).title("" + id).snippet(remark);
                    aMap.addMarker(markerExistTmp);

                    CircleOptions circleExistTmp = new CircleOptions().
                            center(locExistTmp).radius(radiusExistTmp).
                            fillColor(0x20ffff00).strokeWidth(2);
                    aMap.addCircle(circleExistTmp);

                }
            }
        }
    }

    private void setUpMap() {
        aMap.setOnMarkerClickListener(this);
//        aMap.setOnMapClickListener(this);
        aMap.setOnMapLongClickListener(this);
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        setupLocationStyle();

    }

    private void setupLocationStyle() {
        // 自定义系统定位蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        // 自定义定位蓝点图标
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.
                fromResource(R.drawable.gps_point));
        // 自定义精度范围的圆形边框颜色
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));
        //自定义精度范围的圆形边框宽度
        myLocationStyle.strokeWidth(0);
        // 设置圆形的填充颜色
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));
        // 将自定义的 myLocationStyle 对象添加到地图上
        aMap.setMyLocationStyle(myLocationStyle);
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null
                    && amapLocation.getErrorCode() == 0) {

                if (firstrun_flag++ == 0) {
                    LocTmp = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
                    add_M_C(LocTmp);
                }
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
                aMap.moveCamera(CameraUpdateFactory.zoomTo(18));
            } else {

            }
        }
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

//    @Override
//    public void onMapClick(LatLng point) {
//        add_M_C(point);
//    }

    @Override
    public void onMapLongClick(LatLng point) {

        add_M_C(point);
    }

    private void add_M_C(LatLng point) {
        if (markerNew != null) markerNew.remove();
        if (circleNew != null) circleNew.remove();
        addMarker.position(point);
        markerNew = aMap.addMarker(addMarker);

        addCircle = new CircleOptions()
                .center(point)
                .fillColor(FILL_COLOR)
                .radius(radius.getProgress())
                .strokeWidth(2)
                .strokeColor(STROKE_COLOR);
        circleNew = aMap.addCircle(addCircle);
        LocTmp = point;
        RadTmp = radius.getProgress();
        loc.setText(" " + point);
    }

    private void insertLoc(SQLiteDatabase db, String latitude, String longitude, String radius, String remark) {
        db.execSQL("insert into locs values(null,?,?,?,?)", new String[]{latitude, longitude, radius, remark});
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        if (aMap != null) {
            if (marker.getTitle() != null) {
                new AlertDialog.Builder(this).setTitle(
                        "Location " + marker.getTitle()).
                        setMessage(marker.getSnippet()).
                setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.getReadableDatabase().delete("locs", "_id=?", new String[]{marker.getTitle()});
                        refresh();
                    }
                }).
                setNegativeButton("Cancel",null).create().show();
            } else {
                jumpPoint(marker);
            }
        }

        //Toast.makeText(Map_Settings.this, "您点击了Marker", Toast.LENGTH_LONG).show();
        return true;
    }






    /**
     * marker点击时跳动一下
     */
    public void jumpPoint(final Marker marker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = aMap.getProjection();
        final LatLng markerLatlng = marker.getPosition();
        Point markerPoint = proj.toScreenLocation(markerLatlng);
        markerPoint.offset(0, -100);
        final LatLng startLatLng = proj.fromScreenLocation(markerPoint);
        final long duration = 1500;

        final Interpolator interpolator = new BounceInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * markerLatlng.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * markerLatlng.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));
                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    private void refresh() {
        Intent intent = new Intent(Map_Settings.this, Map_Settings.class);
        finish();
        startActivity(intent);

    }
}
