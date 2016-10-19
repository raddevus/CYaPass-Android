package us.raddev.cyapass;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
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
    private int highlightOffset;
    private int topOffset = 20;
    private static List<Point> userShape = new ArrayList<Point>();
    private Point previousPoint;
    private static LineSegments LineSegments = new LineSegments();
    private Canvas xCanvas;
    public int viewWidth;
    public int viewHeight;
    private Point currentPoint;
    private UserPath us = new UserPath();

    int numOfCells = 5;
    public int cellSize; //125
    private Context _context;
    public View vx;

    public GridView(Context context) {
        super(context);
        _context = context;
/*        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        } */
        float density = getResources().getDisplayMetrics().density;
        Log.d("MainActivity", "density : " + String.valueOf(density));
        int densityDPI = getResources().getDisplayMetrics().densityDpi;
        Log.d("MainActivity", "densityDPI : " + String.valueOf(densityDPI));
        viewWidth = getResources().getDisplayMetrics().widthPixels;
        Log.d("MainActivity", "viewWidth : " + String.valueOf(viewWidth));
        viewHeight = getResources().getDisplayMetrics().heightPixels;
        Log.d("MainActivity", "viewHeight : " + String.valueOf(viewHeight));

        vx = this.getRootView();

        Log.d("MainActivity", "id: " + String.valueOf(vx.getId()));
        //postWidth = ((viewWidth / 2) / 6) /5;
        postWidth = (int)viewWidth / 58;
        highlightOffset = postWidth + 10;
        cellSize = centerPoint = (int)viewWidth /7;
        leftOffset = viewWidth - ((numOfCells + 1)* cellSize); //(viewWidth / densityDPI) * 6;

        Log.d("MainActivity", "postWidth : " + String.valueOf(postWidth));
        //leftOffset = (int)(viewWidth / .9) / 4;

        //leftOffset = (int)(viewWidth / .9) / 4;
        Log.d("MainActivity", "leftOffset : " + String.valueOf(leftOffset));
        GenerateAllPosts();

        this.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    int touchX = (int)event.getX();
                    int touchY = (int)event.getY();
                    String output = "Touch coordinates : " +
                            String.valueOf(touchX) + "x" + String.valueOf(touchY);
                    //Toast.makeText(v.getContext(), output, Toast.LENGTH_SHORT).show();
                    currentPoint = new Point(touchX,touchY);
                    if (SelectNewPoint(currentPoint)) {
                        v.invalidate();
                        CalculateGeometricSaltValue();
                        GeneratePassword();
                    }
                }
                return true;
            }
        });
    }

    public void ClearGrid(){
        userShape.clear();
        userShape = new ArrayList<Point>();
        //userShape.removeAll(userShape);
        LineSegments.clear();
        LineSegments = new LineSegments();
        invalidate();
        vx.invalidate();

    }

    public boolean isLineSegmentComplete(){
        //determines whether or not the userShape has at least x points in it
        // this is an arbitrary definition of it being a valid shape based upon size
        Log.d("MainActivity", "isLineSegmentComplete()");
        Log.d("MainActivity", userShape.toString());
        if (LineSegments == null){
            return false;
        }
        if (LineSegments.size() >= 1){ // we consider 2 points a valid shape (a single line)
            return true;
        }
        return false;
    }

    private void DrawUserShapes(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(8);
        paint.setStyle(Paint.Style.STROKE);
        if (LineSegments.size() > 0) {

        }
        for (int j=0;j < LineSegments.size();j++) {
            canvas.drawCircle((float) LineSegments.get(j).Start.x,
                    (float) LineSegments.get(j).Start.y,
                     highlightOffset, paint);
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

    private void DrawHighlight(Point p){
        Log.d("MainActivity", "DrawHighlight()...");
        Log.d("MainActivity", p.toString());
        Paint paint = new Paint();
        if (userShape.size() == 1) {
            paint.setColor(Color.CYAN);
        }
        else
        {
            paint.setColor(Color.BLUE);
        }
        paint.setStrokeWidth(8);
        paint.setStyle(Paint.Style.STROKE);

        xCanvas.drawCircle( p.x,
                 p.y,
                postWidth + 10, paint);

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

            }
            //DrawHighlight(currentPoint);
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
                Log.d("Extra", "Point.x = " + String.valueOf(leftOffset + (centerPoint * x)));
                Log.d("Extra", "Point.y = " + String.valueOf(topOffset + (centerPoint * y)));
            }
        }
    }

    //@TargetApi(19)
    private void CreateHash(){
        //String site = MainActivity.siteKey.getText().toString();  //"amazon";
        if (MainActivity.siteSpinner.getSelectedItemPosition() <= 0){
            MainActivity.SetPassword("");
            return;
        }
        if (!isLineSegmentComplete()){
            MainActivity.SetPassword("");
            return;
        }
        String site = MainActivity.siteSpinner.getSelectedItem().toString();
        Log.d("MainActivity", "site: " + site);
        String text = LineSegments.PostValue + site;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            if (MainActivity.isAddSpecialChars){
                if (MainActivity.specialChars != null && MainActivity.specialChars != "")
                {
                    sb.insert(2, MainActivity.specialChars);
                }
            }
            if (MainActivity.isAddUppercase){
                Log.d("MainActivity", "calling addUpperCase()");
                int firstLetterIndex = addUpperCase(sb.toString());
                Log.d("MainActivity", "firstLetterIndex : " + String.valueOf(firstLetterIndex));
                if (firstLetterIndex >= 0) {
                    // get the string, uppercase it, get the uppercased char at location
                    Log.d("MainActivity", "calling sb.setCharAt()");
                    Log.d("MainActivity", "value : " + String.valueOf(sb.toString().toUpperCase().charAt(firstLetterIndex)));
                    sb.setCharAt(firstLetterIndex, sb.toString().toUpperCase().charAt(firstLetterIndex));
                }
            }
            if (MainActivity.isMaxLength) {
                StringBuilder temp = new StringBuilder();
                temp.insert(0,sb.substring(0,MainActivity.maxLength));
                sb = temp;
            }
            Log.d("MainActivity", sb.toString());
            MainActivity.SetPassword( sb.toString());

            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) _context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", sb.toString());
            clipboard.setPrimaryClip(clip);

        }
        catch (NoSuchAlgorithmException nsa){

        }
    }

    private int addUpperCase(String sb){
        char [] entireString = new char[sb.length()-1];
        int indexCounter=0;
        sb.getChars(0,sb.length()-1,entireString,0);
        for (char c : entireString)
        {
            if (Character.isLetter(c))
            {
                return indexCounter;
            }
            indexCounter++;
        }
        return -1;
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
        if (userShape.size() > 0) {
            DrawHighlight(userShape.get(userShape.size()-1));
        }
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
