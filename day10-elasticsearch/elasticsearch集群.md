# 0.Elasticsearch集群搭建

Elasticsearch 和大多数的组件是一样，你若想要她全心全意的为你服务，你就必须满足她的需求，毕竟巧妇也难为无米之炊嘛。 Elasticsearch 的要求不高，仅仅需要合适的操作系统和JVM版本，这是最基本的要求了，如果无法满足还请放开她。

# 1.环境依赖

## 1.1操作系统版本依赖

若没有特殊说明，以后文章中ES的操作系统运行环境默认为 :

    CentOS Linux release 7.2.1511 (Core)
## 1.2JVM版本依赖

若没有特殊说明，以后文章中运行ES的Java版本默认为 :

    Java version 1.8.0_102



# 2.安装

## 2.1Elasticsearch

[Elasticsearch下载地址](https://www.elastic.co/downloads/elasticsearch)

> 挑选合适的Elasticsearch版本

如何选择Elasticsearch 版本与如何选择找女朋友的原理是一样的。 新的版本、年轻的姑娘相信大家都喜欢.但是新的姑娘大部都分经历少、 可能家务也不会做，如果这缺点你有接受那没有问题。新的Elasticsearch 版本也是一样， 新的Elasticsearch 插件的支持可能没有那么好，新特性未被实际的生产环境验证过，如果 这些都能容忍，那么使用最新的Elasticsearch版本是最好的选择。

## 2.2解压到指定位置

1. mkdir -p $ES_HOME_PARENT  //创建用于存放elasticsearch组件的父目录
2. tar -zxvf elasticsearch-6.1.1.tar.gz  -C $ES_HOME_PARENT 
3. cd  $ES_HOME_PARENT
4. mv elasticsearch-6.1.1 es-6.1.1_benchmark611 //修改个名称
5. mkdir -p $ES_DATA_PATH/store/es-6.1.1_benchmark611  //用于存放Elasticsearch 数据
6. mkdir -p $ES_DATA_PATH/logs/es-6.1.1_benchmark611 //用于存放Elasticsearch 日志 

## 2.3启动前检查

Linux 系统参数检查
为什么要设置这些系统参数呢？如果不设置会对集群产生哪些影响呢？

- **文件句柄( File Descriptors)** 如果设置过小的文件句柄，Elasticsearch 将无法与集群进行通信以及创建新的索引。

- **内存锁定(Memory Lock)** 如果没有锁定内存，操作系统会扫描不使用的内存并把他交换到磁盘上，需要的时候 在加载到内存中。这样的操作会引起磁盘抖动，对于低延时的请求会造成比较大的伤害。 因为JVM已经有垃圾回收器，所以不需要操作系统层面的策略来管理内存，在这里我们 锁定内存来阻止系统层面插手内存管理 。

- **用户线程限制（User maximum number of threads）** Elasticsearch 中有各种线程池，每种线程池里都会运行着不同的任务，如果操作系统支持的用户线程数据设置的较低， 集群将无法创建更多的线程运行任务，导致集群无法正常工作。

- **虚拟内存（Virtual Memory）** 操作系统默认virtual memory都是unlimited,如果不是就重新设置，主要与内存映射总数配置同时设置，加速访问索引数据访问。

设置 文件句柄( File Descriptors) 、 内存锁定(Memory Lock)、用户线程限制（User maximum number of threads）
如下图，我已经修改了操作系统设置,如果你还没设置请用下面的命令设置 查询命令（ulimit -a） 操作系统设置

修改命令（执行此命令需要root 权限）

```
vim /etc/security/limits.conf 
```

内容：

```
esadmin soft nproc 40000
esadmin hard nproc 40000
esadmin soft nofile 65536
esadmin hard nofile 65536
esadmin soft  memlock -1
esadmin hard memlock -1
```


内存映射总数(Max Map Count)

内存映射总数(Max Map Count) Elasticsearch使用mmap把索引映射到虚拟内存空间，Elasticsearch 同样也需求足够的数据来创建内存映射区域。 Elasticsearch 要求最大内存映射总数至少设置 262144，过小可能无法完成索引的映射

修改命令（执行此命令需要root 权限）

```
sysctl -w vm.max_map_count=262144
```

除了以上只是启动前更多需要检查的配置如下

ES启动前检查 （ ← 右击在新标签页打开 ^-^）

集群运行最少的参数配置
这是Master Node 配置参数

```
vim $ES_HOME/config/elasticsearch.yml
```



# 3.集群设定

## 3.1.主节点

```sh
# ======================== ES 参数配置 =========================
#
#
# ------------------------ 集群设定 ----------------------------
#
# 集群名称 
 cluster.name: benchmark612
#
# ------------------------ 节点设定 ----------------------------
#
# 节点名称
 node.name: ${HOSTNAME}
#
# 节点角色
 node.master: true
 node.data: false
 node.ingest: false
#
# ------------------------ 路径设定 ----------------------------
#
# 索引、日志存放路径
 path:
   data: /data/store/es-6.1.2_benchmark612
   logs: /data/logs/es-6.1.2_benchmark612
#
# ------------------------ 内存设定 ----------------------------
#
#
# 锁定内存，阻止操作系统管理内存，可以有效的防止内存数据被交换到磁盘空间，
#   交换过程中磁盘会抖动，会对性能产生较大的影响。因为ES是基于JAVA开发的
#   可以能过垃圾回收器来单独管理内存，所以关闭操作系统级别的内存管理可以
#   提升性能
 bootstrap.memory_lock: true
#
# ------------------------ 网络设定 ----------------------------
#
# 绑定节点上的所有网络接口，用于接收通过任意网卡传输过来的请求
 network.bind_host: 0.0.0.0
#
# 绑定一个网络接口(网卡)，用于集群内部节点通信(一般选择吞吐量大的网卡)
 network.publish_host: _eth0:ipv4_
#
# HTTP 通信端口
 http.port: 50000
#
# TCP 通信端口
 transport.tcp.port: 50100
#
# --------------------------------- 集群发现 模块 ----------------------------------
#
# 集群初始化连接列表，节点启动后，首先通过连接初始化列表里的地址去发现集群。
 discovery.zen.ping.unicast.hosts: ["20.120.203.74:50100","20.120.203.76:50100","20.120.203.81:50100","20.120.203.84:50100","20.120.203.85:50100"]
#
# 为了防止集群脑裂，目前的策略是当且仅当节点有超过半数的master候选者存活时(目前是2台，可以完成选举)，集群才会进行master选举
 discovery.zen.minimum_master_nodes: 2
#
# ---------------------------------- 其它 -----------------------------------
#
# 关闭操作系统内核验证(我的操作系统没有升级，如果不关闭验证则无法启动)
 bootstrap.system_call_filter: false
#
# ------------------------ HTTP ----------------------------
#
# 是否支持跨域访问资源
 http.cors.enabled: true
#
#
#允许访问资源的类型
 http.cors.allow-origin: "*"
#
#
# 允许HTTP请求的方法类型 
 http.cors.allow-methods: OPTIONS,HEAD,GET,POST,PUT,DELETE
#
# 允许HTTP请求头返回类型
 http.cors.allow-headers: X-Requested-With,Content-Type,Content-Length,Authorization,Content-Encoding,Accept-Encoding
#
# 支持HTTP访问API 总开关
 http.enabled: true
#
#
```



## 3.2.从节点

```sh
# ======================== ES 参数配置 =========================
#
#
# ------------------------ 集群设定 ----------------------------
#
# 集群名称 
 cluster.name: benchmark612
#
# ------------------------ 节点设定 ----------------------------
#
# 节点名称
 node.name: ${HOSTNAME}
#
# 节点角色
 node.master: true
 node.data: false
 node.ingest: false
#
# ------------------------ 路径设定 ----------------------------
#
# 索引、日志存放路径
 path:
   data: /data/store/es-6.1.2_benchmark612
   logs: /data/logs/es-6.1.2_benchmark612
#
# ------------------------ 内存设定 ----------------------------
#
#
# 锁定内存，阻止操作系统管理内存，可以有效的防止内存数据被交换到磁盘空间，
#   交换过程中磁盘会抖动，会对性能产生较大的影响。因为ES是基于JAVA开发的
#   可以能过垃圾回收器来单独管理内存，所以关闭操作系统级别的内存管理可以
#   提升性能
 bootstrap.memory_lock: true
#
# ------------------------ 网络设定 ----------------------------
#
# 绑定节点上的所有网络接口，用于接收通过任意网卡传输过来的请求
 network.bind_host: 0.0.0.0
#
# 绑定一个网络接口(网卡)，用于集群内部节点通信(一般选择吞吐量大的网卡)
 network.publish_host: _eth0:ipv4_
#
# HTTP 通信端口
 http.port: 50000
#
# TCP 通信端口
 transport.tcp.port: 50100
#
# --------------------------------- 集群发现 模块 ----------------------------------
#
# 集群初始化连接列表，节点启动后，首先通过连接初始化列表里的地址去发现集群。
 discovery.zen.ping.unicast.hosts: ["20.120.203.74:50100","20.120.203.76:50100","20.120.203.81:50100","20.120.203.84:50100","20.120.203.85:50100"]
#
# 为了防止集群脑裂，目前的策略是当且仅当节点有超过半数的master候选者存活时(目前是2台，可以完成选举)，集群才会进行master选举
 discovery.zen.minimum_master_nodes: 2
#
# ---------------------------------- 其它 -----------------------------------
#
# 关闭操作系统内核验证(我的操作系统没有升级，如果不关闭验证则无法启动)
 bootstrap.system_call_filter: false
#
# ------------------------ HTTP ----------------------------
#
# 是否支持跨域访问资源
 http.cors.enabled: true
#
#
#允许访问资源的类型
 http.cors.allow-origin: "*"
#
#
# 允许HTTP请求的方法类型 
 http.cors.allow-methods: OPTIONS,HEAD,GET,POST,PUT,DELETE
#
# 允许HTTP请求头返回类型
 http.cors.allow-headers: X-Requested-With,Content-Type,Content-Length,Authorization,Content-Encoding,Accept-Encoding
#
# 支持HTTP访问API 总开关
 http.enabled: true
#
#
```

