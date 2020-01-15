package com.ait.mainactivity;

/* **************************************** HEADERS ***************************************** */

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

/* ***************************************** MenuActivity ******************************************* */

public class MenuActivity extends AppCompatActivity
{

    //main navigation buttons
    private Button camButton;
    private Button mapButton;
    private Button transactionsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        System.out.println("@MenuActivity.onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        camButton = findViewById(R.id.camera);
        mapButton = findViewById(R.id.map);
        transactionsButton = findViewById(R.id.transaction);

        requestPermissions();
        setOnClickListners();

    }

    // requesting permissions
    private void requestPermissions()
    {

        if(Build.VERSION.SDK_INT >= 23)
            ActivityCompat.requestPermissions(this,new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    3);
    }

    // setting up on click listeners
    private void setOnClickListners()
    {

        camButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                Animation animation=AnimationUtils.loadAnimation(MenuActivity.this,R.anim.fadeout);
                camButton.startAnimation(animation);
                startIntent(CamActivity.class);
            }
        });

        mapButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                Animation animation=AnimationUtils.loadAnimation(MenuActivity.this,R.anim.fadeout);
                mapButton.startAnimation(animation);
                startIntent(MapsActivity.class);

            }
        });

        transactionsButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                Animation animation=AnimationUtils.loadAnimation(MenuActivity.this,R.anim.fadeout);
                transactionsButton.startAnimation(animation);
            }



        });

    }

    // activity switching logic
    private void startIntent(Class<?> cls)
    {
        Intent intent = new Intent(this,cls);
        startActivity(intent);

    }

}
