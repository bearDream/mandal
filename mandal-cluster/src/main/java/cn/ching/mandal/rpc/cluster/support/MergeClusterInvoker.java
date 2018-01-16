package cn.ching.mandal.rpc.cluster.support;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.NamedThreadFactory;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.extension.ExtensionLoader;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.ConfigUtils;
import cn.ching.mandal.rpc.*;
import cn.ching.mandal.rpc.cluster.Directory;
import cn.ching.mandal.rpc.cluster.Merger;
import cn.ching.mandal.rpc.cluster.MergerFactory;
import cn.ching.mandal.rpc.cluster.directory.StaticDirectory;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.*;

/**
 * 2018/1/16
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MergeClusterInvoker<T> implements Invoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(MergeClusterInvoker.class);

    private final Directory<T> directory;

    private ExecutorService executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new NamedThreadFactory("merge-cluster-executor", true));

    public MergeClusterInvoker(Directory<T> directory) {
        this.directory = directory;
    }

    @Override
    public Class<T> getInterface() {
        return directory.getInterface();
    }

    @Override
    public URL getUrl() {
        return directory.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return directory.isAvailable();
    }

    @Override
    public void destroy() {
        directory.destroy();
    }

    @Override
    public Result invoke(Invocation invocation) {
        List<Invoker<T>> invokers = directory.list(invocation);

        String merge = getUrl().getMethodParameter(invocation.getMethodName(), Constants.MERGER_KEY);
        if (ConfigUtils.isEmpty(merge)){
            for (Invoker<T> invoker : invokers) {
                if (invoker.isAvailable()){
                    return invoker.invoke(invocation);
                }
            }
            return invokers.iterator().next().invoke(invocation);
        }

        Class<?> returnType;
        try {
            returnType = getInterface().getMethod(invocation.getMethodName(), invocation.getParameterTypes()).getReturnType();
        }catch (NoSuchMethodException e){
            returnType = null;
        }

        Map<String, Future<Result>> results = new HashMap<>();
        for (final Invoker<T> invoker : invokers) {
            Future<Result> future = executor.submit((Callable) () -> {
                return invoker.invoke(new RpcInvocation(invocation, invoker));
            });
            results.put(invoker.getUrl().getServiceKey(), future);
        }

        Object result = null;
        List<Result> resultList = new ArrayList<>(results.size());
        int timeout = getUrl().getMethodParameter(invocation.getMethodName(), Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        for (Map.Entry<String, Future<Result>> entry : results.entrySet()){
            Future<Result> future = entry.getValue();
            try {
                Result r = future.get(timeout, TimeUnit.MILLISECONDS);
                if (r.hasException()){
                    logger.error("invoke " + entry.getKey() + " failed. cause by: " + r.getException().getMessage(), r.getException());
                }else {
                    resultList.add(r);
                }
            }catch (Exception e){
                throw new RpcException("failed to invoke service " + entry.getKey() + ": " + e.getMessage(), e);
            }
        }

        if (resultList.size() == 0){
            return new RpcResult((Object) null);
        }else if (resultList.size() == 1){
            return resultList.iterator().next();
        }
        if (returnType == void.class){
            return new RpcResult((Object) null);
        }


        if (merge.startsWith(".")){
            merge = merge.substring(1);
            Method method;
            try {
                method = returnType.getMethod(merge, returnType);
            } catch (NoSuchMethodException e) {
                throw new RpcException("can't merge result because missing method[ " + merge + " ] in class [" + returnType.getClass().getName() + "]");
            }
            if (!Objects.isNull(method)){
                if (!Modifier.isPublic(method.getModifiers())){
                    method.setAccessible(true);
                }
                result = resultList.remove(0).getValue();
                try {
                    if (method.getReturnType() != void.class && method.getReturnType().isAssignableFrom(result.getClass())){
                        for (Result r : resultList) {
                            result = method.invoke(result, r);
                        }
                    }else {
                        for (Result r : resultList) {
                            method.invoke(result, r.getValue());
                        }
                    }
                } catch (Exception e) {
                    throw new RpcException("can't merge result: " + e.getMessage(), e);
                }
            }else {
                throw new RpcException("can't merger result because missing method [" + merge + "] in class [" + returnType.getClass().getName() + "]");
            }
        }else {
            Merger resultMerger;
            if (ConfigUtils.isDefault(merge)){
                resultMerger = MergerFactory.getMerger(returnType);
            }else {
                resultMerger = ExtensionLoader.getExtensionLoader(Merger.class).getExtension(merge);
            }
            if (!Objects.isNull(resultMerger)){
                List<Object> rets = new ArrayList<>(resultList.size());
                resultList.forEach(r -> rets.add(r.getValue()));
                result = resultMerger.merge(rets.toArray((Object[]) Array.newInstance(returnType, 0)));
            }else {
                throw new RpcException("no merger result");
            }
        }
        return new RpcResult(result);
    }
}
