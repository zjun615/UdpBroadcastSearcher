package com.zjun.demo.appdevice;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zjun.searcher.SearcherDevice;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;

/**
 * 设备 —— 简单demo使用
 */
public class DeviceActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_open;
    private TextView tv_info;

    private SearcherDevice mSearcherDevice;
    private boolean mIsDeviceOpen;


    private MyHandler mHandler = new MyHandler(this);
    /**
     * Handler
     */
    private static class MyHandler extends Handler {
        private WeakReference<DeviceActivity> ref;

        MyHandler(DeviceActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            DeviceActivity activity = ref.get();
            activity.tv_info.setText((CharSequence) msg.obj);

            activity.mIsDeviceOpen = false;
            activity.btn_open.setText("打开");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        initData();
        initView();
    }

    private void initData() {
        mSearcherDevice = new SearcherDevice() {
            @Override
            public void onDeviceSearched(InetSocketAddress inetSocketAddress) {
                Message msg = Message.obtain();
                msg.obj = "设备已被主机搜索到，主机IP:" + inetSocketAddress.getAddress().getHostAddress()
                        + "-" + inetSocketAddress.getPort();
                mHandler.sendMessage(msg);
            }

            @Override
            public void printLog(String s) {
                Log.i(SearcherDevice.class.getSimpleName(), s);
            }
        };
    }

    private void initView() {
        btn_open = $(R.id.btn_open);
        tv_info = $(R.id.tv_info);

        btn_open.setOnClickListener(this);
    }

    private <V extends View> V $(int id) {
        return (V) findViewById(id);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_open:
                if (!mIsDeviceOpen) {
                    if (mSearcherDevice.open()) {
                        mIsDeviceOpen = true;
                        btn_open.setText("关闭");
                    } else {
                        Toast.makeText(this, "设备已打开过，无法再打开了", Toast.LENGTH_SHORT).show();
                        mSearcherDevice = null;
                    }
                } else {
                    mIsDeviceOpen = false;
                    mSearcherDevice.close();
                    btn_open.setText("打开");
                }
        }
    }

}
