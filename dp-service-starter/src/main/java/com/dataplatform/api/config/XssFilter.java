package com.dataplatform.api.config;

import com.dataplatform.data.service.security.SecurityProtectionService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * XSS 过滤器
 */
@Slf4j
@Component
@Order(1)
public class XssFilter implements Filter {

    @Autowired(required = false)
    private SecurityProtectionService securityProtectionService;

    private static final Set<String> EXCLUDE_URLS = new HashSet<>(Arrays.asList(
        "/report/", "/chart/", "/page/", "/dataView/", "/collect/",
        "/datax/", "/api/public/", "/actuator/"
    ));

    private static final Set<String> EXCLUDE_CONTENT_TYPES = new HashSet<>(Arrays.asList(
        "multipart/form-data", "application/octet-stream"
    ));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String contentType = httpRequest.getContentType();
        if (contentType != null) {
            for (String excludeType : EXCLUDE_CONTENT_TYPES) {
                if (contentType.toLowerCase().contains(excludeType)) {
                    chain.doFilter(request, response);
                    return;
                }
            }
        }

        String uri = httpRequest.getRequestURI();
        for (String excludeUrl : EXCLUDE_URLS) {
            if (uri.contains(excludeUrl)) {
                chain.doFilter(request, response);
                return;
            }
        }

        XssHttpServletRequestWrapper wrappedRequest = new XssHttpServletRequestWrapper(
            httpRequest, securityProtectionService);

        chain.doFilter(wrappedRequest, response);
    }

    public static class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

        private final SecurityProtectionService securityProtectionService;
        private byte[] body;

        private static final Pattern[] XSS_PATTERNS = {
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bon\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</iframe>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<object[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<svg[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\beval\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdocument\\s*\\.", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwindow\\s*\\.", Pattern.CASE_INSENSITIVE),
        };

        private static final Set<String> SKIP_HEADERS = new HashSet<>(Arrays.asList(
            "authorization", "content-type", "content-length", "accept",
            "accept-encoding", "accept-language", "connection", "host",
            "origin", "referer", "user-agent", "cookie",
            "x-requested-with", "x-forwarded-for", "x-real-ip"
        ));

        public XssHttpServletRequestWrapper(HttpServletRequest request,
                                           SecurityProtectionService securityProtectionService) {
            super(request);
            this.securityProtectionService = securityProtectionService;
            try {
                String ct = request.getContentType();
                if (ct != null && ct.contains("application/json")) {
                    this.body = readRequestBody(request);
                }
            } catch (IOException e) {
                log.warn("读取请求体失败: {}", e.getMessage());
            }
        }

        private byte[] readRequestBody(HttpServletRequest request) throws IOException {
            try (InputStream inputStream = request.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                return outputStream.toByteArray();
            }
        }

        @Override
        public String[] getParameterValues(String parameter) {
            String[] values = super.getParameterValues(parameter);
            if (values == null) return null;
            String[] encodedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                encodedValues[i] = cleanXss(values[i]);
            }
            return encodedValues;
        }

        @Override
        public String getParameter(String parameter) {
            return cleanXss(super.getParameter(parameter));
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            if (name != null && SKIP_HEADERS.contains(name.toLowerCase())) return value;
            return cleanXss(value);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (body == null) return super.getInputStream();
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            String cleanedBody = cleanXss(bodyStr);
            byte[] cleanedBytes = cleanedBody.getBytes(StandardCharsets.UTF_8);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cleanedBytes);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return byteArrayInputStream.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener readListener) {}
                @Override public int read() throws IOException { return byteArrayInputStream.read(); }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        public String cleanXss(String value) {
            if (!StringUtils.hasText(value)) return value;
            String result = value;
            for (Pattern pattern : XSS_PATTERNS) {
                result = pattern.matcher(result).replaceAll("");
            }
            result = result.replace("\0", "");
            if (securityProtectionService != null) {
                result = securityProtectionService.filterXss(result);
            }
            return result;
        }

        public static boolean containsXss(String input) {
            if (!StringUtils.hasText(input)) return false;
            for (Pattern pattern : XSS_PATTERNS) {
                if (pattern.matcher(input).find()) return true;
            }
            return false;
        }
    }

    public static String cleanXss(String input) {
        if (!StringUtils.hasText(input)) return input;
        String result = input;
        for (Pattern pattern : XssHttpServletRequestWrapper.XSS_PATTERNS) {
            result = pattern.matcher(result).replaceAll("");
        }
        result = result.replace("\0", "");
        return result;
    }

    public static boolean containsXss(String input) {
        return XssHttpServletRequestWrapper.containsXss(input);
    }
}
