package com.zjun.searcher;

/**
 * File Name    : SearcherConst
 * Description  : 搜索者常量
 * Author       : Ralap
 * Create Date  : 2016/9/22
 * Version      : v1
 */
public interface SearcherConst {
    int DEVICE_FIND_PORT = 9000; // 设备监听端口
    int RECEIVE_TIME_OUT = 1500; // 接收超时时间
    int RESPONSE_DEVICE_MAX = 250; // 响应设备的最大个数，防止UDP广播攻击

    byte PACKET_TYPE_FIND_DEVICE_REQ_10 = 0x10; // 搜索请求
    byte PACKET_TYPE_FIND_DEVICE_RSP_11 = 0x11; // 搜索响应
    byte PACKET_TYPE_FIND_DEVICE_CHK_12 = 0x12; // 搜索确认

    int DEVICE_RECEIVE_DEFAULT_TIME_OUT = 5000; // 设备默认的接收超时时间

}
