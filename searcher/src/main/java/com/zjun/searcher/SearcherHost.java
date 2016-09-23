package com.zjun.searcher;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

/**
 * File Name    ： SearcherHost
 * Description  ： 搜索主机
 * Author       ： Ralap
 * Create Date  ： 2016/9/22
 * Version      ： v2
 */
public abstract class SearcherHost<T extends SearcherHost.DeviceBean> extends Thread {

    private int mUserDataMaxLen;
    private Class<T> mDeviceClazz;

    private DatagramSocket mHostSocket;
    private Set<T> mDeviceSet;
    private DatagramPacket mSendPack;

    private byte mPackType;
    private String mDeviceIP;

    public SearcherHost() {
        this(0, DeviceBean.class);
    }

    public SearcherHost(int userDataMaxLen, Class clazz) {
        mDeviceClazz = clazz;
        mUserDataMaxLen = userDataMaxLen;
        mDeviceSet = new HashSet<>();

        try {
            mHostSocket = new DatagramSocket();
            // 设置接收超时时间
            mHostSocket.setSoTimeout(SearcherConst.RECEIVE_TIME_OUT);

            byte[] sendData = new byte[1024];
            InetAddress broadIP = InetAddress.getByName("255.255.255.255");
            mSendPack = new DatagramPacket(sendData, sendData.length, broadIP, SearcherConst.DEVICE_FIND_PORT);
        } catch (SocketException | UnknownHostException e) {
            printLog(e.toString());
            if (mHostSocket != null) {
                mHostSocket.close();
            }
        }
    }

    /**
     * 开始搜索
     * @return true-正常启动，false-已经start()启动过，无法再启动。若要启动需重新new
     */
    public boolean search() {
        if (this.getState() != State.NEW) {
            return false;
        }

        this.start();
        return true;
    }

