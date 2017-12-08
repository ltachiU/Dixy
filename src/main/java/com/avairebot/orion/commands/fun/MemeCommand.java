package com.avairebot.orion.commands.fun;

import com.avairebot.orion.Orion;
import com.avairebot.orion.cache.CacheType;
import com.avairebot.orion.chat.SimplePaginator;
import com.avairebot.orion.contracts.commands.Command;
import com.avairebot.orion.factories.MessageFactory;
import com.avairebot.orion.utilities.NumberUtil;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

public class MemeCommand extends Command {

    private final String customUrl = "https://memegen.link/custom/%s/%s.jpg?size=256&alt=%s&discordFormat=some-avatar.png";
    private final String templateUrl = "https://memegen.link/%s/%s/%s.jpg";

    private final Map<String, Map<String, String>> memes = new HashMap<>();
    private final List<String> memeKeys = new ArrayList<>();

    public MemeCommand(Orion orion) {
        super(orion);
    }

    @Override
    public String getName() {
        return "Meme Command";
    }

    @Override
    public String getDescription() {
        return "Generates memes with your given text, you can tag users to use their avatar as a meme, or just give the meme name you wanna use.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Arrays.asList(
            "`:command list` - Lists all the available meme types.",
            "`:command <meme> <top text> <bottom text>` - Generates the meme with the given text.",
            "`:command <user> <top text> <bottom text>` - Generates a meme with the tagged users avatar and the given text."
        );
    }

    @Override
    public String getExampleUsage() {
        return String.join("\n", Arrays.asList(
            "`:command buzz \"Memes\" \"Memes everywhere\"`",
            "`:command @Senither \"Creates a Meme command for Orion\" \"Almost no one uses it\"`"

        ));
    }

    @Override
    public List<String> getTriggers() {
        return Collections.singletonList("meme");
    }

    @Override
    public List<String> getMiddleware() {
        return Collections.singletonList("throttle:user,2,6");
    }

    @Override
    public boolean onCommand(Message message, String[] args) {
        if (args.length == 0) {
            return sendErrorMessage(message,
                "Missing `action` argument, you must include the meme type or user you want to "
                    + "generate a meme for, or `list` to display all the memes types available."
            );
        }

        if (memes.isEmpty() || memeKeys.isEmpty()) {
            loadMemesIntoMemory();
        }

        if (args[0].equalsIgnoreCase("list")) {
            return sendMemeList(message, Arrays.copyOfRange(args, 1, args.length));
        }

        if (!message.getMentionedUsers().isEmpty()) {
            return sendUserMeme(message, message.getMentionedUsers().get(0), Arrays.copyOfRange(args, 1, args.length));
        }

        if (memeKeys.contains(args[0].toLowerCase())) {
            return sendGeneratedMeme(message, args[0].toLowerCase(), Arrays.copyOfRange(args, 1, args.length));
        }

        return sendErrorMessage(message, "Invalid meme type given, `%s` is not a valid meme type!", args[0]);
    }

    private boolean sendMemeList(Message message, String[] args) {
        if (memes.isEmpty() || memeKeys.isEmpty()) {
            loadMemesIntoMemory();
        }

        SimplePaginator paginator = new SimplePaginator(memeKeys, 10, 1);
        if (args.length > 0) {
            paginator.setCurrentPage(NumberUtil.parseInt(args[0], 1));
        }

        final List<String> memesMessages = new ArrayList<>();
        paginator.forEach((index, key, val) -> {
            memesMessages.add(String.format("`%s` => `%s`",
                val, memes.get(val).get("name")
            ));
        });

        MessageFactory.makeSuccess(message, String.format("%s\n\n%s",
            String.join("\n", memesMessages),
            paginator.generateFooter(generateCommandTrigger(message) + " list")
        )).setTitle("Memes").queue();

        // We're returning false here to prevent the Meme command from
        // being throttled for users just wanting to see what types
        // of memes are available without generating any memes.
        return false;
    }

    private boolean sendUserMeme(Message message, User user, String[] args) {
        if (args.length < 2) {
            return sendErrorMessage(message, "You must include the `top text` and `bottom text` arguments to generate a meme.");
        }

        try {
            MessageFactory.makeEmbeddedMessage(message.getChannel())
                .setImage(String.format(
                    customUrl,
                    formatMemeArgument(args[0]),
                    formatMemeArgument(args[1]),
                    URLEncoder.encode(user.getAvatarUrl(), "UTF-8")
                )).queue();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean sendGeneratedMeme(Message message, String meme, String[] args) {
        if (args.length < 2) {
            return sendErrorMessage(message, "You must include the `top text` and `bottom text` arguments to generate a meme.");
        }

        MessageFactory.makeEmbeddedMessage(message.getChannel())
            .setImage(String.format(
                templateUrl,
                meme,
                formatMemeArgument(args[0]),
                formatMemeArgument(args[1])
            )).queue();

        return true;
    }

    private String formatMemeArgument(String string) {
        return string.trim()
            .replaceAll("_", "__")
            .replaceAll("-", "--")
            .replaceAll(" ", "_")
            .replaceAll("\\?", "~q")
            .replaceAll("%", "~p")
            .replaceAll("#", "~h")
            .replaceAll("/", "~s")
            .replaceAll("''", "\"");
    }

    private void loadMemesIntoMemory() {
        Map<String, Map<String, String>> cachedMemes = (Map<String, Map<String, String>>) orion.getCache().getAdapter(CacheType.FILE).get("meme.types");
        List<String> keys = new ArrayList<>(cachedMemes.keySet());
        Collections.sort(keys);

        memeKeys.clear();
        memeKeys.addAll(keys);

        memes.clear();
        memes.putAll(cachedMemes);
    }
}
