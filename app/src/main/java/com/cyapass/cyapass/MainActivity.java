package com.cyapass.cyapass;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
    private static Context appContext;
    private static TextView passwordText;
    private static String password;
    private static boolean isPwdVisible = true;
    public static boolean isAddUppercase = false;
    public static boolean isAddSpecialChars = false;
    public static boolean isMaxLength = false;
    public static String btCurrentDeviceName;
    public static boolean isSendCtrlAltDel = false;
    public static boolean isSendEnter = true;
    static String specialChars;
    static int maxLength = 32;
    Set<BluetoothDevice> pairedDevices;
    BluetoothAdapter btAdapter;
    ConnectThread ct;

    private static List<SiteKey> allSiteKeys;
    public static SiteKey currentSiteKey;
    TabLayout tabLayout;

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

        MainActivity.appContext = getApplicationContext();
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        FloatingActionButton sendFab = (FloatingActionButton)findViewById(R.id.sendFab);
        sendFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MainActivity.btCurrentDeviceName == ""){
                    return;
                }
                sendPasswordViaBT();
                if (isSendCtrlAltDel){
                    ct.writeCtrlAltDel();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Log.d("MainActivity", e.getMessage());
                    }
                }
                writeData();
            }
        });
    }

    /** Called before the activity is destroyed */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void sendPasswordViaBT(){

        if (btAdapter == null) {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (btAdapter != null) {
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        else
        {
            Log.d("MainActivity", "no bt adapter available");
            return; // cannot get btadapter
        }

        if (pairedDevices == null) {
            pairedDevices = btAdapter.getBondedDevices();
        }
        if (pairedDevices.size() > 0) {

            for (BluetoothDevice btItem : pairedDevices) {
                if (btItem != null) {
                    String name = btItem.getName();
                    if (name.equals(MainActivity.btCurrentDeviceName)) {
                        UUID uuid = btItem.getUuids()[0].getUuid();
                        Log.d("MainActivity", uuid.toString());
                        if (ct == null) {
                            ct = new ConnectThread(btItem, uuid, null);
                        }
                        ct.run(btAdapter);

                        return;
                    }
                }
            }
        }
    }

    private void writeData(){
        String clipText = readClipboard();
        if (isSendEnter){
            clipText += "\n";
        }
        Log.d("MainActivity", "on clipboard : " + clipText);
        if (!clipText.equals("")){
            ct.writeMessage(clipText);
            try {
                Thread.sleep(200);
                ct.cancel();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                ct = null;
            }
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

    public static void saveUserPrefValues(){
        SharedPreferences sites = MainActivity.appContext.getSharedPreferences("sites", MODE_PRIVATE);
        String outValues = sites.getString("sites", "");
        Log.d("MainActivity", sites.getString("sites", ""));
        SharedPreferences.Editor edit = sites.edit();

        Gson gson = new Gson();
        outValues = SiteKey.toJson(allSiteKeys);

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
        private static ArrayList<SiteKey> spinnerItems = new ArrayList<SiteKey>();
        private static ArrayAdapter<SiteKey> spinnerAdapter;
        private static View rootView;
        private static View settingsView;
        private CheckBox showPwdCheckBox;
        private Spinner siteSpinner;

        private static CheckBox addCharsTabCheckBox;
        private static CheckBox addUpperCaseTabCheckBox;
        private static CheckBox maxLengthTabCheckBox;
        private static EditText maxLengthTabEditText;
        // clearbutton seems to always work when the gv is NOT static.
        private GridView gv;
        private UserPath up;

        static CheckBox hidePatternCheckbox;

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

        public void loadCurrentDeviceName(){
            SharedPreferences devicePrefs = MainActivity.appContext.getSharedPreferences("deviceName", MODE_PRIVATE);
            MainActivity.btCurrentDeviceName = devicePrefs.getString("deviceName","");

        }

        public void saveDeviceNamePref(){
            SharedPreferences devicePrefs = appContext.getSharedPreferences("deviceName", MODE_PRIVATE);
            SharedPreferences.Editor edit = devicePrefs.edit();
            edit.putString("deviceName",MainActivity.btCurrentDeviceName);
            edit.commit();
            //PlaceholderFragment.loadSitesFromPrefs(v);
        }

        public static void loadSitesFromPrefs(View vx){
            Log.d("MainActivity", "Loading sites from preferences");

            SharedPreferences sitePrefs = MainActivity.appContext.getSharedPreferences("sites", MODE_PRIVATE);
            initializeSpinnerAdapter(vx);
            String sites = sitePrefs.getString("sites", "");
            Gson gson = new Gson();
            try {
                allSiteKeys = (List<SiteKey>)gson.fromJson(sites, new TypeToken<List<SiteKey>>(){}.getType());
                if (allSiteKeys == null){
                    allSiteKeys = new ArrayList<SiteKey>();
                }

                for (SiteKey sk : allSiteKeys){
                    spinnerAdapter.add(sk);
                }
                spinnerAdapter.sort(new Comparator<SiteKey>(){
                    public int compare(SiteKey a1, SiteKey a2) {
                        return a1.toString().compareToIgnoreCase(a2.toString());
                    }
                });
                spinnerAdapter.insert(new SiteKey("select site"),0);

                spinnerAdapter.notifyDataSetChanged();
            }
            catch (Exception x) {
                Log.d("MainActivity", x.getMessage());
                String[] allSites = sites.split(",");
                Log.d("MainActivity", "sites : " + sites);
                Log.d("MainActivity", "Reading items from prefs");
                for (String s : allSites) {
                    Log.d("MainActivity", "s : " + s);
                    if (s != "") {
                        spinnerAdapter.add(new SiteKey(s));
                    }
                }
            }
        }

        private static void initializeSpinnerAdapter(View v){
            if (spinnerAdapter == null) {
                spinnerAdapter = new ArrayAdapter<SiteKey>(v.getContext(), android.R.layout.simple_list_item_1, spinnerItems);
            }
            spinnerAdapter.clear();
            //spinnerAdapter.add(new SiteKey("select site"));
            spinnerAdapter.sort(new Comparator<SiteKey>(){
                public int compare(SiteKey a1, SiteKey a2) {
                    return a1.toString().compareToIgnoreCase(a2.toString());
                }
            });
            spinnerAdapter.notifyDataSetChanged();

        }

        private void setSettingsValues(){

            addUpperCaseTabCheckBox = (CheckBox)rootView.findViewById(R.id.addUCaseTabCheckBox);
            addCharsTabCheckBox = (CheckBox)rootView.findViewById(R.id.addCharsTabCheckBox);
            maxLengthTabCheckBox = (CheckBox)rootView.findViewById(R.id.maxLengthTabCheckBox);
            maxLengthTabEditText = (EditText)rootView.findViewById(R.id.maxLengthTabEditText);

            addCharsTabCheckBox.setChecked(currentSiteKey.isHasSpecialChars());
            addUpperCaseTabCheckBox.setChecked(currentSiteKey.isHasUpperCase());
            maxLengthTabCheckBox.setChecked(currentSiteKey.getMaxLength() > 0);
            if (currentSiteKey.getMaxLength() > 0) {
                maxLengthTabEditText.setText(String.valueOf(currentSiteKey.getMaxLength()));
            }
        }

        private void editSite(){
            LayoutInflater li = LayoutInflater.from(getContext());
            final View v = li.inflate(R.layout.sitelist_dialog_main, null);

            AlertDialog.Builder builder =
                    new AlertDialog.Builder(v.getContext());

            builder.setMessage( "Edit Site").setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            SharedPreferences sites = MainActivity.appContext.getSharedPreferences("sites", MODE_PRIVATE);
                            String outValues = sites.getString("sites", "");
                            Log.d("MainActivity", sites.getString("sites", ""));
                            SharedPreferences.Editor edit = sites.edit();

                            CheckBox ucCheckBox = (CheckBox)v.findViewById(R.id.addUppercaseCheckBox);
                            CheckBox specCharsCheckBox = (CheckBox)v.findViewById(R.id.addSpecialCharsCheckBox);
                            CheckBox maxLengthCheckBox = (CheckBox)v.findViewById(R.id.setMaxLengthCheckBox);
                            EditText maxLengthEditText = (EditText)v.findViewById(R.id.maxLengthEditText);

                            int originalLocation = allSiteKeys.indexOf(currentSiteKey);
                            Log.d("MainActivity", "originalLocation : " + String.valueOf(originalLocation));
                            allSiteKeys.remove(originalLocation);

                            EditText input = (EditText) v.findViewById(R.id.siteText);
                            String currentValue = input.getText().toString();

                            currentSiteKey = new SiteKey(currentValue,
                                    specCharsCheckBox.isChecked(),
                                    ucCheckBox.isChecked(),
                                    maxLengthCheckBox.isChecked(),
                                    maxLengthCheckBox.isChecked() ? Integer.parseInt(String.valueOf(maxLengthEditText.getText())) : 0);

                            allSiteKeys.add(originalLocation,currentSiteKey);
                            spinnerAdapter.notifyDataSetChanged();
                            Gson gson = new Gson();
                            outValues = gson.toJson(allSiteKeys, allSiteKeys.getClass());

                            edit.putString("sites", outValues);
                            edit.commit();
                            Log.d("MainActivity", "final outValues : " + outValues);
                            PlaceholderFragment.loadSitesFromPrefs(v);
                            siteSpinner.setSelection(findSiteSpinnerItemByText(currentValue),true);

                            setSettingsValues();
                        }
                    })
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });

            CheckBox ucCheckBox = (CheckBox)v.findViewById(R.id.addUppercaseCheckBox);
            CheckBox specCharsCheckBox = (CheckBox)v.findViewById(R.id.addSpecialCharsCheckBox);
            CheckBox maxLengthCheckBox = (CheckBox)v.findViewById(R.id.setMaxLengthCheckBox);
            EditText maxLengthEditText = (EditText)v.findViewById(R.id.maxLengthEditText);
            Log.d("MainActivity", "key 3 : " + String.valueOf(currentSiteKey.getKey()));
            Log.d("MainActivity", "maxLength 3 : " + String.valueOf(currentSiteKey.getMaxLength()));
            EditText input = (EditText) v.findViewById(R.id.siteText);
            input.setText(currentSiteKey.getKey());

            Log.d("MainActivity", "EDIT!");
            ucCheckBox.setChecked(currentSiteKey.isHasUpperCase());
            Log.d("MainActivity", "uppercase : " + Boolean.valueOf(currentSiteKey.isHasUpperCase()));
            specCharsCheckBox.setChecked(currentSiteKey.isHasSpecialChars());
            maxLengthCheckBox.setChecked(currentSiteKey.getMaxLength()>0);
            if (currentSiteKey.getMaxLength()>0){
                maxLengthEditText.setText(String.valueOf(currentSiteKey.getMaxLength()));
            }
            AlertDialog alert = builder.create();
            alert.setView(v);
            alert.show();
        }

        private void addNewSite(){

            LayoutInflater li = LayoutInflater.from(getContext());
            final View v = li.inflate(R.layout.sitelist_dialog_main, null);

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

                            CheckBox ucCheckBox = (CheckBox)v.findViewById(R.id.addUppercaseCheckBox);
                            CheckBox specCharsCheckBox = (CheckBox)v.findViewById(R.id.addSpecialCharsCheckBox);
                            CheckBox maxLengthCheckBox = (CheckBox)v.findViewById(R.id.setMaxLengthCheckBox);
                            EditText maxLengthEditText = (EditText)v.findViewById(R.id.maxLengthEditText);

                            //edit.clear();

                            EditText input = (EditText) v.findViewById(R.id.siteText);
                            String currentValue = input.getText().toString();

                            currentSiteKey = new SiteKey(currentValue,
                                    specCharsCheckBox.isChecked(),
                                    ucCheckBox.isChecked(),
                                    maxLengthCheckBox.isChecked(),
                                    maxLengthCheckBox.isChecked() ? Integer.parseInt(String.valueOf(maxLengthEditText.getText())) : 0);

                            allSiteKeys.add(currentSiteKey);
                            Gson gson = new Gson();
                            outValues = gson.toJson(allSiteKeys, allSiteKeys.getClass());
                            edit.putString("sites", outValues);
                            edit.commit();
                            Log.d("MainActivity", "final outValues : " + outValues);
                            PlaceholderFragment.loadSitesFromPrefs(v);

                            siteSpinner.setSelection(findSiteSpinnerItemByText(currentValue), true);

                            setSettingsValues();
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

        private int findSiteSpinnerItemByText(String currentSiteKey){
            Log.d("MainActivity", currentSiteKey);
            for (int x =0; x < spinnerAdapter.getCount();x++) {
                if (spinnerAdapter.getItem(x).getKey().equals(currentSiteKey)) {
                    Log.d("MainActivity", spinnerAdapter.getItem(x).getKey());
                    return x;
                }
            }
            return 0;
        }

        @Override
        public void onStart() {
            super.onStart();
            Log.d("MainActivity", "onStart : " + getArguments().getInt(ARG_SECTION_NUMBER));
            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                case 1: {
                    addUpperCaseTabCheckBox = (CheckBox)settingsView.findViewById(R.id.addUCaseTabCheckBox);
                    addCharsTabCheckBox = (CheckBox)settingsView.findViewById(R.id.addCharsTabCheckBox);
                    maxLengthTabCheckBox = (CheckBox)settingsView.findViewById(R.id.maxLengthTabCheckBox);
                    maxLengthTabEditText = (EditText)settingsView.findViewById(R.id.maxLengthTabEditText);
                    break;
                }
                case 2: {
                    addUpperCaseTabCheckBox = (CheckBox)rootView.findViewById(R.id.addUCaseTabCheckBox);
                    addCharsTabCheckBox = (CheckBox)rootView.findViewById(R.id.addCharsTabCheckBox);
                    maxLengthTabCheckBox = (CheckBox)rootView.findViewById(R.id.maxLengthTabCheckBox);
                    maxLengthTabEditText = (EditText)rootView.findViewById(R.id.maxLengthTabEditText);

                    break;
                }
            }
            if (currentSiteKey != null) {
                if (addUpperCaseTabCheckBox != null) {
                    addUpperCaseTabCheckBox.setChecked(currentSiteKey.isHasUpperCase());
                }
                if (addCharsTabCheckBox != null) {
                    addCharsTabCheckBox.setChecked(currentSiteKey.isHasSpecialChars());
                }
                if (maxLengthTabCheckBox != null) {
                    maxLengthTabCheckBox.setChecked(currentSiteKey.getMaxLength() > 0);
                }
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            if (gv != null){
                Log.d("MainActivity", "app is pausing");
                up = gv.getUserPath();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            if (gv == null) {
                gv = new GridView(appContext);
            }
            if (up!=null) {
                gv.setUserPath(up);
            }
            Log.d("MainActivity", "onResume : " + getArguments().getInt(ARG_SECTION_NUMBER));
            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                case 1: {
                    addUpperCaseTabCheckBox = (CheckBox)settingsView.findViewById(R.id.addUCaseTabCheckBox);
                    addCharsTabCheckBox = (CheckBox)settingsView.findViewById(R.id.addCharsTabCheckBox);
                    maxLengthTabCheckBox = (CheckBox)settingsView.findViewById(R.id.maxLengthTabCheckBox);
                    maxLengthTabEditText = (EditText)settingsView.findViewById(R.id.maxLengthTabEditText);
                    break;
                }
                case 2: {
                    addUpperCaseTabCheckBox = (CheckBox)rootView.findViewById(R.id.addUCaseTabCheckBox);
                    addCharsTabCheckBox = (CheckBox)rootView.findViewById(R.id.addCharsTabCheckBox);
                    maxLengthTabCheckBox = (CheckBox)rootView.findViewById(R.id.maxLengthTabCheckBox);
                    maxLengthTabEditText = (EditText)rootView.findViewById(R.id.maxLengthTabEditText);

                    break;
                }
            }
            if (currentSiteKey != null) {
                if (addUpperCaseTabCheckBox != null) {
                    addUpperCaseTabCheckBox.setChecked(currentSiteKey.isHasUpperCase());
                }
                if (addCharsTabCheckBox != null) {
                    addCharsTabCheckBox.setChecked(currentSiteKey.isHasSpecialChars());
                }
                if (maxLengthTabCheckBox != null) {
                    maxLengthTabCheckBox.setChecked(currentSiteKey.getMaxLength() > 0);
                }
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            loadCurrentDeviceName();

            //final GridView gv = new us.raddev.com.cyapass.cyapass.GridView(rootView.getContext());
            gv = new com.cyapass.cyapass.GridView(appContext);

            rootView = inflater.inflate(R.layout.fragment_main, container, false);

            //rootView.setWillNotDraw(false);
            LinearLayout mainlayout1 = (LinearLayout) rootView.findViewById(R.id.drawcross);
            mainlayout1.addView(gv,gv.cellSize*7,gv.cellSize*7);
            //container.setWillNotDraw(false);

            Button clearGridButton;
            final Spinner btDeviceSpinner;

            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                case 1: {
                    passwordText = (TextView) rootView.findViewById(R.id.password);
                    siteSpinner = (Spinner)rootView.findViewById(R.id.siteSpinner);
                    showPwdCheckBox = (CheckBox)rootView.findViewById(R.id.showPwd);
                    showPwdCheckBox.setChecked(true);
                    clearGridButton = (Button)rootView.findViewById(R.id.clearGrid);
                    Button deleteSiteButton = (Button) rootView.findViewById(R.id.deleteSite);
                    Button addSiteButton = (Button) rootView.findViewById(R.id.addSite);
                    loadSitesFromPrefs(rootView);
                    siteSpinner.setAdapter(spinnerAdapter);

                    settingsView = inflater.inflate(R.layout.fragment_settings, container, false);
                    addUpperCaseTabCheckBox = (CheckBox)settingsView.findViewById(R.id.addUCaseTabCheckBox);
                    addCharsTabCheckBox = (CheckBox)settingsView.findViewById(R.id.addCharsTabCheckBox);
                    maxLengthTabCheckBox = (CheckBox)settingsView.findViewById(R.id.maxLengthTabCheckBox);
                    maxLengthTabEditText = (EditText)settingsView.findViewById(R.id.maxLengthTabEditText);
                    hidePatternCheckbox = (CheckBox)rootView.findViewById(R.id.hidePatternCheckBox);
                    loadSitesFromPrefs(rootView);

                    addSiteButton.requestFocus();

                    siteSpinner.setOnLongClickListener(new View.OnLongClickListener() {
                        public boolean onLongClick(View arg0) {
                            currentSiteKey = (SiteKey)siteSpinner.getSelectedItem();
                            Log.d("MainActivity", "key 2 : " + String.valueOf(currentSiteKey.getKey()));
                            Log.d("MainActivity", "maxLength 2 : " + String.valueOf(currentSiteKey.getMaxLength()));
                            if (currentSiteKey.getKey().equals("select site")){return false;}
                            Log.d("MainActivity", "LONGCLICK!!!");
                            Log.d("MainActivity", currentSiteKey.getKey());
                            editSite();
                            return true;
                        }
                    });

                    siteSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                            if (siteSpinner.getSelectedItemPosition() <= 0){
                                currentSiteKey = null;
                                gv.ClearGrid();
                                gv.invalidate();

                                passwordText.setText("");
                                password = "";

                                clearClipboard();


                                return;
                            }

                            currentSiteKey = (SiteKey)siteSpinner.getSelectedItem();
                            Log.d("MainActivity", "key 1 : " + String.valueOf(currentSiteKey.getKey()));
                            Log.d("MainActivity", "maxLength 1 : " + String.valueOf(currentSiteKey.getMaxLength()));
                            addCharsTabCheckBox.setChecked(currentSiteKey.isHasSpecialChars());
                            addUpperCaseTabCheckBox.setChecked(currentSiteKey.isHasUpperCase());
                            if (currentSiteKey.getMaxLength() > 0) {
                                maxLengthTabEditText.setText(String.valueOf(currentSiteKey.getMaxLength()));
                            }
                            maxLengthTabCheckBox.setChecked(currentSiteKey.getMaxLength() > 0);
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
                            gv.setPatternHidden(false);
                            if (hidePatternCheckbox != null){
                                hidePatternCheckbox.setChecked(false);
                            }

                            gv.ClearGrid();
                            up = null;
                            gv.invalidate();
                            password = "";
                            passwordText.setText("");
                            clearClipboard();
                        }
                    });

                    addSiteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            addNewSite();
                        }
                    });

                    hidePatternCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            Log.d("MainActivity", "hidePatternCheckbox isChecked : " + String.valueOf(isChecked));
                            if (isChecked){
                                gv.setPatternHidden(true);
                                gv.ClearGrid();
                                gv.invalidate();
                            }
                            else{
                                gv.setPatternHidden(false);
                                gv.invalidate();
                            }

                        }
                    });

                    deleteSiteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (currentSiteKey == null){
                                return; // add message box need to select a valid site
                            }
                            else
                            {
                                new AlertDialog.Builder(view.getContext())
                                        .setTitle("Delete site?")
                                        .setMessage("Are you sure you want to delete this site : " + currentSiteKey.getKey() + "?")
                                        .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {

                                                spinnerItems.remove(currentSiteKey);
                                                spinnerAdapter.notifyDataSetChanged();
                                                allSiteKeys.remove(currentSiteKey);
                                                MainActivity.clearAllUserPrefs();
                                                saveUserPrefValues();

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

                    final ListView logView;
                    ArrayList<String> listViewItems = new ArrayList<String>();

                    ArrayList<String> logViewItems = new ArrayList<String>();
                    final ArrayAdapter<String> logViewAdapter;

                    final BluetoothAdapter btAdapter;
                    final CheckBox sendCtrlAltDelCheckbox;
                    final CheckBox sendEnterCheckbox;


                    final EditText outText;
                    EditText specialCharsText;

                    btDeviceSpinner = (Spinner) rootView.findViewById(R.id.btDevice);
                    logView = (ListView) rootView.findViewById(R.id.logView);

                    addUpperCaseTabCheckBox = (CheckBox)rootView.findViewById(R.id.addUCaseTabCheckBox);
                    addCharsTabCheckBox = (CheckBox)rootView.findViewById(R.id.addCharsTabCheckBox);
                    maxLengthTabCheckBox = (CheckBox)rootView.findViewById(R.id.maxLengthTabCheckBox);
                    maxLengthTabEditText = (EditText)rootView.findViewById(R.id.maxLengthTabEditText);
                    specialCharsText = (EditText)rootView.findViewById(R.id.specialCharsTabTextBox);
                    sendCtrlAltDelCheckbox = (CheckBox)rootView.findViewById(R.id.sendCtrlAltDel);
                    sendEnterCheckbox = (CheckBox)rootView.findViewById(R.id.sendEnter);

                    sendEnterCheckbox.setChecked(true);
                    maxLengthTabEditText.setText("32");
                    addUpperCaseTabCheckBox.requestFocus();

                    adapter = new ArrayAdapter<String>(rootView.getContext(), android.R.layout.simple_list_item_1, listViewItems);
                    btDeviceSpinner.setAdapter(adapter);

                    logViewAdapter = new ArrayAdapter<String>(rootView.getContext(), android.R.layout.simple_list_item_1, logViewItems);
                    logView.setAdapter(logViewAdapter);

                    btAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (btAdapter != null) {
                        if (!btAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        }
                        pairedDevices = GetPairedDevices(btAdapter);
                        //DiscoverAvailableDevices();
                    }


                sendEnterCheckbox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (sendEnterCheckbox.isChecked()){
                            MainActivity.isSendEnter = true;
                        }
                        else{
                            MainActivity.isSendEnter = false;
                        }
                    }
                });

                sendCtrlAltDelCheckbox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (sendCtrlAltDelCheckbox.isChecked()){
                            MainActivity.isSendCtrlAltDel = true;
                        }
                        else{
                            MainActivity.isSendCtrlAltDel = false;
                        }
                    }
                });

                    addUpperCaseTabCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked){
                                currentSiteKey.setHasUpperCase(true);
                            }
                            else{
                                currentSiteKey.setHasUpperCase(false);
                            }
                            if (gv.isLineSegmentComplete()){
                                Log.d("MainActivity", "addChars -- Re-generating password...");
                                gv.GeneratePassword();
                            }
                        }
                    });

                    addCharsTabCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked){
                            currentSiteKey.setHasSpecialChars(true);
                        }
                        else{
                            currentSiteKey.setHasSpecialChars(false);
                        }
                        if (gv.isLineSegmentComplete()){
                            Log.d("MainActivity", "addChars -- Re-generating password...");
                            gv.GeneratePassword();
                        }
                    }
                });

                maxLengthTabCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (currentSiteKey == null){return;}
                        if (isChecked){
                            currentSiteKey.setMaxLength(Integer.parseInt(maxLengthTabEditText.getText().toString()));
                        }
                        else{
                            currentSiteKey.setMaxLength(0);
                        }
                        if (gv.isLineSegmentComplete()){
                            Log.d("MainActivity", "addChars -- Re-generating password...");
                            gv.GeneratePassword();
                        }
                    }
                });

                    specialCharsText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void afterTextChanged(Editable s) {
                            if (currentSiteKey == null){return;}
                            MainActivity.specialChars = s.toString();
                            if (currentSiteKey.isHasSpecialChars()){
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
                    maxLengthTabEditText.addTextChangedListener(new TextWatcher() {
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

                    btDeviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                            btCurrentDeviceName = String.valueOf(btDeviceSpinner.getSelectedItem());
                            saveDeviceNamePref();
                            Log.d("MainActivity", "DeviceInfo : " + btCurrentDeviceName);
                            logViewAdapter.add("DeviceInfo : " + btCurrentDeviceName);
                            logViewAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                            // your code here
                        }
                    });

                    InitializeDeviceSpinner(btDeviceSpinner);

                    break;
                }
            }

            return rootView;
        }

        public  void InitializeDeviceSpinner(Spinner btDeviceSpinner){
            if (btCurrentDeviceName != null && btCurrentDeviceName != ""){
                int counter = 0;
                for (; counter <  adapter.getCount();counter++)
                {
                    Log.d("MainActivity", "adapter.getItem : " + adapter.getItem(counter).toString());
                    if (String.valueOf(adapter.getItem(counter)).equals( btCurrentDeviceName)){
                        break;
                    }
                }
                btDeviceSpinner.setSelection(counter);
            }
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
