/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim;

/**
 * The interface containing all configuration related constants.
 */
public interface ConfigConstants {
    /**
     * The name of the property in vmm configuration data graph which represents a name.
     */
    String CONFIG_PROP_NAME = "name";
    /**
     * The type name of the custom properties type.
     */
    String CONFIG_DO_CUSTOM_PROPERTIES = "CustomProperties";

    String CONFIG_PROP_ALLOW_OPERATION_IF_REPOS_DOWN = "allowOperationIfReposDown";
    /**
     * The constant string for the error repository id key
     */
    String VALUE_CONTEXT_FAILURE_REPOSITORY_IDS_KEY = "failureRepositoryIDs";

    String CONFIG_DO_UNIQUE_USER_ID_MAPPING = "uniqueUserIdMapping";
    String CONFIG_DO_USER_SECURITY_NAME_MAPPING = "userSecurityNameMapping";
    String CONFIG_DO_USER_DISPLAY_NAME_MAPPING = "userDisplayNameMapping";
    String CONFIG_DO_UNIQUE_GROUP_ID_MAPPING = "uniqueGroupIdMapping";
    String CONFIG_DO_GROUP_SECURITY_NAME_MAPPING = "groupSecurityNameMapping";
    String CONFIG_DO_GROUP_DISPLAY_NAME_MAPPING = "groupDisplayNameMapping";

    String CONFIG_VALUE_SECURITY_USE_ACTIVE = "active";
    String CONFIG_VALUE_SECURITY_USE_INACTIVE = "inactive";
    String CONFIG_VALUE_SECURITY_USE_NOT_SELECTABLE = "notSelectable";

    // LDAP Adapter related configuration
    String CONFIG_PROP_SSL_CONFIGURATION = "sslConfiguration";

    String CONFIG_PROP_VALUE = "value";

    String CONFIG_PROP_OBJECTCLASS = "objectClass";

}
