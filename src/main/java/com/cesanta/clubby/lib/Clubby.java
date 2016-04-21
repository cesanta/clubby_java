
package com.cesanta.clubby.lib;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private final ScheduledExecutorService executor
      = Executors.newScheduledThreadPool(1);;

    private ClubbyState state = ClubbyState.NOT_CONNECTED;

    private final ListenerManager listenerMan = new ListenerManager(this);
    private final CmdListenerManager cmdListenerMan = new CmdListenerManager();

    private ClubbyOptions defaultOpts = null;

    private WebSocket ws;

    /* TODO(dfrank): probably initial value should be random? */
    private int cmdId = 0;

    private Clubby(Builder builder) throws IOException {
        deviceId = builder.deviceId;
        devicePsk = builder.devicePsk;
        defaultOpts = builder.opts;

        // Init backend address
        if (builder.backend != null) {
            // Just set the value from builder
            backend = builder.backend;
        } else {
            if (builder.serverAddress != null) {
                // Infer backend address from the server address: remove the
                // leading protocol part, and the trailing port. E.g.:
                // "https://api.cesanta.com:80" -> "//api.cesanta.com"

                URL url = new URL(builder.serverAddress);
                backend = "//" + url.getHost();

            } else {
                // Set the default backend
                backend = "//api.cesanta.com";
            }
        }

        // Init server address
        if (builder.serverAddress != null) {
            // Just set the value from builder
            serverAddress = builder.serverAddress;
        } else {
            // Infer server address from the backend (which is guaranteed to
            // be set already)
            serverAddress = "wss:" + backend + ":443";
        }

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

        JsonFrame(Clubby clubby, String dst) {
            this.src = clubby.deviceId;
            this.dst = dst;
            this.key = clubby.devicePsk;
        }

        static JsonFrame createFrameCmd(
                Clubby clubby, String dst, JsonCmd jsonCmd
                ) {
            JsonFrame frame = new JsonFrame(clubby, dst);
            frame.cmds = new ArrayList<JsonCmd>();
            frame.cmds.add(jsonCmd);
            return frame;
        }

        /*
         * Note: not used yet, but might be used in the future, when Clubby
         * lib will support incoming commands
         */
        static JsonFrame createFrameResp(
                Clubby clubby, String dst, JsonResp jsonResp
                ) {
            JsonFrame frame = new JsonFrame(clubby, dst);
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

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public Object args = null;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Integer timeout = null;

        JsonCmd(String cmd, int id, Object args, int timeout) {
            this.cmd = cmd;
            this.id = id;
            this.args = args;
            if (timeout != 0) {
                this.timeout = timeout;
            }
        }

        JsonCmd(String cmd, int id, int timeout) {
            this(cmd, id, null, timeout);
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
     * Connect to the server asynchronously; when the connection is
     * established, {@link ClubbyListener#onConnected(Clubby)
     * ClubbyListener.onConnected()} is called. If the connection fails,
     * {@link ClubbyListener#onConnectError(Clubby, WebSocketException)
     * ClubbyListener.onConnectError()} is called.
     */
    public void connect() {
        ws.connectAsynchronously();
    }

    /**
     * Returns whether the underlying websocket is connected to the server.
     */
    public boolean isConnected() {
        return ws.isOpen();
    }

    /**
     * Disconnect from the server asynchronously; when the socket is
     * disconnected, {@link ClubbyListener#onDisconnected(Clubby)
     * ClubbyListener.onDisconnected()} is called.
     */
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
     *             .timeout(5)
     *             .build();
     */
    public static class Builder {
        private String serverAddress = null;
        private String backend = null;
        private String deviceId = "";
        private String devicePsk = "";
        private ClubbyOptions opts = ClubbyOptions.createDefault();

        public Builder() {
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
         * Set backend address; default is inferred from the server address,
         * or, in case of unspecified server address, `//api.cesanta.com`.
         */
        public Builder backend(String val) {
            this.backend = val;
            return this;
        }

        /**
         * Set server address, default is inferred from the backend.
         *
         * Default example:
         *
         *      Backend address: "//api.cesanta.com"
         *      Server address: "wss://api.cesanta.com:443"
         */
        public Builder serverAddress(String val) {
            this.serverAddress = val;
            return this;
        }

        /**
         * Set default number of seconds after when the command result is no
         * longer relevant (and the {@link CmdListener#onError(int, String)
         * onError()} method of the listener will be called). Set 0 for no
         * timeout. Default: 0.
         */
        public Builder timeout(int timeout) {
            opts.timeout(timeout);
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

                CmdListenerWrapper<?> listenerWrapper = cmdListenerMan.popListener(resp.id);

                if (listenerWrapper != null) {
                    if (resp.status == 0) {
                        /* Status is OK, so, handle the response */
                        listenerWrapper.onResponseGeneric(
                                mapper,
                                mapper.writeValueAsString(resp.resp)
                                );
                    } else {
                        /* Command has failed */
                        listenerWrapper.onError(
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

    public void setDefaultOptions(ClubbyOptions opts) {
        this.defaultOpts = ClubbyOptions.createFrom(opts);
    }

    public ClubbyOptions getOptions() {
        return ClubbyOptions.createFrom(defaultOpts);
    }

    /**
     * Generic clubby call
     *
     * @param dst
     *      Destination address. Example: `//api.cesanta.com/d/my_device`.
     * @param cmd
     *      Command to send. Might be any arbitrary string that the destination
     *      device has a registered handler for.
     * @param args
     *      Object with command arguments; will be serialized into JSON by
     *      means of Jackson library. Usually it does "the right thing" by
     *      default, but in order to fine-tune the serialization process, you
     *      can read its documentation: http://wiki.fasterxml.com/JacksonHome
     * @param listener
     *      Listener that will be notified once response is received, see
     *      {@link CmdListener CmdListener} and {@link CmdAdapter CmdAdapter}.
     * @param respClass
     *      Type of the response (should correspond to the type parameter of
     *      listener).
     * @param opts
     *      Options instance which will override current default options.  If
     *      there is a need to override defaults, use {@link
     *      Clubby#getOptions() getOptions()} to get current defaults, and then
     *      modify received options object in some way.
     */
    public <R> void call(
            String dst,
            String cmd,
            Object args,
            CmdListener<R> listener,
            Class<R> respClass,
            ClubbyOptions opts
            ) {
        // get next command id
        int cmdId = getNextCmdId();

        // if listener is specified, take care of it
        if (listener != null) {
            final CmdListenerWrapper<R> listenerWrapper =
                new CmdListenerWrapper<R>(listener, respClass);

            listenerWrapper.setCmdId(cmdId);

            if (opts == null) {
                opts = defaultOpts;
            }

            if (opts.getTimeout() != 0) {
                Future<?> timeoutFuture = executor.schedule(
                        new TimeoutHandler(cmdId),
                        opts.getTimeout(),
                        TimeUnit.SECONDS
                        );

                listenerWrapper.setTimeoutFuture(timeoutFuture);
            }

            cmdListenerMan.addCmdListener(listenerWrapper);
        }

        //-- prepare JSON frame
        JsonFrame jsonFrame = JsonFrame.createFrameCmd(
                this,
                dst,
                new JsonCmd(cmd, cmdId, args, opts.getTimeout())
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

    public <R> void call(
            String dst,
            String cmd,
            Object args,
            CmdListener<R> listener,
            Class<R> respClass
            ) {
        call(dst, cmd, args, listener, respClass, null);
    }

    /**
     * The same as {@link Clubby#call(String, String, Object, CmdListener,
     * Class) call()} with destination address set to the backend address, see
     * {@link Clubby.Builder#backend(String) Builder.backend()}
     */
    public <R> void callBackend(
            String cmd,
            Object args,
            CmdListener<R> listener,
            Class<R> respClass,
            ClubbyOptions opts
            ) {
        call(backend, cmd, args, listener, respClass, opts);
    }

    public <R> void callBackend(
            String cmd,
            Object args,
            CmdListener<R> listener,
            Class<R> respClass
            ) {
        callBackend(cmd, args, listener, respClass, null);
    }

    public ClubbyState getState() {
        return state;
    }

    private class TimeoutHandler implements Runnable {
        int cmdId;

        public TimeoutHandler(int cmdId) {
            this.cmdId = cmdId;
        }

        @Override
        public void run() {
            CmdListenerWrapper<?> listenerWrapper = cmdListenerMan.popListener(cmdId);
            if (listenerWrapper != null) {
                listenerWrapper.onError(
                        504,
                        "Response timeout"
                        );
            }
        }
    }

}

