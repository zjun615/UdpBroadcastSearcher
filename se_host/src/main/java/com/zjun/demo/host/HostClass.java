package com.zjun.demo.host;

import com.zjun.searcher.SearcherConst;
import com.zjun.searcher.SearcherHost;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 主机 —— 携带用户自定义数据的demo
 */
public class HostClass {
    private static final byte DEVICE_TYPE_NAME_21 = 0x21;
    private static final byte DEVICE_TYPE_ROOM_22 = 0x22;

    public static void main(String[] args) {
        SearcherHost searcherHost = new SearcherHost<MyDevice>(1024, MyDevice.class) {
            @Override
            public void onSearchStart() {
                printf("start to search");
            }

            @Override
            public void onSearchFinish(Set deviceSet) {
                List<DeviceBean> deviceList = new ArrayList<>();
                deviceList.addAll(deviceSet);
                int count = 1;
                for (DeviceBean d : deviceList) {
                    if (d instanceof MyDevice) {
                        printf("%d设备：%s", count++, d.toString());
                    } else {
                        printf("%d匿名设备：%s_-_%s", count++, d.getIp(), d.getPort());
                    }
                }
            }

            @Override
            public void printLog(String log) {
                printf("host is searching...%s", log);
            }

            /**
             * 重写以下3个方法
             */

            /**
             * 打包搜索时数据
             * len(4) + data(n)
             */
            @Override
            protected byte[] packUserData_Search() {
                byte[] data = null;
                try {
                    data = "I am Server, I am searching device. Are you?".getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (data == null || data.length == 0) {
                    return super.packUserData_Search();
                }

                int len = data.length;
                byte[] userData = new byte[4 + len];
                userData[0] = (byte) len;
                userData[1] = (byte) (len >> 8);
                userData[2] = (byte) (len >> 16);
                userData[3] = (byte) (len >> 24);
                System.arraycopy(data, 0, userData, 4, len);
                return userData;
            }

            /**
             * 打包核对时的数据
             * len(4) + data(n)
             */
            @Override
            protected byte[] packUserData_Check() {
                byte[] data = null;
                try {
                    data = "I am Server, I received your response, and remember you.".getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (data == null || data.length == 0) {
                    return super.packUserData_Check();
                }

                int len = data.length;
                byte[] userData = new byte[4 + len];
                userData[0] = (byte) len;
                userData[1] = (byte) (len >> 8);
                userData[2] = (byte) (len >> 16);
                userData[3] = (byte) (len >> 24);
                System.arraycopy(data, 0, userData, 4, len);
                return userData;
            }

            /**
             * 解析用户数据
             * dataType(1) + len(4) + data(n)
             * @param type 类型
             * @param device 设备
             * @param userData 数据
             *
             * @return true-解析成功
             */
            @Override
            public boolean parseUserData(byte type, MyDevice device, byte[] userData) {
                switch (type) {
                    case SearcherConst.PACKET_TYPE_FIND_DEVICE_RSP_11:
                        if (userData.length < 5) {
                            return false;
                        }
                        int offset = 0;
//                        MyDevice newDevice = new MyDevice(device.getIp(), device.getPort());
                        while (offset + 5 < userData.length) {
                            byte dataType = userData[offset++];
                            int len = (userData[offset++] & 0xFF)
                                    | ((userData[offset++] & 0xFF) << 8)
                                    | ((userData[offset++] & 0xFF) << 16)
                                    | ((userData[offset++] & 0xFF) << 24);
                            if (len + offset > userData.length) {
                                return false;
                            }
                            switch (dataType) {
                                case DEVICE_TYPE_NAME_21:
                                    String name = null;
                                    try {
                                        name = new String(userData, offset, len, "UTF-8");
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    if (name != null) {
                                        device.setName(name);
                                    }
                                    break;
                                case DEVICE_TYPE_ROOM_22:
                                    String room = null;
                                    try {
                                        room = new String(userData, offset, len, "UTF-8");
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    if (room != null) {
                                        device.setRoom(room);
                                    }
                                    break;
                                default:
                            }
                            offset += len;
                        }
                        break;

                    default:
                }
                return true;
            }

        };

        searcherHost.search();
    }

    /**
     * 功能扩展设备
     * 添加设备名称和房间
     */
    public static class MyDevice extends SearcherHost.DeviceBean {
        private String name;
        private String room;

        public MyDevice(){}

        public MyDevice(String ip, int port) {
            super(ip, port);
        }

        public MyDevice(String ip, int port, String name, String room) {
            this(ip, port);
            this.name = name;
            this.room = room;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRoom() {
            return room;
        }

        public void setRoom(String room) {
            this.room = room;
        }

        @Override
        public String toString() {
            return getName() + "[" + getRoom() + "," + getIp() + "-" + getPort()+ "]";
        }
    }

    private static void printf(String format, Object... objs) {
        System.out.printf(format + "\n", objs);
    }
}
