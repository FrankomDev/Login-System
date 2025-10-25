package io.github.frankomdev.login;

public class LangFile {

    public String already_registered; // "§4You are already registered! Use /login";
    public String rejoin; // "§1Registered! Please rejoin";
    public String logged_in; // "§9Logged in"
    public String wrong_password; // "§4Wrong password!"
    public String login_hint; // "§aPlease log in (/login <password>)"
    public String register_hint; //"§aPlease register (/register <password>)"

    public LangFile(String already_registered, String rejoin, String logged_in, String wrong_password, String login_hint, String register_hint){
        this.already_registered = already_registered;
        this.rejoin = rejoin;
        this.logged_in = logged_in;
        this.wrong_password = wrong_password;
        this.login_hint = login_hint;
        this.register_hint = register_hint;
    }

}
