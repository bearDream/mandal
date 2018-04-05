package cn.ching.mandal.rpc;

/**
 * 2018/4/5
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ServiceClassHolder {

    private static final ServiceClassHolder SERVICE_CLASS_HOLDER = new ServiceClassHolder();

    private static ThreadLocal<Class> holder = new ThreadLocal<>();

    public static ServiceClassHolder getInstance() {
        return SERVICE_CLASS_HOLDER;
    }

    private ServiceClassHolder(){}

    public void pushServiceClassHolder(Class cls){
        holder.set(cls);
    }

    public Class popServiceClassHolder(){
        Class clazz = holder.get();
        holder.remove();
        return clazz;
    }
}
