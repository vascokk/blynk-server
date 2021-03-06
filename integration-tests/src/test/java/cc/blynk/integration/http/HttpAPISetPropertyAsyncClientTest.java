package cc.blynk.integration.http;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.api.http.HttpAPIServer;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.protocol.model.messages.appllication.SetWidgetPropertyMessage;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.JsonParser;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpAPISetPropertyAsyncClientTest extends IntegrationBase {

    private static BaseServer httpServer;
    private static AsyncHttpClient httpclient;
    private static String httpsServerUrl;

    private static BaseServer hardwareServer;
    private static BaseServer appServer;
    private static ClientPair clientPair;

    @AfterClass
    public static void shutdown() throws Exception {
        httpclient.close();
        httpServer.close();
        appServer.close();
        hardwareServer.close();
        clientPair.stop();
    }

    @Before
    public void init() throws Exception {
        if (httpServer == null) {
            httpServer = new HttpAPIServer(holder).start(transportTypeHolder);
            httpsServerUrl = String.format("http://localhost:%s/", httpPort);
            httpclient = new DefaultAsyncHttpClient(
                    new DefaultAsyncHttpClientConfig.Builder()
                            .setUserAgent("")
                            .setKeepAlive(false)
                            .build()
            );
            hardwareServer = new HardwareServer(holder).start(transportTypeHolder);
            appServer = new AppServer(holder).start(transportTypeHolder);
            if (clientPair == null) {
                clientPair = initAppAndHardPair(tcpAppPort, tcpHardPort, properties);
            }
        }
        clientPair.appClient.reset();
    }

    //----------------------------GET METHODS SECTION

    @Test
    public void testChangeProperty() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();


        Future<Response> f = httpclient.preparePut(httpsServerUrl + token + "/pin/v4/label")
                .setHeader("Content-Type", "application/json")
                .setBody("[\"My-New-Label\"]")
                .execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SetWidgetPropertyMessage(111, b("1 4 label My-New-Label"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = JsonParser.parseProfile(clientPair.appClient.getBody());
        Widget widget = profile.dashBoards[0].findWidgetByPin((byte) 4, PinType.VIRTUAL);
        assertNotNull(widget);
        assertEquals("My-New-Label", widget.label);
    }

}
