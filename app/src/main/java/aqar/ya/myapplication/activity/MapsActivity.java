package aqar.ya.myapplication.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.text.TextUtilsCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Locale;

import aqar.ya.myapplication.models.DriverModel;
import aqar.ya.myapplication.models.GpsTracker;
import aqar.ya.myapplication.ISource;
import aqar.ya.myapplication.R;
import aqar.ya.myapplication.SourceAdapter;
import aqar.ya.myapplication.models.SourceModel;
import aqar.ya.myapplication.databinding.ActivityMapsBinding;

public class MapsActivity extends BaseActivity implements OnMapReadyCallback, ISource, TextWatcher {

    private GoogleMap mMap;
    ActivityMapsBinding binding;
    SourceAdapter sourceAdapter;
    ArrayList<SourceModel> sourceModels = new ArrayList<>();
    private SourceModel sourceLocation = null;
    private SourceModel destinationLocation = null;
    private boolean isResultForSource = false;
    private boolean isResultForDestination = false;
    private Polyline closestDriverLine = null;
    ISource iSource;
    MenuItem previous_item;
    GpsTracker gpsTracker;
    double myAdressLat, myAdressLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_maps);

        setupDrawer();
        initViews();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //get sources document ( adding sources if not found in database)
        checkSources(db);

        //get drivers ( adding drivers if not found in database)
        checkDrivers(db);
    }

    private void checkSources(FirebaseFirestore db)
    {
        db.collection("Source")
                .get()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            addDummySources();
                        }
                    }
                });
    }

    private void checkDrivers(FirebaseFirestore db)
    {
        db.collection("Drivers").get().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                if (task.getResult().isEmpty()) {
                    addDummyDrivers();
                } else
                {
                    for (int i=0 ; i<task.getResult().size();i++)
                    {
                        double lat = (double) task.getResult().getDocuments().get(i).get("latitude");
                        double lng = (double) task.getResult().getDocuments().get(i).get("longitude");
                        String name = (String) task.getResult().getDocuments().get(i).get("name");
                        mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).title(getString(R.string.driver) + name));
                    }
                }
            }
        });
    }

    private void initViews()
    {
        iSource = this;
        binding.rvSource.setHasFixedSize(true);
        binding.rvSource.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        sourceAdapter = new SourceAdapter(MapsActivity.this, sourceModels, iSource);
        binding.rvSource.setAdapter(sourceAdapter);
        binding.rvSource.setVisibility(View.GONE);
        binding.edtSource.addTextChangedListener(this);
        binding.edtDestination.addTextChangedListener(this);

        binding.edtSource.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.rvSource.setVisibility(View.VISIBLE);
                isResultForSource = true;
                isResultForDestination = false;
                sourceAdapter.getFilter().filter(((EditText) v).getText().toString());
            }
        });

        binding.edtDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.rvSource.setVisibility(View.VISIBLE);
                isResultForSource = false;
                isResultForDestination = true;
                sourceAdapter.getFilter().filter(((EditText) v).getText().toString());
            }
        });

        binding.btnRequested.setOnClickListener(v -> {
            if (sourceLocation == null) {
                Toast.makeText(MapsActivity.this, getString(R.string.select_source_first), Toast.LENGTH_SHORT).show();
            } else {
                getDrivers();
            }
        });

        //fill recyclerview
        getSources();
    }

    private void setupDrawer() {
        binding.drawerLayout.setScrimColor(Color.TRANSPARENT);
        binding.drawerLayout.setDrawerElevation(0f);
        binding.img.setOnClickListener(v -> {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        binding.drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            float scaleFactor = 8f;
            boolean isLeftToRight = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_LTR;
            float customElevation = getResources().getDimension(R.dimen._4sdp);
            float customCornerRadius = getResources().getDimension(R.dimen._16sdp);

            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);

                float slideX = drawerView.getWidth() * slideOffset * 0.9f;
                binding.content.setCornerRadius(slideOffset * customCornerRadius);
                binding.content.setElevation(slideOffset * customElevation);
                binding.content.setTranslationX(isLeftToRight ? slideX : -slideX);
                binding.content.setScaleX(1 - (slideOffset / scaleFactor));
                binding.content.setScaleY(1 - (slideOffset / scaleFactor));
            }
        });

        binding.drawerLayout.addDrawerListener(new ActionBarDrawerToggle(this, binding.drawerLayout,
                null, 0, 1) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                binding.imgMenu.setImageResource(R.drawable.navigation_home);
            }
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                binding.imgMenu.setImageResource(R.drawable.back_ic);
            }
        });

        binding.navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                item.setChecked(true);
                if (previous_item!=null)
                   previous_item.setChecked(false);
                previous_item = item ;
                Toast.makeText(MapsActivity.this,item.getTitle(),Toast.LENGTH_LONG).show();
                return false;
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getMyLocation();
    }

    private void getSources() {
        // Access a Cloud Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        CollectionReference source = db.collection("Source");
        source.get().addOnCompleteListener(task -> {
            int i = 0 ;
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    SourceModel sourceModel = document.toObject(SourceModel.class);
                    sourceModel.setPosition(i);
                    sourceModels.add(sourceModel);
                    i++;
                }
                sourceAdapter.notifyDataSetChanged();
            }
        });

    }

    private void getDrivers() {

        // Access a Cloud Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        CollectionReference source = db.collection("Drivers");
        source.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                double smallestDistance = 0.0;
                DriverModel closestDriver = null;

                for (QueryDocumentSnapshot document : task.getResult()) {
                    DriverModel driverModel = document.toObject(DriverModel.class);
                    LatLng startLatLng = new LatLng(driverModel.getLatitude(), driverModel.getLongitude());
                    LatLng endLatLng = new LatLng(sourceLocation.getLatitude(), sourceLocation.getLongitude());
                    double distance = SphericalUtil.computeDistanceBetween(startLatLng, endLatLng)/1000;
                    if (sourceLocation != null) {
                        if (smallestDistance == 0.0 || distance < smallestDistance) {
                            closestDriver = driverModel;
                            smallestDistance = distance;
                        }
                    }
                }

                if (closestDriver != null) {

                    if (closestDriverLine != null) closestDriverLine.remove();

                    LatLngBounds bounds = LatLngBounds.builder()
                            .include(new LatLng(sourceLocation.getLatitude(), sourceLocation.getLongitude()))
                            .include(new LatLng(closestDriver.getLatitude(), closestDriver.getLongitude()))
                            .build();

                    PolylineOptions line =
                            new PolylineOptions().add(
                                    new LatLng(sourceLocation.getLatitude(), sourceLocation.getLongitude()),
                                    new LatLng(closestDriver.getLatitude(), closestDriver.getLongitude()))
                                    .width(10).color(Color.RED);

                    closestDriverLine = mMap.addPolyline(line);

                    try {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                    }catch (Exception e){
                    }

                    Toast.makeText(
                            this,
                            getString(R.string.The_closest_driver_is)+ " "+closestDriver.getName() +" "+ getString(R.string.with_distance) + " "+String.format("%.2f", smallestDistance) +" "+ getString(R.string.km),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });

    }

    private void addDummySources() {

        // Access a Cloud Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        CollectionReference source = db.collection("Source");
        source.add(new SourceModel("Cairo", 29.9923056,30.598769));
        source.add(new SourceModel("Tanta", 30.7930351,29.8787504));
        source.add(new SourceModel("Alex", 31.2240349,29.8148006));
        source.add(new SourceModel("Aswan", 24.0923336,32.8825966));
        source.add(new SourceModel("Benha", 30.4589125,31.1534021));
        source.add(new SourceModel("Menofia",30.4719144,30.5411032 ));
        source.add(new SourceModel("Qena", 26.1712406,32.6864627));
        source.add(new SourceModel("Zagazig", 30.5803763,31.5014953));
        source.add(new SourceModel("Mansoura", 31.0413814,31.3478201));
        source.add(new SourceModel("Damanhour", 31.0334848,30.4377184));
    }


    private void addDummyDrivers() {
        // Access a Cloud Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        CollectionReference drivers = db.collection("Drivers");
        drivers.add(new DriverModel("Ahmed", 31.166415, 30.927197));
        drivers.add(new DriverModel("Mohamed", 31.131156, 30.419079));
        drivers.add(new DriverModel("Khaled", 30.829747, 31.646802));
        drivers.add(new DriverModel("Osman", 30.387702, 30.954663));
        drivers.add(new DriverModel("Eslam", 30.697581, 30.861279));
        drivers.add(new DriverModel("Ali", 31.3766015,31.6805238));
        drivers.add(new DriverModel("Essa",28.328057, 32.084271 ));
        drivers.add(new DriverModel("Samy", 29.443884, 29.887006));
        drivers.add(new DriverModel("Goda", 29.405608, 28.700482));
        drivers.add(new DriverModel("Osama",28.056935, 29.799115 ));

        checkDrivers(db);
    }

    private void getMyLocation() {
        gpsTracker = new GpsTracker(this);
        try {
            myAdressLat = gpsTracker.getLatitude();
            myAdressLng = gpsTracker.getLongitude();
            LatLng latLng = new LatLng(myAdressLat, myAdressLng);
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(latLng).zoom(9).build()));
        } catch (Exception e) {
        }
    }

    @Override
    public void onClick(SourceModel sourceModel) {
        if (isResultForSource) {
            binding.edtSource.setText(sourceModel.getName());
            sourceLocation = sourceModel;
        } else if (isResultForDestination) {
            binding.edtDestination.setText(sourceModel.getName());
            destinationLocation = sourceModel;
        }
        binding.rvSource.setVisibility(View.GONE);
}

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }
    @Override
    public void afterTextChanged(Editable s) {
        sourceAdapter.getFilter().filter(s.toString());
        binding.rvSource.setVisibility(View.VISIBLE);
        if (isResultForSource)
            sourceLocation=null ;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sourceAdapter.filteredItems.size()==0)
                    binding.rvSource.setVisibility(View.GONE);
            }
        },100);
    }
}