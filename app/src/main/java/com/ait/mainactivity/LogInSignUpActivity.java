package com.ait.mainactivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LogInSignUpActivity extends AppCompatActivity
{

    enum AuthenticationState {
        LOGIN,SIGNUP;
    }

    //goggle sign-in fields
    private final static int RC_SIGN_IN = 2;
    private SignInButton googleSignInButton;
    private GoogleSignInClient mGoogleSignInClient;

    //Firebase fields
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    //common sign-in fields
    Button buttonLoginSignup;
    EditText editTextUsername;
    EditText editTextPassword;
    TextView authenticatorChanger;
    ProgressDialog progressDialog;

    AuthenticationState authenticationState = AuthenticationState.LOGIN;

    @Override
    protected void onStart()
    {

        System.out.println("@LoginSignUpActivity.onCreate");
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in_sign_up);

        System.out.println("@LoginSignUpActivity.onCreate");

        mAuth = FirebaseAuth.getInstance();

        editTextUsername = findViewById(R.id.username);
        editTextPassword = findViewById(R.id.password);
        buttonLoginSignup = findViewById(R.id.buttonLoginSignup);
        authenticatorChanger = findViewById(R.id.authenticatorChanger);
        progressDialog = new ProgressDialog(this);

        buttonLoginSignup.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                registerUser();
            }
        });

        authenticatorChanger.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                changeAuthenticationState();
            }
        });

        mAuthListener = new FirebaseAuth.AuthStateListener()
        {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
            {
                if(firebaseAuth.getCurrentUser() != null)
                {
                    startActivity(new Intent(LogInSignUpActivity.this,MenuActivity.class));
                }
            }
        };

        setUpGoogleSignin();
    }



    //*******************************************************************************
    //                          [ GOOGLE SIGN-IN]
    //*******************************************************************************
    private void setUpGoogleSignin()
    {
        System.out.println("@LoginSignUpActivity.setUpGoogleSignIn");

        googleSignInButton = findViewById(R.id.goggle_signin);

        googleSignInButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                signIn();
            }
        });


        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }


    private void signIn()
    {
        System.out.println("@LoginSignUpActivity.signIn");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN)
        {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try
            {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            }
            catch (ApiException e)
            {
                // Google Sign In failed, update UI appropriately
                System.out.println("@LoginSignUpActivity.onActivityResult :" + e.getMessage());
                e.printStackTrace();
                Toast.makeText(LogInSignUpActivity.this,"Google sign in failed",Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account)
    {
        System.out.println("Going to login in firebase");
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        if (task.isSuccessful())
                        {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("TAG", "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
//                            updateUI(user);
                        }
                        else
                        {
                            // If sign in fails, display a message to the user.
                            Log.w("TAG", "signInWithCredential:failure", task.getException());
//                            updateUI(null);
                        }

                        // ...
                    }
                });
    }

    //*******************************************************************************
    //                          [ NORMAL SIGN-IN]
    //*******************************************************************************
    private void changeAuthenticationState()
    {
        if(authenticationState == AuthenticationState.LOGIN)
        {
            buttonLoginSignup.setText("Sign-up Now");
            authenticatorChanger.setText(R.string.login);
            authenticationState = AuthenticationState.SIGNUP;
        }
        else
        {
            buttonLoginSignup.setText("Login");
            authenticatorChanger.setText(R.string.signup);
            authenticationState = AuthenticationState.LOGIN;
        }
    }


    private void registerUser()
    {
        final String username = editTextUsername.getText().toString();
        String password = editTextPassword.getText().toString();

        if(username.isEmpty())
        {
            return;
        }
        if(password.isEmpty())
        {
            return;
        }


        progressDialog.setMessage("Registering User");
        progressDialog.show();

        if(authenticationState == AuthenticationState.SIGNUP)
        {
            mAuth.createUserWithEmailAndPassword(username,password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful())
                                onSuccessfullRegistration(username,"Registered Successfully");
                            else
                                onRegistrationFailed(username,"Registration Failed");
                            progressDialog.dismiss();
                        }
                    });
        }
        else
        {
            mAuth.signInWithEmailAndPassword(username,password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful())
                            {
                                progressDialog.dismiss();
                                onSuccessfullRegistration(username,"Login Successfully");
                                switchActivity(MenuActivity.class);
                            }
                            else
                            {
                                onRegistrationFailed(username,"Login Failed");
                                progressDialog.dismiss();
                            }
                        }
                    });
        }
    }

    private void switchActivity(Class<?> classToBeLoaded)
    {
        startActivity(new Intent(this,classToBeLoaded));
    }

    private void onRegistrationFailed(String email, String msg)
    {
        System.out.println(email + " : " + msg);
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
    }

    private void onSuccessfullRegistration(String email, String msg)
    {
        System.out.println(email + " : " + msg);
        Toast.makeText(this," " + msg,Toast.LENGTH_LONG).show();
    }

}
