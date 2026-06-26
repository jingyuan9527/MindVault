package com.mindvault.common.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis-Plus JSONB 类型处理器
 *
 * 用于处理 PostgreSQL JSONB 类型字段与 Java String 之间的转换。
 * 写入时将 Java String 包装为 PGobject（type = "jsonb"），
 * 读取时直接以 String 形式返回。
 *
 * 使用方式：在 Entity 字段上标注
 * @TableField(typeHandler = JsonbStringTypeHandler.class)
 *
 * 应用场景：knowledge 表的 ai_tags / user_tags / summary 等 JSONB 字段
 */
@MappedTypes(String.class)
public class JsonbStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        PGobject obj = new PGobject();
        obj.setType("jsonb");
        obj.setValue(parameter);
        ps.setObject(i, obj);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return rs.wasNull() ? null : value;
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return rs.wasNull() ? null : value;
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return cs.wasNull() ? null : value;
    }
}
