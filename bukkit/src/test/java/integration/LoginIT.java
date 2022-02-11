package integration;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import com.google.common.io.CharStreams;
import com.mojang.authlib.EnvironmentParser;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

// Warning name is sensitive to the surefire plugin
public class LoginIT {

    private static final String API_TAG = "mockserver-5.11.2";
    private static final String API_IMAGE_NAME = "mockserver/mockserver";
    private static final String API_IMAGE = API_IMAGE_NAME + ':' + API_TAG;

    private static final String HOME_FOLDER = "/home/nonroot/";

    @Rule
    public MockServerContainer mockServer = new MockServerContainer(DockerImageName.parse(API_IMAGE))
        .withReuse(true);

    private static final String SERVER_TAG = "1.18.1@sha256:dd3c8d212de585ec73113a0c0c73ac755ec1ff53e65bb09089061584fac02053";
    private static final String SERVER_IMAGE_NAME = "ghcr.io/games647/paperclip";
    private static final String SERVER_IMAGE = SERVER_IMAGE_NAME + ':' + SERVER_TAG;

    @Rule
    public GenericContainer<?> minecraftServer = new GenericContainer(DockerImageName.parse(SERVER_IMAGE))
        .withEnv("JDK_JAVA_OPTIONS", buildJVMFlags())
        .withExposedPorts(25565)
        // use server settings that use minimal minecraft log to quickly ramp up the server
        .withCopyFileToContainer(MountableFile.forClasspathResource("server.properties"), HOME_FOLDER + "server.properties")
        .withCopyFileToContainer(MountableFile.forClasspathResource("bukkit.yml"), HOME_FOLDER + "bukkit.yml")
        .withCopyFileToContainer(MountableFile.forClasspathResource("spigot.yml"), HOME_FOLDER + "spigot.yml")
        // create folders that are volatile
        .withTmpFs(getTempFS())
        // Done (XXXXs)! For help, type "help"
        .waitingFor(
            Wait.forLogMessage(".*For help, type \"help\"*\\n", 1)
        )
        .withReuse(true);

    private Map<String, String> getTempFS() {
        Map<String, String> tmpfs = new HashMap<>();
        tmpfs.put(HOME_FOLDER + "world", "rw,noexec,nosuid,nodev");
        tmpfs.put(HOME_FOLDER + "logs", "rw,noexec,nosuid,nodev");
        return tmpfs;
    }

    private String buildJVMFlags() {
        Map<String, String> systemProperties = new HashMap<>();
        systemProperties.put("com.mojang.eula.agree", Boolean.toString(true));

        // set the Yggdrasil hosts that will also be used by the vanilla server
        systemProperties.put(EnvironmentParser.PROP_ACCOUNT_HOST, getProxyHost());
        systemProperties.put(EnvironmentParser.PROP_SESSION_HOST, getProxyHost());

        return systemProperties.entrySet().stream()
            .map(entry -> "-D" + entry.getKey() + '=' + entry.getValue())
            .collect(Collectors.joining(" ")) + " -client";
    }

    @Before
    public void setUp() throws Exception {
        System.out.println(minecraftServer.getLogs());
    }

    @Test
    public void checkRunning() throws Exception {
        assertThat(minecraftServer.isRunning(), is(true));

        String host = minecraftServer.getHost();
        int port = minecraftServer.getMappedPort(25565);
        Session clientSession = new TcpClientSession(host, port, new MinecraftProtocol());
        try {
            CompletableFuture<Boolean> connectionResult = new CompletableFuture<>();
            clientSession.addListener(new SessionAdapter() {
                @Override
                public void packetReceived(Session session, Packet packet) {
                    System.out.println("Received: " + packet.getClass());
                    connectionResult.complete(true);
                }

                @Override
                public void disconnected(DisconnectedEvent event) {
                    connectionResult.complete(false);
                }
            });

            clientSession.connect();
            assertThat(connectionResult.get(2, TimeUnit.SECONDS), is(true));
        } finally {
            clientSession.disconnect("Status test complete.");
        }
    }

    private String getProxyHost() {
        return String.format("https://%s:%d", mockServer.getHost(), mockServer.getServerPort());
    }

    @Test
    @Ignore
    public void autoRegisterNewUser() throws Exception {
        assertThat(mockServer.isRunning(), is(true));

        try (MockServerClient client = new MockServerClient(mockServer.getHost(), mockServer.getServerPort())) {
            HttpRequest profileReq = request("/users/profiles/minecraft/" + "username");
            HttpRequest hasJoinedReq = request()
                .withPath("/session/minecraft/hasJoined")
                .withQueryStringParameter("username", "")
                .withQueryStringParameter("serverId", "")
                .withQueryStringParameter("ip", "");

            // check call network request times
            client.verify(profileReq, VerificationTimes.once());
            client.verify(hasJoinedReq, VerificationTimes.once());

            // Verify order
            client.verify(profileReq, hasJoinedReq);

            client
                .when(request()
                    .withPath("/users/profiles/minecraft/" + "username"))
                .respond(response()
                    .withBody("bla"));

            client
                .when(hasJoinedReq)
                .respond(response()
                    .withBody("Test"));

            URLConnection urlConnection = new URL(mockServer.getEndpoint() + "/users/profiles/minecraft/username").openConnection();
            String out = CharStreams.toString(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8));
            System.out.println("OUTPUT: " + out);
        }
    }

    @Test
    @Ignore
    public void failedJoinedVerification() {
        // has joined fails
    }

    @Test
    @Ignore
    public void offlineLoginNewUserDisabledRegister() {
        // auto register disabled, always offline login for new users
    }

    @Test
    @Ignore
    public void offlineLoginNewUser() {
        // auto register enabled, but no paid account
    }

    @Test
    @Ignore
    public void autoLoginRegistered() {
        // registered premium user and paid account login in
    }

    @Test
    @Ignore
    public void failedLoginPremiumRegistered() {
        // registered premium, but tried offline login
    }

    @Test
    @Ignore
    public void offlineLoginRegistered() {
        // assume registered user marked as offline - tried to login
    }

    @Test
    @Ignore
    public void alreadyOnlineDuplicateOwner() {

    }

    @Test
    @Ignore
    public void alreadyOnlineDuplicateCracked() {

    }
}
