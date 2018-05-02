package cn.ching.mandal.rpc.cluster.router.condition;

import cn.ching.mandal.rpc.cluster.Router;
import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.common.utils.NetUtils;
import cn.ching.mandal.common.utils.StringUtils;
import cn.ching.mandal.common.utils.UrlUtils;
import cn.ching.mandal.rpc.Invocation;
import cn.ching.mandal.rpc.Invoker;
import cn.ching.mandal.rpc.RpcException;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2018/1/17
 * eg: host = 10.20.153.10 => host = 10.20.153.11
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ConditionRouter implements Router, Comparable<Router> {

    private static final Logger logger = LoggerFactory.getLogger(ConditionRouter.class);

    private static Pattern ROUTER_PATTERN = Pattern.compile("([&!=,]*)\\s*([^&!=,\\s]+)");

    private URL url;

    private int priority;

    // before "=>"
    private final Map<String, MatchPair> whenCondition;

    // after "=>"
    private final Map<String, MatchPair> thenCondition;

    public ConditionRouter(URL url){
        this.url = url;
        this.priority = url.getParameter(Constants.PRIORITY_KEY, 0);
        try {
            String rule = url.getParameter(Constants.RULE_KEY);
            if (StringUtils.isBlank(rule)){
                throw new IllegalArgumentException("illegal route rule!");
            }
            rule = rule.replace("consumer.", "").replace("provider.", "");
            int index = rule.indexOf("=>");
            String whenRule = index < 0 ? null : rule.substring(0, index).trim();
            String thenRule = index < 0 ? rule.trim() : rule.substring(index + 2).trim();
            this.whenCondition = StringUtils.isBlank(whenRule) || "true".equals(whenRule) ? new HashMap<String, MatchPair>() : parseRule(whenRule);
            this.thenCondition = StringUtils.isBlank(thenRule) || "false".equals(thenRule) ? null : parseRule(thenRule);
        }catch (ParseException e){
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (CollectionUtils.isEmpty(invokers)){
            return invokers;
        }
        try {
            if (!matchWhen(url, invocation)){
                return invokers;
            }
            List<Invoker<T>> result = new ArrayList<>();
            if (Objects.isNull(thenCondition)){
                logger.warn("The current consumer in the service blacklist. comnsumer: " + NetUtils.getLocalHost() + ". service " + url.getServiceKey());
                return result;
            }
            invokers.stream().filter(i -> matchThen(i.getUrl(), url)).forEach(i -> result.add(i));
            if (result.size() > 0){
                return result;
            }else {
                logger.warn("the router result is empty! . consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey() + "router: " + url.getParameterAndDecoded(Constants.RULE_KEY));
                return result;
            }
        }catch (Throwable t){
            logger.error("Failed to execute condition rule: " + getUrl() + ", invokers: " + invokers + ", cause: " + t.getMessage(), t);
        }

        return invokers;
    }

    private boolean matchThen(URL url, URL param) {
        return !(thenCondition == null || thenCondition.isEmpty()) && matchCondition(thenCondition, url, param, null);
    }

    private boolean matchWhen(URL url, Invocation invocation) {
        return whenCondition == null || whenCondition.isEmpty() || matchCondition(whenCondition, url, null, invocation);
    }

    private boolean matchCondition(Map<String, MatchPair> whenCondition, URL url, URL param, Invocation invocation) {
        Map<String, String> sample = url.toMap();
        boolean result = false;
        for (Map.Entry<String, MatchPair> matchPair : whenCondition.entrySet()){
            String key = matchPair.getKey();
            String sampleVal;
            if (!Objects.isNull(invocation) && (Constants.METHOD_KEY.equals(key) || Constants.METHOD_KEY.equals(key))){
                sampleVal = invocation.getMethodName();
            }else {
                sampleVal = sample.get(key);
            }
            if (!Objects.isNull(sampleVal)){
                if (!matchPair.getValue().isMatch(sampleVal, param)){
                    return false;
                }else {
                    result = true;
                }
            }else {
                if (matchPair.getValue().matches.size() > 0){
                    return false;
                }else {
                    return true;
                }
            }
        }
        return result;
    }

    @Override
    public int compareTo(Router o) {
        if (Objects.isNull(0) || o.getClass() != ConditionRouter.class){
            return 1;
        }
        ConditionRouter c = (ConditionRouter) o;
        return this.priority == c.priority ? url.toFullString().compareTo(c.url.toFullString()) : (this.priority > c.priority ? 1 : -1);
    }

    private static Map<String, MatchPair> parseRule(String rule) throws ParseException{
        Map<String, MatchPair> condition = new HashMap<>();
        if (StringUtils.isBlank(rule)){
            return condition;
        }

        MatchPair pair = null;
        Set<String> values = null;
        final Matcher matcher = ROUTER_PATTERN.matcher(rule);
        while (matcher.find()){
            String seperator = matcher.group(1);
            String content = matcher.group(2);
            if (StringUtils.isBlank(seperator)){
                pair = new MatchPair();
                condition.put(content, pair);
            } else if ("&".equals(seperator)){
                if (condition.get(content) == null){
                    pair = new MatchPair();
                    condition.put(content, pair);
                }else {
                    pair = condition.get(content);
                }
            } else if ("=".equals(seperator)){
                if (pair == null){
                    throw new ParseException("illegal router rule " + rule + " The error char " + seperator + " at index" + matcher.start() + " before " + content + " .",matcher.start());
                }
                values=pair.matches;
                values.add(content);
            }else if ("!=".equals(seperator)){
                if (pair == null){
                    throw new ParseException("illegal router rule " + rule + " The error char " + seperator + " at index" + matcher.start() + " before " + content + " .",matcher.start());
                }
                values = pair.mismatches;
                values.add(content);
            }else if (".".equals(seperator)){
                if (CollectionUtils.isEmpty(values)){
                    throw new ParseException("illegal router rule " + rule + " The error char " + seperator + " at index" + matcher.start() + " before " + content + " .",matcher.start());
                }
                values.add(content);
            }else {
                if (pair == null){
                    throw new ParseException("illegal router rule " + rule + " The error char " + seperator + " at index" + matcher.start() + " before " + content + " .",matcher.start());
                }
            }
        }
        return condition;
    }

    private static final class MatchPair{
        final Set<String> matches = new HashSet<>();
        final Set<String> mismatches = new HashSet<>();

        private boolean isMatch(String value, URL param){
            // only have match.
            if (matches.size() > 0 && mismatches.size() == 0){
                for (String match : matches) {
                    if (UrlUtils.isMatchGlobPattern(match, value, param)){
                        return true;
                    }
                }
                return false;
            }
            // only have mismatch
            if (matches.size() == 0 && mismatches.size() > 0){
                for (String mismatch : mismatches) {
                    if (UrlUtils.isMatchGlobPattern(mismatch, value, param)){
                        return true;
                    }
                }
                return false;
            }
            // both match and mismatch.
            if (matches.size() > 0 && mismatches.size() > 0){
                for (String mismatch : mismatches) {
                    if (UrlUtils.isMatchGlobPattern(mismatch, value, param)){
                        return false;
                    }
                }
                for (String match : matches) {
                    if (UrlUtils.isMatchGlobPattern(match, value, param)){
                        return true;
                    }
                }
                return false;
            }
            return false;
        }
    }
}
