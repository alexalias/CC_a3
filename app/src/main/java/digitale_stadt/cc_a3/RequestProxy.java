package digitale_stadt.cc_a3;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 1engelke on 25.05.2016.
 *
 */
public class RequestProxy {

    private Context mContext;

    private SharedPreferences sharedPrefs;

    private RequestQueue mRequestQueue;

    //final String login_url = "https://api.cyc.jmakro.de:4040/get_auth_token.php";
    //final String register_url = "https://api.cyc.jmakro.de:4040/register_user.php";
    //final String data_url = "https://api.cyc.jmakro.de:4040/log_coordinates.php";
    final String login_url = "https://cychh.informatik.uni-hamburg.de:443/get_auth_token.php";
    final String register_url = "https://cychh.informatik.uni-hamburg.de:443/register_user.php";
    final String data_url = "https://cychh.informatik.uni-hamburg.de:443/log_coordinates.php";

    final String server_login_failed = "wrong credentials";
    final String server_register_success = "succesfully";
    final String server_register_anonymous_failed = "wrong credentials";
    final String server_data_transmission_token_not_valid = "auth_token not set";

    RetryPolicy defaultRetryPolicy = new DefaultRetryPolicy(
        DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,   // 1000
        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,  // 1
        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT  //1f
    );

    // package access constructor
    RequestProxy(Context context) {
        mContext = context;
        mRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void Login(final boolean retry, final String username, final String password) { Login(retry, username, password, login_url); }
    public void Login(final boolean retry, final String username, final String password, final String login_url)
    {
        Log.i("RequestProxy", "Connecting with " + username + " / " + password);
        StringRequest postRequest = new StringRequest(Request.Method.POST, login_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // check if response string is server error message
                        if (!response.equals(server_login_failed))
                        {
                            Log.i("Login Response", "Token received: " + response);
                            try {
                                // retrieve auth_token from server response
                                JSONObject myJson = new JSONObject(response);
                                String token = myJson.optString("auth_token");
                                sharedPrefs.edit().putString("auth_token", token).commit();

                                // Nicht versendete Touren aus der Datenbank versenden
                                Tour tour;
                                String tourID;
                                int counter;
                                ArrayList<Position> list = DBManager.getInstance().doRequest().selectAllPositionsNotSent();

                                //creates a tour for each tourID in the list and sends it to the server
                                while (!list.isEmpty())
                                {
                                    //initialize tour with the tourID of the first Position in the list
                                    tourID = list.get(0).getTourID();
                                    tour = new Tour(tourID, new ArrayList<Position>());

                                    // move all elements with same tourID from the list to the tour
                                    counter = 0;
                                    while (counter < list.size()) {
                                        // moves an element to the tour or increases the counter
                                        if (list.get(counter).getTourID().equals(tourID))
                                            tour.AddWayPoint(list.remove(0));
                                        else
                                            counter++;
                                    }
                                    ((MainActivity)mContext).LogSystemData("vor  test: ");
                                    Log.i("Tour from DB to send", tour.toJSON().toString());
                                    DBManager.getInstance().doRequest().updatePositionSentStatus(tourID, tour.GetWayPoints().get(0).getId(), 1);
                                    ((MainActivity)mContext).LogSystemData("nach test: ");
                                    //RequestManager.getInstance().doRequest().SendTourData(token, tour);
                                }
                            }
                            catch (Exception e) {}
                        }
                        else
                        {
                            Log.i("Login Response", "Login rejected: " + response);
                            // delete token and open login screen
                            sharedPrefs.edit().putString("auth_token", "").commit();
                            if (retry == true)
                                ((MainActivity)mContext).displayLoginActivity();
                        }

                        // UI update
                        ((MainActivity)mContext).UpdateUsername();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("Login Error: ", error.toString());
                        // delete token and open login screen
                        sharedPrefs.edit().putString("auth_token", "").commit();
                        if (retry == true)
                            ((MainActivity)mContext).displayLoginActivity();

                        // UI update
                        ((MainActivity)mContext).UpdateUsername();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                return params;
            }
        };

        //set timeout to 1000ms and retries to 1
        postRequest.setRetryPolicy(new DefaultRetryPolicy(2000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        mRequestQueue.add(postRequest);
    }

    public void Register(final boolean retry, final String username, final String password, final String email) { Register(retry, username, password, email, register_url);  }
    public void Register(final boolean retry, final String username, final String password, final String email, final String login_url)
    {
        Log.i("RequestProxy", "Register with " + username + " / " + password);
        StringRequest postRequest = new StringRequest(Request.Method.POST, login_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // check if register was successfull
                        if (response.contains(server_register_success)) {
                            Log.i("Register Response", "Registration successfull: " + response);
                            // save data
                            sharedPrefs.edit().putString("username", username).commit();
                            sharedPrefs.edit().putString("userpassword", password).commit();
                            // login with new username
                            RequestManager.getInstance().doRequest().Login(false, username, password);
                        }
                        else
                        {
                            Log.i("Register Response", "Registration failed: " + response);
                            sharedPrefs.edit().putString("auth_token", "").commit();
                            // open register activity
                            if (retry == true)
                                ((MainActivity)mContext).displayRegisterActivity();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("Register Error: ", error.toString());
                        sharedPrefs.edit().putString("auth_token", "").commit();
                        // open register activity
                        if (retry == true)
                            ((MainActivity)mContext).displayRegisterActivity();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                params.put("email", email);
                return params;
            }
        };

        //set timeout to 1000ms and retries to 1
        postRequest.setRetryPolicy(new DefaultRetryPolicy(2000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mRequestQueue.add(postRequest);
    }

    public void Register_Anonymous(final boolean retry) { Register_Anonymous(retry, register_url); }
    public void Register_Anonymous(final boolean retry, final String register_url)
    {
        Log.i("RequestProxy", "Register Anonymous");
        StringRequest postRequest = new StringRequest(Request.Method.POST, register_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (!response.equals(server_register_anonymous_failed))
                        {
                            Log.i("Reg.Anon Response", "Token received: " + response);
                            try {
                                // retrieve username and auth_token from server response
                                JSONObject myJson = new JSONObject(response);

                                String username = myJson.optString("username");
                                sharedPrefs.edit().putString("username", username).commit();

                                String password = myJson.optString("password");
                                sharedPrefs.edit().putString("userpassword", password).commit();

                                sharedPrefs.edit().putBoolean("anonymous", true).commit();

                                // login with new username
                                RequestManager.getInstance().doRequest().Login(retry, username, password);
                            }
                            catch (Exception e) {}
                        }
                        else
                        {
                            Log.i("Login Response", "Login rejected: " + response);
                            // delete token and open login screen
                            sharedPrefs.edit().putString("auth_token", "").commit();
                            if (retry == true)
                                ((MainActivity)mContext).displayLoginActivity();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("Reg Anon Error.Resp", error.toString());
                        sharedPrefs.edit().putString("auth_token", "").commit();
                        if (retry == true)
                            RequestManager.getInstance().doRequest().Register_Anonymous(retry);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("anonym", "1");
                return params;
            }
        };

        //set timeout to 1000ms and retries to 1
        postRequest.setRetryPolicy(new DefaultRetryPolicy(2000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mRequestQueue.add(postRequest);
    }



    public void SendTourData(String auth_token, Tour tour) { SendTourData(auth_token, tour, data_url); }
    public void SendTourData(final String auth_token, final Tour tour, final String url)
    {
        Log.i("RequestProxy", "Send Tour Data: " + tour.toJSON().toString());
        final StringRequest postRequest = new StringRequest(Request.Method.POST, data_url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // check if transmission was successfull
                if (!response.equals(server_data_transmission_token_not_valid))
                {
                    Log.i("SendTourData", "Data transmitted: " + response);
                    try {
                        ((MainActivity)mContext).LogSystemData("vor  req: ");
                        for (Position pos : tour.GetWayPoints()) {
                            DBManager.getInstance().doRequest().updatePositionSentStatus(pos.getTourID(), pos.getId(), 1);
                                }
                        ((MainActivity)mContext).LogSystemData("nach req: ");
                     }
                    catch (Exception e) {}
                }
                else
                {
                    Log.i("SendTourData", "Login error: resetting token " + response);
                    sharedPrefs.edit().putString("auth_token", "").commit();
                }
            }
        },
        new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("SendData Error.Response", error.toString());
            }
        }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("auth_token", auth_token);
                params.put("data", tour.toJSON().toString());
                return params;
            }
        };

        //set timeout to 1000ms and retries to 1
        postRequest.setRetryPolicy(defaultRetryPolicy);
        //mRequestQueue.add(postRequest);
    }
}