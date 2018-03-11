package cn.ching.mandal.config;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.config.support.Parameter;

import java.util.List;

/**
 * 2018/3/11
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class MethodConfig extends AbstractMethodConfig{
    private static final long serialVersionUID = -2760311660285581387L;

    // method name
    private String name;

    // stat
    private Integer stat;

    // whether to retry
    private Boolean retry;

    // if it's reliable
    private Boolean reliable;

    // thread limits for method invocations
    private Integer executes;

    // if it's deprecated
    private Boolean deprecated;

    // whether to enable sticky
    private Boolean sticky;

    // whether need to return
    private Boolean isReturn;

    // callback instance when async-call is invoked
    private Object oninvoke;

    // callback method when async-call is invoked
    private String oninvokeMethod;

    // callback instance when async-call is returned
    private Object onreturn;

    // callback method when async-call is returned
    private String onreturnMethod;

    // callback instance when async-call has exception thrown
    private Object onthrow;

    // callback method when async-call has exception thrown
    private String onthrowMethod;

    private List<ArgumentConfig> arguments;

    @Parameter(exclude = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        checkMethodName("name", name);
        this.name = name;
        if (id == null || id.length() == 0) {
            id = name;
        }
    }

    public Integer getStat() {
        return stat;
    }

    @Deprecated
    public void setStat(Integer stat) {
        this.stat = stat;
    }

    @Deprecated
    public Boolean isRetry() {
        return retry;
    }

    @Deprecated
    public void setRetry(Boolean retry) {
        this.retry = retry;
    }

    @Deprecated
    public Boolean isReliable() {
        return reliable;
    }

    @Deprecated
    public void setReliable(Boolean reliable) {
        this.reliable = reliable;
    }

    public Integer getExecutes() {
        return executes;
    }

    public void setExecutes(Integer executes) {
        this.executes = executes;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public List<ArgumentConfig> getArguments() {
        return arguments;
    }

    @SuppressWarnings("unchecked")
    public void setArguments(List<? extends ArgumentConfig> arguments) {
        this.arguments = (List<ArgumentConfig>) arguments;
    }

    public Boolean getSticky() {
        return sticky;
    }

    public void setSticky(Boolean sticky) {
        this.sticky = sticky;
    }

    @Parameter(key = Constants.ON_RETURN_INSTANCE_KEY, exclude = true, attribute = true)
    public Object getOnreturn() {
        return onreturn;
    }

    public void setOnreturn(Object onreturn) {
        this.onreturn = onreturn;
    }

    @Parameter(key = Constants.ON_RETURN_METHOD_KEY, exclude = true, attribute = true)
    public String getOnreturnMethod() {
        return onreturnMethod;
    }

    public void setOnreturnMethod(String onreturnMethod) {
        this.onreturnMethod = onreturnMethod;
    }

    @Parameter(key = Constants.ON_THROW_INSTANCE_KEY, exclude = true, attribute = true)
    public Object getOnthrow() {
        return onthrow;
    }

    public void setOnthrow(Object onthrow) {
        this.onthrow = onthrow;
    }

    @Parameter(key = Constants.ON_THROW_METHOD_KEY, exclude = true, attribute = true)
    public String getOnthrowMethod() {
        return onthrowMethod;
    }

    public void setOnthrowMethod(String onthrowMethod) {
        this.onthrowMethod = onthrowMethod;
    }

    @Parameter(key = Constants.ON_INVOKE_INSTANCE_KEY, exclude = true, attribute = true)
    public Object getOninvoke() {
        return oninvoke;
    }

    public void setOninvoke(Object oninvoke) {
        this.oninvoke = oninvoke;
    }

    @Parameter(key = Constants.ON_INVOKE_METHOD_KEY, exclude = true, attribute = true)
    public String getOninvokeMethod() {
        return oninvokeMethod;
    }

    public void setOninvokeMethod(String oninvokeMethod) {
        this.oninvokeMethod = oninvokeMethod;
    }

    public Boolean isReturn() {
        return isReturn;
    }

    public void setReturn(Boolean isReturn) {
        this.isReturn = isReturn;
    }
}
