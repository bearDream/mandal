package cn.ching.mandal.common.logger;

/**
 * Logger interface
 * <p>
 * This interface is referred from commons-logging
 */
public interface Logger {

    /**
     * Logs a message with trace log level.
     *
     * @param msg log this message
     */
    public void trace(String msg);

    /**
     * Logs an error with trace log level.
     *
     * @param e log this cause
     */
    public void trace(Throwable e);

    /**
     * Logs an error with trace log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    public void trace(String msg, Throwable e);

    /**
     * Logs a message with debug log level.
     *
     * @param msg log this message
     */
    public void debug(String msg);

    /**
     * Logs an error with debug log level.
     *
     * @param e log this cause
     */
    public void debug(Throwable e);

    /**
     * Logs an error with debug log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    public void debug(String msg, Throwable e);

    /**
     * Logs a message with info log level.
     *
     * @param msg log this message
     */
    public void info(String msg);

    /**
     * Logs an error with info log level.
     *
     * @param e log this cause
     */
    public void info(Throwable e);

    /**
     * Logs an error with info log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    public void info(String msg, Throwable e);

    /**
     * Logs a message with warn log level.
     *
     * @param msg log this message
     */
    public void warn(String msg);

    /**
     * Logs a message with warn log level.
     *
     * @param e log this message
     */
    public void warn(Throwable e);

    /**
     * Logs a message with warn log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    public void warn(String msg, Throwable e);

    /**
     * Logs a message with error log level.
     *
     * @param msg log this message
     */
    public void error(String msg);

    /**
     * Logs an error with error log level.
     *
     * @param e log this cause
     */
    public void error(Throwable e);

    /**
     * Logs an error with error log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    public void error(String msg, Throwable e);

    /**
     * Is trace logging currently enabled?
     *
     * @return true if trace is enabled
     */
    public boolean isTraceEnabled();

    /**
     * Is debug logging currently enabled?
     *
     * @return true if debug is enabled
     */
    public boolean isDebugEnabled();

    /**
     * Is info logging currently enabled?
     *
     * @return true if info is enabled
     */
    public boolean isInfoEnabled();

    /**
     * Is warn logging currently enabled?
     *
     * @return true if warn is enabled
     */
    public boolean isWarnEnabled();

    /**
     * Is error logging currently enabled?
     *
     * @return true if error is enabled
     */
    public boolean isErrorEnabled();

}