package cn.ching.mandal.rpc.service;

import cn.ching.mandal.common.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * 2018/1/15
 * generic exception
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class GenericException extends RuntimeException {

    private static final long serialVersionUID = -6574339131995362928L;

    @Getter
    @Setter
    private String exceptionClass;

    @Getter
    @Setter
    private String exceptionMessage;

    public GenericException(){

    }

    public GenericException(String exceptionClass, String exceptionMessage){
        super(exceptionMessage);
        this.exceptionClass = exceptionClass;
        this.exceptionMessage = exceptionMessage;
    }

    public GenericException(Throwable t){
        super(StringUtils.toString(t));
        this.exceptionClass = t.getClass().getName();
        this.exceptionMessage = t.getMessage();
    }



}
