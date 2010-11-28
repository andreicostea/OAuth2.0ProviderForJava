/*
 * Copyright 2010 Yutaka Obuchi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.oauth.v2.example.provider.servlets;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.v2.OAuth2;
import net.oauth.v2.OAuth2Accessor;
import net.oauth.v2.OAuth2Message;
import net.oauth.v2.OAuth2ProblemException;
import net.oauth.v2.example.provider.core.SampleOAuth2Provider;
import net.oauth.v2.server.OAuth2Servlet;

/**
 * Access Token request handler for OAuth2.0
 *
 * @author Yutaka Obuchi
 */
public class AccessTokenServlet2 extends HttpServlet {
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // nothing at this point
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        processRequest(request, response);
    }
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        processRequest(request, response);
    }
        
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try{
            OAuth2Message requestMessage = OAuth2Servlet.getMessage(request, null);
            String grant_type = requestMessage.getParameter(OAuth2.GRANT_TYPE);
            OAuth2Accessor accessor = null;
            
            if(grant_type.equals(OAuth2.GrantType.AUTHORIZATION_CODE)){
            	accessor = SampleOAuth2Provider.getAccessorByCode(requestMessage);
            }else if (grant_type.equals(OAuth2.GrantType.REFRESH_TOKEN)){
            	accessor = SampleOAuth2Provider.getAccessorByRefreshToken(requestMessage);
            }else{
            	OAuth2ProblemException problem = new OAuth2ProblemException(OAuth2.Problems.UNSUPPORTED_GRANT_TYPE);
            	throw problem;
            }
            
            SampleOAuth2Provider.VALIDATOR.validateRequestMessageForAccessToken(requestMessage, accessor);
            
            // make sure code is authorized
            if (!Boolean.TRUE.equals(accessor.getProperty("authorized"))) {
                 OAuth2ProblemException problem = new OAuth2ProblemException(OAuth2.Problems.NOT_MARKED_AS_AUTHORIZED);
                throw problem;
            }
            
            // generate access token and secret
            if(grant_type.equals(OAuth2.GrantType.AUTHORIZATION_CODE)){
                if(accessor.accessToken==null) SampleOAuth2Provider.generateAccessAndRefreshToken(accessor);
            }else if (grant_type.equals(OAuth2.GrantType.REFRESH_TOKEN)){
            	SampleOAuth2Provider.generateAccessAndRefreshToken(accessor);
            }
            
            response.setContentType("application/json");
            OutputStream out = response.getOutputStream();
            //OAuth.formEncode(OAuth.newList("oauth_token", accessor.accessToken,
            //                               "oauth_token_secret", accessor.tokenSecret),
            //                 out);
            
            OAuth2.formEncodeInJson(OAuth2.newList(OAuth2.ACCESS_TOKEN, accessor.accessToken,
            								      OAuth2.EXPIRES_IN, "3600",
            								      OAuth2.REFRESH_TOKEN, accessor.refreshToken), out);
            // send response back with JSON
            out.close();
            
        } catch (Exception e){
        	// sendback without Authorization Header
        	// sendback json
        	Boolean sendBodyInJson = true;
            Boolean withAuthHeader = false;
            SampleOAuth2Provider.handleException(e, request, response, sendBodyInJson, withAuthHeader);
        }
    }

    private static final long serialVersionUID = 1L;

}
