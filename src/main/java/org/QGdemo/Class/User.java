package org.QGdemo.Class;

import java.time.LocalDate;

public class User {
    private final String id;
    private final boolean isAdmin;
    private String name;
    private String password;
    private String phone;
    private String personCode;
    private String sex;
    private int age;
    private LocalDate dateOfBirth;

    public User(String id, String password, boolean isAdmin) {
        this.id = id;
        this.isAdmin = isAdmin;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPersonCode() {
        return personCode;
    }

    public void setPersonCode(String personCode) {
        this.personCode = personCode;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(int year, int month, int day) {
        this.dateOfBirth = LocalDate.of(year, month, day);
    }

    public void setDateOfBirth(LocalDate date) {
        this.dateOfBirth = date;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

}
