package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime limits that are locked by default and can be overridden by a key file.
 */
@Slf4j
@Service
public class LicenseLimitService {

    private static final long UNLIMITED = -1L;
    private static final long DEFAULT_REPORT_EXPORT_MAX_ROWS = 100_000L;
    private static final long DEFAULT_CHART_CENTER_PAGE_LIMIT = 10L;
    private static final long DEFAULT_REPORT_CENTER_PAGE_LIMIT = 20L;
    private static final long DEFAULT_AI_DAILY_QUESTION_LIMIT = 20L;
    private static final long DEFAULT_PIPELINE_LIMIT = 10L;
    private static final long REFRESH_INTERVAL_MS = 5_000L;
    private static final String FIXED_CONTAINER_LICENSE_FILE = "/app/license/datateacup-license.json";
    private static final String LEGACY_FIXED_CONTAINER_LICENSE_FILE = "/app/license/datateacup-license.json";
    private static final String LOCAL_LICENSE_FILE = "runtime/license/datateacup-license.json";
    private static final String LEGACY_LOCAL_LICENSE_FILE = "runtime/license/datateacup-license.json";
    private static final String LICENSE_VERSION = "2";
    private static final String PRODUCT_NAME = "DataTeaCup";
    private static final String LEGACY_PRODUCT_NAME = "DataTeaCup";
    private static final String SIGNING_CONTEXT = "datateacup-license-v2";
    private static final String DEFAULT_LICENSE_PUBLIC_KEY =
            "MCowBQYDK2VwAyEAMY28huOA7qXLq5Lh/zZHDp+OZzH3VMAHNjeBSO1lXLc=";

    private final ObjectMapper objectMapper;
    private final Environment environment;

    private volatile LimitSnapshot cachedSnapshot;
    private volatile long lastCheckedAt;
    private volatile String lastPath;
    private volatile long lastModified = Long.MIN_VALUE;

    public LicenseLimitService(ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.environment = environment;
        Path path = resolveLicensePath();
        this.cachedSnapshot = LimitSnapshot.defaults(path.toString());
    }

    public long getReportExportMaxRows() {
        return snapshot().reportExportMaxRows;
    }

    public long getChartCenterPageLimit() {
        return snapshot().chartCenterPageLimit;
    }

    public long getReportCenterPageLimit() {
        return snapshot().reportCenterPageLimit;
    }

    public long getAiDailyQuestionLimit() {
        return snapshot().aiDailyQuestionLimit;
    }

    public long getPipelineLimit() {
        return snapshot().pipelineLimit;
    }

