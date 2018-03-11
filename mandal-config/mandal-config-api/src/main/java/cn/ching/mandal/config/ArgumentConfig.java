package cn.ching.mandal.config;

import cn.ching.mandal.config.support.Parameter;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 2018/3/11
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ArgumentConfig implements Serializable {
    private static final long serialVersionUID = 4441089976430621943L;

    @Setter
    private Integer index = -1;

    @Setter
    private String type;

    @Getter
    @Setter
    private Boolean callback;

    @Parameter(exclude = true)
    public Integer getIndex() {
        return index;
    }

    @Parameter(exclude = true)
    public String getType() {
        return type;
    }
}
