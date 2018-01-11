/*
    RobotCar.Devel.Tracking
    Color based tracking of multiple objects with OpenCV on Android.
    Copyright (C) 2018  Barnabás Nagy - otapiGems.com - otapiGems@protonmail.ch

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.otapigems.robotcar.devel.tracking;
import org.opencv.core.Scalar;


public class TrackObject {
    private int xPos, yPos;
    private String Name;
    private Scalar HSVmin, HSVmax;
    private Scalar Color;
    private double Area;
    private int HierarchyIndex;

    public TrackObject()
    {
        //set values for default constructor
        setName("TrackObject");
        setColor(new Scalar(0,0,0));

    }

    public TrackObject(String name, Scalar HSVmin, Scalar HSVmax, Scalar RGBColor) {
        setName(name);
        setHSVmin(HSVmin);
        setHSVmax(HSVmax);
        setColor(RGBColor);
    }

    public int getXPos(){

        return xPos;

    }

    public void setXPos(int x){

        xPos = x;

    }

    public int getYPos(){

        return yPos;

    }

    public void setYPos(int y){

        yPos = y;

    }

    public Scalar getHSVmin(){

        return HSVmin;

    }
    public Scalar getHSVmax(){

        return HSVmax;
    }

    public void setHSVmin(Scalar min){

        HSVmin = min;
    }


    public void setHSVmax(Scalar max){

        HSVmax = max;
    }
    public String getName(){
        return Name;
    }

    public void setName(String t){
        Name = t;
    }

    public Scalar getColor(){
        return Color;
    }
    public void setColor(Scalar c){
        Color = c;
    }
    public double getArea(){
        return Area;
    }
    public void setArea(double area){
        Area = area;
    }
    public int getHierarchyIndex() {
        return HierarchyIndex;
    }
    public void setHierarchyIndex(int hierarchyIndex) {
        HierarchyIndex = hierarchyIndex;
    }
}
