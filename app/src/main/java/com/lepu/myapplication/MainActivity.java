package com.lepu.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.jeremyliao.liveeventbus.LiveEventBus;
import com.lepu.blepro.constants.Ble;
import com.lepu.blepro.constants.Constant;
import com.lepu.blepro.event.EventMsgConst;
import com.lepu.blepro.event.InterfaceEvent;
import com.lepu.blepro.ext.Ad5Data;
import com.lepu.blepro.ext.BleServiceHelper;
import com.lepu.blepro.objs.Bluetooth;
import com.lepu.blepro.observer.BIOL;
import com.lepu.blepro.observer.BleChangeObserver;

/**
 * @author chenyongfeng
 */
public class MainActivity extends AppCompatActivity implements BleChangeObserver {

    private TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.data);
        initEvent();
        permission();
    }

    private void permission() {
        boolean granted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permission = new String[] {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            };
            for (int i=0; i<permission.length; i++) {
                if (checkSelfPermission(permission[i]) != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
        } else {
            String[] permission = new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
            };
            for (int i=0; i<permission.length; i++) {
                if (checkSelfPermission(permission[i]) != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
        }
        if (granted) {
            BleServiceHelper.Companion.getBleServiceHelper().initService(getApplication());
        } else {
            textView.setText("Please go to Settings to grant permissions");
        }
    }

    private void initEvent() {
        LiveEventBus.get(EventMsgConst.Ble.EventServiceConnectedAndInterfaceInit).observe(this, event -> {
            BleServiceHelper.Companion.getBleServiceHelper().setInterfaces(Bluetooth.MODEL_FETAL);
            getLifecycle().addObserver(new BIOL(this::onBleStateChanged, new  int[]{Bluetooth.MODEL_FETAL}));
            BleServiceHelper.Companion.getBleServiceHelper().startScan();
        });
        LiveEventBus.get(EventMsgConst.Discovery.EventDeviceFound, Bluetooth.class).observe(this, event -> {
            if (event.getName().equals("MD1000AF4")) {
                BleServiceHelper.Companion.getBleServiceHelper().stopScan();
                BleServiceHelper.Companion.getBleServiceHelper().connect(this, Bluetooth.MODEL_FETAL, event.getDevice());
            }
        });
        LiveEventBus.get(EventMsgConst.Ble.EventBleDeviceReady).observe(this, event -> {
            int model = (int) event;
            if (model == Bluetooth.MODEL_FETAL) {
                Toast.makeText(this, "connect success", Toast.LENGTH_SHORT).show();
            }
        });
        LiveEventBus.get(InterfaceEvent.AD5.EventAd5RtHr, InterfaceEvent.class).observe(this, event -> {
            Ad5Data data = (Ad5Data) event.getData();
            textView.setText("data : " + data);
        });
    }

    @Override
    public void onBleStateChanged(int model, int state) {
        // state : Ble.State
        Log.d("MainActivity onBleStateChanged", "model = " + model + ", state = " + state);
    }
}