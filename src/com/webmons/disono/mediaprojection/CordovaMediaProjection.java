package com.webmons.disono.mediaprojection;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.Toast;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

import static android.app.Activity.RESULT_OK;

public class CordovaMediaProjection extends CordovaPlugin {

    private static final String TAG = "CordovaMediaProjection";
    private static final int REQUEST_CODE = 1000;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_PERMISSIONS = 10;
    private boolean isRecording = false;
    private String recording_filename = null;

    private Activity activity;
    private CallbackContext callbackContext;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        activity = cordova.getActivity();
        this.callbackContext = callbackContext;

        if (action.equals("start")) {
            _startRecording();

            // Don't return any result now
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);

            return true;
        } else if (action.equals("stop")) {
            Toast.makeText(activity.getApplicationContext(), "Stop Recording...", Toast.LENGTH_SHORT).show();
            _stopRecording();

            return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }

        if (resultCode != RESULT_OK) {
            Toast.makeText(activity.getApplicationContext(), "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            isRecording = false;

            return;
        }

        mMediaProjectionCallback = new MediaProjectionCallback();
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    /**
     * Start recording and capturing
     */
    private void _startRecording() {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) + ContextCompat
                .checkSelfPermission(activity,
                        Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (activity, Manifest.permission.RECORD_AUDIO)) {

                AlertDialog.Builder builder = new AlertDialog.Builder(activity.getApplicationContext());
                builder.setMessage("Requesting Permission")
                        .setPositiveButton("ENABLE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                cordova.getThreadPool().execute(new Runnable() {
                                    public void run() {
                                        requestPermissionAction(callbackContext);
                                    }
                                });
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
            } else {
                requestPermissionAction(callbackContext);
            }
        } else {
            onScreenShare(true);
        }
    }

    /**
     * Request a permission
     *
     * @param callbackContext Cordova callback context
     */
    private void requestPermissionAction(CallbackContext callbackContext) {
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        };

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.i(TAG, "This only applies to android M.");

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission
                            .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                    REQUEST_PERMISSIONS);
            return;
        }

        cordova.requestPermissions(this, REQUEST_PERMISSIONS, permissions);
    }

    /**
     * Stop recording and capturing
     */
    private void _stopRecording() {
        onScreenShare(false);
    }

    /**
     * Create a virtual display
     *
     * @return VirtualDisplay
     */
    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("CordovaMediaProjection",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }

    /**
     * Lets start or stop the capture
     *
     * @param record Are we recording or stopping the record
     */
    public void onScreenShare(boolean record) {
        if (record && !isRecording) {
            initRecord();
            shareScreen();
        } else {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            Log.v(TAG, "Stopping Recording");
            stopCapture();
        }
    }

    /**
     * Share screen
     */
    private void shareScreen() {
        if (mMediaProjection == null) {
            cordova.startActivityForResult(this, mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }

        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    /**
     * Initialize the recorder
     */
    private void initRecord() {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        mMediaRecorder = new MediaRecorder();
        mProjectionManager = (MediaProjectionManager) activity.getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);

        recording_filename = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/video-" + _unixEpoch() + ".mp4";
        Log.i(TAG, "Recoding Path: " + recording_filename);

        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setOutputFile(recording_filename);
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mMediaRecorder.setVideoFrameRate(30);

            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();

            // we are now starting to record
            isRecording = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop screen sharing or capture
     */
    private void stopCapture() {
        if (mVirtualDisplay == null) {
            return;
        }

        mVirtualDisplay.release();
        destroyMP();
    }

    /**
     * Destroy the media projection
     */
    private void destroyMP() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        isRecording = false;
        _releaseActivity();

        Log.i(TAG, "MediaProjection Stopped");
    }

    /**
     * Media projection callback
     */
    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (isRecording) {
                isRecording = false;
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                Log.v(TAG, "Recording Stopped");
            }

            mMediaProjection = null;
            stopCapture();
        }
    }

    /**
     * Let's request permission to user
     *
     * @param requestCode  Request code
     * @param permissions  Permission we are asking
     * @param grantResults Did we grant
     */
    @Override
    public void onRequestPermissionResult(int requestCode,
                                          @NonNull String permissions[],
                                          @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if ((grantResults.length > 0) && (grantResults[0] +
                        grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
                    // let's start to record the screen
                    onScreenShare(true);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity.getApplicationContext());
                    builder.setMessage("Requesting Permission")
                            .setPositiveButton("ENABLE", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                    activity.startActivity(intent);
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            }).show();
                }
            }
        }
    }

    /**
     * Release activity
     */
    private void _releaseActivity() {
        Toast.makeText(activity.getApplicationContext(), "Saving video to (" + recording_filename + ")", Toast.LENGTH_SHORT).show();

        // at last call sendPluginResult
        PluginResult result = new PluginResult(PluginResult.Status.OK, recording_filename);
        // release status callback in JS side
        result.setKeepCallback(false);
        callbackContext.sendPluginResult(result);
    }

    /**
     * Unix epoch
     *
     * @return Return random number (unix time based)
     */
    private long _unixEpoch() {
        return System.currentTimeMillis() / 1000L;
    }
}