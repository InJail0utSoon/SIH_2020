package com.ait.mainactivity;


import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/********************  Database Manager  *****************/

public class DatabaseManager
{
    // selfRef for singleton design pattern implementation
    private static DatabaseManager ourInstance = new DatabaseManager();

    // HTTP client for communication
    private OkHttpClient okHttpClient;
    // server address
    private String serverURL;

    // public single instance getter method
    public static DatabaseManager getInstance()
    {
        if(ourInstance == null)
        {
            ourInstance = new DatabaseManager();

        }
        return ourInstance;
    }

    // private constructor
    private DatabaseManager()
    {
        okHttpClient = new OkHttpClient();
        serverURL = "http://codecell.club/check/reson.php";

    }

    /********************  Fxn to get Add Potholes  *****************/

    public synchronized void addDataEntry(String userId,String img,Double longitude,Double latitude,Callback callback)
    {

        RequestBody body =  new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("userId", userId)
                .addFormDataPart("longitude",String.valueOf(longitude))
                .addFormDataPart("latitude",String.valueOf(latitude))
                .addFormDataPart("img",img)
                .addFormDataPart("upload","")
                .build();


        Request request = new Request.Builder()
                .url(serverURL)
                .post(body)
                .build();

        okHttpClient.newCall(request).enqueue(callback);

    }

    /********************  Fxn to get Nearest Potholes  *****************/

    public synchronized void getNearbyData(Double longitude,Double latitude,Callback callback)
    {

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload", "")
                .build();

        Request request = new Request.Builder()
                .url(serverURL)
                .post(requestBody)
                .build();

        okHttpClient.newCall(request).enqueue(callback);

    }

}
