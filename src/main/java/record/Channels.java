package record;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import recorders.MessageProcessers;
import util.MutableInteger;
import util.SharedConstants;

/*This is a class designed to store every message
 * sent in a discord server to disk. This is to increase
 * response times since discord has such a large delay to
 * sending/receiving messages*/
public class Channels {

	private final JDA jda;
	private final long guildId;
	public NavigableMap<Long, Messages> channels;
	public NavigableMap<Long, MutableInteger> messageCounter;

	public Channels(JDA jda, long guildId) throws IOException, InterruptedException {
		this.jda = jda;
		this.guildId = guildId;
		channels = new TreeMap();
		messageCounter = new TreeMap();
		Guild guild = jda.getGuildById(guildId);
		List<TextChannel> tempChannels = guild.getTextChannels();
		new File(SharedConstants.MESSAGES_FOLDER).mkdirs();
		for (TextChannel channel : tempChannels)
		{
			long id = channel.getIdLong();
			messageCounter.put(id, new MutableInteger(0));
			channels.put(id, new Messages(id, jda));
		}
	}

	public void searchChannels(MessageProcessers processers) {
		for (Messages messages : channels.values())
		{ messages.searchMessages(processers); }
	}

	public void syncChannel(Message message) {
		long channelId = message.getChannel().getIdLong();
		MutableInteger mutableInteger = messageCounter.get(channelId);
		mutableInteger.increment();
		if (mutableInteger.intValue() > 100)
		{
			mutableInteger.add(-100);
			LoggerFactory.getLogger("sync").debug("Syncing " + channelId);
			channels.get(channelId).fetchMessages(message.getIdLong());
		}
	}

	public void fetchAll(long channel) {
		Logger logger = LoggerFactory.getLogger("Fetch");
		for (Messages messages : channels.values())
		{
			long start = System.currentTimeMillis();
			messages.fetchMessages();
			long end = System.currentTimeMillis();
			String debug = "Fetched <#" + messages.getId() + "> in " + (end - start) + " ms";
			logger.info(debug);
			jda.getTextChannelById(channel).sendMessage(debug).queue();
		}
	}
}