/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.eaglx.callblocker.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 4;

    public DbHelper(Context context) {
        super(context, "database", null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Number._TABLE + "(" +
                Number.NUMBER + " TEXT NOT NULL," +
                Number.NAME + " TEXT NULL," +
                Number.LAST_CALL + " INTEGER NULL," +
                Number.TIMES_CALLED + " INTEGER NOT NULL DEFAULT 0," +
                Number.ALLOW + " INTEGER NULL," +
                Number.ID + " INTEGER PRIMARY KEY" +
        ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int from, int to) {
        db.execSQL("DROP TABLE IF EXISTS " + Number._TABLE);
        onCreate(db);
    }

}
