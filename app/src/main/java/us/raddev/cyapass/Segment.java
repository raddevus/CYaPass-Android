package us.raddev.cyapass;

import android.graphics.Point;

/**
 * Created by roger.deutsch on 10/19/2016.
 */
public class Segment {
    Point Begin;
    Point End;
    int PointValue;

    public Segment(Point begin, Point end, int pointValue) {
        Begin = begin;
        End = end;
        PointValue = pointValue;
    }


    @Override
    public boolean equals(Object obj) {
        Segment incoming = as(obj, Segment.class);

        if (incoming == null)
        {
            return false;
        }

        Boolean a = ((this.Begin.x == incoming.Begin.x) && (this.Begin.y == incoming.Begin.y)) &&
                ((this.End.x == incoming.End.x) && (this.End.y == incoming.End.y));

        Boolean b = ((this.Begin.x == incoming.End.x) && (this.Begin.y == incoming.End.y)) &&
                ((this.End.x == incoming.Begin.x) && (this.End.y == incoming.Begin.y));
        return (a || b);
    }

    private Segment as(Object o, Class<Segment> tClass) {
        return tClass.isInstance(o) ? (Segment) o : null;
    }

    @Override
    public int hashCode() {
        String flippedHashValueString = String.format("%d%d%d%d",
                this.End.x, this.End.y,
                this.Begin.x, this.Begin.y);

        String hashValueString = String.format("%d%d%d%d",
                this.Begin.x, this.Begin.y,
                this.End.x, this.End.y);

        return flippedHashValueString.hashCode() + hashValueString.hashCode();
    }
}
