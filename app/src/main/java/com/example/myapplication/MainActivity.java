package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.PatternMatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final byte leftKey = (byte) 0x25;
    private static final byte rightKey = (byte) 0x27;
    private static final byte media_play_pause = (byte) 0xB3;
    private static final byte media_prev = (byte) 0xB1;
    private static final byte media_next = (byte) 0xB0;
    private Queue<Byte> que;

    private boolean is_connecting = false;
    private View status_button;
    private Connect connect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
//                    != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(
//                        MainActivity.this,
//                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
//                        0);
//            }
//        }

        que = new ArrayDeque<>();

        View button = findViewById(R.id.button);
        status_button = button;
        setIs_connecting(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(is_connecting){
                    Toast toast = Toast.makeText(getBaseContext(), "接続を解除します", Toast.LENGTH_SHORT);
                    toast.show();
                    que.add((byte) 0x00);
                    try{
                        Thread.sleep(100);
                    } catch (Exception e) {
                        Log.d("Interrupted", e.toString());
                    }
                    if(connect != null && connect.isAlive()){
                        connect.cancel();
                    }
                    setIs_connecting(false);
                } else {
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (!bluetoothAdapter.isEnabled()) {
                        Toast toast = Toast.makeText(getBaseContext(), "Bluetoothをオンにしてください", Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            Toast toast = Toast.makeText(getBaseContext(), "Bluetoothの接続を許可してください", Toast.LENGTH_SHORT);
                            toast.show();
                            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 0);
                        }
                    }
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    if (pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            String name = device.getName();
                            String mac = device.getAddress();
                            Log.d("Names", name + ": " + mac);
                        }
                    }

                    // String mac_address = "C8:2A:DD:AC:1D:62";
                    String mac_address = "08:BE:AC:3E:2A:9A";
                    // シリアルポート通信のUUID
                    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                    // UUID uuid = UUID.nameUUIDFromBytes(new byte[] {(byte) 0x1124});
                    BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(mac_address);
                    Log.d("Discovering", "is " + bluetoothAdapter.isDiscovering());

                    connect = new Connect(MainActivity.this, bluetoothDevice, uuid);
                    Thread thread = new Thread(connect);
                    que.clear();
                    setIs_connecting(true);
                    thread.start();
                }
            }
        });

        Button leftButton = findViewById(R.id.left_button);
        leftButton.setOnClickListener(v -> que.add(leftKey));

        Button rightButton = findViewById(R.id.right_button);
        rightButton.setOnClickListener(v -> que.add(rightKey));

        Button mediaPlayPauseButton = findViewById(R.id.media_play_pause_button);
        mediaPlayPauseButton.setOnClickListener(v -> que.add(media_play_pause));

        Button mediaPrevButton = findViewById(R.id.media_prev_button);
        mediaPrevButton.setOnClickListener(v -> que.add(media_prev));

        Button mediaNextButton = findViewById(R.id.media_next_button);
        mediaNextButton.setOnClickListener(v -> que.add(media_next));
    }

    public Queue<Byte> getQue() {
        return que;
    }

    public void setIs_connecting(boolean is_connecting) {
        this.is_connecting = is_connecting;
        if(is_connecting){
            ((Button) status_button).setText("接続解除");
        }else{
            ((Button) status_button).setText("接続開始");
        }
    }
}