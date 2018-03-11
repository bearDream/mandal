package cn.ching.mandal.config;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.config.support.Parameter;
import cn.ching.mandal.rpc.cluster.LoadBalance;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Objects;

/**
 * 2018/3/9
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public abstract class AbstractMethodConfig extends AbstractConfig{
    private static final long serialVersionUID = -740915022128067121L;

    @Getter
    @Setter
    protected Integer timeout;

    @Getter
    @Setter
    protected Integer retries;

    @Getter
    @Setter
    protected Integer actives;

    @Getter
    protected String loadBalance;

    @Getter
    @Setter
    protected Boolean async;

    @Getter
    @Setter
    protected Boolean sent;

    protected String mock;

    @Getter
    @Setter
    protected String merger;

    @Getter
    @Setter
    protected String cache;

    @Getter
    @Setter
    protected String validation;

    @Getter
    protected Map<String, String> parameters;

    @Parameter(escaped = true)
    public String getMock() {
        return mock;
    }

    public void setParameters(Map<String, String> parameters) {
        checkParameterName(parameters);
        this.parameters = parameters;
    }

    public void setMock(String mock) {
        if (!Objects.isNull(mock) && mock.startsWith(Constants.RETURN_PREFIX)){
            checkLength("mock", mock);
        }else {
            checkName("mock", mock);
        }
        this.mock = mock;
    }

    public void setLoadBalance(String loadBalance) {
        checkExtension(LoadBalance.class, "loadBalance", loadBalance);
        this.loadBalance = loadBalance;
    }
}
