package com.dataplatform.analytics.controller;

import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report")
@RequirePermission("report:read")
public class ReportController {
    @Autowired
    private ReportService reportService;

    /**
     * 鏌ヨ鎶ヨ〃鏁版嵁
     *
     * @param dataSourceId 鏁版嵁婧怚D
     * @param tableName 琛ㄥ悕
     * @param page 椤电爜锛岄粯璁?
     * @param pageSize 姣忛〉澶у皬锛岄粯璁?0
     * @param filters 绛涢€夋潯浠讹紙JSON瀛楃涓诧級
     * @return 鍒嗛〉缁撴灉
     */
    @GetMapping("/query")
    public Result<PageResult<Map<String, Object>>> queryReport(
            @RequestParam Long dataSourceId,
            @RequestParam String tableName,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String filters) {
        // 鏌ヨ鏁版嵁鍒楄〃
        List<Map<String, Object>> list = reportService.queryReport(dataSourceId, tableName, page, pageSize, filters);
        // 鏌ヨ鎬绘暟
        long total = reportService.countReport(dataSourceId, tableName, filters);
        
        PageResult<Map<String, Object>> pageResult = new PageResult<>(list, total);
        return Result.success(pageResult);
    }

    /**
     * 瀵煎嚭鎶ヨ〃涓篍xcel
     *
     * @param dataSourceId 鏁版嵁婧怚D
     * @param tableName 琛ㄥ悕
     * @param filters 绛涢€夋潯浠讹紙JSON瀛楃涓诧紝鍙€夛級
     * @return Excel鏂囦欢鍝嶅簲
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportReport(
            @RequestParam Long dataSourceId,
            @RequestParam String tableName,
            @RequestParam(required = false) String filters) {
        try {
            byte[] data = reportService.exportReport(dataSourceId, tableName, filters);
            
            // 瀵规枃浠跺悕杩涜URL缂栫爜锛屾敮鎸佷腑鏂囨枃浠跺悕
            String encodedFileName = java.net.URLEncoder.encode(tableName + ".xlsx", "UTF-8")
                .replaceAll("\\+", "%20");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            // 浣跨敤RFC 5987鏍囧噯鐨刦ilename*鍙傛暟鏀寔UTF-8缂栫爜鐨勬枃浠跺悕
            headers.set("Content-Disposition", 
                "attachment; filename=\"" + tableName + ".xlsx\"; filename*=UTF-8''" + encodedFileName);
            headers.set("Access-Control-Expose-Headers", "Content-Disposition");
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (com.dataplatform.common.exception.BusinessException e) {
            // 涓氬姟寮傚父锛岃繑鍥濲SON閿欒鍝嶅簲
            Result<Void> errorResult = Result.error(e.getCode(), e.getMessage());
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(500)
                .headers(errorHeaders)
                .body(errorResult);
        } catch (Exception e) {
            // 鍏朵粬寮傚父锛岃繑鍥濲SON閿欒鍝嶅簲
            Result<Void> errorResult = Result.error(500, "瀵煎嚭澶辫触: " + e.getMessage());
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(500)
                .headers(errorHeaders)
                .body(errorResult);
        }
    }
    
    /**
     * 瀵煎嚭鎶ヨ〃涓篊SV
     *
     * @param dataSourceId 鏁版嵁婧怚D
     * @param tableName 琛ㄥ悕
     * @param filters 绛涢€夋潯浠讹紙JSON瀛楃涓诧紝鍙€夛級
     * @return CSV鏂囦欢鍝嶅簲
     */
    @GetMapping("/export/csv")
    public ResponseEntity<?> exportReportAsCsv(
            @RequestParam Long dataSourceId,
            @RequestParam String tableName,
            @RequestParam(required = false) String filters) {
        try {
            byte[] data = reportService.exportReportAsCsv(dataSourceId, tableName, filters);
            
            // 娣诲姞BOM澶达紝纭繚Excel姝ｇ‘璇嗗埆UTF-8缂栫爜
            byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
            byte[] dataWithBom = new byte[bom.length + data.length];
            System.arraycopy(bom, 0, dataWithBom, 0, bom.length);
            System.arraycopy(data, 0, dataWithBom, bom.length, data.length);
            
            // 瀵规枃浠跺悕杩涜URL缂栫爜
            String encodedFileName = java.net.URLEncoder.encode(tableName + ".csv", "UTF-8")
                .replaceAll("\\+", "%20");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8));
            headers.set("Content-Disposition", 
                "attachment; filename=\"" + tableName + ".csv\"; filename*=UTF-8''" + encodedFileName);
            headers.set("Access-Control-Expose-Headers", "Content-Disposition");
            return ResponseEntity.ok().headers(headers).body(dataWithBom);
        } catch (com.dataplatform.common.exception.BusinessException e) {
            Result<Void> errorResult = Result.error(e.getCode(), e.getMessage());
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(500)
                .headers(errorHeaders)
                .body(errorResult);
        } catch (Exception e) {
            Result<Void> errorResult = Result.error(500, "瀵煎嚭CSV澶辫触: " + e.getMessage());
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(500)
                .headers(errorHeaders)
                .body(errorResult);
        }
    }
}

