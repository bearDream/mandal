package cn.ching.mandal.rpc;

/**
 * 2018/1/5
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface Exporter<T> {

    /**
     * get invoker.
     *
     * @return invoker
     */
    Invoker<T> getInvoker();

    /**
     * unexport.
     * <p>
     * <code>
     * getInvoker().destroy();
     * </code>
     */
    void unexport();

}
