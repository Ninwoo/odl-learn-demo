# 重启SDN-ODL之路

## 创建项目

```bash
mvn archetype:generate -DarchetypeGroupId=org.opendaylight.controller -DarchetypeArtifactId=opendaylight-startup-archetype -DarchetypeRepository=http://nexus.opendaylight.org/content/repositories/opendaylight.release/ -DarchetypeCatalog=remote -DarchetypeVersion=1.3.4-Carbon
```

进入生产的项目文件夹,执行构建命令:

```bash
mvn clean install -DskipTests
```

## 编写代码

添加rpc语句块

```yaml
module hello {
    yang-version 1;
    namespace "urn:opendaylight:params:xml:ns:yang:hello";
    prefix "hello";

    revision "2015-01-05" {
        description "Initial revision of hello model";
    }

    rpc hello-world {
    	input {
    		leaf name {
    			type string;
    		}
    	}
    	output {
    		leaf greeting {
    			type string;
    		}
    	}
    }
}
```

重新编译

```bash
mvn clean install -DskipTests
```

编译前:

![1548074656173](/home/ninwoo/.config/Typora/typora-user-images/1548074656173.png)

编译后:

![1548074754196](/home/ninwoo/.config/Typora/typora-user-images/1548074754196.png)

这里观察可以发现,重新编译之后,创建了:

* HelloService
* HelloWorldInput  
* HelloWorldInputBuilder
* HelloWorldOutput
* HelloWorldOutputBuilder

我们可以发现,针对我们创建的hello-world rpc,分别生成了Input和Output接口,并为每个接口生成了相应的构造者.HelloService为继承RpcService的接口,也是RPC最为核心的接口.

现在,只是通过yang定义了具体的数据结构,并没有具体实现类,接下来完成具体的实现工作.

显然,具体实现在hello-impl中进行完善.

```java
/*
 * Copyright © 2017 Ninwoo and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package top.ninwoo.impl;

import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.HelloService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.HelloWorldInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.HelloWorldOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class HelloWorldImpl implements HelloService{
	@Override
	public Future<RpcResult<HelloWorldOutput>> helloWorld(HelloWorldInput input) {
		// TODO Auto-generated method stub
		return null;
	}

}
```

首先在impl中创建HelloService的实现类.这里需要注意两点:

1. head中必须包含Copyright信息
2. 注意观察helloWorld返回的对象类型为`Future<RpcResult<HelloWorldOutput>>`.

实现方法如下:

```java
public class HelloWorldImpl implements HelloService{
	@Override
	public Future<RpcResult<HelloWorldOutput>> helloWorld(HelloWorldInput input) {
		// TODO Auto-generated method stub
		HelloWorldOutputBuilder helloBuilder = new HelloWorldOutputBuilder();
		helloBuilder.setGreeting("hello " + input.getName());
		return RpcResultBuilder.success(helloBuilder.build()).buildFuture();
	}
}
```

这里出现的核心对象为RpcResult,与之对应的构造器为RpcResultBuilder.

> 在ODL开发中出现了大量的建造者模式的设计思想,感兴趣可以自行了解.

这里已经实现了一个最基本的RPC,但依旧没有注册到ODL控制上,接下来就开始演示如何注册RPC服务到ODL.

```java
/*
 * Copyright © 2017 Ninwoo and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package top.ninwoo.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloProvider {

    private static final Logger LOG = LoggerFactory.getLogger(HelloProvider.class);

    private final DataBroker dataBroker;

    public HelloProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("HelloProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("HelloProvider Closed");
    }
}
```

注册步骤主要涉及到的就是HelloProvider这个对象.其中我们需要注意这三个方法:

* 构造方法 
* init() 该方法可以类比spring中的init-method方法,即启动该程序将会自动执行该方法,作用可想而知,通常用来初始化额外的依赖项.
* close() 该方法可以类比spring中的destroy方法,即关闭程序将会自动执行该方法,同样用于优雅关机.

所以注册一个RPC主要包括以下几个步骤,首先看完整的实现程序:

