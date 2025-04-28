package com.brandon3055.brandonscore.client;

import com.brandon3055.brandonscore.CommonProxy;
import com.brandon3055.brandonscore.handlers.IProcess;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * Created by Brandon on 14/5/2015.
 */
public class ClientProxy extends CommonProxy {

    @Override
    public MinecraftServer getMCServer() {
        return super.getMCServer();
    }

    @Override
    public Level getClientWorld() {
        return Minecraft.getInstance().level;
    }

    @Override
    public boolean isCTRLKeyDown() {
        return Screen.hasControlDown();
    }

    @Override
    public Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public void addProcess(IProcess iProcess) {
        ProcessHandlerClient.addProcess(iProcess);
    }

    @Override
    public void runSidedProcess(IProcess process) {
        ProcessHandlerClient.addProcess(process);
    }

    @Override
    public void sendIndexedMessage(Player player, Component message, MessageSignature signature) {
        Minecraft mc = Minecraft.getInstance();
        deleteMessage(signature);
        if (message == null) return;
        mc.gui.getChat().addMessage(message, signature, null);
    }

    public void deleteMessage(MessageSignature signature) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gui.getChat() == null) {
            return;
        }
        ChatComponent chat = mc.gui.getChat();
        chat.allMessages.removeIf(e -> Objects.equals(e.signature(), signature));

        chat.trimmedMessages.clear();
        for (int i = chat.allMessages.size() - 1; i >= 0; --i) {
            GuiMessage guiMessage = chat.allMessages.get(i);
            addMessageQuietly(chat, guiMessage.content(), guiMessage.signature(), guiMessage.addedTime(), guiMessage.tag(), true);
        }
    }

    private static void addMessageQuietly(ChatComponent chat, Component component, @Nullable MessageSignature messageSignature, int i, @Nullable GuiMessageTag guiMessageTag, boolean updateOnly) {
        int j = Mth.floor((double) chat.getWidth() / chat.getScale());
        if (guiMessageTag != null && guiMessageTag.icon() != null) {
            j -= guiMessageTag.icon().width + 4 + 2;
        }

        List<FormattedCharSequence> list = ComponentRenderUtils.wrapComponents(component, j, Minecraft.getInstance().font);
        boolean bl2 = chat.isChatFocused();

        for (int k = 0; k < list.size(); ++k) {
            FormattedCharSequence formattedCharSequence = list.get(k);
            if (bl2 && chat.chatScrollbarPos > 0) {
                chat.newMessageSinceScroll = true;
                chat.scrollChat(1);
            }

            boolean bl3 = k == list.size() - 1;
            chat.trimmedMessages.add(0, new GuiMessage.Line(i, formattedCharSequence, guiMessageTag, bl3));
        }

        while (chat.trimmedMessages.size() > 100) {
            chat.trimmedMessages.remove(chat.trimmedMessages.size() - 1);
        }

        if (!updateOnly) {
            chat.allMessages.add(0, new GuiMessage(i, component, messageSignature, guiMessageTag));

            while (chat.allMessages.size() > 100) {
                chat.allMessages.remove(chat.allMessages.size() - 1);
            }
        }
    }

    @Override
    public void setClipboardString(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }
}
