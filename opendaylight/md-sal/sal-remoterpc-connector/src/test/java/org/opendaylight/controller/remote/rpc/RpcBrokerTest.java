/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import akka.actor.Status.Failure;
import akka.testkit.javadsl.TestKit;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.mdsal.dom.api.DOMRpcException;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class RpcBrokerTest extends AbstractRpcTest {

    @Test
    public void testExecuteRpc() {
        new TestKit(node1) {
            {

                final ContainerNode invokeRpcResult = makeRPCOutput("bar");
                final DOMRpcResult rpcResult = new DefaultDOMRpcResult(invokeRpcResult);
                when(domRpcService1.invokeRpc(eq(TEST_RPC_TYPE), Mockito.<NormalizedNode<?, ?>>any())).thenReturn(
                        FluentFutures.immediateFluentFuture(rpcResult));

                final ExecuteRpc executeMsg = ExecuteRpc.from(TEST_RPC_ID, null);

                rpcInvoker1.tell(executeMsg, getRef());

                final RpcResponse rpcResponse = expectMsgClass(duration("5 seconds"), RpcResponse.class);

                assertEquals(rpcResult.getResult(), rpcResponse.getResultNormalizedNode());
            }
        };
    }

    @Test
    public void testExecuteRpcFailureWithException() {
        new TestKit(node1) {
            {
                when(domRpcService1.invokeRpc(eq(TEST_RPC_TYPE), Mockito.<NormalizedNode<?, ?>>any()))
                        .thenReturn(FluentFutures.immediateFailedFluentFuture(
                                new DOMRpcImplementationNotAvailableException("NOT FOUND")));

                final ExecuteRpc executeMsg = ExecuteRpc.from(TEST_RPC_ID, null);

                rpcInvoker1.tell(executeMsg, getRef());

                final Failure rpcResponse = expectMsgClass(duration("5 seconds"), akka.actor.Status.Failure.class);

                Assert.assertTrue(rpcResponse.cause() instanceof DOMRpcException);
            }
        };
    }
}
