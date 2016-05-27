package digitale_stadt.cc_a3;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

/**
 * Created by alexutza_a on 29.04.2016.
 * Implements a Sender
 */
public class Sender {
    private RequestQueue queue;
    final String login_url = "https://api.cyc.jmakro.de:4040/get_auth_token.php";
    final String data_url = "https://api.cyc.jmakro.de:4040/log_coordinates.php";
    String _token;
    Context _context;

    public Sender(Context context)
    {
//        RequestManager.getInstance(context);
//        RequestManager.getInstance().doRequest().login();
        queue = Volley.newRequestQueue(context);
        _context = context;
        _token = "";
        Connect(GetUsername(), GetPassword());
    }

    public void Connect(final String username, final String password)
    {
        Log.i("Connect", "Connecting with " + username + " / " + password);
        StringRequest postRequest = new StringRequest(Request.Method.POST, login_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response

                        if (response == "wrong credentials")
                        {
                            Log.i("Response", "Login rejected");
                            _token = "";
                        }
                        else {
                            Log.i("Response", "Token received");
                            _token = response;
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.i("Error.Response", error.toString());
                        _token = "";
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

        //set timeout to 2000ms and retries to 1
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, // 2000
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, // 1
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)); //1f

        queue.add(postRequest);
    }

    public void Disconnect()
    {

    }

    public void SendTourData(Tour tour)
    {
        SendJSONString(data_url, tour.toJSON().toString());
    }

    public void SendTourData(String url, Tour tour)
    {
        SendJSONString(url, tour.toJSON().toString());
    }

    public void SendJSONString (String url, String s)
    {
        if (_token == "")
        {
            Connect(GetUsername(), GetPassword());
        }
        else {
            HashMap<String, String> map = new HashMap<>();
            map.put("data", s);
            map.put("token", _token);

            Log.i("Sender", "Sending " + s);
            Log.i("Sender", "Token " + _token);
            SendPostRequest(url, map);
        }
    }

    private void SendPostRequest(String url, final Map<String, String> params)
    {
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.i("Response", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.i("Error.Response", error.toString());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        };

        //set timeout to 2000ms and retries to 1
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, // 2000
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES, // 1
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)); //1f

        queue.add(postRequest);
    }

    // fragt den usernamen aus den shared preferences ab
    public String GetUsername(){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(_context);
        return sharedPrefs.getString("username", "");
    }

    // fragt das passwort aus den shared preferences ab
    public String GetPassword(){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(_context);
        return sharedPrefs.getString("userpassword", "");
    }
}
