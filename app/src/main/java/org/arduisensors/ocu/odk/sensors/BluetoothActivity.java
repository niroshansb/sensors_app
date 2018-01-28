/*

 */

package org.arduisensors.ocu.odk.sensors;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import org.arduisensors.ocu.odk.sensors.storage.SharedPreferenceManager;
import org.arduisensors.ocu.odk.sensors.handlers.BluetoothHandler;
import org.arduisensors.ocu.odk.sensors.types.Ardui;
import org.arduisensors.ocu.odk.sensors.types.Type;

import java.util.List;

/**
 *
 */
public class BluetoothActivity
        extends Activity
        implements BluetoothHandler.DeviceFoundListener,
                    BluetoothHandler.BluetoothSessionListener{

    private static String TAG = "ODK Sensors Main Activity";
    private static String KEY_SENSOR = "sensor";
    /*
    Supported sensors include
        - bluetooth
     */
    private static String KEY_DATA_TYPE = "data_type";
    /*
    Supported data types include
        - Arduino sensors
     */

    //private LinearLayout dialogMainLayout;
    private TextView dialogTextTV;

    private BluetoothHandler bluetoothHandler;
    private Type type;

    private List<String> deviceNames;
    private List<BluetoothDevice> bluetoothDevices;
    private String sensorToUse;
    private String returnDataType;

    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_bluetooth);

        dialogTextTV = (TextView)this.findViewById(R.id.dialog_text_tv);

        Bundle bundle = this.getIntent().getExtras();
        if(bundle != null){
            sensorToUse = bundle.getString(KEY_SENSOR);
            if(sensorToUse != null) sensorToUse = sensorToUse.toLowerCase();

            returnDataType = bundle.getString(KEY_DATA_TYPE);
            if(returnDataType != null) {
                returnDataType = returnDataType.toLowerCase();
                if (returnDataType.equals(Ardui.KEY)) {
                    type = new Ardui();
                }
            }


            Log.i("BluetoothActivity", "Gotten data from parent activity");
        }
        else{
            Log.w(TAG, "Was unable to get data from previous activity. Probably because the activity was called from the launcher");
        }
        Log.i(TAG, "onCreated finished");
    }

    private String processOutput(String output) {
        if (type != null) {
            return type.process(output);
        }

        return output;
    }

    /**
     * This method is called second after onCreate
     * Initiate all hardware resources her and not onCreate.
     * Because all the hardware is released when onPause is called and onCreate might not be called
     * if activity resumes eg from lock screen
     *
     *      {App Launched for the 1st time} > onCreate > onResume > {screen times out} > onPause > {user turn screen back on} > onResume
     */
    @Override
    protected void onResume() {
        super.onResume();

        if(bluetoothHandler == null) {
            bluetoothHandler = new BluetoothHandler(this, type, this);
        }
        else {
            Log.i(TAG, "Bluetooth Handler is not null, not reinitializing it");
        }

        if(isChildActivity()){
            initBluetoothSearch();
        }

        Log.i(TAG, "onResume finished");
    }

    /**
     * This is the first method to be called when activity becomes invisible to the user.
     * Please unlink from all hardware resources here and not in onDestroy or any other exit method
     *  to avoid eating up the devices battery
     */
    @Override
    protected void onPause() {
        super.onPause();

        stopBluetoothHandler();

        if(progressDialog != null) progressDialog.dismiss();
        progressDialog = null;
        Log.i(TAG, "onPause finished");
    }

    /**
     * Called when the menu in the action bar has been initialized.
     * Call any method that requires stuff in the actionbar here and not in
     *  onCreate or onResume to avoid getting NullPointerExceptions
     *
     * @param menu
     *
     * @return true if actionbar (panel) is to be displayed
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        //workingMenuItem = menu.findItem(R.id.action_working);

        initBluetoothSearch();
        return true;
    }

    /**
     * This method is called when a menu item is clicked
     *
     * @param item
     * @return true if you are done with processing or false to perform normal menu handling
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        else if(id == R.id.action_scan) {
            initBluetoothSearch();
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateProgressDialog(String message){
        if(isChildActivity()){
            dialogTextTV.setText(message);
        }
        else {
            if (progressDialog == null) {
                progressDialog = ProgressDialog.show(BluetoothActivity.this, null, message, true, true);
                setProgressDialogDismissListener(progressDialog, null, BluetoothActivity.this);
            }
            else {
                progressDialog.setMessage(message);
                progressDialog.show();
            }
        }
    }

    /**
     * This method is called when Bluetooth Handler starts scanning for devices
     */
    @Override
    public void onSearchStart() {
        Log.i(TAG, "Search for bluetooth devices started");

        this.runOnUiThread(new Runnable() {//done in case method is called from a thread that is not the UI thread
            @Override
            public void run() {
                //show the spinning thingy on the action bar
                setProgressBarIndeterminateVisibility(Boolean.TRUE);

                if (isChildActivity()) {
                    //show dialog for scanning
                    updateProgressDialog(getResources().getString(R.string.make_sure_devices_bluetooth_on));
                }
            }
        });
    }

    /**
     * This method is called when a device is found by Bluetooth Handler
     *
     * @param device The BluetoothDevice found
     */
    @Override
    public void onDeviceFound(BluetoothDevice device) {
        Log.i(TAG, "onDeviceFound called. New device is : " + device.getName());

        if(isChildActivity()){
            if(bluetoothHandler.isDevicePaired(device)){
                BluetoothSocketThread bluetoothSocketThread = new BluetoothSocketThread();
                bluetoothSocketThread.execute(device);
            }
        }
        /*else{
            //Get all the found devices so far
            List<BluetoothDevice> availableDevices = bluetoothHandler.getAvailableDevices();
            if(availableDevices != null){
                deviceNames = new ArrayList<String>(availableDevices.size());
                bluetoothDevices = new ArrayList<BluetoothDevice>(availableDevices.size());

                for(int index = 0; index < availableDevices.size(); index++){
                    BluetoothDevice currDevice = availableDevices.get(index);

                    deviceNames.add(currDevice.getName());
                    bluetoothDevices.add(currDevice);// use this instead of bluetoothDevices = bluetoothHandler.getAvailableDevices() to avoid passing by reference
                }

                //repopulate the devices list view
                ((ArrayAdapter)devicesLV.getAdapter()).clear();
                ((ArrayAdapter)devicesLV.getAdapter()).addAll(deviceNames);
                ((ArrayAdapter)devicesLV.getAdapter()).notifyDataSetChanged();
            }
            else{
                Log.w(TAG, "Returned Available device list from bluetooth handler is null ");
            }
        }*/

    }

    /**
     * This method is called when Bluetooth handler stops scanning for devices
     */
    @Override
    public void onSearchStop() {
        this.runOnUiThread(new Runnable() {//done in case the method is called in a thread that is not the UI thread
            @Override
            public void run() {
                //hide the spinning thingy in the action bar
                setProgressBarIndeterminateVisibility(Boolean.FALSE);

                if (isChildActivity()) {
                    if (!bluetoothHandler.isSocketActive()) {//if not connected to a device, take back to parent activity
                        //wait for socket to fully initialize
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (!bluetoothHandler.isSocketActive()) {//if there is still no active connection, give up
                            Toast.makeText(BluetoothActivity.this, getResources().getString(R.string.no_device_found), Toast.LENGTH_LONG).show();
                            Intent intent = new Intent();
                            setResult(RESULT_CANCELED, intent);
                            finish();
                        }
                    }
                }
            }
        });
    }

    /**
     * This method is called when a sub-activity called using startActivityForResults is called
     * Refer to:
     *      - http://developer.android.com/reference/android/app/Activity.html#startActivityForResult(android.content.Intent, int)
     *
     * @param requestCode The code used by this activity to call the sub-activity
     * @param resultCode Can either be RESULT_OK if the result is fine or RESULT_CANCEL if the sub-activity was unable to get a result
     * @param data The data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i(TAG, "onActivityResult called");
        if(requestCode == BluetoothHandler.REQUEST_ENABLE_BT){
            if(resultCode == RESULT_OK){//bluetooth was successfully enabled
                Log.d(TAG, "Bluetooth was successfully enabled");

                //Make sure you reinitialize the bluetooth handler because it's null
                bluetoothHandler = new BluetoothHandler(this, type, this);

                startBluetoothSearch();
            }
            else{//means that user did not enable bluetooth
                Log.d(TAG, "Bluetooth was not enabled");
                Toast.makeText(this, this.getText(R.string.bluetooth_was_not_enabled), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * This method initiates the process of getting available bluetooth devices
     * The process is:
     *      - Checking if bluetooth is enabled
     *      - Searching for available bluetooth devices
     */
    private void initBluetoothSearch(){
        Log.i(TAG, "initBluetoothSearch called");

        if(bluetoothHandler.isBluetootSupported()){
            if(bluetoothHandler.isBluetootEnabled()){
                Log.d(TAG, "Bluetooth is on");
                //check if a default device was set
                String defaultDeviceAddress = SharedPreferenceManager.getSharedPreference(this, SharedPreferenceManager.SP_DEFAULT_BT_RFID_DEVICE_ADDRESS, BluetoothHandler.DEFAULT_BT_MAC_ADDRESS);
                if(defaultDeviceAddress.equals(BluetoothHandler.DEFAULT_BT_MAC_ADDRESS)){
                    Log.i(TAG, "Sensor device not set. Scanning for all available devices");
                    startBluetoothSearch();
                }
                else{
                    Log.i(TAG, "User set Sensor device to "+defaultDeviceAddress);
                    BluetoothDevice bluetoothDevice = bluetoothHandler.getBluetoothDevice(defaultDeviceAddress);
                    if(bluetoothDevice != null){
                        BluetoothSocketThread bluetoothSocketThread = new BluetoothSocketThread();
                        bluetoothSocketThread.execute(bluetoothDevice);
                    }
                    else{
                        startBluetoothSearch();
                    }
                }
            }
            else{
                Log.d(TAG, "Bluetooth is off");
                bluetoothHandler.requestEnableBluetooth();
            }
        }
        else{
            //TODO: tell user in dialog that bluetooth is not supported
        }
    }

    /**
     * This method tells BluetoothHandler to start the actual search
     */
    private void startBluetoothSearch(){
        Log.i(TAG, "Bluetooth search started");
        bluetoothHandler.startScan();
    }

    /**
     * This method kills everything in Bluetooth Handler that needs to be killed before bluetooth
     *  handler is set to null. This includes releasing the bluetooth module and unregistering any
     *  receiver
     */
    private void stopBluetoothHandler(){
        bluetoothHandler.stopScan();
        bluetoothHandler.closeSocket(null, null);//close any hanging socket
        bluetoothHandler.unregisterReceiver();

        bluetoothHandler = null;
    }

    private void setProgressDialogDismissListener(ProgressDialog progressDialog, final BluetoothDevice bluetoothDevice, final BluetoothHandler.BluetoothSessionListener sessionListener){

        if(progressDialog != null){

            progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    if(bluetoothHandler != null) {
                        bluetoothHandler.closeSocket(bluetoothDevice, sessionListener);
                    }
                    else {
                        Log.w(TAG, "Bluetooth Handler is null. Might mean that the dialog's onDismiss was called after the handler was deconstructed");
                    }

                    if(isChildActivity()){
                        //TODO: if activity is child, return null to parent activity
                        Intent intent = new Intent();
                        setResult(RESULT_CANCELED, intent);

                        finish();
                    }
                }
            });
        }
    }

    /**
     * This method is called when BluetoothHandler is able to connect to a bluetooth device
     * @param device The device connected to
     */
    @Override
    public void onConnected(final BluetoothDevice device) {
        this.runOnUiThread(new Runnable() {//Used in case the method is called from a thread that is not the UI thread
            @Override
            public void run() {
                updateProgressDialog(getResources().getString(R.string.scan_using) + " " + device.getName());
            }
        });
    }

    /**
     * This method is called when Bluetooth Handler is able to create a socket to the bluetooth device
     *
     * @param device The device on the other end of the socket
     */
    @Override
    public void onSocketOpened(final BluetoothDevice device) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateProgressDialog(getResources().getString(R.string.trying_to_connect_to_) + " " + device.getName());
            }
        });
    }

    /**
     * This method is called when the first message is gotten from the bluetooth device.
     * The first message is ignored because it might be a message that was cached on the
     *  bluetooth device before it was connected to this (android) device. Observed in:
     *      - Allflex RFID Stick Reader Model No. RS320-3-60
     *
     * @param device    The device that sent the message
     * @param message   The message from the bluetooth device
     */
    @Override
    public void onFirstMessageGotten(final BluetoothDevice device, final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateProgressDialog(getResources().getString(R.string.scan_again)+ " \n " + processOutput(message));
            }
        });
    }

    /**
     * This method checks whether this activity has been called as a child activity by ODK
     * or if called from the launcher
     *
     * @return True if the activity was called as a child activity
     */
    private boolean isChildActivity(){
        if(BluetoothActivity.this.getCallingActivity() == null) return false;
        else return true;
    }

    /**
     * This method is called when the second message is gotten from the bluetooth device.
     * Note that the message here is identical to the first message if we have come this far
     *
     * @param device The device that sent the message
     * @param message The message from the bluetooth device
     */
    @Override
    public void onActualMessageGotten(final BluetoothDevice device, final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(progressDialog != null) progressDialog.dismiss();
                progressDialog = null;//do a bit of house cleaning

                if(message != null){
                    Log.d(TAG, "Message from "+device.getName()+" is "+message);

                    //Test whether the activity was called from the launcher or by ODK Collect as a sub activity
                    if(!isChildActivity()){//activity called from the launcher
                        Toast.makeText(BluetoothActivity.this, "Message from "+device.getName()+" is " + message + ". App not called by other app", Toast.LENGTH_LONG).show();
                        Log.i(TAG, "Activity not called by another activity. Result just displayed");
                    }
                    else{//activity called by odk
                        Log.i(TAG, "Activity called by "+BluetoothActivity.this.getCallingActivity().getClassName() + " sending message there");

                        Intent intent = new Intent();
                        /*
                            "value" on the next line is specific to ODK, If you use anything else, ODK will not insert the message into the textfield
                            Refer to http://opendatakit.org/help/form-design/external-apps/
                         */

                        if(type != null){
                            String value = processOutput(message);
                            intent.putExtra("value", value);
                            setResult(RESULT_OK, intent);
                        }
                        else{
                            setResult(RESULT_CANCELED, intent);

                            Log.e(TAG, "The return data type is null. Probably because ODK form did not provide activity with one");
                            Toast.makeText(BluetoothActivity.this, BluetoothActivity.this.getResources().getString(R.string.something_wrong_odk), Toast.LENGTH_LONG).show();
                        }

                        finish();
                    }
                }
                else {
                    Toast.makeText(BluetoothActivity.this, getResources().getString(R.string.no_message_received_from_) + " " + device.getName(), Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Message from " + device.getName() + " is null");
                }
            }
        });
    }

    /**
     * Called when the bluetooth socket to the device is successfully closed by Bluetooth Handler
     *
     * @param device The device on the other end of the socket (now closed)
     */
    @Override
    public void onSocketClosed(final BluetoothDevice device) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Socket connection with " + device.getName() + " closed");
                //Toast.makeText(BluetoothActivity.this, getResources().getString(R.string.closed_socket_with)+ " " + device.getName(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * This method is called whenever the Bluetooth handler is unable to create or continue with the socket
     *
     * @param device    The device on the other end of the socket
     */
    @Override
    public void onSocketCanceled(final BluetoothDevice device) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, "Was unable to start socket with " + device.getName() + " returning nothing to the parent activity");
                Toast.makeText(BluetoothActivity.this,getString(R.string.unable_to_connect_to_) + " " + device.getName(), Toast.LENGTH_LONG).show();

                Intent intent = new Intent();
                setResult(RESULT_CANCELED, intent);

                finish();
            }
        });
    }

    /**
     * This class creates an asynchronous thread for fetching data from bluetooth since getting values from
     *  InputStreams block a thread.
     *  Refer to:
     *      - http://developer.android.com/reference/android/os/AsyncTask.html
     *      - http://developer.android.com/guide/topics/connectivity/bluetooth.html
     */
    private class BluetoothSocketThread extends AsyncTask<BluetoothDevice, Integer, Boolean>{

        @Override
        protected Boolean doInBackground(BluetoothDevice... bluetoothDevices) {
            Boolean result = bluetoothHandler.getDataFromDevice(bluetoothDevices[0], BluetoothActivity.this);
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if(result == null || result == false){
                Log.w(TAG, "Unable to initiate connection with bluetooth device");
            }
            else{
                Log.i(TAG, "Async task with bluetooth device successfully initiated");
            }
        }
    }
}
