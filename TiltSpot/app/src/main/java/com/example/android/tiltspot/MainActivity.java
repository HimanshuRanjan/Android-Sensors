
package com.example.android.tiltspot;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Date;

import static android.os.SystemClock.sleep;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    // System sensor manager instance.
    private SensorManager mSensorManager;
    private DatabaseReference mDatabase;
    // Accelerometer and magnetometer sensors, as retrieved from the
    // sensor manager.
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetometer;

    // Current data from accelerometer & magnetometer.  The arrays hold values
    // for X, Y, and Z.
    private float[] mAccelerometerData = new float[3];
    private float[] mMagnetometerData = new float[3];
    private GoogleSignInClient mGoogleSignInClient;

    // TextViews to display current sensor values.
    private TextView mTextSensorAzimuth;
    private TextView mTextSensorPitch;
    private TextView Name;
    private TextView mTextSensorRoll;
    // ImageView drawables to display spots.
    private ImageView mSpotTop;
    private ImageView mSpotBottom;
    private ImageView mSpotLeft;
    private ImageView mSpotRight;
    private FirebaseAuth mAuth;
    RadioButton r1;
    RadioButton r2;
    private int pocket;
    /* 1 - upper pocket
        2 - lower pocket */

    private int position;
    /* 1 - upper pocket standing
        2 - upper pocket sitting
        3 - upper pocket sleeping
        4 - lower pocket standing
        5 - lower pocket sitting
        6 - lower pocket sleeping
     */
    // System display. Need this for determining rotation.
    private Display mDisplay;
    float orientationX;
    float orientationY;
    float orientationZ;
    // Very small values for the accelerometer (on all three axes) should
    // be interpreted as 0. This value is the amount of acceptable
    // non-zero drift.
    private static final float VALUE_DRIFT = 0.05f;
    ProgressDialog progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        /*mTextSensorAzimuth = (TextView) findViewById(R.id.value_azimuth);
        mTextSensorPitch = (TextView) findViewById(R.id.value_pitch);
        mTextSensorRoll = (TextView) findViewById(R.id.value_roll);*/
        Name = (TextView) findViewById(R.id.textView);
        mSpotTop = (ImageView) findViewById(R.id.spot_top);
        mSpotBottom = (ImageView) findViewById(R.id.spot_bottom);
        /*mSpotLeft = (ImageView) findViewById(R.id.spot_left);
        mSpotRight = (ImageView) findViewById(R.id.spot_right);*/
        r1 = (RadioButton) findViewById(R.id.upper);
        r2 = (RadioButton) findViewById(R.id.lower);
        // Get accelerometer and magnetometer sensors from the sensor manager.
        // The getDefaultSensor() method returns null if the sensor
        // is not available on the device.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(getApplicationContext(), gso);
        progressBar = new ProgressDialog(this);
        mSensorManager = (SensorManager) getSystemService(
                Context.SENSOR_SERVICE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        mSensorMagnetometer = mSensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD);

        // Get the display from the window manager (for rotation).
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();

        mAuth = FirebaseAuth.getInstance();
        Name.setText("Welcome : Himanshu Ranjan");
    }
    public void signout(View v){
        mAuth.signOut();
        // Google revoke access
        mGoogleSignInClient.revokeAccess().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Intent i = new Intent(getApplicationContext(), FullscreenActivity.class);
                        startActivity(i);
                        progressBar.dismiss();
                        finish();
                    }
                });
    }

    /**
     * Listeners for the sensors are registered in this callback so that
     * they can be unregistered in onStop().
     */
    @Override
    protected void onStart() {
        super.onStart();

        // Listeners for the sensors are registered in this callback and
        // can be unregistered in onStop().
        //
        // Check to ensure sensors are available before registering listeners.
        // Both listeners are registered with a "normal" amount of delay
        // (SENSOR_DELAY_NORMAL).
        if (mSensorAccelerometer != null) {
            mSensorManager.registerListener(this, mSensorAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mSensorMagnetometer != null) {
            mSensorManager.registerListener(this, mSensorMagnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // The sensor type (as defined in the Sensor class).
        int sensorType = sensorEvent.sensor.getType();

        // The sensorEvent object is reused across calls to onSensorChanged().
        // clone() gets a copy so the data doesn't change out from under us
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                mAccelerometerData[0] = sensorEvent.values[0];
                mAccelerometerData[1] = sensorEvent.values[1];
                mAccelerometerData[2] = sensorEvent.values[2];
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagnetometerData = sensorEvent.values.clone();
                break;
            default:
                return;
        }
        // Compute the rotation matrix: merges and translates the data
        // from the accelerometer and magnetometer, in the device coordinate
        // system, into a matrix in the world's coordinate system.
        //
        // The second argument is an inclination matrix, which isn't
        // used in this example.
        float[] rotationMatrix = new float[9];
        boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                null, mAccelerometerData, mMagnetometerData);

        // Remap the matrix based on current device/activity rotation.
        float[] rotationMatrixAdjusted = new float[9];
        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                rotationMatrixAdjusted = rotationMatrix.clone();
                break;
            case Surface.ROTATION_90:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X,
                        rotationMatrixAdjusted);
                break;
            case Surface.ROTATION_180:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,
                        rotationMatrixAdjusted);
                break;
            case Surface.ROTATION_270:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
                        rotationMatrixAdjusted);
                break;
        }

        // Get the orientation of the device (azimuth, pitch, roll) based
        // on the rotation matrix. Output units are radians.
        final float orientationValues[] = new float[3];
        if (rotationOK) {
            SensorManager.getOrientation(rotationMatrixAdjusted,
                    orientationValues);
        }

        // Pull out the individual values from the array.
        float azimuth = orientationValues[0];
        float pitch = orientationValues[1];
        float roll = orientationValues[2];
        /*final Button button =(Button) findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                orientationX = orientationValues[0];
                orientationY = orientationValues[1];
                orientationZ = orientationValues[2];
            }
        });*/
        sleep(10);
        if(Math.abs(pitch) >1 && Math.abs(roll) <2.5 && position != 1 && r1.isChecked())
        {
            position = 1;
            mSpotTop.setImageResource(R.drawable.personstanding);
            mSpotBottom.setImageResource(R.drawable.imagecellphoneshirtpocket);

            Date currentTime = Calendar.getInstance().getTime();
            User user = new User(currentTime.toString(),position);
			mDatabase.child("users").child(mAuth.getCurrentUser().getUid().toString()).child(currentTime.toString()).setValue(user);
            Log.i("XXX",user.timestamp );

            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
        }

        if(Math.abs(pitch) < 0.2 && Math.abs(roll) > 2.5 && position != 3 && r1.isChecked())
        {
            position = 3;
            mSpotTop.setImageResource(R.drawable.sleep);
            mSpotBottom.setImageResource(R.drawable.imagecellphoneshirtpocket);
            Date currentTime = Calendar.getInstance().getTime();
            User user = new User(currentTime.toString(),position);
            mDatabase.child("users").child(mAuth.getCurrentUser().getUid().toString()).child(currentTime.toString()).setValue(user);
            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
        }
        if(Math.abs(pitch) >1 && Math.abs(roll) <2.5 && position != 4 && r2.isChecked())
        {
            position = 4;
            mSpotTop.setImageResource(R.drawable.personstanding);
            mSpotBottom.setImageResource(R.drawable.lower);

            Date currentTime = Calendar.getInstance().getTime();
            User user = new User(currentTime.toString(),position);
            mDatabase.child("users").child(mAuth.getCurrentUser().getUid().toString()).child(currentTime.toString()).setValue(user);
            Log.i("XXX",user.timestamp );
            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
        }

        if(Math.abs(pitch) < 0.2 && Math.abs(roll) < 2.5 && position != 5 && r2.isChecked())
        {
            position = 5;
            mSpotTop.setImageResource(R.drawable.sleep);
            mSpotBottom.setImageResource(R.drawable.lower);
            Date currentTime = Calendar.getInstance().getTime();
            User user = new User(currentTime.toString(),position);
            mDatabase.child("users").child(mAuth.getCurrentUser().getUid().toString()).child(currentTime.toString()).setValue(user);
            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
        }
        // Pitch and roll values that are close to but not 0 cause the
        // animation to flash a lot. Adjust pitch and roll to 0 for very
        // small values (as defined by VALUE_DRIFT).
        if (Math.abs(pitch) < VALUE_DRIFT) {
            pitch = 0;
        }
        if (Math.abs(roll) < VALUE_DRIFT) {
            roll = 0;
        }

        // Fill in the string placeholders and set the textview text.
        /*mTextSensorAzimuth.setText(getResources().getString(
                R.string.value_format, orientationValues[0]));
        mTextSensorPitch.setText(getResources().getString(
                R.string.value_format, orientationValues[1]));
        mTextSensorRoll.setText(getResources().getString(
                R.string.value_format, orientationValues[2]));*/
        /*TextView t=(TextView)findViewById(R.id.label_azimuth);
        t.setText(Float.toString(orientationX));
        TextView t1=(TextView)findViewById(R.id.label_pitch);
        t1.setText(Float.toString(orientationY));
        TextView t2=(TextView)findViewById(R.id.label_roll);
        t2.setText(Float.toString(orientationZ));*/
        // Reset all spot values to 0. Without this animation artifacts can
        // happen with fast tilts.
        //mSpotTop.setAlpha(0f);
        //mSpotBottom.setAlpha(0f);
        //mSpotLeft.setAlpha(0f);
        //mSpotRight.setAlpha(0f);

        // Set spot color (alpha/opacity) equal to pitch/roll.
        // this is not a precise grade (pitch/roll can be greater than 1)
        // but it's close enough for the animation effect.
        /*if (pitch > 0) {
            mSpotBottom.setImageResource(R.drawable.horizontal);
            mSpotTop.setImageResource(R.drawable.horizontal);
        } else {
            mSpotTop.setImageResource(R.drawable.vertical);
            mSpotBottom.setImageResource(R.drawable.vertical);
        }
        if (roll > 0) {
            mSpotLeft.setAlpha(roll);
        } else {
            mSpotRight.setAlpha(Math.abs(roll));
        }*/
    }

    /**
     * Must be implemented to satisfy the SensorEventListener interface;
     * unused in this app.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
