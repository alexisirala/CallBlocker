/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.eaglx.callblocker;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {

    private static final String
            PREF_BLOCK_HIDDEN_NUMBERS = "blockHiddenNumbers",
            PREF_NOTIFICATIONS = "notifications",
            PREF_BLOCK_OUT_OF_LIST = "block_out_of_list",
            PREF_DARK_MODE = "darkmode",
            PREF_EDIT_MODE = "edit_mode";

    private final SharedPreferences pref;


    public Settings(Context context) {
        pref = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
    }


    public boolean blockHiddenNumbers() {
        return pref.getBoolean(PREF_BLOCK_HIDDEN_NUMBERS, false);
    }

    public void blockHiddenNumbers(boolean block) {
        pref.edit()
            .putBoolean(PREF_BLOCK_HIDDEN_NUMBERS, block)
            .apply();
    }


    public boolean showNotifications() {
        return pref.getBoolean(PREF_NOTIFICATIONS, true);
    }

    public void showNotifications(boolean show) {
        pref.edit()
            .putBoolean(PREF_NOTIFICATIONS, show)
            .apply();
    }


    public boolean blockOutOfList() {
        return pref.getBoolean(PREF_BLOCK_OUT_OF_LIST, false);
    }

    public void blockOutOfList(boolean blockOutOfList) {
        pref.edit()
                .putBoolean(PREF_BLOCK_OUT_OF_LIST, blockOutOfList)
                .apply();
    }

    public boolean darkmode() {
        return pref.getBoolean(PREF_DARK_MODE, false);
    }

    public void darkmode(boolean darkmode) {
        pref.edit()
                .putBoolean(PREF_DARK_MODE, darkmode)
                .apply();
    }

    public boolean edit_mode() {
        return pref.getBoolean(PREF_EDIT_MODE, false);
    }

    public void edit_mode(boolean edit_mode) {
        pref.edit()
                .putBoolean(PREF_EDIT_MODE, edit_mode)
                .apply();
    }

}
