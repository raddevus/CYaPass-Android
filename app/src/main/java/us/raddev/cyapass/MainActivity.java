package us.raddev.cyapass;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    static int REQUEST_ENABLE_BT = 1;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private AdView mAdView;
    private static Context appContext;
    private static TextView passwordText;
    private static String password;
    public static Spinner siteSpinner;
    private static boolean isPwdVisible = true;
    public static boolean isAddUppercase = false;
    public static boolean isAddSpecialChars = false;
    public static boolean isMaxLength = false;
    static String specialChars;
    static int maxLength = 25;

    private LinearLayout layout1;

    private void clearClipboard(){
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) appContext.getSystemService(Context.CLIPBOARD_SERVICE);
        //android.content.ClipData clip = android.content.ClipData.newPlainText("", "");
        android.content.ClipData clip = android.content.ClipData.newPlainText(null,null);
        clipboard.setPrimaryClip(clip);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(getApplicationContext(), getResources().getString(R.string.banner_ad_unit_id));

        // Gets the ad view defined in layout/ad_fragment.xml with ad unit ID set in
        // values/strings.xml.
        mAdView = (AdView) findViewById(R.id.ad_view);

        // THIS IS THE CODE FOR PROD BUILDS WITH ADMOB
        /* AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest); */

        // Create an ad request. Check your logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();

        // Start loading the ad in the background.
        mAdView.loadAd(adRequest);

        MainActivity.appContext = getApplicationContext();
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        FloatingActionButton sendFab = (FloatingActionButton)findViewById(R.id.sendFab);
        sendFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendPasswordViaBT();
            }
        });
    }

    /** Called before the activity is destroyed */
    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    private void sendPasswordViaBT(){
        ConnectThread ct = null;
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice btItem : pairedDevices) {
                if (btItem != null) {
                    String name = btItem.getName();
                    if (name.equals("RADBluex")) {
                        UUID uuid = btItem.getUuids()[0].getUuid();
                        Log.d("MainActivity", uuid.toString());
                        if (ct == null) {
                            ct = new ConnectThread(btItem, uuid, null);
                        }
                        ct.run(btAdapter);
                        break;
                    }
                }
            }
        }

        String clipText = readClipboard();
        Log.d("MainActivity", "on clipboard : " + clipText);
        if (clipText != ""){
            ct.writeMessage(clipText);
            ct.cancel();
        }

    }

    private String readClipboard() {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) appContext.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = clipboard.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            ClipData.Item item = clip.getItemAt(clip.getItemCount() - 1);
            return item.getText().toString();
        }
        return "";

    }

    public static void addUserPrefValue(String currentValue){
        SharedPreferences sites = MainActivity.appContext.getSharedPreferences("sites", MODE_PRIVATE);
        String outValues = sites.getString("sites", "");
        Log.d("MainActivity", sites.getString("sites", ""));
        SharedPreferences.Editor edit = sites.edit();

        if (outValues != "") {
            outValues += "," + currentValue;
        } else {
            outValues += currentValue;

        }
        edit.putString("sites", outValues);
        edit.commit();
        Log.d("MainActivity", "final outValues : " + outValues);

    }

    public static void clearAllUserPrefs(){
        SharedPreferences sites = appContext.getSharedPreferences("sites", MODE_PRIVATE);
        SharedPreferences.Editor edit = sites.edit();
        edit.clear();
        edit.commit();
        //PlaceholderFragment.loadSitesFromPrefs(v);
    }

    public  void DiscoverAvailableDevices(final ArrayAdapter<String>adapter, final ArrayAdapter<BluetoothDevice>otherDevices){
        final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    //btDevice = device;
                    adapter.add(device.getName());// + "\n" + device.getAddress());
                    otherDevices.add(device);
                    adapter.notifyDataSetChanged();
                }
            }
        };
// Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public static void SetPassword(String pwd){
        PlaceholderFragment.password = pwd;
        if (isPwdVisible) {
            passwordText.setText(pwd);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        final static int REQUEST_ENABLE_BT = 1;
        private static ArrayAdapter<String> adapter;
        private ArrayList<BluetoothDevice> otherDevices = new ArrayList<BluetoothDevice>();
        private ConnectThread ct;
        private Set<BluetoothDevice> pairedDevices;
        private static String password;
        private static ArrayList<SiteInfo> spinnerItems = new ArrayList<SiteInfo>();
        private static ArrayAdapter<SiteInfo> spinnerAdapter;


        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }


        private Set<BluetoothDevice> GetPairedDevices(BluetoothAdapter btAdapter) {

            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    adapter.add(device.getName());// + "\n" + device.getAddress());
                }
                adapter.notifyDataSetChanged();
            }
            return pairedDevices;
        }

        private void clearClipboard(){
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            //android.content.ClipData clip = android.content.ClipData.newPlainText("", "");
            android.content.ClipData clip = android.content.ClipData.newPlainText(null,null);
            clipboard.setPrimaryClip(clip);
        }

        private String readClipboard() {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                ClipData.Item item = clip.getItemAt(clip.getItemCount() - 1);
                return item.getText().toString();
            }
            return "";
        }

        public static void loadSitesFromPrefs(View vx){
            Log.d("MainActivity", "Loading sites from preferences");

            SharedPreferences sitePrefs = MainActivity.appContext.getSharedPreferences("sites", MODE_PRIVATE);
            initializeSpinnerAdapter(vx);
            String sites = sitePrefs.getString("sites", "");

            String[] allSites = sites.split(",");
            Log.d("MainActivity", "sites : " + sites);
            Log.d("MainActivity", "Reading items from prefs");
            for (String s : allSites){
                Log.d("MainActivity", "s : " + s);
                if (s != "") {
                    spinnerAdapter.add(new SiteInfo(s));
                }
            }
            spinnerAdapter.notifyDataSetChanged();
        }

        private static void initializeSpinnerAdapter(View v){
            if (spinnerAdapter == null) {
                spinnerAdapter = new ArrayAdapter<SiteInfo>(v.getContext(), android.R.layout.simple_list_item_1, spinnerItems);
            }
            spinnerAdapter.clear();
            spinnerAdapter.add(new SiteInfo ("select site"));
            spinnerAdapter.sort(new Comparator<SiteInfo>(){
                public int compare(SiteInfo a1, SiteInfo a2) {
                    return a1.toString().compareToIgnoreCase(a2.toString());
                }
            });
            spinnerAdapter.notifyDataSetChanged();

        }

        private void addNewSite(int id){

            LayoutInflater li = LayoutInflater.from(getContext());
            final View v = li.inflate(R.layout.sitelist_main, null);

            AlertDialog.Builder builder =
                    new AlertDialog.Builder(v.getContext());

            builder.setMessage( "Add new site").setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            SharedPreferences sites = MainActivity.appContext.getSharedPreferences("sites", MODE_PRIVATE);
                            String outValues = sites.getString("sites", "");
                            Log.d("MainActivity", sites.getString("sites", ""));
                            SharedPreferences.Editor edit = sites.edit();

                            //edit.clear();

                            EditText input = (EditText) v.findViewById(R.id.siteText);
                            String currentValue = input.getText().toString();
                            if (currentValue != "") {
                                if (outValues != "") {
                                    outValues += "," + currentValue;
                                } else {
                                    outValues += currentValue;
                                }
                            }
                            edit.putString("sites", outValues);
                            edit.commit();
                            Log.d("MainActivity", "final outValues : " + outValues);
                            PlaceholderFragment.loadSitesFromPrefs(v);
                            siteSpinner.setSelection(siteSpinner.getCount()-1, true);
                        }
                    })
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });
            AlertDialog alert = builder.create();
            alert.setView(v);
            alert.show();
        }



        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {


            View rootView = null;
            //final GridView gv = new us.raddev.cyapass.GridView(rootView.getContext());
            final GridView gv = new us.raddev.cyapass.GridView(appContext);

            rootView = inflater.inflate(R.layout.fragment_main, container, false);

            //rootView.setWillNotDraw(false);
            LinearLayout mainlayout1 = (LinearLayout) rootView.findViewById(R.id.drawcross);
            mainlayout1.addView(gv,gv.cellSize*7,gv.cellSize*7);
            //container.setWillNotDraw(false);

            Button clearGridButton;

            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                case 1: {

                    passwordText = (TextView) rootView.findViewById(R.id.password);
                    siteSpinner = (Spinner)rootView.findViewById(R.id.siteSpinner);
                    final CheckBox showPwdCheckBox = (CheckBox)rootView.findViewById(R.id.showPwd);
                    showPwdCheckBox.setChecked(true);
                    clearGridButton = (Button)rootView.findViewById(R.id.clearGrid);
                    Button deleteSiteButton = (Button) rootView.findViewById(R.id.deleteSite);
                    Button addSiteButton = (Button) rootView.findViewById(R.id.addSite);
                    loadSitesFromPrefs(rootView);
                    siteSpinner.setAdapter(spinnerAdapter);

                    loadSitesFromPrefs(rootView);

                    addSiteButton.requestFocus();

                    siteSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                            if (siteSpinner.getSelectedItemPosition() <= 0){
                                gv.ClearGrid();
                                gv.invalidate();

                                passwordText.setText("");
                                password = "";

                                clearClipboard();

                                return;
                            }

                            if (gv.isLineSegmentComplete()){
                                gv.GeneratePassword();
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                            // your code here
                        }

                    });

                    showPwdCheckBox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (showPwdCheckBox.isChecked())
                            {
                                passwordText.setVisibility(View.VISIBLE);
                                passwordText.setText(password);
                                Log.d("MainActivity","password : " + password);
                                isPwdVisible = true;
                            }
                            else{
                                passwordText.setText("");
                                passwordText.setVisibility(View.INVISIBLE);
                                isPwdVisible = false;
                            }
                        }
                    });

                    clearGridButton.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            gv.ClearGrid();
                            gv.invalidate();
                            password = "";
                            passwordText.setText("");
                            clearClipboard();
                        }
                    });

                    addSiteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            addNewSite(R.id.siteText);
                        }
                    });

                    deleteSiteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (MainActivity.siteSpinner.getSelectedItemPosition() <= 0){
                                return; // add message box need to select a valid site
                            }
                            else
                            {
                                new AlertDialog.Builder(view.getContext())
                                        .setTitle("Delete site?")
                                        .setMessage("Are you sure you want to delete this site?")
                                        .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                spinnerItems.remove(siteSpinner.getSelectedItemPosition());
                                                spinnerAdapter.notifyDataSetChanged();
                                                MainActivity.clearAllUserPrefs();
                                                int siteCounter = 0;
                                                for (SiteInfo s : spinnerItems){
                                                    // siteCounter insures we do not add the empty
                                                    // site item to the user prefs
                                                    if (siteCounter > 0) {
                                                        MainActivity.addUserPrefValue(s.toString());
                                                    }
                                                    siteCounter++;
                                                }
                                                siteSpinner.setSelection(0,true);
                                            }
                                        })
                                        .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                // do nothing
                                            }
                                        })
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .show();
                            }
                        }
                    });
                    break;
                }
                case 2:
                {
                    rootView = inflater.inflate(R.layout.fragment_settings, container, false);

                    final ListView listView;
                    final ListView logView;
                    ArrayList<String> listViewItems = new ArrayList<String>();

                    ArrayList<String> logViewItems = new ArrayList<String>();
                    final ArrayAdapter<String> logViewAdapter;

                    final BluetoothAdapter btAdapter;


                    Button yesButton;
                    Button noButton;
                    Button sendButton;

                    final CheckBox addUpperCaseCheckBox;
                    final CheckBox addCharsCheckBox;
                    final CheckBox maxLengthCheckBox;
                    final EditText outText;
                    EditText specialCharsText;
                    EditText maxLengthText;

                    listView = (ListView) rootView.findViewById(R.id.mainListView);
                    logView = (ListView) rootView.findViewById(R.id.logView);
                    yesButton = (Button)rootView.findViewById(R.id.YesButton);
                    noButton = (Button)rootView.findViewById(R.id.NoButton);
                    sendButton = (Button)rootView.findViewById(R.id.sendButton);

                    outText = (EditText)rootView.findViewById(R.id.outText);
                    addUpperCaseCheckBox = (CheckBox)rootView.findViewById(R.id.addUCaseCheckBox);
                    addCharsCheckBox = (CheckBox)rootView.findViewById(R.id.addCharsCheckBox);
                    maxLengthCheckBox = (CheckBox)rootView.findViewById(R.id.maxLengthCheckBox);
                    maxLengthText = (EditText)rootView.findViewById(R.id.maxLengthEditText);
                    specialCharsText = (EditText)rootView.findViewById(R.id.specialCharsTextBox);

                    maxLengthText.setText("25");
                    addUpperCaseCheckBox.requestFocus();

                    adapter = new ArrayAdapter<String>(rootView.getContext(), android.R.layout.simple_list_item_1, listViewItems);
                    listView.setAdapter(adapter);

                    logViewAdapter = new ArrayAdapter<String>(rootView.getContext(), android.R.layout.simple_list_item_1, logViewItems);
                    logView.setAdapter(logViewAdapter);

                    adapter.notifyDataSetChanged();

                    btAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (btAdapter != null) {
                        if (!btAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        }
                        pairedDevices = GetPairedDevices(btAdapter);
                        //DiscoverAvailableDevices();
                    }

                    addUpperCaseCheckBox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (addUpperCaseCheckBox.isChecked()){
                                MainActivity.isAddUppercase = true;
                            }
                            else{
                                MainActivity.isAddUppercase = false;
                            }

                            if (gv.isLineSegmentComplete()){
                                Log.d("MainActivity", "add uppercase -- Re-generating password...");
                                gv.GeneratePassword();
                            }
                        }
                    });

                    addCharsCheckBox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (addCharsCheckBox.isChecked()){
                                MainActivity.isAddSpecialChars = true;
                            }
                            else{
                                MainActivity.isAddSpecialChars = false;
                            }

                            if (gv.isLineSegmentComplete()){
                                Log.d("MainActivity", "addChars -- Re-generating password...");
                                gv.GeneratePassword();
                            }
                        }
                    });

                    maxLengthCheckBox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (maxLengthCheckBox.isChecked()){
                                MainActivity.isMaxLength = true;
                            }
                            else{
                                MainActivity.isMaxLength = false;
                            }

                            if (gv.isLineSegmentComplete()){
                                Log.d("MainActivity", "set maxLength -- Re-generating password...");
                                gv.GeneratePassword();
                            }
                        }
                    });

                    noButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ct.writeNo();
                        }
                    });

                    yesButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ct.writeYes();
                        }
                    });

                    specialCharsText.addTextChangedListener(new TextWatcher() {

                        @Override
                        public void afterTextChanged(Editable s) {
                            MainActivity.specialChars = s.toString();
                            if (MainActivity.isAddSpecialChars){
                                gv.GeneratePassword();
                            }
                        }

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                    });
                    maxLengthText.addTextChangedListener(new TextWatcher() {

                        @Override
                        public void afterTextChanged(Editable s) {
                            if (s != null) {
                                if (s.length() > 0) {
                                    MainActivity.maxLength = Integer.parseInt(s.toString());
                                    if (isMaxLength){
                                        gv.GeneratePassword();
                                    }
                                }

                            }
                        }

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                    });

                    sendButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String clipText = readClipboard();
                            Log.d("MainActivity", "on clipboard : " + clipText);
                            if (outText.getText().toString() == ""){
                                ct.writeMessage(clipText);
                            }
                            else {
                                ct.writeMessage(outText.getText().toString());
                            }
                        }
                    });

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        public void onItemClick(AdapterView<?> parent, View view,
                                                int position, long id) {
                            Log.d("MainActivity", "item clicked");

                            Object o = listView.getItemAtPosition(position);
                            String btDeviceInfo=(String)o;
                            Log.d("MainActivity", "DeviceInfo : " + btDeviceInfo);
                            logViewAdapter.add("DeviceInfo : " + btDeviceInfo);
                            logViewAdapter.notifyDataSetChanged();

                            if (pairedDevices.size() > 0) {
                                for (BluetoothDevice btItem : pairedDevices) {
                                    if (btItem != null) {
                                        logViewAdapter.add("btItem is good!");
                                        logViewAdapter.notifyDataSetChanged();
                                        String name = btItem.getName();
                                        logViewAdapter.add(name);
                                        logViewAdapter.notifyDataSetChanged();
                                        if (name.equals(btDeviceInfo)) {
                                            UUID uuid = btItem.getUuids()[0].getUuid();
                                            Log.d("MainActivity", uuid.toString());
                                            logViewAdapter.add("UUID : " + uuid.toString());
                                            logViewAdapter.notifyDataSetChanged();
                                            if (ct == null) {
                                                ct = new ConnectThread(btItem, uuid, logViewAdapter);
                                            }
                                            ct.run(btAdapter);
                                            return;
                                        }
                                    }
                                }
                            }
                            else {
                                logViewAdapter.add("btDevice is null");
                                logViewAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                    break;
                }
            }

            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "MAIN";
                case 1:
                    return "SETTINGS";
            }
            return null;
        }
    }
}
