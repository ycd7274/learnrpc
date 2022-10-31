package io.learn.rpc.proxy.api.object;

import io.learn.rpc.protocol.RpcProtocol;
import io.learn.rpc.protocol.header.RpcHeaderFactory;
import io.learn.rpc.protocol.request.RpcRequest;
import io.learn.rpc.proxy.api.consumer.Consumer;
import io.learn.rpc.proxy.api.future.RpcFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @projectName: rpc
 * @package: io.learn.rpc.proxy.api.object
 * @className: ObjectProxy
 * @author: ycd20
 * @description: object proxy
 * @date: 2022/10/31 20:57
 * @version: 1.0
 */
public class ObjectProxy<T> implements InvocationHandler {
    private static final Logger log = LoggerFactory.getLogger(ObjectProxy.class);

    /**
     * interface class object
     */
    private Class<T> clazz;

    /**
     * server version
     */
    private String serviceVersion;
    /**
     * service group
     */
    private String serviceGroup;

    /**
     * timeout default 15s
     */
    private long timeout = 15000;

    /**
     * service consumer
     */
    private Consumer consumer;

    /**
     * serialization type
     */
    private String serializationType;

    /**
     * is async
     */
    private boolean async;

    /**
     * oneway
     */
    private boolean oneway;

    public ObjectProxy(Class<T> clazz) {
        this.clazz = clazz;
    }

    public ObjectProxy(Class<T> clazz, String serviceVersion, String serviceGroup, long timeout, Consumer consumer,
                       String serializationType, boolean async, boolean oneway) {
        this.clazz = clazz;
        this.serviceVersion = serviceVersion;
        this.serviceGroup = serviceGroup;
        this.timeout = timeout;
        this.consumer = consumer;
        this.serializationType = serializationType;
        this.async = async;
        this.oneway = oneway;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class == method.getDeclaringClass()) {
            String name = method.getName();
            if ("equals".equals(name)) {
                return proxy == args[0];
            } else if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            } else if ("toString".equals(name)) {
                return proxy.getClass().getName() + "@" +
                        Integer.toHexString(System.identityHashCode(proxy)) +
                        ",with InvocationHandler " + this;
            } else {
                throw new IllegalStateException(String.valueOf(method));
            }
        }
        RpcProtocol<RpcRequest> requestRpcProtocol =
                new RpcProtocol<>();
        requestRpcProtocol.setHeader(RpcHeaderFactory.getRequestHeader(serializationType));
        RpcRequest request = new RpcRequest();
        request.setVersion(this.serviceVersion);
        request.setClassName(method.getDeclaringClass().getName());
        request.setParameters(method.getParameterTypes());
        request.setGroup(this.serviceGroup);
        request.setAsync(async);
        request.setOneway(oneway);
        requestRpcProtocol.setBody(request);
        //Debug
        log.debug(method.getDeclaringClass().getName());
        log.debug(method.getName());

        if (method.getParameterTypes() != null && method.getParameterTypes().length > 0) {
            for (int i = 0; i < method.getParameterTypes().length; ++i) {
                log.debug(method.getParameterTypes()[i].getName());
            }
        }
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; ++i) {
                log.debug(args[i].toString());
            }
        }
        RpcFuture rpcFuture =
                this.consumer.sendRequest(requestRpcProtocol);
        return rpcFuture == null ? null : timeout > 0 ?
                rpcFuture.get(timeout, TimeUnit.MILLISECONDS) : rpcFuture.get();
    }
}