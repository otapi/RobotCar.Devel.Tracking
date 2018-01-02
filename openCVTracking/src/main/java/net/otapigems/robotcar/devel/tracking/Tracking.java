/*
    RobotCar.Devel.Tracking
    Color based tracking of multiple objects with OpenCV on Android.
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

package net.otapigems.robotcar.devel.tracking;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Moments;

import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.moments;

public class Tracking {
    private static final String  TAG = "Tracking";

    //max number of objects to be detected in frame
    final int MAX_NUM_OBJECTS=50;
    //minimum and maximum object area
    final int MIN_OBJECT_AREA = 15*15;

    Mat HSV, threshold;
    public UIStates UIState;
    public enum UIStates {
        CALIBRATION,
        TRACKING
    }
    public enum ViewTypes {
        RGB,
        HSV,
        Eroded,
        Dilated;


        private static ViewTypes[] vals = values();

        public ViewTypes next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }
    public ViewTypes viewTypes;
    List<TrackObject> objects;
    ArrayList<TrackObject> objectOccurencies;

    public Tracking() {
        objects = new ArrayList<>();
        objectOccurencies = new ArrayList<>();
        UIState = UIStates.CALIBRATION;
        viewTypes = ViewTypes.RGB;
    }
    String intToString(int number){
        return Integer.toString(number);
    }

    void drawObject(TrackObject obj, Mat frame, List<MatOfPoint> contours, Mat hierarchy){
        //Scalar color = obj.getColor();
        Scalar color = new Scalar(255, 255, 255);

        Imgproc.drawContours(frame,contours,obj.getHierarchyIndex(),color,3,8,hierarchy, 1, new Point(0, 0));
        Imgproc.circle(frame,new Point(obj.getXPos(),obj.getYPos()),5,color);
        Imgproc.putText(frame,intToString(obj.getXPos())+ " , " + intToString(obj.getYPos()),new Point(obj.getXPos(),obj.getYPos()+20),1,1,color);
        Imgproc.putText(frame,obj.getName(),new Point(obj.getXPos(),obj.getYPos()-20),1,2,color);
    }

    void morphOps(Mat thresh){

        //create structuring element that will be used to "dilate" and "erode" image.
        //the element chosen here is a 3px by 3px rectangle

        Mat erodeElement = Imgproc.getStructuringElement( MORPH_RECT, new Size(3,3));
        //dilate with larger element so make sure object is nicely visible
        Mat dilateElement = Imgproc.getStructuringElement( MORPH_RECT, new Size(8,8));

        Imgproc.erode(thresh,thresh,erodeElement);
        Imgproc.erode(thresh,thresh,erodeElement);

        Imgproc.dilate(thresh,thresh,dilateElement);
        Imgproc.dilate(thresh,thresh,dilateElement);
    }

    void trackFilteredObject(TrackObject theObject, Mat threshold, Mat cameraFeed){

        //these two vectors needed for output of findContours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        //find contours of filtered image using openCV findContours function
        Mat temp = new Mat();
        threshold.copyTo(temp);
        Imgproc.findContours(temp,contours,hierarchy,Imgproc.RETR_CCOMP,Imgproc.CHAIN_APPROX_SIMPLE );
        //use moments method to find our filtered object
        double refArea = 0;
        ArrayList<TrackObject> localObjectOccurencies = new ArrayList<>();
        if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
            int numObjects = contours.size();
            //if number of objectOccurencies greater than MAX_NUM_OBJECTS we have a noisy filter
            if (numObjects < MAX_NUM_OBJECTS) {
                for (int index = 0; index >= 0; index = (int) hierarchy.get(0, index)[0]) {

                    Moments moment = moments(contours.get(index));
                    double area = moment.m00;

                    //if the area is less than 20 px by 20px then it is probably just noise
                    //if the area is the same as the 3/2 of the image size, probably just a bad filter
                    //we only want the object with the largest area so we safe a reference area each
                    //iteration and compare it to the area in the next iteration.
                    //if number of objectOccurencies greater than MAX_NUM_OBJECTS we have a noisy filter
                    if (area > MIN_OBJECT_AREA && area<(cameraFeed.rows()*cameraFeed.cols()/1.5) && area>refArea && objectOccurencies.size()<MAX_NUM_OBJECTS ) {

                        TrackObject trackObject = new TrackObject();

                        trackObject.setXPos((int) Math.round(moment.m10 / area));
                        trackObject.setYPos((int) Math.round(moment.m01 / area));
                        trackObject.setName(theObject.getName());
                        trackObject.setColor(theObject.getColor());
                        trackObject.setArea(area);
                        trackObject.setHierarchyIndex(index);

                        localObjectOccurencies.add(trackObject);
                        refArea = area;
                    }
                }


            } else {
                Imgproc.putText(cameraFeed, "TOO MUCH NOISE! ADJUST FILTER", new Point(0, 50), 1, 2, new Scalar(0, 0, 255), 2);
            }
        }

        for(TrackObject obj : localObjectOccurencies) {
            drawObject(obj, cameraFeed, contours,hierarchy);
        }
        objectOccurencies.addAll(localObjectOccurencies);
    }

    Mat onCameraFrame(Mat cameraFeed) {
        if (cameraFeed.empty()) {
            return cameraFeed;
        }

        switch (UIState) {
            case CALIBRATION:
                break;
            case TRACKING:
                objectOccurencies = new ArrayList<>();

                HSV = new Mat();
                cvtColor(cameraFeed,HSV,Imgproc.COLOR_RGB2HSV_FULL);

                for (TrackObject trackObject : objects) {
                    //convert frame from RGB to HSV colorspace
                    threshold = new Mat();
                    Core.inRange(HSV, trackObject.getHSVmin(),trackObject.getHSVmax(),threshold);
                    morphOps(threshold);
                    if (viewTypes == ViewTypes.Eroded) {
                        threshold.copyTo(cameraFeed);
                    }
                    trackFilteredObject(trackObject,threshold,cameraFeed);
                    threshold.release();
                }

                if (viewTypes == ViewTypes.HSV) {
                    HSV.copyTo(cameraFeed);
                }
                HSV.release();
                break;
            default:
        }
                return cameraFeed;
    }


    /**
     * Automatically tune an HSV filter for the given frame and as new TrackObject
     * @param name optional. If null, it will give an order number
     * @param rectangleROI Selected rectangle
     * @param touchedRegionRgba Selected image RGBA
     * @return name of the object
     */
    public String addTrackObject(String name, Rect rectangleROI, Mat touchedRegionRgba) {

        String n = name;
        if (n == null) {
            n = "Obj "+objects.size();
        };
        Log.i(TAG, "touchedRegionRgba: cols="+touchedRegionRgba.cols()+", rows="+touchedRegionRgba.rows());

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        Scalar mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = rectangleROI.width*rectangleROI.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        Scalar mBlobColorRgba = convertScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        int H_MIN = 256;
        int H_MAX = 0;
        int S_MIN = 256;
        int S_MAX = 0;
        int V_MIN = 256;
        int V_MAX = 0;

        // find min and max values inside the selected section
        for(int row=1;row<touchedRegionHsv.rows();row++) {
            for(int col=1;col<touchedRegionHsv.cols();col++) {
                double[] val = touchedRegionHsv.get(row, col);
                if (val[0]<H_MIN) {
                    H_MIN = (int) val[0];
                }
                if (val[0]>H_MAX) {
                    H_MAX = (int) val[0];
                }
                if (val[1]<S_MIN) {
                    S_MIN = (int) val[1];
                }
                if (val[1]>S_MAX) {
                    S_MAX = (int) val[1];
                }
                if (val[2]<V_MIN) {
                    V_MIN = (int) val[2];
                }
                if (val[2]>V_MAX) {
                    V_MAX = (int) val[2];
                }
            }
        }

        objects.add(new TrackObject(n, new Scalar(H_MIN, S_MIN, V_MIN), new Scalar(H_MAX, S_MAX, V_MAX), mBlobColorRgba));
        touchedRegionRgba.release();
        touchedRegionHsv.release();
        UIState = UIStates.TRACKING;
        return n;
    }

    private Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}
