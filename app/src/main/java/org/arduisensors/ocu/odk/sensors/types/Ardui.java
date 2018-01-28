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

package org.arduisensors.ocu.odk.sensors.types;

import android.util.Log;

/**
 *
 */
public class Ardui extends Type {

    private static final String TAG = "ODK Sensors Ardui";

    public static final String KEY = "ardui";

    /**
     *
     */
    @Override
    public String process(String raw) {
        if(raw != null){
            raw = raw.replaceAll("\\s+", "");
            if (raw.length() == 35) {// Seven digit header, followed by the 15 digit tag number, then a 12 digit timestamp
                return raw.substring(8, raw.length() - 12);
            }
            else if (raw.length()>=15) {
                return raw.substring(raw.length() - 15, raw.length());
            }
            else{
                Log.w(TAG, "The length of the provided ardui sensor string is less than 15 (without the whitespaces). Cannot process this string");
            }
        }
        else{
            Log.w(TAG, "provided Ardui sensor string was null");
        }
        Log.w(TAG, "Process method returning the original string");
        return raw;
    }

    @Override
    public String getName() {
        return KEY;
    }
}
