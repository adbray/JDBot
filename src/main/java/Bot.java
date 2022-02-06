import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Random;



public class Bot extends ListenerAdapter {


    private final String[] conchResponse = {
            "Maybe someday.",
            "Nothing.",
            "Neither",
            "I don't think so.",
            "No.",
            "Yes.",
            "Try asking again."
    };
    private final Random rand = new Random();

    public static void main(String[] args) throws LoginException {
        final String token = args[0];
        //Bot will crash when a chat message occurs if token is invalid
        JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(new Bot())
                .setActivity(Activity.playing("Type !ping"))
                .build();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        MessageChannel channel = event.getChannel();
        String[] input = msg.getContentRaw().trim().split(" ");

        //User message count is updated every message
        updateUser(msg.getAuthor().getIdLong(), msg.getAuthor().getAsTag());


        switch (input[0]) {
            case "_ping" -> {
                long time = System.currentTimeMillis();
                channel.sendMessage("Pong!") /* => RestAction<Message> */
                        .queue(response /* => Message */ -> response.editMessageFormat("Pong: %d ms", System.currentTimeMillis() - time).queue());
            }
            case "_leaderboard" -> {
                EmbedBuilder leaderboard = new EmbedBuilder();
                leaderboard.setTitle("User: " + msg.getAuthor().getAsTag());
                leaderboard.addField("Beef", "Broccoli", false);
                channel.sendMessageEmbeds(leaderboard.build()).queue();
            }
            case "_rest" -> {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/user?id=" + msg.getAuthor().getIdLong()))
                        .timeout(Duration.ofSeconds(5))
                        .GET().build();
                HttpClient client = HttpClient.newBuilder().build();
                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println(response.body());
                    sendEmbed(channel, response.body());
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            case "_conch" -> channel.sendMessage(conchResponse[rand.nextInt(7)]).queue();
        }
    }

    public void updateUser(Long id, String name) {
        //Sends post request to rest api, if user does not exist they will be created and entered into the database
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/user?id=" + id + "&name=" +name))
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpClient client = HttpClient.newBuilder().build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendEmbed(MessageChannel channel, String info) {
        info = info.substring(2,info.length()-2);
        info = info.replaceAll("\"", "");
        String[] fields = info.split(",");
        EmbedBuilder msg = new EmbedBuilder();
        msg.setTitle(fields[0].substring(fields[0].indexOf(":") + 1));
        msg.setDescription("Post Count = " + fields[2].substring(fields[2].indexOf(":") + 1));
        channel.sendMessageEmbeds(msg.build()).queue();
    }
}
