package com.siva.services.withdrawservice.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public abstract class DatabaseService<V> {


    protected DatabaseService() {}

    public abstract String insert(V t);

    public abstract Boolean update(V t);

    public abstract Optional<V> get(String param);

    public abstract List<V> listAll();
    public abstract Boolean delete(String id);

}
