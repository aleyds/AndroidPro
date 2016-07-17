package com.alcohol.db;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.conn.ssl.SSLSocketFactory;

public class CBLTLSSocketFactory extends SSLSocketFactory {
	
	SSLContext sslContext = SSLContext.getInstance("TLSv1.2");

	public CBLTLSSocketFactory(KeyStore truststore)
			throws NoSuchAlgorithmException, KeyManagementException,
			KeyStoreException, UnrecoverableKeyException {
		super(truststore);
		
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
		tmf.init(truststore);
		TrustManager[] trustManagers = tmf.getTrustManagers();
		sslContext.init(null, trustManagers, null);
	}

	@Override
	public Socket createSocket() throws IOException {
		SSLSocket ssl_socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
		String[] protocols = {"TLSv1.2"};
		ssl_socket.setEnabledProtocols(protocols);
		return ssl_socket;
	}

	@Override
	public Socket createSocket(Socket socket, String host, int port,
			boolean autoClose) throws IOException, UnknownHostException {
		SSLSocket ssl_socket = (SSLSocket) sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		String[] protocols = {"TLSv1.2"};
		ssl_socket.setEnabledProtocols(protocols);
		return ssl_socket;
	}
	
	

}
