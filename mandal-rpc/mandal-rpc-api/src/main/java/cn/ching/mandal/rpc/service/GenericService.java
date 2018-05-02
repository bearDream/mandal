package cn.ching.mandal.rpc.service;

/**
 * 2018/1/15
 * Generic invocation
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface GenericService {

    /**
     *
     * @param methodName
     * @param parameterTypes
     * @param args
     * @return invocation return value
     * @throws GenericException
     */
    Object $invoke(String methodName, String[] parameterTypes, Object[] args) throws GenericException;
}
