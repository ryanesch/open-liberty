/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 */
public class AcmeCA extends ConfigElement {

    private String host;
    private String challengeType;
    private boolean alsoRevoke;
    private boolean acceptCATermsOfService;

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    @XmlAttribute(name = "host")
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the challengeType
     */
    public String getChallengeType() {
        return challengeType;
    }

    /**
     * @param challengeType the challengeType to set
     */
    @XmlAttribute(name = "challengeType")
    public void setChallengeType(String challengeType) {
        this.challengeType = challengeType;
    }

    /**
     * @return the alsoRevoke
     */
    public boolean isAlsoRevoke() {
        return alsoRevoke;
    }

    /**
     * @param alsoRevoke the alsoRevoke to set
     */
    public void setAlsoRevoke(boolean alsoRevoke) {
        this.alsoRevoke = alsoRevoke;
    }

    /**
     * @return the acceptCATermsOfService
     */
    public boolean isAcceptCATermsOfService() {
        return acceptCATermsOfService;
    }

    /**
     * @param acceptCATermsOfService the acceptCATermsOfService to set
     */
    public void setAcceptCATermsOfService(boolean acceptCATermsOfService) {
        this.acceptCATermsOfService = acceptCATermsOfService;
    }
}
