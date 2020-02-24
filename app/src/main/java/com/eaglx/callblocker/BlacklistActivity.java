/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.eaglx.callblocker;

import android.Manifest;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.eaglx.callblocker.global.AppConstants;
import com.eaglx.callblocker.model.BlacklistFile;
import com.eaglx.callblocker.model.DbHelper;
import com.eaglx.callblocker.model.Number;

public class BlacklistActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Set<Number>>, AdapterView.OnItemClickListener {

    protected Settings settings;
    CoordinatorLayout coordinatorLayout;

    ListView list;
    ArrayAdapter<Number> adapter;

    protected String[] fileList;
    protected static File basePath;
    protected static final int DIALOG_LOAD_FILE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blacklist);

        settings = new Settings(this);
        if(settings.darkmode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        list = (ListView)findViewById(R.id.numbers);
        list.setAdapter(adapter = new NumberAdapter(this));
        list.setOnItemClickListener(this);

        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                getMenuInflater().inflate(R.menu.blacklist_delete_numbers, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.delete) {
                    deleteSelectedNumbers();
                    actionMode.finish();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
            }
        });

        requestPermissions();

        getLoaderManager().initLoader(0, null, this);

        basePath = new File(String.valueOf(getExternalFilesDir(null)));
        File file = new File(getExternalFilesDir(null), BlacklistFile.DEFAULT_FILENAME);
        if(!file.exists()) {
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write("blacklist empty".getBytes());
                fileOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        AppConstants.setEdit_mode(settings.edit_mode());
    }

    protected void requestPermissions() {
        List<String> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Manifest.permission.CALL_PHONE);
        requiredPermissions.add(Manifest.permission.READ_PHONE_STATE);
        requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        requiredPermissions.add(Manifest.permission.READ_CALL_LOG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requiredPermissions.add(Manifest.permission.ANSWER_PHONE_CALLS);
        }

        List<String> missingPermissions = new ArrayList<>();

        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    missingPermissions.toArray(new String[0]), 0);
        }
    }

    private static class DeleteFromDB extends AsyncTask<Void, Void, Void> {

        private List<String> numbers;
        private BlacklistActivity context;

        DeleteFromDB(BlacklistActivity context, List<String> numbers) {
            this.numbers = numbers;
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            DbHelper dbHelper = new DbHelper(context);
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                for (String number : numbers)
                    db.delete(Number._TABLE, Number.NUMBER + "=?", new String[] { number });
            } finally {
                dbHelper.close();
            }

            context.getLoaderManager().restartLoader(0, null, context);
            return null;
        }
    }

    protected void deleteSelectedNumbers() {
        final List<String> numbers = new LinkedList<>();

        SparseBooleanArray checked = list.getCheckedItemPositions();
        for (int i = checked.size() - 1; i >= 0; i--)
            if (checked.valueAt(i)) {
                int position = checked.keyAt(i);
                numbers.add(adapter.getItem(position).number);
            }

        new DeleteFromDB(this, numbers).execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean ok = true;
        if (grantResults.length != 0) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    ok = false;
                    break;
                }
            }
        } else {
            // treat cancellation as failure
            ok = false;
        }

        if (!ok)
            Snackbar.make(coordinatorLayout, R.string.blacklist_permissions_required, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.blacklist_request_permissions, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            requestPermissions();
                        }
                    })
                    .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_blacklist, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.block_hidden_numbers).setChecked(settings.blockHiddenNumbers());
        menu.findItem(R.id.notifications).setChecked(settings.showNotifications());
        menu.findItem(R.id.block_out_of_list).setChecked(settings.blockOutOfList());
        menu.findItem(R.id.darkmode).setChecked(settings.darkmode());
        menu.findItem(R.id.edit_mode).setChecked(settings.edit_mode());
        return true;
    }

    public void onBlockHiddenNumbers(MenuItem item) {
        settings.blockHiddenNumbers(!item.isChecked());
    }

    public void onShowNotifications(MenuItem item) {
        settings.showNotifications(!item.isChecked());
    }

    public void onBlockOutOfList(MenuItem item) {
        settings.blockOutOfList(!item.isChecked());
    }

    public void onDarkMode(MenuItem item) {
        settings.darkmode(!item.isChecked());
        finish();
        startActivity(new Intent(BlacklistActivity.this, BlacklistActivity.this.getClass()));
    }

    public void onEditMode(MenuItem item) {
        settings.edit_mode(!item.isChecked());
        AppConstants.setEdit_mode(settings.edit_mode());
        adapter.notifyDataSetChanged();
    }

    public void onImportBlacklist(MenuItem item) {
        createDialog(DIALOG_LOAD_FILE);
    }

    public void createDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (id == DIALOG_LOAD_FILE) {
            final int lastDot = BlacklistFile.DEFAULT_FILENAME.lastIndexOf(".");
            final String ext = BlacklistFile.DEFAULT_FILENAME.substring(lastDot);
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(ext);
                }
            };

            fileList = basePath.list(filter);

            builder.setTitle(R.string.blacklist_import);
            builder.setItems(fileList, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    commitBlacklist(new BlacklistFile(basePath, fileList[which]));
                }
            });
        }
        builder.show();
    }

    public void commitBlacklist(@NonNull BlacklistFile blacklist) {
        DbHelper dbHelper = new DbHelper(BlacklistActivity.this);
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values;
            Boolean exists;
            for (Number number : blacklist.load()) {

                values = new ContentValues(4);
                values.put(Number.NAME, number.name);
                values.put(Number.NUMBER, Number.wildcardsViewToDb(number.number));
                values.put(Number.ALLOW, number.allow);

                exists = db.query(Number._TABLE, null, Number.NUMBER + "=?", new String[]{number.number}, null, null, null).moveToNext();
                if (exists)
                    db.update(Number._TABLE, values, Number.NUMBER + "=?" + " and " + Number.ALLOW + "=?", new String[]{number.number, String.valueOf(number.allow)});
                else
                    db.insert(Number._TABLE, null, values);
            }
        } finally {
            dbHelper.close();
        }

        getLoaderManager().restartLoader(0, null, BlacklistActivity.this);
    }

    public void onExportBlacklist(MenuItem item) {
        BlacklistFile f = new BlacklistFile(basePath, BlacklistFile.DEFAULT_FILENAME);

        List<Number> numbers = new LinkedList<>();
        for (int i = 0; i < adapter.getCount(); i++)
            numbers.add(adapter.getItem(i));

        f.store(numbers);

        Toast.makeText(
                getApplicationContext(),
                getResources().getText(R.string.blacklist_exported_to) + " " + getExternalFilesDir(null) + "/" + BlacklistFile.DEFAULT_FILENAME,
                Toast.LENGTH_LONG
        ).show();
    }

    public void onAbout(MenuItem item) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/eaglx/CallBlocker")));
    }

    public void addNumber(View view) {
        startActivity(new Intent(this, EditNumberActivity.class));
    }


    @Override
    public Loader<Set<Number>> onCreateLoader(int i, Bundle bundle) {
        return new NumberLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Set<Number>> loader, Set<Number> numbers) {
        adapter.clear();
        adapter.addAll(numbers);
    }

    @Override
    public void onLoaderReset(Loader<Set<Number>> loader) {
        adapter.clear();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Number number = adapter.getItem(position);

        Intent intent = new Intent(this, EditNumberActivity.class);
        intent.putExtra(EditNumberActivity.EXTRA_NUMBER, number.number);
        startActivity(intent);
    }

    protected static class NumberLoader extends AsyncTaskLoader<Set<Number>> implements BlacklistObserver.Observer {

        NumberLoader(Context context) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            BlacklistObserver.addObserver(this, true);
        }

        @Override
        public Set<Number> loadInBackground() {
            DbHelper dbHelper = new DbHelper(getContext());
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                Set<Number> numbers = new LinkedHashSet<>();
                Cursor c = db.query(Number._TABLE, null, null, null, null, null, Number.ID + " ASC");
                while (c.moveToNext()) {
                    ContentValues values = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(c, values);
                    numbers.add(Number.fromValues(values));
                }
                c.close();

                return numbers;
            } finally {
                dbHelper.close();
            }
        }

        @Override
        public void onBlacklistUpdate() {
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            BlacklistObserver.removeObserver(this);
        }

    }

}
