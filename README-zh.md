# cozo-lib-java

[![java](https://img.shields.io/maven-central/v/io.github.cozodb/cozo_java)](https://mvnrepository.com/artifact/io.github.cozodb/cozo_java)

[Cozo](https://www.cozodb.org) 数据库的 Java 库。

本文叙述的是如何安装设置库本身。有关如何使用 CozoDB（CozoScript）的信息，见 [文档](https://docs.cozodb.org/zh_CN/latest/index.html) 。

Clojure 用户可使用 [专门的 Clojure 库](https://github.com/cozodb/cozo-clj/blob/main/README-zh.md)（[国内镜像](https://gitee.com/cozodb/cozo-clj)）。

Android 用户请移步 [这里](https://github.com/cozodb/cozo-lib-android/blob/main/README-zh.md)（[国内镜像](https://gitee.com/cozodb/cozo-lib-android)）。

## 安装

库可从 [maven central](https://mvnrepository.com/artifact/io.github.cozodb/cozo_java) 下载：

```groovy
implementation 'io.github.cozodb:cozo_java:0.7.5'
```

## 使用

```java
import org.cozodb.CozoDb;

class Main {
    public static void main(String[] args) {
        try {
            CozoDb db = new CozoDb();
            System.out.println(db.query("?[] <- [['hello', 'world!']]"));
            db.close();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
```

详情可参见 `CozoDb` 类的 JavaDocs。

## 工作原理

这个库会调用存储于本地路径
`~/.cozo_java_native_lib/` 下面的 JNI 原生库。如果所需要的原生库没有找到，则库会尝试从 [GitHub 下载页](https://github.com/cozodb/cozo/releases) 下载，下载时会在 `stderr` 中输出具体的信息。同样的原生库只需要下载一次。

并不是所有的平台都有预编译的原生库。如果你的平台没有，则在载入 `CozoDb` 类时会报错。

如果你无法联网，你需要将事先准备好的原生库放在对应的位置。

为什么不能直接把原生库打包到 JAR 里？第一，JAR 里面包含的原生库必须解压成单独的文件才能被调用，第二，如果要支持多种不同操作系统及处理器，则需要将所有的原生库都打包进去，这样包会变得非常大（安卓平台的 AAR 文件格式就不存在这个问题）。

## 由于网络原因无法下载原生库，怎么办？

如上一小节所叙，可以手动下载原生库然后放在需要的地方，具体在程序尝试下载时会有相关信息。

为方便国内用户，[Gitee 发布页](https://gitee.com/cozodb/cozo/releases) 里面有原生库下载的镜像（但是必须手动下载）。

## 编译原生库

详见 [这里](https://github.com/cozodb/cozo/blob/main/cozo-lib-java/README-zh.md)（[国内镜像](https://gitee.com/cozodb/cozo/blob/main/cozo-lib-java)）。