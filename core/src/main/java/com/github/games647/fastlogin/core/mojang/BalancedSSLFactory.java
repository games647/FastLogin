package com.github.games647.fastlogin.core.mojang;

import com.google.common.collect.Iterables;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class BalancedSSLFactory extends SSLSocketFactory {

    //in order to be thread-safe
    private final Iterator<InetAddress> iterator;
    private final SSLSocketFactory oldFactory;

    public BalancedSSLFactory(SSLSocketFactory oldFactory, Iterable<InetAddress> localAddresses) {
        this.oldFactory = oldFactory;
        this.iterator = Iterables.cycle(localAddresses).iterator();
    }

    public BalancedSSLFactory(Iterable<InetAddress> iterator) {
        this(HttpsURLConnection.getDefaultSSLSocketFactory(), iterator);
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
        synchronized (iterator) {
            return iterator.next();
        }
    }
}
