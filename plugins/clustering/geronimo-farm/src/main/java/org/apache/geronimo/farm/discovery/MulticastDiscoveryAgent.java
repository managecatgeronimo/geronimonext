/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.farm.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.geronimo.gbean.annotation.GBean;
import org.apache.geronimo.gbean.annotation.ParamAttribute;
import org.apache.geronimo.gbean.annotation.ParamReference;
import org.apache.geronimo.gbean.GBeanLifecycle;

/**
 * @version $Rev$ $Date$
 */
@GBean
public class MulticastDiscoveryAgent implements DiscoveryAgent, GBeanLifecycle {

    private static final Logger log = LoggerFactory.getLogger(MulticastDiscoveryAgent.class);

    private static final int BUFF_SIZE = 8192;


    private AtomicBoolean started = new AtomicBoolean(false);
    private MulticastSocket multicast;

    private String host = "239.255.3.2";
    private int port = 6142;

    private int timeToLive = 1;
    private boolean loopbackMode = false;
    private SocketAddress address;

    private Map<String, Service> registeredServices = new ConcurrentHashMap<String, Service>();

    private int maxMissedHeartbeats = 10;
    private long heartRate = 500;

    private final Listener listener;

    public MulticastDiscoveryAgent() {
        listener = new Listener();
    }

    // ---------------------------------
    // Listenting specific settings
    private long initialReconnectDelay = 1000 * 5;
    private long maxReconnectDelay = 1000 * 30;
    private long backOffMultiplier = 0;
    private boolean useExponentialBackOff;
    private int maxReconnectAttempts = 10; // todo: check this out
    // ---------------------------------

    public MulticastDiscoveryAgent(@ParamReference(name="MulticastLocation")MulticastLocation location,
                                   @ParamAttribute(name="heartRate") long heartRate,
                                   @ParamAttribute(name="maxMissedHeartbeats") int maxMissedHeartbeats,
                                   @ParamAttribute(name="loopbackMode")  boolean loopbackMode,
                                   @ParamAttribute(name="initialReconnectDelay") long initialReconnectDelay,
                                   @ParamAttribute(name="maxReconnectDelay") long maxReconnectDelay,
                                   @ParamAttribute(name="maxReconnectAttempts") int maxReconnectAttempts,
                                   @ParamAttribute(name="backOffMultiplier") long backOffMultiplier,
                                   @ParamAttribute(name="useExponentialBackOff") boolean useExponentialBackOff) {
        this.host = location.getHost();
        this.port = location.getPort();
        this.heartRate = heartRate;
        this.maxMissedHeartbeats = maxMissedHeartbeats;
        this.loopbackMode = loopbackMode;
        this.initialReconnectDelay = initialReconnectDelay;
        this.maxReconnectDelay = maxReconnectDelay;
        this.maxReconnectAttempts = maxReconnectAttempts;
        this.backOffMultiplier = backOffMultiplier;
        this.useExponentialBackOff = useExponentialBackOff;
        listener = new Listener();
    }


    public String getIP() {
        return host;
    }

    public String getName() {
        return "multicast";
    }

    public int getPort() {
        return port;
    }

    public void setDiscoveryListener(DiscoveryListener listener) {
        this.listener.setDiscoveryListener(listener);
    }

    public void registerService(URI serviceUri) throws IOException {
        Service service = new Service(serviceUri);
        this.registeredServices.put(service.uriString, service);
    }

    public void unregisterService(URI serviceUri) throws IOException {
        Service service = new Service(serviceUri);
        this.registeredServices.remove(service.uriString);
    }

    public void reportFailed(URI serviceUri) throws IOException {
        listener.reportFailed(serviceUri);
    }


    private boolean isSelf(Service service) {
        return isSelf(service.uriString);
    }

    private boolean isSelf(String service) {
        return registeredServices.keySet().contains(service);
    }

    public static void main(String[] args) throws Exception {
    }

    /**
     * start the discovery agent
     *
     * @throws Exception on error
     */
    public void doStart() throws Exception {
            if (started.compareAndSet(false, true)) {

                newSocket();

                Thread listenerThread = new Thread(listener);
                listenerThread.setName("MulticastDiscovery: Listener");
                listenerThread.setDaemon(true);
                listenerThread.start();

                Broadcaster broadcaster = new Broadcaster();

                Timer timer = new Timer("MulticastDiscovery: Broadcaster", true);
                timer.scheduleAtFixedRate(broadcaster, 0, heartRate);
            }
    }

