package aqar.ya.myapplication.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import aqar.ya.myapplication.R;

public class SplashActivity extends BaseActivity {

    Dialog dialog ;
    private ActivityResultLauncher gpsActivityResult;
    private final   int requestPermission = 100 ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        gpsActivityResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isNetworkConnected())
            Toast.makeText(SplashActivity.this , getString(R.string.errorNetworkConnection),Toast.LENGTH_LONG).show();

        Init();
    }

    private void Init()
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (isNetworkConnected())
                {
                    if (!googleServicesIsEnabled())
                        showAlertDialog(1);
                    else
                        checkPermissionLocation();
                }
                else
                    Init();
            }
        },1000);
    }


    private void checkPermissionLocation()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestPermission);
        else
        {
            startActivity(new Intent(this,MapsActivity.class));
            finish();
        }
    }

    private void askPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION))
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestPermission);
            else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                showAlertDialog(2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case requestPermission: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // permission was granted,
                        startActivity(new Intent(this,MapsActivity.class));
                        finish();
                } else {
                    // permission denied, boo! Disable the functionality that depends on this permission.

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            askPermission();
                        }
                    },4000);
                }
            }
        }
    }

    // 1 location setting
    // 2 app setting
    private void showAlertDialog(int type)
    {
        try {
            if (dialog == null || !dialog.isShowing()) {
                dialog = new Dialog(this, R.style.full_screen_dialog);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.gpsalertdialog);
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                TextView txtMsg = dialog.findViewById(R.id.tv_common_popup_info);
                TextView enable_gps = dialog.findViewById(R.id.enable_gps);

                if (type == 2) {
                    txtMsg.setText(getString(R.string.enableLocationSetting));
                    enable_gps.setText(getString(R.string.go));
                }
                dialog.findViewById(R.id.enable_gps).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (type == 2)
                            openAppSetting();
                        else
                            gpsActivityResult.launch((new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));

                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        }catch (Exception e){
        }
    }

    private void openAppSetting() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
}