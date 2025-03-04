package org.QGdemo.Class;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DatabaseManager {
    private final String url;       //数据库url
    private final String username;  //数据库用户名
    private final String password;  //数据库用户密码

    private HikariDataSource dataSource;    //连接池
    private boolean connected;  //是否已经连接到数据库

    private final ArrayList<String> tables = new ArrayList<>();  //数据库中应该存在的表名
    private final HashMap<String, ArrayList<String>> columns = new HashMap<>(); //数据库中各表中应该存在的字段（列）

    public DatabaseManager(String ip, String port, String database, String username, String password) {
        //连接到数据库所需的信息
        this.url = String.format("jdbc:mysql://%s:%s/%s", ip, port, database);
        this.username = username;
        this.password = password;

        //数据库应含有的表
        tables.add("users_password");
        tables.add("admins_register_core");
        tables.add("users_information");

        //数据库各表应含有的字段
        columns.put("users_password", new ArrayList<>(List.of("id VARCHAR(20) PRIMARY KEY", "password VARCHAR(20)")));
        columns.put("admins_register_core", new ArrayList<>(List.of("register_cores VARCHAR(40) PRIMARY KEY")));
        columns.put("users_information", new ArrayList<>(List.of("id VARCHAR(20) PRIMARY KEY", "name VARCHAR(20)", "phone VARCHAR(20)", "personCode VARCHAR(20)", "sex VARCHAR(8)", "birthday DATE", "isAdmin TINYINT(1)", "age INT")));

        //设置连接池
        setupConnectionPool();

        //确保各表及字段存在，如果遇到错误，取消连接
        ensureExists();

    }

    //设置连接池
    private void setupConnectionPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1800000);

        try {
            dataSource = new HikariDataSource(config);
            connected = true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            connected = false;
        }

    }

    //关闭连接池
    public void closeConnectionPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    //从连接池得到一个连接
    private Connection getConnection() throws SQLException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        }
    }

    //返回是否已与数据库建立连接
    public Boolean isConnected() {
        return connected;
    }

    //确保各表及表中的字段存在
    private boolean ensureExists() {
        if(!connected) return false;

        try(Connection connection = getConnection();
            Statement statement = connection.createStatement()) {

            //开启事务
            connection.setAutoCommit(false);

            //检查各表及表中字段是否完整
            try {
                for(String table : tables) {
                    statement.addBatch("CREATE TABLE IF NOT EXISTS " + table + " (timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"); //检查表

                    ArrayList<String> columnList = getColumns(connection, table);       //检查字段
                    for(String column : columns.get(table)) {
                        if(!columnList.contains(column.split(" ")[0])) {
                            statement.addBatch("ALTER TABLE " + table + " ADD COLUMN " + column);     //补全字段
                        }
                    }
                }
            } catch (SQLException e) {
                connection.rollback();
                System.out.println(e.getMessage());
                return false;
            }

            statement.executeBatch();
            connection.commit();    //提交事务
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    //获取某个表的所有字段（列）
    private ArrayList<String> getColumns(Connection connection, String table) throws SQLException {
        ArrayList<String> columns = new ArrayList<>();
        try(ResultSet resultSet = connection.getMetaData().getColumns(null, null, table, null)) {
            while(resultSet.next()) {
                columns.add(resultSet.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }

    //得到所有用户的账号及密码
    public HashMap<String, String> getUsersPassword() {
        HashMap<String, String> users = new HashMap<>();

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + "users_password")) {
            while(resultSet.next()) {
                users.put(resultSet.getString("id"), resultSet.getString("password"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return users;
    }

    //得到所有的管理员注册码
    public ArrayList<String> getAdminsCore() {
        ArrayList<String> cores = new ArrayList<>();

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + "admins_register_core")) {
            while(resultSet.next()) {
                cores.add(resultSet.getString("register_cores"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return cores;
    }

    //添加一条管理员注册码
    public boolean addAdminCore(String core) {

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO " + "admins_register_core" + " (register_cores) VALUES (?)")) {
            connection.setAutoCommit(false);

            try {
                statement.setString(1, core);
                statement.executeUpdate();
            } catch (SQLException e) {
                connection.rollback();
                return false;
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            //System.out.println(e.getMessage());
            return false;
        }
    }

    //删除一条管理员注册码
    public boolean deleteAdminCore(String core) {

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM " + "admins_register_core" + " WHERE register_cores=?")) {
            connection.setAutoCommit(false);

            try {
                statement.setString(1, core);
                statement.executeUpdate();
            } catch (SQLException e) {
                connection.rollback();
                return false;
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    //得到所有的用户
    public HashMap<String, User> getUsers(HashMap<String, String> usersPassword) {
        HashMap<String, User> users = new HashMap<>();

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + "users_information")) {
            while (resultSet.next()) {
                String id = resultSet.getString("id");
                String password = usersPassword.get(id);
                String name = resultSet.getString("name");

                String phone = resultSet.getString("phone");
                String personCode = resultSet.getString("personCode");
                boolean isAdmin = resultSet.getBoolean("isAdmin");

                int age = resultSet.getInt("age");
                String sex = resultSet.getString("sex");

                Date date = resultSet.getDate("birthday");

                LocalDate birthday = (date == null) ? null : date.toLocalDate();

                User u = new User(id, password, isAdmin);
                u.setName(name);
                u.setPhone(phone);
                u.setPersonCode(personCode);
                u.setAge(age);
                u.setSex(sex);
                u.setDateOfBirth(birthday);

                users.put(id, u);

            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return users;
    }

    //添加一名用户的密码
    public boolean addUserPassword(String id, String password) {

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO " + "users_password" + " (id,password) VALUES (?,?)")) {
            connection.setAutoCommit(false);

            try {
                statement.setString(1, id);
                statement.setString(2, password);
                statement.executeUpdate();
            } catch (SQLException e) {
                connection.rollback();
                return false;
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    //删除一名用户的密码
    public boolean deleteUserPassword(String id) {

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM " + "users_password" + " WHERE id=?")) {
            connection.setAutoCommit(false);

            try {
                statement.setString(1, id);
                statement.executeUpdate();
            } catch (SQLException e) {
                connection.rollback();
                return false;
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    //更新一名用户的密码
    public boolean updateUserPassword(String id, String password) {

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE " + "users_password" + " SET password=? WHERE id=?")) {
            connection.setAutoCommit(false);

            try {
                statement.setString(1, password);
                statement.setString(2, id);
                statement.executeUpdate();
            } catch (SQLException e) {
                connection.rollback();
                return false;
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    //添加一名用户
    public boolean addUser(String id, boolean isAdmin) {

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO " + "users_information" + " (id,isAdmin) VALUES (?,?)")) {
            connection.setAutoCommit(false);

            try {
                statement.setString(1, id);
                statement.setBoolean(2, isAdmin);
                statement.executeUpdate();
            } catch (SQLException e) {
                connection.rollback();
                return false;
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    //删除一名用户
    public boolean deleteUser(String id) {

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM " + "users_information" + " WHERE id=?")) {
            connection.setAutoCommit(false);

            try {
                statement.setString(1, id);
                statement.executeUpdate();
            } catch (SQLException e) {
                connection.rollback();
                return false;
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    //更新一名用户的信息
    public boolean updateUser(String id, String name, String phone, String personCode, String sex, LocalDate birthday, int age) {

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE " + "users_information" + " SET name=?,phone=?,personCode=?,sex=?,birthday=?,age=? WHERE id=?")) {
            connection.setAutoCommit(false);

            try {
                statement.setString(1, name);
                statement.setString(2, phone);
                statement.setString(3, personCode);
                statement.setString(4, sex);
                statement.setDate(5, Date.valueOf(birthday));
                statement.setInt(6, age);
                statement.setString(7, id);
                statement.executeUpdate();
            } catch (SQLException e) {
                connection.rollback();
                return false;
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }


}
