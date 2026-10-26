package com.example.locationservice;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_GPS = 100;
    private LocationManager locationManager;
    private Button btnLocate;
    private TextView txtStatus, txtCoordinates, txtDistance;
    private LocationListener locationListener;
    private static final double HOME_LATITUDE = 54.757140;
    private static final double HOME_LONGITUDE = 32.113145;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLocate = findViewById(R.id.btnLocate);
        txtStatus = findViewById(R.id.txtStatus);
        txtCoordinates = findViewById(R.id.txtCoordinates);
        txtDistance = findViewById(R.id.txtDistance);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location location) { showLocation(location); }
            @Override public void onProviderEnabled(String provider) {
                txtStatus.setText("Провайдер местоположения включён: " + provider);
            }
            @Override public void onProviderDisabled(String provider) {
                txtStatus.setText("Включите геолокацию на устройстве.");
            }
        };

        btnLocate.setOnClickListener(v -> OnGetGPS());
    }

    // Проверка разрешений на геопозицию.
    private boolean GetPermissionsGPS() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Запрос разрешения у пользователя.
    private void OnGetGPS() {
        if (!GetPermissionsGPS()) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_GPS);
            return;
        }
        requestCurrentLocation();
    }

    private void requestCurrentLocation() {
        if (!GetPermissionsGPS()) return;
        try {
            txtStatus.setText("Получение местоположения…");
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            String provider = gpsEnabled ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
            Location last = locationManager.getLastKnownLocation(provider);
            if (last != null) showLocation(last);
            locationManager.requestLocationUpdates(provider, 2000L, 1f, locationListener);
        } catch (SecurityException e) {
            txtStatus.setText("Нет разрешения на доступ к местоположению.");
        } catch (IllegalArgumentException e) {
            txtStatus.setText("Провайдер местоположения недоступен. Включите геолокацию.");
        }
    }

    private void showLocation(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        txtStatus.setText("Местоположение получено.");
        txtCoordinates.setText(String.format(Locale.getDefault(), "Широта: %.6f\nДолгота: %.6f", lat, lon));

        float[] result = new float[1];
        Location.distanceBetween(lat, lon, HOME_LATITUDE, HOME_LONGITUDE, result);
        double km = result[0] / 1000.0;
        double minutes = (km / 6.0) * 60.0;
        String time;
        if (minutes < 60) time = String.format(Locale.getDefault(), "%.0f мин.", minutes);
        else time = String.format(Locale.getDefault(), "%d ч %d мин.", (int) minutes / 60, Math.round(minutes) % 60);
        txtDistance.setText(String.format(Locale.getDefault(),
                "Расстояние до заданных координат дома: %.2f км\nВремя пешком при 6 км/ч: %s\nАдрес: %s",
                km, time, getAddress(lat, lon)));
    }

    private String getAddress(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String address = addresses.get(0).getAddressLine(0);
                return address == null ? "Адрес не найден" : address;
            }
        } catch (IOException | IllegalArgumentException e) {
            return "Не удалось получить адрес";
        }
        return "Адрес не найден";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_GPS) {
            boolean granted = false;
            for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) granted = true;
            if (granted) requestCurrentLocation();
            else Toast.makeText(this, "Для определения местоположения необходимо разрешение.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (GetPermissionsGPS()) requestCurrentLocation();
    }

    @Override protected void onPause() {
        super.onPause();
        if (locationManager != null && locationListener != null && GetPermissionsGPS()) {
            locationManager.removeUpdates(locationListener);
        }
    }
}
