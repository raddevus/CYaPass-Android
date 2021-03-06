package com.cyapass.cyapass;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by roger.deutsch on 10/19/2016.
 */
public class UserPath {
    List<Point> allPoints = new ArrayList<Point>();
    public HashSet<Segment> allSegments = new HashSet<Segment>();
    private Point currentPoint;
    public int PointValue;
    private int previousPostValue;

    public void append(Point currentPoint, int postValue)
    {
        this.currentPoint = currentPoint;

        if (allPoints.size() >= 1)
        {
            int currentSegmentCount = allSegments.size();
            if (allPoints.get(allPoints.size() - 1).x == currentPoint.x &&
                    allPoints.get(allPoints.size() - 1).y == currentPoint.y)
            {
                // user clicked the same point twice
                return;
            }
            allSegments.add(new Segment(allPoints.get(allPoints.size() - 1),
                    currentPoint, postValue + previousPostValue));
        }
        allPoints.add(currentPoint);
        previousPostValue = postValue;
    }

    public void CalculateGeometricValue()
    {
        this.PointValue = 0;
        for (Segment s : allSegments)
        {
            this.PointValue += s.PointValue;
        }
    }
}
