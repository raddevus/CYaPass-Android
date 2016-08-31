package us.raddev.cyapass;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by roger.deutsch on 7/6/2016.
 */
public class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private ArrayAdapter<String> logViewAdapter;
    //private final InputStream mmInStream;
    private OutputStream mmOutStream;
    private InputStream mmInStream;

    public ConnectThread(BluetoothDevice device, UUID uuid,  ArrayAdapter<String> logViewAdapter) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final

        BluetoothSocket tmp = null;
        mmDevice = device;
        if (logViewAdapter != null) {
            this.logViewAdapter = logViewAdapter;
            logViewAdapter.add("in ConnectThread()...");
            logViewAdapter.notifyDataSetChanged();
        }
        OutputStream tmpOut;
        InputStream tmpIn;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            Log.d("MainActivity", "creating RfcommSocket...");
            if (logViewAdapter != null) {
                logViewAdapter.add("creating RfcommSocket...");
                logViewAdapter.notifyDataSetChanged();
            }
            tmp = device.createRfcommSocketToServiceRecord(uuid);
            Log.d("MainActivity", "created.");
            if (logViewAdapter != null) {
                logViewAdapter.add("created");
                logViewAdapter.notifyDataSetChanged();
            }
        } catch (IOException e) {
            Log.d("MainActivity", "FAILED! : " + e.getMessage());
            if (logViewAdapter != null) {
                logViewAdapter.add("FAILED! : " + e.getMessage());
                logViewAdapter.notifyDataSetChanged();
            }
        }
        mmSocket = tmp;
        try {
            tmpOut = tmp.getOutputStream();
            mmOutStream = tmpOut;
            tmpIn = mmSocket.getInputStream();
            mmInStream = tmpIn;
            //mmInStream = tmp.getInputStream();
        }
        catch (IOException iox) {
            Log.d("MainActivity", "failed to get stream : " + iox.getMessage());
        }
        catch (NullPointerException npe){
            Log.d("MainActivity", "null pointer on stream : " + npe.getMessage());

        }

    }

    public void writeYes() {
        try {
            byte [] outByte = new byte[]{121};

            mmOutStream.write(outByte);
            if (logViewAdapter != null) {
                logViewAdapter.add("Success; Wrote YES!");
                logViewAdapter.notifyDataSetChanged();
            }

            Thread.sleep(500);

        }
        catch (IOException e) { }
        catch (InterruptedException e) {
            Log.d("MainActivity", e.getStackTrace().toString());
        }
    }

    public void writeNo() {
        try {
            byte [] outByte = new byte[]{110};
            mmOutStream.write(outByte);
            if (logViewAdapter != null) {
                logViewAdapter.add("Success; Wrote NO");
                logViewAdapter.notifyDataSetChanged();
            }

            mmOutStream.write(outByte);
        } catch (IOException e) { }
    }

    public void writeCtrlAltDel(){
        try {
            byte[] outByte = new byte[]{(byte)224,(byte)226,(byte)42};
            mmOutStream.write(outByte);            if (logViewAdapter != null) {
                logViewAdapter.add("Success; Wrote &");
                logViewAdapter.notifyDataSetChanged();
            }
        }
        catch (IOException e) { }
    }

    public void writeMessage(String message) {
        try {
            byte [] outByte = new byte[message.length()];
            outByte = message.getBytes();
            mmOutStream.write(outByte);
            //logViewAdapter.add("Success; Wrote YES!");
            if (logViewAdapter != null) {
                logViewAdapter.notifyDataSetChanged();
            }

        } catch (IOException e) { }
    }

    public void run(BluetoothAdapter btAdapter) {
        // Cancel discovery because it will slow down the connection
        btAdapter.cancelDiscovery();

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            Log.d("MainActivity", "Connecting...");
            if (logViewAdapter != null) {
                logViewAdapter.add("Connecting...");
                logViewAdapter.notifyDataSetChanged();
            }
            mmSocket.connect();
            Log.d("MainActivity", "Connected");
            if (logViewAdapter != null) {
                logViewAdapter.add("Connected");
                logViewAdapter.notifyDataSetChanged();
            }
            if (mmOutStream != null) {
                if (logViewAdapter != null) {
                    mmOutStream.write(new byte[]{65, 66});
                    logViewAdapter.add("Success; Wrote 2 bytes!");
                    logViewAdapter.notifyDataSetChanged();
                }
            }
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            Log.d("MainActivity", "Failed! : " + connectException.getMessage());
            if (logViewAdapter != null) {
                logViewAdapter.add("Failed! : " + connectException.getMessage());
                logViewAdapter.notifyDataSetChanged();
            }
            try {
                mmSocket.close();
            } catch (IOException closeException) { }
            return;
        }

        // Do work to manage the connection (in a separate thread)
        //manageConnectedSocket(mmSocket);
    }

    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        if (logViewAdapter != null) {
            logViewAdapter.add("Reading from BT!...");
            logViewAdapter.notifyDataSetChanged();
        }
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);
                // Send the obtained bytes to the UI activity
                if (logViewAdapter != null) {
                    logViewAdapter.add(String.valueOf(bytes));
                    logViewAdapter.notifyDataSetChanged();
                }
            } catch (IOException e) {
                if (logViewAdapter != null) {
                    logViewAdapter.add("IOException on read: " + e.getMessage());
                    logViewAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
