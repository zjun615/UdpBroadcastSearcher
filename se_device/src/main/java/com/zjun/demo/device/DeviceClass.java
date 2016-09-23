package com.zjun.demo.device;

import com.zjun.searcher.SearcherConst;
import com.zjun.searcher.SearcherDevice;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * 设备 —— 携带用户自定义数据的demo
 */
public class DeviceClass {
    private static final byte DEVICE_TYPE_NAME_21 = 0x21;
    private static final byte DEVICE_TYPE_ROOM_22 = 0x22;

    public static void main(String[] args) {
        SearcherDevice searcherDevice = new SearcherDevice(1024) {
            @Override
            public void onDeviceSearched(InetSocketAddress inetSocketAddress) {
                String hostIP = inetSocketAddress.getAddress().getHostAddress();
                int hostPort = inetSocketAddress.getPort();
                printf("I am found by host: " + hostIP + "-" + hostPort);
            }

            @Override
            public void printLog(String s) {
                printf("Device is ready..." + s);
            }


            /**
             * 重写以下2个方法
             */

            /**
             * 响应时的打包数据
             * dataType(1) + len(4) + data(n)
             */
            @Override
            protected byte[] packUserData() {
                String name = "LED灯";
                String room = "客厅";
                try {
                    byte[] nameBytes = name.getBytes("UTF-8");
                    byte[] roomBytes = room.getBytes("UTF-8");
                    byte[] data = new byte[5 + nameBytes.length + 5 + roomBytes.length];
                    int offset = 0;

                    data[offset++] = DEVICE_TYPE_NAME_21;
                    data[offset++] = (byte) nameBytes.length;
                    data[offset++] = (byte) (nameBytes.length >> 8);
                    data[offset++] = (byte) (nameBytes.length >> 16);
                    data[offset++] = (byte) (nameBytes.length >> 24);
                    System.arraycopy(nameBytes, 0 , data, offset, nameBytes.length);
                    offset += nameBytes.length;

                    data[offset++] = DEVICE_TYPE_ROOM_22;
                    data[offset++] = (byte) roomBytes.length;
                    data[offset++] = (byte) (roomBytes.length >> 8);
                    data[offset++] = (byte) (roomBytes.length >> 16);
                    data[offset++] = (byte) (roomBytes.length >> 24);
                    System.arraycopy(roomBytes, 0 , data, offset, roomBytes.length);

                    return data;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return super.packUserData();
            }

            /**
             * 解析用户数据
             * len(4) + data(n)
             */
            @Override
            public boolean parseUserData(byte type, byte[] userData) {
                if (userData.length < 4) {
                    return false;
                }
                int len = userData[0] & 0xFF;
                len |= (userData[1] << 8) & 0xFF00;
                len |= (userData[2] << 16) & 0xFF0000;
                len |= (userData[3] << 24) & 0xFF000000;

                if (len != userData.length - 4) {
                    return false;
                }

                String content = null;
                try {
                    content = new String(userData, 4, len, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (content == null) {
                    return false;
                }

                switch (type) {
                    case SearcherConst.PACKET_TYPE_FIND_DEVICE_REQ_10:
                        printf("主机请求数据：" + content);
                        break;
                    case SearcherConst.PACKET_TYPE_FIND_DEVICE_CHK_12:
                        printf("主机核对数据：" + content);
                        break;
                }
                return true;
            }

            /**
             * 判断ip是否是本机ip
             * @param ip 判断的ip地址
             * @return
             */
            @Override
            public boolean isOwnIp(String ip) {
                if (ip == null) {
                    return false;
                }
                String ownIp = null;
                try {
                    ownIp = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                return ip.equals(ownIp);
            }
        };

        searcherDevice.open();
    }

    private static void printf(String format, Object... objs) {
        System.out.printf(format + "\n", objs);
    }
}
