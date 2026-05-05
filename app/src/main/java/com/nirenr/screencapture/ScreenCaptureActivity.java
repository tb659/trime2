package com.nirenr.screencapture;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.androlua.LuaUtil;
import com.osfans.trime.util.CustomToast;

import java.util.ArrayList;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
/**
 * 屏幕截图权限请求 Activity。
 * 用于请求 MediaProjection 权限以进行屏幕截图。
 */
public class ScreenCaptureActivity extends Activity {

    /** 媒体投影权限请求码 */
    public static final int REQUEST_MEDIA_PROJECTION = 18;
    /** 提示文本视图 */
    private TextView view;
    /** 权限列表(未使用) */
    private ArrayList<String> permissions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new TextView(this);
        view.setText("请授予权限"); // 显示提示信息
        setContentView(view);
        requesturePermission(); // 请求权限
    }

    /**
     * 请求屏幕截图权限。
     * 检查系统版本并启动 MediaProjection 权限请求。
     */
    public void requesturePermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //5.0 之后才允许使用屏幕截图
            CustomToast.show(this, "仅支持安卓5以上系统", Toast.LENGTH_SHORT, false); // 显示不支持提示
            //TalkManAccessibilityService.getInstance().toBack();
            return;
        }
        try {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE); // 获取媒体投影管理器
            startActivityForResult(
                    mediaProjectionManager.createScreenCaptureIntent(), // 创建截屏意图
                    REQUEST_MEDIA_PROJECTION); // 启动权限请求
        } catch (Exception e) {
            e.printStackTrace();
            ScreenShot.setResultData(null); // 设置结果为 null
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_MEDIA_PROJECTION:
                if (resultCode == RESULT_OK && data != null) {
                    ScreenShot.setResultData(data); // 保存权限结果数据
                    //Toast.makeText(this,"获得权限成功",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                ScreenShot.setResultData(null); // 失败时设置为 null
        }
        finish(); // 关闭 Activity
    }

    @Override
    public void finish() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask(); // 5.0+ 从最近任务中移除
        } else {
            super.finish(); // 普通关闭
        }
    }
}
