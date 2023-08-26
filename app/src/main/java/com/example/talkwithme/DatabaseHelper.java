package com.example.talkwithme;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "contacts";
    private String TABLE_NAME;
    private String [] COL_NAMES;
    static Map<String, String[]> tableColumnsMap  = new HashMap<String, String[]>() {{
        put("friends_table", new String[]{"Name", "Gender", "Organisation", "Status"});
        put("pending_table", new String[]{"Name", "Gender", "Organisation", "Status"});
        put("sensors_table", new String[]{"BPM", "Temperature", "Moisture", "Time"});
        put("map_table", new String[]{"Name"});
    }};
    SQLiteDatabase db;

    /*
    online tables
    1.login_table
    2.organisation_table
    3.online_table

    offline tables
    1.friends_table
    2.pending_table
    */

    DatabaseHelper(Context context, String id, String TABLE_NAME, String[] COL_NAMES) {
        super(context, DATABASE_NAME + id, null, 1);
        this.TABLE_NAME = TABLE_NAME;
        this.COL_NAMES = new String[COL_NAMES.length];
        this.COL_NAMES = COL_NAMES;
        this.db = this.getWritableDatabase();
        onCreate(db);
        db.close();
    }

    DatabaseHelper(Context context, String id, String TABLE_NAME) {
        super(context, DATABASE_NAME + id, null, 1);
        this.TABLE_NAME = TABLE_NAME;
        this.COL_NAMES = tableColumnsMap.get(TABLE_NAME);
        this.db = this.getWritableDatabase();
        onCreate(db);
        db.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //Create table for the first time for storage within phone
        StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS " + TABLE_NAME +" (ID INTEGER PRIMARY KEY AUTOINCREMENT");
        for (String columnName:COL_NAMES){
            query.append(", ").append(columnName).append(" TEXT");
        }
        query.append(")");
        db.execSQL(query.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    Cursor getAllData() {
        db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    @SuppressWarnings("unused")
    private void printAllData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        if (res.moveToFirst()) {
            while (!res.isAfterLast()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0 ; i < res.getColumnCount(); i++){
                    sb.append("/").append(res.getString(i));
                }
                Log.v(TABLE_NAME, sb.toString());
                res.moveToNext();
            }
        }
        res.close();
        db.close();
    }

    boolean insertData(String ID, String[] values) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("ID", ID);
        for (int i = 0; i < COL_NAMES.length; i++) {
            contentValues.put(COL_NAMES[i], values[i]);
        }
        long row_id = db.insert(TABLE_NAME, null, contentValues);
        db.close();
        return row_id != -1;
    }

    boolean updateData(String ID, String[] values) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        for (int i = 0; i < COL_NAMES.length; i++) {
            contentValues.put(COL_NAMES[i], values[i]);
        }
        long row_id = db.update(TABLE_NAME, contentValues, "ID = ?", new String[]{ID});
        db.close();
        return row_id != -1;
    }

    void deleteData(String ID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, "ID = ?", new String[]{ID});
        db.close();
    }

    boolean IDExist(String ID) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.query(TABLE_NAME, null, "ID=?"
                , new String[]{ID}, null, null, null);
        if (res.getCount() == 0) {
            res.close();
            db.close();
            return false;
        }else{
            res.close();
            db.close();
            return true;
        }
    }

    boolean fieldExist(String field, String value) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.query(TABLE_NAME, null, field + "=?"
                , new String[]{value}, null, null, null);
        if (res.getCount() == 0) {
            res.close();
            db.close();
            return false;
        }else{
            res.close();
            db.close();
            return true;
        }
    }

    long getRowCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, TABLE_NAME);
        db.close();
        return count;
    }
}


@SuppressWarnings("unused")
class DatabaseInfoHelper extends SQLiteOpenHelper {
    private String classname = getClass().getName();
    private static final String DATABASE_NAME = "contacts";

    DatabaseInfoHelper(Context context, String id) {
        super(context, DATABASE_NAME + id, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    void getAllColumnnames() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        if (res.moveToFirst()) {
            while (!res.isAfterLast()) {
                String table_name = res.getString(0);
                Log.v(classname, "Table Name=> " + table_name);
                Cursor res2 = db.query(table_name, null, null, null, null, null, null);
                String[] columnNames = res2.getColumnNames();
                for(String columnName:columnNames){
                    Log.v(classname, "Column Name=> " + columnName);
                }
                res2.close();
                res.moveToNext();
            }
        }
        res.close();
        db.close();
    }

    void dropTables(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        if (res.moveToFirst()) {
            while (!res.isAfterLast()) {
                String table_name = res.getString(0);
                if (!table_name.equals("sqlite_sequence")) {
                    db.execSQL("DROP TABLE IF EXISTS " + table_name);
                }
                res.moveToNext();
            }
        }
        res.close();
        db.close();
        Log.v(classname, "Tables dropped");
    }
}