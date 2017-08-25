package com.github.games647.fastlogin.core;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLSocketFactory;

public class BalancedSSLFactory extends SSLSocketFactory {

    private final SSLSocketFactory oldFactory;

    //in order to be thread-safe
    private final List<InetAddress> localAddresses;

    private AtomicInteger id;

    public BalancedSSLFactory(SSLSocketFactory oldFactory, Iterable<InetAddress> localAddresses) {
        this.oldFactory = oldFactory;
        this.localAddresses = ImmutableList.copyOf(localAddresses);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return oldFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return oldFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return oldFactory.createSocket(host, port, getNextLocalAddress(), 0);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return oldFactory.createSocket(host, port, getNextLocalAddress(), 0);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort)
            throws IOException {
        //default
        return oldFactory.createSocket(host, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return oldFactory.createSocket(host, port, getNextLocalAddress(), 0);
    }

    @Override
    public Socket createSocket(InetAddress host, int port, InetAddress local, int localPort) throws IOException {
        //Default
        return oldFactory.createSocket(host, port, local, localPort);
    }

    private InetAddress getNextLocalAddress() {
        int index = id.incrementAndGet() % localAddresses.size();
        return localAddresses.get(index);
    }
}
