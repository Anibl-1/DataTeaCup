package com.dataplatform;

import com.dataplatform.data.service.LicenseLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LicenseLimitServiceTest {

    private static final String LEGACY_VALID_KEY =
            "DP-LICENSE-2026-ENTERPRISE-DataTeaCup-FULL-UNLOCK-9K4M-Q8R2-V7TX-P3NC";
    private static final String LEGACY_VALID_KEY_HASH =
            "9332ae55e4f3e5eaa71e1fd3c800666124b0b2f086498ccddc5533204ac8c870";
    private static final String TEST_PUBLIC_KEY =
            "MCowBQYDK2VwAyEA1l059vgJtz0l7XXHxjcXF2cJjZF4E7XK+o0VgDv4Hg8=";
    private static final String TEST_PRIVATE_KEY =
            "MC4CAQAwBQYDK2VwBCIEIItdv+hZtTthXku6rIbVPo9+0snuNuc1EdDHxFrMmPFW";

    @TempDir
    Path tempDir;

    @Test
    void missingKeyFileUsesDefaultLimits() {
        Path missing = tempDir.resolve("missing-license.json");
        LicenseLimitService service = serviceFor(missing);

        Map<String, Object> status = service.getStatus();

        assertThat(status.get("validKeyFile")).isEqualTo(false);
        assertThat(status.get("unlocked")).isEqualTo(false);
        assertThat(status.get("licenseMode")).isEqualTo("default");
        assertThat(status.get("reportExportMaxRows")).isEqualTo(100_000L);
        assertThat(status.get("chartCenterPageLimit")).isEqualTo(10L);
        assertThat(status.get("reportCenterPageLimit")).isEqualTo(20L);
        assertThat(status.get("aiDailyQuestionLimit")).isEqualTo(20L);
        assertThat(status.get("pipelineLimit")).isEqualTo(10L);
    }

    @Test
    void blankOrWrongKeyFileDoesNotUnlock() throws Exception {
        Path file = tempDir.resolve("datateacup-license.json");

        Files.writeString(file, "not-a-valid-license-key", StandardCharsets.UTF_8);
        LicenseLimitService wrongKeyService = serviceFor(file);
        assertThat(wrongKeyService.getStatus().get("unlocked")).isEqualTo(false);
        assertThat(wrongKeyService.getReportExportMaxRows()).isEqualTo(100_000L);

        Files.writeString(file, "   ", StandardCharsets.UTF_8);
        LicenseLimitService blankService = serviceFor(file);
        assertThat(blankService.getStatus().get("unlocked")).isEqualTo(false);
        assertThat(blankService.getChartCenterPageLimit()).isEqualTo(10L);
    }

    @Test
    void rawComplexKeyFileDoesNotUnlockWithoutConfiguredHash() throws Exception {
        Path file = tempDir.resolve("datateacup-license.json");
        Files.writeString(file, LEGACY_VALID_KEY, StandardCharsets.UTF_8);

        LicenseLimitService service = serviceFor(file);

        assertThat(service.getStatus().get("unlocked")).isEqualTo(false);
        assertThat(service.getReportExportMaxRows()).isEqualTo(100_000L);
    }

    @Test
    void signedLicenseFileUnlocksAllLimits() throws Exception {
        Path file = tempDir.resolve("datateacup-license.json");
        Files.writeString(file, signedLicense(-1, -1, -1, -1, -1), StandardCharsets.UTF_8);

        LicenseLimitService service = serviceFor(file);
        Map<String, Object> status = service.getStatus();

        assertThat(status.get("validKeyFile")).isEqualTo(true);
        assertThat(status.get("unlocked")).isEqualTo(true);
        assertThat(status.get("licenseMode")).isEqualTo("signed");
        assertThat(status.get("licenseId")).isEqualTo("DP-TEST-001");
        assertThat(status.get("edition")).isEqualTo("enterprise");
        assertThat(status.get("expiresAt")).isEqualTo("2099-12-31");
        assertThat(service.getReportExportMaxRows()).isEqualTo(-1L);
        assertThat(service.getChartCenterPageLimit()).isEqualTo(-1L);
        assertThat(service.getReportCenterPageLimit()).isEqualTo(-1L);
        assertThat(service.getAiDailyQuestionLimit()).isEqualTo(-1L);
        assertThat(service.getPipelineLimit()).isEqualTo(-1L);
    }

    @Test
    void signedLicenseCanOverrideLimits() throws Exception {
        Path file = tempDir.resolve("datateacup-license.json");
        Files.writeString(file, signedLicense(200_000, 30, 40, 50, 60), StandardCharsets.UTF_8);

        LicenseLimitService service = serviceFor(file);

        assertThat(service.getStatus().get("unlocked")).isEqualTo(true);
        assertThat(service.getReportExportMaxRows()).isEqualTo(200_000L);
        assertThat(service.getChartCenterPageLimit()).isEqualTo(30L);
        assertThat(service.getReportCenterPageLimit()).isEqualTo(40L);
        assertThat(service.getAiDailyQuestionLimit()).isEqualTo(50L);
        assertThat(service.getPipelineLimit()).isEqualTo(60L);
    }

    @Test
    void jsonWithoutSignatureOrConfiguredLegacyKeyDoesNotUnlock() throws Exception {
        Path file = tempDir.resolve("datateacup-license.json");
        Files.writeString(file, """
                {
                  "licenseKey": "%s",
                  "limits": {
                    "reportExportMaxRows": -1,
                    "chartCenterPageLimit": -1
                  }
                }
                """.formatted(LEGACY_VALID_KEY), StandardCharsets.UTF_8);

        LicenseLimitService service = serviceFor(file);

        assertThat(service.getStatus().get("unlocked")).isEqualTo(false);
        assertThat(service.getReportExportMaxRows()).isEqualTo(100_000L);
        assertThat(service.getChartCenterPageLimit()).isEqualTo(10L);
    }

    @Test
    void tamperedSignedLicenseDoesNotUnlock() throws Exception {
        Path file = tempDir.resolve("datateacup-license.json");
        String license = signedLicense(200_000, 30, 40, 50, 60)
                .replace("\"aiDailyQuestionLimit\": 50", "\"aiDailyQuestionLimit\": -1");
        Files.writeString(file, license, StandardCharsets.UTF_8);

        LicenseLimitService service = serviceFor(file);

        assertThat(service.getStatus().get("unlocked")).isEqualTo(false);
        assertThat(service.getStatus().get("validationMessage")).isEqualTo("signature verification failed");
        assertThat(service.getAiDailyQuestionLimit()).isEqualTo(20L);
    }

    @Test
    void legacyRawKeyCanStillUnlockWhenHashIsExplicitlyConfigured() throws Exception {
        Path file = tempDir.resolve("datateacup-license.json");
        Files.writeString(file, LEGACY_VALID_KEY, StandardCharsets.UTF_8);

        LicenseLimitService service = serviceFor(file, LEGACY_VALID_KEY_HASH);

        assertThat(service.getStatus().get("unlocked")).isEqualTo(true);
        assertThat(service.getStatus().get("licenseMode")).isEqualTo("legacy-key");
        assertThat(service.getReportExportMaxRows()).isEqualTo(-1L);
    }

    private LicenseLimitService serviceFor(Path path) {
        return serviceFor(path, null);
    }

    private LicenseLimitService serviceFor(Path path, String legacyHash) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("datateacup.license.file", path.toString())
                .withProperty("datateacup.license.public-key", TEST_PUBLIC_KEY);
        if (legacyHash != null) {
            environment.withProperty("datateacup.license.key-hash", legacyHash);
        }
        return new LicenseLimitService(new ObjectMapper(), environment);
    }

    private String signedLicense(long reportExportMaxRows, long chartCenterPageLimit,
                                 long reportCenterPageLimit, long aiDailyQuestionLimit,
                                 long pipelineLimit) throws Exception {
        String signature = signCanonicalPayload(reportExportMaxRows, chartCenterPageLimit,
                reportCenterPageLimit, aiDailyQuestionLimit, pipelineLimit);
        return """
                {
                  "licenseVersion": "2",
                  "licenseId": "DP-TEST-001",
                  "product": "DataTeaCup",
                  "subject": "DataTeaCup Test Customer",
                  "edition": "enterprise",
                  "issuedAt": "2024-01-01",
                  "notBefore": "2024-01-01",
                  "expiresAt": "2099-12-31",
                  "limits": {
                    "reportExportMaxRows": %d,
                    "chartCenterPageLimit": %d,
                    "reportCenterPageLimit": %d,
                    "aiDailyQuestionLimit": %d,
                    "pipelineLimit": %d
                  },
                  "signature": "ed25519:%s"
                }
                """.formatted(reportExportMaxRows, chartCenterPageLimit, reportCenterPageLimit,
                aiDailyQuestionLimit, pipelineLimit, signature);
    }

    private String signCanonicalPayload(long reportExportMaxRows, long chartCenterPageLimit,
                                        long reportCenterPageLimit, long aiDailyQuestionLimit,
                                        long pipelineLimit) throws Exception {
        String payload = String.join("\n",
                "licenseVersion=2",
                "licenseId=DP-TEST-001",
                "product=DataTeaCup",
                "subject=DataTeaCup Test Customer",
                "edition=enterprise",
                "issuedAt=2024-01-01",
                "notBefore=2024-01-01",
                "expiresAt=2099-12-31",
                "limits.aiDailyQuestionLimit=" + aiDailyQuestionLimit,
                "limits.chartCenterPageLimit=" + chartCenterPageLimit,
                "limits.pipelineLimit=" + pipelineLimit,
                "limits.reportCenterPageLimit=" + reportCenterPageLimit,
                "limits.reportExportMaxRows=" + reportExportMaxRows);

        byte[] privateKeyBytes = Base64.getDecoder().decode(TEST_PRIVATE_KEY);
        PrivateKey privateKey = KeyFactory.getInstance("Ed25519")
                .generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(privateKey);
        signer.update(("datateacup-license-v2\n" + payload).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }
}
