package com.xchb.xrpc.transport.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.xchb.xrpc.common.proto.RpcRequestProto;
import com.xchb.xrpc.common.proto.RpcResponseProto;
import com.xchb.xrpc.loadbalance.LoadBalance;
import com.xchb.xrpc.util.ProtoBufUtils;
import com.xchb.xrpc.util.SingleFactory;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import com.xchb.xrpc.common.ServerParam;
import com.xchb.xrpc.exceptions.ServerNotFindExeception;
import com.xchb.xrpc.extension.ExtensionLoader;
import com.xchb.xrpc.register.ServerRegister;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author XDD
 * @project xrpc
 * @date 2021/1/10
 * @description Good Good Study,Day Day Up.
 */
@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<RpcRequestProto.RpcRequest> {


    private Class<ServerRegister> local;
    private ServerRegister serverRegister;
    Class<LoadBalance> algorithm;

    public ServerHandler(){
        local = ExtensionLoader.get(ServerRegister.class, "zk");
        serverRegister = SingleFactory.getInstance(local);
        algorithm = ExtensionLoader.get(LoadBalance.class, "algorithm");;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcRequestProto.RpcRequest rpcRequest) throws Exception {
        log.debug("服务接收到消息,{}",rpcRequest);
        Object result = assembleResult(rpcRequest);

        String type = result.getClass().getName();
        String data = ProtoBufUtils.buildString(type,result);
        RpcResponseProto.RpcResponse rpcResponse = RpcResponseProto.RpcResponse.newBuilder()
                .setId(rpcRequest.getId()).setDataType(type).setData(data).build();
        ctx.channel().writeAndFlush(rpcResponse);
    }

    public static void main(String[] args) {
        System.out.println(JSONObject.parseObject("Boolean"));
    }


    private Object assembleResult(RpcRequestProto.RpcRequest request) {
        try {

            String serverName = ServerParam.buildServerName(request.getInterfaceClass(),
                    request.getVersion(), request.getGroup());
            List<ServerParam> serverParams = serverRegister.find(serverName);
            log.debug("发现服务列表：{}",serverParams);
            if(serverParams==null || serverParams.size()<1){
                throw new ServerNotFindExeception();
            }

            LoadBalance loadBalance = SingleFactory.getInstance(algorithm);
            ServerParam serverParam = loadBalance.load(serverParams);

            if(serverParam==null){
                log.error("没有发现对应的服务: {}",serverName);
                return null;
            }

            Object obj = SingleFactory.getInstance(serverParam.getImplClass());
            if(obj==null){
                return null;
            }


            Object[][] parseRresult = ProtoBufUtils.parseObj(request.getParamInfosMap());

            Class<?>[] paramTypes = (Class<?>[]) parseRresult[0];
            Object[] params = parseRresult[1];

            log.info("参数解析结果,type={},param={}",Arrays.asList(paramTypes),Arrays.asList(params));
            Method declaredMethod =
                    obj.getClass().getDeclaredMethod(request.getMethodName(),paramTypes);
            log.info("方法信息:{}",declaredMethod);
            if(declaredMethod==null){
                log.error("没有匹配的方法" );
                return null;
            }

            return declaredMethod.invoke(obj,params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
