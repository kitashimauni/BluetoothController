package com.example.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Queue;
import java.util.UUID;

public class Connect extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final MainActivity mainActivity;
    private final Queue<Byte> que;
    public Connect(MainActivity mainActivity, BluetoothDevice device, UUID uuid) {
        BluetoothSocket tmp = null;
        mmDevice = device;
        this.mainActivity = mainActivity;
        que = mainActivity.getQue();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast toast = Toast.makeText(mainActivity.getBaseContext(), "Bluetoothの接続を許可してください", Toast.LENGTH_SHORT);
                toast.show();
                mainActivity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 0);
            }
        }

        try {
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.e("Bluetooth Error", e + " in " + this);
        }

        mmSocket = tmp;
    }

    @Override
    public void run() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast toast = Toast.makeText(mainActivity.getBaseContext(), "Bluetoothの接続を許可してください", Toast.LENGTH_SHORT);
                    toast.show();
                    mainActivity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 0);
                }
            }
            Log.d("From", "ここから接続開始");
            mmSocket.connect();
        } catch (IOException connectException) {
            try {
                Log.e("Bluetooth Error", connectException + " in " + this);
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e("Bluetooth Error", closeException + " in " + this);
            }
            mainActivity.setIs_connecting(false);
            return;
        }

        try{
            OutputStream outputStream = mmSocket.getOutputStream();
            while(true){
                if(que.size() > 0){
                    Byte data = que.poll();
                    if(data == null) continue;
                    byte[] command = new byte[]{data};
                    outputStream.write(command);
                    if(data == 0x0) break;
                }
            }
        } catch (IOException e){
            Log.e("OutputStream Error", e + " in " + this);
            mainActivity.setIs_connecting(false);
            return;
        }

        try {
            mmSocket.close();
        } catch (IOException closeException) {
            Log.e("Bluetooth Error", closeException + " in " + this);
        } finally {
            mainActivity.setIs_connecting(false);
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException closeException) {
            Log.e("Bluetooth Error", closeException + " in " + this);
        } finally {
            mainActivity.setIs_connecting(false);
        }
    }
}
