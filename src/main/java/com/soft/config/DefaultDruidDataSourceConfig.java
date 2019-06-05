package com.soft.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import tk.mybatis.spring.annotation.MapperScan;

import javax.sql.DataSource;

/**
 * 主数据源
 * Create by Yuanquan.Liu on 2018/5
 */
@Configuration
@MapperScan(basePackages = {"com.soft.live.mapper"}, sqlSessionTemplateRef = "defaultSqlSessionTemplate")
public class DefaultDruidDataSourceConfig {
    // 数据源配置
    @Bean(name = "defaultDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.druid.default")
    @Primary
    public DataSource defaultDataSource() {
        return DataSourceBuilder.create().type(DruidDataSource.class).build();
    }

    // 会话工厂配置
    @Bean(name = "defaultSqlSessionFactory")
    @Primary
    public SqlSessionFactory defaultSqlSessionFactory(@Qualifier("defaultDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        // 加载全局的配置文件
        factoryBean.setConfigLocation(new PathMatchingResourcePatternResolver().getResource("classpath:mybatis/mybatis-config.xml"));
        // 配置mapper的扫描，找到所有的mapper.xml映射文件
        // factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mappers/award/*.xml"));
        return factoryBean.getObject();
    }

    // 事务管理配置
    @Bean(name = "dataSourceTransactionManager")
    @Primary
    public DataSourceTransactionManager dataSourceTransactionManager(@Qualifier("defaultDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    // 会话模板配置
    @Bean(name = "defaultSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate defaultSqlSessionTemplate(@Qualifier("defaultSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}