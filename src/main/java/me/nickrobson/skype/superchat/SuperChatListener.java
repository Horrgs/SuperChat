package me.nickrobson.skype.superchat;

import in.kyle.ezskypeezlife.Chat;
import in.kyle.ezskypeezlife.api.SkypeConversationType;
import in.kyle.ezskypeezlife.api.SkypeUserRole;
import in.kyle.ezskypeezlife.api.obj.SkypeConversation;
import in.kyle.ezskypeezlife.api.obj.SkypeMessage;
import in.kyle.ezskypeezlife.api.obj.SkypeUser;
import in.kyle.ezskypeezlife.events.conversation.SkypeConversationUserJoinEvent;
import in.kyle.ezskypeezlife.events.conversation.SkypeConversationUserLeaveEvent;
import in.kyle.ezskypeezlife.events.conversation.SkypeMessageEditedEvent;
import in.kyle.ezskypeezlife.events.conversation.SkypeMessageReceivedEvent;
import in.kyle.ezskypeezlife.events.user.SkypeContactRequestEvent;
import me.nickrobson.skype.superchat.cmd.Command;

public class SuperChatListener {

    public void join(SkypeConversationUserJoinEvent event) {
        SkypeUser user = event.getUser();
        SkypeConversation convo = event.getConversation();
        GroupConfiguration cfg = SuperChatController.GCONFIGS.get(convo.getLongId());
        if (cfg != null && cfg.isShowJoinMessage()) {
            String welcome = String.format(SuperChatController.WELCOME_MESSAGE_JOIN, user.getUsername(), convo.getTopic());
            String help = "You can access my help menu by typing `" + SuperChatController.COMMAND_PREFIX + "help`";
            String message = MessageBuilder.html_escape(Chat.bold(welcome)) + "\n" + MessageBuilder.html_escape(help);
            convo.sendMessage(message);
        }
    }

    public void leave(SkypeConversationUserLeaveEvent event) {
        SuperChatController.wipe(event.getUser().getUsername());
    }

    public void contactRequest(SkypeContactRequestEvent event) {
        event.getSkypeUser().setContact(true);
    }

    public synchronized void chat(SkypeMessageReceivedEvent event) {
        cmd(event.getMessage());
    }

    public synchronized void chat(SkypeMessageEditedEvent event) {
        SkypeConversation convo = event.getSkypeMessage().getConversation();
        GroupConfiguration conf = SuperChatController.GCONFIGS.get(convo.getLongId());
        boolean isGroup = convo.getConversationType() == SkypeConversationType.GROUP;
        if (isGroup && conf != null && conf.isShowEditedMessages()) {
            MessageBuilder mb = new MessageBuilder();
            mb.bold(true).text("( " + event.getSkypeUser().getUsername() + " )").bold(false);
            mb.text("Edited their message:").newLine();
            mb.text(event.getContentOld()).newLine();
            mb.text("Became:").newLine();
            mb.text(event.getContentNew());
            convo.sendMessage(mb.build());
        }
        cmd(event.getSkypeMessage());
    }

    public synchronized void cmd(SkypeMessage message) {
        SkypeUser user = message.getSender();
        SkypeConversation group = message.getConversation();
        String msg = message.getMessage().trim();
        String[] words = msg.split("\\s+");

        if (msg.isEmpty() || words.length == 0 || !words[0].startsWith(SuperChatController.COMMAND_PREFIX)) {
            return;
        }

        String cmdName = words[0].substring(SuperChatController.COMMAND_PREFIX.length()).toLowerCase();
        Command cmd = SuperChatController.COMMANDS.get(cmdName);
        if (cmd == null)
            return;

        GroupConfiguration cfg = SuperChatController.GCONFIGS.get(group.getLongId());
        if (cfg != null && !cfg.isCommandEnabled(cmd) && !cmd.alwaysEnabled())
            return;
        if (cfg == null && !cmd.alwaysEnabled())
            return;

        String[] args = new String[words.length - 1];
        for (int i = 1; i < words.length; i++)
            args[i - 1] = words[i];

        boolean userchat = group.getConversationType() == SkypeConversationType.USER && cmd.userchat();

        if (cmd.role() == SkypeUserRole.USER || group.isAdmin(user) || userchat)
            cmd.exec(user, group, cmdName, args, message);
    }

}