    public void assertReportExportRowsAllowed(long rows) {
        long limit = getReportExportMaxRows();
        if (isLimited(limit) && rows > limit) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    String.format("报表导出最多支持 %,d 行，当前约 %,d 行。请减少筛选条件，或放入有效密钥文件解锁限制。", limit, rows));
        }
    }

    public void assertChartPageCreationAllowed(long currentCount) {
        assertCreationAllowed("图表中心页面", currentCount, getChartCenterPageLimit());
    }

    public void assertReportPageCreationAllowed(long currentCount) {
        assertCreationAllowed("报表中心页面", currentCount, getReportCenterPageLimit());
    }

    public void assertPipelineCreationAllowed(long currentCount) {
        assertCreationAllowed("数据流程", currentCount, getPipelineLimit());
    }

    public Map<String, Object> getStatus() {
        LimitSnapshot s = snapshot();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("validKeyFile", s.validKeyFile);
        result.put("unlocked", s.unlocked);
        result.put("licenseFile", s.source);
        result.put("licenseMode", s.licenseMode);
        result.put("validationMessage", s.validationMessage);
        result.put("licenseId", s.licenseId);
        result.put("subject", s.subject);
        result.put("edition", s.edition);
        result.put("expiresAt", s.expiresAt);
        result.put("reportExportMaxRows", s.reportExportMaxRows);
        result.put("chartCenterPageLimit", s.chartCenterPageLimit);
        result.put("reportCenterPageLimit", s.reportCenterPageLimit);
        result.put("aiDailyQuestionLimit", s.aiDailyQuestionLimit);
        result.put("pipelineLimit", s.pipelineLimit);
        return result;
    }

    private void assertCreationAllowed(String label, long currentCount, long limit) {
        if (isLimited(limit) && currentCount >= limit) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    String.format("%s最多支持 %,d 个，当前已创建 %,d 个。请删除无用数据，或放入有效密钥文件解锁限制。",
                            label, limit, currentCount));
        }
    }

    private boolean isLimited(long limit) {
        return limit >= 0;
    }

    private LimitSnapshot snapshot() {
        long now = System.currentTimeMillis();
        if (now - lastCheckedAt < REFRESH_INTERVAL_MS) {
            return cachedSnapshot;
        }

        synchronized (this) {
            now = System.currentTimeMillis();
            if (now - lastCheckedAt < REFRESH_INTERVAL_MS) {
                return cachedSnapshot;
            }
            lastCheckedAt = now;

            Path path = resolveLicensePath();
            String pathText = path.toString();
            long modified = readLastModified(path);
            if (pathText.equals(lastPath) && modified == lastModified) {
                return cachedSnapshot;
            }

            lastPath = pathText;
            lastModified = modified;
            cachedSnapshot = loadSnapshot(path);
            return cachedSnapshot;
        }
    }

    private Path resolveLicensePath() {
        String configured = environment.getProperty("datateacup.license.file");
        if (!StringUtils.hasText(configured)) {
            configured = environment.getProperty("DATATEACUP_LICENSE_FILE");
        }
        if (!StringUtils.hasText(configured)) {
            configured = System.getenv("DATATEACUP_LICENSE_FILE");
        }
        if (StringUtils.hasText(configured)) {
            return toAbsolutePath(configured.trim());
        }

        Path fixedPath = Paths.get(FIXED_CONTAINER_LICENSE_FILE);
        if (Files.isRegularFile(fixedPath)) {
            return fixedPath.normalize();
        }
        Path legacyFixedPath = Paths.get(LEGACY_FIXED_CONTAINER_LICENSE_FILE);
        if (Files.isRegularFile(legacyFixedPath)) {
            return legacyFixedPath.normalize();
        }
        Path localPath = toAbsolutePath(LOCAL_LICENSE_FILE);
        if (Files.isRegularFile(localPath)) {
            return localPath;
        }
        Path legacyLocalPath = toAbsolutePath(LEGACY_LOCAL_LICENSE_FILE);
        if (Files.isRegularFile(legacyLocalPath)) {
            return legacyLocalPath;
        }
        return toAbsolutePath(LOCAL_LICENSE_FILE);
    }

    private Path toAbsolutePath(String pathText) {
        Path path = Paths.get(pathText);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path);
        }
        return path.normalize();
    }

    private long readLastModified(Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                return Long.MIN_VALUE;
            }
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception e) {
            log.warn("读取密钥文件修改时间失败: {}", e.getMessage());
            return Long.MIN_VALUE + 1;
        }
    }

    private LimitSnapshot loadSnapshot(Path path) {
        if (!Files.isRegularFile(path)) {
            return LimitSnapshot.defaults(path.toString());
        }

        try {
            String content = stripBom(Files.readString(path, StandardCharsets.UTF_8)).trim();
            if (!StringUtils.hasText(content)) {
                log.warn("密钥文件为空，继续使用默认限制: {}", path);
                return LimitSnapshot.defaults(path.toString());
            }

            if (!content.startsWith("{")) {
                String licenseKey = extractRawLicenseKey(content);
                if (!validateLegacyLicenseKey(licenseKey)) {
                    log.warn("密钥文件存在但密钥无效，继续使用默认限制: {}", path);
                    return LimitSnapshot.defaults(path.toString(), "legacy key hash is not configured or key does not match");
                }
                log.info("兼容密钥文件校验通过，功能限制已解锁: {}", path);
                return LimitSnapshot.unlimited(path.toString(), "legacy-key");
            }

            Map<String, Object> root = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
            LicenseValidation signedValidation = validateSignedLicense(root, path.toString());
            if (signedValidation.valid) {
                log.info("签名许可证文件校验通过，功能限制已按许可证生效: {}", path);
                return signedValidation.snapshot;
            }

            if (root.containsKey("signature")) {
                log.warn("签名许可证文件无效，继续使用默认限制: {}, reason={}", path, signedValidation.message);
                return LimitSnapshot.defaults(path.toString(), signedValidation.message);
            }

            LimitSnapshot legacySnapshot = loadLegacyJsonSnapshot(root, path.toString());
            if (legacySnapshot.validKeyFile) {
                log.info("兼容JSON密钥文件校验通过，功能限制已按密钥文件生效: {}", path);
                return legacySnapshot;
            }

            log.warn("许可证文件无有效签名或兼容密钥，继续使用默认限制: {}", path);
            return legacySnapshot;
        } catch (Exception e) {
            log.warn("密钥文件无效，继续使用默认限制: {}, error={}", path, e.getMessage());
            return LimitSnapshot.defaults(path.toString(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private LimitSnapshot loadLegacyJsonSnapshot(Map<String, Object> root, String source) {
        Map<String, Object> limits = root.get("limits") instanceof Map
                ? (Map<String, Object>) root.get("limits")
                : Map.of();

        if (!validateLegacyLicenseKey(root.get("licenseKey"))) {
            return LimitSnapshot.defaults(source, "legacy licenseKey is not configured or does not match");
        }

        long reportExportMaxRows = readLong(limits, root, "reportExportMaxRows", UNLIMITED);
        long chartCenterPageLimit = readLong(limits, root, "chartCenterPageLimit", UNLIMITED);
        long reportCenterPageLimit = readLong(limits, root, "reportCenterPageLimit", UNLIMITED);
        long aiDailyQuestionLimit = readLong(limits, root, "aiDailyQuestionLimit", UNLIMITED);
        long pipelineLimit = readLong(limits, root, "pipelineLimit", UNLIMITED);

        return new LimitSnapshot(true, true, source, "legacy-key",
                "legacy licenseKey hash matched", null, null, null, null,
                reportExportMaxRows, chartCenterPageLimit, reportCenterPageLimit,
                aiDailyQuestionLimit, pipelineLimit);
    }

    @SuppressWarnings("unchecked")
    private LicenseValidation validateSignedLicense(Map<String, Object> root, String source) {
        if (!StringUtils.hasText(root.get("signature") == null ? null : root.get("signature").toString())) {
            return LicenseValidation.invalid("signature is missing");
        }
        if (!LICENSE_VERSION.equals(root.get("licenseVersion") == null ? "" : root.get("licenseVersion").toString().trim())) {
            return LicenseValidation.invalid("unsupported licenseVersion");
        }

        String product = text(root.get("product"));
        if (!PRODUCT_NAME.equals(product) && !LEGACY_PRODUCT_NAME.equals(product)) {
            return LicenseValidation.invalid("product must be " + PRODUCT_NAME);
        }

        String licenseId = text(root.get("licenseId"));
        if (!StringUtils.hasText(licenseId)) {
            return LicenseValidation.invalid("licenseId is missing");
        }

        String issuedAt = text(root.get("issuedAt"));
        String notBefore = text(root.get("notBefore"));
        String expiresAt = text(root.get("expiresAt"));
        LocalDate today = LocalDate.now();
        if (StringUtils.hasText(issuedAt) && parseDate(issuedAt, "issuedAt").isAfter(today)) {
            return LicenseValidation.invalid("license is not issued yet");
        }
        if (StringUtils.hasText(notBefore) && parseDate(notBefore, "notBefore").isAfter(today)) {
            return LicenseValidation.invalid("license is not active yet");
        }
        if (!StringUtils.hasText(expiresAt)) {
            return LicenseValidation.invalid("expiresAt is missing");
        }
        if (parseDate(expiresAt, "expiresAt").isBefore(today)) {
            return LicenseValidation.invalid("license expired");
        }

        Map<String, Object> limits = root.get("limits") instanceof Map
                ? (Map<String, Object>) root.get("limits")
                : Map.of();

        long reportExportMaxRows = readLong(limits, root, "reportExportMaxRows", UNLIMITED);
        long chartCenterPageLimit = readLong(limits, root, "chartCenterPageLimit", UNLIMITED);
        long reportCenterPageLimit = readLong(limits, root, "reportCenterPageLimit", UNLIMITED);
        long aiDailyQuestionLimit = readLong(limits, root, "aiDailyQuestionLimit", UNLIMITED);
        long pipelineLimit = readLong(limits, root, "pipelineLimit", UNLIMITED);

        String subject = text(root.get("subject"));
        String edition = text(root.get("edition"));
        String canonicalPayload = canonicalPayload(licenseId, product, subject, edition, issuedAt,
                notBefore, expiresAt, reportExportMaxRows, chartCenterPageLimit, reportCenterPageLimit,
                aiDailyQuestionLimit, pipelineLimit);

        if (!verifySignature(canonicalPayload, text(root.get("signature")))) {
            return LicenseValidation.invalid("signature verification failed");
        }

        LimitSnapshot snapshot = new LimitSnapshot(true, true, source, "signed",
                "valid signed license", licenseId, subject, edition, expiresAt,
                reportExportMaxRows, chartCenterPageLimit, reportCenterPageLimit,
                aiDailyQuestionLimit, pipelineLimit);
        return LicenseValidation.valid(snapshot);
    }

    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " must be ISO date yyyy-MM-dd");
        }
    }

    private String extractRawLicenseKey(String content) {
        String text = stripBom(content);
        for (String line : text.lines().toList()) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed) || trimmed.startsWith("#")) {
                continue;
            }
            if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                    || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
            if (trimmed.startsWith("licenseKey=")) {
                return trimmed.substring("licenseKey=".length()).trim();
            }
            if (trimmed.startsWith("licenseKey:")) {
                return trimmed.substring("licenseKey:".length()).trim();
            }
            return trimmed;
        }
        return "";
    }

    private String stripBom(String text) {
        if (text != null && text.startsWith("\uFEFF")) {
            return text.substring(1);
        }
        return text;
    }

    private boolean validateLegacyLicenseKey(Object value) {
        if (value == null || !StringUtils.hasText(value.toString())) {
            return false;
        }
        String expectedHash = environment.getProperty("datateacup.license.key-hash");
        if (!StringUtils.hasText(expectedHash)) {
            expectedHash = environment.getProperty("DATATEACUP_LICENSE_KEY_HASH");
        }
        if (!StringUtils.hasText(expectedHash)) {
            expectedHash = System.getenv("DATATEACUP_LICENSE_KEY_HASH");
        }
        if (!StringUtils.hasText(expectedHash)) {
            return false;
        }
        return expectedHash.trim().equalsIgnoreCase(sha256(value.toString().trim()));
    }

    private boolean verifySignature(String canonicalPayload, String signatureText) {
        try {
            PublicKey publicKey = loadPublicKey();
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update((SIGNING_CONTEXT + "\n" + canonicalPayload).getBytes(StandardCharsets.UTF_8));
            return verifier.verify(decodeSignature(signatureText));
        } catch (Exception e) {
            log.warn("许可证签名校验失败: {}", e.getMessage());
            return false;
        }
    }

    private PublicKey loadPublicKey() throws Exception {
        String configured = environment.getProperty("datateacup.license.public-key");
        if (!StringUtils.hasText(configured)) {
            configured = environment.getProperty("DATATEACUP_LICENSE_PUBLIC_KEY");
        }
        if (!StringUtils.hasText(configured)) {
            configured = System.getenv("DATATEACUP_LICENSE_PUBLIC_KEY");
        }
        if (!StringUtils.hasText(configured)) {
            configured = DEFAULT_LICENSE_PUBLIC_KEY;
        }

        byte[] keyBytes = Base64.getDecoder().decode(configured.trim());
        return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    private byte[] decodeSignature(String signatureText) {
        String value = signatureText.trim();
        if (value.startsWith("ed25519:")) {
            value = value.substring("ed25519:".length()).trim();
        }
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ignored) {
            return Base64.getUrlDecoder().decode(padBase64(value));
        }
    }

    private String padBase64(String value) {
        int remainder = value.length() % 4;
        if (remainder == 0) {
            return value;
        }
        return value + "=".repeat(4 - remainder);
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256算法不可用", e);
        }
    }

    private long readLong(Map<String, Object> limits, Map<String, Object> root, String key, long defaultValue) {
        Object value = limits.containsKey(key) ? limits.get(key) : root.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = value.toString().trim();
        if ("unlimited".equalsIgnoreCase(text) || "none".equalsIgnoreCase(text)) {
            return UNLIMITED;
        }
        return Long.parseLong(text);
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private String canonicalPayload(String licenseId, String product, String subject, String edition,
                                    String issuedAt, String notBefore, String expiresAt,
                                    long reportExportMaxRows, long chartCenterPageLimit,
                                    long reportCenterPageLimit, long aiDailyQuestionLimit,
                                    long pipelineLimit) {
        return String.join("\n",
                "licenseVersion=" + LICENSE_VERSION,
                "licenseId=" + licenseId,
                "product=" + product,
                "subject=" + subject,
                "edition=" + edition,
                "issuedAt=" + issuedAt,
                "notBefore=" + notBefore,
                "expiresAt=" + expiresAt,
                "limits.aiDailyQuestionLimit=" + aiDailyQuestionLimit,
                "limits.chartCenterPageLimit=" + chartCenterPageLimit,
                "limits.pipelineLimit=" + pipelineLimit,
                "limits.reportCenterPageLimit=" + reportCenterPageLimit,
                "limits.reportExportMaxRows=" + reportExportMaxRows);
    }

    private static class LicenseValidation {
        private final boolean valid;
        private final String message;
        private final LimitSnapshot snapshot;

        private LicenseValidation(boolean valid, String message, LimitSnapshot snapshot) {
            this.valid = valid;
            this.message = message;
            this.snapshot = snapshot;
        }

        private static LicenseValidation valid(LimitSnapshot snapshot) {
            return new LicenseValidation(true, "valid", snapshot);
        }

        private static LicenseValidation invalid(String message) {
            return new LicenseValidation(false, message, null);
        }
    }

    private static class LimitSnapshot {
        private final boolean validKeyFile;
        private final boolean unlocked;
        private final String source;
        private final String licenseMode;
        private final String validationMessage;
        private final String licenseId;
        private final String subject;
        private final String edition;
        private final String expiresAt;
        private final long reportExportMaxRows;
        private final long chartCenterPageLimit;
        private final long reportCenterPageLimit;
        private final long aiDailyQuestionLimit;
        private final long pipelineLimit;

        private LimitSnapshot(boolean validKeyFile, boolean unlocked, String source,
                              String licenseMode, String validationMessage, String licenseId,
                              String subject, String edition, String expiresAt,
                              long reportExportMaxRows, long chartCenterPageLimit,
                              long reportCenterPageLimit, long aiDailyQuestionLimit,
                              long pipelineLimit) {
            this.validKeyFile = validKeyFile;
            this.unlocked = unlocked;
            this.source = source;
            this.licenseMode = licenseMode;
            this.validationMessage = validationMessage;
            this.licenseId = licenseId;
            this.subject = subject;
            this.edition = edition;
            this.expiresAt = expiresAt;
            this.reportExportMaxRows = reportExportMaxRows;
            this.chartCenterPageLimit = chartCenterPageLimit;
            this.reportCenterPageLimit = reportCenterPageLimit;
            this.aiDailyQuestionLimit = aiDailyQuestionLimit;
            this.pipelineLimit = pipelineLimit;
        }

        private static LimitSnapshot defaults(String source) {
            return defaults(source, "license file is missing");
        }

        private static LimitSnapshot defaults(String source, String validationMessage) {
            return new LimitSnapshot(false, false, source, "default", validationMessage,
                    null, null, null, null,
                    DEFAULT_REPORT_EXPORT_MAX_ROWS,
                    DEFAULT_CHART_CENTER_PAGE_LIMIT,
                    DEFAULT_REPORT_CENTER_PAGE_LIMIT,
                    DEFAULT_AI_DAILY_QUESTION_LIMIT,
                    DEFAULT_PIPELINE_LIMIT);
        }

        private static LimitSnapshot unlimited(String source, String licenseMode) {
            return new LimitSnapshot(true, true, source, licenseMode, "valid",
                    null, null, null, null,
                    UNLIMITED,
                    UNLIMITED,
                    UNLIMITED,
                    UNLIMITED,
                    UNLIMITED);
        }
    }
}
