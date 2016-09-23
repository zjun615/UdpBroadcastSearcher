package com.zjun.demo.searcher;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zjun.searcher.SearcherHost;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 搜索主机 —— 简单demo
 */
public class HostActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MESSAGE_SEARCH_START= 1;
    private static final int MESSAGE_SEARCH_FINISH = 2;

    private Button btn_search;
    private TextView tv_device;

    private List<SearcherHost.DeviceBean> mDeviceList = new ArrayList<>();

    private MyHandler mHandler = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();
        initView();
    }

    private void initData() {
    }

    private void initView() {
        btn_search = $(R.id.btn_search);
        tv_device = $(R.id.tv_device);

        btn_search.setOnClickListener(this);
    }

<<<<<<< HEAD
=======


>>>>>>> 14e21603f58a2ff67878bb7ea5e9993a2bfba80d
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_search:
                startToSearch();
        }
    }

    /**
     * 开始搜索
     */
    private void startToSearch() {
        new SearcherHost() {
            @Override
            public void onSearchStart() {
                pushMessage(MESSAGE_SEARCH_START);
            }

            @Override
            public void onSearchFinish(Set deviceSet) {
                mDeviceList.clear();
                mDeviceList.addAll(deviceSet);
                pushMessage(MESSAGE_SEARCH_FINISH);
            }

            @Override
            public void printLog(String log) {
                Log.i(SearcherHost.class.getSimpleName(), "Searching..." + log);
            }
        }.search();
    }

    private void pushMessage(int what) {
        mHandler.sendEmptyMessage(what);
    }

    /**
     * Handler
     */
    private static class MyHandler extends Handler {
        private WeakReference<HostActivity> ref;

        MyHandler(HostActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            HostActivity activity = ref.get();
            switch (msg.what) {
                case MESSAGE_SEARCH_START:
                    activity.btn_search.setText("正在搜索...");
                    activity.tv_device.setText("");
                    break;
                case MESSAGE_SEARCH_FINISH:
                    activity.btn_search.setText("搜索");
                    for (SearcherHost.DeviceBean d : activity.mDeviceList) {
                        String show = d.getIp() + " : " + d.getPort();
                        activity.tv_device.append(show + "\n\n");
                    }

            }
        }
    }

    private <V extends View> V $(int id) {
        return (V) findViewById(id);
    }
}
