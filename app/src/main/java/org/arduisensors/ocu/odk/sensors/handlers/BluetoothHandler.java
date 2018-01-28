/*
 Note that this application has been custom made by and for use by the ILRI Azizi Biorepository team. (C) 2015 Jason Rogena <j.rogena@cgiar.org>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.arduisensors.ocu.odk.sensors.handlers;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.arduisensors.ocu.odk.sensors.types.Type;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**

 */
public class BluetoothHandler {

    public static final int REQUEST_ENABLE_BT = 3321;
    public static final String KEY = "bluetooth";

    public static final String DEFAULT_BT_MAC_ADDRESS = "0000";

    private static final String TAG = "ODK Sensors BluetoothHandler";

    private final Activity activity;
    private final BluetoothAdapter bluetoothAdapter;
    private final Type type;// The data type to be gotten using the handler
    private final BroadcastReceiver broadcastReceiver;
    private final List<BluetoothDevice> availableDevices;
    private final DeviceFoundListener deviceFoundListener;
    private BluetoothSocket bluetoothSocket;//There should only be one bluetooth socket open
    private InputStream bluetoothInputStream;//bluetooth socket's input stream
    private OutputStream bluetoothOutputStream;//bluetooth socket's output stream


    /**
     * The constructor. If you don't know about constructors, well.., take more programming lessons then
     * come back ;)
     *
     * @param activity
     */
    public BluetoothHandler(Activity activity, Type type, final DeviceFoundListener deviceFoundListener){
        this.activity = activity;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.type = type;

        availableDevices = new ArrayList<BluetoothDevice>();

        this.deviceFoundListener = deviceFoundListener;

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    availableDevices.add(device);
                    if(deviceFoundListener != null) deviceFoundListener.onDeviceFound(device);
                }

