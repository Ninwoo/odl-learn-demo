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
        LOG.info("HelloProvider Closed");
    }
}