    @Override
    public void run() {
        if (mHostSocket == null || mHostSocket.isClosed() || mSendPack == null) {
            return;
        }

        try {
            // ready
            onSearchStart();

            // start to search
            for (int i = 0; i < 3; i++) {
                // 发送搜索广播
                mPackType = SearcherConst.PACKET_TYPE_FIND_DEVICE_REQ_10;
                mSendPack.setData(packData(i + 1));
                mHostSocket.send(mSendPack);

                // 监听来信
                byte[] receData = new byte[2 + mUserDataMaxLen];
                DatagramPacket recePack = new DatagramPacket(receData, receData.length);
                try {
                    // 最多接收250个，或超时跳出循环
                    int rspCount = SearcherConst.RESPONSE_DEVICE_MAX;
                    while (rspCount-- > 0) {
                        recePack.setData(receData);
                        mHostSocket.receive(recePack);
                        if (recePack.getLength() > 0) {
                            mDeviceIP = recePack.getAddress().getHostAddress();
                            if (parsePack(recePack)) {
                                printLog("a response from：" + mDeviceIP);
                                // 发送一对一的确认信息。使用接收报，因为接收报中有对方的实际IP，发送报时广播IP
                                mPackType = SearcherConst.PACKET_TYPE_FIND_DEVICE_CHK_12;
                                recePack.setData(packData(rspCount)); // 注意：设置数据的同时，把recePack.getLength()也改变了
                                mHostSocket.send(recePack);
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                }
                printLog(String.format("the %dth search finished", i));

            }
            // finish
            onSearchFinish(mDeviceSet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (mHostSocket != null) {
                mHostSocket.close();
            }
        }

    }

    /**
     * 搜索开始时执行
     */
    public abstract void onSearchStart();

    /**
     * 打包搜索时的用户数据
     * packed the userData by caller when searching
     */
    protected byte[] packUserData_Search() {
        return new byte[0];
    }

    /**
     * 打包确认时的用户数据
     * packed userData by caller when checking，and override the method when pack
     */
    protected byte[] packUserData_Check() {
        return new byte[0];
    }


    /**
     * 解析数据
     * parse if have userData
     * @param type 数据类型
     * @param device 设备
     * @param userData 数据
     *
     * @return return the result of parse, true if parse success, else false
     */
    public boolean parseUserData(byte type, T device, byte[] userData) {
        return true;
    }

    /**
     * 搜索结束后执行
     * @param deviceSet 搜索到的设备集合
     */
    public abstract void onSearchFinish(Set deviceSet);

    /**
     * 打印日志
     * 由调用者打印，SE和Android不同
     */
    public abstract void printLog(String log);


    /**
     * 解析报文
     * 协议：$ + packType(1) + userData(n)
     *
     *  @param pack 数据报
     */
    private boolean parsePack(DatagramPacket pack) {
        if (pack == null || pack.getAddress() == null) {
            return false;
        }

        String ip = pack.getAddress().getHostAddress();
        int port = pack.getPort();
        for (T d : mDeviceSet) {
            if (d.getIp().equals(ip)) {
                return false;
            }
        }

        // 解析头部数据
        byte[] data = pack.getData();
        int dataLen = pack.getLength();

        if (dataLen < 2 || data[0] != '$' || data[1] != SearcherConst.PACKET_TYPE_FIND_DEVICE_RSP_11) {
            return false;
        }

        T device = null;

        try {
            Constructor constructor = mDeviceClazz.getDeclaredConstructor(String.class, int.class);
            device = (T) constructor.newInstance(ip, port);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        if (device == null) {
            return false;
        }

        // or use this reflect
//        try {
//            device = mDeviceClazz.newInstance();
//        } catch (InstantiationException | IllegalAccessException e) {
//            e.printStackTrace();
//        }
//
//        if (device == null) {
//            return false;
//        }
//        device.setIp(ip);
//        device.setPort(port);

        if (mUserDataMaxLen == 0 && dataLen == 2) {
            return mDeviceSet.add(device);
        }

        // 解析用户数据
        int userDataLen = dataLen - 2;
        byte[] userData = new byte[userDataLen];
        System.arraycopy(data, 2, userData, 0, userDataLen);

        return parseUserData(data[1], device, userData) && mDeviceSet.add(device);
    }

    /**
     * 打包搜索报文
     * 协议：$ + packType(1) + sendSeq(4) [+ deviceIpLen(1) + deviceIp(n<=15)] [+ userData]
     *  packType - 报文类型
     *  sendSeq - 发送序列
     *  deviceIpLen - 设备IP长度
     *  deviceIp - 设备IP，仅在确认时携带
     *  userData - 用户数据
     *
     *  @param seq 发送序列号
     */
    private byte[] packData(int seq) {
        byte[] data = new byte[1024];
        int offset = 0;

        // 打包数据头部
        data[offset++] = '$';

        data[offset++] = mPackType;

        seq = seq == 3 ? 1 : ++seq; // can't use findSeq++
        data[offset++] = (byte) seq;
        data[offset++] = (byte) (seq >> 8 );
        data[offset++] = (byte) (seq >> 16);
        data[offset++] = (byte) (seq >> 24);

        switch (mPackType) {
            case SearcherConst.PACKET_TYPE_FIND_DEVICE_REQ_10: {
                // userData
                byte[] userData = packUserData_Search();
                if (data.length < offset + userData.length) {
                    byte[] tmp = new byte[offset + userData.length];
                    System.arraycopy(data, 0, tmp, 0, offset);
                    data = tmp;
                }
                System.arraycopy(userData, 0, data, offset, userData.length);
                offset += userData.length;
                break;
            }
            case SearcherConst.PACKET_TYPE_FIND_DEVICE_CHK_12: {
                // deviceIp
                byte[] ips = mDeviceIP.getBytes(Charset.forName("UTF-8"));
                data[offset++] = (byte) ips.length;
                System.arraycopy(ips, 0, data, offset, ips.length);
                offset += ips.length;

                // userData
                byte[] userData = packUserData_Check();
                if (data.length < offset + userData.length) {
                    byte[] tmp = new byte[offset + userData.length];
                    System.arraycopy(data, 0, tmp, 0, offset);
                    data = tmp;
                }
                System.arraycopy(userData, 0, data, offset, userData.length);
                offset += userData.length;
                break;
            }
            default:
        }

        byte[] result = new byte[offset];
        System.arraycopy(data, 0, result, 0, offset);
        return result;
    }


    /**
     * 设备Bean
     * 只要IP一样，则认为是同一个设备
     */
    public static class DeviceBean{
        String ip;      // IP地址
        int port;       // 端口

        public DeviceBean(){}

        public DeviceBean(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public int hashCode() {
            return ip.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DeviceBean) {
                return this.ip.equals(((DeviceBean)o).getIp());
            }
            return super.equals(o);
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

    }
}
