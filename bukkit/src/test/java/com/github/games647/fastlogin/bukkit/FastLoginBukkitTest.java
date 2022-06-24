package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.core.CommonUtil;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

import junit.framework.TestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FastLoginBukkitTest extends TestCase {

    @Test
    public void testRGB() {
        var message = "&x00002a00002b&lText";
        var msg = CommonUtil.translateColorCodes(message);
        assertThat(msg, is("§x00002a00002b§lText"));

        var components = TextComponent.fromLegacyText(msg);
        var expected = """
            {"bold":true,"color":"#00a00b","text":"Text"}""";
        assertThat(ComponentSerializer.toString(components), is(expected));
    }
}
