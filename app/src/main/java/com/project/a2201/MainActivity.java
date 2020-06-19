package com.project.a2201;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private TessBaseAPI m_tess;
    String result;
    String mCameraFilePath;
    ImageView imgView;
    Bitmap mBitmap;
    private static final int CAMERA_REQUEST_CODE = 23;
    private static final int GALLERY_REQUEST_CODE = 321;
    private static final int REQUEST_PERMISSION_READ = 123;
    private static final int REQUEST_PERMISSION_WRITE = 113;
    private TextView resultView;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultView = (TextView) findViewById(R.id.txt_result);
        initImageView();
        try {
            prepareLanguageDir();
            m_tess = new TessBaseAPI();
            m_tess.init(String.valueOf(getFilesDir()), "vie");
        } catch (Exception e) {
            // Logging here
        }
    }

    private void initImageView() {
        imgView = (ImageView) findViewById(R.id.img_input);
        Bitmap input = BitmapFactory.decodeResource(getResources(), R.drawable.input_image3);
        imgView.setImageBitmap(input);
    }

    // copy file from assets to another folder due to accessible
    private void copyFile() throws IOException {
        // work with assets folder
        AssetManager assMng = getAssets();
        InputStream is = assMng.open("tessdata/vie.traineddata");
        OutputStream os = new FileOutputStream(getFilesDir() +
                "/tessdata/vie.traineddata");
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
            os.write(buffer, 0, read);
        }

        is.close();
        os.flush();
        os.close();
    }

    private void prepareLanguageDir() throws IOException {
        File dir = new File(getFilesDir() + "/tessdata");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File trainedData = new File(getFilesDir() + "/tessdata/vie.traineddata");
        if (!trainedData.exists()) {
            copyFile();
        }
    }

    private Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {

        ExifInterface ei = new ExifInterface(selectedImage.getPath());
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_READ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                finish();
                startActivity(getIntent());
            } else finish();
        }
        else if (requestCode == REQUEST_PERMISSION_WRITE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                finish();
                startActivity(getIntent());
            } else finish();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK)
            switch (requestCode){
                case GALLERY_REQUEST_CODE:
                    //data.getData return the content URI for the selected Image
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };
                    // Get the cursor
                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    // Move to first row
                    cursor.moveToFirst();
                    //Get the column index of MediaStore.Images.Media.DATA
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    //Gets the String value in the column
                    String imgDecodableString = cursor.getString(columnIndex);
                    cursor.close();
                    // Set the Image in ImageView after decoding the String
                    try {
                        mBitmap = rotateImageIfRequired(BitmapFactory.decodeFile(imgDecodableString)
                                , Uri.parse(imgDecodableString));
                        imgView.setImageBitmap(mBitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case CAMERA_REQUEST_CODE:
//                    try {
//                        mBitmap = rotateImageIfRequired(BitmapFactory.decodeFile(mCameraFilePath)
//                                , Uri.parse(mCameraFilePath));
//                        imgView.setImageBitmap(mBitmap);
                        Bundle extras = data.getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
//                        imgView.setImageBitmap(imageBitmap);

                    //                    try {
                        mBitmap =rotateImage(imageBitmap,90);
                        imgView.setImageBitmap(mBitmap);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                    break;
            }
    }

    public void pickFromGallery(View view){
        //Create an Intent with action as ACTION_PICK
        Intent intent=new Intent(Intent.ACTION_PICK);
        // Sets the type as image/*. This ensures only components of type image are selected
        intent.setType("image/*");
        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES,mimeTypes);
        // Launching the Intent
        startActivityForResult(intent,GALLERY_REQUEST_CODE);
    }
    public void captureFromCamera(View view) {
//        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//            intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", createImageFile()));
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
    }



    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        //This is the directory in which the file will be created. This is the default location of Camera photos
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for using again
        mCameraFilePath = image.getAbsolutePath();
        return image;
    }


    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }


    public void doRecognize(View view) {
        if (m_tess == null) {
            return;
        }

        try {

            Bitmap bitmap = ((BitmapDrawable) imgView.getDrawable()).getBitmap();
            m_tess.setImage(bitmap);
            result = m_tess.getUTF8Text();l

            resultView.setText(result);

        } catch (Exception e) {
            // Do what you like here...
        }
    }

    public void doSolve(View view) {

        System.out.println(result);
        String[] output = result.split("=");
        System.out.println(output[0]);
        System.out.println(output[1]);
        float D = Float.parseFloat(output[1]);
        if (D != 0) {
            output[0] = output[0] + "-" + output[1];
            System.out.println(output[0]);
            String[] front = output[0].split("\\+");
            System.out.println(front[0]);
            System.out.println(front[1]);
            System.out.println(front[2]);
            String[] a = front[0].split("x");
            System.out.println(a[0]);
            System.out.println(a[1]);

        } else {
            String[] front = output[0].split("\\+");
            System.out.println(front[0]);
            System.out.println(front[1]);
            System.out.println(front[2]);
            String[] a = front[0].split("x");
            System.out.println(a[0]);
            System.out.println(a[1]);
            String b = front[1].substring(0, 1);
            System.out.println(b);
            float A = Float.parseFloat(a[0]);
            float B = Float.parseFloat(b);
            float C = Float.parseFloat(front[2]);
            System.out.println(A);
            System.out.println(B);
            System.out.println(C);
            ///ptb2
            float delta = B * B - 4 * A * C;
            double sqrt = Math.sqrt(delta);
            double x1;
            double x2;

            if (A == 0) {
                if (B == 0) {
                    System.out.println("quadratic equation has no solution");
                } else {
                    System.out.println("quadratic equations have a solution: " + "x = " + (-C / B));
                }
            } else {
                if (delta > 0.0) {
                    System.out.println("delta: " + delta);
                    x1 = (-B + sqrt) / (2 * A);
                    x2 = (-B - sqrt) / (2 * A);
                    System.out.println("Roots are::" + x1 + "and" + x2);
                } else if (delta == 0) {
                    System.out.println("Root is::" + (-B + sqrt) / (2 * A));
                } else if (delta < 0) {
                    System.out.println("quadratic equation has no solution");
                }

            }
        }
    }
}
