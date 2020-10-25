/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FilenameUtils;

import com.ibm.ws.security.utility.IFileUtility;
import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

/**
 * Main class for password encryption utility.
 * Not bundled with the core runtime jars by design.
 */
public class RetrieveSignerFromPortTask extends BaseCommandTask {
    private static final String ARG_HOST = "--host";
    private static final String ARG_PORT = "--port";
    private static final String ARG_KEYSTORE = "--keystore";
    private static final String ARG_V = "--v";
    private static final String ARG_VERBOSE = "--verbose";
    private static final List<String> ARG_TABLE = Arrays.asList(ARG_HOST, ARG_PORT, ARG_KEYSTORE, ARG_V, ARG_VERBOSE);
    private static List<String> resultLabel = new ArrayList<String>();
    private final IFileUtility fileUtility;

    public RetrieveSignerFromPortTask(IFileUtility fileUtility, String scriptName) {
        super(scriptName);
        this.fileUtility = fileUtility;
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskName() {
        return "retrieveSignerFromPort";
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {
        return getTaskHelp("retrieveSignerFromPort.desc", "retrieveSignerFromPort.usage.options",
                           "retrieveSignerFromPort.required-key.", "retrieveSignerFromPort.required-desc.",
                           "retrieveSignerFromPort.option-key.", "retrieveSignerFromPort.option-desc.",
                           null, null,
                           scriptName);
    }

    @Override
    public String getTaskDescription() {
        return getOption("retrieveSignerFromPort.desc", true);
    }

    /** {@inheritDoc} */
    @Override
    public SecurityUtilityReturnCodes handleTask(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr, String[] args) throws Exception {
        stdout.println("servers: " + fileUtility.getServersDirectory());
        stdout.println("client: " + fileUtility.getClientsDirectory());
        stdout.println("wlp: " + fileUtility.getServersDirectory().substring(0, fileUtility.getServersDirectory().lastIndexOf("usr")));

        String targetDir = fileUtility.getServersDirectory();
        if (targetDir == null || targetDir.isEmpty()) {

        } else {
            targetDir = targetDir.substring(0, targetDir.lastIndexOf("usr"));
        }
        String host = getArgumentValue(ARG_HOST, args, null);
        //String keystore = getArgumentValue(ARG_KEYSTORE, args, null);
        int port = Integer.parseInt(getArgumentValue(ARG_PORT, args, null));

        File keystorefile = null;//new File(keystore);
        // if (!keystorefile.isFile()) {
        File dir = new File(new File(System.getProperty("java.home"), "lib"), "security");
        keystorefile = new File(dir, "cacerts");
        // }

        InputStream in = new FileInputStream(keystorefile);
        KeyStore keystore2 = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore2.load(in, "changeit".toCharArray());
        in.close();

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keystore2);
        TrustManager tm = new ReaderTrustManager((X509TrustManager) (tmf.getTrustManagers()[0]));
        SSLContext certificateContext = SSLContext.getInstance("TLS");
        certificateContext.init(null, new TrustManager[] { tm }, null);
        SSLSocket certificateSocket = (SSLSocket) certificateContext.getSocketFactory().createSocket(host, port);
        certificateSocket.startHandshake();
        X509Certificate[] chain = ((ReaderTrustManager) tm).getChain();

        String result = "";
        for (int j = 0; j < chain.length; j++) {
            result += "chain[" + j + "]\n-----BEGIN CERTIFICATE-----\n" + chain[j] + "\n-----END CERTIFICATE-----\n";
//            System.out.println(); //Base64.getEncoder().encode(, chain[j].getEncoded()
//            System.out.println("chain[" + j + "]\n-----BEGIN CERTIFICATE-----\n" + chain[j] + "\n-----END CERTIFICATE-----\n");
        }
        certificateSocket.close();
        File outputFile = generateConfigFileName(this.getTaskName(), targetDir, stdout);

        fileUtility.createParentDirectory(stdout, outputFile);
        fileUtility.writeToFile(stderr, result, outputFile);
//        for (String line : resultLabel) {
//            stdout.println(line);
//        }
        return SecurityUtilityReturnCodes.OK;
    }

    /**
     * @param args The command arguments
     * @return True if (v)erbose was input
     */
    private boolean checkVerboseArgs(String[] args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(ARG_V) || arg.equalsIgnoreCase(ARG_VERBOSE)) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    boolean isKnownArgument(String arg) {
        boolean value = false;
        if (arg != null) {
            value = ARG_TABLE.contains(arg);
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    void checkRequiredArguments(String[] args) {
        String message = "";
        // We expect at least two arguments and the task name
        if (args.length < 2) {
            message = getMessage("insufficientArgs");
        }

        boolean portFound = false;
        boolean hostFound = false;
        for (String arg : args) {
            if (arg.startsWith(ARG_PORT)) {
                portFound = true;
            }
            if (arg.startsWith(ARG_HOST)) {
                hostFound = true;
            }
        }
        if (!hostFound) {
            message += " " + getMessage("missingArg", ARG_HOST);
        }
        if (!portFound) {
            message += " " + getMessage("missingArg", ARG_PORT);
        }
        if (!message.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    protected String getArgumentValue(String arg, String[] args,
                                      String defalt) {
        for (int i = 1; i < args.length; i++) {
            String key = args[i].split("=")[0];
            if (key.equals(arg)) {
                return getValue(args[i]);
            }
        }
        return defalt;
    }

    protected File generateConfigFileName(String taskName, String targetDir, PrintStream stdout) {
        // if no file path was provided, create the config file in the server directory
//        if (targetFilepath == null || targetFilepath.equals("")) {
//            targetFilepath = serverDir + SLASH;
//        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd-HH.mm.ss");
        LocalDateTime now = LocalDateTime.now();
        String date = dtf.format(now);

        // if the file path is a directory, generate a file name
        File outputFile = new File(targetDir);
        if (fileUtility.isDirectory(outputFile)) {
            outputFile = new File(outputFile, taskName + "-" + date + ".txt");
        }

        // generate a new file name until we have no conflicts
        if (fileUtility.exists(outputFile)) {
            String filePath = FilenameUtils.removeExtension(outputFile.getPath());
            String fileExt = FilenameUtils.getExtension(outputFile.getPath());
            int counter = 1;
            do {
                outputFile = new File(filePath + counter + "." + fileExt);
                counter++;
            } while (fileUtility.exists(outputFile));
        }

        return outputFile;
    }

    private class ReaderTrustManager implements X509TrustManager {

        private final X509TrustManager tm;
        private X509Certificate[] chain;

        ReaderTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
            //throw new UnsupportedOperationException();
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            this.chain = chain;
            tm.checkServerTrusted(chain, authType);
        }

        public X509Certificate[] getChain() {
            return chain;
        }

    }
}