                else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                    availableDevices.clear();
                    if(deviceFoundListener != null) deviceFoundListener.onSearchStart();
                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                    if(deviceFoundListener != null) deviceFoundListener.onSearchStop();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        activity.registerReceiver(broadcastReceiver, intentFilter);
    }

    /**
     * This method checks whether the device has supported bluetooth hardware
     *
     * @return true if bluetooth hardware available in device else false
     */
    public boolean isBluetootSupported(){
        if(bluetoothAdapter == null) return false;
        return true;
    }

    /**
     * This method checks whether bluetooth is on.
     * It's advisable to first check if the device has supported BT hardware before checking if
     * bluetooth is on
     *
     * @return true if bluetooth is on and false if not on or device does not have supported BT hardware
     */
    public boolean isBluetootEnabled(){
        if(bluetoothAdapter != null){
            return bluetoothAdapter.isEnabled();
        }

        return false;
    }

    /**
     * This method requests user to enable bluetooth if:
     *   - the device has supported hardware
     *   - bluetooth is off
     *
     * This method should be used with the onActivityResult() callback
     * for determining when bluetooth is enabled by the user
     *
     */
    public void requestEnableBluetooth(){
        if(isBluetootSupported() && !isBluetootEnabled()){
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * This method checks whether the given bluetooth device is paired with this android device
     *
     * @param device The bluetooth device that you want to determine the pairing status
     *
     * @return true if the device has already been paired
     */
    public boolean isDevicePaired(BluetoothDevice device){

        if(isBluetootEnabled()){
            if(device != null){
                Set<BluetoothDevice> pairedDevices = getPairedDevices();
                for(BluetoothDevice currDevice : pairedDevices){
                    if(currDevice.getAddress().equals(device.getAddress())){
                        Log.d(TAG, "The device has already been paired");
                        return true;
                    }
                }
            }
            else{
                Log.w(TAG, "Provided device is null. Returning false in isDevicePaired");
            }
        }
        else {
            Log.w(TAG, "BluetoothHandler is either not enabled or the device does not have supported bluetooth hardware. Returning null for getAvailablePairedDevice()");
        }

        return false;
    }

    public Set<BluetoothDevice> getPairedDevices() {
        return bluetoothAdapter.getBondedDevices();
    }

    public BluetoothDevice getBluetoothDevice(String macAddress) {
        Set<BluetoothDevice> pairedDevices = getPairedDevices();

        for(BluetoothDevice currDevice : pairedDevices){
            if(currDevice.getAddress().equals(macAddress)){
                return currDevice;
            }
        }
        return null;
    }

    /**
     * This method tries to start the actual scanning for bluetooth devices
     *
     * @return true is scanning started successfully
     */
    public boolean startScan(){
        if(isBluetootEnabled()){
            if(broadcastReceiver != null){
                return bluetoothAdapter.startDiscovery();
            }
            else{
                Log.w(TAG, "For some reason broadcast receiver is null. Returning false for startScan()");
            }
        }
        else {
            Log.w(TAG, "BluetoothHandler is not enabled or device does not have supported hardware. Returning false for startScan()");
        }

        return false;
    }

    /**
     * This method gracefully unregisters any broadcast receiver created by this class.
     * Make sure you call this method whenever the parent activity goes to sleep (in onPause)
     *  if you want to save the devices battery
     */
    public void unregisterReceiver(){
        try{//sand boxed because there is really no way to check if receiver is still registered
            activity.unregisterReceiver(broadcastReceiver);
            Log.i(TAG, "broadcastReceiver unregistered");
        }
        catch (Exception e){
            Log.w(TAG, "broadcastReceiver was already unregistered");
        }
    }

    /**
     * This method stops the bluetoothAdapter from scanning for bluetooth devices.
     * Note that the adapter might have already have stopped scanning. Refer to:
     *      - http://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html#startDiscovery()
     *
     * Call this method when you want to do anything else with bluetooth. It'll make that process much faster
     */
    public void stopScan(){
        bluetoothAdapter.cancelDiscovery();
    }

    /**
     * This method returns a list of bluetooth devices found so far in the current/last scan.
     * Note that this list is set to 0 whenever the scan is restarted and not when the current scan is complete
     *
     * @return The list of discoverable bluetooth devices found in the current/last scan
     */
    public List<BluetoothDevice> getAvailableDevices(){
        return availableDevices;
    }

    /**
     * This method returns the first supported UUID for the bluetooth device
     *
     * @param device The device we are using the get a UUID
     *
     * @return The first supported UUID gotten from the device
     */
    private UUID getUUID(BluetoothDevice device){
        return device.getUuids()[0].getUuid();//Reason why the minimum sdk is 15
    }

    /**
     * This method initiates the process of getting data from the connected bluetooth device
     * Please make sure you call this method from a thread that is asynchronous to the UI thread.
     * Refer to:
     *      - http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @param device The device we are getting data from
     * @param sessionListener The listener that the UI thread will use when socket is started
     *
     * @return true if we are indeed able to make the initial connection to the device
     */
    public boolean getDataFromDevice(BluetoothDevice device, BluetoothSessionListener sessionListener){
        if(device !=null){
            stopScan();

            AsClientConnectionThread clientConnectionThread = new AsClientConnectionThread(device, sessionListener);
            clientConnectionThread.run();
            return true;
        }
        else{
            Log.w(TAG, "The bluetooth device provided to initiateConnectionAsClient is null. initiateConnectionAsClient returning false");
        }
        return false;
    }

    /**
     * This class initialises a synchronous thread that does the connection to the bluetooth device and opens up the socket
     */
    private class AsClientConnectionThread extends Thread {
        private final BluetoothDevice device;
        private final BluetoothSessionListener sessionListener;

        private int connectionRetries;

        /**
         * The constructor.
         *
         * @param device The device to connect to
         * @param sessionListener The session listener being used by the UI thread to receive updates (since I assume we are running asynchronously here)
         */
        public AsClientConnectionThread (BluetoothDevice device, BluetoothSessionListener sessionListener){
            this.device = device;
            this.sessionListener = sessionListener;
            BluetoothSocket tmpSocket = null;


            try {
                tmpSocket = device.createRfcommSocketToServiceRecord(getUUID(device));
            }

            catch (IOException e) {
                Log.e(TAG, "Was unable to initiate a socket with Bluetooth server in AsClientConnectionThread");
                e.printStackTrace();
            }

            closeSocket(null, null);//Close any previous lingering socket, if any

            bluetoothSocket = tmpSocket;
            sessionListener.onSocketOpened(device);

            connectionRetries = 0;
        }

        /**
         * This method holds the code to be run in the thread being initialized.
         * Note that this thread is not asynchronous
         * Refer to:
         *      - http://developer.android.com/guide/components/processes-and-threads.html#Threads
         */
        @Override
        public void run() {
            super.run();

            stopScan();//Stop scan (in case the user started it again after getDataFromDevice was called)

            connectionRetries = 0;
            tryToConnect();

            if(bluetoothSocket != null){
                sessionListener.onConnected(device);

                getData(device, sessionListener);
            }
            else{
                Log.w(TAG, "Bluetooth Socket deinitialized from another thread. Cannot get data from it");
            }
        }

        private void tryToConnect() {
            try {
                if(bluetoothSocket != null){//this might be called when the bluetooth socket was set to null from another thread
                    bluetoothSocket.connect();//This right here blocks the thread until a connection is gotten or a timeout is reached
                }
            }
            catch (IOException e) {
                Log.w(TAG, "Was unable to connect to socket with Bluetooth server in AsClientConnectionThread");

                try {
                    Thread.sleep(500);
                }
                catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                connectionRetries++;

                if(connectionRetries < 6){
                    Log.i(TAG, "Trying for the " + connectionRetries + " time to initialize the bluetooth socket");
                    tryToConnect();//recurrsive method
                }
                else{
                    connectionRetries = 0;
                    closeSocket(device, sessionListener);
                    Log.w(TAG, "Giving up on trying to initialize connection with "+device.getName());

                    if(sessionListener != null){
                        sessionListener.onSocketCanceled(device);
                    }
                }
            }
        }
    }

    /**
     * This method initiated the process of getting the actual data from the bluetooth device
     *
     * @param device The device to get data from
     * @param sessionListener The session listener being used by the UI thread to receive updates
     */
    private void getData(BluetoothDevice device, BluetoothSessionListener sessionListener){
        TransferThread transferThread = new TransferThread(device, sessionListener);
        transferThread.run();
    }

    /**
     * This class initializes a thread for getting the data from the connected bluetooth device
     */
    private class TransferThread extends Thread {
        private final BluetoothDevice device;
        private final BluetoothSessionListener sessionListener;

        /**
         * The constructor.
         *
         * @param device The device to get data from
         * @param sessionListener The session listener being used by the UI thread to receive updates
         */
        public TransferThread(BluetoothDevice device, BluetoothSessionListener sessionListener){
            this.device = device;
            this.sessionListener = sessionListener;

            InputStream bluetoothInputStream = null;
            OutputStream bluetoothOutputStream = null;

            try{
                bluetoothInputStream = bluetoothSocket.getInputStream();
                bluetoothOutputStream = bluetoothSocket.getOutputStream();
            }
            catch (IOException e){
                Log.e(TAG, "IOException thrown while tying to create an input and output stream to bluetooth device");
                e.printStackTrace();
            }

            BluetoothHandler.this.bluetoothInputStream = bluetoothInputStream;
            BluetoothHandler.this.bluetoothOutputStream = bluetoothOutputStream;
        }

        /**
         * This method holds the code to be run in the thread being initialized.
         * Note that this thread is not asynchronous.
         * Refer to:
         *      - http://developer.android.com/guide/components/processes-and-threads.html#Threads
         */
        @Override
        public void run() {
            super.run();

            String message = convertStreamToString(bluetoothInputStream);//this method will block the thread until something is gotten

            closeSocket(device, sessionListener);

            sessionListener.onActualMessageGotten(device, message);//this method is called last because code called after it might not be run
        }

        /**
         * This method converts the provided inputStream into a string.
         * Note that some lines of code in this method block the thread until something is returned from
         *  the other side.
         * Also note that it's not doing a conversion but rather extraction (for lack of a better word)
         *
         * @param inputStream The input stream to be converted into a string
         *
         * @return The string
         */
        private String convertStreamToString(InputStream inputStream){
            try{
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;

                boolean confirmed = false;

                //do until the first line gotten from the input stream matches the second
                while(confirmed == false){
                    /*
                    The first line should be discarded afterward since some devices returned a cached value first before returning the
                    actual scan value. Observed in:
                            - Allflex RFID Stick Reader Model No. RS320-3-60
                     */

                    String firstScan = processTypeOutput(reader.readLine());//this line of code blocks the thread until something is returned
                    Log.d(TAG, firstScan);

                    sessionListener.onFirstMessageGotten(device, firstScan);

                    line = processTypeOutput(reader.readLine());//Process this string and not firstScan. This line of code also blocks the thread
                    Log.d(TAG, line);

                    if(firstScan.equals(line)){
                        confirmed = true;
                    }
                }

                //inputStream.close();

                return line;
            }
            catch (Exception e){
                Log.e(TAG, "An error occurred while trying to convert input stream to string");
            }

            return null;
        }
    }

    private String processTypeOutput(String output) {
        if (type != null) {
            return type.process(output);
        }

        return output;
    }

    /**
     * This method closes the socket to the bluetooth device.
     * Make sure it's called after the InputStream and OutputStream are closed
     */
    public void closeSocket(BluetoothDevice device, BluetoothSessionListener sessionListener){
        try{
            if(bluetoothInputStream != null) {
                bluetoothInputStream.close();
                bluetoothInputStream = null;
            }

            if(bluetoothOutputStream != null){
                bluetoothOutputStream.close();
                bluetoothOutputStream = null;
            }

            if(bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }

            if(sessionListener !=null && device != null) {
                sessionListener.onSocketClosed(device);
            }
            Log.i(TAG, "Bluetooth socket successfully closed");
        }
        catch (Exception e){
            Log.e(TAG, "Something went wrong while trying to close the bluetooth socket with device");
            e.printStackTrace();
        }
    }

    /**
     * This interface describes a listener for device discovery
     */
    public interface DeviceFoundListener {
        void onSearchStart();
        void onDeviceFound(BluetoothDevice device);
        void onSearchStop();
    }

    /**
     * This interface describes a listener for connection with a bluetooth device
     * and transfer of data from that device
     */
    public interface BluetoothSessionListener {
        void onConnected(BluetoothDevice device);
        void onSocketOpened(BluetoothDevice device);
        void onFirstMessageGotten(BluetoothDevice device, String message);
        void onActualMessageGotten(BluetoothDevice device, String message);
        void onSocketClosed(BluetoothDevice device);
        void onSocketCanceled(BluetoothDevice device);
    }

    /**
     * This method checks whether there is an active bluetooth socket.
     * Note that this assumes that only one socket object is used in this class
     *
     * @return True if there is an active socket
     */
    public boolean isSocketActive(){
        if(bluetoothSocket == null){
            return false;
        }
        else return true;
    }

}
