package io.github.frankomdev.login;
import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.GlobalPos;

import java.io.*;
import java.util.Optional;

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

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->{
            dispatcher.register(CommandManager.literal("register").then(CommandManager.argument("password", StringArgumentType.string())
                    .executes(context->{
                        //context.getSource().sendFeedback(()->Text.literal("Called command"), false);
                        if (check_user(context.getSource().getPlayer().getName().getString())){
                            context.getSource().getPlayer().sendMessage(Text.of("§4Już jesteś zarejestrowany! Użyj /login"));
                        }else {
                            String password = StringArgumentType.getString(context, "password");
                            register_user(context.getSource().getPlayer().getName().getString(), password);
                            context.getSource().getPlayer().networkHandler.disconnect(Text.of("§1Zarejestrowano! Wejdź ponownie"));
                        }
                        return  1;
                    })));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->{
            dispatcher.register(CommandManager.literal("login").then(CommandManager.argument("password", StringArgumentType.string())
                    .executes(context->{
                        String password = StringArgumentType.getString(context, "password");
                        if (check_password(context.getSource().getPlayer().getName().getString(), password)){
                            context.getSource().getPlayer().sendMessage(Text.of("§9Zalogowano"));
                            context.getSource().getPlayer().removeStatusEffect(StatusEffects.SLOWNESS);
                            context.getSource().getPlayer().removeStatusEffect(StatusEffects.BLINDNESS);
                            context.getSource().getPlayer().removeStatusEffect(StatusEffects.MINING_FATIGUE);
                        } else{
                            context.getSource().getPlayer().networkHandler.disconnect(Text.of("§4Złe hasło!"));
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
                player.sendMessage(Text.of("Zaloguj sie (/login <haslo>)"));
            } else {
                player.sendMessage(Text.of("Zarejestruj sie (/register <haslo>)"));
            }
        });

    }

}
