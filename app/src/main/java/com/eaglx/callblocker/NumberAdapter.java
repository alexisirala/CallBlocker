package com.eaglx.callblocker;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.eaglx.callblocker.global.AppConstants;
import com.eaglx.callblocker.model.DbHelper;
import com.eaglx.callblocker.model.Number;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

public class NumberAdapter extends ArrayAdapter<Number> {

    NumberAdapter(Context context) {
        super(context, R.layout.blacklist_item);
    }

    @Override
    public View getView(final int position, View view, @NonNull ViewGroup parent) {
        if (view == null)
            view = View.inflate(getContext(), R.layout.blacklist_item, null);

        final Number number = getItem(position);

        TextView tv = (TextView)view.findViewById(R.id.number);
        tv.setText(Number.wildcardsDbToView(number.number));

        tv = (TextView)view.findViewById(R.id.name);
        tv.setText(number.name);

        tv = (TextView)view.findViewById(R.id.rule);
        if(number.allow == 1) {
            tv.setText(R.string.allow);
        }
        else {
            tv.setText(R.string.block);
        }

        tv = (TextView)view.findViewById(R.id.stats);
        if (number.lastCall != null) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(getContext().getResources().getQuantityString(R.plurals.blacklist_call_details, number.timesCalled,
                    number.timesCalled, SimpleDateFormat.getDateTimeInstance().format(new Date(number.lastCall))));
        } else
            tv.setVisibility(View.GONE);

        ImageButton upButton = (ImageButton) view.findViewById(R.id.upEditButton);
        ImageButton downButton = (ImageButton) view.findViewById(R.id.downEditButton);

        upButton.setOnClickListener(new View.OnClickListener() {
            Number n = number;

            @Override
            public void onClick(View v) {
                DbHelper dbHelper = new DbHelper(getContext());

                try{
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    Cursor c = db.query(Number._TABLE, null, null, null, null, null, Number.ID + " ASC");
                    ContentValues values = new ContentValues();
                    Number previous = null;

                    while (c.moveToNext()) {
                        DatabaseUtils.cursorRowToContentValues(c, values);

                        if(Number.fromValues(values).id.equals(n.id)) {
                            if(c.moveToPrevious()) {
                                DatabaseUtils.cursorRowToContentValues(c, values);
                                if (Number.fromValues(values).id != n.id) {
                                    previous = Number.fromValues(values);
                                }
                            }
                            break;
                        }
                    }

                    if(previous != null) {
                        db.delete(Number._TABLE, Number.ID + "=?", new String[] { "" + n.id });
                        db.delete(Number._TABLE, Number.ID + "=?", new String[] { "" + previous.id });

                        Integer tmp = previous.id;

                        previous.id = n.id;
                        n.id = tmp;

                        values = new ContentValues();
                        values.put(Number.NAME, previous.name);
                        values.put(Number.NUMBER, previous.number);
                        values.put(Number.ALLOW, previous.allow);
                        values.put(Number.ID, previous.id);
                        values.put(Number.LAST_CALL, previous.lastCall);
                        values.put(Number.TIMES_CALLED, previous.timesCalled);
                        db.insert(Number._TABLE, null, values);

                        values = new ContentValues();
                        values.put(Number.NAME, n.name);
                        values.put(Number.NUMBER, n.number);
                        values.put(Number.ALLOW, n.allow);
                        values.put(Number.ID, n.id);
                        values.put(Number.LAST_CALL, n.lastCall);
                        values.put(Number.TIMES_CALLED, n.timesCalled);
                        db.insert(Number._TABLE, null, values);

                        Set<Number> numbers = new LinkedHashSet<>();
                        c = db.query(Number._TABLE, null, null, null, null, null, Number.ID + " ASC");
                        while (c.moveToNext()) {
                            values = new ContentValues();
                            DatabaseUtils.cursorRowToContentValues(c, values);
                            numbers.add(Number.fromValues(values));
                        }
                        c.close();
                        clear();
                        addAll(numbers);
                    }
                } finally {
                    dbHelper.close();
                }
            }
        });

        downButton.setOnClickListener(new View.OnClickListener() {
            Number n = number;

            @Override
            public void onClick(View v) {
                DbHelper dbHelper = new DbHelper(getContext());

                try{
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    Cursor c = db.query(Number._TABLE, null, null, null, null, null, Number.ID + " ASC");
                    ContentValues values = new ContentValues();
                    Number next = null;

                    while (c.moveToNext()) {
                        DatabaseUtils.cursorRowToContentValues(c, values);
                        if(Number.fromValues(values).id == n.id) {
                            if(c.moveToNext()) {
                                DatabaseUtils.cursorRowToContentValues(c, values);
                                if (Number.fromValues(values).id != n.id) {
                                    next = Number.fromValues(values);
                                }
                            }
                            break;
                        }
                    }

                    if(next != null) {
                        db.delete(Number._TABLE, Number.ID + "=?", new String[] { "" + n.id });
                        db.delete(Number._TABLE, Number.ID + "=?", new String[] { "" + next.id });

                        Integer tmp = next.id;

                        next.id = n.id;
                        n.id = tmp;

                        values = new ContentValues();
                        values.put(Number.NAME, next.name);
                        values.put(Number.NUMBER, next.number);
                        values.put(Number.ALLOW, next.allow);
                        values.put(Number.ID, next.id);
                        values.put(Number.LAST_CALL, next.lastCall);
                        values.put(Number.TIMES_CALLED, next.timesCalled);
                        db.insert(Number._TABLE, null, values);

                        values = new ContentValues();
                        values.put(Number.NAME, n.name);
                        values.put(Number.NUMBER, n.number);
                        values.put(Number.ALLOW, n.allow);
                        values.put(Number.ID, n.id);
                        values.put(Number.LAST_CALL, n.lastCall);
                        values.put(Number.TIMES_CALLED, n.timesCalled);
                        db.insert(Number._TABLE, null, values);

                        Set<Number> numbers = new LinkedHashSet<>();
                        c = db.query(Number._TABLE, null, null, null, null, null, Number.ID + " ASC");
                        while (c.moveToNext()) {
                            values = new ContentValues();
                            DatabaseUtils.cursorRowToContentValues(c, values);
                            numbers.add(Number.fromValues(values));
                        }
                        c.close();
                        clear();
                        addAll(numbers);
                    }
                } finally {
                    dbHelper.close();
                }
            }
        });

        if(AppConstants.getEdit_mode()) {
            upButton.setVisibility(View.VISIBLE);
            downButton.setVisibility(View.VISIBLE);
        }
        else {
            upButton.setVisibility(View.INVISIBLE);
            downButton.setVisibility(View.INVISIBLE);
        }

        return view;
    }

}