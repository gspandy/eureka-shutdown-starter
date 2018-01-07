# 优雅停机模块
### 用于控制服务停机前的预备工作
> 适用 spring-boot项目和cloud项目
* 修改健康检查页（负载均衡设备，会停止向该节点分发请求）
* 修改SpringCloud注册中心中的该节点状态（SpringCloud调用方，会停止向该节点分发请求）
* 检查自身不再有新请求进入，且已经进入的请求都处理完毕（shutdown_timeout时长过后，会强制停机）
* 最后真正开始关机

最终实现优雅停机（停机过程中不会丢失任何一个业务请求）

### eureka-server配置

```yaml
eureka:
  server:
    response-cache-update-interval-ms: 3000 #注册中心缓存刷新时间 由于是内存间的拷贝可以设置相对较短的时间
```
### eureka-client配置

```yaml
eureka:
  instance:
    shutdown:
      broadcast-type: http #设置广播方式  http方式只要被广播到就会更新缓存 redis会检查是否依赖了该服务 没有依赖则不更新缓存
      fast: true #是否启用shutdown default true
      timeout: 30 #超时强制关闭 default 30
      interval: 5 #检测间隔 default 5
      broadcast-delay: 3 #广播缓存刷新延时需要大于等于服务端配置的缓存刷新时间 default 3
```
### endpoint 端点

- **status** 健康检查端点 200:OK 301:正在停机
- **refreshEurekaCache** 客户端缓存刷新端点 200:刷新成功 500:刷新失败 

### cloud eureka client 缓存刷新流程

![image](/doc/image/eureka客户端缓存刷新流程.png)

### boot 项目停机流程

![image](/doc/image/spring boot项目shutdown流程.png)