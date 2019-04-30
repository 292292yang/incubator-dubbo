# Dubbo Project

[![Build Status](https://travis-ci.org/apache/incubator-dubbo.svg?branch=master)](https://travis-ci.org/apache/incubator-dubbo) 
[![codecov](https://codecov.io/gh/apache/incubator-dubbo/branch/master/graph/badge.svg)](https://codecov.io/gh/apache/incubator-dubbo)
[![Gitter](https://badges.gitter.im/alibaba/dubbo.svg)](https://gitter.im/alibaba/dubbo?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
![license](https://img.shields.io/github/license/alibaba/dubbo.svg)
![maven](https://img.shields.io/maven-central/v/com.alibaba/dubbo.svg)

Dubbo is a high-performance, java based RPC framework open-sourced by Alibaba. Please visit [dubbo.io](http://dubbo.io) for quick start and other information.

We are now collecting dubbo user info in order to help us to improve dubbo better, pls. kindly help us by providing yours on [issue#1012: Wanted: who's using dubbo](https://github.com/alibaba/dubbo/issues/1012), thanks :)

## Links

* [Side projects](http://github.com/dubbo)
* [Gitter channel](https://gitter.im/alibaba/dubbo)
* [Mailing list](https://groups.google.com/forum/#!forum/dubbo)
* [Dubbo user manual](http://dubbo.io/books/dubbo-user-book/)
* [Dubbo developer guide](http://dubbo.io/books/dubbo-dev-book/)
* [Dubbo admin manual](http://dubbo.io/books/dubbo-admin-book/)

## 本地jar上传到仓库命令

* 上传到本地仓库
  ```
   mvn install:install-file -DgroupId=com.lagou.plat.bi.mysql-connector-java-bin -DartifactId=mysql-connector-java-bin -Dversion=0.0.1 -Dpackaging=jar -Dfile=D:\workspace\test\lagouBI\mysql-connector-java-bin.jar
  ```
* 上传到远端仓库
  ```
   mvn deploy:deploy-file -DgroupId=com.lagou.plat.bi.mysql-connector-java-bin -DartifactId=mysql-connector-java-bin -Dversion=0.0.1 -Dpackaging=jar -Dfile=D:\workspace\test\lagouBI\mysql-connector-java-bin.jar -Durl=http://nexus.lagou.com/content/repositories/releases -DrepositoryId=releases
  ```
  


