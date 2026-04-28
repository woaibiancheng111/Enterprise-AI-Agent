package com.shixi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shixi.business.entity.EmployeeEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<EmployeeEntity> {
}