    private void newSocket() throws IOException {
        InetAddress inetAddress = InetAddress.getByName(host);

        this.address = new InetSocketAddress(inetAddress, port);

        multicast = new MulticastSocket(port);
        multicast.setLoopbackMode(loopbackMode);
        multicast.setTimeToLive(timeToLive);
        multicast.joinGroup(inetAddress);
        multicast.setSoTimeout((int) heartRate);
    }

    /**
     * stop the channel
     *
     * @throws Exception pm error
     */
    public void doStop() throws Exception {
        if (started.compareAndSet(true, false)) {
            multicast.close();
        }
    }

    /**
     * Fails the GBean.  This informs the GBean that it is about to transition to the failed state.
     */
    public void doFail() {
        try {
            doStop();
        } catch (Exception e) {
            //ignore
        }
    }


    class Service {
        private final URI uri;
        private final String uriString;

        public Service(URI uri) {
            this.uri = uri;
            this.uriString = uri.toString();
        }

        public Service(String uriString) throws URISyntaxException {
            this(new URI(uriString));
        }
    }

    private class ServiceVitals {

        private final Service service;

        private long lastHeartBeat;
        private long recoveryTime;
        private int failureCount;
        private boolean dead;

        public ServiceVitals(Service service) {
            this.service = service;
            this.lastHeartBeat = System.currentTimeMillis();
        }

        public synchronized void heartbeat() {
            lastHeartBeat = System.currentTimeMillis();

            // Consider that the service recovery has succeeded if it has not
            // failed in 60 seconds.
            if (!dead && failureCount > 0 && (lastHeartBeat - recoveryTime) > 1000 * 60) {
                if (log.isDebugEnabled()) {
                    log.debug("I now think that the " + service + " service has recovered.");
                }
                failureCount = 0;
                recoveryTime = 0;
            }
        }

        public synchronized long getLastHeartbeat() {
            return lastHeartBeat;
        }

        public synchronized boolean pronounceDead() {
            if (!dead) {
                dead = true;
                failureCount++;

                long reconnectDelay;
                if (useExponentialBackOff) {
                    reconnectDelay = (long) Math.pow(backOffMultiplier, failureCount);
                    if (reconnectDelay > maxReconnectDelay) {
                        reconnectDelay = maxReconnectDelay;
                    }
                } else {
                    reconnectDelay = initialReconnectDelay;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Remote failure of " + service + " while still receiving multicast advertisements.  " +
                            "Advertising events will be suppressed for " + reconnectDelay
                            + " ms, the current failure count is: " + failureCount);
                }

                recoveryTime = System.currentTimeMillis() + reconnectDelay;
                return true;
            }
            return false;
        }

        /**
         * @return true if this broker is marked failed and it is now the right
         *         time to start recovery.
         */
        public synchronized boolean doRecovery() {
            if (!dead) {
                return false;
            }

            // Are we done trying to recover this guy?
            if (maxReconnectAttempts > 0 && failureCount > maxReconnectAttempts) {
                if (log.isDebugEnabled()) {
                    log.debug("Max reconnect attempts of the " + service + " service has been reached.");
                }
                return false;
            }

            // Is it not yet time?
            if (System.currentTimeMillis() < recoveryTime) {
                return false;
            }

            if (log.isDebugEnabled()) {
                log.debug("Resuming event advertisement of the " + service + " service.");
            }
            dead = false;
            return true;
        }

