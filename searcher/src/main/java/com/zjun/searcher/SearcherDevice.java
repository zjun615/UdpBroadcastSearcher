package com.zjun.searcher;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;

/**
 * File Name    ：SearcherDevice
 * Description  ：搜素者设备
 * Author       ： Ralap
 * Create Date  ： 2016/9/22
 * Version      ： v2
 */
public abstract class SearcherDevice extends Thread {

    private int mUserDataMaxLen;

    private volatile boolean mOpenFlag;

    private DatagramSocket mSocket;

    /**
     * 构造函数
     * 不需要用户数据
     */
    public SearcherDevice() {
        this(0);
    }

    /**
     * 构造函数
     *
     * @param userDataMaxLen 搜索主机发送数据的最大长度
     */
    public SearcherDevice(int userDataMaxLen) {
       this.mUserDataMaxLen = userDataMaxLen;
    }

    /**
     * 打开
     * 即可以上线
     */
    public boolean open() {
        // 线程只能start()一次，重启必须重新new。因此这里也只能open()一次
        if (this.getState() != State.NEW) {
            return false;
        }

        mOpenFlag = true;
        this.start();
        return true;
    }

    /**
     * 关闭
     */
    public void close() {
        mOpenFlag = false;
    }

    @Override
    public void run() {
        printLog("设备开启");
        DatagramPacket recePack = null;
        try {
            mSocket = new DatagramSocket(SearcherConst.DEVICE_FIND_PORT);
            // 初始
            mSocket.setSoTimeout(SearcherConst.DEVICE_RECEIVE_DEFAULT_TIME_OUT);
            byte[] buf = new byte[32 + mUserDataMaxLen];
            recePack = new DatagramPacket(buf, buf.length);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        if (mSocket == null || mSocket.isClosed() || recePack == null) {
            return;
        }

        while (mOpenFlag) {
            try {
                // waiting for search from host
                mSocket.receive(recePack);
                // verify the data
                if (verifySearchData(recePack)) {
                    byte[] sendData = packData();
                    DatagramPacket sendPack = new DatagramPacket(sendData, sendData.length, recePack.getAddress(), recePack.getPort());
                    printLog("接收到请求，给主机回复信息");
                    mSocket.send(sendPack);
                    printLog("等待主机接收确认");
                    mSocket.setSoTimeout(SearcherConst.RECEIVE_TIME_OUT);
                    try {
                        mSocket.receive(recePack);
                        if (verifyCheckData(recePack)) {
                            printLog("确认成功");
                            onDeviceSearched((InetSocketAddress) recePack.getSocketAddress());
                            mOpenFlag = false;
                            break;
                        }
                    } catch (SocketTimeoutException e) {
                    }
                    mSocket.setSoTimeout(SearcherConst.DEVICE_RECEIVE_DEFAULT_TIME_OUT); // 还原连接超时
                }

            } catch (IOException e) {
            }
        }
        mSocket.close();
        printLog("设备关闭或已被找到");
    }

    /**
     * 打包响应报文
     * 协议：$ + packType(1) + userData(n)
     *
     */
    private byte[] packData() {
        byte[] data = new byte[1024];
        int offset = 0;
        data[offset++] = '$';
        data[offset++] = SearcherConst.PACKET_TYPE_FIND_DEVICE_RSP_11;

        // add userData
        byte[] userData = packUserData();
        if (userData.length + offset > data.length) {
            byte[] tmp = new byte[userData.length + offset];
            System.arraycopy(data, 0, tmp, 0, offset);
            data = tmp;
        }
        System.arraycopy(userData, 0, data, offset, userData.length);
        offset += userData.length;

        byte[] retVal = new byte[offset];
        System.arraycopy(data, 0, retVal, 0, offset);

        return retVal;
    }


    /**
     * 校验搜索数据
     * 协议：$ + packType(1) + sendSeq(4) [+ deviceIpLen(1) + deviceIp(n<=15)] [+ userData]
     *  packType - 报文类型
     *  sendSeq - 发送序列
     *  deviceIpLen - 设备IP长度
     *  deviceIp - 设备IP，仅在确认时携带
     *  userData - 用户数据
     */
    private boolean verifySearchData(DatagramPacket pack) {
        if (pack.getLength() < 6) {
            return false;
        }

        byte[] data = pack.getData();
        int offset = pack.getOffset();
        int sendSeq;
        if (data[offset++] != '$' || data[offset++] != SearcherConst.PACKET_TYPE_FIND_DEVICE_REQ_10) {
            return false;
        }
        sendSeq = data[offset++] & 0xFF;
        sendSeq |= (data[offset++] << 8 ) & 0xFF00;
        sendSeq |= (data[offset++] << 16) & 0xFF0000;
        sendSeq |= (data[offset++] << 24) & 0xFF000000;
        if (sendSeq < 1 || sendSeq > 3) {
            return false;
        }

        if (mUserDataMaxLen == 0 && offset == data.length) {
            return true;
        }

        // has userData
        byte[] userData = new byte[pack.getLength() - offset];
        System.arraycopy(data, offset, userData, 0, userData.length);
        return parseUserData(data[1], userData);
    }

    /**
     * 校验确认数据
     * 协议：$ + packType(1) + sendSeq(4) [+ deviceIpLen(1) + deviceIp(n<=15)] [+ userData]
     *  packType - 报文类型
     *  sendSeq - 发送序列
     *  deviceIpLen - 设备IP长度
     *  deviceIp - 设备IP，仅在确认时携带
     *  userData - 用户数据
     */
    private boolean verifyCheckData(DatagramPacket pack) {
        if (pack.getLength() < 6 + 1 +7) {
            return false;
        }

        byte[] data = pack.getData();
        int offset = pack.getOffset();
        int sendSeq;
        if (data[offset++] != '$' || data[offset++] != SearcherConst.PACKET_TYPE_FIND_DEVICE_CHK_12) {
            return false;
        }
        sendSeq = data[offset++] & 0xFF;
        sendSeq |= (data[offset++] << 8 ) & 0xFF;
        sendSeq |= (data[offset++] << 16) & 0xFF00;
        sendSeq |= (data[offset++] << 24) & 0xFF0000;
        if (sendSeq < 1 || sendSeq > SearcherConst.RESPONSE_DEVICE_MAX) {
            return false;
        }

        // ip
        int ipLen = data[offset++];
        if (data.length < offset + ipLen) {
            return false;
        }
        String ip = new String(data, offset, ipLen, Charset.forName("UTF-8"));
        offset += ipLen;
        printLog("Device's ip from host=" + ip);
        if (!isOwnIp(ip)) {
            return false;
        }

        if (mUserDataMaxLen == 0 && offset == data.length) {
            return true;
        }

        // has userData
        byte[] userData = new byte[pack.getLength() - offset];
        System.arraycopy(data, offset, userData, 0, userData.length);
        return parseUserData(data[1], userData);

    }

    /**
     * 打包用户数据
     * 如果调用者需要，则重写
     * @return
     */
    protected byte[] packUserData() {
        return new byte[0];
    }

    /**
     * 当设备被发现时执行
     */
    public abstract void onDeviceSearched(InetSocketAddress socketAddr);

    /**
     * 获取本机在Wifi中的IP
     * 默认都是返回true，如果需要真实验证，需调用自己重写本方法
     * @param ip 需要判断的ip地址
     * @return true-是本机地址
     */
    public boolean isOwnIp(String ip){
        return true;
    }

    /**
     * 解析用户数据
     * 默认返回true，如果调用者有自己的数据，需重写
     * @param type 类型，搜索请求or搜索确认
     * @param userData 用户数据
     * @return 解析结果
     */
    public boolean parseUserData(byte type, byte[] userData) {
        return true;
    }

    /**
     * 打印日志
     * 由调用者打印，SE和Android不同
     */
    public abstract void printLog(String log);

}
