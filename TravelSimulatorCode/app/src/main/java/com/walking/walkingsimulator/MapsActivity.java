package com.walking.walkingsimulator;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener,  View.OnClickListener  {

    private GoogleMap googleMap;

    LatLng myLocation;
    LatLng myDestination;

    Location location;
    LocationManager locationManager;

    ImageButton btnRouteType, btnSettings, btnUndo;
    Button btnGo;

    boolean routeCycleMode = true;

    int numberOfPoints = 0;

   ArrayList<Polyline> polyLineList = new ArrayList<Polyline>();
    ArrayList<LatLng> latLngList = new ArrayList<LatLng>();

    private static final String TAG = MapsActivity.class.getName();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        StringBuilder builder = new StringBuilder();

        builder.append("\n Username: "
                + sharedPrefs.getString("prefUsername", "NULL"));


        ActionBar bar = getActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.blueActionDark)));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btnRouteType = (ImageButton) findViewById(R.id.btnRouteType);
        btnRouteType.setOnClickListener(this);

        btnUndo = (ImageButton) findViewById(R.id.btnUndo);
        btnUndo.setOnClickListener(this);
        btnUndo.setVisibility(View.INVISIBLE);

        btnGo = (Button) findViewById(R.id.btnGo);
        btnGo.setOnClickListener(this);
        btnGo.setClickable(false);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);

            // We now have permission, so load map now in onRequestPermissionsResult
        } else {
            location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

            // Load the map because we have permission already
            loadMap(location);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, Preferences.class);
                startActivity(i);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void loadMap(Location location) {

        if (location == null) {
            Log.e(TAG, "Error - Unable to get location");
        } else {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            this.googleMap.setMinZoomPreference(15);
            this.googleMap.setOnMapClickListener(this);

            myLocation = new LatLng(latitude, longitude);
            latLngList.add(myLocation);
            this.googleMap.addMarker(new MarkerOptions().position(myLocation).title("My Location"));
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                    loadMap(location);
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MapsActivity.this, "Permission denied to read your Location", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {

        myDestination = new LatLng(latLng.latitude, latLng.longitude);
        numberOfPoints++;
        googleMap.addMarker(new MarkerOptions().position(myDestination).title("Checkpoint #" + numberOfPoints));

        constructURL(myLocation, myDestination);
    }

    public void constructURL(LatLng aLocation, LatLng aDestination){
        String url = makeURL(aLocation.latitude, aLocation.longitude, aDestination.latitude, aDestination.longitude);

        myLocation = aDestination;
        latLngList.add(myLocation);

        connectAsyncTask c = new connectAsyncTask(url);
        c.execute();
    }

    public String makeURL (double sourcelat, double sourcelog, double destlat, double destlog ){
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString.append(Double.toString( sourcelog));
        urlString.append("&destination=");// to
        urlString.append(Double.toString( destlat));
        urlString.append(",");
        urlString.append(Double.toString( destlog));
        urlString.append("&sensor=false&mode=driving&alternatives=true");
        urlString.append("&key=AIzaSyCBuSXNUsLY46FU6gCuUKsDtmnn_bY37LE");
        return urlString.toString();
    }

    public void drawPath(String  result) {

        System.out.println("RESULT" + result);

        try {
            //Tranform the string into a json object
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);
            Polyline line = googleMap.addPolyline(new PolylineOptions()
                    .addAll(list)
                    .width(12)
                    .color(Color.parseColor("#05b1fb"))//Google maps blue color
                    .geodesic(true)
            );
            polyLineList.add(line);

            if (numberOfPoints > 0){
                btnUndo.setVisibility(View.VISIBLE);
                changeGoButton(true);
            }
        }
        catch (JSONException e) {
            Log.e(TAG, "Error - Unable to get draw path");
        }
    }

    public void changeGoButton(Boolean go){
        if (go){
            btnGo.setText("Complete & Go!");
            btnGo.setBackgroundColor(getResources().getColor(R.color.btnGoGreen));
            btnGo.setClickable(true);
        } else {
            btnGo.setText("Select Route!");
            btnGo.setBackgroundColor(getResources().getColor(R.color.btnGoRed));
            btnGo.setClickable(false);
        }
    }

    @Override
    public void onClick(View v) {
            switch(v.getId()) {
                case R.id.btnGo:
                    Toast.makeText(this, "Pressed GO button", Toast.LENGTH_SHORT).show();
                    constructURL(latLngList.get(latLngList.size()-1), latLngList.get(0));
                    break;
                case R.id.btnRouteType:
                    Drawable drawable = btnRouteType.getDrawable();
                    if (drawable.getConstantState().equals(getResources().getDrawable(R.drawable.ic_repeat_black_48dp).getConstantState())){
                        btnRouteType.setImageResource(R.drawable.ic_arrow_upward_black_48dp);
                        routeCycleMode = false;
                        Toast.makeText(this, "Set route to single path.", Toast.LENGTH_SHORT).show();
                    } else if (drawable.getConstantState().equals(getResources().getDrawable(R.drawable.ic_arrow_upward_black_48dp).getConstantState())){
                        btnRouteType.setImageResource(R.drawable.ic_repeat_black_48dp);
                        routeCycleMode = true;
                        Toast.makeText(this, "Set route to cycle path.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.btnUndo:
                    Toast.makeText(this, "Pressed Undo button", Toast.LENGTH_SHORT).show();
                    break;
            }
    }

    private class connectAsyncTask extends AsyncTask<Void, Void, String>{
        private ProgressDialog progressDialog;
        String url;
        connectAsyncTask(String urlPass){
            url = urlPass;
        }
        @Override
        protected void onPreExecute() {

            super.onPreExecute();
            progressDialog = new ProgressDialog(MapsActivity.this);
            progressDialog.setMessage("Fetching route, Please wait...");
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }
        @Override
        protected String doInBackground(Void... params) {
            JSONParser jParser = new JSONParser();
            String json = jParser.getJSONFromUrl(url);
            return json;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.hide();
            if(result!=null){
                drawPath(result);
            }
        }
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }



}