        public boolean isDead() {
            return dead;
        }
    }


    class Listener implements Runnable {
        private Map<String, ServiceVitals> discoveredServices = new ConcurrentHashMap<String, ServiceVitals>();
        private DiscoveryListener discoveryListener;

        public void setDiscoveryListener(DiscoveryListener discoveryListener) {
            this.discoveryListener = discoveryListener;
        }

        public void run() {
            byte[] buf = new byte[BUFF_SIZE];
            DatagramPacket packet = new DatagramPacket(buf, 0, buf.length);
            while (started.get()) {
                checkServices();
                try {
                    multicast.receive(packet);
                    if (packet.getLength() > 0) {
                        String str = new String(packet.getData(), packet.getOffset(), packet.getLength());
//                        System.out.println("read = " + str);
                        processData(str);
                    }
                } catch (SocketTimeoutException se) {
                    // ignore
                } catch (IOException e) {
                    if (started.get()) {
                        log.error("failed to process packet: " + e);
                    }
                }
            }
        }

        private void processData(String uriString) {
            if (discoveryListener == null) {
                return;
            }
            if (isSelf(uriString)) {
                return;
            }

            ServiceVitals vitals = discoveredServices.get(uriString);

            if (vitals == null) {
                try {
                    vitals = new ServiceVitals(new Service(uriString));

                    discoveredServices.put(uriString, vitals);

                    fireServiceAddEvent(vitals.service.uri);
                } catch (URISyntaxException e) {
                    // don't continuously log this
                }

            } else {
                vitals.heartbeat();

                if (vitals.doRecovery()) {
                    fireServiceAddEvent(vitals.service.uri);
                }
            }
        }

        private void checkServices() {
            long expireTime = System.currentTimeMillis() - (heartRate * maxMissedHeartbeats);
            for (ServiceVitals serviceVitals : discoveredServices.values()) {
                if (serviceVitals.getLastHeartbeat() < expireTime && !isSelf(serviceVitals.service)) {

                    ServiceVitals vitals = discoveredServices.remove(serviceVitals.service.uriString);
                    if (vitals != null && !vitals.isDead()) {
                        fireServiceRemovedEvent(vitals.service.uri);
                    }
                }
            }
        }

        private final Executor executor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            public Thread newThread(Runnable runable) {
                Thread t = new Thread(runable, "Multicast Discovery Agent Notifier");
                t.setDaemon(true);
                return t;
            }
        });

        private void fireServiceRemovedEvent(final URI uri) {
            if (discoveryListener != null) {
                final DiscoveryListener discoveryListener = this.discoveryListener;

                // Have the listener process the event async so that
                // he does not block this thread since we are doing time sensitive
                // processing of events.
                executor.execute(new Runnable() {
                    public void run() {
                        if (discoveryListener != null) {
                            discoveryListener.serviceRemoved(uri);
                        }
                    }
                });
            }
        }

        private void fireServiceAddEvent(final URI uri) {
            if (discoveryListener != null) {
                final DiscoveryListener discoveryListener = this.discoveryListener;

                // Have the listener process the event async so that
                // he does not block this thread since we are doing time sensitive
                // processing of events.
                executor.execute(new Runnable() {
                    public void run() {
                        if (discoveryListener != null) {
                            discoveryListener.serviceAdded(uri);
                        }
                    }
                });
            }
        }

        public void reportFailed(URI serviceUri) {
            final Service service = new Service(serviceUri);
            ServiceVitals serviceVitals = discoveredServices.get(service.uriString);
            if (serviceVitals != null && serviceVitals.pronounceDead()) {
                fireServiceRemovedEvent(service.uri);
            }
        }
    }

    class Broadcaster extends TimerTask {
        private IOException failed;

        public void run() {
            if (started.get()) {
                heartbeat();
            }
        }

        private void heartbeat() {
            for (String uri : registeredServices.keySet()) {
                try {
                    byte[] data = uri.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, 0, data.length, address);
//                    System.out.println("ann = " + uri);
                    multicast.send(packet);
                    failed = null;
                } catch (IOException e) {
                    // If a send fails, chances are all subsequent sends will fail
                    // too.. No need to keep reporting the
                    // same error over and over.
                    try {
                        newSocket();
                    } catch (IOException e1) {
                        //ignore
                    }
                    if (failed == null) {
                        failed = e;

                        log.error("Failed to advertise our service: " + uri, e);
                        if ("Operation not permitted".equals(e.getMessage())) {
                            log.error("The 'Operation not permitted' error has been know to be caused by improper firewall/network setup.  "
                                    + "Please make sure that the OS is properly configured to allow multicast traffic over: " + multicast.getLocalAddress());
                        }
                    }
                }
            }
        }
    }

}
