package com.github.games647.fastlogin.bukkit.hooks;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.proxy.NoOp;

import org.bukkit.entity.Player;

import ultraauth.api.UltraAuthAPI;

/**
 * Project page: http://dev.bukkit.org/bukkit-plugins/ultraauth-aa/
 */
public class UltraAuthHook implements AuthPlugin {

    @Override
    public void forceLogin(Player player) {
        UltraAuthAPI.authenticatedPlayer(player);
    }

    @Override
    public boolean isRegistered(final String playerName) {
        return UltraAuthAPI.isRegisterd(createFakePlayer(playerName));
    }

    private Player createFakePlayer(final String playerName) {
        Callback implementation = new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                String methodName = method.getName();

                if (methodName.equals("getName")) {
                    return playerName;
                }

                // Ignore all other methods
                throw new UnsupportedOperationException(
                        "The method " + method.getName() + " is not supported for temporary players.");
            }
        };

        // CGLib is amazing
        Enhancer ex = new Enhancer();
        ex.setInterfaces(new Class<?>[]{Player.class});
        ex.setCallbacks(new Callback[]{NoOp.INSTANCE, implementation});

        return (Player) ex.create();
    }

    @Override
    public void forceRegister(Player player, String password) {
        UltraAuthAPI.setPlayerPasswordOnline(player, password);
    }
}
