package com.roboami.ryan.pi_tooth;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;

    final byte delimiter = 33;
    int readBufferPosition = 0;

    Handler handler;
    Button lockButton;
    TextView myLabel;
    Button unlockButton;

    InputStream mmInputStream;
    OutputStream mmOutputStream;

    AlarmManager alarm_manager;
    TimePicker time_picker;
    TextView update_text;
    Context context;
    PendingIntent pending_intent;


    public void sendBtMsg(String msg2send){
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        //UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID
        try {

            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            Log.e("log", mmSocket.toString());
            if (!mmSocket.isConnected()){
                mmSocket.connect();
            }

            String msg = msg2send;
            //msg += "\n";
            mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(msg.getBytes());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toast.makeText(MainActivity.this, "ON CREATE",
                Toast.LENGTH_LONG).show();

        setContentView(R.layout.activity_main);



        handler = new Handler();

        myLabel = (TextView) findViewById(R.id.btResult);
        lockButton = (Button) findViewById(R.id.LockButton);
        unlockButton = (Button) findViewById(R.id.UnlockButton);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        final class workerThread implements Runnable {

            private String btMsg;

            public workerThread(String msg) {
                btMsg = msg;
            }

            public void run()
            {
                sendBtMsg(btMsg);
                while(!Thread.currentThread().isInterrupted())
                {
                    int bytesAvailable;
                    boolean workDone = false;

                    try {



                        final InputStream mmInputStream;
                        mmInputStream = mmSocket.getInputStream();
                        bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {

                            byte[] packetBytes = new byte[bytesAvailable];
                            Log.e(" Aquariumrecv bt", "bytes available");
                            byte[] readBuffer = new byte[1024];
                            mmInputStream.read(packetBytes);
                            Log.e("data receive", Integer.toString(bytesAvailable));
                            String data_receive = new String(packetBytes);
                            Log.e("data receive", data_receive);


                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    //The variable data now contains our full command
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            Log.e("data receive",data);
                                            Toast.makeText(MainActivity.this, "this is my Toast message!!! =)",
                                                    Toast.LENGTH_LONG).show();
                                            myLabel.setText(data);
                                        }
                                    });

                                    workDone = true;
                                    break;


                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }

                            if (workDone == true){
                                mmSocket.close();
                                break;
                            }

                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }
        };


        // start temp button handler

        lockButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on temp button click

                (new Thread(new workerThread("lock"))).start();

            }
        });


        //end temp button handler

        //start light on button handler
        unlockButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on temp button click

                (new Thread(new workerThread("unlock"))).start();

            }
        });
        //end light on button handler

        //start light off button handler

        // end light off button handler

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("raspberrypi")) //Note, you will need to change this to match the name of your device
                {
                    Log.e("log",device.getName());
                    mmDevice = device;

                    Method getUuidsMethod = null;
                    try {
                        getUuidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }

                    ParcelUuid[] uuids = new ParcelUuid[0];
                    try {
                        uuids = (ParcelUuid[]) getUuidsMethod.invoke(mBluetoothAdapter, null);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    for (ParcelUuid uuid: uuids) {
                        Log.d(null, "UUID: " + uuid.getUuid().toString());
                    }

                    break;
                }


            }

        }

    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }




    @Override
    public void onStart ()
    {
        super.onStart();
        //Toast.makeText(this, "ON START!", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onRestart ()
    {
        super.onRestart();
        // Toast.makeText(this, "ON RESTART!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume ()
    {
        super.onResume();
        //Toast.makeText(this, "ON RESUME!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause ()
    {
        super.onPause();
        Toast.makeText(this, "ON PAUSE!", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onAttachedToWindow() {
        Window window = getWindow();

        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onAttachedToWindow();
    }



    @Override
    public void onStop ()
    {
        super.onStop();
        Toast.makeText(this, "ON STOP!", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onDestroy ()
    {
        super.onDestroy();
        //Toast.makeText(this, "ON DESTROY!", Toast.LENGTH_SHORT).show();
    }

    // functii folosite pentru salvarea si restaurarea starii

    @Override
    public void onSaveInstanceState (Bundle outState)
    {
        // apelarea functiei din activitatea parinte este recomandata, dar nu obligatorie
        super.onSaveInstanceState(outState);
        //Toast.makeText(this, "ON SAVE INSTANCE STATE!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRestoreInstanceState (Bundle inState)
    {
        // apelarea functiei din activitatea parinte este recomandata, dar nu obligatorie
        super.onRestoreInstanceState(inState);
        //Toast.makeText(this, "ON RESTORE INSTANCES STATE!", Toast.LENGTH_SHORT).show();
    }


}