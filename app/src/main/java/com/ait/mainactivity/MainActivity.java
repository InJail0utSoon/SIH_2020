package com.ait.mainactivity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        switchActivity();

    }

    private void switchActivity()
    {

        System.out.println("@switchActivity");

        startActivity(new Intent(this,LogInSignUpActivity.class));
    }

}
