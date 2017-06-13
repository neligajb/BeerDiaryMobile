package com.n.boone.beerdiary;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ViewReviewsActivity extends AppCompatActivity {

    GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 1442;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_reviews);

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
            String idToken = acct.getIdToken();
            Log.d("TOKEN", idToken);

            // do the GET
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            OkHttpClient mOkHttpClient = new OkHttpClient();
            HttpUrl reqUrl = HttpUrl.parse("http://beer-diary-169400.appspot.com/reviews");
            Request request = new Request.Builder()
                    .url(reqUrl)
                    .addHeader("Authorization", idToken)
                    .build();
            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String r = response.body().string();
                    try {
                        JSONArray items = new JSONArray(r);
                        final List<Map<String, String>> reviews = new ArrayList<Map<String, String>>();
                        for(int i = 0; i < items.length(); i++){
                            HashMap<String, String> m = new HashMap<String, String>();
                            m.put("beerName", items.getJSONObject(i).getString("beerName"));
                            m.put("style", items.getJSONObject(i).getString("style"));
                            m.put("brewery", items.getJSONObject(i).getString("brewery"));
                            m.put("rating", items.getJSONObject(i).getString("rating"));
                            m.put("id", items.getJSONObject(i).getString("id"));
                            reviews.add(m);
                        }
                        final SimpleAdapter reviewAdapter = new SimpleAdapter(
                                ViewReviewsActivity.this,
                                reviews,
                                R.layout.review_item,
                                new String[]{"beerName", "style", "brewery", "rating", "id"},
                                new int[]{R.id.review_item_beer, R.id.review_item_style,
                                        R.id.review_item_brewery, R.id.review_item_rating,
                                        R.id.review_id}) {

                            @Override
                            public View getView (final int position, View convertView, ViewGroup parent) {
                                View v = super.getView(position, convertView, parent);
                                TextView id_textView = (TextView) v.findViewById(R.id.review_id);
                                String id = id_textView.getText().toString();

                                // set listener for update button
                                Button update_button = (Button) v.findViewById(R.id.update_review_button);
                                update_button.setTag(id);
                                update_button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        updateReview(v);
                                    }
                                });

                                // set listener for delete button
                                Button delete_button = (Button) v.findViewById(R.id.delete_review_button);
                                delete_button.setTag(id);
                                delete_button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        reviews.remove(position);
                                        deleteReview(v);
                                    }
                                });

                                return v;
                            }
                        };

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((ListView)findViewById(R.id.reviews_list)).setAdapter(reviewAdapter);
                            }
                        });
                    }
                    catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                }
            });
        } else {
            Log.d("TOKEN", "bad token");
        }
    }

    private void updateReview(View v) {
        String review_id = v.getTag().toString();

        // do the PUT
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        OkHttpClient mOkHttpClient = new OkHttpClient();
        HttpUrl reqUrl = HttpUrl.parse("http://beer-diary-169400.appspot.com/reviews/" + review_id);

        // collect review data
        LinearLayout reviewView = (LinearLayout) ((LinearLayout) v.getParent().getParent());

        EditText beerField = (EditText) reviewView.findViewById(R.id.review_item_beer);
        String beer_content = beerField.getText().toString();
        EditText styleField = (EditText) reviewView.findViewById(R.id.review_item_style);
        String style_content = styleField.getText().toString();
        EditText breweryField = (EditText) reviewView.findViewById(R.id.review_item_brewery);
        String brewery_content = breweryField.getText().toString();
        EditText ratingField = (EditText) reviewView.findViewById(R.id.review_item_rating);
        String rating_content = ratingField.getText().toString();

        RequestBody body = RequestBody.create(JSON,
                "{\"beer\": \"" +
                        beer_content + "\", \"style\": \"" + style_content +
                        "\", \"brewery\": \"" + brewery_content +
                        "\", \"rating\": \"" + rating_content + "\"" + "}");
        Request request = new Request.Builder()
                .url(reqUrl)
                .put(body)
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

    private void deleteReview(View v) {
        String review_id = v.getTag().toString();

        // remove item from ListView Adapter
        ListView review_list = (ListView) findViewById(R.id.reviews_list);
        SimpleAdapter review_list_adapter = (SimpleAdapter) review_list.getAdapter();
        review_list_adapter.notifyDataSetChanged();

        // prepare the DELETE
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        OkHttpClient mOkHttpClient = new OkHttpClient();
        HttpUrl reqUrl = HttpUrl.parse("http://beer-diary-169400.appspot.com/reviews/" + review_id);

        // get review_item View
        LinearLayout reviewView = (LinearLayout) ((LinearLayout) v.getParent().getParent());

        Request request = new Request.Builder()
                .url(reqUrl)
                .delete()
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
}
