package cn.ching.mandal.config;

import cn.ching.mandal.common.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * 2018/3/22
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ConsumerConfig extends AbstractReferenceConfig {

    private static final long serialVersionUID = 273195062620277912L;

    private Boolean isDefault;

    // network framework. eg:netty,mina...
    @Getter
    @Setter
    private String client;

    @Override
    public void setTimeout(Integer timeout) {
        super.setTimeout(timeout);
        String rmiTimeOut = System.getProperty("sun.rmi.transport.tcp.responseTimeout");
        if ((timeout != null && timeout > 0) && (StringUtils.isEmpty(rmiTimeOut))){
            System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(timeout));
        }
    }

    public Boolean isDefault(){
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault){
        this.isDefault = isDefault;
    }

}
