package cn.ching.mandal.config.spring.status;

import cn.ching.mandal.common.extension.Activate;
import cn.ching.mandal.common.logger.Logger;
import cn.ching.mandal.common.logger.LoggerFactory;
import cn.ching.mandal.common.status.Status;
import cn.ching.mandal.common.status.StatusChecker;
import cn.ching.mandal.common.utils.CollectionUtils;
import cn.ching.mandal.config.ServiceConfig;
import cn.ching.mandal.config.spring.ServiceBean;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

/**
 * 2018/3/25
 * DataSourceStatusCheck
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Activate
public class DataSourceStatusCheck implements StatusChecker{

    private final static Logger logger = LoggerFactory.getLogger(DataSourceStatusCheck.class);

    @Override
    public Status check() {

        ApplicationContext context = ServiceBean.getSPRING_CONTEXT();
        if (Objects.isNull(context)){
            return new Status(Status.Level.UNKNOWN);
        }

        Map<String, DataSource> dataSourceMap = context.getBeansOfType(DataSource.class, false, false);
        if (CollectionUtils.isEmpty(dataSourceMap)){
            return new Status(Status.Level.UNKNOWN);
        }

        StringBuilder sb = new StringBuilder();
        Status.Level level = Status.Level.OK;
        for (Map.Entry<String, DataSource> dataSourceEntry : dataSourceMap.entrySet()) {
            DataSource dataSource = dataSourceEntry.getValue();
            if (sb.length() > 0){
                sb.append(", ");
            }
            sb.append(dataSourceEntry.getKey());
            try {
                Connection connection = dataSource.getConnection();

                try {
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getTypeInfo();
                    try {
                        if (resultSet.next()){
                            level = Status.Level.ERROR;
                        }
                    }finally {
                        resultSet.close();
                    }

                    sb.append(metaData.getURL());
                    sb.append("(");
                    sb.append(metaData.getDatabaseProductName());
                    sb.append("-");
                    sb.append(metaData.getDatabaseProductVersion());
                    sb.append(")");
                }finally {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.warn(e.getMessage(), e);
                return new Status(level, e.getMessage());
            }
        }
        return new Status(level, sb.toString());
    }
}
