/*
 Note that this application has been custom made by and for use by the ILRI Azizi Biorepository team. (C) 2017 Jason Rogena <j.rogena@cgiar.org>

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

/**
 * Created by Jason Rogena - jrogena@ona.io on 11/29/17.
 */

public abstract class Type {
    public abstract String process(String raw);
    public abstract String getName();
}
