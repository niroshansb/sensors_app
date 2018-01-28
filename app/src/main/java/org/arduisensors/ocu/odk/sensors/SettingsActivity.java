/*

 */

package org.arduisensors.ocu.odk.sensors;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.util.Log;

import org.arduisensors.ocu.odk.sensors.handlers.BluetoothHandler;
import org.arduisensors.ocu.odk.sensors.storage.SharedPreferenceManager;
import org.arduisensors.ocu.odk.sensors.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class SettingsActivity extends PreferenceActivity
                              implements Preference.OnPreferenceChangeListener{

    private static final String TAG = "SettingsActivity";

    private PreferenceCategory bluetoothPC;
    private ListPreference btRFIDDefaultDevLP;

    private List<CharSequence> pairedBTDeviceNames;
    private List<CharSequence> pairedBTDeviceAddresses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.activity_settings);

        bluetoothPC = (PreferenceCategory)findPreference("bluetooth_pc");
        btRFIDDefaultDevLP = (ListPreference)findPreference("bt_rfid_default_dev_lp");
        btRFIDDefaultDevLP.setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadPairedBTDevices();
        loadSetPreferences();
    }

    private void loadPairedBTDevices() {
        //load the paired bluetooth devices
        BluetoothHandler bluetoothHandler = new BluetoothHandler(this, null, null);
        Set<BluetoothDevice> pairedDevices = bluetoothHandler.getPairedDevices();

        pairedBTDeviceNames = new ArrayList<CharSequence>();
        pairedBTDeviceAddresses = new ArrayList<CharSequence>();

        pairedBTDeviceNames.add(getString(R.string.none));
        pairedBTDeviceAddresses.add(BluetoothHandler.DEFAULT_BT_MAC_ADDRESS);

        for(BluetoothDevice currDevice : pairedDevices){
            pairedBTDeviceNames.add(currDevice.getName());
            pairedBTDeviceAddresses.add(currDevice.getAddress());
        }

        btRFIDDefaultDevLP.setEntries(pairedBTDeviceNames.toArray(new CharSequence[pairedBTDeviceNames.size()]));
        btRFIDDefaultDevLP.setEntryValues(pairedBTDeviceAddresses.toArray(new CharSequence[pairedBTDeviceAddresses.size()]));
    }

    private void loadSetPreferences(){
        String defaultBTRFIDDevice = SharedPreferenceManager.getSharedPreference(this, SharedPreferenceManager.SP_DEFAULT_BT_RFID_DEVICE_ADDRESS, BluetoothHandler.DEFAULT_BT_MAC_ADDRESS);
        String btRFIDDefaultDevName = null;

        btRFIDDefaultDevLP.setValue(defaultBTRFIDDevice);

        if(!defaultBTRFIDDevice.equals(BluetoothHandler.DEFAULT_BT_MAC_ADDRESS)){
            if(pairedBTDeviceAddresses != null && pairedBTDeviceNames != null){
                for(int i = 0; i < pairedBTDeviceAddresses.size(); i++){
                    if(pairedBTDeviceAddresses.get(i).equals(defaultBTRFIDDevice)){
                        btRFIDDefaultDevName = pairedBTDeviceNames.get(i).toString();
                    }
                }

                if(btRFIDDefaultDevName == null){//means that the default rfid device is no longer paired with this device.
                    //set the default device to no device
                    defaultBTRFIDDevice = BluetoothHandler.DEFAULT_BT_MAC_ADDRESS;
                    SharedPreferenceManager.setSharedPreference(this, SharedPreferenceManager.SP_DEFAULT_BT_RFID_DEVICE_ADDRESS, BluetoothHandler.DEFAULT_BT_MAC_ADDRESS);

                    Log.w(TAG, "The saved default bluetooth sensor device is no longer paired with this device. Setting the default bluetooth Sensor device to nothing");
                }
            }
        }

        btRFIDDefaultDevLP.setValue(defaultBTRFIDDevice);
        if(btRFIDDefaultDevName != null) {
            btRFIDDefaultDevLP.setSummary(btRFIDDefaultDevName);
        }
        else {
            btRFIDDefaultDevLP.setSummary(getString(R.string.pref_bt_rfid_default_dev_summary));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(preference == btRFIDDefaultDevLP){
            String defaultDeviceAddress = newValue.toString();
            String defaultDeviceName = "";
            for(int i = 0; i < pairedBTDeviceAddresses.size(); i++){
                if(pairedBTDeviceAddresses.get(i).equals(defaultDeviceAddress)){
                    defaultDeviceName = pairedBTDeviceNames.get(i).toString();
                }
            }

            btRFIDDefaultDevLP.setSummary(defaultDeviceName);
            SharedPreferenceManager.setSharedPreference(this, SharedPreferenceManager.SP_DEFAULT_BT_RFID_DEVICE_ADDRESS, defaultDeviceAddress);
        }
        return true;
    }
}
