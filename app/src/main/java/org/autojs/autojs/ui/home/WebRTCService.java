package org.autojs.autojs.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpSender;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.VideoSink;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WebRTC服务类
 * 
 * 使用WebRTC实现屏幕共享功能
 */
public class WebRTCService {
    private static final String TAG = "WebRTCService";
    
    // 连接状态常量
    public static final int STATE_NEW = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_DISCONNECTED = 3;
    public static final int STATE_FAILED = 4;
    
    // 默认信令服务器地址
    private static final String DEFAULT_SIGNALING_SERVER = "ws://192.168.0.104:7070/websocket/webrtc";
    private static final String DEFAULT_ROOM = "auto-js-room";
    private static final String DEFAULT_CLIENT_ID = "auto-js-client-" + UUID.randomUUID().toString().substring(0, 8);
    
    // 视频常量
    private static final String VIDEO_TRACK_ID = "ARDAMSv0";
    private static final String AUDIO_TRACK_ID = "ARDAMSa0";
    private static final String LOCAL_STREAM_ID = "ARDAMS";
    
    // 连接变量
    private String mServerUrl = DEFAULT_SIGNALING_SERVER;
    private String mRoomId = DEFAULT_ROOM;
    private String mClientId = DEFAULT_CLIENT_ID;
    private int mState = STATE_NEW;
    
    // WebSocket客户端
    private org.java_websocket.client.WebSocketClient mWebSocketClient;
    
    // 媒体相关
    private MediaProjection mediaProjection;
    private MediaProjectionManager projectionManager;
    private Intent mediaProjectionPermissionResultData;
    public static final int CAPTURE_PERMISSION_REQUEST_CODE = 1001;
    
    // WebRTC相关
    private PeerConnectionFactory peerConnectionFactory;
    private List<PeerConnection.IceServer> iceServers;
    private EglBase eglBase;
    private MediaConstraints audioConstraints;
    private MediaConstraints videoConstraints;
    private MediaConstraints sdpConstraints;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private VideoCapturer videoCapturer;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private MediaStream localMediaStream;
    private SurfaceTextureHelper surfaceTextureHelper;
    
    // 屏幕捕获相关变量
    private int captureWidth = 1280;
    private int captureHeight = 720;
    private int captureFrameRate = 20;
    private ScreenCapturerAndroid screenCapturer;
    
    // 维护所有对等连接
    private List<Peer> peers = new ArrayList<>();
    
    // 线程池
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    // 事件监听器和上下文
    private WebRTCListener mListener;
    private Context mContext;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 心跳定时器
    private java.util.Timer heartbeatTimer;
    
    // 添加帧率监控变量
    private long frameCount = 0;
    private long lastFpsLogTime = 0;
    private final Object fpsLock = new Object();
    
    /**
     * 表示一个对等连接
     */
    private class Peer {
        private String id;
        private PeerConnection peerConnection;
        private boolean isInitiator = false;
        private boolean isNegotiating = false;
        
        public Peer(String id) {
            this.id = id;
            createPeerConnection();
        }
        
