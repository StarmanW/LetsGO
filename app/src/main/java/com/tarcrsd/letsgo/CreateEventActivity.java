package com.tarcrsd.letsgo;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.tarcrsd.letsgo.Models.EventAttendees;
import com.tarcrsd.letsgo.Models.EventOrganizer;
import com.tarcrsd.letsgo.Models.Events;
import com.tarcrsd.letsgo.Module.DateFormatterModule;
import com.tarcrsd.letsgo.Module.DatePickerFragment;
import com.tarcrsd.letsgo.Module.TimePickerFragment;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateEventActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    // POJO object
    private Events newEvent = new Events();

    // CONSTANT
    private static final int EVENT_IMG_REQUEST = 10;
    private static final int PLACE_AUTOCOMPLETE_REQUEST = 11;
    private static final String EVENT_IMG_STORAGE_PATH = "eventsImg/";

    // Event image upload
    private Uri fileUri;
    private String eventImgPath;

    // Firebase references
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;

    // UI Components
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView eventImgView;
    private EditText txtEventName;
    private EditText txtLocation;
    private EditText txtDate;
    private EditText txtTime;
    private EditText txtDescription;
    private TextView lblEventNameErr;
    private TextView lblLocationErr;
    private TextView lblDateErr;
    private TextView lblTimeErr;
    private TextView lblDescriptionErr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        initUI();
    }

    private void initUI() {
        // Replace the default toolbar with Collapsible Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Initializes views
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        eventImgView = findViewById(R.id.eventImgView);
        txtEventName = findViewById(R.id.txtEventName);
        txtLocation = findViewById(R.id.txtLocation);
        txtDate = findViewById(R.id.txtDate);
        txtTime = findViewById(R.id.txtTime);
        txtDescription = findViewById(R.id.txtDescription);
        lblEventNameErr = findViewById(R.id.lblEventNameErr);
        lblLocationErr = findViewById(R.id.lblLocationErr);
        lblDateErr = findViewById(R.id.lblDateErr);
        lblTimeErr = findViewById(R.id.lblTimeErr);
        lblDescriptionErr = findViewById(R.id.lblDescriptionErr);

        collapsingToolbar.setTitle(" ");

        // Set typing listener
        txtEventName.addTextChangedListener(this);
    }

    /**
     * Buttons onClick event handler
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnCreateEvent:
                createNewEvent();
                break;
            case R.id.eventImgView:
                uploadEventImage();
                break;
            case R.id.txtLocation:
                updateLocation();
                break;
            case R.id.txtDate:
                showDatePicker(txtDate);
                break;
            case R.id.txtTime:
                showDatePicker(txtTime);
                break;
        }
    }

    /**
     * Upload event image
     */
    private void uploadEventImage() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        photoPickerIntent.setType("image/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            startActivityForResult(Intent.createChooser(photoPickerIntent, "Please select an image"), EVENT_IMG_REQUEST);
        } else {
            startActivityForResult(photoPickerIntent, EVENT_IMG_REQUEST);
        }
    }

    /**
     * Update event details
     */
    private void createNewEvent() {
        if (isValidData()) {
            try {
                DocumentReference eventOrganierRef = db.collection("eventOrganizer").document();
                DocumentReference eventAttendeesRef = db.collection("eventAttendees").document();

                // Set new event details
                newEvent.setEventID(db.collection("events").document().getId());
                newEvent.setImage(eventImgPath);
                newEvent.setName(txtEventName.getText().toString());
                newEvent.setDate(DateFormatterModule.getDate(txtDate.getText().toString()));
                newEvent.setTime(DateFormatterModule.getTime(txtTime.getText().toString()));
                newEvent.setDescription(txtDescription.getText().toString());

                // Add new event organizer record
                EventOrganizer eventOrganizer = new EventOrganizer(
                        eventOrganierRef.getId(),
                        db.document("users/" + mAuth.getUid()),
                        db.document("events/" + newEvent.getEventID()));
                eventOrganierRef.set(eventOrganizer);

                // Add new event attendees record
                EventAttendees eventAttendee = new EventAttendees(
                        eventAttendeesRef.getId(),
                        db.document("users/" + mAuth.getUid()),
                        db.document("events/" + newEvent.getEventID()),
                        newEvent.getDate(),
                        1);
                eventAttendeesRef.set(eventAttendee);

                // Add new event record
                db.document("/events/" + newEvent.getEventID())
                        .set(newEvent)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                collapsingToolbar.setTitle(txtEventName.getText());
                                Toast.makeText(getApplicationContext(), "New event added!", Toast.LENGTH_LONG).show();

                                Intent toEventDetail = new Intent(getApplicationContext(), EventDetailsActivity.class);
                                toEventDetail.putExtra("eventID", newEvent.getEventID());
                                startActivity(toEventDetail);
                            }
                        });
            } catch (ParseException ex) {
                Log.i("ERR add new Event", ex.getMessage());
            }
        }
    }

    private void updateLocation() {
        final String api_key = getResources().getString(R.string.google_maps_key);

        /**
         * Initialize Places. For simplicity, the API key is hard-coded. In a production
         * environment we recommend using a secure mechanism to manage API keys.
         */
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), api_key);
        }

        // Start the autocomplete intent.
        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN, Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG))
                .build(this);
        startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST);
    }

    /**
     * Called once user has completed
     * select image activity
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if any image is selected
        if (requestCode == EVENT_IMG_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            // Get the image
            fileUri = data.getData();
            try {
                // Convert selected image into Bitmap.
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), fileUri);

                // Setting up bitmap selected image into ImageView.
                eventImgView.setImageBitmap(bitmap);

                // Upload image to firebase storage
                uploadImageToFirebaseStorage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                if (place != null) {
                    // Update text view location
                    txtLocation.setText(place.getAddress());

                    // Update location & locality for event object
                    newEvent.setLocation(place.getAddress());
                    try {
                        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(place.getLatLng().latitude, place.getLatLng().longitude, 1);
                        if (addresses.size() > 0) {
                            newEvent.setLocality(addresses.get(0).getLocality());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Toast.makeText(this, status.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Upload image to firebase storage
     */
    public void uploadImageToFirebaseStorage() {
        // Checking whether fileUri is empty or not.
        if (fileUri != null) {
            final FrameLayout uploadImgOverlay = findViewById(R.id.progressBarHolder);
            final StorageReference eventImgRef = mStorageRef.child(EVENT_IMG_STORAGE_PATH + System.currentTimeMillis() + "." + getFileExtension(fileUri));

            // Adding addOnSuccessListener to second StorageReference.
            eventImgRef.putFile(fileUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Remove loading screen
                            uploadImgOverlay.setVisibility(View.INVISIBLE);

                            // Get the firebase image path
                            eventImgPath = eventImgRef.getPath();
                            Toast.makeText(CreateEventActivity.this, "Event image added!", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception ex) {
                            Toast.makeText(CreateEventActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            // Display loading screen
                            uploadImgOverlay.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }

    /**
     * Get image file extension
     *
     * @param uri
     * @return
     */
    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        // Returning the file Extension.
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    /**
     * Handles the button click to create a new date picker fragment and
     * show it.
     *
     * @param view View that was clicked
     */
    public void showDatePicker(View view) {
        if (view.getId() == R.id.txtDate) {
            DialogFragment newFragment = new DatePickerFragment();
            newFragment.show(getSupportFragmentManager(),
                    getString(R.string.datepicker));
        } else if (view.getId() == R.id.txtTime) {
            DialogFragment newFragment = new TimePickerFragment();
            newFragment.show(getSupportFragmentManager(), getString(R.string.timepicker));
        }
    }

    /**
     * Data field validation
     *
     * @return
     */
    private boolean isValidData() {
        String eventName = txtEventName.getText().toString();
        String description = txtDescription.getText().toString();
        String location = txtLocation.getText().toString();
        String time = txtTime.getText().toString();
        String date = txtDate.getText().toString();
        boolean isValidData = true;

        if (!eventName.matches("^[\\S\\s\\D\\d]+$")) {
            lblEventNameErr.setVisibility(View.VISIBLE);
            isValidData = false;
        } else {
            lblEventNameErr.setVisibility(View.GONE);
        }

        if (!description.matches("^[\\S\\s\\D\\d]+$")) {
            lblDescriptionErr.setVisibility(View.VISIBLE);
            isValidData = false;
        } else {
            lblDescriptionErr.setVisibility(View.GONE);
        }

        if (!location.matches("^[\\S\\s\\D\\d]+$")) {
            lblLocationErr.setVisibility(View.VISIBLE);
            isValidData = false;
        } else {
            lblLocationErr.setVisibility(View.GONE);
        }

        if (!date.matches("^[\\S\\s\\D\\d]+$")) {
            lblDateErr.setVisibility(View.VISIBLE);
            isValidData = false;
        } else {
            lblDateErr.setVisibility(View.GONE);
        }

        if (!time.matches("^[\\S\\s\\D\\d]+$")) {
            lblTimeErr.setVisibility(View.VISIBLE);
            isValidData = false;
        } else {
            lblTimeErr.setVisibility(View.GONE);
        }

        return isValidData;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        collapsingToolbar.setTitle(txtEventName.getText().toString());
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);

        String month_string = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
        String day_string = Integer.toString(day);
        String year_string = Integer.toString(year);

        txtDate.setText(day_string +
                "-" + month_string +
                "-" + year_string +
                " " + cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));
    }

    @Override
    public void onTimeSet(TimePicker view, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, hour);
        cal.set(Calendar.MINUTE, minute);
        txtTime.setText(String.format("%02d:%02d %s", cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), DateFormatterModule.getAMOrPM(cal.get(Calendar.AM_PM))));
    }
}
