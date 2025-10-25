package io.github.frankomdev.login;
import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

import java.io.*;

public class Login implements DedicatedServerModInitializer{

    public void create_file(String nick, String password) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        UserConfig user = new UserConfig(nick, password);
        try {
            FileWriter fileWriter = new FileWriter("config/login/logins.json");
            gson.toJson(user, fileWriter);
            fileWriter.flush();
        } catch (IOException e){
            System.out.println(e);
        }
    }

    public void register_user(String nick, String password){
        File file = new File("config/login/logins.json");
        if (!file.exists()){
            create_file(nick, password);
        } else{
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try {
                Reader reader = new FileReader("config/login/logins.json");
                JsonElement json = JsonParser.parseReader(reader);
                if (json.isJsonArray()){
                    JsonElement new_user = JsonParser.parseString("{\"username\":"+nick+",\"password\":\""+password+"\"}");
                    json.getAsJsonArray().add(new_user);
                    FileWriter fileWriter = new FileWriter("config/login/logins.json");
                    gson.toJson(json, fileWriter);
                    fileWriter.flush();
                } else{
                    UserConfig new_user = new UserConfig(nick, password);
                    UserConfig old_user = gson.fromJson(json, UserConfig.class);
                    UserConfig[] users = {old_user, new_user};
                    FileWriter fileWriter = new FileWriter("config/login/logins.json");
                    gson.toJson(users, fileWriter);
                    fileWriter.flush();
                }
                reader.close();
            } catch (IOException e){
                System.out.println(e);
            }
        }
    }

    public boolean check_user(String nick){
        Gson gson = new Gson();
        try (Reader reader = new FileReader("config/login/logins.json")){
            JsonElement json = JsonParser.parseReader(reader);
            if (json.isJsonArray()) {
                UserConfig[] users = gson.fromJson(json, UserConfig[].class);
                for (UserConfig u : users) {
                    if (u.username.equals(nick)) {
                        return true;
                    }
                }
                return false;
            } else{
                UserConfig user = gson.fromJson(json, UserConfig.class);
                return user.username.equals(nick);
            }
        } catch (IOException e){
            System.out.println(e);
            return false;
        }
    }

    public boolean check_password(String nick, String password){
        Gson gson = new Gson();
        try (Reader reader = new FileReader("config/login/logins.json")){
            JsonElement json = JsonParser.parseReader(reader);
            if (json.isJsonArray()) {
                UserConfig[] users = gson.fromJson(json, UserConfig[].class);
                for (UserConfig u : users) {
                    if (u.username.equals(nick) && u.password.equals(password)) {
                        return true;
                    }
                }
                return false;
            } else{
                UserConfig user = gson.fromJson(json, UserConfig.class);
                return (user.username.equals(nick) && user.password.equals(password));
            }
        } catch (IOException e){
            System.out.println(e);
            return false;
        }
    }

    public LangFile read_lang_file() {
        Gson gson = new Gson();
        try (FileReader fileReader = new FileReader("config/login/lang.json")) {
            return gson.fromJson(fileReader, LangFile.class);
        } catch (IOException e) {
            System.out.println(e);
        }
        return null;
    }

    public LangFile create_lang_file(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String already_registered = "§4You are already registered! Use /login";
        String rejoin = "§1Registered! Please rejoin";
        String logged_in = "§9Logged in";
        String wrong_password = "§4Wrong password!";
        String login_hint = "§aPlease log in (/login <password>)";
        String register_hint = "§aPlease register (/register <password>)";
        LangFile langFile = new LangFile(already_registered, rejoin, logged_in, wrong_password, login_hint, register_hint);
        try (FileWriter writer = new FileWriter("config/login/lang.json")){
            gson.toJson(langFile, writer);
        }catch (IOException e){
            System.out.println(e);
        }
        return read_lang_file();
    }

    @Override
    public void onInitializeServer(){
        File config_dir = new File("config/login");
        if (!config_dir.exists()){
            boolean creating_dir = config_dir.mkdirs();
            if (creating_dir){
                System.out.println("Created config path");
                //create_config();
            }
        }
        LangFile langFile;
        File lang = new File("config/login/lang.json");
        if (lang.exists()) {
            langFile = read_lang_file();
        }else {
            langFile = create_lang_file();
        }


        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->{
            dispatcher.register(CommandManager.literal("register").then(CommandManager.argument("password", StringArgumentType.string())
                    .executes(context->{
                        //context.getSource().sendFeedback(()->Text.literal("Called command"), false);
                        if (check_user(context.getSource().getPlayer().getName().getString())){
                            context.getSource().getPlayer().sendMessage(Text.of(langFile.already_registered));
                        }else {
                            String password = StringArgumentType.getString(context, "password");
                            register_user(context.getSource().getPlayer().getName().getString(), password);
                            context.getSource().getPlayer().networkHandler.disconnect(Text.of(langFile.rejoin));
                        }
                        return  1;
                    })));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->{
            dispatcher.register(CommandManager.literal("login").then(CommandManager.argument("password", StringArgumentType.string())
                    .executes(context->{
                        String password = StringArgumentType.getString(context, "password");
                        if (check_password(context.getSource().getPlayer().getName().getString(), password)){
                            context.getSource().getPlayer().sendMessage(Text.of(langFile.logged_in));
                            context.getSource().getPlayer().removeStatusEffect(StatusEffects.SLOWNESS);
                            context.getSource().getPlayer().removeStatusEffect(StatusEffects.BLINDNESS);
                            context.getSource().getPlayer().removeStatusEffect(StatusEffects.MINING_FATIGUE);
                        } else{
                            context.getSource().getPlayer().networkHandler.disconnect(Text.of(langFile.wrong_password));
                        }
                        return  1;
                    })));
        });

        ServerPlayerEvents.JOIN.register((player) ->{
            StatusEffectInstance slowness = new StatusEffectInstance(StatusEffects.SLOWNESS, 999999, 999999);
            StatusEffectInstance blindness = new StatusEffectInstance(StatusEffects.BLINDNESS, 999999, 999999);
            StatusEffectInstance mining_fatigue = new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 999999, 999999);
            player.addStatusEffect(slowness);
            player.addStatusEffect(blindness);
            player.addStatusEffect(mining_fatigue);
            if (check_user(player.getName().getString())) {
                player.sendMessage(Text.of(langFile.login_hint));
            } else {
                player.sendMessage(Text.of(langFile.register_hint));
            }
        });

    }

}
