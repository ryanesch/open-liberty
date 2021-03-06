/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.web;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.TraceConstants;
import com.ibm.ws.webcontainer.internalRuntimeExport.srt.IPrivateRequestAttributes;

public class WebUtils {
    private static final TraceComponent tc = Tr.register(WebUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    /**
     * Encodes the given string using {@link java.net.URLEncoder} and UTF-8 encoding.
     *
     * @param value
     * @return
     */
    public static String urlEncode(String value) {
        if (value == null) {
            return value;
        }
        try {
            value = URLEncoder.encode(value, CommonWebConstants.UTF_8);
        } catch (UnsupportedEncodingException e) {
            // Do nothing - UTF-8 should always be supported
        }
        return value;
    }

    /**
     * Encodes each parameter in the provided query. Expects the query argument to be the query string of a URL with parameters
     * in the format: param=value(&param2=value2)*
     *
     * @param query
     * @return
     */
    public static String encodeQueryString(String query) {
        if (query == null) {
            return null;
        }

        StringBuilder rebuiltQuery = new StringBuilder();

        // Encode parameters to mitigate XSS attacks
        String[] queryParams = query.split("&");
        for (String param : queryParams) {
            String rebuiltParam = urlEncode(param);

            int equalIndex = param.indexOf("=");
            if (equalIndex > -1) {
                String name = param.substring(0, equalIndex);
                String value = (equalIndex < (param.length() - 1)) ? param.substring(equalIndex + 1) : "";
                rebuiltParam = urlEncode(name) + "=" + urlEncode(value);
            }

            if (!rebuiltParam.isEmpty()) {
                rebuiltQuery.append(rebuiltParam + "&");
            }
        }
        // Remove trailing '&' character
        if (rebuiltQuery.length() > 0 && rebuiltQuery.charAt(rebuiltQuery.length() - 1) == '&') {
            rebuiltQuery.deleteCharAt(rebuiltQuery.length() - 1);
        }
        return rebuiltQuery.toString();
    }

    /**
     * Encodes the given string so that it can be used as an HTTP cookie value.
     *
     * @param string
     *            the string to convert
     */
    public static String encodeCookie(String string) {
        if (string == null) {
            return null;
        }
        string = string.replaceAll("%", "%25");
        string = string.replaceAll(";", "%3B");
        string = string.replaceAll(",", "%2C");
        return string;
    }

    /**
     * Decodes the given string from percent encoding.
     *
     * @param string
     *            the string to convert
     */
    public static String decodeCookie(String string) {
        if (string == null) {
            return null;
        }
        string = string.replaceAll("%2C", ",");
        string = string.replaceAll("%3B", ";");
        string = string.replaceAll("%25", "%");
        return string;
    }

    /**
     * Encodes the given string so that it can be used within an HTML page.
     * 
     * @param string
     *            the string to convert
     */
    public static String htmlEncode(String string) {
        return htmlEncode(string, true, true, true);
    }

    /**
     * Encodes the given string so that it can be used within an HTML page.
     * 
     * @param string
     *            the string to convert
     * @param encodeNewline
     *            if true newline characters are converted to &lt;br&gt;'s
     * @param encodeSubsequentBlanksToNbsp
     *            if true subsequent blanks are converted to &amp;nbsp;'s
     * @param encodeNonLatin
     *            if true encode non-latin characters as numeric character references
     */
    public static String htmlEncode(String string, boolean encodeNewline, boolean encodeSubsequentBlanksToNbsp, boolean encodeNonLatin) {
        if (string == null) {
            return "";
        }

        StringBuilder sb = null; // create later on demand
        String app;
        char c;
        for (int i = 0; i < string.length(); ++i) {
            app = null;
            c = string.charAt(i);

            // All characters before letters
            if (c < 0x41) {
                switch (c) {
                case '"':
                    app = "&quot;";
                    break; // "
                case '&':
                    app = "&amp;";
                    break; // &
                case '<':
                    app = "&lt;";
                    break; // <
                case '>':
                    app = "&gt;";
                    break; // >
                case ' ':
                    if (encodeSubsequentBlanksToNbsp && (i == 0 || (i - 1 >= 0 && string.charAt(i - 1) == ' '))) {
                        // Space at beginning or after another space
                        app = "&#160;";
                    }
                    break;
                case '\n':
                    if (encodeNewline) {
                        app = "<br/>";
                    }
                    break;
                default:
                    // No special encoding needed
                    break;
                }
            } else if (encodeNonLatin && c > 0x80) {
                switch (c) {
                // german umlauts
                case '\u00E4':
                    app = "&auml;";
                    break;
                case '\u00C4':
                    app = "&Auml;";
                    break;
                case '\u00F6':
                    app = "&ouml;";
                    break;
                case '\u00D6':
                    app = "&Ouml;";
                    break;
                case '\u00FC':
                    app = "&uuml;";
                    break;
                case '\u00DC':
                    app = "&Uuml;";
                    break;
                case '\u00DF':
                    app = "&szlig;";
                    break;

                // misc
                // case 0x80: app = "&euro;"; break; sometimes euro symbol is ascii 128, should we suport it?
                case '\u20AC':
                    app = "&euro;";
                    break;
                case '\u00AB':
                    app = "&laquo;";
                    break;
                case '\u00BB':
                    app = "&raquo;";
                    break;
                case '\u00A0':
                    app = "&#160;";
                    break;

                default:
                    // encode all non basic latin characters
                    app = "&#" + ((int) c) + ";";
                    break;
                }
            }
            if (app != null) {
                if (sb == null) {
                    sb = new StringBuilder(string.substring(0, i));
                }
                sb.append(app);
            } else {
                if (sb != null) {
                    sb.append(c);
                }
            }
        }

        if (sb == null) {
            return string;
        } else {
            return sb.toString();
        }
    }

    public static boolean validateUriFormat(final String uri) {
        return validateUriFormat(uri, "https?://" + CommonWebConstants.VALID_URI_PATH_CHARS + "+");
    }

    public static boolean validateUriFormat(final String uri, String regexToMatch) {
        if (uri == null || uri.isEmpty()) {
            // TODO - NLS message
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Provided URI [" + uri + "] was null or empty");
            }
            return false;
        }
        try {
            // Verify that the authorization endpoint is a valid URI
            AccessController.doPrivileged(new PrivilegedExceptionAction<URI>() {
                @Override
                public URI run() throws URISyntaxException {
                    return new URI(uri);
                }
            });
        } catch (PrivilegedActionException e) {
            // TODO - NLS message
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "URI was not formatted correctly: " + e.getException().getLocalizedMessage());
            }
            return false;
        }
        if (!Pattern.matches(regexToMatch, uri)) {
            // TODO - NLS message?
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "URI did not match expected URI pattern");
            }
            return false;
        }
        return true;
    }

    /* a little bit of magic from webcontainer for use with proxies */
    /* beware, returns the https port, even if protocol isn't https */
    public Integer getRedirectPortFromRequest(HttpServletRequest req) {

        HttpServletRequest sr = getWrappedServletRequestObject(req);
        if (sr instanceof IPrivateRequestAttributes) {
            return (Integer) ((IPrivateRequestAttributes) sr).getPrivateAttribute("SecurityRedirectPort");
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getRedirectUrl called for non-IPrivateRequestAttributes object", req);
            }
            return null;
        }

    }

    /**
     * Drill down through any possible HttpServletRequestWrapper objects.
     *
     * @param sr
     * @return
     */
    private HttpServletRequest getWrappedServletRequestObject(HttpServletRequest sr) {
        if (sr instanceof HttpServletRequestWrapper) {
            HttpServletRequestWrapper w = (HttpServletRequestWrapper) sr;
            // make sure we drill all the way down to an
            // SRTServletRequest...there
            // may be multiple proxied objects
            sr = (HttpServletRequest) w.getRequest();
            while (sr instanceof HttpServletRequestWrapper)
                sr = (HttpServletRequest) ((HttpServletRequestWrapper) sr).getRequest();
        }
        return sr;
    }

}
