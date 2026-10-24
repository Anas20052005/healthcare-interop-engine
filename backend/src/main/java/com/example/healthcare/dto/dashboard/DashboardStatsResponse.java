package com.example.healthcare.dto.dashboard;

import com.example.healthcare.dto.delivery.DeliveryResultResponse;
import com.example.healthcare.dto.hl7.ConversionHistoryResponse;
import java.util.List;

/**
 * Data transfer object encapsulating comprehensive, tenant-isolated real-time
 * dashboard statistics derived directly from persistent database records.
 */
public class DashboardStatsResponse {

    private Long hospitalId;
    private String hospitalName;
    private String hospitalCode;

    // Conversion statistics
    private long totalConversions;
    private long successfulConversions;
    private long failedConversions;
    private double conversionSuccessRate;

    // Clinical resources processed
    private long patientsProcessed;
    private long encountersProcessed;
    private long observationsProcessed;

    // Delivery statistics (Priority 7, 9)
    private long totalDeliveries;
    private long successfulDeliveries;
    private long failedDeliveries;
    private long retryingDeliveries;
    private long pendingDeliveries;
    private double deliverySuccessRate;

    // Destination metrics (Priority 5, 6)
    private long totalDestinations;
    private long enabledDestinations;

    // System / Engine Health (Priority 10)
    private String engineStatus;
    private String databaseStatus;

    // Recent activity
    private List<ConversionHistoryResponse> recentConversions;
    private List<DeliveryResultResponse> recentDeliveries;

    public DashboardStatsResponse() {
    }

    public DashboardStatsResponse(Long hospitalId, String hospitalName, String hospitalCode,
                                  long totalConversions, long successfulConversions, long failedConversions,
                                  double conversionSuccessRate, long patientsProcessed, long encountersProcessed,
                                  long observationsProcessed, long totalDeliveries, long successfulDeliveries,
                                  long failedDeliveries, long retryingDeliveries, long pendingDeliveries,
                                  double deliverySuccessRate, long totalDestinations, long enabledDestinations,
                                  String engineStatus, String databaseStatus,
                                  List<ConversionHistoryResponse> recentConversions,
                                  List<DeliveryResultResponse> recentDeliveries) {
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.hospitalCode = hospitalCode;
        this.totalConversions = totalConversions;
        this.successfulConversions = successfulConversions;
        this.failedConversions = failedConversions;
        this.conversionSuccessRate = conversionSuccessRate;
        this.patientsProcessed = patientsProcessed;
        this.encountersProcessed = encountersProcessed;
        this.observationsProcessed = observationsProcessed;
        this.totalDeliveries = totalDeliveries;
        this.successfulDeliveries = successfulDeliveries;
        this.failedDeliveries = failedDeliveries;
        this.retryingDeliveries = retryingDeliveries;
        this.pendingDeliveries = pendingDeliveries;
        this.deliverySuccessRate = deliverySuccessRate;
        this.totalDestinations = totalDestinations;
        this.enabledDestinations = enabledDestinations;
        this.engineStatus = engineStatus;
        this.databaseStatus = databaseStatus;
        this.recentConversions = recentConversions;
        this.recentDeliveries = recentDeliveries;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getHospitalCode() {
        return hospitalCode;
    }

    public void setHospitalCode(String hospitalCode) {
        this.hospitalCode = hospitalCode;
    }

    public long getTotalConversions() {
        return totalConversions;
    }

    public void setTotalConversions(long totalConversions) {
        this.totalConversions = totalConversions;
    }

    public long getSuccessfulConversions() {
        return successfulConversions;
    }

    public void setSuccessfulConversions(long successfulConversions) {
        this.successfulConversions = successfulConversions;
    }

    public long getFailedConversions() {
        return failedConversions;
    }

    public void setFailedConversions(long failedConversions) {
        this.failedConversions = failedConversions;
    }

    public double getConversionSuccessRate() {
        return conversionSuccessRate;
    }

    public void setConversionSuccessRate(double conversionSuccessRate) {
        this.conversionSuccessRate = conversionSuccessRate;
    }

    public long getPatientsProcessed() {
        return patientsProcessed;
    }

    public void setPatientsProcessed(long patientsProcessed) {
        this.patientsProcessed = patientsProcessed;
    }

    public long getEncountersProcessed() {
        return encountersProcessed;
    }

    public void setEncountersProcessed(long encountersProcessed) {
        this.encountersProcessed = encountersProcessed;
    }

    public long getObservationsProcessed() {
        return observationsProcessed;
    }

    public void setObservationsProcessed(long observationsProcessed) {
        this.observationsProcessed = observationsProcessed;
    }

    public long getTotalDeliveries() {
        return totalDeliveries;
    }

    public void setTotalDeliveries(long totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
    }

    public long getSuccessfulDeliveries() {
        return successfulDeliveries;
    }

    public void setSuccessfulDeliveries(long successfulDeliveries) {
        this.successfulDeliveries = successfulDeliveries;
    }

    public long getFailedDeliveries() {
        return failedDeliveries;
    }

    public void setFailedDeliveries(long failedDeliveries) {
        this.failedDeliveries = failedDeliveries;
    }

    public long getRetryingDeliveries() {
        return retryingDeliveries;
    }

    public void setRetryingDeliveries(long retryingDeliveries) {
        this.retryingDeliveries = retryingDeliveries;
    }

    public long getPendingDeliveries() {
        return pendingDeliveries;
    }

    public void setPendingDeliveries(long pendingDeliveries) {
        this.pendingDeliveries = pendingDeliveries;
    }

    public double getDeliverySuccessRate() {
        return deliverySuccessRate;
    }

    public void setDeliverySuccessRate(double deliverySuccessRate) {
        this.deliverySuccessRate = deliverySuccessRate;
    }

    public long getTotalDestinations() {
        return totalDestinations;
    }

    public void setTotalDestinations(long totalDestinations) {
        this.totalDestinations = totalDestinations;
    }

    public long getEnabledDestinations() {
        return enabledDestinations;
    }

    public void setEnabledDestinations(long enabledDestinations) {
        this.enabledDestinations = enabledDestinations;
    }

    public String getEngineStatus() {
        return engineStatus;
    }

    public void setEngineStatus(String engineStatus) {
        this.engineStatus = engineStatus;
    }

    public String getDatabaseStatus() {
        return databaseStatus;
    }

    public void setDatabaseStatus(String databaseStatus) {
        this.databaseStatus = databaseStatus;
    }

    public List<ConversionHistoryResponse> getRecentConversions() {
        return recentConversions;
    }

    public void setRecentConversions(List<ConversionHistoryResponse> recentConversions) {
        this.recentConversions = recentConversions;
    }

    public List<DeliveryResultResponse> getRecentDeliveries() {
        return recentDeliveries;
    }

    public void setRecentDeliveries(List<DeliveryResultResponse> recentDeliveries) {
        this.recentDeliveries = recentDeliveries;
    }
}
