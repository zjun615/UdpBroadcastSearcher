# UdpBroadcastSearcher —— UDP广播搜索者
用于局域网中，在不知道设备的内网IP的情况下，通过UDP广播搜索局域网内所有的的设备

## 一、导入
### 方法1：直接导入源码
  把工程下的searcher文件夹里的源码全部导入即可
  
### 方法2：导入jar包
  在工程下有一个searcher.jar包，拷贝到自己的工程中
  
  
  
  
## 二、使用方法

### 2.1 不携带用户数据的使用（最简单的使用）

只要实现内部的抽象方法即可。

**主机：**
```java
new SearcherHost() {
    @Override
    public void onSearchStart() {
        ...
    }

    @Override
    public void onSearchFinish(Set deviceSet) {
        //所有设备集合都在deviceSet中，通过getIp()可获取设备IP，getPort()获取端口
        ...
    }

    @Override
    public void printLog(String log) {
        ...
    }
}.search();
```

**设备：**
```java
// 创建设备
SearcherDevice mSearcherDevice = new SearcherDevice() {
    @Override
    public void onDeviceSearched(InetSocketAddress inetSocketAddress) {
        // 主机IP: inetSocketAddress.getAddress().getHostAddress()
        // 主机端口：inetSocketAddress.getPort()
        ...
    }

    @Override
    public void printLog(String s) {
        ...
    }
};

// 打开设备
mSearcherDevice.open();

// 关闭设备
mSearcherDevice.close();
```

demo参考工程下的AppHost和AppDevice

###2.2 携带用户数据的使用

若需要携带用户数据，必须重写类的用户数据打包与解包方法。

**主机：**

1. 【可选】在构造方法中，指定泛型的具体类型。若不指定，parseUserData()也许需要强转。
2. 在构造方法中，设置用于接收设备用户数据的最大字节长度（≥设备发送的用户数据长度）
3. 在构造方法中，设置设备类的字节码，没有重写，则写SearcherHost.DeviceBean.class（用于内部通过反射创建实例对象）
4. 重写两个用户数据打包方法： 
    packUserData_Search()和packUserData_Check()
5. 重写用户数据解析方法： 
    parseUserData(byte type, MyDevice device, byte[] userData)

```java
SearcherHost searcherHost = new SearcherHost<MyDevice>(1024, MyDevice.class) {
    @Override
    public void onSearchStart() {
        ...
    }

    @Override
    public void onSearchFinish(Set deviceSet) {
        ...
    }

    @Override
    public void printLog(String log) {
        ...
    }

    /**
     * 重写以下3个方法
     */

    /**
     * 打包搜索时数据
     */
    @Override
    protected byte[] packUserData_Search() {
       ...
    }

    /**
     * 打包核对时的数据
     */
    @Override
    protected byte[] packUserData_Check() {
        ...
    }

    /**
     * 解析用户数据
     * @param type 类型。搜索申请or搜索确认
     * @param device 设备
     * @param userData 数据
     *
     * @return true-解析成功
     */
    @Override
    public boolean parseUserData(byte type, MyDevice device, byte[] userData) {
        ...
    }

};

// 开始搜索
searcherHost.search();
```

**设备：**

1. 在构造方法中，设置用于接收设备用户数据的最大字节长度（≥主机发送的用户数据长度）
2. 重写设备响应时的用户数据打包方法： 
    `packUserData()`
3. 重写用户数据解析方法： 
    `parseUserData(byte type, byte[] userData)`
4. 【可选】重写判断是否为本机ip地址的方法（主要用于判断主机确认返回的ip地址是否正确，默认返回true）： 
    `isOwnIp(String ip)`

```java
SearcherDevice searcherDevice = new SearcherDevice(1024) {
    @Override
    public void onDeviceSearched(InetSocketAddress inetSocketAddress) {
        ...
    }

    @Override
    public void printLog(String s) {
        ...
    }


    /**
     * 重写以下2个方法
     */

    /**
     * 响应时的打包数据
     */
    @Override
    protected byte[] packUserData() {
        ...
    }

   /**
    * 解析用户数据
    * @param type 类型，搜索请求or搜索确认
    * @param userData 用户数据
    * @return 解析结果是否成功
    */
    @Override
    public boolean parseUserData(byte type, byte[] userData) {
        ...
    }

    /**
     * 判断ip是否是本机ip
     * @param ip 判断的ip地址
     * @return
     */
    @Override
    public boolean isOwnIp(String ip) {
        ...
    }
};

searcherDevice.open();
```

demo参考工程下的se_host和se_device
