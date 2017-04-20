package com.example.oregami.androidserver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.Server;
import com.yanzhenjie.andserver.website.AssetsWebsite;
import com.yanzhenjie.andserver.RequestHandler;
import android.content.res.AssetManager;
import android.view.Window;
import android.view.WindowManager;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    private Server mServer;
    private AssetManager mAssetManager;
    private static final String TAG = "CamTestActivity";
    Preview preview;
    Camera camera;
    Activity act;
    Context ctx;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        act = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        preview = new Preview(this, (SurfaceView)findViewById(R.id.surfaceView));
        preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ((FrameLayout) findViewById(R.id.layout)).addView(preview);
        preview.setKeepScreenOn(true);


        AndServer andServer = new AndServer.Build()
                .port(12345)
                .timeout(10 * 1000)
                .registerHandler("test", new RequestLoginHandler())
//                // .registerHandler("download", new RequestFileHandler("Your file path"))
//                .registerHandler("upload", new RequestUploadHandler())
                .website(new AssetsWebsite(mAssetManager, ""))
//                .listener(mListener)
                .build();

        // Create server.
        mServer = andServer.createServer();
        mServer.start();


    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                camera.takePicture(shutterCallback, rawCallback, jpegCallback);
                Toast.makeText(ctx, "photo taken", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        int numCams = Camera.getNumberOfCameras();
        if(numCams > 0){
            try{
                camera = Camera.open(0);
                camera.setDisplayOrientation(90);
                camera.startPreview();
                preview.setCamera(camera);
            } catch (RuntimeException ex){
                Toast.makeText(ctx, "not found", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        if(camera != null) {
            camera.stopPreview();
            preview.setCamera(null);
            camera.release();
            camera = null;
        }
        super.onPause();
    }

    private void resetCam() {
        camera.startPreview();
        preview.setCamera(camera);
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            //			 Log.d(TAG, "onShutter'd");
        }
    };

    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            //			 Log.d(TAG, "onPictureTaken - raw");
        }
    };

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            new SaveImageTask().execute(data);
            resetCam();
            Log.d(TAG, "onPictureTaken - jpeg");
        }
    };

    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;
            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (sdCard.getAbsolutePath() + "/camtest");
                dir.mkdirs();

                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);
                outStream = new FileOutputStream(outFile);
                BufferedOutputStream bos =
                        new BufferedOutputStream(outStream);
                Bitmap bMap = BitmapFactory.decodeByteArray(data[0], 0, data[0].length);
                Bitmap bMapRotate;

                Matrix matrix = new Matrix();
                matrix.reset();
                matrix.postRotate(90);
                bMapRotate = Bitmap.createBitmap(bMap, 0, 0, bMap.getWidth(),
                        bMap.getHeight(), matrix, true);
                bMap = bMapRotate;
                bMap.compress(Bitmap.CompressFormat.JPEG, 100, bos);

//              outStream.write(data[0]);
                outStream.flush();
                outStream.close();

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

                refreshGallery(outFile);
            } catch (FileNotFoundException e) {
                Log.d(TAG, e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "here3");
                Log.d(TAG, e.getMessage());
                e.printStackTrace();
            } finally {
            }
            return null;
        }

    }

    class RequestLoginHandler implements RequestHandler {

        @Override
        public void handle(HttpRequest req, HttpResponse res, HttpContext con) throws java.io.UnsupportedEncodingException{
            Message msg = new Message();
            msg.what = 1;
            handler.sendMessage(msg);
            StringEntity stringEntity = new StringEntity("my android server resp", "utf-8");
            res.setEntity(stringEntity);

        }
    }
}


