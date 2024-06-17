# this is an idea plugin that able to restart the spring boot project opened on idea
---
author: huang xiongbin
title: idea 项目重启器
date: 2024-06-17

#### 介绍

1、这是个idea的插件，可以实现从外部发送http命令重启你打开的idea spring boot结构的项目，当然你可以修改以支持更多的项目重启
2、因为整个项目组开发和维护的项目数量有点多，通常都是打开好几个项目同步开发，经常就碰到需要重启，部署，核心服打依赖包的情况，就比较头疼，所以想通过一种类似运维大盘的方式来管理这些项目的devops操作。
所以想到了用python
开发一个桌面app，然后添加项目到这个app中进行管理，实现在大盘中一键重启，一键部署，一键打核心服依赖包（涉及切换分支，重启，并下载dev下的swagger文档）、一键打开dev或者beta环境的项目swagger文档页面等操作。哈哈哈哈跑题了
3、言归正传，比较关键的一点是实现在python 桌面应用对idea的项目进行重启，因为是多个项目打开并行开发，所以想到开发一个插件

1. 插件监听一个组播端口，由python桌面端发送"restart ${项目名}"报文到组播地址，230.0.0.0:43211
2. 插件接收到命令后，提取出项目名称，然后刷选出当前idea打开的项目，并刷选出命令要重启的项目，
3. 查询到该项目的spring boot Run/Debug Configurations
   刷选出SpringBootApplicationRunConfiguration（有需要的小伙伴可以自行修改来支持其他类型的项目）进行重启并DEBUG（有需要也可以改成RUN），

#### 软件架构

1. idea plugin listening multi broadcast 230.0.0.0:43211
2. client send "restart ${项目名}" package to 230.0.0.0:43211
3. plugin restart the project which is opened on idea

#### 安装教程

1. ./gradlew build -x buildSearchableOptions --stacktrace
2. 在idea -> Settings -> plugins -> install on disk ,选中插件所在根目录下build\libs\restart-1.0-SNAPSHOT.jar 即可
3. 修改源码后得重复1，2步骤

 

#### 参与贡献

1. Fork 本仓库
2. 新建 Feat_xxx 分支
3. 提交代码
4. 新建 Pull Request


