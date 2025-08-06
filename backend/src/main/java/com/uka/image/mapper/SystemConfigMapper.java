package com.uka.image.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uka.image.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * System configuration mapper interface
 * 
 * @author Uka Team
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * Find configuration by key
     */
    @Select("SELECT * FROM system_config WHERE config_key = #{configKey}")
    SystemConfig findByConfigKey(@Param("configKey") String configKey);

    /**
     * Get configuration value by key
     */
    @Select("SELECT config_value FROM system_config WHERE config_key = #{configKey}")
    String getConfigValue(@Param("configKey") String configKey);

    /**
     * Check if configuration key exists
     */
    @Select("SELECT COUNT(*) FROM system_config WHERE config_key = #{configKey}")
    int countByConfigKey(@Param("configKey") String configKey);
}