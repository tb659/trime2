package com.androlua;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.osfans.trime.util.CustomToast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ImportProject extends Activity {
    private File mDownloadDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LuaApplication app = LuaApplication.getInstance();
        mDownloadDir = new File(app.getLuaExtDir("Download"));
        if (!mDownloadDir.exists())
            mDownloadDir.mkdirs();
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            String path = data.getPath();
            Log.i("ImportProject", path + "");
            if (path != null) {
                if ("content".equals(data.getScheme())) {
                    try {
                        InputStream in = getContentResolver().openInputStream(data);
                        String path2 = new File(mDownloadDir, new File(data.getPath()).getName()).getAbsolutePath();
                        FileOutputStream out = new FileOutputStream(path2);
                        LuaUtil.copyFile(in, out);
                        out.close();
                        load(path2);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                int idx = path.indexOf("/storage/emulated/");
                if (idx > 0)
                    path = path.substring(idx);
                load(path);
            }
        }
    }

    private boolean load(String path) {
        if (!new File(path).exists()) {
            finish();
            return false;
        }
        String dir;
        String type = path.substring(path.length() - 4);
        LuaApplication app = LuaApplication.getInstance();
        return load(path, app.getLuaExtDir("Projects") + File.separator, type);
    }

    private boolean load(final String path, final String dir, final String type) {
        String name = new File(path).getName();
        int i = name.lastIndexOf(".");
        if (i > 0) {
            name = name.substring(0, i);
        }
        i = name.indexOf("_");
        if (i > 0) {
            name = name.substring(0, i);
        }
        i = name.indexOf("(");
        if (i > 0) {
            name = name.substring(0, i);
        }
        String title = "导入 " + name;
        final String fname = name;
        final EditText edit = new EditText(this);
        edit.setText(name);
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(title)
                .setView(edit)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            LuaUtil.unZip(path, new File(dir, edit.getText().toString()).getAbsolutePath());
                            CustomToast.show(ImportProject.this, "导入完成", Toast.LENGTH_SHORT, false);
                        } catch (IOException e) {
                            e.printStackTrace();
                            CustomToast.show(ImportProject.this, e.getMessage(), Toast.LENGTH_SHORT, false);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            finishAndRemoveTask();
                        } else {
                            finish();
                        }

                    }
                })
                .create()
                .show();
        return true;
    }

}
