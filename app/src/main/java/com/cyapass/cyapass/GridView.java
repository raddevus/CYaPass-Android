package com.cyapass.cyapass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.nio.charset.Charset;
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
    private Canvas xCanvas;
    public int viewWidth;
    public int viewHeight;
    private Point currentPoint;
    private static UserPath us = new UserPath();
    int hitTestIdx;
    private static boolean isPatternHidden;

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
                if (isPatternHidden) {return true;}
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    int touchX = (int)event.getX();
                    int touchY = (int)event.getY();
                    String output = "Touch coordinates : " +
                            String.valueOf(touchX) + "x" + String.valueOf(touchY);
                    //Toast.makeText(v.getContext(), output, Toast.LENGTH_SHORT).show();
                    currentPoint = new Point(touchX,touchY);
                    if (SelectNewPoint(currentPoint)) {
                        v.invalidate();
                        us.CalculateGeometricValue();
                        GeneratePassword();
                    }
                }
                return true;
            }
        });
    }

    public void ClearGrid(){
        if (!isPatternHidden) {
            us = new UserPath();
        }
        invalidate();
        vx.invalidate();
    }

    public boolean isLineSegmentComplete(){
        //if (us == null || us.allSegments == null){return false;}
        Log.d("MainActivity", "size ; " + String.valueOf(us.allSegments.size()));
        Log.d("MainActivity", "size ; " + String.valueOf(this.us.allSegments.size()));
        Log.d("MainActivity", "size ; " + String.valueOf(us.allPoints.size()));
        return Boolean.valueOf(us.allSegments.size() > 0);
    }

    private void DrawUserShape(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(8);
        paint.setStyle(Paint.Style.STROKE);

        for (Segment s : us.allSegments) {
            canvas.drawCircle((float) s.Begin.x,
                    (float) s.Begin.y,
                     highlightOffset, paint);
            canvas.drawLine(s.Begin.x, s.Begin.y,
                    s.End.x,
                    s.End.y, paint);
//            Log.d("MainActivity", "DONE drawing line...");
        }

    }

    private void DrawHighlight(Point p){
        Log.d("MainActivity", "DrawHighlight()...");
        Log.d("MainActivity", p.toString());
        Paint paint = new Paint();
        if (us.allPoints.size() == 1) {
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
        Point currentPoint = HitTest(new Point(p.x, p.y));
        if (currentPoint == null)
        {
            return false;
        }
        us.append(currentPoint,hitTestIdx + (hitTestIdx * (hitTestIdx / 6) * 10));
        us.CalculateGeometricValue();

        return true;
    }

    public void GeneratePassword(){CreateHash();}

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
        if (MainActivity.currentSiteKey == null){
            MainActivity.SetPassword("");
            return;
        }
        if (!isLineSegmentComplete()){
            MainActivity.SetPassword("");
            return;
        }
        SiteKey currentSiteKey = MainActivity.currentSiteKey;
        Log.d("MainActivity", "site: " + currentSiteKey.toString());
        String text = us.PointValue + currentSiteKey.toString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            if (currentSiteKey.isHasSpecialChars()){
                // yes, I still get the special chars from what the user typed on the form
                // because I don't store special chars in JSON as a protection
                if (MainActivity.specialChars != null && MainActivity.specialChars != "")
                {
                    sb.insert(2, MainActivity.specialChars);
                }
            }
            if (currentSiteKey.isHasUpperCase()){
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
            if (currentSiteKey.getMaxLength()>0) {
                StringBuilder temp = new StringBuilder();
                temp.insert(0,sb.substring(0,currentSiteKey.getMaxLength()));
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
        if (!isPatternHidden) {
            DrawUserShape(canvas);
            if (us.allPoints.size() > 0) {
                DrawHighlight(us.allPoints.get(0));
            }
        }
    }

    @Override
    protected void dispatchDraw (Canvas canvas){
        this.xCanvas = canvas;
        super.dispatchDraw(canvas);

        DrawPosts();
        DrawGridLines();
        if (!isPatternHidden) {
            DrawUserShape(canvas);
            if (us.allPoints.size() > 0) {
                DrawHighlight(us.allPoints.get(0));
            }
        }
    }

    private Point HitTest(Point p)
    {
        int loopcount = 0;
        hitTestIdx = 0;
        for (Point Pt : _allPosts)
        {
            if ((p.x >= (Pt.x ) - (postWidth*2)) && (p.x <= (Pt.x) + (postWidth*2)))
            {
                if ((p.y >= (Pt.y ) - (postWidth*2)) && (p.y <= (Pt.y) + (postWidth*2)))
                {
                    //String output = String.format("it's a hit: %d %d",p.x,p.y);
                    //Toast.makeText(this.getContext(), output, Toast.LENGTH_SHORT).show();
                    hitTestIdx = loopcount;
                    return Pt;
                }
            }
            loopcount++;
        }

        return null;
    }

    public boolean isPatternHidden() {
        return isPatternHidden;
    }

    public void setPatternHidden(boolean patternHidden) {
        isPatternHidden = patternHidden;
    }
}
