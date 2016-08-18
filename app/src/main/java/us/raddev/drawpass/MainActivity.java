package us.raddev.drawpass;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.provider.CalendarContract;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
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
import java.util.Set;
import java.util.UUID;

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

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

/*        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        }); */

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

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = null;
            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                case 1: {
                    rootView = inflater.inflate(R.layout.fragment_main, container, false);
                    final GridView gv = new us.raddev.drawpass.GridView(rootView.getContext());
                    LinearLayout mainlayout1 = (LinearLayout) rootView.findViewById(R.id.drawcross);
                    mainlayout1.addView(gv,gv.cellSize*7,gv.cellSize*7);
                    passwordText = (TextView) rootView.findViewById(R.id.password);
                    siteSpinner = (Spinner)rootView.findViewById(R.id.siteSpinner);
                    final CheckBox showPwdCheckBox = (CheckBox)rootView.findViewById(R.id.showPwd);
                    showPwdCheckBox.setChecked(true);
                    final Button clearGridButton = (Button) rootView.findViewById(R.id.clearGridButton);
                    Button genPasswordButton = (Button) rootView.findViewById(R.id.genPasswordButton);

                    ArrayList<SiteInfo> spinnerItems = new ArrayList<SiteInfo>();
                    final ArrayAdapter<SiteInfo> spinnerAdapter = new ArrayAdapter<SiteInfo>(getContext(), android.R.layout.simple_list_item_1, spinnerItems);
                    siteSpinner.setAdapter(spinnerAdapter);
                    spinnerAdapter.add(new SiteInfo ("[--choose site--]"));
                    spinnerAdapter.add(new SiteInfo("amazon"));
                    spinnerAdapter.add(new SiteInfo("computer"));
                    spinnerAdapter.notifyDataSetChanged();

                    clearGridButton.dckListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            gv.ClearGrid();
                            passwordText.setText("");
                            clearClipboard();
                            gv.invalidate();
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

                    genPasswordButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (MainActivity.siteSpinner.getSelectedItemPosition() <= 0){
                                return; // add message box need to select a valid site
                            }
                            if (gv.userShape != null){
                                if (gv.userShape.size() <= 0){
                                    return; // add message box to warn user
                                }
                            }
                            else{
                                return; // no points, it's null
                            }

                            gv.GeneratePassword();
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
                            ct.writeMessage(outText.getText().toString());
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
