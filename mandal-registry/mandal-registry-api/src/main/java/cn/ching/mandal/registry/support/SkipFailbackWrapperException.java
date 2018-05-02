package cn.ching.mandal.registry.support;

/**
 * 2018/1/20
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class SkipFailbackWrapperException extends RuntimeException {

    public SkipFailbackWrapperException(Throwable t){
        super(t);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }
}
