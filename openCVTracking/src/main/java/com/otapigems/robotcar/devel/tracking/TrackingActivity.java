/*
    RobotCar.Devel.Tracking
    Color based tracking of multiple objects with OpenCV on Android. Activity layer for Android app.
    Copyright (C) 2018  Barnabás Nagy - otapiGems.net - otapiGems@protonmail.ch

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.otapigems.robotcar.devel.tracking;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;

public class TrackingActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG              = "Tracking::Activity";
    private static final int TOUCHED_AREA = 200;
    private Mat mRgba;
    private Tracking tracking;
    private CameraBridgeViewBase mOpenCvCameraView;
    private Rect selectRect = null;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(TrackingActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public TrackingActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        selectRect = new Rect();

        // Set it to a square, not to a rectangle
        selectRect.width = TOUCHED_AREA;
        selectRect.height = TOUCHED_AREA;

        selectRect.x = (width / 2) - (selectRect.width/2);
        selectRect.y = (height / 2)- (selectRect.height/2);

        tracking = new Tracking();
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {

        switch (tracking.UIState) {
            case CALIBRATION:
                tracking.addTrackObject(null, selectRect, mRgba.submat(selectRect));
                break;
            case TRACKING:
                int cols = mRgba.cols();
                int rows = mRgba.rows();

                int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
                int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

                int x = (int)event.getX() - xOffset;
                int y = (int)event.getY() - yOffset;

                Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");
                if (y < rows/2) {
                    // Switch to calibration mode if the upper part of screen is touched
                    tracking.UIState = Tracking.UIStates.CALIBRATION;
                } else {
                    // Switch to next view if the lower part of screen is touched
                    tracking.viewTypes = tracking.viewTypes.next();
                }
                break;
            default:
        }
                return false; // don't need subsequent touch events
    }



    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Mat ret = tracking.onCameraFrame(mRgba);
        switch (tracking.UIState) {
            case CALIBRATION:
                Scalar color = AverageRgbaColorOfRect(ret,selectRect);
                Imgproc.rectangle(ret, selectRect.tl(), selectRect.br(), new Scalar(255,255,255));
                Imgproc.putText(ret, color.val[0] + ", " + color.val[1] +
                        ", " + color.val[2] + ", " + color.val[3], new Point(selectRect.tl().x, selectRect.tl().y-10),1, 2, color, 2);
                break;
            case TRACKING:
                int i=0;
                for(TrackObject obj : tracking.objectOccurencies){
                    int x = 10;
                    int y = 30+i*30;
                    Imgproc.circle(ret,new Point(x, y),5,obj.getColor());
                    String text = obj.getName()+": "+obj.getXPos()+", "+obj.getYPos()+", area: "+obj.getArea();
                    Imgproc.putText(ret, text, new Point(x+5, y), 1, 2, new Scalar(255, 255, 255),2);
                    i++;
                }
                break;
            default:
        }
        return ret;
    }

    static Scalar AverageRgbaColorOfRect(Mat frameRgba, Rect section) {

        Scalar mColorRgba = Core.sumElems(frameRgba.submat(section));
        int pointCount = section.width*section.height;
        for (int i = 0; i < mColorRgba.val.length; i++)
            mColorRgba.val[i] /= Math.round(pointCount);

        Log.i(TAG, "Touched rgba color: (" + mColorRgba.val[0] + ", " + mColorRgba.val[1] +
                ", " + mColorRgba.val[2] + ", " + mColorRgba.val[3] + ")");
        return mColorRgba;
    }

}