```java
/*
 * Copyright © 2017 Ninwoo and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package top.ninwoo.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.HelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloProvider {

    private static final Logger LOG = LoggerFactory.getLogger(HelloProvider.class);

    private final DataBroker dataBroker;

    private final RpcProviderRegistry rpcProviderRegistry;
    private RpcRegistration<HelloService> serviceRegistration;

    public HelloProvider(final DataBroker dataBroker, final RpcProviderRegistry rpcProviderRegistry) {
        this.dataBroker = dataBroker;
        this.rpcProviderRegistry = rpcProviderRegistry;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("HelloProvider Session Initiated");
        // 注册具体实现类
        serviceRegistration = rpcProviderRegistry.addRpcImplementation(HelloService.class,
        		new HelloWorldImpl());
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
    	serviceRegistration.close();
        LOG.info("HelloProvider Clohenggongsed");
    }
}
```

1. 首先,我们添加了两个成员变量:
   1. RpcProviderRegistry:Rpc注册中心,用于注册RPC服务
   2. RpcRegistration<>:用于保存1中返回的Rpc服务实现类,留存备份以待关闭时关闭.
2. 在初始化方法中使用Rpc注册中心注册实现的RPC服务
3. 在销毁方法中调用close()方法

> 上述实现代码中,缺少了很多安全处理,这里只列出核心代码

到此,看起来已经完成了RPC实现类的绑定,但是这里我们忽略了其中的构造方法.同样类比Spring(底层很有可能就是使用spring实现的),OSGI本身也是一种容器技术,实例化HelloProvider时,需要绑定对应依赖,在我们的代码中,依赖项为:

* DataBroker
* RpcProviderRegistry

显然这里需要一种依赖注入(DI)技术,在ODL项目中,impl-blueprint.xml负责维护依赖.

![1548079055494](/home/ninwoo/.config/Typora/typora-user-images/1548079055494.png)

首先,我们先看最原始的文件样式:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- vi: set et smarttab sw=4 tabstop=4: -->
<!--
Copyright © 2017 Ninwoo and others. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License v1.0 which accompanies this distribution,
and is available at http://www.eclipse.org/legal/epl-v10.html
-->
<blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0"
  xmlns:odl="http://opendaylight.org/xmlns/blueprint/v1.0.0"
  odl:use-default-for-reference-types="true">

  <reference id="dataBroker"
    interface="org.opendaylight.controller.md.sal.binding.api.DataBroker"
    odl:type="default" />
    
  <bean id="provider"
    class="top.ninwoo.impl.HelloProvider"
    init-method="init" destroy-method="close">
    <argument ref="dataBroker" />
  </bean>
    
</blueprint>
```

这里和spring的注释非常相似了.我们可以发现,使用reference标签定义了一个dataBroker接口类型的依赖,然后在provider中使用argument注入了dataBroker依赖.所以同理,我们添加一个RpcProviderRegistry依赖,并使用相同方式注入依赖,所以这里修改代码为:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- vi: set et smarttab sw=4 tabstop=4: -->
<!--
Copyright © 2017 Ninwoo and others. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License v1.0 which accompanies this distribution,
and is available at http://www.eclipse.org/legal/epl-v10.html
-->
<blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0"
  xmlns:odl="http://opendaylight.org/xmlns/blueprint/v1.0.0"
  odl:use-default-for-reference-types="true">

  <reference id="dataBroker"
    interface="org.opendaylight.controller.md.sal.binding.api.DataBroker"
    odl:type="default" />

  <reference id="rpcRegistry"
  	interface="org.opendaylight.controller.sal.binding.api.RpcProviderRegistry"/>

  <bean id="provider"
    class="top.ninwoo.impl.HelloProvider"
    init-method="init" destroy-method="close">
    <argument ref="dataBroker" />
    <argument ref="rpcRegistry" />
  </bean>

</blueprint>
```

到此全部工作完成,重新编译项目,并启动项目:

```bash
./karaf/target/assembly/bin/karaf 
```

打开浏览器,打开[ApiDoc](http://127.0.0.1:8181/apidoc/explorer/index.html),账号密码默认均为`admin`

![1548079882641](/home/ninwoo/.config/Typora/typora-user-images/1548079882641.png)

在input中输入:

```json
{
    "input": {
        "name": "joliu"
    }
}
```

点击Try out!

![1548079953106](/home/ninwoo/.config/Typora/typora-user-images/1548079953106.png)

如果显示上面结果,证明该RPC项目开发成功.