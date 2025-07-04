package com.helltractor.exchange.db;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;

/**
 * Hold criteria query information.
 *
 * @param <T> Entity type.
 */
final class Criteria<T> {

    DbTemplate dataBase;
    Mapper<T> mapper;
    Class<T> clazz;
    List<String> select = null;
    String table = null;
    String where = null;
    List<Object> whereParams = null;
    List<String> orderBy = null;
    int offset = 0;
    int maxResults = 0;

    Criteria(DbTemplate dataBase) {
        this.dataBase = dataBase;
    }

    String sql() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("SELECT ");
        sb.append((select == null ? "*" : String.join(", ", select)));
        sb.append(" FROM ").append(mapper.tableName);
        if (where != null) {
            sb.append(" WHERE ").append(String.join(" ", where));
        }
        if (orderBy != null) {
            sb.append(" ORDER BY ").append(String.join(", ", orderBy));
        }
        if (offset >= 0 && maxResults > 0) {
            sb.append(" LIMIT ?, ?");
        }
        String s = sb.toString();
        return s;
    }

    Object[] params() {
        List<Object> params = new ArrayList<>();
        if (where != null) {
            for (Object obj : whereParams) {
                params.add(obj);
            }
        }
        if (offset >= 0 && maxResults > 0) {
            params.add(offset);
            params.add(maxResults);
        }
        return params.toArray();
    }

    List<T> list() {
        String selectSql = sql();
        Object[] selectParams = params();
        return dataBase.jdbcTemplate.query(selectSql, mapper.resultSetExtractor, selectParams);
    }

    T first() {
        this.offset = 0;
        this.maxResults = 1;
        String selectSql = sql();
        Object[] selectParams = params();
        List<T> list = dataBase.jdbcTemplate.query(selectSql, mapper.resultSetExtractor, selectParams);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    T unique() {
        this.offset = 0;
        this.maxResults = 2;
        String selectSql = sql();
        Object[] selectParams = params();
        List<T> list = dataBase.jdbcTemplate.query(selectSql, mapper.resultSetExtractor, selectParams);
        if (list == null || list.isEmpty()) {
            throw new NoResultException("Expected unique row but nothing found.");
        }
        if (list.size() > 1) {
            throw new NonUniqueResultException("Expected unique row but more than 1 rows found.");
        }
        return list.get(0);
    }
}
