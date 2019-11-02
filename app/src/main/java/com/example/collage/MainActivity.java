package com.example.collage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "MAIN_ACTIVITY";

    private ImageButton mImageButton1, mImageButton2, mImageButton3, mImageButton4; //image button variables

    private List<ImageButton> mImageButtons; //list of all image buttons
    private ArrayList<String> mImageFilePaths; //list of file paths for image files

    private String mCurrentImagePath; //String to hold the file path for an image

    private final static String BUNDLE_KEY_IMAGE_FILE_PATHS = "bundle key image file paths";
    private final static String BUNDLE_KEY_MOST_RECENT_FILE_PATH = "bundle key most recent file path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageButton1 = findViewById(R.id.imageButton1); //connect to front end
        mImageButton2 = findViewById(R.id.imageButton2);
        mImageButton3 = findViewById(R.id.imageButton3);
        mImageButton4 = findViewById(R.id.imageButton4);

        mImageButtons = new ArrayList<>(Arrays.asList(mImageButton1, mImageButton2, mImageButton3, mImageButton4)); //add buttons to list

        for(ImageButton button : mImageButtons){
            button.setOnClickListener(this); //listener in this activity
        }


        if(savedInstanceState != null){ //retrieve bundles
            mCurrentImagePath = savedInstanceState.getString(BUNDLE_KEY_MOST_RECENT_FILE_PATH);
            mImageFilePaths = savedInstanceState.getStringArrayList(BUNDLE_KEY_IMAGE_FILE_PATHS);
        }

        if (mCurrentImagePath == null){
            mCurrentImagePath = ""; //empty string instead of null object
        }

        if(mImageFilePaths == null){
            mImageFilePaths = new ArrayList<>(Arrays.asList("","","","")); //same as above
        }
    }

    @Override
    public void onClick(View view){ //listener for all buttons
        int requestCodeButtonIndex = mImageButtons.indexOf(view); //takes imagebutton from list index
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //send to image capture intent

        if(takePictureIntent.resolveActivity(getPackageManager()) != null){ //makes sure there's an image
            try{
                File imageFile = createImageFile(); //create file for image
                if(imageFile != null){ //use URI for image, retrieve from intent
                    Uri imageURI = FileProvider.getUriForFile(this, "com.example.collage.fileprovider", imageFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI);
                    startActivityForResult(takePictureIntent, requestCodeButtonIndex);
                } else {
                    Log.e(TAG, "Image file is null");
                }
            } catch (IOException e){
                Log.e(TAG, "Error creating image file" + e);
            }
        }
    }

    private File createImageFile() throws IOException {
        String imageFilename = "COLLAGE_" + new Date().getTime(); //unique file name due to time created
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); //file path
        File imageFile = File.createTempFile( //create file name/type/path
                imageFilename,
                ".jpg",
                storageDir
        );

        mCurrentImagePath = imageFile.getAbsolutePath(); //formalize path
        return imageFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Log.d(TAG, "onActivityResult for request code " + requestCode +
                    " and current path " + mCurrentImagePath);
            mImageFilePaths.set(requestCode, mCurrentImagePath); //retrieve file path, add to list
            requestSaveImageToMediaStore(); //send file path to media store
        } else if (resultCode == RESULT_CANCELED) {
            mCurrentImagePath = "";
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){ //handle rotate
        Log.d(TAG, "focus changed " + hasFocus);
        if(hasFocus){
            for(int index = 0; index < mImageButtons.size(); index++){
                loadImage(index);
            }
        }
    }

    private void loadImage(int index) {

        ImageButton imageButton = mImageButtons.get(index); //use index of image button
        String path = mImageFilePaths.get(index); //retrieve index of image button to find file

        if (path != null && !path.isEmpty()){ //check if empty
            Picasso.get() //use picasso to load file into image button
                    .load(new File(path))
                    .error(android.R.drawable.stat_notify_error)
                    .fit()
                    .centerCrop()
                    .into(imageButton, new Callback(){
                        @Override
                        public void onSuccess(){
                            Log.d(TAG, "Image loaded");
                        }
                        @Override
                        public void onError(Exception e){
                            Log.e(TAG, "error loading image", e);
                        }
                    });
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outBundle){ //send info to bundles
        super.onSaveInstanceState(outBundle);
        outBundle.putString(BUNDLE_KEY_MOST_RECENT_FILE_PATH, mCurrentImagePath);
        outBundle.putStringArrayList(BUNDLE_KEY_IMAGE_FILE_PATHS, mImageFilePaths);
    }

    private void requestSaveImageToMediaStore(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == //permission already granted
                PackageManager.PERMISSION_GRANTED){
            saveImage(); //permission already granted? Save.
        } else { //Ask for permissions
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            saveImage(); //permission granted? save.
        } else { //let user no image won't be saved
            Toast.makeText(this, "Images will NOT be saved to media store", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImage(){ //save image to file path
        try{
            MediaStore.Images.Media.insertImage(getContentResolver(), mCurrentImagePath, "Collage", "Collage");
        } catch (IOException e){
            Log.e(TAG, "Image file not found", e);
        }
    }
}
