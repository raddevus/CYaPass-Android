package us.raddev.drawpass;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by roger.deutsch on 6/7/2016.
 */
public class GridView extends View {

    private List<Point> _allPosts;
    private int centerPoint;;
    private int postWidth;
    private int leftOffset;
    private int topOffset = 20;
    private List<Point> userShape = new ArrayList<Point>();
    private Point previousPoint;
    private LineSegments LineSegments = new LineSegments();
    private Canvas xCanvas;
    private int viewWidth;
    private int viewHeight;

    int numOfCells = 5;
    int cellSize; //125

    public GridView(Context context) {
        super(context);
        float density = getResources().getDisplayMetrics().density;
        Log.d("MainActivity", "density : " + String.valueOf(density));
        int densityDPI = getResources().getDisplayMetrics().densityDpi;
        Log.d("MainActivity", "densityDPI : " + String.valueOf(densityDPI));
        viewWidth = getResources().getDisplayMetrics().widthPixels;
        Log.d("MainActivity", "viewWidth : " + String.valueOf(viewWidth));
        viewHeight = getResources().getDisplayMetrics().heightPixels;
        Log.d("MainActivity", "viewHeight : " + String.valueOf(viewHeight));

        View vx = this.getRootView();

        Log.d("MainActivity", "id: " + String.valueOf(vx.getId()));
        //postWidth = ((viewWidth / 2) / 6) /5;
        postWidth = 13;
        cellSize = centerPoint = (int)viewWidth /7;
        leftOffset = viewWidth - ((numOfCells + 1)* cellSize); //(viewWidth / densityDPI) * 6;

        Log.d("MainActivity", "postWidth : " + String.valueOf(postWidth));
        //leftOffset = (int)(viewWidth / .9) / 4;

        //leftOffset = (int)(viewWidth / .9) / 4;
        Log.d("MainActivity", "leftOffset : " + String.valueOf(leftOffset));
        GenerateAllPosts();
/*        this.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Toast.makeText(v.getContext(), "You made a mess", Toast.LENGTH_LONG).show();
            }

        }); */


//        Button yourButton = new Button(this.getContext());
        //do stuff like add text and listeners.


//       addView(yourButton);


        this.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    int touchX = (int)event.getX();
                    int touchY = (int)event.getY();
                    String output = "Touch coordinates : " +
                            String.valueOf(touchX) + "x" + String.valueOf(touchY);
                    //Toast.makeText(v.getContext(), output, Toast.LENGTH_SHORT).show();
                    if (SelectNewPoint(new Point(touchX,touchY))) {
                        v.invalidate();
                        CalculateGeometricSaltValue();
                    }
                }
                return true;
            }
        });
    }

    public void ClearGrid(){
        userShape = new ArrayList<Point>();
        LineSegments = new LineSegments();
    }

    private void DrawUserShapes(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(5);
        for (int j=0;j < LineSegments.size();j++) {
            canvas.drawLine(LineSegments.get(j).Start.x, LineSegments.get(j).Start.y,
                    LineSegments.get(j).End.x,
                    LineSegments.get(j).End.y, paint);
//            Log.d("MainActivity", "DONE drawing line...");
        }

    }

    private boolean IsNewLineSegment(LineSegment l)
    {
        LineSegment duplicate = LineSegments.CheckDuplicate(l);//  Find(ls => (ls.Start.X == l.Start.X && ls.Start.Y == l.Start.Y) && (ls.End.X == l.End.X && ls.End.Y == l.End.Y) || (ls.End.X == l.Start.X && ls.End.Y == l.Start.Y) && (ls.Start.X == l.End.X && ls.Start.Y == l.End.Y));
        if (duplicate == null)
        {
            Log.d("MainActivity", "NOT a DUP!");
            return true;
        }
        else
        {
            Log.d("MainActivity", "It's a DUP!");
            return false;
        }
    }

    private boolean SelectNewPoint(Point p)
    {
        boolean isNewPoint = false;
        Point currentPoint = HitTest(new Point(p.x, p.y));
        if (currentPoint != null)
        {
            if (IsNewPoint(currentPoint))
            {
                if (userShape.size() > 0)
                {
                    if (IsNewLineSegment(new LineSegment(userShape.get(userShape.size() - 1), currentPoint))) // never allow a duplicate line segment in the list
                    {
                        LineSegments.add(new LineSegment(userShape.get(userShape.size() - 1), currentPoint));
                    }
                }
                userShape.add(currentPoint);
                isNewPoint = true;
                //DrawHighlight();
            }

            previousPoint = currentPoint;
        }
        return isNewPoint;
    }

    public void GeneratePassword(){
        CreateHash();
    }

    private void CalculateGeometricSaltValue()
    {
        LineSegments.PostPoints = "";
        LineSegments.PostValue = 0;

        for (LineSegment l : LineSegments)
        {
            for (int x = 0; x < _allPosts.size(); x++)
            {
                if (l.Start.x == _allPosts.get(x).x && l.Start.y == _allPosts.get(x).y)
                {
                    Log.d("MainActivity",String.format("START x : %d", x));
                    LineSegments.AddOn(x);
                }
                if (l.End.x == _allPosts.get(x).x && l.End.y == _allPosts.get(x).y)
                {
                    Log.d("MainActivity",String.format("END x : %d", x));
                    LineSegments.AddOn(x);
                }
            }
        }
        Log.d("MainActivity",String.format("Value : %d",LineSegments.PostValue));
        Log.d("MainActivity",String.format("Points: %s",LineSegments.PostPoints));
    }

    private boolean IsNewPoint(Point currentPoint)
    {
        if (userShape.size() > 0)
        {
            if (userShape.size() > 1)
            {

                if (!((currentPoint.x == userShape.get(userShape.size() - 1).x && currentPoint.y == userShape.get(userShape.size() - 1).y)
                        || (currentPoint.x == userShape.get(userShape.size() - 2).x || currentPoint.y == userShape.get(userShape.size() - 2).y)))
                {
                    return true;
                }
            }
            if (!(currentPoint.x == userShape.get(userShape.size() - 1).x && currentPoint.y == userShape.get(userShape.size() - 1).y))
            {
                return true;
            }
        }
        else { return true; }
        return false;
    }

    private void GenerateAllPosts(){
        _allPosts = new ArrayList<Point>();
        // NOTE: removed the -(postWid/2) because drawLine works via centerpoint instead of offset like C#
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 6; y++) {
                _allPosts.add(new Point(leftOffset + (centerPoint * x), topOffset +(centerPoint * y) ));
                Log.d("MainActivity", "Point.x = " + String.valueOf(leftOffset + (centerPoint * x)));
                Log.d("MainActivity", "Point.y = " + String.valueOf(topOffset + (centerPoint * y)));
            }
        }
    }

    //@TargetApi(19)
    private void CreateHash(){
        String site = MainActivity.siteKey.getText().toString();  //"amazon";
        String text = LineSegments.PostValue + site;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            Log.d("MainActivity", sb.toString());
            MainActivity.SetPassword( sb.toString());
        }
        catch (NoSuchAlgorithmException nsa){

        }
    }

    private void DrawGridLines(){
        Paint paint=new Paint();

        for (int y = 0; y <= numOfCells; ++y)
        {
            xCanvas.drawLine(0 +leftOffset, (y * cellSize)+topOffset,
                    (numOfCells * cellSize)+leftOffset,
                    (y * cellSize)+topOffset,paint);
        }

        for (int x = 0; x <= numOfCells; ++x)
        {
            xCanvas.drawLine( (x * cellSize)+leftOffset, 0+topOffset,
                    (x * cellSize)+leftOffset, (numOfCells * cellSize)+topOffset,
                    paint);
        }

    }

    private void DrawPosts(){

        Paint paint=new Paint();
        // Use Color.parseColor to define HTML colors
        paint.setColor(Color.parseColor("#CD5C5C"));

        for (Point Pt : _allPosts) {
            xCanvas.drawCircle(Pt.x, Pt.y, postWidth, paint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        this.xCanvas = canvas;
        super.onDraw(canvas);

        DrawPosts();
        DrawGridLines();
        DrawUserShapes(canvas);
    }

    private Point HitTest(Point p)
    {
        for (Point Pt : _allPosts)
        {
            if ((p.x >= (Pt.x ) - (postWidth*2)) && (p.x <= (Pt.x) + (postWidth*2)))
            {
                if ((p.y >= (Pt.y ) - (postWidth*2)) && (p.y <= (Pt.y) + (postWidth*2)))
                {
                    //String output = String.format("it's a hit: %d %d",p.x,p.y);
                    //Toast.makeText(this.getContext(), output, Toast.LENGTH_SHORT).show();
                    return Pt;
                }
            }
        }

        return null;
    }
}