        private void createPeerConnection() {
            PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
            // 使用TCP候选优先（更适合局域网环境）
            rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED;
            rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
            rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
            rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
            rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
            rtcConfig.enableDtlsSrtp = true;
            // 修改SDP语义：将Unified Plan改为Plan B，以便支持addStream方法
            rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.PLAN_B; // 使用Plan B代替Unified Plan
            
            PeerConnection.Observer pcObserver = new PeerConnection.Observer() {
                @Override
                public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                    Log.d(TAG, "onSignalingChange: " + signalingState);
                }

                @Override
                public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                    Log.d(TAG, "onIceConnectionChange: " + iceConnectionState);
                    if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                        // 连接已建立
                        mainHandler.post(() -> {
                            if (mListener != null) {
                                mListener.onPeerConnected(id);
                            }
                        });
                        // 重新检查媒体流是否正确添加
                        ensureMediaStreamAdded();
                    } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED || 
                              iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
                        // 连接已断开
                        mainHandler.post(() -> {
                            if (mListener != null) {
                                mListener.onPeerDisconnected(id);
                            }
                        });
                        removePeer(id);
                    }
                }

                @Override
                public void onIceConnectionReceivingChange(boolean receiving) {
                    Log.d(TAG, "onIceConnectionReceivingChange: " + receiving);
                }

                @Override
                public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                    Log.d(TAG, "onIceGatheringChange: " + iceGatheringState);
                }

                @Override
                public void onIceCandidate(IceCandidate iceCandidate) {
                    try {
                        if (iceCandidate == null) {
                            Log.d(TAG, "收到空ICE候选");
                            return;
                        }
                        
                        Log.d(TAG, "生成ICE候选: " + iceCandidate.sdp);
                        
                        // 创建标准格式的ICE候选对象
                        JSONObject candidateJson = new JSONObject();
                        candidateJson.put("sdpMid", iceCandidate.sdpMid);
                        candidateJson.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                        candidateJson.put("candidate", iceCandidate.sdp);
                        
                        // 创建信令消息
                        JSONObject message = new JSONObject();
                        message.put("type", "candidate");
                        message.put("from", mClientId);
                        message.put("to", id);
                        message.put("candidate", candidateJson);
                        
                        // 发送到信令服务器
                        sendMessage(message);
                    } catch (Exception e) {
                        Log.e(TAG, "发送ICE候选时出错: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                    Log.d(TAG, "onIceCandidatesRemoved");
                }

                @Override
                public void onAddStream(MediaStream mediaStream) {
                    Log.d(TAG, "onAddStream: " + mediaStream);
                    // 我们不期望收到远程流，因为我们只分享屏幕
                }

                @Override
                public void onRemoveStream(MediaStream mediaStream) {
                    Log.d(TAG, "onRemoveStream");
                }

                @Override
                public void onDataChannel(org.webrtc.DataChannel dataChannel) {
                    Log.d(TAG, "onDataChannel");
                }

                @Override
                public void onRenegotiationNeeded() {
                    Log.d(TAG, "onRenegotiationNeeded");
                    // 需要重新协商
                    createOffer();
                }

                @Override
                public void onAddTrack(org.webrtc.RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                    Log.d(TAG, "onAddTrack");
                }
            };
            
            this.peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, pcObserver);
            
            // 添加本地媒体流
            ensureMediaStreamAdded();
        }
        
        // 确保媒体流被正确添加到连接中
        private void ensureMediaStreamAdded() {
            if (peerConnection != null && localMediaStream != null) {
                try {
                    // 首先检查是否已经添加
                    boolean alreadyAdded = false;
                    for (RtpSender sender : peerConnection.getSenders()) {
                        if (sender.track() != null) {
                            alreadyAdded = true;
                            break;
                        }
                    }
                    
                    if (!alreadyAdded) {
                        Log.d(TAG, "向PeerConnection添加本地媒体流，轨道数: " + localMediaStream.videoTracks.size());
                        
                        // 添加视频轨道
                        if (localMediaStream.videoTracks.size() > 0) {
                            VideoTrack videoTrack = localMediaStream.videoTracks.get(0);
                            Log.d(TAG, "添加视频轨道到PeerConnection: " + videoTrack.id());
                            
                            try {
                                // 使用Plan B语义的addStream方法
                                peerConnection.addStream(localMediaStream);
                                Log.d(TAG, "使用addStream方法添加媒体流成功");
                            } catch (Exception e) {
                                Log.e(TAG, "使用addStream方法失败，尝试使用addTrack方法: " + e.getMessage(), e);
                                
                                // 如果addStream失败，尝试使用addTrack方法
                                try {
                                    peerConnection.addTrack(videoTrack, java.util.Arrays.asList(LOCAL_STREAM_ID));
                                    Log.d(TAG, "使用addTrack方法添加视频轨道成功");
                                } catch (Exception e2) {
                                    Log.e(TAG, "添加视频轨道失败: " + e2.getMessage(), e2);
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "媒体轨道已添加到PeerConnection");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "添加媒体流到PeerConnection失败", e);
                }
            } else {
                Log.w(TAG, "无法添加媒体流：PeerConnection或本地媒体流为null");
            }
        }
        
        public void createOffer() {
            isInitiator = true;
            isNegotiating = true;
            
            // 确保媒体流已添加
            ensureMediaStreamAdded();
            
            // 创建适用于屏幕共享的约束
            MediaConstraints offerConstraints = new MediaConstraints();
            offerConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
            offerConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
            
            this.peerConnection.createOffer(new SimpleSdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    Log.d(TAG, "创建Offer成功，SDP: " + sessionDescription.description.substring(0, Math.min(100, sessionDescription.description.length())) + "...");
                    
                    // 优化SDP以确保视频质量和兼容性
                    String sdp = sessionDescription.description;
                    sdp = optimizeSdp(sdp);
                    
                    SessionDescription optimizedSdp = new SessionDescription(sessionDescription.type, sdp);
                    
                    peerConnection.setLocalDescription(new SimpleSdpObserver() {
                        @Override
                        public void onSetSuccess() {
                            Log.d(TAG, "设置本地描述成功");
                            try {
                                // 为了与web客户端兼容，使用简单的SDP格式
                                JSONObject message = new JSONObject();
                                message.put("type", "offer");
                                message.put("from", mClientId);
                                message.put("to", id);
                                message.put("sdp", optimizedSdp.description);
                                sendMessage(message);
                            } catch (JSONException e) {
                                Log.e(TAG, "发送offer失败", e);
                                isNegotiating = false;
                            }
                        }
                        
                        @Override
                        public void onSetFailure(String s) {
                            Log.e(TAG, "设置本地描述失败: " + s);
                            isNegotiating = false;
                        }
                    }, optimizedSdp);
                }
                
                @Override
                public void onCreateFailure(String s) {
                    Log.e(TAG, "创建Offer失败: " + s);
                    isNegotiating = false;
                }
            }, offerConstraints);
        }
        
        // 优化SDP
        private String optimizeSdp(String sdp) {
            try {
                Log.d(TAG, "开始优化SDP，原始长度: " + sdp.length());
                
                String[] lines = sdp.split("\r\n");
                StringBuilder newSdp = new StringBuilder();
                boolean isVideoSection = false;
                boolean hasAddedVideoBandwidth = false;
                
                for (String line : lines) {
                    // 检测视频部分
                    if (line.startsWith("m=video")) {
                        isVideoSection = true;
                    } else if (line.startsWith("m=")) {
                        isVideoSection = false;
                    }
                    
                    // 在视频部分添加优化
                    if (isVideoSection) {
                        // 添加原始行
                        newSdp.append(line).append("\r\n");
                        
                        // 在视频部分的特定位置添加带宽限制
                        if (line.startsWith("a=rtpmap") && !hasAddedVideoBandwidth) {
                            // 设置内容类型为屏幕共享
                            newSdp.append("a=content:main\r\n");
                            
                            // 标记为发送流
                            newSdp.append("a=sendonly\r\n");
                            
                            // 设置带宽限制 - 标准格式
                            newSdp.append("b=AS:300\r\n");
                            newSdp.append("b=TIAS:300000\r\n");
                            
                            // 添加标准的fmtp参数，确保格式正确
                            int payloadType = extractPayloadType(line);
                            if (payloadType > 0) {
                                // 使用提取的payload type添加fmtp参数
                                newSdp.append("a=fmtp:").append(payloadType).append(" x-google-start-bitrate=200;x-google-min-bitrate=100;x-google-max-bitrate=300\r\n");
                                
                                // 添加标准的反馈机制
                                newSdp.append("a=rtcp-fb:").append(payloadType).append(" nack\r\n");
                                newSdp.append("a=rtcp-fb:").append(payloadType).append(" nack pli\r\n");
                                newSdp.append("a=rtcp-fb:").append(payloadType).append(" ccm fir\r\n");
                                newSdp.append("a=rtcp-fb:").append(payloadType).append(" goog-remb\r\n");
                            }
                            
                            hasAddedVideoBandwidth = true;
                        }
                    } else {
                        // 非视频部分直接添加
                        newSdp.append(line).append("\r\n");
                    }
                }
                
                String optimizedSdp = newSdp.toString();
                Log.d(TAG, "SDP优化完成，新长度: " + optimizedSdp.length());
                return optimizedSdp;
            } catch (Exception e) {
                Log.e(TAG, "优化SDP时出错: " + e.getMessage(), e);
                return sdp; // 出错时返回原始SDP
            }
        }
        
        // 从rtpmap行中提取payload type
        private int extractPayloadType(String rtpmapLine) {
            try {
                // 格式: a=rtpmap:96 VP8/90000
                String[] parts = rtpmapLine.split(":");
                if (parts.length > 1) {
                    String[] subParts = parts[1].split(" ");
                    if (subParts.length > 0) {
                        return Integer.parseInt(subParts[0]);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "提取payload type失败: " + e.getMessage());
            }
            return 96; // 默认返回96，这是VP8的常用值
        }
        
        public void handleRemoteOffer(String sdp) {
            // 检查当前是否已经有本地offer
            if (isInitiator || isNegotiating) {
                Log.d(TAG, "检测到offer冲突，对比客户端ID决定优先权");
                
                // 使用客户端ID来决定谁的offer优先
                // 如果本地ID大于远程ID，则本地offer优先，忽略远程offer
                if (mClientId.compareTo(id) > 0) {
                    Log.d(TAG, "本地客户端ID优先，忽略远程offer");
                    return;
                }
                
                // 否则，关闭当前连接并重新创建
                Log.d(TAG, "远程客户端ID优先，重置连接");
                close();
                createPeerConnection();
            }
            
            isNegotiating = true;
            isInitiator = false;
            
            SessionDescription remoteSdp = new SessionDescription(SessionDescription.Type.OFFER, sdp);
            peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                @Override
                public void onSetSuccess() {
                    Log.d(TAG, "设置远程描述(offer)成功");
                    createAnswer();
                }
                
                @Override
                public void onSetFailure(String s) {
                    Log.e(TAG, "设置远程描述(offer)失败: " + s);
                    isNegotiating = false;
                }
            }, remoteSdp);
        }
        
        public void createAnswer() {
            peerConnection.createAnswer(new SimpleSdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    Log.d(TAG, "创建Answer成功");
                    peerConnection.setLocalDescription(new SimpleSdpObserver() {
                        @Override
                        public void onSetSuccess() {
                            Log.d(TAG, "设置本地描述(answer)成功");
                            try {
                                JSONObject message = new JSONObject();
                                message.put("type", "answer");
                                message.put("from", mClientId);
                                message.put("to", id);
                                message.put("sdp", sessionDescription.description);
                                sendMessage(message);
                                isNegotiating = false;
                            } catch (JSONException e) {
                                Log.e(TAG, "发送answer失败", e);
                                isNegotiating = false;
                            }
                        }
                        
                        @Override
                        public void onSetFailure(String s) {
                            Log.e(TAG, "设置本地描述(answer)失败: " + s);
                            isNegotiating = false;
                        }
                    }, sessionDescription);
                }
                
                @Override
                public void onCreateFailure(String s) {
                    Log.e(TAG, "创建Answer失败: " + s);
                    isNegotiating = false;
                }
            }, sdpConstraints);
        }
        
        public void handleRemoteAnswer(String sdp) {
            isNegotiating = true;
            SessionDescription remoteSdp = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
            peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                @Override
                public void onSetSuccess() {
                    Log.d(TAG, "设置远程描述(answer)成功");
                    isNegotiating = false;
                }
                
                @Override
                public void onSetFailure(String s) {
                    Log.e(TAG, "设置远程描述(answer)失败: " + s);
                    isNegotiating = false;
                }
            }, remoteSdp);
        }
        
        public void addRemoteIceCandidate(JSONObject candidateJson) {
            try {
                IceCandidate iceCandidate = deserializeIceCandidate(candidateJson);
                peerConnection.addIceCandidate(iceCandidate);
            } catch (JSONException e) {
                Log.e(TAG, "添加ICE候选失败", e);
            }
        }
        
        public void close() {
            if (peerConnection != null) {
                peerConnection.close();
                peerConnection = null;
            }
        }
    }
    
    /**
     * 简化版SDP观察者
     */
    private class SimpleSdpObserver implements org.webrtc.SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
        }

        @Override
        public void onSetSuccess() {
        }

        @Override
        public void onCreateFailure(String s) {
            Log.e(TAG, "创建SDP失败: " + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.e(TAG, "设置SDP失败: " + s);
        }
    }
    
    /**
     * 默认构造函数
     */
    public WebRTCService() {
        // 默认构造函数
    }
    
    /**
     * 带Context的构造函数
     */
    public WebRTCService(Context context) {
        mContext = context;
        
        try {
            // 创建EglBase，使用共享上下文以避免EGL错误
            eglBase = EglBase.create(null, EglBase.CONFIG_PLAIN);
            if (eglBase == null) {
                Log.e(TAG, "无法创建EglBase");
            } else {
                Log.d(TAG, "EglBase创建成功");
            }
        } catch (Exception e) {
            Log.e(TAG, "创建EglBase失败", e);
        }
        
        // 初始化WebRTC工厂
        initFactory();
        
        // 初始化ICE服务器
        iceServers = new ArrayList<>();
        // 添加Google的STUN服务器
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        
        // 初始化媒体约束
        initMediaConstraints();
        
        // 获取MediaProjectionManager
        projectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        
        // 创建主线程Handler
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 设置监听器
     */
    public void setListener(WebRTCListener listener) {
        this.mListener = listener;
    }
    
    /**
     * 设置信令服务器地址
     */
    public void setServerUrl(String serverUrl) {
        this.mServerUrl = serverUrl;
    }
    
    /**
     * 设置房间ID
     */
    public void setRoomId(String roomId) {
        this.mRoomId = roomId;
    }
    
    /**
     * 设置客户端ID
     */
    public void setClientId(String clientId) {
        this.mClientId = clientId;
    }
    
    /**
     * 获取当前状态
     */
    public int getState() {
        return mState;
    }
    
    /**
     * 初始化PeerConnectionFactory
     */
    private void initFactory() {
        try {
            // 初始化WebRTC
            // 避免重复初始化
            PeerConnectionFactory.InitializationOptions initializationOptions = 
                    PeerConnectionFactory.InitializationOptions.builder(mContext)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions();
            
            PeerConnectionFactory.initialize(initializationOptions);
            
            // 创建编解码器工厂，并进行优化
            DefaultVideoEncoderFactory defaultVideoEncoderFactory;
            DefaultVideoDecoderFactory defaultVideoDecoderFactory;
            
            if (eglBase != null && eglBase.getEglBaseContext() != null) {
                // 创建视频编码器工厂，启用硬件加速和H.264高性能模式
                defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                        eglBase.getEglBaseContext(), 
                        true,  // 启用硬件编码器
                        true); // 启用H.264高性能模式
                
                // 优化编码器设置
                try {
                    // 获取支持的编解码器
                    org.webrtc.VideoCodecInfo[] supportedCodecs = 
                            defaultVideoEncoderFactory.getSupportedCodecs();
                    
                    Log.d(TAG, "支持的视频编解码器:");
                    for (org.webrtc.VideoCodecInfo codec : supportedCodecs) {
                        Log.d(TAG, "  - " + codec.name);
                    }
                    
                    // 优先使用VP8/VP9编解码器
                    java.util.ArrayList<org.webrtc.VideoCodecInfo> preferredCodecs = 
                            new java.util.ArrayList<>();
                    
                    // 首先添加VP8（如果支持）- VP8通常延迟更低
                    for (org.webrtc.VideoCodecInfo codec : supportedCodecs) {
                        if (codec.name.equalsIgnoreCase("VP8")) {
                            preferredCodecs.add(codec);
                            break;
                        }
                    }
                    
                    // 然后添加H264（如果支持）
                    for (org.webrtc.VideoCodecInfo codec : supportedCodecs) {
                        if (codec.name.equalsIgnoreCase("H264")) {
                            preferredCodecs.add(codec);
                            break;
                        }
                    }
                    
                    // 最后添加VP9（如果支持）
                    for (org.webrtc.VideoCodecInfo codec : supportedCodecs) {
                        if (codec.name.equalsIgnoreCase("VP9")) {
                            preferredCodecs.add(codec);
                            break;
                        }
                    }
                    
                    // 添加其他编解码器
                    for (org.webrtc.VideoCodecInfo codec : supportedCodecs) {
                        if (!preferredCodecs.contains(codec)) {
                            preferredCodecs.add(codec);
                        }
                    }
                    
                    Log.d(TAG, "优先使用的编解码器顺序:");
                    for (org.webrtc.VideoCodecInfo codec : preferredCodecs) {
                        Log.d(TAG, "  - " + codec.name);
                    }
                    
                    // 创建自定义编码器工厂
                    defaultVideoEncoderFactory = new CustomVideoEncoderFactory(
                            eglBase.getEglBaseContext(),
                            true,
                            true,
                            preferredCodecs.toArray(new org.webrtc.VideoCodecInfo[0]));
                } catch (Exception e) {
                    Log.e(TAG, "优化视频编码器失败，使用默认设置: " + e.getMessage(), e);
                }
                
                // 创建解码器工厂
                defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
            } else {
                Log.w(TAG, "EglBase为null或EglBaseContext为null，使用默认编解码器工厂");
                defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(null, true, true);
                defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(null);
            }
            
            // 创建PeerConnectionFactory
            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            // 添加低延时选项
            options.disableNetworkMonitor = true; // 禁用网络监控以减少开销
            options.disableEncryption = false;    // 保持加密
            
            // 创建工厂
            peerConnectionFactory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    .setVideoEncoderFactory(defaultVideoEncoderFactory)
                    .setVideoDecoderFactory(defaultVideoDecoderFactory)
                    .createPeerConnectionFactory();
            
            if (peerConnectionFactory == null) {
                throw new RuntimeException("无法创建PeerConnectionFactory");
            }
            
            // 尝试设置工厂的全局参数
            try {
                Method setVideoHwAccelerationOptions = peerConnectionFactory.getClass().getDeclaredMethod(
                        "setVideoHwAccelerationOptions", EglBase.Context.class, EglBase.Context.class);
                if (setVideoHwAccelerationOptions != null && eglBase != null) {
                    setVideoHwAccelerationOptions.invoke(
                            peerConnectionFactory, 
                            eglBase.getEglBaseContext(), 
                            eglBase.getEglBaseContext());
                    Log.d(TAG, "成功设置视频硬件加速选项");
                }
            } catch (Exception e) {
                Log.e(TAG, "设置视频硬件加速选项失败: " + e.getMessage());
            }
            
            Log.d(TAG, "PeerConnectionFactory创建成功");
        } catch (Exception e) {
            Log.e(TAG, "初始化PeerConnectionFactory失败", e);
            throw new RuntimeException("初始化PeerConnectionFactory失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化媒体约束
     */
    private void initMediaConstraints() {
        // 初始化媒体约束
        audioConstraints = new MediaConstraints();
        videoConstraints = new MediaConstraints();
        sdpConstraints = new MediaConstraints();
        
        // 设置SDP约束
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
        
        // 添加视频约束，优化屏幕共享
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", "1280"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", "720"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", "25"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", "15"));
    }
    
    /**
     * 释放资源
     */
    public void release() {
        try {
            Log.d(TAG, "开始释放WebRTC资源");
            
            // 确保先停止服务
            stop();
            
            // 等待500毫秒，确保stop操作完成
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.e(TAG, "等待stop完成时被中断", e);
            }
            
            // 关闭执行器
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }
            
            // 释放视频轨道
            if (localVideoTrack != null) {
                localVideoTrack.dispose();
                localVideoTrack = null;
            }
            
            // 释放音频轨道
            if (localAudioTrack != null) {
                localAudioTrack.dispose();
                localAudioTrack = null;
            }
            
            // 释放视频源
            if (videoSource != null) {
                videoSource.dispose();
                videoSource = null;
            }
            
            // 释放音频源
            if (audioSource != null) {
                audioSource.dispose();
                audioSource = null;
            }
            
            // 停止视频捕获
            if (videoCapturer != null) {
                try {
                    videoCapturer.stopCapture();
                } catch (Exception e) {
                    Log.e(TAG, "停止视频捕获出错", e);
                    // 继续释放资源
                } finally {
                    videoCapturer.dispose();
                    videoCapturer = null;
                }
            }
            
            // 停止并释放MediaProjection
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            
            // 释放SurfaceTextureHelper
            if (surfaceTextureHelper != null) {
                surfaceTextureHelper.dispose();
                surfaceTextureHelper = null;
            }
            
            // 释放PeerConnectionFactory
            if (peerConnectionFactory != null) {
                peerConnectionFactory.dispose();
                peerConnectionFactory = null;
            }
            
            // 释放EglBase
            try {
                if (eglBase != null) {
                    eglBase.release();
                    eglBase = null;
                }
            } catch (Exception e) {
                Log.e(TAG, "释放EglBase出错", e);
            }
            
            // 清除回调避免内存泄漏
            mListener = null;
            
            Log.d(TAG, "WebRTC资源已释放");
        } catch (Exception e) {
            Log.e(TAG, "释放WebRTC资源时出错", e);
        }
    }
    
    /**
     * 请求屏幕捕获权限
     */
    public void requestScreenCapturePermission(Activity activity) {
        if (mContext == null) {
            mContext = activity.getApplicationContext();
        }
        
        // 启动系统的屏幕捕获权限请求
        activity.startActivityForResult(
                projectionManager.createScreenCaptureIntent(),
                CAPTURE_PERMISSION_REQUEST_CODE);
        
        Log.d(TAG, "已请求屏幕捕获权限");
    }
    
    /**
     * 处理屏幕捕获权限结果
     */
    public void handleScreenCapturePermissionResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            mediaProjectionPermissionResultData = data;
            Log.d(TAG, "已获取屏幕捕获权限");
            
            if (mListener != null) {
                mainHandler.post(() -> mListener.onScreenCapturePermissionGranted());
            }
        } else {
            Log.e(TAG, "屏幕捕获权限被拒绝");
            
            if (mListener != null) {
                mainHandler.post(() -> mListener.onError("屏幕捕获权限被拒绝"));
            }
        }
    }
    
    /**
     * 启动WebRTC服务
     */
    public void start() {
        if (mediaProjectionPermissionResultData == null) {
            Log.e(TAG, "未获取屏幕捕获权限，无法启动");
            if (mListener != null) {
                mainHandler.post(() -> mListener.onError("未获取屏幕捕获权限，请先调用requestScreenCapturePermission"));
            }
            return;
        }
        
        // 检查网络连接
        if (!isNetworkAvailable()) {
            Log.e(TAG, "网络不可用，无法启动WebRTC服务");
            if (mListener != null) {
                mainHandler.post(() -> mListener.onError("网络不可用，请检查网络连接后重试"));
            }
            return;
        }
        
        mState = STATE_CONNECTING;
        if (mListener != null) {
            mainHandler.post(() -> mListener.onStateChanged(STATE_CONNECTING));
        }
        
        // 在后台线程中处理
        executor.execute(() -> {
            try {
                // 1. 创建屏幕捕获器
                createScreenCapturer();
                
                // 2. 创建本地媒体流
                createLocalStream();
                
                // 3. 连接信令服务器
                connectToSignalingServer();
                
                // 4. 更新状态
                mState = STATE_CONNECTED;
                if (mListener != null) {
                    mainHandler.post(() -> {
                        mListener.onStateChanged(STATE_CONNECTED);
                        mListener.onConnected();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "启动WebRTC服务失败", e);
                mState = STATE_FAILED;
                if (mListener != null) {
                    mainHandler.post(() -> {
                        mListener.onError("启动WebRTC服务失败: " + e.getMessage());
                        mListener.onStateChanged(STATE_FAILED);
                    });
                }
                
                // 释放资源
                releaseScreenCapturer();
            }
        });
    }
    
    /**
     * 停止WebRTC服务
     */
    public void stop() {
        try {
            Log.d(TAG, "正在停止WebRTC服务...");
            
            // 关闭所有对等连接
            for (Peer peer : new ArrayList<>(peers)) {
                try {
                    peer.close();
                } catch (Exception e) {
                    Log.e(TAG, "关闭对等连接失败: " + peer.id, e);
                }
            }
            peers.clear();
            
            // 释放屏幕捕获器
            releaseScreenCapturer();
            
            // 断开信令服务器连接
            disconnectFromSignalingServer();
            
            // 更新状态
            setState(STATE_DISCONNECTED);
            
            Log.d(TAG, "WebRTC服务已停止");
        } catch (Exception e) {
            Log.e(TAG, "停止WebRTC服务时出错", e);
            if (mListener != null) {
                mainHandler.post(() -> mListener.onError("停止WebRTC服务失败: " + e.getMessage()));
            }
        }
    }
    
    /**
     * 检查网络是否可用
     */
    private boolean isNetworkAvailable() {
        try {
            android.net.ConnectivityManager connectivityManager = 
                    (android.net.ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager == null) {
                return false;
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.net.Network network = connectivityManager.getActiveNetwork();
                if (network == null) {
                    return false;
                }
                
                android.net.NetworkCapabilities capabilities = 
                        connectivityManager.getNetworkCapabilities(network);
                return capabilities != null && (
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                );
            } else {
                @SuppressWarnings("deprecation")
                android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "检查网络状态时出错", e);
            return false;
        }
    }
    
    /**
     * 创建屏幕捕获器
     */
    private void createScreenCapturer() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                // 获取屏幕尺寸
                DisplayMetrics metrics = new DisplayMetrics();
                WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                windowManager.getDefaultDisplay().getRealMetrics(metrics);
                int screenWidth = metrics.widthPixels;
                int screenHeight = metrics.heightPixels;
                float screenDensity = metrics.density;
                
                // 记录屏幕实际尺寸
                Log.d(TAG, "Screen size: " + screenWidth + "x" + screenHeight + ", density: " + screenDensity);
                
                // 计算合适的捕获尺寸
                int captureWidth, captureHeight;
                
                // 使用更高的分辨率以确保画面清晰
                captureWidth = 640;  // 使用640p分辨率
                captureHeight = 360; // 16:9比例
                captureFrameRate = 15; // 使用15fps，平衡延迟和流畅度
                
                Log.d(TAG, "使用屏幕捕获设置: " + captureWidth + "x" + captureHeight + "@" + captureFrameRate + "fps");
                
                // 确保宽高比与屏幕一致
                float screenRatio = (float)screenWidth / screenHeight;
                float captureRatio = (float)captureWidth / captureHeight;
                
                if (Math.abs(screenRatio - captureRatio) > 0.01) {
                    // 宽高比不匹配，调整以保持屏幕宽高比
                    captureHeight = Math.round(captureWidth / screenRatio);
                }
                
                // 确保宽高是偶数（编码器要求）
                captureWidth = captureWidth - (captureWidth % 2);
                captureHeight = captureHeight - (captureHeight % 2);
                
                // 更新捕获参数
                this.captureWidth = captureWidth;
                this.captureHeight = captureHeight;
                
                Log.d(TAG, "最终捕获分辨率: " + captureWidth + "x" + captureHeight + "@" + captureFrameRate + "fps");
                
                // 创建屏幕捕获器
                MediaProjectionManager mediaProjectionManager =
                        (MediaProjectionManager) mContext.getSystemService(
                                Context.MEDIA_PROJECTION_SERVICE);
                
                try {
                    // 尝试使用新版本的构造函数（带有MediaProjection.Callback参数）
                    screenCapturer = new ScreenCapturerAndroid(
                            mediaProjectionPermissionResultData,
                            new MediaProjection.Callback() {
                                @Override
                                public void onStop() {
                                    Log.e(TAG, "MediaProjection stopped");
                                }
                            });
                    Log.d(TAG, "成功创建ScreenCapturerAndroid (带回调)");
                } catch (NoSuchMethodError e) {
                    Log.d(TAG, "新版本构造函数不可用，尝试使用旧版本构造函数");
                    try {
                        // 尝试使用旧版本的构造函数（不带回调参数）
                        // 使用反射来调用不同参数的构造函数
                        java.lang.reflect.Constructor<?> constructor = 
                            ScreenCapturerAndroid.class.getConstructor(Intent.class);
                        screenCapturer = (ScreenCapturerAndroid) constructor.newInstance(mediaProjectionPermissionResultData);
                        Log.d(TAG, "成功使用旧版本构造函数创建ScreenCapturerAndroid");
                    } catch (Exception e2) {
                        Log.e(TAG, "创建ScreenCapturerAndroid失败: " + e2.getMessage(), e2);
                        throw new RuntimeException("无法创建屏幕捕获器", e2);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "创建ScreenCapturerAndroid异常: " + e.getMessage(), e);
                    throw new RuntimeException("无法创建屏幕捕获器", e);
                }
                
                // 将screenCapturer赋值给videoCapturer
                videoCapturer = screenCapturer;
                
                // 设置捕获参数
                SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("ScreenCapturerThread", eglBase.getEglBaseContext());
                videoSource = peerConnectionFactory.createVideoSource(true); // 强制设置为屏幕共享模式
                
                // 设置视频源的额外优化参数
                try {
                    // 设置较低的初始分辨率和较高的帧率
                    if (videoSource != null) {
                        Method adaptOutputFormatMethod = null;
                        
                        // 查找adaptOutputFormat方法
                        for (Method method : videoSource.getClass().getDeclaredMethods()) {
                            if (method.getName().equals("adaptOutputFormat")) {
                                adaptOutputFormatMethod = method;
                                break;
                            }
                        }
                        
                        if (adaptOutputFormatMethod != null) {
                            adaptOutputFormatMethod.setAccessible(true);
                            adaptOutputFormatMethod.invoke(videoSource, captureWidth, captureHeight, captureFrameRate);
                            Log.d(TAG, "强制设置视频源格式为 " + captureWidth + "x" + captureHeight + "@" + captureFrameRate + "fps");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error optimizing video source parameters: " + e.getMessage());
                }
                
                videoCapturer.initialize(surfaceTextureHelper, mContext, videoSource.getCapturerObserver());
                this.surfaceTextureHelper = surfaceTextureHelper;
                
                // 初始化视频捕获器后
                try {
                    // 1. 使用标准方法启动捕获
                    videoCapturer.startCapture(captureWidth, captureHeight, captureFrameRate);
                    Log.d(TAG, "已启动标准捕获: " + captureWidth + "x" + captureHeight + "@" + captureFrameRate);
                    
                    // 添加捕获状态监听
                    if (videoCapturer instanceof ScreenCapturerAndroid) {
                        ScreenCapturerAndroid screenCap = (ScreenCapturerAndroid)videoCapturer;
                        try {
                            // 尝试添加捕获回调
                            Method addCaptureCallbackMethod = screenCap.getClass().getDeclaredMethod("addCaptureCallback", 
                                Class.forName("org.webrtc.VideoCapturer$CapturerObserver"));
                            if (addCaptureCallbackMethod != null) {
                                addCaptureCallbackMethod.setAccessible(true);
                                
                                // 创建捕获回调
                                Object captureObserver = new Object() {
                                    public void onCapturerStarted(boolean success) {
                                        Log.d(TAG, "屏幕捕获已启动: " + success);
                                    }
                                    
                                    public void onCapturerStopped() {
                                        Log.d(TAG, "屏幕捕获已停止");
                                    }
                                    
                                    public void onFrameCaptured(VideoFrame frame) {
                                        // 记录帧信息
                                        if (frame != null) {
                                            Log.d(TAG, "捕获到帧: 宽度=" + frame.getBuffer().getWidth() + 
                                                  ", 高度=" + frame.getBuffer().getHeight() + 
                                                  ", 旋转=" + frame.getRotation());
                                        }
                                    }
                                };
                                
                                // 添加回调
                                addCaptureCallbackMethod.invoke(screenCap, captureObserver);
                                Log.d(TAG, "成功添加捕获回调");
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "添加捕获回调失败: " + e.getMessage());
                        }
                    }
                    
                    // 检查捕获是否成功启动
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (videoSource != null) {
                            try {
                                Method isCapturingMethod = videoSource.getClass().getDeclaredMethod("isCapturing");
                                if (isCapturingMethod != null) {
                                    isCapturingMethod.setAccessible(true);
                                    boolean isCapturing = (boolean)isCapturingMethod.invoke(videoSource);
                                    Log.d(TAG, "视频源捕获状态: " + (isCapturing ? "正在捕获" : "未捕获"));
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "检查捕获状态失败: " + e.getMessage());
                            }
                        }
                    }, 1000); // 1秒后检查
                    
                } catch (Exception e) {
                    Log.e(TAG, "设置捕获参数失败: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating screen capturer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 连接信令服务器
     */
    private void connectToSignalingServer() {
        try {
            Log.d(TAG, "正在连接信令服务器: " + mServerUrl);
            
            // 构建WebSocket URL
            final String wsUrl = mServerUrl + "/" + mRoomId + "/" + mClientId;
            Log.d(TAG, "完整WebSocket URL: " + wsUrl);
            
            // 创建WebSocket连接
            URI uri = new URI(wsUrl);
            mWebSocketClient = new org.java_websocket.client.WebSocketClient(uri) {
                @Override
                public void onOpen(org.java_websocket.handshake.ServerHandshake handshakedata) {
                    Log.d(TAG, "信令服务器连接成功");
                    
                    // 连接成功后启动心跳
                    startHeartbeat();
                    
                    if (mListener != null) {
                        mainHandler.post(() -> mListener.onSignalingConnected());
                    }
                }
                
                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "收到信令消息: " + (message.length() > 100 ? message.substring(0, 100) + "..." : message));
                    handleSignalingMessage(message);
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "信令服务器连接关闭: code=" + code + ", reason=" + reason + ", remote=" + remote);
                    
                    // 停止心跳
                    stopHeartbeat();
                    
                    if (mListener != null) {
                        mainHandler.post(() -> mListener.onSignalingDisconnected());
                    }
                }
                
                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "信令服务器连接错误", ex);
                    if (mListener != null) {
                        mainHandler.post(() -> mListener.onError("信令服务器连接错误: " + ex.getMessage()));
                    }
                }
            };
            
            // 设置连接参数
            mWebSocketClient.setConnectionLostTimeout(30); // 30秒超时检测
            mWebSocketClient.connect();
            
            // 等待连接建立（最多5秒）
            long startTime = System.currentTimeMillis();
            while (!mWebSocketClient.isOpen() && !mWebSocketClient.isClosed() && !mWebSocketClient.isClosing()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
                
                if (System.currentTimeMillis() - startTime > 5000) {
                    throw new RuntimeException("连接信令服务器超时");
                }
            }
            
            if (!mWebSocketClient.isOpen()) {
                throw new RuntimeException("连接信令服务器失败");
            }
            
            Log.d(TAG, "信令服务器连接已建立");
        } catch (URISyntaxException e) {
            Log.e(TAG, "信令服务器URL格式错误", e);
            throw new RuntimeException("信令服务器URL格式错误: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "连接信令服务器失败", e);
            throw new RuntimeException("连接信令服务器失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 断开信令服务器连接
     */
    private void disconnectFromSignalingServer() {
        if (mWebSocketClient != null) {
            try {
                mWebSocketClient.close();
            } catch (Exception e) {
                Log.e(TAG, "断开信令服务器连接失败", e);
            } finally {
                mWebSocketClient = null;
            }
        }
        
        stopHeartbeat();
        
        if (mListener != null) {
            mainHandler.post(() -> mListener.onSignalingDisconnected());
        }
    }
    
    /**
     * 开始心跳
     */
    private void startHeartbeat() {
        stopHeartbeat(); // 先停止已有的心跳
        
        heartbeatTimer = new java.util.Timer("WebRTC-Heartbeat");
        heartbeatTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                sendHeartbeat();
            }
        }, 1000, 15000); // 每15秒发送一次心跳
    }
    
    /**
     * 停止心跳
     */
    private void stopHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }
    
    /**
     * 发送心跳
     */
    private void sendHeartbeat() {
        try {
            if (mWebSocketClient != null && mWebSocketClient.isOpen()) {
                JSONObject heartbeat = new JSONObject();
                heartbeat.put("type", "heartbeat");
                mWebSocketClient.send(heartbeat.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "发送心跳失败", e);
        }
    }
    
    /**
     * 处理信令消息
     */
    private void handleSignalingMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type");
            
            switch (type) {
                case "offer":
                    handleRemoteOffer(json);
                    break;
                    
                case "answer":
                    handleRemoteAnswer(json);
                    break;
                    
                case "candidate":
                    handleRemoteIceCandidate(json);
                    break;
                    
                case "join":
                    // 有新客户端加入房间，向其发起连接
                    String clientId = json.optString("client");
                    if (!clientId.equals(mClientId)) {
                        createPeerConnection(clientId);
                    }
                    break;
                    
                case "leave":
                    // 客户端离开房间
                    String leaveClientId = json.optString("client");
                    removePeer(leaveClientId);
                    break;
                    
                case "room_clients":
                    // 已在房间的客户端列表
                    handleRoomClients(json);
                    break;
                    
                case "heartbeat_response":
                    // 心跳响应，无需处理
                    break;
                    
                case "command":
                    // 处理远程命令
                    String command = json.optString("command");
                    if (mListener != null) {
                        mainHandler.post(() -> mListener.onRemoteCommand(command));
                    }
                    break;
                    
                default:
                    Log.d(TAG, "未处理的消息类型: " + type);
                    break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析信令消息失败", e);
        }
    }
    
    /**
     * 处理房间客户端列表
     */
    private void handleRoomClients(JSONObject json) throws JSONException {
        JSONArray clientsArray = json.getJSONArray("clients");
        for (int i = 0; i < clientsArray.length(); i++) {
            String clientId = clientsArray.getString(i);
            if (!clientId.equals(mClientId)) {
                createPeerConnection(clientId);
            }
        }
    }
    
    /**
     * 处理远程Offer
     */
    private void handleRemoteOffer(JSONObject json) throws JSONException {
        String fromClientId = json.getString("from");
        
        // 修复SDP解析逻辑 - 处理嵌套的SDP对象
        String sdpString;
        if (json.has("sdp") && json.get("sdp") instanceof JSONObject) {
            JSONObject sdpObject = json.getJSONObject("sdp");
            sdpString = sdpObject.getString("sdp");
        } else {
            sdpString = json.getString("sdp");
        }
        
        Peer peer = getPeer(fromClientId);
        if (peer == null) {
            peer = createPeerConnection(fromClientId);
        }
        
        peer.handleRemoteOffer(sdpString);
    }
    
    /**
     * 处理远程Answer
     */
    private void handleRemoteAnswer(JSONObject json) throws JSONException {
        String fromClientId = json.getString("from");
        
        // 修复SDP解析逻辑 - 处理嵌套的SDP对象
        String sdpString;
        if (json.has("sdp") && json.get("sdp") instanceof JSONObject) {
            JSONObject sdpObject = json.getJSONObject("sdp");
            sdpString = sdpObject.getString("sdp");
        } else {
            sdpString = json.getString("sdp");
        }
        
        Peer peer = getPeer(fromClientId);
        if (peer != null) {
            peer.handleRemoteAnswer(sdpString);
        }
    }
    
    /**
     * 处理远程ICE候选
     */
    private void handleRemoteIceCandidate(JSONObject json) throws JSONException {
        try {
            String fromClientId = json.getString("from");
            JSONObject candidateJson = json.getJSONObject("candidate");
            
            Peer peer = getPeer(fromClientId);
            if (peer != null) {
                peer.addRemoteIceCandidate(candidateJson);
            }
        } catch (Exception e) {
            Log.e(TAG, "处理ICE候选失败", e);
        }
    }
    
    /**
     * 创建对等连接
     */
    private Peer createPeerConnection(String clientId) {
        Peer peer = new Peer(clientId);
        peers.add(peer);
        
        // 只有在本地客户端ID小于远程ID时才主动创建Offer
        // 这样可以避免双方同时创建offer导致的冲突
        if (mClientId.compareTo(clientId) < 0) {
            peer.createOffer();
        }
        
        return peer;
    }
    
    /**
     * 获取对等连接
     */
    private Peer getPeer(String clientId) {
        for (Peer peer : peers) {
            if (peer.id.equals(clientId)) {
                return peer;
            }
        }
        return null;
    }
    
    /**
     * 移除对等连接
     */
    private void removePeer(String clientId) {
        Peer peerToRemove = null;
        for (Peer peer : peers) {
            if (peer.id.equals(clientId)) {
                peerToRemove = peer;
                break;
            }
        }
        
        if (peerToRemove != null) {
            peerToRemove.close();
            peers.remove(peerToRemove);
        }
    }
    
    /**
     * 发送消息到信令服务器
     */
    private void sendMessage(JSONObject message) {
        if (mWebSocketClient != null && mWebSocketClient.isOpen()) {
            mWebSocketClient.send(message.toString());
        } else {
            Log.e(TAG, "无法发送消息：WebSocket连接未建立");
        }
    }
    
    /**
     * 序列化ICE候选
     */
    private JSONObject serializeIceCandidate(IceCandidate iceCandidate) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("sdpMid", iceCandidate.sdpMid);
        json.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
        json.put("sdp", iceCandidate.sdp);
        return json;
    }
    
    /**
     * 反序列化ICE候选
     */
    private IceCandidate deserializeIceCandidate(JSONObject json) throws JSONException {
        try {
            // 检查标准格式
            if (json.has("sdpMid") && json.has("sdpMLineIndex") && json.has("sdp")) {
                return new IceCandidate(
                        json.getString("sdpMid"),
                        json.getInt("sdpMLineIndex"),
                        json.getString("sdp"));
            }
            
            // 检查替代格式 - 一些信令服务器可能用不同字段
            if (json.has("id") && json.has("label") && json.has("candidate")) {
                return new IceCandidate(
                        json.getString("id"),
                        json.getInt("label"),
                        json.getString("candidate"));
            }
            
            // 处理候选字段直接在候选JSON对象中的情况
            if (json.has("candidate")) {
                // 尝试从candidate字段中解析完整的候选信息
                String candidateStr = json.getString("candidate");
                
                // 提取sdpMid (通常是音频或视频标识符，如"audio"或"video")
                String sdpMid = json.optString("sdpMid", "video");
                
                // 默认使用0作为sdpMLineIndex，常见于视频轨道
                int sdpMLineIndex = json.optInt("sdpMLineIndex", 0);
                
                return new IceCandidate(sdpMid, sdpMLineIndex, candidateStr);
            }
            
            throw new JSONException("不支持的ICE候选格式: " + json.toString());
        } catch (Exception e) {
            Log.e(TAG, "反序列化ICE候选失败: " + json.toString(), e);
            throw e;
        }
    }
    
    /**
     * WebRTC事件监听器接口
     */
    public interface WebRTCListener {
        // 状态变化回调
        void onStateChanged(int state);
        
        // 错误回调
        void onError(String errorMessage);
        
        // 信令服务器连接成功回调
        void onSignalingConnected();
        
        // 信令服务器断开连接回调
        void onSignalingDisconnected();
        
        // 连接成功回调
        void onConnected();
        
        // 对等方连接成功回调
        void onPeerConnected(String peerId);
        
        // 对等方断开连接回调
        void onPeerDisconnected(String peerId);
        
        // 接收到远程命令回调
        void onRemoteCommand(String command);
        
        // 屏幕捕获权限授予回调
        void onScreenCapturePermissionGranted();
        
        // 屏幕捕获停止回调
        void onScreenCaptureStopped();
        
        // 本地媒体流创建成功回调
        default void onLocalStreamCreated(MediaStream localMediaStream) {
            // 默认实现，旧代码不需要实现此方法
        }
    }

    /**
     * 设置服务状态
     */
    private void setState(int state) {
        mState = state;
        if (mListener != null) {
            mainHandler.post(() -> mListener.onStateChanged(state));
        }
    }

    /**
     * 自定义视频编码器工厂，用于优先使用指定的编解码器
     */
    private static class CustomVideoEncoderFactory extends DefaultVideoEncoderFactory {
        private final org.webrtc.VideoCodecInfo[] preferredCodecs;
        
        public CustomVideoEncoderFactory(EglBase.Context eglContext, 
                                         boolean enableIntelVp8Encoder,
                                         boolean enableH264HighProfile,
                                         org.webrtc.VideoCodecInfo[] preferredCodecs) {
            super(eglContext, enableIntelVp8Encoder, enableH264HighProfile);
            this.preferredCodecs = preferredCodecs;
        }
        
        @Override
        public org.webrtc.VideoCodecInfo[] getSupportedCodecs() {
            return preferredCodecs != null ? preferredCodecs : super.getSupportedCodecs();
        }
    }

    /**
     * 释放屏幕捕获器
     */
    private void releaseScreenCapturer() {
        try {
            // 停止屏幕捕获器
            if (screenCapturer != null) {
                screenCapturer.stopCapture();
                screenCapturer.dispose();
                screenCapturer = null;
            }
            
            // 释放视频源
            if (videoSource != null) {
                videoSource.dispose();
                videoSource = null;
            }
            
            // 释放视频轨道
            if (localVideoTrack != null) {
                localVideoTrack.dispose();
                localVideoTrack = null;
            }
            
            // 停止MediaProjection
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            
            Log.d(TAG, "屏幕捕获器已释放");
        } catch (Exception e) {
            Log.e(TAG, "释放屏幕捕获器出错", e);
        }
    }

    /**
     * 创建本地媒体流
     */
    private void createLocalStream() {
        try {
            Log.d(TAG, "创建本地媒体流");
            
            // 确保视频源已创建
            if (videoSource == null) {
                Log.w(TAG, "视频源为null，尝试创建屏幕捕获器");
                createScreenCapturer();
                return;
            }
            
            // 创建音频源（但保持禁用状态）
            if (audioSource == null) {
                audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
                Log.d(TAG, "创建了音频源");
            }
            
            // 创建视频轨道
            if (localVideoTrack == null && videoSource != null) {
                localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
                localVideoTrack.setEnabled(true);
                Log.d(TAG, "创建了视频轨道: " + localVideoTrack.id());
                
                // 添加帧率监控
                try {
                    // 初始化帧率监控时间
                    lastFpsLogTime = System.currentTimeMillis();
                    
                    // 获取原始VideoSink
                    Field sinkField = localVideoTrack.getClass().getDeclaredField("sink");
                    if (sinkField != null) {
                        sinkField.setAccessible(true);
                        VideoSink originalSink = (VideoSink) sinkField.get(localVideoTrack);
                        
                        // 创建并设置带监控的VideoSink
                        if (originalSink != null) {
                            FrameRateMonitor monitor = new FrameRateMonitor(originalSink);
                            sinkField.set(localVideoTrack, monitor);
                            Log.d(TAG, "成功添加帧率监控器");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "添加帧率监控器失败: " + e.getMessage());
                }
                
                // 设置视频轨道的内容提示为屏幕共享
                try {
                    // 检查setContentHint方法是否存在
                    Method setContentHintMethod = null;
                    
                    for (Method method : localVideoTrack.getClass().getDeclaredMethods()) {
                        if (method.getName().equals("setContentHint")) {
                            setContentHintMethod = method;
                            break;
                        }
                    }
                    
                    if (setContentHintMethod != null) {
                        // 获取参数类型
                        Class<?>[] paramTypes = setContentHintMethod.getParameterTypes();
                        if (paramTypes.length == 1) {
                            // 根据参数类型调用方法
                            if (paramTypes[0].isEnum()) {
                                // 如果参数是枚举类型
                                try {
                                    // 尝试获取SCREENSHARE枚举值
                                    Class<?> enumClass = paramTypes[0];
                                    Object[] constants = enumClass.getEnumConstants();
                                    Object screenShareEnum = null;
                                    
                                    // 查找名为SCREENSHARE的枚举常量
                                    for (Object constant : constants) {
                                        if (constant.toString().equals("SCREENSHARE")) {
                                            screenShareEnum = constant;
                                            break;
                                        }
                                    }
                                    
                                    if (screenShareEnum != null) {
                                        // 调用setContentHint方法
                                        setContentHintMethod.invoke(localVideoTrack, screenShareEnum);
                                        Log.d(TAG, "Successfully set content hint to SCREENSHARE (enum)");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error setting content hint (enum): " + e.getMessage());
                                }
                            } else if (paramTypes[0] == int.class || paramTypes[0] == Integer.class) {
                                // 如果参数是整数类型，假设SCREENSHARE的值为2
                                setContentHintMethod.invoke(localVideoTrack, 2);
                                Log.d(TAG, "Successfully set content hint to SCREENSHARE (int)");
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error setting content hint: " + e.getMessage());
                }
            }
            
            // 创建音频轨道（但保持禁用状态）
            if (localAudioTrack == null && audioSource != null) {
                localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
                localAudioTrack.setEnabled(false); // 默认禁用音频
                Log.d(TAG, "创建了音频轨道: " + localAudioTrack.id() + " (禁用状态)");
            }
            
            // 创建本地媒体流
            if (localMediaStream == null) {
                localMediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID);
                // Log.d(TAG, "创建了本地媒体流: " + localMediaStream.id());
                
                // 添加视频轨道
                if (localVideoTrack != null && localMediaStream.videoTracks.size() == 0) {
                    localMediaStream.addTrack(localVideoTrack);
                    Log.d(TAG, "添加视频轨道到媒体流");
                }
                
                // 添加音频轨道（虽然禁用）
                if (localAudioTrack != null && localMediaStream.audioTracks.size() == 0) {
                    localMediaStream.addTrack(localAudioTrack);
                    Log.d(TAG, "添加音频轨道到媒体流(禁用状态)");
                }
                
                // 通知监听器
                if (mListener != null) {
                    mainHandler.post(() -> mListener.onLocalStreamCreated(localMediaStream));
                }
                
                // Log.d(TAG, "本地媒体流创建完成: " + localMediaStream.id() +
                //         ", 视频轨道数: " + localMediaStream.videoTracks.size() +
                //         ", 音频轨道数: " + localMediaStream.audioTracks.size());
            }
        } catch (Exception e) {
            Log.e(TAG, "创建本地媒体流失败", e);
            throw new RuntimeException("创建本地媒体流失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建帧率监控器
     */
    private class FrameRateMonitor implements VideoSink {
        private VideoSink target;
        
        public FrameRateMonitor(VideoSink target) {
            this.target = target;
        }
        
        @Override
        public void onFrame(VideoFrame frame) {
            // 转发帧到目标
            if (target != null) {
                target.onFrame(frame);
            }
            
            // 计数并记录帧率
            synchronized (fpsLock) {
                frameCount++;
                long now = System.currentTimeMillis();
                if (now - lastFpsLogTime > 5000) { // 每5秒记录一次
                    float fps = frameCount * 1000.0f / (now - lastFpsLogTime);
                    Log.i(TAG, "发送帧率: " + fps + " FPS");
                    frameCount = 0;
                    lastFpsLogTime = now;
                }
            }
        }
    }
}