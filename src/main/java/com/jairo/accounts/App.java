
package com.jairo.accounts;

import com.jairo.accounts.javalin.JavalinApp;

public class App {

    public static void main(String[] args) {
        JavalinApp javalinApp = new JavalinApp();
        javalinApp.getApp().start(8080);
    }

}
