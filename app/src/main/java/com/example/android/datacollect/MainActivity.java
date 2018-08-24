package com.example.android.datacollect;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView timer;
    private static List<Float> accel;
    private static List<Float> gyro;
    private static List<Float> rAccel;
    private RadioButton jog;
    private RadioButton walk;
    private Switch loop;
    private Context cont = this;

    private int numReadings;

    private String accelX;
    private String accelY;
    private String accelZ;
    private String gyrX;
    private String gyrY;
    private String gyrZ;
    private String rAccelX;
    private String rAccelY;
    private String rAccelZ;
    private String action128;

    private static int monitor = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        numReadings = 0;
        accelX = "";
        accelY = "";
        accelZ = "";
        gyrX = "";
        gyrY = "";
        gyrZ = "";
        rAccelX = "";
        rAccelY = "";
        rAccelZ = "";
        action128 = "";

        timer = findViewById(R.id.timer);
        jog = findViewById(R.id.jogg);
        walk = findViewById(R.id.walk);
        loop = findViewById(R.id.loop);

        accel = new ArrayList<>();
        gyro = new ArrayList<>();
        rAccel = new ArrayList<>();

        verifyStoragePermissions(this);

        final RadioGroup radioGroup = findViewById(R.id.rgroup);
        final Button start = findViewById(R.id.start);
        final Button stop = findViewById(R.id.stop);
        start.setEnabled(false);
        stop.setEnabled(false);
        loop.setEnabled(false);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                start.setEnabled(true);
                loop.setEnabled(true);
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                start.setEnabled(false);
                jog.setEnabled(false);
                walk.setEnabled(false);
                loop.setEnabled(false);
                stop.setEnabled(true);
                writeId(""+(Integer.parseInt(readId())+1));
                final CountDownTimer countTime = new CountDownTimer(65000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        if(millisUntilFinished / 1000 > 60){
                            String timerText = "Data Collection starts in: " + ((millisUntilFinished / 1000)-60) + " seconds ";
                            timer.setText(timerText);
                        } else {
                            monitor = 1;
                            String timerText = "Monitoring activity: " + millisUntilFinished / 1000 + " seconds ";
                            timer.setText(timerText);
                        }
                    }

                    public void onFinish() {
                        if(loop.isChecked()){
                            start.setEnabled(false);
                            jog.setEnabled(false);
                            walk.setEnabled(false);
                            loop.setEnabled(false);
                            stop.setEnabled(true);
                            writeId(""+(Integer.parseInt(readId())+1));
                            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600);
                            this.start();
                        } else {
                            monitor = 0;
                            start.setEnabled(true);
                            jog.setEnabled(true);
                            walk.setEnabled(true);
                            loop.setEnabled(true);
                            stop.setEnabled(false);
                            String done = "done!";
                            timer.setText(done);
                            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600);
                        }
                    }
                }.start();

                stop.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        loop.setChecked(false);
                        countTime.cancel();
                        countTime.onFinish();
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed(){

    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void writeToFile(String data, String fileName) {
        Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

        File file;
        FileOutputStream outputStream;
        try {
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            outputStream = new FileOutputStream(file, true);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //function used for every time the sensors read a change they store the data
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;
        if(monitor == 1) {
            if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                accel.add(sensorEvent.values[0]);
                accel.add(sensorEvent.values[1]);
                accel.add(sensorEvent.values[2]);
            } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                gyro.add(sensorEvent.values[0]);
                gyro.add(sensorEvent.values[1]);
                gyro.add(sensorEvent.values[2]);
            } else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                rAccel.add(sensorEvent.values[0]);
                rAccel.add(sensorEvent.values[1]);
                rAccel.add(sensorEvent.values[2]);
            }

            if (accel.size() == 3 && gyro.size() == 3 && rAccel.size() == 3) {
                int type = 0;
                if(jog.isChecked()){
                    type = 2;
                } else {
                    type = 1;
                }
                SimpleDateFormat curtimesdf = new SimpleDateFormat("HH:mm:ss");
                String curtime = curtimesdf.format(new Date());
                SimpleDateFormat curdatesdf = new SimpleDateFormat("dd/MM/yyyy");
                String curdate = curdatesdf.format(new Date());
                String curId = readId();

                numReadings++;
                accelX = accelX + " " + accel.get(0) + " ";
                accelY = accelY + " " + accel.get(1) + " ";
                accelZ = accelZ + " " + accel.get(2) + " ";
                gyrX = gyrX + " " + gyro.get(0) + " ";
                gyrY = gyrY + " " + gyro.get(1) + " ";
                gyrZ = gyrZ + " " + gyro.get(2) + " ";
                rAccelX = rAccelX + " " + rAccel.get(0) + " ";
                rAccelY = rAccelY + " " + rAccel.get(1) + " ";
                rAccelZ = rAccelZ + " " + rAccel.get(2) + " ";

                String toWrite = accel.get(0) + "\t" + accel.get(1) + "\t" + accel.get(2) + "\t" +
                        gyro.get(0) + "\t" + gyro.get(1) + "\t" + gyro.get(2) + "\t" +
                        rAccel.get(0) + "\t" + rAccel.get(1) + "\t" + rAccel.get(2) + "\t" + type +
                        "\t" + curtime + "\t" + curdate + "\t" + curId + "\n";
                writeToFile(toWrite, "dataCollection.txt");

                //accel is without gravity(body for old data), raw is with
                if(numReadings == 128){
                    Random random = new Random();
                    int testTrain = random.nextInt((100 - 1) + 1) + 1;

                    if(testTrain <= 30){
                        action128 = "" + type;
                        writeToFile(action128 + "\n", "y_test.txt");
                        writeToFile(accelX + "\n", "body_acc_x_test.txt");
                        writeToFile(accelY + "\n", "body_acc_y_test.txt");
                        writeToFile(accelZ + "\n", "body_acc_z_test.txt");
                        writeToFile(gyrX + "\n", "body_gyro_x_test.txt");
                        writeToFile(gyrY + "\n", "body_gyro_y_test.txt");
                        writeToFile(gyrZ + "\n", "body_gyro_z_test.txt");
                        writeToFile(rAccelX + "\n", "total_acc_x_test.txt");
                        writeToFile(rAccelY + "\n", "total_acc_y_test.txt");
                        writeToFile(rAccelZ + "\n", "total_acc_z_test.txt");
                    } else {
                        action128 = "" + type;
                        writeToFile(action128 + "\n", "y_train.txt");
                        writeToFile(accelX + "\n", "body_acc_x_train.txt");
                        writeToFile(accelY + "\n", "body_acc_y_train.txt");
                        writeToFile(accelZ + "\n", "body_acc_z_train.txt");
                        writeToFile(gyrX + "\n", "body_gyro_x_train.txt");
                        writeToFile(gyrY + "\n", "body_gyro_y_train.txt");
                        writeToFile(gyrZ + "\n", "body_gyro_z_train.txt");
                        writeToFile(rAccelX + "\n", "total_acc_x_train.txt");
                        writeToFile(rAccelY + "\n", "total_acc_y_train.txt");
                        writeToFile(rAccelZ + "\n", "total_acc_z_train.txt");
                    }
                    numReadings = 0;
                    accelX = "";
                    accelY = "";
                    accelZ = "";
                    gyrX = "";
                    gyrY = "";
                    gyrZ = "";
                    rAccelX = "";
                    rAccelY = "";
                    rAccelZ = "";
                    action128 = "";
                }

                accel.clear();
                gyro.clear();
                rAccel.clear();
            }
        } else {
            numReadings = 0;
            accelX = "";
            accelY = "";
            accelZ = "";
            gyrX = "";
            gyrY = "";
            gyrZ = "";
            rAccelX = "";
            rAccelY = "";
            rAccelZ = "";
            action128 = "";
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    protected void onPause() {
        //if program is paused stop listening to the sensors
        getSensorManager().unregisterListener(this);
        super.onPause();
        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);

        activityManager.moveTaskToFront(getTaskId(), 0);
    }

    protected void onResume() {
        //if program is resumed start listening to the sensors again
        super.onResume();
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
    }

    private SensorManager getSensorManager() {
        //get our sensor manager
        return (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    private void writeId(String data) {
        Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        String fileName = "id.txt";

        File file = null;
        FileOutputStream outputStream;
        try {
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            outputStream = new FileOutputStream(file);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readId() {
        Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        String ret = "";

        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "id.txt");
            InputStream inputStream = new FileInputStream(file);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
            else {
                return "null";
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
            return e.toString();
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
            return e.toString();
        }

        return ret;
    }

}
