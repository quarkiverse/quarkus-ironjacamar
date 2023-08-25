package io.quarkiverse.ironjacamar;

import org.apache.activemq.artemis.api.core.client.loadbalance.FirstElementConnectionLoadBalancingPolicy;
import org.apache.activemq.artemis.api.core.client.loadbalance.RandomConnectionLoadBalancingPolicy;
import org.apache.activemq.artemis.api.core.client.loadbalance.RandomStickyConnectionLoadBalancingPolicy;
import org.apache.activemq.artemis.api.core.client.loadbalance.RoundRobinConnectionLoadBalancingPolicy;
import org.apache.activemq.artemis.core.protocol.hornetq.client.HornetQClientProtocolManagerFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.ra.ActiveMQResourceAdapter;

import io.netty.channel.epoll.EpollSocketChannel;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {
        // org.apache.activemq
        NettyConnectorFactory.class,
        FirstElementConnectionLoadBalancingPolicy.class,
        RandomConnectionLoadBalancingPolicy.class,
        RandomStickyConnectionLoadBalancingPolicy.class,
        RoundRobinConnectionLoadBalancingPolicy.class,
        ActiveMQResourceAdapter.class,
        HornetQClientProtocolManagerFactory.class,
        // io.netty
        EpollSocketChannel.class
})
public class Reflection {
}
