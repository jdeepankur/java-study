package com.testapp.view;

public class chatPage {
    public static String header(String welcome) {
        return String.format("""
        <html>
        <head>
            <title>Chatbot</title>
        </head>
        <body>
            <h1>%s</h1>
        """, welcome);
    }

    public static String getPage(String welcome) {
        return header(welcome) + entry() + chatlog();
    }
}
