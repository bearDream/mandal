package cn.ching.mandal.monitor.api;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;

import java.util.List;

/**
 * 2018/3/11
 *
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public interface MonitorService {
    String APPLICATION = "application";

    String INTERFACE = "interface";

    String METHOD = "method";

    String GROUP = "group";

    String VERSION = "version";

    String CONSUMER = "consumer";

    String PROVIDER = "provider";

    String TIMESTAMP = "timestamp";

    String SUCCESS = "success";

    String FAILURE = "failure";

    String INPUT = Constants.INPUT_KEY;

    String OUTPUT = Constants.OUTPUT_KEY;

    String ELAPSED = "elapsed";

    String CONCURRENT = "concurrent";

    String MAX_INPUT = "max.input";

    String MAX_OUTPUT = "max.output";

    String MAX_ELAPSED = "max.elapsed";

    String MAX_CONCURRENT = "max.concurrent";

    /**
     * Collect monitor data
     * 1. support invocation count: count://host/interface?application=foo&method=foo&provider=10.20.153.11:20880&success=12&failure=2&elapsed=135423423
     * 1.1 host,application,interface,group,version,method: record source host/application/interface/method
     * 1.2 add provider address parameter if it's data sent from consumer, otherwise, add source consumer's address in parameters
     * 1.3 success,failure,elapsed: record success count, failure count, and total cost for success invocations, average cost (total cost/success calls)
     * @param statistics
     */
    void collect(URL statistics);

    /**
     * search monitor data.
     * 1. support lookup by day: count://host/interface?application=foo&method=foo&side=provider&view=chart&date=2012-07-03
     * 1.1 host,application,interface,group,version,method: query criteria for looking up by host, application, interface, method. When one criterion is not existed, it means ALL will be accepted, but 0.0.0.0 is ALL for host
     * 1.2 side=consumer,provider: decide the data from which side, both provider and consumer are returned by default
     * 1.3 default value is view=summary, to return the summarized data for the whole day. view=chart will return the URL address showing the whole day trend which is convenient for embedding in other web page
     * 1.4 date=2012-07-03: specify the date to collect the data, today is the default value
     * @param query
     * @return
     */
    List<URL> lookup(URL query);
}
