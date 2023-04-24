package com.de.search.util;


import android.content.Context;
import android.content.SharedPreferences;


public class LocalStorageUtils {

    private static final String NAME = "data_info";

    // Save a key-value pair to SharedPreferences.
    public static void setParam(Context context, String key, Object object) {

        String type = object.getClass().getSimpleName();

        SharedPreferences sharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        switch (type){
            case "Long":
                editor.putLong(key, (Long) object);
                break;
            case "String":
                editor.putString(key, (String) object);
                break;
            case "Boolean":
                editor.putBoolean(key, (Boolean) object);
                break;
            case "Integer":
                editor.putInt(key, (Integer) object);
                break;
            case "Float":
                editor.putFloat(key, (Float) object);
                break;
        }

        editor.commit();
    }

    // Retrieve a value from SharedPreferences with a specified key. If the key does not exist, return the default value.
    public static Object getParam(Context context, String key, Object defaultObject) {
        String type = defaultObject.getClass().getSimpleName();
        SharedPreferences sp = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);

        switch (type){
            case "Long":
                return sp.getLong(key, (Long) defaultObject);
            case "String":
                return sp.getString(key, (String) defaultObject);
            case "Boolean":
                return sp.getBoolean(key, (Boolean) defaultObject);
            case "Integer":
                return sp.getInt(key, (Integer) defaultObject);
            case "Float":
                return sp.getFloat(key, (Float) defaultObject);
        }


        return null;
    }
}

