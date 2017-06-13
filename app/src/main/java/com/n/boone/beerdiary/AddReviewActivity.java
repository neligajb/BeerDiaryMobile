package com.n.boone.beerdiary;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddReviewActivity extends AppCompatActivity {

    GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 1442;
    String idToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_review);

        // Configure sign-in to request the user's idToken.
        // ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("805072041815-5ligrpf92hcoegjkq77kigglsflhemr3.apps.googleusercontent.com")
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.d("CONNECTION", connectionResult.toString());
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        signIn();

        (findViewById(R.id.submit_review_button)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // prepare the POST
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                OkHttpClient mOkHttpClient = new OkHttpClient();
                HttpUrl reqUrl = HttpUrl.parse("http://beer-diary-169400.appspot.com/reviews");

                // collect review data
                EditText beerField = (EditText) findViewById(R.id.beer_content);
                String beer_content = beerField.getText().toString();
                EditText styleField = (EditText) findViewById(R.id.style_content);
                String style_content = styleField.getText().toString();
                EditText breweryField = (EditText) findViewById(R.id.brewery_content);
                String brewery_content = breweryField.getText().toString();
                EditText ratingField = (EditText) findViewById(R.id.rating_content);
                String rating_content = ratingField.getText().toString();

                RequestBody body = RequestBody.create(JSON,
                        "{\"idToken\": \"" + idToken + "\", \"beer\": \"" +
                                beer_content + "\", \"style\": \"" + style_content +
                                "\", \"brewery\": \"" + brewery_content +
                                "\", \"rating\": \"" + rating_content + "\"" + "}");
                Request request = new Request.Builder()
                        .url(reqUrl)
                        .post(body)
                        .build();
                mOkHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String r = response.body().string();
                        Log.d("RESPONSE", r);
                        try {
                            JSONObject j = new JSONObject(r);

                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d("SIGNIN", "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, send token to server
            GoogleSignInAccount acct = result.getSignInAccount();
            idToken = acct.getIdToken();
            Log.d("TOKEN", idToken);

        } else {
            // TODO: deal with bad google sign in
            Log.d("STATUS", "nope");
        }
    }

}
