package com.acabra;

import com.acabra.data.UrlData;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class TinyUrlDBService {

    private final Connection db;

    public TinyUrlDBService() throws SQLException {
        this.db = getConnection();
        initDb(db);
    }

    private void initDb(Connection db) {
        try {
            final Statement statement = db.createStatement();
            statement.execute(
                """
                CREATE TABLE url (
                        pkey VARCHAR(10) NOT NULL, 
                        user_id INT NOT NULL, 
                        url VARCHAR(255) NOT NULL, 
                        created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
                        expiration DATE NOT NULL,
                        PRIMARY KEY (pkey)
                );
                """
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:test");
    }

    public boolean insertKey(
            String key, String url, LocalDateTime created, LocalDateTime validExpiration, int userId)  {
        try {
            final PreparedStatement statement = db.prepareStatement(
                    """
                    INSERT INTO url (pkey, user_id, url, created, expiration)
                    VALUES (?,?,?,?, ?)
                    """
            );
            statement.setObject(1, key);
            statement.setObject(2, userId);
            statement.setString(3, url);
            statement.setObject(4, created.atOffset(ZoneOffset.UTC));
            statement.setObject(5, validExpiration.atOffset(ZoneOffset.UTC));
            statement.execute();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasKey(String key) {
        try {
            final PreparedStatement statement = db.prepareStatement("SELECT pkey FROM url WHERE pkey = ?");
            statement.setString(1, key);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
            return true;
        }
    }

    public UrlData getUrlData(String key) {
        try {
            final PreparedStatement statement = db.prepareStatement(
                    "SELECT pkey, user_id, url, created, expiration,  FROM url WHERE pkey = ?");
            statement.setString(1, key);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                return new UrlData(
                        resultSet.getString(1),
                        resultSet.getString(3),
                        resultSet.getInt(2),
                        resultSet.getObject(4, OffsetDateTime.class).toLocalDateTime(),
                        resultSet.getObject(5, OffsetDateTime.class).toLocalDateTime()
                );
            }
            return null;
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
            return null;
        }
    }
}
