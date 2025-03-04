package org.QGdemo;

import org.QGdemo.Class.Config;
import org.QGdemo.Class.DatabaseManager;
import org.QGdemo.Class.Page;
import org.QGdemo.Class.User;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    private static Config config;       //程序的配置
    private static DatabaseManager databaseManager; //数据库管理器
    private static Scanner sc;   //用于输入

    private static int page = Page.LOGIN;   //代表当前页码
    private static boolean quit = false;    //代表用户是否选择退出程序
    private static User user;               //代表当前登录的用户
    private static User selectedUser;       //某些操作所选择的用户，如查看其它用户信息时所选的用户

    private static HashMap<String, String> usersPassword = new HashMap<>();         //学/工号密码键值对
    private static ArrayList<String> adminCode = new ArrayList<>();         //管理员注册码
    private static HashMap<String, User> users = new HashMap<>();         //用户id与用户键值对

    public static void main(String[] args) {

        if(!loadConfig()) {
            print("配置文件加载失败，请检查配置文件");
            print("按 Enter 结束程序");
            new Scanner(System.in).nextLine();
            quit = true;
        }

        try {
            sc = new Scanner(System.in, config.getEncoding());
        } catch (IllegalArgumentException e) {
            print("配置文件config.properties中字符集编码“encoding”的值无效，请重新设置");
            quit = true;
        }

        //连接到数据库
        databaseManager = new DatabaseManager(config.getMysqlIp(), config.getMysqlPort(), config.getDatabaseName(), config.getMysqlUsername(), config.getMysqlPassword());
        if(!databaseManager.isConnected()) {
            print("无法连接到数据库，请检查网络或配置文件的配置是否正确");
            print("按 Enter 结束程序");
            sc.nextLine();
            quit = true;
        }
        else {
            //从数据库获取用户数据
            adminCode = databaseManager.getAdminsCore();
            usersPassword = databaseManager.getUsersPassword();
            users = databaseManager.getUsers(usersPassword);
        }

        if (quit) return;

        //如果没有初始管理员，则创建
        fistAdmin();

//        for(String id : users.keySet()) print(id);
//        sc.nextLine();

        //显示相应内容
        while(!quit) {
            clearScreen();
            showMenu(page);
        }
        databaseManager.closeConnectionPool();
    }

    private static void showMenu(int page) {
        print("学生信息管理系统");
        switch(page) {
            case Page.LOGIN: loginSelect(); break;
            case Page.LOGIN_AS_USER: loginAsUser(); break;
            case Page.REGISTER_AS_STUDENT: registerAsStudent(); break;
            case Page.REGISTER_AS_ADMIN: registerAsAdmin(); break;
            case Page.STUDENT: studentPage(); break;
            case Page.ADMIN: adminPage(); break;
            case Page.USER_INFO: userInfoPage(); break;
            case Page.RESET_PASSWORD: resetPasswordPage(user != null); break;
            case Page.USERS: usersListPage(new ArrayList<>(users.keySet())); break;
            case Page.PERSON_INFO: userInfoPage(selectedUser); break;
            case Page.ADMIN_CORE: adminCodePage(); break;
            case Page.SEARCH: searchPage(); break;

        }
    }

    //用户登录选择页
    private static void loginSelect() {
        String selection;

        print("1-用户登录");
        print("2-忘记密码");
        print("3-注册为学生");
        print("4-注册为管理员");
        print("5-退出");

        while(true) {
            print("请输入以上选项（1~5）");
            if(!sc.hasNextLine()) continue;
            selection = sc.nextLine();

            switch (selection) {
                case "1": page = Page.LOGIN_AS_USER; return;
                case "2": page = Page.RESET_PASSWORD; return;
                case "3": page = Page.REGISTER_AS_STUDENT; return;
                case "4": page = Page.REGISTER_AS_ADMIN; return;
                case "5": quit = true; return;
            }
        }
    }

    //用户登录页
    private static void loginAsUser() {
        String username;
        String password;

        while(true) {
            print("请输入工号/学号，如果要退出，请输入“back”");
            while(true) {
                if(!sc.hasNextLine()) continue;
                username = sc.nextLine();

                if(username.equalsIgnoreCase("back")) {
                    page = Page.LOGIN;
                    return;
                }

                break;
            }

            print("请输入密码，如果要退出，请输入“back”");
            while(true) {
                if(!sc.hasNextLine()) continue;
                password = sc.nextLine();

                if(password.equalsIgnoreCase("back")) {
                    page = Page.LOGIN;
                    return;
                }

                break;
            }

            if(usersPassword.containsKey(username) && usersPassword.get(username).equals(password)) {
                user = users.get(username);
                userInfoComplete();
                userPage();
                return;
            }
            else print("学号/工号或密码错误");
        }
    }

    //学生注册页
    private static void registerAsStudent() {
        String username;
        String password;

        //输入学号
        print("请输入学号（长度为10位，且以3123开头），若取消注册，请输入“back”");
        while(true) {
            if(!sc.hasNextLine()) continue;
            username = sc.nextLine();

            //用户是否选择退出
            if(username.equalsIgnoreCase("back")) {
                page = Page.LOGIN;
                return;
            }

            //输入是否符合条件
            if(usersPassword.containsKey(username)) {
                print("该学号已被注册");
                continue;
            }

            if(username.matches("^[0-9]{10}$") && username.startsWith("3123")) break;
            print("学号必须长度为10位，且以3123开头");
        }

        //输入密码
        password = buildPassword();
        if(password == null) {
            page = Page.LOGIN;
            return;
        }

        while(!databaseManager.addUserPassword(username, password)) {
            print("添加账号失败，请检查数据库连接");
            print("输入 Enter 重试，或“back”返回");
            if(!sc.hasNextLine()) continue;
            if(sc.nextLine().equalsIgnoreCase("back")) return;
        }
        while(!databaseManager.addUser(username, false)) {
            print("添加账号失败，请检查数据库连接");
            print("输入 Enter 重试，或“back”返回");
            if(!sc.hasNextLine()) continue;
            if(sc.nextLine().equalsIgnoreCase("back")) return;
        }

        usersPassword.put(username, password);
        user = new User(username, password, false);
        users.put(username, user);

        print("注册成功！按下 Enter 键返回");
        sc.nextLine();
        userInfoComplete();
        userPage();
    }

    //管理员注册页
    private static void registerAsAdmin() {
        String code;
        String username;
        String password;

        print("请输入管理员注册码，若取消注册，请输入“back”");
        while(true) {
            if(!sc.hasNextLine()) continue;
            code = sc.nextLine();

            if(code.equalsIgnoreCase("back")) {
                page = Page.LOGIN;
                return;
            }

            if(!adminCode.contains(code)) {
                print("该注册码无效，请重新输入");
                continue;
            }
            break;
        }

        //输入工号
        print("请输入工号（长度为10位，且以4234开头），若取消注册，请输入“back”");
        while(true) {
            if(!sc.hasNextLine()) continue;
            username = sc.nextLine();

            //用户是否选择退出
            if(username.equalsIgnoreCase("back")) {
                page = Page.LOGIN;
                return;
            }

            //输入符合条件
            if(usersPassword.containsKey(username)) {
                print("该工号已被注册");
                continue;
            }

            if(username.matches("^[0-9]{10}$") && username.startsWith("4234")) break;
            print("工号必须长度为10位，且以4234开头");
        }

        //输入密码
        password = buildPassword();
        if(password == null) {
            page = Page.LOGIN;
            return;
        }

        while(!databaseManager.deleteAdminCore(code)) {
            print("验证注册码失败，请检查数据库连接");
            print("输入 Enter 重试，或“back”返回");
            if(!sc.hasNextLine()) continue;
            if(sc.nextLine().equalsIgnoreCase("back")) return;
        }
        adminCode.remove(code);
        while(!databaseManager.addUserPassword(username, password)) {
            print("添加账号失败，请检查数据库连接");
            print("输入 Enter 重试，或“back”返回");
            if(!sc.hasNextLine()) continue;
            if(sc.nextLine().equalsIgnoreCase("back")) return;
        }
        while(!databaseManager.addUser(username, true)) {
            print("添加账号失败，请检查数据库连接");
            print("输入 Enter 重试，或“back”返回");
            if(!sc.hasNextLine()) continue;
            if(sc.nextLine().equalsIgnoreCase("back")) return;
        }

        usersPassword.put(code, password);
        user = new User(username, password, true);
        users.put(username, user);

        print("注册成功！按下 Enter 键返回");
        sc.nextLine();
        userInfoComplete();
        userPage();
    }

    //用户设置出一条密码，若用户取消设置，将返回 null
    private static String buildPassword() {
        String password;
        String checkPassword;

        //输入密码
        print("请输入密码（为数字及字母的组合，长度6~16）");
        while(true) {
            if(!sc.hasNextLine()) continue;
            password = sc.nextLine();

            if(password.equalsIgnoreCase("back")) return null;

            if(password.matches("^[0-9a-zA-Z]{6,16}$")) break;
            print("密码必须为数字及字母的组合，且长度6~16");
        }

        //确认密码
        print("确认密码");
        while(true) {
            if(!sc.hasNextLine()) continue;
            checkPassword = sc.nextLine();

            if(checkPassword.equalsIgnoreCase("back")) return null;

            if(checkPassword.equals(password)) break;
            print("前后密码不一致，请重新输入");
        }

        return password;
    }

    //完善用户信息
    private static void userInfoComplete() {
        String name = user.getName();
        String phone = user.getPhone();
        String sex = user.getSex();
        int age = user.getAge();
        LocalDate birthday = user.getDateOfBirth();
        String personCode = user.getPersonCode();

        if(user.getName() == null || user.getName().isEmpty()) {
            print("请输入您的姓名，长度不超过16个字");
            while (true) {
                if (!sc.hasNextLine()) continue;
                name = sc.nextLine();

                if(name.contains(" ") || name.isEmpty()) {
                    print("名字不能带有空格");
                    continue;
                }

                if(name.length() > 16) {
                    print("您输入的名字太长了!");
                    continue;
                }

                break;
            }
        }

        if(user.getPhone() == null || user.getPhone().isEmpty()) {
            print("请输入您的电话号码（以1开头，长度为11位的数字）");

            while (true) {
                if (!sc.hasNextLine()) continue;

                phone = sc.nextLine();
                if (!(phone.matches("[0-9]{11}") && phone.startsWith("1"))) {
                    print("电话号码必须为以1开头，长度为11位的数字");
                    continue;
                }

                break;
            }
        }

        if(user.getSex() == null || user.getSex().isEmpty()) {

            print("请选择您的性别：");
            print("1-男");
            print("2-女");

            while (true) {
                if (!sc.hasNextLine()) continue;
                sex = sc.nextLine();

                if (sex.equals("1")) sex = "男";
                else if (sex.equals("2")) sex = "女";
                else {
                    print("请输入以上选项");
                    continue;
                }

                break;
            }
        }

        if(user.getAge() == 0) {
            String input;
            print("请输入您的年龄");

            while (true) {
                if (!sc.hasNextLine()) continue;
                input = sc.nextLine();

                if(input.matches("^\\d+$")) {
                    try {
                        age = Integer.parseInt(input);
                        if(age < 1) {
                            print("您输入的数字太小了！");
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        print("您输入的数字太了！");
                        continue;
                    }
                    break;
                }
            }
        }

        if(user.getDateOfBirth() == null) {
            int year;
            int month;
            int day;
            String input;

            print("请输入您的出生日期");
            print("年：");
            while (true) {
                if (!sc.hasNextLine()) continue;
                input = sc.nextLine();
                if(!input.matches("^(19|20)[0-9]{2}$")) {
                    print("年份应为数字且以19或20开头，不超过4位");
                    continue;
                }
                year = Integer.parseInt(input);
                break;
            }

            print("月：");
            while (true) {
                if (!sc.hasNextLine()) continue;
                input = sc.nextLine();
                if(!input.matches("^(0?[1-9]|1[0-2])$")) {
                    print("月数应为数字，且大于0小于13");
                    continue;
                }
                month = Integer.parseInt(input);
                break;
            }

            print("日：");
            while (true) {
                if (!sc.hasNextLine()) continue;
                input = sc.nextLine();
                if(!input.matches("^(0?[1-9]|[12][0-9]|3[01])$")) {
                    print("日期应大于0小于31");
                    continue;
                }
                day = Integer.parseInt(input);

                if(month == 2) {
                    if((year % 4==0 && year % 100 != 0) || year % 400 == 0) {
                        if(day > 29) {
                            print("日期应小于30");
                            continue;
                        }
                    }
                    else if(day > 28) {
                        print("日期应小于29");
                        continue;
                    }
                }

                if(day > 30 && !((month < 8 && month % 2 == 1) || (month > 7 && month % 2 == 0))) {
                    print("日期应小于31");
                    continue;
                }

                birthday = LocalDate.of(year, month, day);
                break;
            }
        }

        if(user.getPersonCode() == null || user.getPersonCode().isEmpty()) {
            print("请输入您的身份证");

            while(true) {
                if (!sc.hasNextLine()) continue;
                personCode = sc.nextLine();

                if(!personCode.matches("^(?!0)\\d{6}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}(\\d|x|X)$")) {
                    print("您所输入的身份证不符合身份证格式，请重新输入");
                    continue;
                }

                break;
            }
        }

        while(!databaseManager.updateUser(user.getId(), name, phone, personCode, sex, birthday, age)) {
            print("信息修改失败，请检查数据库连接");
            print("输入 Enter 重试，或“back”返回");
            if(!sc.hasNextLine()) continue;
            if(sc.nextLine().equalsIgnoreCase("back")) return;
        }

        user.setName(name);
        user.setPhone(phone);
        user.setSex(sex);
        user.setAge(age);
        user.setDateOfBirth(birthday);
        user.setPersonCode(personCode);

    }

    //修改密码页
    private static void resetPasswordPage(boolean isLogin) {
        if(isLogin) {
            resetPassword(user);
            return;
        }

        String input;
        print("请输入您的学/工号");
        while (true) {
            if(!sc.hasNextLine()) continue;
            input = sc.nextLine();
            if(input.equalsIgnoreCase("back")) {
                page = Page.LOGIN;
                return;
            }
            break;
        }
        resetPassword(users.get(input));
    }

    private static void resetPassword(User u) {
        String input;
        String password;

        //验证手机号
        print("请输入该账号所绑定的电话号码，若忘记电话号码或未绑定，请联系相关管理员");
        print("输入“back可返回上一页”");
        while (true) {
            if (!sc.hasNextLine()) continue;
            input = sc.nextLine();

            if(input.equals("back")) {
                if(user == null) page = Page.LOGIN;
                else page = (user.isAdmin()) ? Page.ADMIN : Page.LOGIN;
                return;
            }

            if(u==null || !u.getPhone().equals(input)) {
                print("账号或手机号错误");
                continue;
            }
            break;
        }

        //重置密码
        password = buildPassword();
        if(password == null) {
            if(user == null) page = Page.LOGIN;
            else page = (user.isAdmin()) ? Page.ADMIN : Page.LOGIN;
            return;
        }

        while(!databaseManager.updateUserPassword(u.getId(), password)) {
            print("修改密码失败，请检查数据库连接");
            print("输入 Enter 重试，或“back”返回");
            if(!sc.hasNextLine()) continue;
            if(sc.nextLine().equalsIgnoreCase("back")) return;
        }

        u.setPassword(password);
        usersPassword.put(u.getId(), password);
        user = null;
        page = Page.LOGIN;
    }

    //跳转到用户界面
    private static void userPage() {
        if(user.isAdmin()) page = Page.ADMIN;
        else page = Page.STUDENT;
    }

    //学生主页
    private static void studentPage() {
        print("当前登录用户：" + user.getId() + " " + user.getName());
        print("1-查看个人信息");
        print("2-修改密码");
        print("3-退出登录");
        print("4-注销账号");
        print("输入选项选择操作:");

        while(true) {
            if (!sc.hasNextLine()) continue;

            switch(sc.nextLine()) {
                case "1": page = Page.USER_INFO; return;
                case "2": page = Page.RESET_PASSWORD; return;
                case "3":
                    page = Page.LOGIN;
                    user = null;
                    return;
                case "4":
                    print("确定注销该账号码？该操作不可逆！");
                    print("1-确定");
                    print("2-取消");
                    while (true) {
                        if (!sc.hasNextLine()) continue;
                        switch(sc.nextLine()) {
                            case "1":
                                while(!databaseManager.deleteUser(user.getId())) {
                                    print("注销账号失败，请检查数据库连接");
                                    print("输入 Enter 重试，或“back”返回");
                                    if(!sc.hasNextLine()) continue;
                                    if(sc.nextLine().equalsIgnoreCase("back")) return;
                                }
                                while(!databaseManager.deleteUserPassword(user.getId())) {
                                    print("注销账号失败，请检查数据库连接");
                                    print("输入 Enter 重试，或“back”返回");
                                    if(!sc.hasNextLine()) continue;
                                    if(sc.nextLine().equalsIgnoreCase("back")) return;
                                }
                                usersPassword.remove(user.getId());
                                users.remove(user.getId());
                                user = null;
                                page = Page.LOGIN;
                                return;
                            case "2": return;
                        }
                    }
                default: print("请输入所给选项");
            }
        }
    }

    //管理员主页
    private static void adminPage() {
        print("当前登录用户：" + user.getId() + " " + user.getName());
        print("1-查看个人信息");
        print("2-修改密码");
        print("3-查看所有用户信息");
        print("4-搜索用户");
        print("5-获取管理员注册码");
        print("6-退出登录");
        print("输入选项选择操作:");

        while(true) {
            if (!sc.hasNextLine()) continue;

            switch(sc.nextLine()) {
                case "1": page = Page.USER_INFO; return;
                case "2": page = Page.RESET_PASSWORD; return;
                case "3": page = Page.USERS; return;
                case "4": page = Page.SEARCH; return;
                case "5": page = Page.ADMIN_CORE; return;
                case "6":
                    page = Page.LOGIN;
                    user = null;
                    return;
                default: print("请输入所给选项");
            }
        }
    }

    //列举用户页
    private static void usersListPage(ArrayList<String> uIds) {
        int select;
        int maxPage = uIds.size()/10 + (uIds.size()%10 == 0 && !uIds.isEmpty() ? 0 : 1);
        int listPage = 1;
        String input;

        while(true) {
            for(int index = 0; index < 10 && index + (listPage - 1) * 10 < uIds.size(); index++) {
                print((index + 1) + "-\t" + uIds.get(index + (listPage - 1) * 10) + "\t" + users.get(uIds.get(index + (listPage - 1) * 10)).getName());
            }
            print("第" + listPage + "页，共" + maxPage + "页");

            print("输入“<”或“>”以翻页，数字以查看个人信息，“goto 页数”跳转到对应页或“back”返回");

            while(true) {
                if(!sc.hasNextLine()) continue;
                input = sc.nextLine();
                if(input.equalsIgnoreCase("back")) {
                    page = Page.ADMIN;
                    return;
                }
                else if(input.equals("<")) {
                    listPage = Math.max(1, listPage - 1);
                    clearScreen();
                    break;
                }
                else if(input.equals(">")) {
                    listPage = Math.min(maxPage, listPage + 1);
                    clearScreen();
                    break;
                }
                else if(input.split(" ").length == 2 && input.split(" ")[0].equalsIgnoreCase("goto")) {
                    String p = input.split(" ")[1];
                    if(p.matches("^\\d+$")) {
                        try {
                            listPage = (Integer.parseInt(p) > 1) ? Math.min(maxPage, Integer.parseInt(p)) : 1;
                        } catch(NumberFormatException e) {
                            listPage = maxPage;
                        }
                        clearScreen();
                        break;
                    }
                    else print("无效的输入");
                }
                else if(input.matches("^\\d+$")) {
                    try {
                         select = Math.min(Integer.parseInt(input) + (listPage - 1) * 10, uIds.size()) - 1;
                         if(select < 0) print("无效的输入");
                         else {
                             selectedUser = users.get(uIds.get(select));
                             page = Page.PERSON_INFO;
                             return;
                         }
                    } catch (NumberFormatException e) {
                        print("无效的输入");
                    }
                }
                else print("无效的输入");
            }
        }
    }

    //查看当前用户个人信息
    private static void userInfoPage() {
        String input;
        print("学/工号：\t" + user.getId());
        print("账号类型：\t" + ((user.isAdmin()) ? "管理员" : "学生"));
        print("1-姓名：   \t" + user.getName());
        print("2-性别：   \t" + user.getSex());
        print("3-身份证号：\t" + user.getPersonCode());
        print("4-电话号码：\t" + user.getPhone());
        print("5-生日：   \t" + user.getDateOfBirth());
        print("6-年龄：   \t" + user.getAge());

        while(true) {
            print("输入1~6选择你要修改的信息，或输入“back”以返回上一页");
            if (!sc.hasNextLine()) continue;
            input = sc.nextLine();

            switch(input) {
                case "1": user.setName(null); break;
                case "2": user.setSex(null); break;
                case "3": user.setPersonCode(null); break;
                case "4": user.setPhone(null); break;
                case "5": user.setDateOfBirth(null); break;
                case "6": user.setAge(0); break;
                case "back" :
                    page = (user.isAdmin()) ? Page.ADMIN : Page.STUDENT;
                    break;
                default: continue;
            }
            userInfoComplete();
            return;
        }
    }

    //查看所选的用户的信息
    private static void userInfoPage(User user) {
        print("学/工号：\t" + user.getId());
        print("账号类型：\t" + ((user.isAdmin()) ? "管理员" : "学生"));
        print("姓名：   \t" + user.getName());
        print("性别：   \t" + user.getSex());
        print("身份证号：\t" + user.getPersonCode());
        print("电话号码：\t" + user.getPhone());
        print("生日：   \t" + user.getDateOfBirth());
        print("年龄:    \t" + user.getAge());

        print("输入“back”以返回");
        while(true) {
            if(!sc.hasNextLine()) continue;
            if (sc.nextLine().equalsIgnoreCase("back")) {
                page = Page.USERS;
                selectedUser = null;
                return;
            }
        }
    }

    //查看管理员注册码
    private static void adminCodePage() {
        int maxPage = adminCode.size()/10 + (adminCode.size()%10 == 0 && !adminCode.isEmpty()? 0 : 1);
        int listPage = 1;

        while(true) {
            for(int index = 0; index < 10 && index + (listPage - 1) * 10 < adminCode.size(); index++) {
                print((index + 1) + "-\t" + adminCode.get(index + (listPage - 1) * 10));
            }
            print("第" + listPage + "页，共" + maxPage + "页");

            print("输入“<”或“>”以翻页，“add”新增管理员注册码，“del”删除，数字跳转到对应页或“back”返回");
            String input;

            while(true) {
                if(!sc.hasNextLine()) continue;
                input = sc.nextLine();

                if(input.equalsIgnoreCase("back")) {
                    page = Page.ADMIN;
                    return;
                }
                else if(input.equals("<")) {
                    listPage = Math.max(1, listPage - 1);
                    clearScreen();
                    break;
                }
                else if(input.equals(">")) {
                    listPage = Math.min(maxPage, listPage + 1);
                    clearScreen();
                    break;
                }
                else if(input.matches("^\\d+$")) {
                    try {
                        listPage = (Integer.parseInt(input) > 1) ? Math.min(maxPage, Integer.parseInt(input)) : 1;

                    } catch (NumberFormatException e) {
                        listPage = maxPage;
                    }
                    clearScreen();
                    break;
                }
                else if(input.equals("add")) {
                    addAdminCode();
                    return;
                }
                else if (input.equals("del")) {
                    if(!adminCode.isEmpty()) adminCode.remove(0);
                    return;
                }
                else print("无效的输入");
            }
        }
    }

    //新增管理员注册码
    private static void addAdminCode() {
        String core = UUID.randomUUID().toString();
        while(true) {
            if(adminCode.contains(core)) core = UUID.randomUUID().toString();
            else break;
        }

        while(!databaseManager.addAdminCore(core)) {
            print("添加注册码失败，请检查数据库连接");
            print("输入 Enter 重试，或“back”返回");
            if(!sc.hasNextLine()) continue;
            if(sc.nextLine().equalsIgnoreCase("back")) return;
        }

        adminCode.add(core);
    }

    //搜索页
    private static void searchPage() {
        print("用户搜索页");
        print("1-根据学/工号搜索（从第一位开始匹配）");
        print("2-根据学/工号搜索（从任意部分匹配）");
        print("3-根据名字搜索（从第一个字开始匹配）");
        print("4-根据名字搜索（从任意部分匹配）");
        print("5-根据性别搜索");
        print("6-根据年龄搜索");
        print("7-根据出生月份搜索");
        print("输入以上选项进行搜索，或输入“back”返回上一页");

        String input;
        while(true) {
            if(!sc.hasNextLine()) continue;
            input = sc.nextLine();

            switch(input) {
                case "1" : usersListPage(searchById(true));  return;
                case "2" : usersListPage(searchById(false));  return;
                case "3" : usersListPage(searchBy("NAME", true));  return;
                case "4" : usersListPage(searchBy("NAME", false));  return;
                case "5" : usersListPage(searchBy("SEX", true));  return;
                case "6" : usersListPage(searchBy("AGE", true));  return;
                case "7" : usersListPage(searchBy("MONTH", true));  return;
                case "back" : page = Page.ADMIN; return;
                default: print("请输入以上选项");
            }
        }


    }

    //根据学/工号搜索
    private static ArrayList<String> searchById(boolean strict) {
        ArrayList<String> us = new ArrayList<>();
        String regex;

        print("请输入您要搜索的内容");
        while(true) {
            if(!sc.hasNextLine()) continue;
            regex = sc.nextLine();
            break;
        }

        if(usersPassword.containsKey(regex)) {
            us.add(regex);
            return us;
        }

        for(String id : usersPassword.keySet()) {
            if((!strict) && id.matches(".*" + regex + ".*")) us.add(id);
            if(strict && id.startsWith(regex)) us.add(id);
        }

        return us;
    }

    //根据其他条件搜索
    private static ArrayList<String> searchBy(String byWhat, boolean strict) {
        ArrayList<String> us = new ArrayList<>();
        String regex;

        print("请输入您要搜索的内容");
        while(true) {
            if(!sc.hasNextLine()) continue;
            regex = sc.nextLine();
            break;
        }

        for(User u : users.values()) {
            switch(byWhat) {
                case "NAME" :
                    if(u.getName() == null) break;
                    if(strict && u.getName().matches("^" + regex + ".*")) us.add(u.getId());
                    else if((!strict) && u.getName().matches(".*" + regex + ".*")) us.add(u.getId());
                    break;
                case "SEX"  :
                    if(u.getSex() == null) break;
                    if(u.getSex().equals(regex)) us.add(u.getId()); break;
                case "AGE"  :
                    if(regex.equals(String.valueOf(u.getAge()))) us.add(u.getId()); break;
                case "MONTH":
                    if(u.getDateOfBirth() == null) break;
                    if(regex.equals(String.valueOf(u.getDateOfBirth().getMonth().getValue()))) us.add(u.getId()); break;

            }
        }


        return us;
    }

    //加载配置
    private static boolean loadConfig() {
        config = new Config();
        return config.isLoaded();
    }

    //清屏
    private static void clearScreen() {
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (IOException | InterruptedException e) {    //清屏 Plan B
            print(e.getMessage());
            print("\033[H\033[2J");
            System.out.flush();
        }
    }

    //输出一行文本
    private static void print(String msg) {
        System.out.println(msg);
    }

    //创建第一个管理员
    private static void fistAdmin() {
        String username = config.getAdminUsername();
        String password = config.getAdminPassword();
        if(!usersPassword.containsKey(username)) {

            while(!databaseManager.addUserPassword(username, password)) {
                print("初始化失败，请检查数据库连接");
                print("输入 Enter 重试，或“back”返回");
                if(!sc.hasNextLine()) continue;
                if(sc.nextLine().equalsIgnoreCase("back")) return;
            }
            while(!databaseManager.addUser(username, true)) {
                print("初始化失败，请检查数据库连接");
                print("输入 Enter 重试，或“back”返回");
                if(!sc.hasNextLine()) continue;
                if(sc.nextLine().equalsIgnoreCase("back")) return;
            }

            usersPassword.put(username, password);
            users.put(username, new User(username, password, true));
        }
    }
}
