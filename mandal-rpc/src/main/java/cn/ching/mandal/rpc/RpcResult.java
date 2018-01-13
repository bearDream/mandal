package cn.ching.mandal.rpc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 2018/1/13
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class RpcResult implements Result, Serializable {

    private static final long serialVersionUID = -7087479573467865025L;

    private Object result;

    private Throwable exception;

    private Map<String, String> attachments = new HashMap<>();

    public RpcResult(){

    }

    public RpcResult(Object result){
        this.result = result;
    }

    public RpcResult(Throwable throwable){
        this.exception = throwable;
    }

    public void setAttachments(String key, String value){
        attachments.put(key, value);
    }

    public void setAttachments(Map<String, String> attachments){
        if (Objects.nonNull(attachments) && attachments.size() > 0){
            attachments.putAll(attachments);
        }
    }

    @Override
    public Object getValue() {
        return result;
    }

    public void setValue(Object value){
        this.result = value;
    }

    @Override
    public Throwable getException() {
        return exception;
    }

    public void setException() {
        this.exception = exception;
    }

    @Override
    public boolean hasException() {
        return !Objects.isNull(exception);
    }

    @Override
    public Object recreate() throws Throwable {
        if (Objects.nonNull(exception)){
            throw exception;
        }
        return result;
    }

    @Override
    public Map<String, String> getAttachments() {
        return attachments;
    }

    @Override
    public String getAttachment(String key) {
        return attachments.get(key);
    }

    @Override
    public String getAttachment(String key, String defaultValue) {
        return attachments.getOrDefault(key, defaultValue);
    }
}
