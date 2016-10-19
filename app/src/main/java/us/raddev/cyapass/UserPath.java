package us.raddev.cyapass;

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
        previousPostValue = postValue;

        if (allPoints.size() >= 1)
        {
            int currentSegmentCount = allSegments.size();

            allSegments.add(new Segment(allPoints.get(allPoints.size() - 1),
                    currentPoint, postValue + previousPostValue));
        }
        allPoints.add(currentPoint);

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
