package me.nickrobson.skype.superchat.cmd;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import me.nickrobson.skype.superchat.MessageBuilder;
import xyz.gghost.jskype.Group;
import xyz.gghost.jskype.message.FormatUtils;
import xyz.gghost.jskype.message.Message;
import xyz.gghost.jskype.user.GroupUser;

public class ViewingOrderCommand implements Command {

    @Override
    public String[] names() {
        return new String[] { "vo", "viewingorder" };
    }

    @Override
    public String[] help(GroupUser user, boolean userChat) {
        return new String[] { "[mcu,af]", "shows the advised viewing order for the show" };
    }

    @Override
    public void exec(GroupUser user, Group group, String used, String[] args, Message message) {
        try {
            MessageBuilder builder = new MessageBuilder();
            if (args.length == 0)
                builder.bold(true).text("Usage: ").bold(false).text("~vo [mcu,af]");
            else {
                InputStream stream = getClass().getResourceAsStream("/viewingorder/" + args[0].toLowerCase() + ".txt");
                if (stream == null)
                    builder.text("No viewing order found with name " + args[0].toLowerCase());
                else {
                    builder.bold(true).text("Viewing order for " + args[0].toLowerCase() + ":").bold(false);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.html("\n");
                        String s = "";
                        if (line.startsWith("**"))
                            s = FormatUtils.bold(FormatUtils.encodeRawText(line.substring(2)));
                        else if (line.startsWith("//"))
                            s = FormatUtils.italic(FormatUtils.encodeRawText(line.substring(2)));
                        else
                            s = FormatUtils.encodeRawText(line);
                        builder.html(s);
                    }
                    reader.close();
                }
            }
            sendMessage(group, builder.build());
        } catch (Exception ex) {
            sendMessage(group, encode("Looks like an error occurred!"));
            ex.printStackTrace();
        }
    }

}
