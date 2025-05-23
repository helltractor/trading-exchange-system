package com.helltractor.exchange.web;

import com.helltractor.exchange.bean.AuthToken;
import com.helltractor.exchange.support.LoggerSupport;
import com.helltractor.exchange.util.HttpUtil;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieService extends LoggerSupport {

    public final String SESSION_COOKIE = "_exsession_";

    @Value("#{exchangeConfiguration.hmacKey}")
    private String hmacKey;

    @Value("#{exchangeConfiguration.sessionTimeout}")
    private Duration sessionTimeout;

    public long getExpiresInSeconds() {
        return sessionTimeout.toSeconds();
    }

    @Nullable
    public AuthToken findSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE.equals(cookie.getName())) {
                String cookieStr = cookie.getValue();
                AuthToken token = AuthToken.fromSecureString(cookieStr, this.hmacKey);
                return token.isExpired() ? null : token;
            }
        }
        return null;
    }

    public void setSessionCookie(HttpServletRequest request, HttpServletResponse response, AuthToken token) {
        String cookieStr = token.toSecureString(this.hmacKey);
        logger.info("[Cookie] set session cookie: {}", cookieStr);
        Cookie cookie = new Cookie(SESSION_COOKIE, cookieStr);
        cookie.setPath("/");
        cookie.setMaxAge(3600);
        cookie.setHttpOnly(true);
        cookie.setSecure(HttpUtil.isSecure(request));
        String host = request.getServerName();
        if (host != null) {
            cookie.setDomain(host);
        }
        response.addCookie(cookie);
    }

    public void deleteSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        logger.info("delete session cookie...");
        Cookie cookie = new Cookie(SESSION_COOKIE, "-deleted-");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setHttpOnly(true);
        cookie.setSecure(HttpUtil.isSecure(request));
        String host = request.getServerName();
        if (host != null && host.startsWith("www.")) {
            // set cookie for domain "domain.com"
            String domain = host.substring(4);
            cookie.setDomain(domain);
        }
        response.addCookie(cookie);
    }
}
