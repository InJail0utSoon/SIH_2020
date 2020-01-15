package com.ait.mainactivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelInterpreterOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class CamActivity extends AppCompatActivity
{
    //requestCodes
    private final int RC_CAMERA = 1;

    String pathToFile;
    Bitmap curBitmap;

    //layout var
    ImageView imageView;
    Button sendBtn;
    Button rejectBtn;

    //location var
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;


    //ML var
    FirebaseCustomRemoteModel remoteModel;
    FirebaseModelDownloadConditions conditions;
    FirebaseModelInterpreter interpreter;
    FirebaseModelInputOutputOptions inputOutputOptions;
    boolean isModelDownloaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam);

        imageView = findViewById(R.id.imageView);
        sendBtn = findViewById(R.id.sendButton);
        rejectBtn = findViewById(R.id.rejectButton);


        setUpLocation();

        bindButtonCalls();

        mlToolKitSetup();

        dispatchPictureTakenAction();

        runDemonstrationCode();
    }


    //---------------------------------------------------------------
    //                      INITIAL_SET_UP
    //---------------------------------------------------------------
    private void bindButtonCalls()
    {
        rejectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                dispatchPictureTakenAction();
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(interpreter != null && !pathToFile.isEmpty())
                {
                    checkImageValidity(curBitmap);
                }
                else
                    Toast.makeText(CamActivity.this,
                            "Please wait for a while",
                            Toast.LENGTH_SHORT)
                            .show();
            }
        });
    }

    //---------------------------------------------------------------
    //                      LOCATION
    //---------------------------------------------------------------
    private void setUpLocation()
    {
        locationRequest = new LocationRequest()
                .setInterval(TimeUnit.SECONDS.toMillis(10))
                .setFastestInterval(TimeUnit.SECONDS.toMillis(10))
                .setExpirationDuration(TimeUnit.SECONDS.toMillis(100000))
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback()
        {
            @Override
            public void onLocationResult(LocationResult locationResult)
            {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations())
                {
                    mLastKnownLocation = location;
                }
            }
        };

        startLocationUpdates();
    }

    private void startLocationUpdates()
    {

        mFusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }


    //---------------------------------------------------------------
    //                      UPLOAD_IMAGE
    //---------------------------------------------------------------
    private void uploadImage(Bitmap bitmap)
    {
        if(mLastKnownLocation == null)
        {
            onUnSuccessfulUpload(null);
            return;
        }

        Toast.makeText(this,"Uploading image...",Toast.LENGTH_SHORT).show();
        imageView.setImageDrawable(null);
        sendBtn.setVisibility(View.GONE);
        rejectBtn.setText("Re-Take");

        //send image to the server
        String encodedImageBase64 = convertBitmapToString(bitmap);
        String longitude = String.valueOf(mLastKnownLocation.getLongitude());
        String latitude = String.valueOf(mLastKnownLocation.getLatitude());
        String userId = "fetch-from-firebase";
        onSuccessfulUpload(null);
        //call to rana fn()
        DatabaseManager.getInstance().addDataEntry("dummy_user_id",encodedImageBase64,longitude,latitude,new Callback(){

            @Override
            public void onFailure(Call call, IOException ex)
            {
                ex.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {

                    System.out.println("Response :: "+response.body().toString());

            }

        });

        System.out.println("====================================================");
        System.out.println(encodedImageBase64);
        System.out.println("====================================================");
        System.out.println(FirebaseAuth.getInstance().getCurrentUser().getEmail());

    }

    private void onSuccessfulUpload(Response response)
    {
        try {
            String resp = response!=null ? response.body().string():"OK";
            System.out.println("Uploading Image Successful\n" + resp);
            Toast.makeText(this,"Upload Successfull",Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onUnSuccessfulUpload(Response response)
    {
        try {
            String resp = response!=null ? response.body().string():"FAILED";
            System.out.println("Uploading Image Failed\n" + resp);
            Toast.makeText(this,"Uploading Image Failed",Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Base64 Converter
    private static String convertBitmapToString(Bitmap bitmap)
    {
        String encodedImage = "";

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        try {
            encodedImage= URLEncoder.encode(Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return encodedImage;
    }

    //---------------------------------------------------------------
    //                      CAMERA CODE
    //---------------------------------------------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK)
        {
            switch (requestCode)
            {
                case RC_CAMERA:
                    curBitmap = BitmapFactory.decodeFile(pathToFile);
                    imageView.setImageBitmap(curBitmap);
                    sendBtn.setVisibility(View.VISIBLE);
                    break;

                case RC_DEMONSTRATION:
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(data.getData());
                        curBitmap = BitmapFactory.decodeStream(inputStream);
                        imageView.setImageBitmap(curBitmap);
                     } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sendBtn.setVisibility(View.VISIBLE);

                    break;
            }
        }
        else
            Toast.makeText(this,"Activity Corrupted",Toast.LENGTH_LONG).show();
    }

    private void dispatchPictureTakenAction()
    {
        Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePic.resolveActivity(getPackageManager()) != null)
        {
            File photoFile ;
            try
            {
                photoFile = createPhotoFile();
                pathToFile = photoFile.getAbsolutePath();
                Uri photoUri = FileProvider.getUriForFile(CamActivity.this,"com.ait.mainactivity.fileProvider", photoFile);
                takePic.putExtra(MediaStore.EXTRA_OUTPUT,photoUri);
                startActivityForResult(takePic,RC_CAMERA);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            System.out.println("oh my godd:---:");

        }
    }

    private File createPhotoFile() throws Exception
    {
        String name = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z", Locale.US).format(new java.util.Date());
        File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(name,".jpg",storageDir);
    }


    //*****************************************************************
    //                      [ ML MODEL_CODE ]
    //*****************************************************************

    private void mlToolKitSetup()
    {
        remoteModel = new FirebaseCustomRemoteModel.Builder("pothole-detector").build();

        conditions = new FirebaseModelDownloadConditions.Builder()
                .build();

        FirebaseModelManager.getInstance().download(remoteModel, conditions)
                .addOnCompleteListener(new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {

                        Toast.makeText(CamActivity.this,
                                "Model Downloaded Successfully",
                                Toast.LENGTH_SHORT)
                                .show();

                        FirebaseModelInterpreterOptions options;
                        options = new FirebaseModelInterpreterOptions.Builder(remoteModel).build();
                        try {
                            interpreter = FirebaseModelInterpreter.getInstance(options);
                        }catch (Exception e){

                        }
                    }
                });


        FirebaseModelManager.getInstance().isModelDownloaded(remoteModel)
                .addOnSuccessListener(new OnSuccessListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean isDownloaded) {
                        FirebaseModelInterpreterOptions options;
                        try {
                            if (isDownloaded) {
                                options = new FirebaseModelInterpreterOptions.Builder(remoteModel).build();
                                interpreter = FirebaseModelInterpreter.getInstance(options);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

        try {
            inputOutputOptions = new FirebaseModelInputOutputOptions.Builder()
                    .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 224, 224, 3})
                    .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 2})
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,"I/O option setup Failed",Toast.LENGTH_LONG).show();
        }
    }

    private void checkImageValidity(Bitmap bitmap)
    {
        //check whether the image is Pothole or not
        try {
//            final Bitmap bitmap = BitmapFactory.decodeFile(pathToFile);

            //for debugging puposes
            //bitmap = BitmapFactory.decodeResource( getResources(),R.drawable.pothole);

            final Bitmap FIN_BIT_MAP = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

            int batchNum = 0;
            float[][][][] input = new float[1][224][224][3];
            for (int x = 0; x < 224; x++) {
                for (int y = 0; y < 224; y++) {
                    int pixel = bitmap.getPixel(x, y);
                    // Normalize channel values to [-1.0, 1.0]. This requirement varies by
                    // model. For example, some models might require values to be normalized
                    // to the range [0.0, 1.0] instead.
                    input[batchNum][x][y][0] = (Color.red(pixel) - 127) / 128.0f;
                    input[batchNum][x][y][1] = (Color.green(pixel) - 127) / 128.0f;
                    input[batchNum][x][y][2] = (Color.blue(pixel) - 127) / 128.0f;
                }
            }


            FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                    .add(input)  // add() as many input arrays as your model requires
                    .build();

            interpreter.run(inputs, inputOutputOptions)
                    .addOnSuccessListener(
                            new OnSuccessListener<FirebaseModelOutputs>() {
                                @Override
                                public void onSuccess(FirebaseModelOutputs result)
                                {
                                    float[][] output = result.getOutput(0);
                                    float[] probabilities = output[0];

                                    if(probabilities[1] > probabilities[0] )
                                    {
                                        Toast.makeText(CamActivity.this,
                                                "IMAGE ACCEPTED",
                                                Toast.LENGTH_SHORT)
                                                .show();

                                        uploadImage(FIN_BIT_MAP);
                                    }
                                    else
                                    {
                                        Toast.makeText(CamActivity.this,
                                                "Not a valid Pothole image",
                                                Toast.LENGTH_LONG)
                                                .show();
                                        onInvalidImageAction();
                                    }
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e)
                                {
                                    System.out.println("************************************************");
                                    e.printStackTrace();
                                    System.out.println("************************************************");
                                    Toast.makeText(CamActivity.this,
                                            e.toString(),
                                            Toast.LENGTH_SHORT)
                                            .show();
                                }
                            });


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void onInvalidImageAction()
    {
        sendBtn.setVisibility(View.GONE);
        rejectBtn.setText("RE_TAKE");
    }

    //*****************************************************************
    //                      [ DEMONSTRATION CODE ]
    //*****************************************************************

    Button buttonFilePicker;
    private final static int RC_DEMONSTRATION = 20;

    private void runDemonstrationCode()
    {
        buttonFilePicker = findViewById(R.id.fileChooserButton);

        buttonFilePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile();
            }
        });
    }

    private void chooseFile()
    {
        Intent fileChooserIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileChooserIntent.setType("image/*");
        startActivityForResult(fileChooserIntent,RC_DEMONSTRATION);
    }


}


  /*private void doMultiPartRequest(String pathToFile)
    {
        String url = "http://codecell.club/check/check.php";
        File imgFile = new File(pathToFile);
        Uri uri = null;
        OkHttpClient client = new OkHttpClient();

        String longitude = "";
        String latitude = "";

        if(mLastKnownLocation != null)
        {
            longitude = String.valueOf(mLastKnownLocation.getLongitude());
            latitude = String.valueOf(mLastKnownLocation.getLatitude());
        }

        RequestBody body =  new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload","")
                .addFormDataPart("longe",longitude)
                .addFormDataPart("late",latitude)
                .addFormDataPart("image", imgFile.getName(),
                        RequestBody.create(imgFile,MediaType.parse("image/jpeg")))
                .build();


        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call,@NotNull IOException e)
            {
                e.printStackTrace();
                System.out.println("unScus");
            }

            @Override
            public void onResponse(@NotNull Call call,@NotNull Response response)
            {
                if(response.isSuccessful())
                {
                    final Response finResponse = response;
                    CamActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            OnSuccessfulUpload(finResponse);
                        }
                    });
                }
            }
        });

    }
*/
