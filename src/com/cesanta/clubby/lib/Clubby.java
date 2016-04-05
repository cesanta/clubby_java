
package com.cesanta.clubby.lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;

public final class Clubby {
    private final String serverAddress;
    private final String deviceId;
    private final String devicePsk;
    private final String backend;

    private final ObjectMapper mapper;

    private ClubbyState state = ClubbyState.NOT_CONNECTED;

    private final ListenerManager listenerMan = new ListenerManager(this);
    private final CmdListenerManager cmdListenerMan = new CmdListenerManager();

    private WebSocket ws;

    /* TODO(dfrank): probably initial value should be random? */
    private int cmdId = 0;

    private Clubby(Builder builder) throws IOException {
        serverAddress = builder.serverAddress;
        deviceId = builder.deviceId;
        devicePsk = builder.devicePsk;
        backend = builder.backend;

        mapper = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        WebSocketFactory factory = new WebSocketFactory();

        ws = factory.createSocket(serverAddress);
        ws.addProtocol("clubby.cesanta.com");
        ws.addExtension("clubby.cesanta.com-encoding; in=json; out=json");
        ws.addListener(wsListener);
    }

    /**
     * A JSON frame
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class JsonFrame {
        public int v = 1;
        public String src = "";
        public String dst = "";

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String key = null;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public List<JsonCmd> cmds = null;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public List<JsonResp> resp = null;

        /*
         * Note: default constructor is needed for JSON deserialization
         */
        JsonFrame() {
        }

        JsonFrame(Clubby clubby) {
            this.src = clubby.deviceId;
            this.dst = clubby.backend;
            this.key = clubby.devicePsk;
        }

        static JsonFrame createFrameCmd(
                Clubby clubby, JsonCmd jsonCmd
                ) {
            JsonFrame frame = new JsonFrame(clubby);
            frame.cmds = new ArrayList<JsonCmd>();
            frame.cmds.add(jsonCmd);
            return frame;
        }

