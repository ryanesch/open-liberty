/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.acme.fat;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.AcmeCaException;
import com.ibm.ws.security.acme.AcmeCertificate;
import com.ibm.ws.security.acme.docker.PebbleContainer;
import com.ibm.ws.security.acme.internal.AcmeClient;
import com.ibm.ws.security.acme.internal.CSROptions;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;
import com.ibm.ws.security.acme.utils.HttpChallengeServer;

import componenttest.custom.junit.runner.FATRunner;

/**
 * Unit tests for the {@link AcmeClient} class. These tests are limited to those
 * that do not interact with an ACME CA service.
 */
@RunWith(FATRunner.class)
public class AcmeClientTest {

	private static final String TEST_DOMAIN_1 = "domain1.com";
	private static final String TEST_DOMAIN_2 = "domain2.com";
	private static final String TEST_DOMAIN_3 = "domain3.com";
	private static final String TEST_DOMAIN_4 = "domain4.com";

	private static final String FILE_MINICA_PEM = "lib/LibertyFATTestFiles/pebble.minica.pem";
	private static final String TRUSTSTORE_FILE;
	private static final String FILE_ACCOUNT_KEY;
	private static final String FILE_DOMAIN_KEY;
	private static final String TRUSTSTORE_PASSWORD = "password";
	private static X509Certificate pebbleIntermediateCertificate = null;
	private static HttpChallengeServer challengeServer = null;
	private static final String acmeDirectoryURI = FATSuite.pebble.getAcmeDirectoryURI();

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	static {

		try {
			/*
			 * Create temp files that we can use to create file paths.
			 */
			File truststore = File.createTempFile("truststore", "jks");
			File accountKey = File.createTempFile("accountKey", "pem");
			File domainKey = File.createTempFile("domainKey", "pem");
			TRUSTSTORE_FILE = truststore.getAbsolutePath();
			FILE_ACCOUNT_KEY = accountKey.getAbsolutePath();
			FILE_DOMAIN_KEY = domainKey.getAbsolutePath();

			/*
			 * Delete them so we can generate them in the test.
			 */
			truststore.delete();
			accountKey.delete();
			domainKey.delete();

			/*
			 * Delete the keys on exit.
			 */
			truststore.deleteOnExit();
			accountKey.deleteOnExit();
			domainKey.deleteOnExit();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		/*
		 * Download the ACME CA's intermediate certificate and convert it from a
		 * PEM to an X.509 certificate and install it in a trust store.
		 */
		try {
			/*
			 * Retrieve the intermediate certificate that will be used to sign
			 * any generated certificates. This is re-generated on each run.
			 * 
			 * Currently, this test doesn't require this certificate, but this
			 * is just an example of how to do so for *real* tests that will
			 * verify that the generated certificate is actually signed by the
			 * root and intermediate certificates.
			 */
			pebbleIntermediateCertificate = AcmeFatUtils
					.getX509Certificate(new ByteArrayInputStream(FATSuite.pebble.getAcmeCaIntermediateCertificate()));
			Log.info(AcmeClientTest.class, "<cinit>",
					"Pebble Intermediate Cert: " + String.valueOf(pebbleIntermediateCertificate));

			/*
			 * Get the certificate generated by miniCA for the ACME HTTPS API.
			 * This is static and required to be used to talk to ACME over
			 * HTTPS.
			 */
			Certificate acmeHttpsCert = AcmeFatUtils.getX509Certificate(new FileInputStream(new File(FILE_MINICA_PEM)));
			Log.info(AcmeClientTest.class, "<cinit>", "ACME HTTPS Cert: " + String.valueOf(acmeHttpsCert));

			/*
			 * Write it to a JKS.
			 */
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(null, null);
			ks.setCertificateEntry("acme-https", acmeHttpsCert);
			ks.store(new FileOutputStream(TRUSTSTORE_FILE), TRUSTSTORE_PASSWORD.toCharArray());
		} catch (Exception e) {
			Log.error(AcmeClientTest.class, "<cinit>", e);
		}
	}

	@BeforeClass
	public static void beforeClass() throws IOException {
		/*
		 * Start a simple HTTP server to respond to challenges.
		 */
		challengeServer = new HttpChallengeServer(PebbleContainer.HTTP_PORT);
		challengeServer.start();
	}

	@AfterClass
	public static void afterClass() throws InterruptedException {
		if (challengeServer != null) {
			challengeServer.stop();
			challengeServer = null;
		}
	}

	@Before
	public void beforeTest() throws Exception {

		/*
		 * Setup the Mock DNS server to redirect requests to the test domains to
		 * this client.
		 */
		for (String domain : new String[] { TEST_DOMAIN_1, TEST_DOMAIN_2, TEST_DOMAIN_3, TEST_DOMAIN_4 }) {
			/*
			 * Disable the IPv6 responses for this domain. The Pebble CA server
			 * responds on AAAA (IPv6) responses before A (IPv4) responses, and
			 * we don't currently have the testcontainer host's IPv6 address.
			 */
			FATSuite.challtestsrv.addARecord(domain, FATSuite.pebble.getClientHost());
			FATSuite.challtestsrv.addAAAARecord(domain, "");
		}
	}

	/**
	 * Fetch a certificate for a single domain.
	 * 
	 * @throws Exception
	 */
	@Test
	public void fetchCertificate_SingleDomain() throws Exception {

		/*
		 * Create an AcmeService to test.
		 */
		AcmeClient acmeClient = new AcmeClient(acmeDirectoryURI, FILE_ACCOUNT_KEY, FILE_DOMAIN_KEY, null);
		acmeClient.setAcceptTos(true);

		/*
		 * Link the new client to the challenge response server.
		 */
		challengeServer.setAcmeClient(acmeClient);

		/*
		 * Get the certificate from the ACME CA server.
		 */
		AcmeCertificate newCertificate = acmeClient
				.fetchCertificate(new CSROptions(Arrays.asList(new String[] { TEST_DOMAIN_1 })));

		/*
		 * Verify that the certificate returned from the ACME CA is signed by
		 * the ACME CA's intermediate certificate public key.
		 */
		newCertificate.getCertificate().verify(pebbleIntermediateCertificate.getPublicKey());
		assertEquals("CN=" + TEST_DOMAIN_1, newCertificate.getCertificate().getSubjectDN().getName());
	}

	/**
	 * Test fetching a certificate form the ACME CA for several domains.
	 * 
	 * @throws Exception
	 */
	@Test
	public void fetchCertificate_MultiDomain_Success() throws Exception {

		/*
		 * Create an AcmeService to test.
		 */
		AcmeClient acmeClient = new AcmeClient(acmeDirectoryURI, FILE_ACCOUNT_KEY, FILE_DOMAIN_KEY, null);
		acmeClient.setAcceptTos(true);

		/*
		 * Link the new client to the challenge response server.
		 */
		challengeServer.setAcmeClient(acmeClient);

		/*
		 * Get the certificate from the ACME CA server.
		 */
		AcmeCertificate newCertificate = acmeClient.fetchCertificate(new CSROptions(
				Arrays.asList(new String[] { TEST_DOMAIN_1, TEST_DOMAIN_2, TEST_DOMAIN_3, TEST_DOMAIN_4 })));

		/*
		 * Verify that the certificate returned from the ACME CA is signed by
		 * the ACME CA's intermediate certificate public key.
		 * 
		 * When processing multiple domains, the subject DN will be that of the
		 * first domain.
		 */
		newCertificate.getCertificate().verify(pebbleIntermediateCertificate.getPublicKey());
		assertEquals("CN=" + TEST_DOMAIN_1, newCertificate.getCertificate().getSubjectDN().getName());
	}

	/**
	 * Validate that an exception is thrown if the terms of service are no
	 * accepted.
	 * 
	 * @throws Exception
	 */
	@Test
	public void fetchCertificate_RejectTermsOfService() throws Exception {

		/**
		 * We want a new account so delete the existing account key file.
		 */
		File accountKeyFile = new File(FILE_ACCOUNT_KEY);
		if (accountKeyFile.exists()) {
			accountKeyFile.delete();
		}

		/*
		 * Without accepting the terms of service, fetchCertificate is going to
		 * fail. Expect the following exception and message.
		 */
		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage(
				"The account must accept the terms of service by setting the ACME CA configuration to have a value of \"true\" for 'acceptTermsOfService' "
						+ "in the server.xml. The terms of service can be found at the following URI: data:text/plain,Do%20what%20thou%20wilt");

		/*
		 * Create an AcmeService to test.
		 */
		AcmeClient acmeClient = new AcmeClient(acmeDirectoryURI, FILE_ACCOUNT_KEY, FILE_DOMAIN_KEY, null);
		acmeClient.setAcceptTos(false);

		/*
		 * Link the new client to the challenge response server.
		 */
		challengeServer.setAcmeClient(acmeClient);

		/*
		 * Get the certificate from the ACME CA server.
		 */
		acmeClient.fetchCertificate(new CSROptions(Arrays.asList(new String[] { TEST_DOMAIN_1 })));
	}

	/**
	 * Verify that we can revoke a fetched certificate.
	 * 
	 * @throws Exception
	 */
	@Test
	public void fetchCertificate_Revoke() throws Exception {
		/*
		 * Create an AcmeService to test.
		 */
		AcmeClient acmeClient = new AcmeClient(acmeDirectoryURI, FILE_ACCOUNT_KEY, FILE_DOMAIN_KEY, null);
		acmeClient.setAcceptTos(true);

		/*
		 * Link the new client to the challenge response server.
		 */
		challengeServer.setAcmeClient(acmeClient);

		/*
		 * Get the certificate from the ACME CA server.
		 */
		AcmeCertificate newCertificate = acmeClient
				.fetchCertificate(new CSROptions(Arrays.asList(new String[] { TEST_DOMAIN_1 })));

		/*
		 * The certificate should be valid.
		 */
		assertEquals("The new certificate should be valid.", "Valid",
				FATSuite.pebble.getAcmeCertificateStatus(newCertificate.getCertificate()));

		/*
		 * Revoke the certificate.
		 */
		acmeClient.revoke(newCertificate.getCertificate());

		/*
		 * The certificate should now be revoked.
		 */
		assertEquals("The new certificate should be revoked.", "Revoked",
				FATSuite.pebble.getAcmeCertificateStatus(newCertificate.getCertificate()));
	}
}
