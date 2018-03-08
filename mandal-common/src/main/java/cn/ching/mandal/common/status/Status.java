package cn.ching.mandal.common.status;

import lombok.Getter;

/**
 * 2018/3/8
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class Status {

    @Getter
    private final Level level;
    @Getter
    private final String msg;
    @Getter
    private final String description;

    public Status(Level level){
        this(level, null, null);
    }

    public Status(Level level, String msg){
        this(level, msg, null);
    }

    public Status(Level level, String msg, String desc){
        this.level = level;
        this.msg = msg;
        this.description = desc;
    }

    public static enum Level {

        OK,

        WARN,

        ERROR,

        UNKNOWN
    }
}