        /*
         * Note: not used yet, but might be used in the future, when Clubby
         * lib will support incoming commands
         */
        static JsonFrame createFrameResp(
                Clubby clubby, JsonResp jsonResp
                ) {
            JsonFrame frame = new JsonFrame(clubby);
            frame.resp = new ArrayList<JsonResp>();
            frame.resp.add(jsonResp);
            return frame;
        }
    }

    /**
     * Single JSON command.
     */
    static class JsonCmd {
        public String cmd = "";
        public int id = 0;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Object args = null;

        JsonCmd(String cmd, int id, Object args) {
            this.cmd = cmd;
            this.id = id;
            this.args = args;
        }

        JsonCmd(String cmd, int id) {
            this(cmd, id, null);
        }
    }

    /*
     * TODO(dfrank) : probably add @JsonIgnoreProperties(ignoreUnknown = true)
     * here, just in order to keep the code working if future Clubby backend
     * will provide more fields
     */
    /**
     * Single JSON response.
     */
    static class JsonResp {
        public int id = 0;
        public int status = 0;
        public String status_msg = "";

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Object resp = null;
    }


    /**
     * Add generic Clubby listener. Null is silently ignored.
     */
    public void addListener(ClubbyListener listener) {
        listenerMan.addListener(listener);
    }

    /**
     * Remove generic Clubby listener. Null is silently ignored.
     */
    public void removeListener(ClubbyListener listener) {
        listenerMan.removeListener(listener);
    }

    /**
     * Connect to the server asynchronously. If the connection fails,
     * `ListenerManager.onConnectError()` is called.
     */
    public void connect() {
        ws.connectAsynchronously();
    }

    public boolean isConnected() {
        return ws.isOpen();
    }

    public void disconnect() {
        ws.disconnect();
    }

    /**
     * Clubby object builder.
     *
     * Usage example:
     *
     *     Clubby myClubby = new Clubby.Builder()
     *             .serverAddress("http://some.address.com")
     *             .device("//api.cesanta.com/d/my_device_id", "my_device_psk")
     *             .build();
     */
    public static class Builder {
        private String serverAddress = "http://api.cesanta.com:80";
        //private String serverAddress = "https://api.cesanta.com:443";
        private String deviceId = "";
        private String devicePsk = "";
        private String backend = "//api.cesanta.com";

        public Builder() {
        }

        /**
         * Set server address. Default is: "http://api.cesanta.com:80"
         */
        public Builder serverAddress(String val) {
            this.serverAddress = val;
            return this;
        }

        /**
         * Set device id and psk. Defaults are empty strings.
         */
        public Builder device(String id, String psk) {
            this.deviceId = id;
            this.devicePsk = psk;
            return this;
        }

        /**
         * Set backend value. Default is: "//api.cesanta.com"
         */
        public Builder backend(String val) {
            this.backend = val;
            return this;
        }

        /**
         * Build an instance of Clubby client from the current builder
         * instance.
         *
         * @throws IOException - Failed to create a socket. Or, HTTP proxy
         *         handshake or SSL handshake failed.
         */
        public Clubby build() throws IOException {
            return new Clubby(this);
        }
    }

    void sendText(String text) {
        if (isConnected()) {
            /* TODO(dfrank): remove this debug print */
            //System.out.println("sending text data: " + text);
            ws.sendText(text);
        } else {
            throw new IllegalStateException("Clubby is not connected");
        }
    }

    WebSocketAdapter wsListener = new WebSocketAdapter() {

        @Override
        public void onTextMessage(WebSocket websocket, final String text) throws Exception {
            /* TODO(dfrank): remove this debug print */
            //System.out.println("New message from server: " + text);

            listenerMan.onDataReceived(text);

            JsonFrame jsonFrameResp = mapper.readValue(text, JsonFrame.class);
            for (JsonResp resp : jsonFrameResp.resp) {

                CmdListenerWrapper listenerWrapper = cmdListenerMan.popListener(resp.id);

                if (listenerWrapper != null) {
                    if (resp.status == 0) {
                        /* Status is OK, so, handle the response */
                        listenerWrapper.onResponseGeneric(
                                mapper,
                                mapper.writeValueAsString(resp.resp)
                                );
                    } else {
                        /* Command has failed */
                        listenerWrapper.listener.onError(
                                resp.status,
                                resp.status_msg
                                );
                    }
                }
            }
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            listenerMan.onConnected();
        }

        @Override
        public void onDisconnected(
                WebSocket websocket,
                WebSocketFrame serverCloseFrame,
                WebSocketFrame clientCloseFrame,
                boolean closedByServer
                ) throws Exception {
            listenerMan.onDisconnected();
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            listenerMan.onError(cause);
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException cause) throws Exception {
            listenerMan.onConnectError(cause);
        }

        @Override
        public void onStateChanged(WebSocket websocket, WebSocketState newState) {
            switch (newState) {
                case CREATED:
                    Clubby.this.state = ClubbyState.NOT_CONNECTED;
                    break;
                case CLOSED:
                    Clubby.this.state = ClubbyState.NOT_CONNECTED;
                    break;
                case CONNECTING:
                    Clubby.this.state = ClubbyState.CONNECTING;
                    break;
                case OPEN:
                    Clubby.this.state = ClubbyState.CONNECTED;
                    break;
                case CLOSING:
                    Clubby.this.state = ClubbyState.DISCONNECTING;
                    break;
            }

            listenerMan.onStateChanged(Clubby.this.state);
        }

        @Override
        public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            listenerMan.onDataSending(frame.getPayloadText());
        }

        @Override
        public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {
            listenerMan.handleCallbackError(cause);
        }

    };

    private int getNextCmdId() {
        return ++cmdId;
    }

    public void call(
            String cmd,
            Object args,
            CmdListenerWrapper listenerWrapper
            ) {
        // get next command id
        int cmdId = getNextCmdId();

        // if listener is specified, take care of it
        if (listenerWrapper != null) {
            listenerWrapper.setCmdId(cmdId);
            cmdListenerMan.addCmdListener(listenerWrapper);
        }

        //-- prepare JSON frame
        JsonFrame jsonFrame = JsonFrame.createFrameCmd(
                this,
                new JsonCmd(cmd, cmdId, args)
                );

        //-- convert it to text
        String jsonStr = null;
        try {
            jsonStr = mapper.writeValueAsString(jsonFrame);
        } catch (JsonProcessingException e){
            e.printStackTrace();
            System.exit(1);
        }

        //-- send it
        sendText(jsonStr);
    }

    public ClubbyState getState() {
        return state;
    }

}

