package us.raddev.cyapass;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by roger.deutsch on 6/7/2016.
 */
class LineSegments extends ArrayList<LineSegment>
{
    public String PostPoints;
    public int PostValue;
    public void AddOn(int pointValue){
        PostValue += pointValue;
        PostPoints += String.format("%s,", pointValue);
    }

    public LineSegment CheckDuplicate(LineSegment l){
        //if (this.size() == 0){return null;}
        for (LineSegment ls : this){
            if (((ls.Start.x == l.Start.x && ls.Start.y == l.Start.y) && (ls.End.x == l.End.x && ls.End.y == l.End.y))
                    || ((ls.End.x == l.Start.x && ls.End.y == l.Start.y) && (ls.Start.x == l.End.x && ls.Start.y == l.End.y)))

            {
                return ls;
            }
        }
        return null;
    }

}