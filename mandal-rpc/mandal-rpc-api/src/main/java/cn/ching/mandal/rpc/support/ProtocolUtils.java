package cn.ching.mandal.rpc.support;

import cn.ching.mandal.common.Constants;
import cn.ching.mandal.common.URL;
import cn.ching.mandal.common.utils.StringUtils;

import java.util.Objects;

/**
 * 2018/1/11
 * Protocol helper
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class ProtocolUtils {

    private final static String NULL_VERSION_NUM = "0.0.0";

    private ProtocolUtils(){

    }

    /**
     * return serviceKey by {@link URL}
     * @see {@link ProtocolUtils#serviceKey(int, String, String, String)}
     * @param url
     * @return
     */
    public static String serviceKey(URL url){
        return serviceKey(url.getPort(), url.getPath(), url.getParameter(Constants.VERSION_KEY), url.getParameter(Constants.GROUP_KEY));
    }

    /**
     * return serviceKey
     * @param port
     * @param serviceName
     * @param servicecVersion
     * @param serviceGroup
     * @return group/serviceName:1.0.0:80
     */
    public static String serviceKey(int port, String serviceName, String servicecVersion, String serviceGroup){
        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isBlank(serviceGroup)){
            sb.append(serviceGroup);
            sb.append("/");
        }
        sb.append(serviceName);
        if (!StringUtils.isBlank(servicecVersion) && !NULL_VERSION_NUM.equals(servicecVersion)){
            sb.append(":");
            sb.append(servicecVersion);
        }
        sb.append(":");
        sb.append(port);
        return sb.toString();
    }

    public static boolean isGeneric(String generic) {
        return generic != null
                && !"".equals(generic)
                && (Constants.GENERIC_SERIALIZATION_DEFAULT.equalsIgnoreCase(generic)
                || Constants.GENERIC_SERIALIZATION_NATIVE_JAVA.equalsIgnoreCase(generic)
                || Constants.GENERIC_SERIALIZATION_BEAN.equalsIgnoreCase(generic));
    }
}
